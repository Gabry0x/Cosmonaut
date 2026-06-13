package com.italiarevenge.cosmonaut.listener;

import com.italiarevenge.cosmonaut.Cosmonaut;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.DROWNING) return;
        if (plugin.getConfigManager().getPlanetByWorld(player.getWorld().getName()) != null) {
            event.setCancelled(true);
        }
    }

    // Blocca spawn naturale di creature (include mob e animali)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (plugin.getConfigManager().getPlanetByWorld(event.getLocation().getWorld().getName()) != null) {
            event.setCancelled(true);
        }
    }

    // Blocca spawn di entità viventi non coperte da CreatureSpawnEvent (es. Ender Dragon)
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntity() instanceof Player) return; // i giocatori usano eventi diversi
        if (!(event.getEntity() instanceof LivingEntity)) return;
        if (plugin.getConfigManager().getPlanetByWorld(event.getLocation().getWorld().getName()) != null) {
            event.setCancelled(true);
        }
    }
}
