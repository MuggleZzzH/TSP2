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
        private final double totalDistance; // Distance to the specific destination city
        private final double[] distArray; // Distances from source to all other cities in the network
        private final double calculationTimeMillis;
        private final boolean timedOut;
        private final String algorithmName;

        // Updated constructor to include distArray
        public PathResult(List<City> path, double totalDistance, double[] distArray, double calculationTimeMillis, boolean timedOut, String algorithmName) {
            this.path = path;
            this.totalDistance = totalDistance;
            this.distArray = distArray; // Can be null if not computed or not applicable
            this.calculationTimeMillis = calculationTimeMillis;
            this.timedOut = timedOut;
            this.algorithmName = algorithmName;
        }

        // Constructor for services that might not compute the full distArray (e.g., if only target distance is needed)
        // Or if PathResult is used by optimizers that only care about a single distance value from a precomputed matrix
        public PathResult(List<City> path, double totalDistance, double calculationTimeMillis, boolean timedOut, String algorithmName) {
            this(path, totalDistance, null, calculationTimeMillis, timedOut, algorithmName); // Pass null for distArray
        }

        public List<City> getPath() {
            return path;
        }

        public double getTotalDistance() {
            return totalDistance;
        }

        // Getter for the distance array
        public double[] getDistArray() {
            return distArray;
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
            // For timedOut, distArray can be null or an array of infinities
            return new PathResult(List.of(), Double.POSITIVE_INFINITY, null, calculationTimeMillis, true, algorithmName);
        }
        
        public static PathResult empty(String algorithmName) {
            return new PathResult(List.of(), Double.POSITIVE_INFINITY, null, 0.0, false, algorithmName);
        }

        @Override
        public String toString() {
            return "PathResult{" +
                   "path=" + path +
                   ", totalDistance=" + totalDistance +
                   ", distArray=" + java.util.Arrays.toString(distArray) +
                   ", calculationTimeMillis=" + calculationTimeMillis +
                   ", timedOut=" + timedOut +
                   ", algorithmName='" + algorithmName + '\'' +
                   '}';
        }
    }
} 