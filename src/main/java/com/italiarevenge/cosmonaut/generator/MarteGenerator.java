package com.italiarevenge.cosmonaut.generator;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public class MarteGenerator extends ChunkGenerator {

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random,
                               int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        int minY = worldInfo.getMinHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkX * 16 + x;
                int worldZ = chunkZ * 16 + z;
                int surfaceY = getDuneHeight(worldX, worldZ);

                chunkData.setBlock(x, minY, z, Material.BEDROCK);
                for (int y = minY + 1; y < surfaceY - 3; y++) {
                    chunkData.setBlock(x, y, z, Material.RED_TERRACOTTA);
                }
                for (int y = Math.max(minY + 1, surfaceY - 3); y < surfaceY; y++) {
                    chunkData.setBlock(x, y, z, Material.TERRACOTTA);
                }
                chunkData.setBlock(x, surfaceY, z, Material.RED_SAND);
            }
        }
    }

    private int getDuneHeight(int worldX, int worldZ) {
        double dx = worldX * 0.04;
        double dz = worldZ * 0.04;
        double noise = Math.sin(dx) * Math.cos(dz * 0.8) * 3.5
                + Math.cos(dx * 1.4) * Math.sin(dz * 1.1) * 2.5
                + Math.sin(dx * 0.5 + dz * 0.5) * 1.5;
        return 64 + (int) Math.round(noise);
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return new BiomeProvider() {
            @Override
            public @NotNull Biome getBiome(@NotNull WorldInfo wi, int x, int y, int z) {
                return Biome.NETHER_WASTES;
            }

            @Override
            public @NotNull List<Biome> getBiomes(@NotNull WorldInfo wi) {
                return List.of(Biome.NETHER_WASTES);
            }
        };
    }

    @Override public boolean shouldGenerateNoise() { return false; }
    @Override public boolean shouldGenerateSurface() { return false; }
    @Override public boolean shouldGenerateBedrock() { return false; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateMobs() { return false; }
    @Override public boolean shouldGenerateStructures() { return false; }
    @Override public boolean shouldGenerateCaves() { return false; }
}
