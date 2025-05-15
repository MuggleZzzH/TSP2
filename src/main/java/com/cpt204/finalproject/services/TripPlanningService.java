package com.cpt204.finalproject.services;

import com.cpt204.finalproject.dto.TripPlan;
import com.cpt204.finalproject.model.Attraction;
import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.model.RoadNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;

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
    private final DistanceCache distanceCache; // New
    private final PathfindingService denseDijkstraService; // New, specifically DenseDijkstraService instance

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
        this.distanceCache = new DistanceCache();
        this.denseDijkstraService = new DenseDijkstraService();
    }

    /**
     * Constructs a TripPlanningService with additional parameters.
     *
     * @param roadNetwork The road network data.
     * @param pathfindingService The service used to find paths between two cities.
     * @param permutationOptimizer The optimizer using permutation (for small number of POIs).
     * @param dpOptimizer The optimizer using dynamic programming (for moderate number of POIs).
     * @param distanceCache The distance cache for precomputed distances.
     * @param denseDijkstraService The dense Dijkstra service for dense Dijkstra algorithm.
     */
    public TripPlanningService(RoadNetwork roadNetwork, 
                               PathfindingService pathfindingService, 
                               PoiOptimizerService permutationOptimizer, 
                               PoiOptimizerService dpOptimizer,
                               DistanceCache distanceCache,
                               PathfindingService denseDijkstraService) {
        this.roadNetwork = roadNetwork;
        this.pathfindingService = pathfindingService;
        this.permutationOptimizer = permutationOptimizer;
        this.dpOptimizer = dpOptimizer;
        this.distanceCache = distanceCache;
        this.denseDijkstraService = denseDijkstraService;
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
                final String normalizedAttractionName = attractionName.trim().toLowerCase();
                Attraction foundAttraction = roadNetwork.getAllAttractions().stream()
                        .filter(a -> a.getAttractionName() != null && 
                                     a.getAttractionName().trim().toLowerCase().equals(normalizedAttractionName))
                        .findFirst().orElse(null);
                        
                if (foundAttraction == null) {
                     return TripPlan.failure("Attraction '" + attractionName + "' not found.");
                }
                City poiCity = roadNetwork.getCityByName(foundAttraction.getCityName());
                 if (poiCity == null) {
                     return TripPlan.failure("City '" + foundAttraction.getCityName() + "' for attraction '" + attractionName + "' not found in network.");
                 }
                 if (!poiCities.contains(poiCity)) {
                    poiCities.add(poiCity);
                 }
                 attractionsToVisit.add(foundAttraction);
            }
        }

        // 2. Optimize POI Order
        PoiOptimizerService.OptimizerResult optimizerResult;
        String effectiveOptimizerName;
        PoiOptimizerService optimizerToUse; 

        if (poiCities.isEmpty()) {
            System.out.println("No POIs selected. Calculating direct path.");
            optimizerToUse = permutationOptimizer; 
            optimizerResult = optimizerToUse.findBestPoiOrder(roadNetwork, startCity, endCity, poiCities, useTimeout, timeoutMillis);
            effectiveOptimizerName = "None";
        } else if (poiCities.size() >= 2) { // 2 OR MORE POIs - Use DP
            optimizerToUse = dpOptimizer;
            System.out.println("Using Dynamic Programming Optimizer for " + poiCities.size() + " POIs.");

            Set<City> S = new LinkedHashSet<>(); 
            S.add(startCity);
            S.addAll(poiCities); 
            S.add(endCity);

            List<City> orderedNodesInS = new ArrayList<>(S);
            Map<City, Integer> nodeToIndexInS = new HashMap<>();
            for (int i = 0; i < orderedNodesInS.size(); i++) {
                nodeToIndexInS.put(orderedNodesInS.get(i), i);
            }

            int numNodesInS = S.size();
            double[][] shortestDistances = new double[numNodesInS][numNodesInS];
            long remainingTimeoutForPrecomputation = timeoutMillis; 

            for (int i = 0; i < numNodesInS; i++) {
                for (int j = 0; j < numNodesInS; j++) {
                    long precomputeStartTime = System.currentTimeMillis();
                    if (i == j) {
                        shortestDistances[i][j] = 0.0;
                    } else {
                        City cityA = orderedNodesInS.get(i);
                        City cityB = orderedNodesInS.get(j);
                        
                        PathfindingService.PathResult distResult = this.pathfindingService.findShortestPath(
                                roadNetwork, cityA, cityB, Collections.emptyList(), 
                                useTimeout, remainingTimeoutForPrecomputation); 
                        
                        if (distResult.isTimedOut()) {
                             return TripPlan.failure("Timeout during distance pre-computation for DP optimizer between " + cityA.getName() + " and " + cityB.getName());
                        }
                        if (distResult.getTotalDistance() == Double.POSITIVE_INFINITY) {
                            shortestDistances[i][j] = Double.POSITIVE_INFINITY;
                        } else {
                            shortestDistances[i][j] = distResult.getTotalDistance();
                        }
                    }
                    if (useTimeout) {
                        long precomputeTimeTaken = System.currentTimeMillis() - precomputeStartTime;
                        remainingTimeoutForPrecomputation -= precomputeTimeTaken;
                        if (remainingTimeoutForPrecomputation <= 0) {
                            return TripPlan.failure("Overall timeout exceeded during distance pre-computation phase for DP optimizer.");
                        }
                    }
                }
            }
            
            if (dpOptimizer instanceof DynamicProgrammingPoiOptimizerService) {
                optimizerResult = ((DynamicProgrammingPoiOptimizerService) dpOptimizer).findBestPoiOrder(
                        startCity, endCity, poiCities, 
                        S, shortestDistances, nodeToIndexInS, orderedNodesInS,
                        useTimeout, remainingTimeoutForPrecomputation); 
            } else {
                System.err.println("Error: dpOptimizer is not an instance of DynamicProgrammingPoiOptimizerService. Falling back to deprecated method or failing.");
                optimizerResult = dpOptimizer.findBestPoiOrder(roadNetwork, startCity, endCity, poiCities, useTimeout, timeoutMillis);
                 if (optimizerResult.isTimedOut()) { // Check result of fallback
                     return TripPlan.failure("DP optimization timed out (type issue fallback).");
                 } else if (optimizerResult.getTotalDistance() == Double.POSITIVE_INFINITY) {
                     return TripPlan.failure("DP optimization failed (type issue fallback, no path).");
                 }
            }
            effectiveOptimizerName = optimizerResult.getAlgorithmName(); // Get name from actual optimizer
        } else { // Exactly 1 POI - Use Permutation
            optimizerToUse = permutationOptimizer;
            System.out.println("Using Permutation Optimizer for " + poiCities.size() + " POI.");
            optimizerResult = optimizerToUse.findBestPoiOrder(
                roadNetwork, startCity, endCity, poiCities, useTimeout, timeoutMillis);
            effectiveOptimizerName = optimizerResult.getAlgorithmName();
        }

        if (optimizerResult.isTimedOut()) {
            if ("None".equals(effectiveOptimizerName)) {
                 System.err.println("Warning: Optimizer reported timeout for a 0-POI (None) scenario. This should be investigated but proceeding with timeout status.");
            } else {
                return TripPlan.failure("POI optimization timed out using " + effectiveOptimizerName + ".");
            }
        }
        
        if (optimizerResult.getTotalDistance() == Double.POSITIVE_INFINITY && !optimizerResult.isTimedOut() && !poiCities.isEmpty()) {
             return TripPlan.failure("Could not find a valid order to visit all POIs with " + effectiveOptimizerName + ".");
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
        boolean pathfinderSegmentTimedOut = false; 

        finalFullPath.add(startCity); 

        for (int i = 0; i < fullVisitOrder.size() - 1; i++) {
            City segmentStart = fullVisitOrder.get(i);
            City segmentEnd = fullVisitOrder.get(i + 1);

            PathfindingService.PathResult segmentResult = pathfindingService.findShortestPath(
                    roadNetwork, segmentStart, segmentEnd, Collections.emptyList(), false, 0);

            detailedSegments.add(segmentResult);
            totalPathfindingTime += segmentResult.getCalculationTimeMillis();
            
             if (segmentResult.isTimedOut()) { 
                 pathfinderSegmentTimedOut = true;
                 System.err.println("Warning: Pathfinding timed out for segment: " + segmentStart.getName() + " -> " + segmentEnd.getName());
            }

            if (segmentResult.getTotalDistance() == Double.POSITIVE_INFINITY) {
                return TripPlan.failure("Could not find path for segment: " + segmentStart.getName() + " -> " + segmentEnd.getName() + ".");
            }

            totalCalculatedDistance += segmentResult.getTotalDistance();
            
            List<City> segmentPath = segmentResult.getPath();
            if (!segmentPath.isEmpty()) {
                 finalFullPath.addAll(segmentPath.subList(1, segmentPath.size()));
            }
        }

        // 4. Assemble Final TripPlan
        String status;
        if (optimizerResult.isTimedOut() || pathfinderSegmentTimedOut) {
            status = "Completed with Timeouts";
        } else if (totalCalculatedDistance == Double.POSITIVE_INFINITY) {
            if (poiCities.isEmpty() && "None".equals(effectiveOptimizerName)) {
                status = "Failed to find direct path (Start to End)";
            } else {
                status = "Failed to find complete path including POIs";
            }
        } else {
            status = "Success";
        }
        
        if (Math.abs(totalCalculatedDistance - optimizerResult.getTotalDistance()) > 1e-6 && 
            optimizerResult.getTotalDistance() != Double.POSITIVE_INFINITY &&
            !"None".equals(effectiveOptimizerName)) { // Don't warn for "None" if distances differ slightly due to direct path vs "optimized" 0 POI path
            System.out.println("Warning: Optimizer distance (" + optimizerResult.getTotalDistance() + 
                               ") differs from sum of segment distances (" + totalCalculatedDistance + 
                               "). Effective optimizer: " + effectiveOptimizerName);
        }

        return new TripPlan(
                finalFullPath,
                detailedSegments,
                totalCalculatedDistance, 
                optimizerResult.getCalculationTimeMillis(),
                totalPathfindingTime,
                effectiveOptimizerName, // Use the modified name
                detailedSegments.isEmpty() ? "N/A" : detailedSegments.get(0).getAlgorithmName(), 
                optimizerResult.isTimedOut(),
                pathfinderSegmentTimedOut,
                status
        );
    }

    public TripPlan planTrip(City startCity, City endCity, List<Attraction> attractionsToVisit, String optimizerType) {
        final long methodStartTime = System.nanoTime();

        if (startCity == null || endCity == null) {
            return TripPlan.createErrorPlan("Start or end city cannot be null.");
        }

        // Resolve city names to City objects from the network to ensure consistency
        City resolvedStartCity = roadNetwork.getCityByName(startCity.getName());
        City resolvedEndCity = roadNetwork.getCityByName(endCity.getName());

        if (resolvedStartCity == null) {
            return TripPlan.createErrorPlan("Start city '" + startCity.getName() + "' not found in the network.");
        }
        if (resolvedEndCity == null) {
            return TripPlan.createErrorPlan("End city '" + endCity.getName() + "' not found in the network.");
        }

        List<City> poiCities = new ArrayList<>();
        if (attractionsToVisit != null) {
            for (Attraction attraction : attractionsToVisit) {
                if (attraction != null) {
                    City cityOfAttraction = roadNetwork.getCityByName(attraction.getCityName());
                    if (cityOfAttraction != null && !poiCities.contains(cityOfAttraction)) {
                        // Avoid adding start/end cities if they are also POI locations for the optimizer internal list
                        if (!cityOfAttraction.equals(resolvedStartCity) && !cityOfAttraction.equals(resolvedEndCity)) {
                            poiCities.add(cityOfAttraction);
                        }
                    } else if (cityOfAttraction == null){
                        System.err.println("Warning: Attraction '" + attraction.getAttractionName() + "' in city '" + attraction.getCityName() + "' which was not found. Skipping this POI.");
                    }
                }
            }
        }

        // Construct the set S for DistanceCache: Start + POIs + End
        // CRITICAL: Use LinkedHashSet to preserve order for matrix indexing later in DP
        Set<City> S = new LinkedHashSet<>();
        S.add(resolvedStartCity);
        poiCities.forEach(S::add); // Add all unique POI cities
        S.add(resolvedEndCity); // End city might be same as start or a POI city, Set handles uniqueness

        // Precompute pairwise shortest paths among cities in S using DenseDijkstraService
        // The denseDijkstraService instance should be injected or available here.
        double[][] shortestDistancesMatrix = distanceCache.getOrComputeDistances(S, roadNetwork, this.denseDijkstraService);
        Map<City, Integer> nodeToIndexInS = distanceCache.getNodeToIndexMap(S); // Get the mapping for S
        List<City> orderedNodesInS = distanceCache.getNodeList(S); // Get the ordered list for S
        
        // Select the optimizer based on optimizerType
        PoiOptimizerService selectedOptimizerInstance; // Renamed for clarity
        boolean usePrecomputedForSelectedOptimizer = false;

        // Determine which optimizer instance and method to use
        if ("DynamicProgrammingPoiOptimizerService".equalsIgnoreCase(optimizerType) || 
            "DP".equalsIgnoreCase(optimizerType) || 
            "HeldKarp".equalsIgnoreCase(optimizerType)) {
            // Assuming this.dpOptimizer is an instance of DynamicProgrammingPoiOptimizerService
            // and is configured to use the new method.
            // If TripPlanningService holds specific optimizer instances (e.g., this.dpOptimizer):
            selectedOptimizerInstance = this.dpOptimizer; // Assumes dpOptimizer is already new DynamicProgrammingPoiOptimizerService(this.roadNetwork, this.denseDijkstraService)
            usePrecomputedForSelectedOptimizer = true;
        } else if ("PermutationPoiOptimizerService".equalsIgnoreCase(optimizerType) || 
                   "BruteForce".equalsIgnoreCase(optimizerType)) {
            // Assuming this.permutationOptimizer is an instance of PermutationPoiOptimizerService
            selectedOptimizerInstance = this.permutationOptimizer; // Assumes permutationOptimizer is new PermutationPoiOptimizerService(this.denseDijkstraService)
            usePrecomputedForSelectedOptimizer = false; // Permutation optimizer will use its standard method
        } else {
            System.err.println("Unknown or unsupported optimizer type: " + optimizerType + ". Defaulting to Permutation-based.");
            selectedOptimizerInstance = this.permutationOptimizer; // Default
            usePrecomputedForSelectedOptimizer = false;
        }

        PoiOptimizerService.OptimizerResult optimizerResult;
        long optimizerTimeoutMillis = 10000; // Example timeout, configure as needed
        boolean useTimeoutForOptimizer = true; // Example, configure as needed

        if (usePrecomputedForSelectedOptimizer) {
            if (!(selectedOptimizerInstance instanceof DynamicProgrammingPoiOptimizerService)) {
                 return TripPlan.createErrorPlan("Selected optimizer for precomputation is not a DynamicProgrammingPoiOptimizerService instance.");
            }
            // Call the new method that accepts precomputed distances
            optimizerResult = selectedOptimizerInstance.findBestPoiOrder(
                resolvedStartCity, resolvedEndCity, poiCities, // poiCities here are the pure POIs
                S, shortestDistancesMatrix, nodeToIndexInS, orderedNodesInS,
                useTimeoutForOptimizer, optimizerTimeoutMillis
            );
        } else {
            // Call the original method that requires RoadNetwork for pathfinding
            optimizerResult = selectedOptimizerInstance.findBestPoiOrder(
                roadNetwork, // Pass the roadNetwork
                resolvedStartCity, resolvedEndCity, poiCities,
                useTimeoutForOptimizer, optimizerTimeoutMillis
            );
        }

        // 3. Construct Full Path and Calculate Segment Details
        List<City> optimizedPoiOrder = optimizerResult.getBestOrder();
        List<City> fullVisitOrder = new ArrayList<>();
        fullVisitOrder.add(resolvedStartCity);
        fullVisitOrder.addAll(optimizedPoiOrder);
        fullVisitOrder.add(resolvedEndCity);

        List<PathfindingService.PathResult> detailedSegments = new ArrayList<>();
        List<City> finalFullPath = new ArrayList<>();
        double totalCalculatedDistance = 0;
        double totalPathfindingTime = 0.0;
        boolean pathfinderSegmentTimedOut = false; // Track if any segment timed out

        finalFullPath.add(resolvedStartCity); // Start with the first city

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
        if (totalCalculatedDistance == Double.POSITIVE_INFINITY && !pathfinderSegmentTimedOut && !poiCities.isEmpty()) { // Added !poiCities.isEmpty() here
            status = "Failed to find complete path";
        } else if (totalCalculatedDistance == Double.POSITIVE_INFINITY && !pathfinderSegmentTimedOut && poiCities.isEmpty() && !"None".equals(optimizerResult.getAlgorithmName())){
            // Case where 0 POIs, but path start->end failed.
            status = "Failed to find direct path";
        }
        
        // Consistency check: The distance from optimizer *should* match the sum of segment distances if the same pathfinder is used internally.
        // For "None" (0 POI), optimizerResult.getTotalDistance() will be for start->end from Permutation, which should match totalCalculatedDistance.
        if (Math.abs(totalCalculatedDistance - optimizerResult.getTotalDistance()) > 1e-6 && optimizerResult.getTotalDistance() != Double.POSITIVE_INFINITY) {
            System.out.println("Warning: Optimizer distance (" + optimizerResult.getTotalDistance() + ") differs from sum of segment distances (" + totalCalculatedDistance + "). This might indicate different pathfinders or caching effects.");
        }

        return new TripPlan(
                finalFullPath,
                detailedSegments,
                totalCalculatedDistance, 
                optimizerResult.getCalculationTimeMillis(),
                totalPathfindingTime,
                optimizerResult.getAlgorithmName(), // Use the original name
                detailedSegments.isEmpty() ? "N/A" : detailedSegments.get(0).getAlgorithmName(), 
                optimizerResult.isTimedOut(),
                pathfinderSegmentTimedOut,
                status
        );
    }
} 