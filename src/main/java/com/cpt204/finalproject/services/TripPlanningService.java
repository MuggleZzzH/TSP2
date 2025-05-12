package com.cpt204.finalproject.services;

import com.cpt204.finalproject.dto.TripPlan;
import com.cpt204.finalproject.model.Attraction;
import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.model.RoadNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * High-level service responsible for orchestrating the trip planning process.
 * It uses PoiOptimizerService to find the best order of POIs and
 * PathfindingService to find the paths between consecutive points in the optimized route.
 */
public class TripPlanningService {

    private final RoadNetwork roadNetwork;
    private final PathfindingService pathfindingService;
    private final PoiOptimizerService permutationOptimizer; // For small N
    private final PoiOptimizerService dpOptimizer;          // For larger N

    private static final int PERMUTATION_THRESHOLD = 3;
    private static final long DEFAULT_TIMEOUT_MS = 30000; // Example 30 seconds timeout

    /**
     * Constructs a TripPlanningService.
     *
     * @param roadNetwork The road network data.
     * @param pathfindingService The service used to find paths between two cities.
     * @param permutationOptimizer The optimizer using permutation (for small number of POIs).
     * @param dpOptimizer The optimizer using dynamic programming (for moderate number of POIs).
     */
    public TripPlanningService(RoadNetwork roadNetwork, 
                               PathfindingService pathfindingService, 
                               PoiOptimizerService permutationOptimizer, 
                               PoiOptimizerService dpOptimizer) {
        this.roadNetwork = roadNetwork;
        this.pathfindingService = pathfindingService;
        this.permutationOptimizer = permutationOptimizer;
        this.dpOptimizer = dpOptimizer;
    }

    /**
     * Plans a trip given a start city, end city, and a list of attractions to visit.
     *
     * @param startCityName The name of the starting city.
     * @param endCityName The name of the ending city.
     * @param attractionNames The list of names of attractions to visit.
     * @return A TripPlan object containing the result.
     */
    public TripPlan planTrip(String startCityName, String endCityName, List<String> attractionNames) {
        return planTrip(startCityName, endCityName, attractionNames, true, DEFAULT_TIMEOUT_MS);
    }
    
     /**
     * Plans a trip given a start city, end city, and a list of attractions to visit, with timeout option.
     *
     * @param startCityName The name of the starting city.
     * @param endCityName The name of the ending city.
     * @param attractionNames The list of names of attractions to visit.
     * @param useTimeout Apply timeout to potentially long operations (like POI optimization).
     * @param timeoutMillis Timeout duration in milliseconds.
     * @return A TripPlan object containing the result.
     */
    public TripPlan planTrip(String startCityName, String endCityName, List<String> attractionNames, boolean useTimeout, long timeoutMillis) {

        // 1. Validate Input Cities and Attractions
        City startCity = roadNetwork.getCityByName(startCityName);
        City endCity = roadNetwork.getCityByName(endCityName);

        if (startCity == null) {
            return TripPlan.failure("Start city '" + startCityName + "' not found.");
        }
        if (endCity == null) {
            return TripPlan.failure("End city '" + endCityName + "' not found.");
        }

        List<Attraction> attractionsToVisit = new ArrayList<>();
        List<City> poiCities = new ArrayList<>(); // Cities containing the required attractions
        if (attractionNames != null) {
            for (String attractionName : attractionNames) {
                 // Find the attraction (case-insensitive and trimmed matching)
                final String normalizedAttractionName = attractionName.trim().toLowerCase();
                Attraction foundAttraction = roadNetwork.getAllAttractions().stream()
                        .filter(a -> a.getAttractionName() != null && 
                                     a.getAttractionName().trim().toLowerCase().equals(normalizedAttractionName))
                        .findFirst().orElse(null);
                        
                if (foundAttraction == null) {
                     return TripPlan.failure("Attraction '" + attractionName + "' not found.");
                }
                City poiCity = roadNetwork.getCityByName(foundAttraction.getCityName());
                 if (poiCity == null) { // Should not happen if RoadNetwork is consistent
                     return TripPlan.failure("City '" + foundAttraction.getCityName() + "' for attraction '" + attractionName + "' not found in network.");
                 }
                 if (!poiCities.contains(poiCity)) { // Avoid duplicate cities if multiple attractions are in the same city
                    poiCities.add(poiCity);
                 }
                 attractionsToVisit.add(foundAttraction); // Keep track of specific attractions if needed later
            }
        }

        // 2. Optimize POI Order
        PoiOptimizerService optimizerToUse;
        if (poiCities.size() > PERMUTATION_THRESHOLD) {
            optimizerToUse = dpOptimizer;
             System.out.println("Using Dynamic Programming Optimizer for " + poiCities.size() + " POIs (more than " + PERMUTATION_THRESHOLD + ").");
        } else {
            optimizerToUse = permutationOptimizer;
             System.out.println("Using Permutation Optimizer for " + poiCities.size() + " POIs.");
        }

        PoiOptimizerService.OptimizerResult optimizerResult = optimizerToUse.findBestPoiOrder(
                roadNetwork, startCity, endCity, poiCities, useTimeout, timeoutMillis);

        if (optimizerResult.getTotalDistance() == Double.POSITIVE_INFINITY && !optimizerResult.isTimedOut()) {
             return TripPlan.failure("Could not find a valid order to visit all POIs. " + optimizerResult.getAlgorithmName() + ".");
        }
        if (optimizerResult.isTimedOut()) {
             // Even if timed out, we might have a partial result, but let's report failure for now.
             // A more sophisticated approach could try to return the best partial plan found.
             return TripPlan.failure("POI optimization timed out using " + optimizerResult.getAlgorithmName() + ".");
        }

        // 3. Construct Full Path and Calculate Segment Details
        List<City> optimizedPoiOrder = optimizerResult.getBestOrder();
        List<City> fullVisitOrder = new ArrayList<>();
        fullVisitOrder.add(startCity);
        fullVisitOrder.addAll(optimizedPoiOrder);
        fullVisitOrder.add(endCity);

        List<PathfindingService.PathResult> detailedSegments = new ArrayList<>();
        List<City> finalFullPath = new ArrayList<>();
        double totalCalculatedDistance = 0;
        double totalPathfindingTime = 0.0;
        boolean pathfinderSegmentTimedOut = false; // Track if any segment timed out

        finalFullPath.add(startCity); // Start with the first city

        for (int i = 0; i < fullVisitOrder.size() - 1; i++) {
            City segmentStart = fullVisitOrder.get(i);
            City segmentEnd = fullVisitOrder.get(i + 1);

            // Note: Pathfinding between segments currently doesn't use the main timeout.
            // You might want to pass a portion of the remaining time if needed.
            PathfindingService.PathResult segmentResult = pathfindingService.findShortestPath(
                    roadNetwork, segmentStart, segmentEnd, Collections.emptyList(), false, 0);

            detailedSegments.add(segmentResult);
            totalPathfindingTime += segmentResult.getCalculationTimeMillis();
            
             if (segmentResult.isTimedOut()) { // Check if the specific pathfinder implementation can time out
                 pathfinderSegmentTimedOut = true;
                 // Decide how to handle segment timeout: fail entire plan or try to continue?
                 // For now, let's mark the plan but potentially return partial results.
                 System.err.println("Warning: Pathfinding timed out for segment: " + segmentStart.getName() + " -> " + segmentEnd.getName());
                 // Could add placeholder or break, here we continue but flag it
            }

            if (segmentResult.getTotalDistance() == Double.POSITIVE_INFINITY) {
                return TripPlan.failure("Could not find path for segment: " + segmentStart.getName() + " -> " + segmentEnd.getName() + ".");
            }

            totalCalculatedDistance += segmentResult.getTotalDistance();
            
            // Add the cities from the segment path (excluding the start of the segment, as it's the end of the previous one)
            List<City> segmentPath = segmentResult.getPath();
            if (!segmentPath.isEmpty()) {
                 finalFullPath.addAll(segmentPath.subList(1, segmentPath.size()));
            }
        }

        // 4. Assemble Final TripPlan
        String status = optimizerResult.isTimedOut() || pathfinderSegmentTimedOut ? "Completed with Timeouts" : "Success";
        if (totalCalculatedDistance == Double.POSITIVE_INFINITY && !pathfinderSegmentTimedOut) {
            status = "Failed to find complete path";
        }
        
        // Consistency check: The distance from optimizer *should* match the sum of segment distances if the same pathfinder is used internally.
        if (Math.abs(totalCalculatedDistance - optimizerResult.getTotalDistance()) > 1e-6 && optimizerResult.getTotalDistance() != Double.POSITIVE_INFINITY) {
            System.out.println("Warning: Optimizer distance (" + optimizerResult.getTotalDistance() + ") differs from sum of segment distances (" + totalCalculatedDistance + "). This might indicate different pathfinders or caching effects.");
            // Decide whether to use optimizerResult.getTotalDistance() or totalCalculatedDistance
            // Using sum of segments distance for now as it reflects the actual path constructed.
        }

        return new TripPlan(
                finalFullPath,
                detailedSegments,
                totalCalculatedDistance, // Use sum of actual segments found
                optimizerResult.getCalculationTimeMillis(),
                totalPathfindingTime,
                optimizerResult.getAlgorithmName(),
                detailedSegments.isEmpty() ? "N/A" : detailedSegments.get(0).getAlgorithmName(), // Get pathfinder name from first segment
                optimizerResult.isTimedOut(),
                pathfinderSegmentTimedOut,
                status
        );
    }
} 