package com.italiarevenge.cosmonaut.model;

public class Planet {

    private final String name;
    private final String worldName;
    private final double gravityMultiplier;
    private final boolean hasAtmosphere;

    public Planet(String name, String worldName, double gravityMultiplier, boolean hasAtmosphere) {
        this.name = name;
        this.worldName = worldName;
        this.gravityMultiplier = gravityMultiplier;
        this.hasAtmosphere = hasAtmosphere;
    }

    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public double getGravityMultiplier() { return gravityMultiplier; }
    public boolean hasAtmosphere() { return hasAtmosphere; }
}
