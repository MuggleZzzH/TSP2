package com.cpt204.finalproject.services;

import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.model.RoadNetwork;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implements PoiOptimizerService using Dynamic Programming based on the Held-Karp algorithm concept
 * for the Traveling Salesperson Problem (TSP), adapted for finding the best order of POIs between
 * a fixed start and end city.
 *
 * This approach is generally more efficient than permutation for a moderate number of POIs.
 */
public class DynamicProgrammingPoiOptimizerService implements PoiOptimizerService {

    private static final String ALGORITHM_NAME = "Dynamic Programming (Held-Karp variant)";
    private final PathfindingService pathfindingService;

    private final Map<String, PathfindingService.PathResult> distanceCache;

    public DynamicProgrammingPoiOptimizerService(PathfindingService pathfindingService) {
        if (pathfindingService == null) {
            throw new IllegalArgumentException("PathfindingService cannot be null");
        }
        this.pathfindingService = pathfindingService;
        this.distanceCache = new HashMap<>();
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
        distanceCache.clear(); // Clear cache for each new request

        if (roadNetwork == null || startCity == null || endCity == null || poisToVisit == null) {
            System.err.println("Error: RoadNetwork, start/end cities, and POI list cannot be null.");
            return OptimizerResult.empty(ALGORITHM_NAME);
        }
        
        // --- Basic Validation ---
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

        // Handle empty POI list
        if (poisToVisit.isEmpty()) {
            PathfindingService.PathResult directPathResult = getDistance(roadNetwork, startCity, endCity, deadline, timeoutOccurred);
            long endTime = System.currentTimeMillis();
             if (timeoutOccurred.get() || directPathResult.isTimedOut()) {
                 return OptimizerResult.timedOut(ALGORITHM_NAME, endTime - startTime);
             }
             if (directPathResult.getTotalDistance() == Double.POSITIVE_INFINITY) {
                 System.err.println("Warning: No direct path found between start and end for empty POI list (DP).");
                 return OptimizerResult.empty(ALGORITHM_NAME);
             }
            return new OptimizerResult(Collections.emptyList(), directPathResult.getTotalDistance(), endTime - startTime, false, ALGORITHM_NAME);
        }

        // --- DP Setup ---
        List<City> allPois = new ArrayList<>(poisToVisit);
        int n = allPois.size(); // Number of POIs to visit

        // dp[mask][i] stores the minimum cost to visit the subset of POIs represented by mask,
        // ending at POI allPois.get(i).
        // The mask is a bitmask: if the j-th bit is set, it means allPois.get(j) has been visited.
        Map<Integer, Map<Integer, Double>> dp = new HashMap<>();

        // parent[mask][i] stores the predecessor POI index in the optimal path ending at POI i with visited set mask.
        Map<Integer, Map<Integer, Integer>> parent = new HashMap<>();

        // --- Base Case: Distance from startCity to each POI ---
        for (int i = 0; i < n; i++) {
             if (checkTimeout(deadline, timeoutOccurred)) return OptimizerResult.timedOut(ALGORITHM_NAME, System.currentTimeMillis() - startTime);
             
            PathfindingService.PathResult pathResult = getDistance(roadNetwork, startCity, allPois.get(i), deadline, timeoutOccurred);
            if (timeoutOccurred.get() || pathResult.isTimedOut() || pathResult.getTotalDistance() == Double.POSITIVE_INFINITY) {
                 // If timed out during getDistance or path is impossible
                 return handleFailureOrTimeout(timeoutOccurred.get(), startTime); 
            }

            int mask = 1 << i; // Mask representing only the i-th POI visited
            dp.computeIfAbsent(mask, k -> new HashMap<>()).put(i, pathResult.getTotalDistance());
            parent.computeIfAbsent(mask, k -> new HashMap<>()).put(i, -1); // -1 indicates startCity is the predecessor
        }

        // --- DP Calculation ---
        // Iterate through all possible subset sizes (from 1 POI up to n POIs)
        for (int mask = 1; mask < (1 << n); mask++) {
             if (checkTimeout(deadline, timeoutOccurred)) return OptimizerResult.timedOut(ALGORITHM_NAME, System.currentTimeMillis() - startTime);
             
             if (dp.get(mask) == null) continue; // Skip masks that are unreachable or not yet processed

            // For each POI 'i' that is the last visited city in the current subset 'mask'
            for (int i = 0; i < n; i++) {
                if ((mask & (1 << i)) != 0) { // Check if POI 'i' is in the current subset 'mask'
                    double currentCost = dp.get(mask).getOrDefault(i, Double.POSITIVE_INFINITY);
                    if (currentCost == Double.POSITIVE_INFINITY) continue; // Skip if this state is unreachable
                    
                    // Try extending the path to include a new POI 'j' not already in the mask
                    for (int j = 0; j < n; j++) {
                        if ((mask & (1 << j)) == 0) { // If POI 'j' is not in the current mask
                             if (checkTimeout(deadline, timeoutOccurred)) return OptimizerResult.timedOut(ALGORITHM_NAME, System.currentTimeMillis() - startTime);
                             
                            PathfindingService.PathResult pathResult = getDistance(roadNetwork, allPois.get(i), allPois.get(j), deadline, timeoutOccurred);
                             if (timeoutOccurred.get() || pathResult.isTimedOut() || pathResult.getTotalDistance() == Double.POSITIVE_INFINITY) {
                                 if (timeoutOccurred.get()) return OptimizerResult.timedOut(ALGORITHM_NAME, System.currentTimeMillis() - startTime);
                                 continue; // Cannot reach city j from i, try next j
                            }

                            double distanceItoJ = pathResult.getTotalDistance();
                            int nextMask = mask | (1 << j); // New mask including POI j
                            double newCost = currentCost + distanceItoJ;

                            // If this path to POI 'j' via POI 'i' is shorter than any previously found path to 'j' with the same visited set 'nextMask'
                            if (newCost < dp.computeIfAbsent(nextMask, k -> new HashMap<>()).getOrDefault(j, Double.POSITIVE_INFINITY)) {
                                dp.get(nextMask).put(j, newCost);
                                parent.computeIfAbsent(nextMask, k -> new HashMap<>()).put(j, i); // Record that 'i' is the predecessor of 'j' for this mask
                            }
                        }
                    }
                }
            }
        }

        // --- Find the final path and minimum cost ---
        // After filling the DP table, find the minimum cost path that visits all POIs (mask = (1<<n) - 1)
        // and ends at the endCity.
        double minTotalDistance = Double.POSITIVE_INFINITY;
        int lastPoiIndex = -1;
        int finalMask = (1 << n) - 1;

         if (dp.get(finalMask) == null) { // Never reached the state where all POIs were visited
             System.err.println("Error: Could not find a path visiting all POIs.");
             return handleFailureOrTimeout(timeoutOccurred.get(), startTime);
         }

        for (int i = 0; i < n; i++) {
             if (checkTimeout(deadline, timeoutOccurred)) return OptimizerResult.timedOut(ALGORITHM_NAME, System.currentTimeMillis() - startTime);
             
            double costToLastPoi = dp.get(finalMask).getOrDefault(i, Double.POSITIVE_INFINITY);
             if (costToLastPoi == Double.POSITIVE_INFINITY) continue;

            PathfindingService.PathResult pathResult = getDistance(roadNetwork, allPois.get(i), endCity, deadline, timeoutOccurred);
             if (timeoutOccurred.get() || pathResult.isTimedOut() || pathResult.getTotalDistance() == Double.POSITIVE_INFINITY) {
                  if (timeoutOccurred.get()) return OptimizerResult.timedOut(ALGORITHM_NAME, System.currentTimeMillis() - startTime);
                 continue; // Cannot reach end city from this last POI
             }
            
            double distanceLastPoiToEnd = pathResult.getTotalDistance();
            double totalCost = costToLastPoi + distanceLastPoiToEnd;

            if (totalCost < minTotalDistance) {
                minTotalDistance = totalCost;
                lastPoiIndex = i;
            }
        }

        if (lastPoiIndex == -1) {
            System.err.println("Error: Could not find a complete path to the end city visiting all POIs.");
             return handleFailureOrTimeout(timeoutOccurred.get(), startTime);
        }

        // --- Reconstruct the best path order ---
        List<City> bestPoiOrder = new LinkedList<>(); // Use LinkedList for efficient prepending
        int currentMask = finalMask;
        int currentIndex = lastPoiIndex;
        while (currentIndex != -1) {
            bestPoiOrder.add(0, allPois.get(currentIndex)); // Prepend to list
            int predecessorIndex = parent.get(currentMask).get(currentIndex);
            currentMask = currentMask ^ (1 << currentIndex); // Remove current city from mask
            currentIndex = predecessorIndex;
        }

        long endTime = System.currentTimeMillis();
        
        if (timeoutOccurred.get()) {
           System.out.println("Warning: DP POI optimization timed out after " + (endTime - startTime) + " ms.");
           return OptimizerResult.timedOut(ALGORITHM_NAME, endTime - startTime);
        }

        return new OptimizerResult(bestPoiOrder, minTotalDistance, endTime - startTime, false, ALGORITHM_NAME);
    }
    
    /** Helper to check timeout */
    private boolean checkTimeout(long deadline, AtomicBoolean timeoutOccurred) {
        if (System.currentTimeMillis() > deadline) {
            timeoutOccurred.set(true);
            return true;
        }
        return false;
    }
    
    /** Helper to handle returning empty or timed-out result */
    private OptimizerResult handleFailureOrTimeout(boolean timedOut, long startTime) {
        long endTime = System.currentTimeMillis();
        if(timedOut) {
            return OptimizerResult.timedOut(ALGORITHM_NAME, endTime - startTime);
        }
        return OptimizerResult.empty(ALGORITHM_NAME);
    }

    /**
     * Helper method to get distance between two cities using PathfindingService.
     * Restored caching logic.
     */
    private PathfindingService.PathResult getDistance(
            RoadNetwork roadNetwork, // Needed again for PathfindingService
            City cityA,
            City cityB,
            long deadline, // Still used for overall timeout checking
            AtomicBoolean timeoutOccurred) {
        
        if (checkTimeout(deadline, timeoutOccurred)) {
            // Return a dummy timed-out result if timeout happens before pathfinding call
             return new PathfindingService.PathResult(Collections.emptyList(), Double.POSITIVE_INFINITY, 0, true, "CacheCheckTimeout");
        }

        // Create a unique cache key (order city names alphabetically)
        String key = cityA.getName().compareTo(cityB.getName()) < 0
                     ? cityA.getName() + "_" + cityB.getName()
                     : cityB.getName() + "_" + cityA.getName();

        // Check cache first
        if (distanceCache.containsKey(key)) {
            return distanceCache.get(key);
        }

        // If not in cache, calculate using the pathfinding service
        PathfindingService.PathResult result = pathfindingService.findShortestPath(roadNetwork, cityA, cityB, Collections.emptyList(), false, 0); 

        // Check for timeout immediately after the call returns (in case the call was long)
        if (checkTimeout(deadline, timeoutOccurred)) {
             // Even if pathfinding returned a result, if timeout occurred during/after, treat as timed out
            return new PathfindingService.PathResult(Collections.emptyList(), Double.POSITIVE_INFINITY, 0, true, "PathfindCheckTimeout");
        }
        
         // Cache the result (even if distance is infinity, to avoid recalculating)
         // We cache the whole PathResult now.
         distanceCache.put(key, result);
 
         return result;
    }
} 