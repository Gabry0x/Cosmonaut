package com.italiarevenge.cosmonaut.generator;

public class PlanetNoise {

    private final long seed;

    public PlanetNoise(long seed) {
        this.seed = seed;
    }

    public float noise(float x, float z) {
        int ix = (int) Math.floor(x);
        int iz = (int) Math.floor(z);
        float fx = x - ix;
        float fz = z - iz;

        float ux = smoothstep(fx);
        float uz = smoothstep(fz);

        float v00 = hash(ix,     iz    );
        float v10 = hash(ix + 1, iz    );
        float v01 = hash(ix,     iz + 1);
        float v11 = hash(ix + 1, iz + 1);

        return lerp(lerp(v00, v10, ux), lerp(v01, v11, ux), uz);
    }

    public float fbm(float x, float z, int octaves, float lacunarity, float gain) {
        float value = 0f;
        float amplitude = 0.5f;
        float frequency = 1f;
        float maxValue = 0f;
        for (int i = 0; i < octaves; i++) {
            value    += noise(x * frequency, z * frequency) * amplitude;
            maxValue += amplitude;
            amplitude  *= gain;
            frequency  *= lacunarity;
        }
        return value / maxValue;
    }

    // Pseudo-3D: combines two 2D slices shifted by y
    public float noise3D(float x, float y, float z) {
        float n1 = noise(x, z);
        float n2 = noise(x + y * 0.7f, z + y * 0.7f);
        return (n1 + n2) * 0.5f;
    }

    private float hash(int x, int z) {
        long h = seed ^ ((long) x * 374761393L) ^ ((long) z * 668265263L);
        h = (h ^ (h >> 13)) * 1274126177L;
        h = h ^ (h >> 16);
        return (float) ((h & 0xFFFFL) / 65536.0);
    }

    private float smoothstep(float t) {
        return t * t * (3f - 2f * t);
    }

    private float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }
}
