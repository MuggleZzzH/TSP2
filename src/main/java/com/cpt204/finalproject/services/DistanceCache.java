package com.cpt204.finalproject.services;

import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.model.RoadNetwork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DistanceCache {
    // Using LinkedHashMap or ensuring key set (S) is LinkedHashSet for consistent iteration order if needed elsewhere,
    // but for cache lookup, Set equality is based on content.
    private final Map<Set<City>, double[][]> cache = new HashMap<>();
    private final Map<Set<City>, Map<City, Integer>> nodeToIndexMapCache = new HashMap<>();
    private final Map<Set<City>, List<City>> nodeListCache = new HashMap<>();


    /**
     * Retrieves the precomputed shortest path distances for a given set of nodes (S).
     * If not in cache, it computes them using the provided Dijkstra service and stores them.
     *
     * @param S A set of cities (start, POIs, end) for which pairwise distances are needed. Must be a LinkedHashSet to preserve order for indexing.
     * @param network The road network.
     * @param dijkstraService The pathfinding service (e.g., DenseDijkstraService) to compute paths.
     * @return A 2D array {@code shortest[m][m]} where {@code m = S.size()} and {@code shortest[i][j]} is the
     * distance from the i-th city in S to the j-th city in S.
     */
    public double[][] getOrComputeDistances(Set<City> S, RoadNetwork network, PathfindingService dijkstraService) {
        // It's crucial that S is a LinkedHashSet to maintain insertion order for consistent indexing in the resulting matrix.
        // If S is just a HashSet, the order of nodes can vary, making the returned matrix's indexing unreliable.
        if (!(S instanceof LinkedHashSet)) {
            // Or convert it, but this signals a contract violation to the caller.
            throw new IllegalArgumentException("Input set S must be a LinkedHashSet to guarantee predictable matrix indexing.");
        }

        return cache.computeIfAbsent(S, key -> {
            List<City> nodes = new ArrayList<>(key); // Order is preserved from LinkedHashSet
            int m = nodes.size();
            double[][] shortestDistances = new double[m][m];
            Map<City, Integer> nodeToIndexMapping = new HashMap<>();
            for (int i = 0; i < m; i++) {
                nodeToIndexMapping.put(nodes.get(i), i);
            }

            // Store the mapping and list for this key set S
            nodeToIndexMapCache.put(key, nodeToIndexMapping);
            nodeListCache.put(key, nodes);

            for (int i = 0; i < m; i++) {
                City sourceCity = nodes.get(i);
                // Call Dijkstra from sourceCity to all other nodes in the network.
                // The `dst` parameter is null because we want the dist[] array for all nodes from PathResult.
                PathfindingService.PathResult result = dijkstraService.findShortestPath(network, sourceCity, null, List.of(), false, 0);
                double[] allDistancesFromSource = result.getDistArray();

                if (allDistancesFromSource == null) {
                    // This should not happen if DenseDijkstraService is implemented correctly to always return distArray.
                    // Fill with infinity to indicate error or missing data.
                    System.err.println("Error: DenseDijkstraService did not return a distance array for source: " + sourceCity.getName());
                    for (int j = 0; j < m; j++) {
                        shortestDistances[i][j] = Double.POSITIVE_INFINITY;
                    }
                    continue; // Move to the next source city in S
                }

                for (int j = 0; j < m; j++) {
                    City targetCity = nodes.get(j);
                    Integer targetCityNetworkIndex = network.getCityIndex(targetCity);
                    if (targetCityNetworkIndex != null && targetCityNetworkIndex < allDistancesFromSource.length) {
                        shortestDistances[i][j] = allDistancesFromSource[targetCityNetworkIndex];
                    } else {
                        // Should not happen if cities in S are valid and in the network.
                        shortestDistances[i][j] = Double.POSITIVE_INFINITY;
                        System.err.println("Error: Target city " + targetCity.getName() + " not found in network index or distArray during cache computation.");
                    }
                }
            }
            return shortestDistances;
        });
    }

    /**
     * Gets the mapping from City to its index within the specific ordered set S used for a cache entry.
     * This map is created when getOrComputeDistances is first called for a set S.
     * @param S The set of cities (must be the same instance or equal to one used in getOrComputeDistances).
     * @return A map from City to its 0-based index in the ordered list derived from S, or null if S not cached.
     */
    public Map<City, Integer> getNodeToIndexMap(Set<City> S) {
        return nodeToIndexMapCache.get(S);
    }
    
    /**
     * Gets the ordered list of cities for a given set S, corresponding to the cache entry.
     * This list defines the row/column order for the distance matrix from getOrComputeDistances.
     * @param S The set of cities.
     * @return The ordered list of cities, or null if S not cached.
     */
    public List<City> getNodeList(Set<City> S){
        return nodeListCache.get(S);
    }
} 