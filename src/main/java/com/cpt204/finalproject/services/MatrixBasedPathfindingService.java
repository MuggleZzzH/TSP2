package com.cpt204.finalproject.services;

import com.cpt204.finalproject.model.Attraction;
import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.model.RoadNetwork;

import java.util.Collections;
import java.util.List;

/**
 * An implementation of PathfindingService that relies on precomputed shortest path information
 * provided by a ShortestPathInfo object (computed using Floyd-Warshall or similar).
 * This service provides very fast lookups for shortest paths and distances.
 */
public class MatrixBasedPathfindingService implements PathfindingService {

    private static final String ALGORITHM_NAME = "Precomputed (Floyd-Warshall)";
    private final ShortestPathInfo shortestPathInfo;

    /**
     * Constructs a MatrixBasedPathfindingService.
     *
     * @param shortestPathInfo An instance of ShortestPathInfo where all-pairs paths
     *                         have already been computed.
     */
    public MatrixBasedPathfindingService(ShortestPathInfo shortestPathInfo) {
        if (shortestPathInfo == null) {
            throw new IllegalArgumentException("ShortestPathInfo cannot be null.");
        }
        // We could add a check here: if (!shortestPathInfo.isComputed()) { throw ... }
        // But maybe allow lazy computation? For now, assume it's computed.
        this.shortestPathInfo = shortestPathInfo;
    }

    @Override
    public PathResult findShortestPath(
            RoadNetwork roadNetwork, // Not directly used, info comes from shortestPathInfo
            City startCity,
            City endCity,
            List<Attraction> attractionsToVisit, // Intermediate attractions not handled by direct path lookup
            boolean useTimeout, // Timeout not applicable to precomputed lookup
            long timeoutMillis) {
        
        long startTime = System.nanoTime(); // Use nanoTime for potentially very fast lookups
        
        if (startCity == null || endCity == null) {
             return PathResult.empty(ALGORITHM_NAME); // Or throw exception
        }

        // Basic Dijkstra service ignored attractions, this one does too for direct path lookup
        if (attractionsToVisit != null && !attractionsToVisit.isEmpty()) {
            System.out.println("Warning: MatrixBasedPathfindingService does not process intermediate attractions. Finding direct precomputed path.");
        }

        double distance = shortestPathInfo.getShortestDistance(startCity, endCity);
        List<City> path = shortestPathInfo.reconstructPath(startCity, endCity);
        
        long endTime = System.nanoTime();
        long durationMillis = (endTime - startTime) / 1_000_000; // Convert nanoseconds to milliseconds

        if (distance == Double.POSITIVE_INFINITY || path.isEmpty()) {
            // Check path emptiness too, as reconstructPath returns empty for invalid cities
            return new PathResult(Collections.emptyList(), Double.POSITIVE_INFINITY, durationMillis, false, ALGORITHM_NAME);
        } else {
            return new PathResult(path, distance, durationMillis, false, ALGORITHM_NAME);
        }
    }
} 