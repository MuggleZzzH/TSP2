package com.cpt204.finalproject.model;

import java.util.Objects;

/**
 * Represents an attraction located in a city.
 * Primarily used during data loading.
 */
public class Attraction {
    private final String attractionName;
    private final String cityName;

    public Attraction(String attractionName, String cityName) {
        if (attractionName == null || attractionName.trim().isEmpty() || 
            cityName == null || cityName.trim().isEmpty()) {
            throw new IllegalArgumentException("Attraction name and city name cannot be null or empty.");
        }
        this.attractionName = attractionName.trim();
        this.cityName = cityName.trim();
    }

    public String getAttractionName() {
        return attractionName;
    }

    public String getCityName() {
        return cityName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attraction that = (Attraction) o;
        return attractionName.equals(that.attractionName) &&
               cityName.equals(that.cityName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attractionName, cityName);
    }

    @Override
    public String toString() {
        return "Attraction{" +
               "attractionName='" + attractionName + '\'' +
               ", cityName='" + cityName + '\'' +
               '}';
    }
} 