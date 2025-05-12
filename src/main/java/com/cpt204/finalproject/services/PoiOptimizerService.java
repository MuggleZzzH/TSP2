package com.cpt204.finalproject.services;

import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.model.RoadNetwork;

import java.util.List;

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

    // Inner class or record to represent the result of a POI optimization
    // Similar to PathResult, contains the ordered list, distance, time, timeout status.
    class OptimizerResult {
        private final List<City> bestOrder;
        private final double totalDistance;
        private final long calculationTimeMillis;
        private final boolean timedOut;
        private final String algorithmName;

        public OptimizerResult(List<City> bestOrder, double totalDistance, long calculationTimeMillis, boolean timedOut, String algorithmName) {
            // Ensure start and end cities are included if necessary, or adjust based on how you use this.
            // This basic version assumes bestOrder is just the optimal sequence of the poisToVisit.
            // A more complete implementation might prepend startCity and append endCity.
            this.bestOrder = bestOrder;
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

        public long getCalculationTimeMillis() {
            return calculationTimeMillis;
        }

        public boolean isTimedOut() {
            return timedOut;
        }

        public String getAlgorithmName() {
            return algorithmName;
        }
        
        public static OptimizerResult timedOut(String algorithmName, long calculationTimeMillis) {
             return new OptimizerResult(List.of(), Double.POSITIVE_INFINITY, calculationTimeMillis, true, algorithmName);
        }

        public static OptimizerResult empty(String algorithmName) {
            return new OptimizerResult(List.of(), Double.POSITIVE_INFINITY, 0, false, algorithmName);
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