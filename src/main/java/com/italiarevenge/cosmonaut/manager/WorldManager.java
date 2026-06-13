package com.italiarevenge.cosmonaut.manager;

import com.italiarevenge.cosmonaut.Cosmonaut;
import com.italiarevenge.cosmonaut.generator.LunaGenerator;
import com.italiarevenge.cosmonaut.generator.MarteGenerator;
import com.italiarevenge.cosmonaut.model.Planet;
import org.bukkit.*;
import org.bukkit.entity.SpawnCategory;
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
        creator.environment(World.Environment.THE_END); // cielo nero/stellato senza resource pack
        if (generator != null) creator.generator(generator);

        World world = Bukkit.createWorld(creator);
        if (world == null) {
            plugin.getLogger().severe("Impossibile creare/caricare il mondo: " + worldName);
            return;
        }

        // Nessun mob spawn — impostato sempre, anche su mondo già esistente
        for (SpawnCategory cat : SpawnCategory.values()) {
            try { world.setSpawnLimit(cat, 0); } catch (Exception ignored) {}
        }

        if (!existsOnDisk) {
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
