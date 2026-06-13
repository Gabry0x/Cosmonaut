package com.italiarevenge.cosmonaut.manager;

import com.italiarevenge.cosmonaut.Cosmonaut;
import com.italiarevenge.cosmonaut.model.Planet;
import com.italiarevenge.cosmonaut.model.Rocket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.BlockDisplay;
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
            rockets.put(locationKey(loc), new Rocket(loc, dest));
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
        meta.lore(List.of(Component.text("Piazza il razzo e usa /lancio").color(NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(Cosmonaut.ROCKET_ITEM_KEY, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createSpaceHelmet() {
        ItemStack item = new ItemStack(Material.IRON_HELMET);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Casco Spaziale").color(NamedTextColor.AQUA));
        meta.lore(List.of(Component.text("Protegge dall'assenza di atmosfera").color(NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(Cosmonaut.SPACE_HELMET_KEY, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createPressurizerItem() {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Generatore di Pressione").color(NamedTextColor.LIGHT_PURPLE));
        meta.lore(List.of(Component.text("Piazza per creare una zona pressurizzata").color(NamedTextColor.GRAY)));
        meta.getPersistentDataContainer().set(Cosmonaut.PRESSURIZER_KEY, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isRocketItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(Cosmonaut.ROCKET_ITEM_KEY, PersistentDataType.BOOLEAN);
    }

    public boolean isPressurizerItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(Cosmonaut.PRESSURIZER_KEY, PersistentDataType.BOOLEAN);
    }

    public void placeRocketStructure(Block base) {
        for (BlockEntry e : getRocketBlocks())
            base.getRelative(e.dx(), e.dy(), e.dz()).setType(e.material());
    }

    private List<BlockEntry> getRocketBlocks() {
        List<BlockEntry> b = new ArrayList<>();
        // Booster legs Y=0..3
        for (int y = 0; y <= 3; y++) {
            b.add(new BlockEntry(-2, y, -2, Material.BLACK_CONCRETE));
            b.add(new BlockEntry( 2, y, -2, Material.BLACK_CONCRETE));
            b.add(new BlockEntry(-2, y,  2, Material.BLACK_CONCRETE));
            b.add(new BlockEntry( 2, y,  2, Material.BLACK_CONCRETE));
        }
        // Body Y=0..3 (3x3, black corners)
        for (int y = 0; y <= 3; y++)
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    b.add(new BlockEntry(dx, y, dz, corner(dx,dz) ? Material.BLACK_CONCRETE : Material.ORANGE_CONCRETE));
        // Lower metallic band Y=4
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                b.add(new BlockEntry(dx, 4, dz, Material.GRAY_CONCRETE));
        // Body Y=5
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                b.add(new BlockEntry(dx, 5, dz, corner(dx,dz) ? Material.BLACK_CONCRETE : Material.ORANGE_CONCRETE));
        // Window rows Y=6..7
        for (int y = 6; y <= 7; y++) {
            b.add(new BlockEntry(-1, y, -1, Material.BLACK_CONCRETE));
            b.add(new BlockEntry( 1, y, -1, Material.BLACK_CONCRETE));
            b.add(new BlockEntry(-1, y,  1, Material.BLACK_CONCRETE));
            b.add(new BlockEntry( 1, y,  1, Material.BLACK_CONCRETE));
            b.add(new BlockEntry( 0, y, -1, Material.ORANGE_STAINED_GLASS));
            b.add(new BlockEntry( 0, y,  1, Material.ORANGE_STAINED_GLASS));
            b.add(new BlockEntry(-1, y,  0, Material.ORANGE_STAINED_GLASS));
            b.add(new BlockEntry( 1, y,  0, Material.ORANGE_STAINED_GLASS));
            b.add(new BlockEntry( 0, y,  0, Material.ORANGE_CONCRETE));
        }
        // Body Y=8
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                b.add(new BlockEntry(dx, 8, dz, corner(dx,dz) ? Material.BLACK_CONCRETE : Material.ORANGE_CONCRETE));
        // Upper metallic band Y=9
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                b.add(new BlockEntry(dx, 9, dz, Material.GRAY_CONCRETE));
        // Upper body Y=10..11
        for (int y = 10; y <= 11; y++)
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    b.add(new BlockEntry(dx, y, dz, corner(dx,dz) ? Material.BLACK_CONCRETE : Material.ORANGE_CONCRETE));
        // Nose cross Y=12
        b.add(new BlockEntry( 0, 12, -1, Material.BLACK_CONCRETE));
        b.add(new BlockEntry( 0, 12,  1, Material.BLACK_CONCRETE));
        b.add(new BlockEntry(-1, 12,  0, Material.BLACK_CONCRETE));
        b.add(new BlockEntry( 0, 12,  0, Material.BLACK_CONCRETE));
        b.add(new BlockEntry( 1, 12,  0, Material.BLACK_CONCRETE));
        // Tip Y=13
        b.add(new BlockEntry(0, 13, 0, Material.BLACK_CONCRETE));
        return b;
    }

    private static boolean corner(int dx, int dz) { return Math.abs(dx) == 1 && Math.abs(dz) == 1; }

    private record BlockEntry(int dx, int dy, int dz, Material material) {}

    private static final int DISPLAY_INTERVAL = 4; // ticks between display position packets

    private List<BlockDisplay> spawnRocketDisplays(Location base) {
        World w = base.getWorld();
        if (w == null) return List.of();
        List<BlockDisplay> displays = new ArrayList<>();
        for (BlockEntry e : getRocketBlocks()) {
            Location loc = base.clone().add(e.dx(), e.dy(), e.dz());
            BlockDisplay bd = w.spawn(loc, BlockDisplay.class);
            bd.setBlock(e.material().createBlockData());
            // Client interpolates smoothly between teleports: same visual quality, 4× fewer packets
            bd.setTeleportDuration(DISPLAY_INTERVAL);
            displays.add(bd);
        }
        return displays;
    }

    public boolean canPlaceRocket(Block base) {
        for (int i = 0; i <= 13; i++) {
            if (!base.getRelative(0, i, 0).getType().isAir()) return false;
        }
        return true;
    }

    public boolean isValidRocketStructure(Location base) {
        World w = base.getWorld();
        if (w == null) return false;
        Block b0 = w.getBlockAt(base);
        if (b0.getType() != Material.ORANGE_CONCRETE) return false;
        if (b0.getRelative(0, 4, 0).getType() != Material.GRAY_CONCRETE) return false;
        if (b0.getRelative(0, 13, 0).getType() != Material.BLACK_CONCRETE) return false;
        return b0.getRelative(-2, 0, -2).getType() == Material.BLACK_CONCRETE;
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

    // ── PDC: posizione overworld ──────────────────────────────────────────────

    private void saveOverworldPosition(Player player) {
        Location loc = player.getLocation();
        String data = loc.getWorld().getName() + ","
                + loc.getX() + "," + loc.getY() + "," + loc.getZ() + ","
                + loc.getYaw() + "," + loc.getPitch();
        player.getPersistentDataContainer().set(
                Cosmonaut.LAST_OVERWORLD_POS_KEY, PersistentDataType.STRING, data);
    }

    private Location getSavedOverworldLocation(Player player) {
        String data = player.getPersistentDataContainer()
                .get(Cosmonaut.LAST_OVERWORLD_POS_KEY, PersistentDataType.STRING);
        if (data == null) return null;
        String[] p = data.split(",");
        if (p.length < 4) return null;
        World world = Bukkit.getWorld(p[0]);
        if (world == null) return null;
        try {
            double x     = Double.parseDouble(p[1]);
            double y     = Double.parseDouble(p[2]);
            double z     = Double.parseDouble(p[3]);
            float  yaw   = p.length > 4 ? Float.parseFloat(p[4]) : 0f;
            float  pitch = p.length > 5 ? Float.parseFloat(p[5]) : 0f;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ── Launch ────────────────────────────────────────────────────────────────

    public void startLaunch(Player player, Rocket rocket) {
        if (inFlight.contains(player.getUniqueId())) return;
        inFlight.add(player.getUniqueId());

        boolean returning = plugin.getConfigManager()
                .getPlanetByWorld(player.getWorld().getName()) != null;

        Planet destination = null;
        if (!returning) {
            saveOverworldPosition(player);
            destination = plugin.getConfigManager().getPlanet(rocket.getDestinationPlanet());
        }

        final Planet finalDest = destination;

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
                    executeLaunch(player, rocket, finalDest, returning);
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

    private void executeLaunch(Player player, Rocket rocket, Planet destination, boolean returning) {
        GameMode originalMode = player.getGameMode();
        player.setGameMode(GameMode.SPECTATOR);
        player.setInvulnerable(true);

        Location base = rocket.getBaseLocation();
        World w = base.getWorld();
        if (w != null) {
            w.playSound(base, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.MASTER, 3f, 0.5f);
        }

        // Spawn visual displays before removing real blocks (no visual gap)
        final List<BlockDisplay> displays = spawnRocketDisplays(base);
        removeStructure(base);
        removeRocket(base);

        // Position player in the cockpit window area of the rocket
        Location playerPos = base.clone().add(0.5, 7.5, 0.5);
        player.teleport(playerPos);

        new BukkitRunnable() {
            int ticks = 0;
            double intervalAccum = 0; // Y distance accumulated since last display batch

            @Override
            public void run() {
                if (!player.isOnline()) {
                    displays.forEach(org.bukkit.entity.Entity::remove);
                    cancel();
                    inFlight.remove(player.getUniqueId());
                    return;
                }

                ticks++;
                double speed = 0.3 + ticks * 0.012;
                intervalAccum += speed;

                // Player: teleport every tick (1 cheap packet, keeps movement smooth)
                playerPos.add(0, speed, 0);
                player.teleport(playerPos);

                // Displays: batch update every DISPLAY_INTERVAL ticks.
                // setTeleportDuration() makes the client interpolate visually between
                // each teleport, so the rocket looks perfectly smooth despite fewer packets.
                // 120 entities × 25 updates = 3 000 packets instead of 12 000.
                if (ticks % DISPLAY_INTERVAL == 0) {
                    double dist = intervalAccum;
                    for (BlockDisplay bd : displays)
                        bd.teleport(bd.getLocation().add(0, dist, 0));
                    intervalAccum = 0;

                    // Exhaust particles piggybacked on the same interval (5 updates/sec)
                    Location exhaust = playerPos.clone().subtract(0, 9, 0);
                    World pw = exhaust.getWorld();
                    if (pw != null) {
                        pw.spawnParticle(Particle.FLAME, exhaust, 8, 0.8, 0.2, 0.8, 0.07);
                        pw.spawnParticle(Particle.SMOKE, exhaust, 5, 1.0, 0.3, 1.0, 0.04);
                    }
                }

                if (ticks >= 100) {
                    displays.forEach(org.bukkit.entity.Entity::remove);
                    cancel();
                    showBlackScreenAndTeleport(player, destination, originalMode, returning);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void showBlackScreenAndTeleport(Player player, Planet destination,
                                             GameMode originalMode, boolean returning) {
        player.showTitle(Title.title(
                Component.text("          ").color(NamedTextColor.BLACK),
                Component.empty(),
                Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(2500), Duration.ofMillis(700))
        ));

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) { inFlight.remove(player.getUniqueId()); return; }

            if (returning) {
                // Ritorno all'overworld: usa la posizione salvata nel PDC
                Location savedLoc = getSavedOverworldLocation(player);
                World overworld = savedLoc != null ? savedLoc.getWorld() : null;
                if (overworld == null) overworld = Bukkit.getWorld("world");
                if (overworld == null) {
                    overworld = Bukkit.getWorlds().stream()
                            .filter(wo -> plugin.getConfigManager().getPlanetByWorld(wo.getName()) == null)
                            .findFirst().orElse(null);
                }

                Location target = (savedLoc != null && savedLoc.getWorld() != null)
                        ? savedLoc : (overworld != null ? overworld.getSpawnLocation() : null);

                if (target != null) {
                    target.getWorld().loadChunk(target.getBlockX() >> 4, target.getBlockZ() >> 4);
                    player.teleport(target);
                }
                player.setGameMode(originalMode);
                player.setInvulnerable(false);
                player.showTitle(Title.title(
                        Component.text("Benvenuto sulla Terra").color(NamedTextColor.GREEN),
                        Component.empty(),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))
                ));

            } else {
                // Lancio verso un pianeta
                World dest = destination != null ? Bukkit.getWorld(destination.getWorldName()) : null;
                if (dest != null) {
                    player.teleport(dest.getSpawnLocation());
                    player.setGameMode(originalMode);
                    player.setInvulnerable(false);
                    player.showTitle(Title.title(
                            Component.text("Benvenuto su " + capitalize(destination.getName())).color(NamedTextColor.GOLD),
                            Component.empty(),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000))
                    ));
                } else {
                    player.setGameMode(originalMode);
                    player.setInvulnerable(false);
                    player.sendMessage(Component.text("Errore: mondo di destinazione non trovato!").color(NamedTextColor.RED));
                }
            }

            cooldowns.put(player.getUniqueId(),
                    System.currentTimeMillis() + plugin.getConfigManager().getLaunchCooldown() * 1000L);
            inFlight.remove(player.getUniqueId());
        }, 50L);
    }

    private void removeStructure(Location base) {
        World w = base.getWorld();
        if (w == null) return;
        Block b = w.getBlockAt(base);
        for (BlockEntry e : getRocketBlocks())
            b.getRelative(e.dx(), e.dy(), e.dz()).setType(Material.AIR);
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
