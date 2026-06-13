package com.italiarevenge.cosmonaut.command;

import com.italiarevenge.cosmonaut.Cosmonaut;
import com.italiarevenge.cosmonaut.model.Planet;
import com.italiarevenge.cosmonaut.model.Rocket;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CosmonautCommand implements CommandExecutor, TabCompleter {

    private final Cosmonaut plugin;

    public CosmonautCommand(Cosmonaut plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list" -> cmdList(sender);
            case "reload" -> {
                if (!sender.hasPermission("cosmonaut.admin")) { noPermission(sender); return true; }
                plugin.getConfigManager().load();
                sender.sendMessage(Component.text("Config ricaricata.").color(NamedTextColor.GREEN));
            }
            case "tp" -> {
                if (!sender.hasPermission("cosmonaut.admin")) { noPermission(sender); return true; }
                if (!(sender instanceof Player player)) { senderMustBePlayer(sender); return true; }
                if (args.length < 2) { usage(sender, "/cosmonaut tp <pianeta>"); return true; }
                cmdTp(player, args[1]);
            }
            case "setplanet" -> {
                if (!sender.hasPermission("cosmonaut.admin")) { noPermission(sender); return true; }
                if (!(sender instanceof Player player)) { senderMustBePlayer(sender); return true; }
                if (args.length < 2) { usage(sender, "/cosmonaut setplanet <nome>"); return true; }
                cmdSetPlanet(player, args[1]);
            }
            case "give" -> {
                if (!sender.hasPermission("cosmonaut.admin")) { noPermission(sender); return true; }
                if (!(sender instanceof Player player)) { senderMustBePlayer(sender); return true; }
                if (args.length < 2) { usage(sender, "/cosmonaut give <rocket|helmet|pressurizer>"); return true; }
                cmdGive(player, args[1]);
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void cmdList(CommandSender sender) {
        List<Planet> planets = plugin.getConfigManager().getPlanets();
        if (planets.isEmpty()) {
            sender.sendMessage(Component.text("Nessun pianeta configurato.").color(NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text("=== Pianeti disponibili ===").color(NamedTextColor.GOLD));
        for (Planet p : planets) {
            World w = Bukkit.getWorld(p.getWorldName());
            String status = w != null ? "caricato" : "non caricato";
            sender.sendMessage(Component.text("  • ").color(NamedTextColor.GRAY)
                    .append(Component.text(p.getName()).color(NamedTextColor.AQUA))
                    .append(Component.text(" [" + p.getWorldName() + "] (" + status + ")").color(NamedTextColor.GRAY))
                    .append(Component.text(" gravita'=" + p.getGravityMultiplier()).color(NamedTextColor.DARK_GRAY))
                    .append(Component.text(p.hasAtmosphere() ? " atm" : " no-atm").color(
                            p.hasAtmosphere() ? NamedTextColor.GREEN : NamedTextColor.RED)));
        }
    }

    private void cmdTp(Player player, String planetName) {
        Planet planet = plugin.getConfigManager().getPlanet(planetName);
        if (planet == null) {
            player.sendMessage(Component.text("Pianeta '" + planetName + "' non trovato.").color(NamedTextColor.RED));
            return;
        }
        World world = Bukkit.getWorld(planet.getWorldName());
        if (world == null) {
            player.sendMessage(Component.text("Il mondo '" + planet.getWorldName() + "' non e' caricato.").color(NamedTextColor.RED));
            return;
        }
        player.teleport(world.getSpawnLocation());
        player.sendMessage(Component.text("Teletrasportato su " + planet.getName() + ".").color(NamedTextColor.GREEN));
    }

    private void cmdSetPlanet(Player player, String planetName) {
        Planet planet = plugin.getConfigManager().getPlanet(planetName);
        if (planet == null) {
            player.sendMessage(Component.text("Pianeta '" + planetName + "' non trovato. Usa /cosmonaut list.").color(NamedTextColor.RED));
            return;
        }

        Rocket rocket = plugin.getRocketManager().findNearestRocket(player.getLocation(), 15.0);
        if (rocket == null) {
            // Try to auto-detect an unregistered structure nearby
            if (!tryAutoRegisterRocket(player, planetName)) {
                player.sendMessage(Component.text("Nessun razzo trovato entro 15 blocchi.").color(NamedTextColor.RED));
            }
            return;
        }

        rocket.setDestinationPlanet(planetName);
        plugin.getRocketManager().saveData();
        player.sendMessage(Component.text("Destinazione del razzo impostata su: ")
                .color(NamedTextColor.GREEN)
                .append(Component.text(planetName).color(NamedTextColor.GOLD)));
    }

    private boolean tryAutoRegisterRocket(Player player, String planetName) {
        org.bukkit.Location loc = player.getLocation();
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                for (int dy = -5; dy <= 5; dy++) {
                    org.bukkit.Location check = loc.clone().add(dx, dy, dz);
                    if (plugin.getRocketManager().isValidRocketStructure(check)) {
                        plugin.getRocketManager().registerRocket(check, planetName);
                        player.sendMessage(Component.text("Razzo trovato e registrato! Destinazione: ")
                                .color(NamedTextColor.GREEN)
                                .append(Component.text(planetName).color(NamedTextColor.GOLD)));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void cmdGive(Player player, String type) {
        switch (type.toLowerCase()) {
            case "rocket" -> {
                player.getInventory().addItem(plugin.getRocketManager().createRocketItem());
                player.sendMessage(Component.text("Ricevuto: Razzo").color(NamedTextColor.GREEN));
            }
            case "helmet" -> {
                player.getInventory().addItem(plugin.getRocketManager().createSpaceHelmet());
                player.sendMessage(Component.text("Ricevuto: Casco Spaziale").color(NamedTextColor.GREEN));
            }
            case "pressurizer" -> {
                player.getInventory().addItem(plugin.getRocketManager().createPressurizerItem());
                player.sendMessage(Component.text("Ricevuto: Generatore di Pressione").color(NamedTextColor.GREEN));
            }
            default -> usage(player, "/cosmonaut give <rocket|helmet|pressurizer>");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== Cosmonaut ===").color(NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/cosmonaut list").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Lista pianeti").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/cosmonaut setplanet <nome>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Imposta destinazione razzo piu' vicino").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/cosmonaut tp <pianeta>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Teletrasporto admin").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/cosmonaut give <rocket|helmet|pressurizer>").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Dai item speciali").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/cosmonaut reload").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Ricarica config").color(NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/lancio").color(NamedTextColor.YELLOW)
                .append(Component.text(" - Avvia sequenza di lancio").color(NamedTextColor.GRAY)));
    }

    private void noPermission(CommandSender sender) {
        sender.sendMessage(Component.text("Non hai il permesso.").color(NamedTextColor.RED));
    }

    private void senderMustBePlayer(CommandSender sender) {
        sender.sendMessage("Solo i giocatori possono usare questo comando.");
    }

    private void usage(CommandSender sender, String usage) {
        sender.sendMessage(Component.text("Uso: " + usage).color(NamedTextColor.RED));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("list", "setplanet", "tp", "reload", "give");
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "setplanet", "tp" -> plugin.getConfigManager().getPlanets()
                        .stream().map(Planet::getName).toList();
                case "give" -> List.of("rocket", "helmet", "pressurizer");
                default -> List.of();
            };
        }
        return List.of();
    }
}
