package com.italiarevenge.cosmonaut.manager;

import com.italiarevenge.cosmonaut.Cosmonaut;
import com.italiarevenge.cosmonaut.model.Planet;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AtmosphereManager {

    private final Cosmonaut plugin;
    private final List<PressurizedZone> zones = new ArrayList<>();
    private BukkitTask task;
    private File dataFile;
    private YamlConfiguration dataConfig;

    public AtmosphereManager(Cosmonaut plugin) {
        this.plugin = plugin;
    }

    public void loadData() {
        dataFile = new File(plugin.getDataFolder(), "zones.yml");
        if (!dataFile.exists()) { plugin.saveResource("zones.yml", false); }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        zones.clear();
        List<?> raw = dataConfig.getList("zones", List.of());
        for (Object obj : raw) {
            if (!(obj instanceof Map<?, ?> map)) continue;
            try {
                String world = (String) map.get("world");
                double x = ((Number) map.get("x")).doubleValue();
                double y = ((Number) map.get("y")).doubleValue();
                double z = ((Number) map.get("z")).doubleValue();
                zones.add(new PressurizedZone(world, x, y, z));
            } catch (Exception ignored) {}
        }
        plugin.getLogger().info("Caricate " + zones.size() + " zone pressurizzate.");
    }

    public void saveData() {
        if (dataFile == null) return;
        List<Map<String, Object>> list = new ArrayList<>();
        for (PressurizedZone z : zones) {
            list.add(Map.of("world", z.world, "x", z.x, "y", z.y, "z", z.z));
        }
        dataConfig.set("zones", list);
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public void addZone(Location loc) {
        zones.removeIf(z -> z.world.equals(loc.getWorld().getName())
                && z.distanceSq(loc) < 4);
        zones.add(new PressurizedZone(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ()));
        saveData();
    }

    public void removeZone(Location loc) {
        zones.removeIf(z -> z.world.equals(loc.getWorld().getName())
                && z.distanceSq(loc) < 4);
        saveData();
    }

    public boolean isInPressurizedZone(Player player, double radiusSq) {
        String worldName = player.getWorld().getName();
        for (PressurizedZone zone : zones) {
            if (!zone.world.equals(worldName)) continue;
            if (zone.distanceSq(player.getLocation()) <= radiusSq) return true;
        }
        return false;
    }

    public void startTask() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    private void tick() {
        double radiusSq = (double) plugin.getConfigManager().getPressurizedRadius();
        radiusSq *= radiusSq;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Planet planet = plugin.getConfigManager().getPlanetByWorld(player.getWorld().getName());
            if (planet == null || planet.hasAtmosphere()) continue;
            if (player.hasPermission("cosmonaut.bypass")) continue;
            if (isInPressurizedZone(player, radiusSq) || hasSpaceHelmet(player)) {
                restoreAir(player);
                continue;
            }
            drainAir(player);
        }
    }

    private void drainAir(Player player) {
        int remaining = player.getRemainingAir();
        if (remaining > 0) {
            player.setRemainingAir(Math.max(0, remaining - 15));
        } else {
            player.damage(2.0);
        }
    }

    private void restoreAir(Player player) {
        int max = player.getMaximumAir();
        if (player.getRemainingAir() < max) {
            player.setRemainingAir(Math.min(max, player.getRemainingAir() + 20));
        }
    }

    private boolean hasSpaceHelmet(Player player) {
        var helmet = player.getInventory().getHelmet();
        if (helmet == null || !helmet.hasItemMeta()) return false;
        return helmet.getItemMeta().getPersistentDataContainer()
                .has(Cosmonaut.SPACE_HELMET_KEY, PersistentDataType.BOOLEAN);
    }

    public List<PressurizedZone> getZones() { return zones; }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
    }

    public static class PressurizedZone {
        public final String world;
        public final double x, y, z;

        public PressurizedZone(String world, double x, double y, double z) {
            this.world = world; this.x = x; this.y = y; this.z = z;
        }

        public double distanceSq(Location loc) {
            double dx = loc.getX() - x, dy = loc.getY() - y, dz = loc.getZ() - z;
            return dx * dx + dy * dy + dz * dz;
        }
    }
}
