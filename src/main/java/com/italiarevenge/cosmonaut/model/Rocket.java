package com.italiarevenge.cosmonaut.model;

import org.bukkit.Location;

public class Rocket {

    private final Location baseLocation;
    private String destinationPlanet;

    public Rocket(Location baseLocation, String destinationPlanet) {
        this.baseLocation = baseLocation.clone();
        this.destinationPlanet = destinationPlanet;
    }

    public Location getBaseLocation() { return baseLocation.clone(); }
    public String getDestinationPlanet() { return destinationPlanet; }
    public void setDestinationPlanet(String destinationPlanet) { this.destinationPlanet = destinationPlanet; }
}
