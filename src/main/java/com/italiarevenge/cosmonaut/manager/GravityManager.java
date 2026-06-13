package com.italiarevenge.cosmonaut.manager;

import com.italiarevenge.cosmonaut.Cosmonaut;
import com.italiarevenge.cosmonaut.model.Planet;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class GravityManager {

    private final Cosmonaut plugin;
    private BukkitTask task;

    public GravityManager(Cosmonaut plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    @SuppressWarnings("deprecation")
    private void tick() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("cosmonaut.bypass")) continue;
            if (player.isFlying() || player.isOnGround()) continue;

            String worldName = player.getWorld().getName();
            Planet planet = plugin.getConfigManager().getPlanetByWorld(worldName);
            if (planet == null) continue;

            double counterforce = 0.08 * (1.0 - planet.getGravityMultiplier());
            Vector vel = player.getVelocity();
            vel.setY(vel.getY() + counterforce);
            player.setVelocity(vel);
        }
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
    }
}
