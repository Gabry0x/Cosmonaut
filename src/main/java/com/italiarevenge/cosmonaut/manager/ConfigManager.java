package com.italiarevenge.cosmonaut.manager;

import com.italiarevenge.cosmonaut.Cosmonaut;
import com.italiarevenge.cosmonaut.model.Planet;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class ConfigManager {

    private final Cosmonaut plugin;
    private int launchCooldown;
    private int pressurizedRadius;
    private List<Planet> planets;

    public ConfigManager(Cosmonaut plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();
        launchCooldown = plugin.getConfig().getInt("launch-cooldown", 60);
        pressurizedRadius = plugin.getConfig().getInt("pressurized-radius", 10);

        planets = new ArrayList<>();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("planets");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                String worldName = sec.getString(key + ".world-name", key);
                double gravity = sec.getDouble(key + ".gravity-multiplier", 1.0);
                boolean hasAtmo = sec.getBoolean(key + ".has-atmosphere", true);
                planets.add(new Planet(key, worldName, gravity, hasAtmo));
            }
        }
    }

    public int getLaunchCooldown() { return launchCooldown; }
    public int getPressurizedRadius() { return pressurizedRadius; }
    public List<Planet> getPlanets() { return planets; }

    public Planet getPlanet(String name) {
        return planets.stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public Planet getPlanetByWorld(String worldName) {
        return planets.stream()
                .filter(p -> p.getWorldName().equalsIgnoreCase(worldName))
                .findFirst().orElse(null);
    }
}
