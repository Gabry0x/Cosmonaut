package com.italiarevenge.cosmonaut.listener;

import com.italiarevenge.cosmonaut.Cosmonaut;
import com.italiarevenge.cosmonaut.manager.AtmosphereManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class RocketListener implements Listener {

    private final Cosmonaut plugin;

    public RocketListener(Cosmonaut plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!plugin.getRocketManager().isRocketItem(item)) return;

        event.setCancelled(true);

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Block base = clicked.getRelative(0, 1, 0);
        if (!plugin.getRocketManager().canPlaceRocket(base)) {
            event.getPlayer().sendMessage(
                    Component.text("Non c'e' abbastanza spazio per posizionare il razzo!").color(NamedTextColor.RED));
            return;
        }

        plugin.getRocketManager().placeRocketStructure(base);

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            event.getPlayer().getInventory().setItemInMainHand(null);
        }

        event.getPlayer().sendMessage(
                Component.text("Razzo posizionato! Usa ").color(NamedTextColor.GREEN)
                        .append(Component.text("/cosmonaut setplanet <nome>").color(NamedTextColor.YELLOW))
                        .append(Component.text(" per impostare la destinazione, poi ").color(NamedTextColor.GREEN))
                        .append(Component.text("/lancio").color(NamedTextColor.YELLOW))
                        .append(Component.text(" per partire.").color(NamedTextColor.GREEN))
        );
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (!item.hasItemMeta()) return;
        if (!item.getItemMeta().getPersistentDataContainer()
                .has(Cosmonaut.PRESSURIZER_KEY, PersistentDataType.BOOLEAN)) return;

        Location loc = event.getBlock().getLocation();
        plugin.getAtmosphereManager().addZone(loc);
        event.getPlayer().sendMessage(
                Component.text("Zona pressurizzata creata (raggio: "
                        + plugin.getConfigManager().getPressurizedRadius() + " blocchi).")
                        .color(NamedTextColor.AQUA));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        boolean removed = false;
        for (AtmosphereManager.PressurizedZone zone : plugin.getAtmosphereManager().getZones()) {
            if (zone.world.equals(loc.getWorld().getName()) && zone.distanceSq(loc) < 1) {
                removed = true;
                break;
            }
        }
        if (removed) {
            plugin.getAtmosphereManager().removeZone(loc);
            event.getPlayer().sendMessage(
                    Component.text("Zona pressurizzata rimossa.").color(NamedTextColor.YELLOW));
        }
    }
}
