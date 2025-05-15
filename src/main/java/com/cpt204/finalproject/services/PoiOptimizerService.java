package com.cpt204.finalproject.services;

import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.model.RoadNetwork;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.ArrayList;

/**
 * Defines the contract for services that optimize the order of visiting Points of Interest (POIs).
 */
public interface PoiOptimizerService {

    /**
     * Finds the best order to visit a list of POIs (represented by Cities), starting from a startCity
     * and ending at an endCity.
     *
     * @param roadNetwork The road network containing the cities and connections.
     * @param startCity The starting city of the entire trip.
     * @param endCity The final destination city of the entire trip.
     * @param poisToVisit The list of POIs (cities) to visit between the start and end cities.
     * @param useTimeout Whether to apply a timeout to the optimization algorithm.
     * @param timeoutMillis The timeout value in milliseconds, if useTimeout is true.
     * @return An OptimizerResult object containing the best order of cities, total distance, etc.
     */
    OptimizerResult findBestPoiOrder(
        RoadNetwork roadNetwork,
        City startCity,
        City endCity,
        List<City> poisToVisit,
        boolean useTimeout,
        long timeoutMillis
    );

    /**
     * Finds the best order of POIs using precomputed distances among a specific set of relevant nodes.
     *
     * @param start The starting city.
     * @param end The ending city.
     * @param poisToVisit The list of POI cities to visit (excluding start and end unless they are also POIs explicitly).
     * @param S The complete set of unique, ordered nodes (start, POIs, end) for which distances are precomputed. Typically a LinkedHashSet.
     * @param shortestDistances A matrix where shortestDistances[i][j] is the precomputed shortest path distance
     *                          between the i-th city in ordered S and the j-th city in ordered S.
     * @param nodeToIndexInS A map from City objects in S to their 0-based index in the ordered S list (and thus in shortestDistances matrix).
     * @param orderedNodesInS An ordered list of cities in S, where the order corresponds to the indices in shortestDistances matrix.
     * @param useTimeout Whether to use a timeout for the optimization process.
     * @param timeoutMillis The timeout duration in milliseconds.
     * @return An OptimizerResult containing the best order of POIs, total distance, and other metrics.
     */
    default OptimizerResult findBestPoiOrder(
            City start, City end, List<City> poisToVisit,
            Set<City> S, double[][] shortestDistances, Map<City, Integer> nodeToIndexInS, List<City> orderedNodesInS,
            boolean useTimeout, long timeoutMillis) {
        // Default implementation could throw UnsupportedOperationException or adapt to the old method if feasible (though unlikely efficient).
        // This forces implementing classes to provide a version that uses the precomputed data if they support it.
        throw new UnsupportedOperationException("This optimizer does not support precomputed distance matrices.");
    }

    // Inner class or record to represent the result of a POI optimization
    // Similar to PathResult, contains the ordered list, distance, time, timeout status.
    class OptimizerResult {
        private final List<City> bestOrder;
        private final double totalDistance;
        private final double calculationTimeMillis;
        private final boolean timedOut;
        private final String algorithmName;

        public OptimizerResult(List<City> bestOrder, double totalDistance, double calculationTimeMillis, boolean timedOut, String algorithmName) {
            this.bestOrder = Collections.unmodifiableList(new ArrayList<>(bestOrder));
            this.totalDistance = totalDistance;
            this.calculationTimeMillis = calculationTimeMillis;
            this.timedOut = timedOut;
            this.algorithmName = algorithmName;
        }

        public List<City> getBestOrder() {
            return bestOrder;
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
        
        public static OptimizerResult timedOut(String algorithmName, double calculationTimeMillis) {
             return new OptimizerResult(Collections.emptyList(), Double.POSITIVE_INFINITY, calculationTimeMillis, true, algorithmName);
        }

        public static OptimizerResult empty(String algorithmName) {
            return new OptimizerResult(Collections.emptyList(), Double.POSITIVE_INFINITY, 0.0, false, algorithmName);
        }
        
        @Override
        public String toString() {
            return "OptimizerResult{" +
                   "bestOrder=" + bestOrder +
                   ", totalDistance=" + totalDistance +
                   ", calculationTimeMillis=" + calculationTimeMillis +
                   ", timedOut=" + timedOut +
                   ", algorithmName='" + algorithmName + '\'' +
                   '}';
        }
    }
} 