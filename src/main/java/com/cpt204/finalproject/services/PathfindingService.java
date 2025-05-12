package com.cpt204.finalproject.services;

import com.cpt204.finalproject.model.Attraction;
import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.model.RoadNetwork;

import java.util.List;

/**
 * Defines the contract for pathfinding services.
 */
public interface PathfindingService {

    /**
     * Finds the shortest path between a start city and an end city, potentially visiting a list of attractions.
     *
     * @param roadNetwork The road network to search within.
     * @param startCity The starting city.
     * @param endCity The destination city.
     * @param attractionsToVisit A list of attractions to visit. Can be empty or null if no specific attractions are required.
     * @param useTimeout Whether to apply a timeout to the pathfinding algorithm.
     * @param timeoutMillis The timeout value in milliseconds, if useTimeout is true.
     * @return A PathResult object containing the path, total distance, and other relevant information.
     */
    PathResult findShortestPath(
        RoadNetwork roadNetwork,
        City startCity,
        City endCity,
        List<Attraction> attractionsToVisit,
        boolean useTimeout,
        long timeoutMillis
    );

    // Inner class or record to represent the result of a pathfinding operation
    // This should be defined according to what information needs to be returned.
    // For example: list of cities in path, total distance, calculation time, whether timeout occurred.
    class PathResult {
        private final List<City> path;
        private final double totalDistance;
        private final double calculationTimeMillis;
        private final boolean timedOut;
        private final String algorithmName;

        public PathResult(List<City> path, double totalDistance, double calculationTimeMillis, boolean timedOut, String algorithmName) {
            this.path = path;
            this.totalDistance = totalDistance;
            this.calculationTimeMillis = calculationTimeMillis;
            this.timedOut = timedOut;
            this.algorithmName = algorithmName;
        }

        public List<City> getPath() {
            return path;
        }

        public double getTotalDistance() {
            return totalDistance;
        }

        public double getCalculationTimeMillis() {
            return calculationTimeMillis;
        }

        public boolean isTimedOut() {
            return timedOut;
        }

        public String getAlgorithmName() {
            return algorithmName;
        }

        public static PathResult timedOut(String algorithmName, double calculationTimeMillis) {
            return new PathResult(List.of(), Double.POSITIVE_INFINITY, calculationTimeMillis, true, algorithmName);
        }
        
        public static PathResult empty(String algorithmName) {
            return new PathResult(List.of(), Double.POSITIVE_INFINITY, 0.0, false, algorithmName);
        }

        @Override
        public String toString() {
            return "PathResult{" +
                   "path=" + path +
                   ", totalDistance=" + totalDistance +
                   ", calculationTimeMillis=" + calculationTimeMillis +
                   ", timedOut=" + timedOut +
                   ", algorithmName='" + algorithmName + '\'' +
                   '}';
        }
    }
} 