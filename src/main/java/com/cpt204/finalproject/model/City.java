package com.cpt204.finalproject.model;

import java.util.Objects;

/**
 * Represents a city in the road network.
 * Each city has a unique name.
 */
public class City {
    private final String name;

    /**
     * Constructs a new City.
     * @param name The unique name of the city. Cannot be null or empty.
     */
    public City(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("City name cannot be null or empty.");
        }
        this.name = name.trim();
    }

    /**
     * Gets the name of the city.
     * @return The city name.
     */
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        City city = (City) o;
        return name.equals(city.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "City{" +
               "name='" + name + '\'' +
               '}';
    }
} 