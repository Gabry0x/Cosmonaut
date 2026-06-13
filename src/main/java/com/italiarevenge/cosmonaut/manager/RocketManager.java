package com.italiarevenge.cosmonaut.manager;

import com.italiarevenge.cosmonaut.Cosmonaut;
import com.italiarevenge.cosmonaut.model.Planet;
import com.italiarevenge.cosmonaut.model.Rocket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.PointedDripstone;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;

public class RocketManager {

    private final Cosmonaut plugin;
    private final Map<String, Rocket> rockets = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Set<UUID> inFlight = new HashSet<>();
    private File dataFile;
    private YamlConfiguration dataConfig;

    public RocketManager(Cosmonaut plugin) {
        this.plugin = plugin;
    }

    public void loadData() {
        dataFile = new File(plugin.getDataFolder(), "rockets.yml");
        if (!dataFile.exists()) return;
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        var sec = dataConfig.getConfigurationSection("rockets");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            String world = sec.getString(key + ".world");
            int x = sec.getInt(key + ".x");
            int y = sec.getInt(key + ".y");
            int z = sec.getInt(key + ".z");
            String dest = sec.getString(key + ".destination", "");
            World w = Bukkit.getWorld(world != null ? world : "");
            if (w == null) continue;
            Location loc = new Location(w, x, y, z);
            Rocket rocket = new Rocket(loc, dest);
            rockets.put(locationKey(loc), rocket);
        }
        plugin.getLogger().info("Caricati " + rockets.size() + " razzi.");
    }

    public void saveData() {
        if (dataFile == null) dataFile = new File(plugin.getDataFolder(), "rockets.yml");
        if (dataConfig == null) dataConfig = new YamlConfiguration();
        dataConfig.set("rockets", null);
        int i = 0;
        for (Rocket r : rockets.values()) {
            Location loc = r.getBaseLocation();
            String path = "rockets." + i;
            dataConfig.set(path + ".world", loc.getWorld() != null ? loc.getWorld().getName() : "");
            dataConfig.set(path + ".x", loc.getBlockX());
            dataConfig.set(path + ".y", loc.getBlockY());
            dataConfig.set(path + ".z", loc.getBlockZ());
            dataConfig.set(path + ".destination", r.getDestinationPlanet());
            i++;
        }
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public void registerRecipe() {
        ItemStack rocket = createRocketItem();
        ShapedRecipe recipe = new ShapedRecipe(Cosmonaut.ROCKET_ITEM_KEY, rocket);
        recipe.shape(" I ", "IGI", "ILI");
        recipe.setIngredient('I', Material.IRON_BLOCK);
        recipe.setIngredient('G', Material.GUNPOWDER);
        recipe.setIngredient('L', Material.LAVA_BUCKET);
        plugin.getServer().addRecipe(recipe);
    }

    public ItemStack createRocketItem() {
        ItemStack item = new ItemStack(Material.FIREWORK_ROCKET);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Razzo").color(NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("Piazza il razzo e usa /lancio").color(NamedTextColor.GRAY)
        ));
        meta.getPersistentDataContainer().set(Cosmonaut.ROCKET_ITEM_KEY, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createSpaceHelmet() {
        ItemStack item = new ItemStack(Material.IRON_HELMET);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Casco Spaziale").color(NamedTextColor.AQUA));
        meta.lore(List.of(
                Component.text("Protegge dall'assenza di atmosfera").color(NamedTextColor.GRAY)
        ));
        meta.getPersistentDataContainer().set(Cosmonaut.SPACE_HELMET_KEY, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createPressurizerItem() {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Generatore di Pressione").color(NamedTextColor.LIGHT_PURPLE));
        meta.lore(List.of(
                Component.text("Piazza per creare una zona pressurizzata").color(NamedTextColor.GRAY)
        ));
        meta.getPersistentDataContainer().set(Cosmonaut.PRESSURIZER_KEY, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isRocketItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(Cosmonaut.ROCKET_ITEM_KEY, PersistentDataType.BOOLEAN);
    }

    public boolean isPressurizerItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
                .has(Cosmonaut.PRESSURIZER_KEY, PersistentDataType.BOOLEAN);
    }

    public void placeRocketStructure(Block base) {
        base.setType(Material.IRON_BLOCK);
        base.getRelative(0, 1, 0).setType(Material.IRON_BLOCK);
        base.getRelative(0, 2, 0).setType(Material.IRON_BLOCK);
        Block cone = base.getRelative(0, 3, 0);
        cone.setType(Material.POINTED_DRIPSTONE);
        if (cone.getBlockData() instanceof PointedDripstone pd) {
            pd.setVerticalDirection(BlockFace.UP);
            pd.setThickness(PointedDripstone.Thickness.TIP);
            cone.setBlockData(pd);
        }
    }

    public boolean canPlaceRocket(Block base) {
        for (int i = 0; i < 4; i++) {
            if (!base.getRelative(0, i, 0).getType().isAir()) return false;
        }
        return true;
    }

    public boolean isValidRocketStructure(Location base) {
        World w = base.getWorld();
        if (w == null) return false;
        Block b0 = w.getBlockAt(base);
        if (b0.getType() != Material.IRON_BLOCK) return false;
        if (b0.getRelative(0, 1, 0).getType() != Material.IRON_BLOCK) return false;
        if (b0.getRelative(0, 2, 0).getType() != Material.IRON_BLOCK) return false;
        return b0.getRelative(0, 3, 0).getType() == Material.POINTED_DRIPSTONE;
    }

    public Rocket findNearestRocket(Location from, double maxDist) {
        double maxSq = maxDist * maxDist;
        Rocket nearest = null;
        double nearestSq = Double.MAX_VALUE;
        for (Rocket r : rockets.values()) {
            Location bl = r.getBaseLocation();
            if (!Objects.equals(bl.getWorld(), from.getWorld())) continue;
            double sq = bl.distanceSquared(from);
            if (sq < maxSq && sq < nearestSq) {
                nearestSq = sq;
                nearest = r;
            }
        }
        return nearest;
    }

    public void registerRocket(Location base, String destination) {
        rockets.put(locationKey(base), new Rocket(base, destination));
        saveData();
    }

    public void removeRocket(Location base) {
        rockets.remove(locationKey(base));
        saveData();
    }

    public void startLaunch(Player player, Rocket rocket) {
        if (inFlight.contains(player.getUniqueId())) return;
        inFlight.add(player.getUniqueId());

        Planet destination = plugin.getConfigManager().getPlanet(rocket.getDestinationPlanet());

        new BukkitRunnable() {
            int countdown = 10;

            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); inFlight.remove(player.getUniqueId()); return; }

                if (countdown > 0) {
                    NamedTextColor color = countdown <= 3 ? NamedTextColor.RED : NamedTextColor.YELLOW;
                    player.showTitle(Title.title(
                            Component.text(String.valueOf(countdown)).color(color),
                            Component.text("Lancio tra...").color(NamedTextColor.WHITE),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ofMillis(100))
                    ));
                    player.playSound(player.getLocation(),
                            Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1f,
                            countdown <= 3 ? 2f : 1f);
                    spawnCountdownParticles(rocket.getBaseLocation());
                    countdown--;
                } else {
                    cancel();
                    executeLaunch(player, rocket, destination);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void spawnCountdownParticles(Location base) {
        Location loc = base.clone().add(0.5, -0.5, 0.5);
        World w = loc.getWorld();
        if (w == null) return;
        w.spawnParticle(Particle.SMOKE, loc, 15, 0.3, 0.3, 0.3, 0.05);
    }

    private void executeLaunch(Player player, Rocket rocket, Planet destination) {
        GameMode originalMode = player.getGameMode();
        player.setGameMode(GameMode.SPECTATOR);
        player.setInvulnerable(true);

        Location base = rocket.getBaseLocation();
        World w = base.getWorld();
        if (w != null) {
            w.playSound(base, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.MASTER, 3f, 0.5f);
        }

        removeStructure(base);
        removeRocket(base);

        final Location[] pos = {base.clone().add(0.5, 4, 0.5)};

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); inFlight.remove(player.getUniqueId()); return; }

                ticks++;
                double speed = 0.4 + ticks * 0.015;
                pos[0].add(0, speed, 0);
                player.teleport(pos[0]);

                World pw = pos[0].getWorld();
                if (pw != null) {
                    Location exhaust = pos[0].clone().subtract(0, 3, 0);
                    pw.spawnParticle(Particle.FLAME, exhaust, 25, 0.4, 0.2, 0.4, 0.08);
                    pw.spawnParticle(Particle.SMOKE, exhaust, 20, 0.5, 0.5, 0.5, 0.05);
                }

                if (ticks >= 100) {
                    cancel();
                    showBlackScreenAndTeleport(player, destination, originalMode);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void showBlackScreenAndTeleport(Player player, Planet destination, GameMode originalMode) {
        player.showTitle(Title.title(
                Component.text("          ").color(NamedTextColor.BLACK),
                Component.empty(),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(700))
        ));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) { inFlight.remove(player.getUniqueId()); return; }

            World dest = destination != null ? Bukkit.getWorld(destination.getWorldName()) : null;
            if (dest != null) {
                player.teleport(dest.getSpawnLocation());
                player.setGameMode(originalMode);
                player.setInvulnerable(false);
                String planetName = destination.getName();
                player.showTitle(Title.title(
                        Component.text("Benvenuto su " + capitalize(planetName)).color(NamedTextColor.GOLD),
                        Component.empty(),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))
                ));
            } else {
                player.setGameMode(originalMode);
                player.setInvulnerable(false);
                player.sendMessage(Component.text("Errore: mondo di destinazione non trovato!").color(NamedTextColor.RED));
            }

            cooldowns.put(player.getUniqueId(),
                    System.currentTimeMillis() + plugin.getConfigManager().getLaunchCooldown() * 1000L);
            inFlight.remove(player.getUniqueId());
        }, 50L);
    }

    private void removeStructure(Location base) {
        World w = base.getWorld();
        if (w == null) return;
        for (int i = 0; i < 4; i++) {
            w.getBlockAt(base.clone().add(0, i, 0)).setType(Material.AIR);
        }
    }

    public boolean isOnCooldown(Player player) {
        Long expiry = cooldowns.get(player.getUniqueId());
        return expiry != null && System.currentTimeMillis() < expiry;
    }

    public long getCooldownSeconds(Player player) {
        Long expiry = cooldowns.get(player.getUniqueId());
        if (expiry == null) return 0;
        return Math.max(0, (expiry - System.currentTimeMillis()) / 1000);
    }

    public boolean isInFlight(UUID uuid) { return inFlight.contains(uuid); }

    private String locationKey(Location loc) {
        return (loc.getWorld() != null ? loc.getWorld().getName() : "null")
                + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
