package com.italiarevenge.cosmonaut.listener;

import com.italiarevenge.cosmonaut.Cosmonaut;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

public class AtmosphereListener implements Listener {

    private final Cosmonaut plugin;

    public AtmosphereListener(Cosmonaut plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // RocketManager BukkitRunnable handles !player.isOnline() already
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        // Suppress drowning damage - we handle atmosphere damage manually
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.DROWNING) return;

        String worldName = player.getWorld().getName();
        if (plugin.getConfigManager().getPlanetByWorld(worldName) != null) {
            event.setCancelled(true); // We manage air depletion ourselves
        }
    }
}
