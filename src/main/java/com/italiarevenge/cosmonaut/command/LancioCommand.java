package com.italiarevenge.cosmonaut.command;

import com.italiarevenge.cosmonaut.Cosmonaut;
import com.italiarevenge.cosmonaut.model.Planet;
import com.italiarevenge.cosmonaut.model.Rocket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LancioCommand implements CommandExecutor {

    private final Cosmonaut plugin;

    public LancioCommand(Cosmonaut plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo i giocatori possono usare questo comando.");
            return true;
        }

        if (!player.hasPermission("cosmonaut.use")) {
            player.sendMessage(Component.text("Non hai il permesso per usare il razzo.").color(NamedTextColor.RED));
            return true;
        }

        if (plugin.getRocketManager().isInFlight(player.getUniqueId())) {
            player.sendMessage(Component.text("Sei gia' in volo!").color(NamedTextColor.RED));
            return true;
        }

        if (plugin.getRocketManager().isOnCooldown(player)) {
            long secs = plugin.getRocketManager().getCooldownSeconds(player);
            player.sendMessage(Component.text("Devi aspettare ancora " + secs + "s prima del prossimo lancio.")
                    .color(NamedTextColor.RED));
            return true;
        }

        // Se il giocatore è su un pianeta → cerca struttura nelle vicinanze per il ritorno
        boolean onPlanet = plugin.getConfigManager().getPlanetByWorld(player.getWorld().getName()) != null;
        if (onPlanet) {
            Location structure = findNearbyStructure(player.getLocation(), 15.0);
            if (structure == null) {
                player.sendMessage(Component.text("Costruisci la struttura razzo e usa /lancio.")
                        .color(NamedTextColor.RED));
                return true;
            }
            // Razzo temporaneo: non viene registrato, la destinazione è gestita da RocketManager
            Rocket returnRocket = new Rocket(structure, "world");
            plugin.getRocketManager().startLaunch(player, returnRocket);
            return true;
        }

        // Lancio normale dall'overworld
        Rocket rocket = plugin.getRocketManager().findNearestRocket(player.getLocation(), 15.0);
        if (rocket == null) {
            player.sendMessage(Component.text("Nessun razzo registrato nelle vicinanze.")
                    .color(NamedTextColor.RED)
                    .appendNewline()
                    .append(Component.text("Posiziona un Razzo e usa /cosmonaut setplanet <nome>.").color(NamedTextColor.GRAY)));
            return true;
        }

        if (rocket.getDestinationPlanet() == null || rocket.getDestinationPlanet().isEmpty()) {
            player.sendMessage(Component.text("Questo razzo non ha una destinazione impostata.")
                    .color(NamedTextColor.RED)
                    .appendNewline()
                    .append(Component.text("Usa /cosmonaut setplanet <nome>.").color(NamedTextColor.GRAY)));
            return true;
        }

        Planet destination = plugin.getConfigManager().getPlanet(rocket.getDestinationPlanet());
        if (destination == null) {
            player.sendMessage(Component.text("Pianeta destinazione '" + rocket.getDestinationPlanet()
                    + "' non trovato nella config.").color(NamedTextColor.RED));
            return true;
        }

        if (!plugin.getRocketManager().isValidRocketStructure(rocket.getBaseLocation())) {
            player.sendMessage(Component.text("La struttura del razzo e' stata distrutta o modificata!")
                    .color(NamedTextColor.RED));
            plugin.getRocketManager().removeRocket(rocket.getBaseLocation());
            return true;
        }

        plugin.getRocketManager().startLaunch(player, rocket);
        return true;
    }

    private Location findNearbyStructure(Location origin, double maxDist) {
        int r = (int) maxDist;
        double maxSq = maxDist * maxDist;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -5; dy <= 5; dy++) {
                    if (dx * dx + dz * dz > maxSq) continue;
                    Location check = origin.clone().add(dx, dy, dz);
                    if (plugin.getRocketManager().isValidRocketStructure(check)) return check;
                }
            }
        }
        return null;
    }
}
