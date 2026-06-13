package com.italiarevenge.cosmonaut.manager;

import com.italiarevenge.cosmonaut.Cosmonaut;
import com.italiarevenge.cosmonaut.model.Planet;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final Cosmonaut plugin;
    private int launchCooldown;
    private int pressurizedRadius;
    private List<Planet> planets;
    private final Map<String, Planet> worldPlanetMap = new HashMap<>();
    private final Map<String, Planet> namePlanetMap  = new HashMap<>();

    public ConfigManager(Cosmonaut plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();
        launchCooldown    = plugin.getConfig().getInt("launch-cooldown", 60);
        pressurizedRadius = plugin.getConfig().getInt("pressurized-radius", 10);

        planets = new ArrayList<>();
        worldPlanetMap.clear();
        namePlanetMap.clear();

        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("planets");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                String worldName = sec.getString(key + ".world-name", key);
                double gravity   = sec.getDouble(key + ".gravity-multiplier", 1.0);
                boolean hasAtmo  = sec.getBoolean(key + ".has-atmosphere", true);
                Planet planet    = new Planet(key, worldName, gravity, hasAtmo);
                planets.add(planet);
                worldPlanetMap.put(worldName.toLowerCase(), planet);
                namePlanetMap.put(key.toLowerCase(), planet);
            }
        }
    }

    public int getLaunchCooldown()    { return launchCooldown; }
    public int getPressurizedRadius() { return pressurizedRadius; }
    public List<Planet> getPlanets()  { return planets; }

    public Planet getPlanet(String name) {
        return namePlanetMap.get(name.toLowerCase());
    }

    public Planet getPlanetByWorld(String worldName) {
        return worldPlanetMap.get(worldName.toLowerCase());
    }
}
