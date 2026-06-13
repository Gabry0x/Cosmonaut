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

    private static final int BASE_SURFACE    = 72;
    private static final int TERRAIN_AMP     = 18;
    private static final int DEEP_THRESHOLD  = 22; // depth below surface → switch to deepslate

    private PlanetNoise terrainNoise;
    private PlanetNoise detailNoise;
    private PlanetNoise caveNoise;
    private PlanetNoise variantNoise;

    private void init(long seed) {
        if (terrainNoise != null) return;
        terrainNoise = new PlanetNoise(seed);
        detailNoise  = new PlanetNoise(seed ^ 0xDEADBEEFL);
        caveNoise    = new PlanetNoise(seed ^ 0xCAFEBABEL);
        variantNoise = new PlanetNoise(seed ^ 0x1A2B3C4DL);
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

                float t  = terrainNoise.fbm(wx * 0.004f, wz * 0.004f, 5, 2f, 0.5f);
                float d  = detailNoise.fbm(wx * 0.012f, wz * 0.012f, 3, 2f, 0.5f);
                int surfY = BASE_SURFACE + (int) ((t * 0.7f + d * 0.3f - 0.5f) * 2f * TERRAIN_AMP);

                data.setBlock(x, minY, z, Material.BEDROCK);

                for (int y = minY + 1; y <= surfY; y++) {
                    // Cave carving
                    if (y > minY + 3 && y < surfY - 8) {
                        float cv = caveNoise.noise3D(wx * 0.04f, y * 0.06f, wz * 0.04f);
                        if (cv > 0.70f) continue; // air
                    }
                    data.setBlock(x, y, z, pickBlock(wx, wz, y, surfY - y, surfY, minY));
                }
            }
        }

        generateVeins(chunkX, chunkZ, data, worldInfo);
        carveCraters(chunkX, chunkZ, data, worldInfo);
        generateStalactites(chunkX, chunkZ, data, worldInfo);
    }

    private Material pickBlock(int wx, int wz, int y, int depth, int surfY, int minY) {
        float v = variantNoise.noise(wx * 0.09f + y * 0.02f, wz * 0.09f + y * 0.02f);

        if (depth == 0) {
            if (v < 0.40f) return Material.GRAY_CONCRETE;
            if (v < 0.75f) return Material.LIGHT_GRAY_CONCRETE;
            return Material.CALCITE;
        }
        if (depth == 1) return Material.GRAY_CONCRETE;
        if (depth == 2) return v < 0.5f ? Material.LIGHT_GRAY_CONCRETE : Material.SMOOTH_STONE;

        if (depth <= 10) {
            if (v < 0.25f) return Material.SMOOTH_STONE;
            if (v < 0.50f) return Material.STONE;
            if (v < 0.75f) return Material.COBBLESTONE;
            return Material.GRAVEL;
        }

        if (surfY - y > DEEP_THRESHOLD) {
            if (v < 0.40f) return Material.DEEPSLATE;
            if (v < 0.70f) return Material.COBBLED_DEEPSLATE;
            return Material.STONE;
        }

        return v < 0.60f ? Material.STONE : Material.COBBLESTONE;
    }

    private void generateVeins(int chunkX, int chunkZ, ChunkData data, WorldInfo worldInfo) {
        int minY = worldInfo.getMinHeight();
        Random r = new Random(worldInfo.getSeed() ^ (chunkX * 341873128712L) ^ (chunkZ * 132897987541L));
        int count = 3 + r.nextInt(4);
        for (int i = 0; i < count; i++) {
            int vx = r.nextInt(16), vz = r.nextInt(16);
            int vy = minY + 5 + r.nextInt(BASE_SURFACE - minY - 10);
            Material mat = r.nextBoolean() ? Material.CALCITE : Material.TUFF;
            int rad = 1 + r.nextInt(2);
            placeSphere(data, vx, vy, vz, rad, mat, minY,
                    new Material[]{Material.STONE, Material.DEEPSLATE, Material.COBBLED_DEEPSLATE, Material.COBBLESTONE, Material.SMOOTH_STONE});
        }
    }

    private void carveCraters(int chunkX, int chunkZ, ChunkData data, WorldInfo worldInfo) {
        int minY = worldInfo.getMinHeight();
        for (int cx = chunkX - 2; cx <= chunkX + 2; cx++) {
            for (int cz = chunkZ - 2; cz <= chunkZ + 2; cz++) {
                Random r = new Random(worldInfo.getSeed() ^ (cx * 341873128712L) ^ (cz * 132897987541L) ^ 0xC4A7L);
                if (r.nextInt(8) != 0) continue;

                int crWX = cx * 16 + r.nextInt(16);
                int crWZ = cz * 16 + r.nextInt(16);
                int radius = 4 + r.nextInt(7);

                float t = terrainNoise.fbm(crWX * 0.004f, crWZ * 0.004f, 5, 2f, 0.5f);
                float d = detailNoise.fbm(crWX * 0.012f, crWZ * 0.012f, 3, 2f, 0.5f);
                int crSurfY = BASE_SURFACE + (int) ((t * 0.7f + d * 0.3f - 0.5f) * 2f * TERRAIN_AMP);
                int crCY = crSurfY - radius / 2;

                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        int wx = chunkX * 16 + x;
                        int wz = chunkZ * 16 + z;
                        int dx = wx - crWX, dz = wz - crWZ;
                        double hDist = Math.sqrt((double)(dx * dx + dz * dz));
                        if (hDist > radius + 2) continue;

                        for (int y = crSurfY + 2; y >= Math.max(minY + 1, crCY - radius); y--) {
                            double dy = y - crCY;
                            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                            if (dist < radius - 0.5) {
                                data.setBlock(x, y, z, Material.AIR);
                            } else if (dist < radius + 1.5 && hDist >= radius - 1.5) {
                                Material cur = data.getType(x, y, z);
                                if (cur != Material.AIR && cur != Material.BEDROCK) {
                                    data.setBlock(x, y, z, Material.LIGHT_GRAY_CONCRETE_POWDER);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void generateStalactites(int chunkX, int chunkZ, ChunkData data, WorldInfo worldInfo) {
        int minY = worldInfo.getMinHeight();
        Random r = new Random(worldInfo.getSeed() ^ (chunkX * 987654321L) ^ (chunkZ * 123456789L));
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = r.nextInt(16), z = r.nextInt(16);
            for (int y = BASE_SURFACE + TERRAIN_AMP - 5; y > minY + 5; y--) {
                if (data.getType(x, y, z) != Material.AIR) continue;
                if (data.getType(x, y + 1, z) == Material.AIR) continue;
                // Cave ceiling found at y+1 — hang stalactite downward
                int len = 1 + r.nextInt(4);
                for (int i = 0; i < len && y - i > minY; i++) {
                    if (data.getType(x, y - i, z) != Material.AIR) break;
                    data.setBlock(x, y - i, z, Material.DRIPSTONE_BLOCK);
                }
                break;
            }
        }
    }

    private void placeSphere(ChunkData data, int cx, int cy, int cz, int rad, Material mat,
                              int minY, Material[] replaceOnly) {
        for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
                for (int dy = -rad; dy <= rad; dy++) {
                    if (dx * dx + dy * dy + dz * dz > rad * rad) continue;
                    int bx = cx + dx, by = cy + dy, bz = cz + dz;
                    if (bx < 0 || bx > 15 || bz < 0 || bz > 15 || by <= minY) continue;
                    for (Material m : replaceOnly) {
                        if (data.getType(bx, by, bz) == m) {
                            data.setBlock(bx, by, bz, mat);
                            break;
                        }
                    }
                }
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
