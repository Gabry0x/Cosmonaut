package com.italiarevenge.cosmonaut;

import com.italiarevenge.cosmonaut.command.CosmonautCommand;
import com.italiarevenge.cosmonaut.command.LancioCommand;
import com.italiarevenge.cosmonaut.listener.AtmosphereListener;
import com.italiarevenge.cosmonaut.listener.RocketListener;
import com.italiarevenge.cosmonaut.manager.*;
import org.bukkit.NamespacedKey;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Cosmonaut extends JavaPlugin {

    private static Cosmonaut instance;

    private ConfigManager configManager;
    private WorldManager worldManager;
    private RocketManager rocketManager;
    private AtmosphereManager atmosphereManager;
    private GravityManager gravityManager;

    public static NamespacedKey ROCKET_ITEM_KEY;
    public static NamespacedKey SPACE_HELMET_KEY;
    public static NamespacedKey PRESSURIZER_KEY;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        ROCKET_ITEM_KEY  = new NamespacedKey(this, "cosmonaut_rocket");
        SPACE_HELMET_KEY = new NamespacedKey(this, "space_helmet");
        PRESSURIZER_KEY  = new NamespacedKey(this, "pressurizer");

        configManager    = new ConfigManager(this);
        worldManager     = new WorldManager(this);
        rocketManager    = new RocketManager(this);
        atmosphereManager = new AtmosphereManager(this);
        gravityManager   = new GravityManager(this);

        worldManager.loadWorlds();
        rocketManager.loadData();
        atmosphereManager.loadData();

        atmosphereManager.startTask();
        gravityManager.startTask();

        getServer().getPluginManager().registerEvents(new RocketListener(this), this);
        getServer().getPluginManager().registerEvents(new AtmosphereListener(this), this);

        rocketManager.registerRecipe();

        getCommand("cosmonaut").setExecutor(new CosmonautCommand(this));
        getCommand("lancio").setExecutor(new LancioCommand(this));

        getLogger().info("Cosmonaut avviato!");
    }

    @Override
    public void onDisable() {
        if (rocketManager != null) rocketManager.saveData();
        if (atmosphereManager != null) atmosphereManager.saveData();
        getLogger().info("Cosmonaut disabilitato.");
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return worldManager != null ? worldManager.getGenerator(worldName) : null;
    }

    public static Cosmonaut getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public WorldManager getWorldManager() { return worldManager; }
    public RocketManager getRocketManager() { return rocketManager; }
    public AtmosphereManager getAtmosphereManager() { return atmosphereManager; }
    public GravityManager getGravityManager() { return gravityManager; }
}
