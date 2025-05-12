package com.cpt204.finalproject.model;

import java.util.Objects;

/**
 * Represents a directed road segment between two cities.
 */
public class Road {
    private final City source;
    private final City destination;
    private final double distance;

    /**
     * Constructs a new Road segment.
     * @param source The starting city. Cannot be null.
     * @param destination The ending city. Cannot be null.
     * @param distance The distance of the road. Must be non-negative.
     */
    public Road(City source, City destination, double distance) {
        if (source == null || destination == null) {
            throw new IllegalArgumentException("Source and destination cities cannot be null.");
        }
        if (distance < 0) {
            throw new IllegalArgumentException("Distance cannot be negative.");
        }
        this.source = source;
        this.destination = destination;
        this.distance = distance;
    }

    public City getSource() {
        return source;
    }

    public City getDestination() {
        return destination;
    }

    public double getDistance() {
        return distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Road road = (Road) o;
        return Double.compare(road.distance, distance) == 0 &&
               source.equals(road.source) &&
               destination.equals(road.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, destination, distance);
    }

    @Override
    public String toString() {
        return "Road{" +
               "source=" + source.getName() +
               ", destination=" + destination.getName() +
               ", distance=" + distance +
               '}';
    }
} 