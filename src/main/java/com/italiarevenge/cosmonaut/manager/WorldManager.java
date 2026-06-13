package com.italiarevenge.cosmonaut.manager;

import com.italiarevenge.cosmonaut.Cosmonaut;
import com.italiarevenge.cosmonaut.generator.LunaGenerator;
import com.italiarevenge.cosmonaut.generator.MarteGenerator;
import com.italiarevenge.cosmonaut.model.Planet;
import org.bukkit.*;
import org.bukkit.generator.ChunkGenerator;

import java.io.File;

public class WorldManager {

    private final Cosmonaut plugin;

    public WorldManager(Cosmonaut plugin) {
        this.plugin = plugin;
    }

    public void loadWorlds() {
        for (Planet planet : plugin.getConfigManager().getPlanets()) {
            loadOrCreateWorld(planet);
        }
    }

    private void loadOrCreateWorld(Planet planet) {
        String worldName = planet.getWorldName();

        // Guard: already loaded in memory — nothing to do
        if (Bukkit.getWorld(worldName) != null) return;

        // Check disk before calling createWorld, so we know whether to init defaults
        File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
        boolean existsOnDisk = worldFolder.isDirectory();

        ChunkGenerator generator = getGenerator(worldName);
        WorldCreator creator = new WorldCreator(worldName);
        creator.environment(World.Environment.NORMAL);
        if (generator != null) creator.generator(generator);

        // createWorld loads an existing world from disk without overwriting chunks;
        // for a brand-new world it generates it from scratch.
        World world = Bukkit.createWorld(creator);
        if (world == null) {
            plugin.getLogger().severe("Impossibile creare/caricare il mondo: " + worldName);
            return;
        }

        if (!existsOnDisk) {
            // First-time creation only — never touch an already-existing world's settings
            world.setDifficulty(Difficulty.NORMAL);
            world.setTime(6000);
            world.setSpawnLocation(0, 70, 0);
            plugin.getLogger().info("Mondo '" + worldName + "' creato ex-novo.");
        } else {
            plugin.getLogger().info("Mondo '" + worldName + "' caricato da disco (chunk esistenti intatti).");
        }
    }

    public ChunkGenerator getGenerator(String worldName) {
        return switch (worldName.toLowerCase()) {
            case "luna" -> new LunaGenerator();
            case "marte" -> new MarteGenerator();
            default -> null;
        };
    }
}
