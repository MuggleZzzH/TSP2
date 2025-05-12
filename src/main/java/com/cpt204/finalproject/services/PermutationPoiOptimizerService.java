package com.cpt204.finalproject.services;

import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.model.RoadNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implements PoiOptimizerService using a permutation-based (brute-force) approach.
 * It calculates the distance for every possible order of visiting POIs and selects the shortest.
 * This is computationally expensive and suitable only for a small number of POIs.
 */
public class PermutationPoiOptimizerService implements PoiOptimizerService {

    private static final String ALGORITHM_NAME = "Permutation (Brute-Force)";
    private final PathfindingService pathfindingService;

    public PermutationPoiOptimizerService(PathfindingService pathfindingService) {
        if (pathfindingService == null) {
            throw new IllegalArgumentException("PathfindingService cannot be null");
        }
        this.pathfindingService = pathfindingService;
    }

    @Override
    public OptimizerResult findBestPoiOrder(
            RoadNetwork roadNetwork,
            City startCity,
            City endCity,
            List<City> poisToVisit,
            boolean useTimeout,
            long timeoutMillis) {

        long startTime = System.currentTimeMillis();
        AtomicBoolean timeoutOccurred = new AtomicBoolean(false);
        long deadline = useTimeout ? startTime + timeoutMillis : Long.MAX_VALUE;

        if (roadNetwork == null || startCity == null || endCity == null || poisToVisit == null) {
             System.err.println("Error: RoadNetwork, start/end cities, and POI list cannot be null.");
             return OptimizerResult.empty(ALGORITHM_NAME); // Or throw exception
        }
        
        // Ensure start/end/pois exist in the network (basic check)
        if (roadNetwork.getCityByName(startCity.getName()) == null || roadNetwork.getCityByName(endCity.getName()) == null) {
             System.err.println("Error: Start or End city not found in network.");
             return OptimizerResult.empty(ALGORITHM_NAME);
        }
        for(City poi : poisToVisit) {
            if (roadNetwork.getCityByName(poi.getName()) == null) {
                System.err.println("Error: POI city " + poi.getName() + " not found in network.");
                return OptimizerResult.empty(ALGORITHM_NAME);
            }
        }

        List<City> currentPois = new ArrayList<>(poisToVisit);
        AtomicReference<Double> minTotalDistance = new AtomicReference<>(Double.POSITIVE_INFINITY);
        AtomicReference<List<City>> bestPoiOrder = new AtomicReference<>(null);

        // Handle empty POI list separately using PathfindingService
        if (currentPois.isEmpty()) {
            PathfindingService.PathResult directPathResult = pathfindingService.findShortestPath(roadNetwork, startCity, endCity, Collections.emptyList(), false, 0);
            long endTime = System.currentTimeMillis();
            // Check if pathfinding itself resulted in an issue (e.g., cities not found, though handled above)
            if (directPathResult.getTotalDistance() == Double.POSITIVE_INFINITY && !directPathResult.getPath().isEmpty()) {
                 // Path is empty, but distance is infinity means no path. If path is not empty but distance is infinity, that's an issue.
                 // However, PathResult usually returns empty path for infinity distance.
            }
            if (directPathResult.isTimedOut()) { // Should not happen with current Dijkstra, but good practice
                 return OptimizerResult.timedOut(ALGORITHM_NAME, endTime - startTime);
            }
            return new OptimizerResult(Collections.emptyList(), directPathResult.getTotalDistance(), endTime - startTime, false, ALGORITHM_NAME);
        }

        // Generate permutations using Heap's algorithm 
        int n = currentPois.size();
        int[] c = new int[n];
        // Initialize c with zeros
        for (int k=0; k < n; k++) c[k] = 0;
        
        List<City> currentPermutation = new ArrayList<>(currentPois); // Initial permutation

        calculatePermutationDistance(roadNetwork, startCity, endCity, currentPermutation, minTotalDistance, bestPoiOrder, deadline, timeoutOccurred);

        // Heap's algorithm implementation adapted for timeout checking
        int i = 0;
        while (i < n && !timeoutOccurred.get()) {
            if (System.currentTimeMillis() > deadline) {
                timeoutOccurred.set(true);
                break;
            }
            if (c[i] < i) {
                if (i % 2 == 0) {
                    Collections.swap(currentPermutation, 0, i);
                } else {
                    Collections.swap(currentPermutation, c[i], i);
                }
                calculatePermutationDistance(roadNetwork, startCity, endCity, currentPermutation, minTotalDistance, bestPoiOrder, deadline, timeoutOccurred);
                c[i]++;
                i = 0; // Reset to check the new permutation from the start of the array manipulation logic
            } else {
                c[i] = 0;
                i++;
            }
        }
        
        long endTime = System.currentTimeMillis();

        if (timeoutOccurred.get()) {
            System.out.println("Warning: Permutation POI optimization timed out after " + (endTime - startTime) + " ms.");
            // Even if timed out, return the best found so far, if any
            if (bestPoiOrder.get() != null) {
                return new OptimizerResult(bestPoiOrder.get(), minTotalDistance.get(), endTime - startTime, true, ALGORITHM_NAME);
            }
            return OptimizerResult.timedOut(ALGORITHM_NAME, endTime - startTime);
        }

        List<City> finalBestOrder = bestPoiOrder.get();
        if (finalBestOrder == null) { // No path found for any permutation
             return OptimizerResult.empty(ALGORITHM_NAME);
        }
        
        return new OptimizerResult(finalBestOrder, minTotalDistance.get(), endTime - startTime, false, ALGORITHM_NAME);
    }

    /**
     * Calculates the total distance for a given permutation of POIs.
     */
    private void calculatePermutationDistance(
            RoadNetwork roadNetwork,
            City startCity,
            City endCity,
            List<City> poiPermutation,
            AtomicReference<Double> minTotalDistance,
            AtomicReference<List<City>> bestPoiOrder,
            long deadline,
            AtomicBoolean timeoutOccurred) {

        if (System.currentTimeMillis() > deadline || poiPermutation.isEmpty()) { // Added check for empty poiPermutation
            timeoutOccurred.set(true);
            return;
        }

        double currentTotalDistance = 0;
        City currentCity = startCity;
        
        // Path from startCity to the first POI in the permutation
        PathfindingService.PathResult segmentResult = pathfindingService.findShortestPath(roadNetwork, currentCity, poiPermutation.get(0), Collections.emptyList(), false, 0);
        if (segmentResult.isTimedOut() || segmentResult.getTotalDistance() == Double.POSITIVE_INFINITY) {
            // Cannot reach first POI or segment calculation failed
            // Mark this permutation as infinitely long, or handle as error
            // For simplicity, we might just return if the first segment is bad, 
            // as the whole permutation becomes invalid.
            // Or, set currentTotalDistance to POSITIVE_INFINITY and let it be skipped.
            return; 
        }
        currentTotalDistance += segmentResult.getTotalDistance();
        currentCity = poiPermutation.get(0); // Update current city to the first POI

        // Paths between consecutive POIs in the permutation
        for (int i = 0; i < poiPermutation.size() - 1; i++) {
            if (System.currentTimeMillis() > deadline) { timeoutOccurred.set(true); return; }
            City nextPoi = poiPermutation.get(i + 1);
            segmentResult = pathfindingService.findShortestPath(roadNetwork, currentCity, nextPoi, Collections.emptyList(), false, 0);
            if (segmentResult.isTimedOut() || segmentResult.getTotalDistance() == Double.POSITIVE_INFINITY) {
                currentTotalDistance = Double.POSITIVE_INFINITY; // Mark permutation as invalid
                break; // No need to calculate further for this permutation
            }
            currentTotalDistance += segmentResult.getTotalDistance();
            currentCity = nextPoi;
        }

        // Path from the last POI in the permutation to endCity
        if (currentTotalDistance != Double.POSITIVE_INFINITY) { // Only if path so far is feasible
            if (System.currentTimeMillis() > deadline) { timeoutOccurred.set(true); return; }
            segmentResult = pathfindingService.findShortestPath(roadNetwork, currentCity, endCity, Collections.emptyList(), false, 0);
            if (segmentResult.isTimedOut() || segmentResult.getTotalDistance() == Double.POSITIVE_INFINITY) {
                currentTotalDistance = Double.POSITIVE_INFINITY; // Mark permutation as invalid
            } else {
                currentTotalDistance += segmentResult.getTotalDistance();
            }
        }

        // Update minimum distance and best order if this permutation is better
        if (currentTotalDistance < minTotalDistance.get()) {
            minTotalDistance.set(currentTotalDistance);
            bestPoiOrder.set(new ArrayList<>(poiPermutation)); // Store a copy
        }
    }
} 