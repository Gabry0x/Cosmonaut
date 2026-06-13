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

    private static final int BASE_SURFACE = 70;
    private static final int TERRAIN_AMP  = 24; // amplificato per le dune

    private PlanetNoise terrainNoise;
    private PlanetNoise detailNoise;
    private PlanetNoise canyonNoise;
    private PlanetNoise variantNoise;
    private PlanetNoise pillarNoise;

    private void init(long seed) {
        if (terrainNoise != null) return;
        terrainNoise = new PlanetNoise(seed);
        detailNoise  = new PlanetNoise(seed ^ 0xDEADBEEFL);
        canyonNoise  = new PlanetNoise(seed ^ 0xFEEDFACEL);
        variantNoise = new PlanetNoise(seed ^ 0x87654321L);
        pillarNoise  = new PlanetNoise(seed ^ 0xABCDEF01L);
    }

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random,
                               int chunkX, int chunkZ, @NotNull ChunkData data) {
        init(worldInfo.getSeed());
        int minY = worldInfo.getMinHeight();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int wx = chunkX * 16 + x;
                int wz = chunkZ * 16 + z;

                float t       = terrainNoise.fbm(wx * 0.005f, wz * 0.005f, 5, 2f, 0.5f);
                float d       = detailNoise.fbm(wx * 0.015f, wz * 0.015f, 3, 2f, 0.5f);
                float canyon  = canyonNoise.fbm(wx * 0.007f, wz * 0.007f, 3, 2f, 0.5f);

                int surfY = BASE_SURFACE + (int) ((t * 0.65f + d * 0.35f - 0.5f) * 2f * TERRAIN_AMP);

                // Canyon: dove il noise è basso, taglia in profondità
                if (canyon < 0.28f) {
                    int cut = (int) ((0.28f - canyon) / 0.28f * 22);
                    surfY -= cut;
                }

                data.setBlock(x, minY, z, Material.BEDROCK);
                for (int y = minY + 1; y <= surfY; y++) {
                    data.setBlock(x, y, z, pickBlock(wx, wz, y, surfY - y, surfY));
                }
            }
        }

        generateIronVeins(chunkX, chunkZ, data, worldInfo);
        generatePillars(chunkX, chunkZ, data, worldInfo);
    }

    private Material pickBlock(int wx, int wz, int y, int depth, int surfY) {
        float v = variantNoise.noise(wx * 0.08f + y * 0.015f, wz * 0.08f + y * 0.015f);

        if (depth == 0) {
            if (v < 0.40f) return Material.RED_SAND;
            if (v < 0.70f) return Material.RED_SANDSTONE;
            return Material.TERRACOTTA;
        }
        if (depth == 1) return v < 0.5f ? Material.RED_SANDSTONE : Material.RED_TERRACOTTA;
        if (depth == 2) return v < 0.5f ? Material.RED_TERRACOTTA : Material.TERRACOTTA;

        if (depth <= 10) {
            if (v < 0.20f) return Material.RED_TERRACOTTA;
            if (v < 0.40f) return Material.TERRACOTTA;
            if (v < 0.60f) return Material.BROWN_TERRACOTTA;
            if (v < 0.80f) return Material.ORANGE_TERRACOTTA;
            return Material.SMOOTH_SANDSTONE;
        }

        if (surfY - y > 28) {
            if (v < 0.40f) return Material.DEEPSLATE;
            if (v < 0.70f) return Material.TUFF;
            return Material.STONE;
        }

        return v < 0.55f ? Material.STONE : Material.TUFF;
    }

    private void generateIronVeins(int chunkX, int chunkZ, ChunkData data, WorldInfo worldInfo) {
        int minY = worldInfo.getMinHeight();
        Random r = new Random(worldInfo.getSeed() ^ (chunkX * 341873128712L) ^ (chunkZ * 132897987541L) ^ 0xFE01L);
        int count = 2 + r.nextInt(3);
        for (int i = 0; i < count; i++) {
            int vx = r.nextInt(16), vz = r.nextInt(16);
            int vy = minY + 5 + r.nextInt(BASE_SURFACE - minY - 10);
            Material mat = r.nextBoolean() ? Material.IRON_ORE : Material.RAW_IRON_BLOCK;
            int rad = 1 + r.nextInt(2);
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    for (int dy = -rad; dy <= rad; dy++) {
                        if (dx * dx + dy * dy + dz * dz > rad * rad) continue;
                        int bx = vx + dx, bz = vz + dz, by = vy + dy;
                        if (bx < 0 || bx > 15 || bz < 0 || bz > 15 || by <= minY) continue;
                        Material cur = data.getType(bx, by, bz);
                        if (cur == Material.STONE || cur == Material.TUFF
                                || cur == Material.TERRACOTTA || cur == Material.RED_TERRACOTTA
                                || cur == Material.SMOOTH_SANDSTONE) {
                            data.setBlock(bx, by, bz, mat);
                        }
                    }
                }
            }
        }
    }

    private void generatePillars(int chunkX, int chunkZ, ChunkData data, WorldInfo worldInfo) {
        Random r = new Random(worldInfo.getSeed() ^ (chunkX * 987654321L) ^ (chunkZ * 123456789L) ^ 0xBEEFL);
        int count = r.nextInt(3); // 0-2 pilastri per chunk
        for (int i = 0; i < count; i++) {
            int px = 1 + r.nextInt(14), pz = 1 + r.nextInt(14);
            int height = 5 + r.nextInt(11); // 5-15

            // Trova la superficie in questo punto
            int surfY = BASE_SURFACE + TERRAIN_AMP + 5;
            for (int y = surfY; y > worldInfo.getMinHeight(); y--) {
                if (data.getType(px, y, pz) != Material.AIR) { surfY = y; break; }
            }

            for (int h = 1; h <= height; h++) {
                int by = surfY + h;
                if (by >= worldInfo.getMaxHeight()) break;
                Material mat = h <= height / 2 ? Material.SMOOTH_RED_SANDSTONE : Material.CUT_RED_SANDSTONE;
                data.setBlock(px, by, pz, mat);
            }
        }
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(@NotNull WorldInfo worldInfo) {
        return new BiomeProvider() {
            @Override public @NotNull Biome getBiome(@NotNull WorldInfo wi, int x, int y, int z) { return Biome.THE_END; }
            @Override public @NotNull List<Biome> getBiomes(@NotNull WorldInfo wi) { return List.of(Biome.THE_END); }
        };
    }

    @Override public boolean shouldGenerateNoise()       { return false; }
    @Override public boolean shouldGenerateSurface()     { return false; }
    @Override public boolean shouldGenerateBedrock()     { return false; }
    @Override public boolean shouldGenerateDecorations() { return false; }
    @Override public boolean shouldGenerateMobs()        { return false; }
    @Override public boolean shouldGenerateStructures()  { return false; }
    @Override public boolean shouldGenerateCaves()       { return false; }
}
