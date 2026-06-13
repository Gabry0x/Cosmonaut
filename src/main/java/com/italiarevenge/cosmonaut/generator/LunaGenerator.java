package com.italiarevenge.cosmonaut.generator;

import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Random;

public class LunaGenerator extends ChunkGenerator {

    private static final int SURFACE_Y = 64;

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random,
                               int chunkX, int chunkZ, @NotNull ChunkData chunkData) {
        int minY = worldInfo.getMinHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                chunkData.setBlock(x, minY, z, Material.BEDROCK);
                for (int y = minY + 1; y < SURFACE_Y - 3; y++) {
                    chunkData.setBlock(x, y, z, Material.STONE);
                }
                for (int y = SURFACE_Y - 3; y <= SURFACE_Y; y++) {
                    chunkData.setBlock(x, y, z, Material.GRAY_CONCRETE);
                }
            }
        }

        carveCraters(chunkX, chunkZ, chunkData, minY);
    }

    private void carveCraters(int chunkX, int chunkZ, ChunkData chunkData, int minY) {
        for (int cx = chunkX - 1; cx <= chunkX + 1; cx++) {
            for (int cz = chunkZ - 1; cz <= chunkZ + 1; cz++) {
                Random r = new Random(cx * 341873128712L + cz * 132897987541L);
                if (r.nextInt(7) != 0) continue;

                int craterX = cx * 16 + r.nextInt(16);
                int craterZ = cz * 16 + r.nextInt(16);
                int radius = 3 + r.nextInt(5);

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int worldX = chunkX * 16 + x;
                        int worldZ = chunkZ * 16 + z;
                        int dx = worldX - craterX;
                        int dz = worldZ - craterZ;

                        if (dx * dx + dz * dz > radius * radius) continue;

                        for (int y = SURFACE_Y; y >= Math.max(minY + 1, SURFACE_Y - radius - 2); y--) {
                            double dy = y - (SURFACE_Y - radius / 2.0);
                            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                            if (dist <= radius) {
                                chunkData.setBlock(x, y, z, Material.AIR);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return new BiomeProvider() {
            @Override
            public @NotNull Biome getBiome(@NotNull WorldInfo wi, int x, int y, int z) {
                return Biome.THE_VOID;
            }

            @Override
            public @NotNull List<Biome> getBiomes(@NotNull WorldInfo wi) {
                return List.of(Biome.THE_VOID);
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
