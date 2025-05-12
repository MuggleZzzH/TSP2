package com.cpt204.finalproject.services;

import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.model.Road;
import com.cpt204.finalproject.model.RoadNetwork;

import java.util.*;

/**
 * Computes and stores all-pairs shortest paths information (distances and predecessors)
 * using the Floyd-Warshall algorithm. Designed for dense graphs where precomputation is beneficial.
 */
public class ShortestPathInfo {

    private final List<City> cities; // Ordered list of cities corresponding to matrix indices
    private final Map<City, Integer> cityToIndex; // Map city object to its index
    private double[][] distMatrix; // distMatrix[i][j] = shortest distance from cities.get(i) to cities.get(j)
    private City[][] nextHopMatrix; // nextHopMatrix[i][j] = the next city on the shortest path from i to j
    private boolean computed = false;

    /**
     * Constructs ShortestPathInfo based on a RoadNetwork.
     * Does not compute paths immediately; call computeAllPairsPaths() explicitly.
     *
     * @param network The RoadNetwork containing the graph data.
     */
    public ShortestPathInfo(RoadNetwork network) {
        if (network == null) {
            throw new IllegalArgumentException("RoadNetwork cannot be null.");
        }
        // Create a fixed order for cities to map them to matrix indices
        this.cities = new ArrayList<>(network.getAllCities());
        this.cityToIndex = new HashMap<>();
        for (int i = 0; i < cities.size(); i++) {
            City city = cities.get(i);
            cityToIndex.put(city, i);
        }

        // Rename and change logic to directly use RoadNetwork's matrix
        initializeMatricesFromNetworkMatrix(network);
    }

    // Renamed method and updated logic
    private void initializeMatricesFromNetworkMatrix(RoadNetwork network) {
        int n = cities.size();
        this.distMatrix = new double[n][n];
        this.nextHopMatrix = new City[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                City cityI = cities.get(i);
                City cityJ = cities.get(j);
                
                // Get the direct distance from the RoadNetwork's matrix
                double directDistance = network.getDirectDistance(cityI, cityJ);
                this.distMatrix[i][j] = directDistance; // Initialize with direct distance

                if (i == j) {
                    this.nextHopMatrix[i][j] = cityI; // Next hop to self is self
                } else if (directDistance != Double.POSITIVE_INFINITY) {
                    // If there is a direct path, the first hop is the destination itself
                    this.nextHopMatrix[i][j] = cityJ; 
                } else {
                    // No direct path known initially
                    this.nextHopMatrix[i][j] = null;
                }
            }
        }
        // No need to iterate adjacency list anymore
    }
    
    // Removed the initializeMatricesFromAdjacencyList method

    /**
     * Computes all-pairs shortest paths using the Floyd-Warshall algorithm.
     * Must be called before retrieving path information.
     */
    public void computeAllPairsPaths() {
        if (computed) {
            return; // Already computed
        }
        System.out.println("Starting Floyd-Warshall computation...");
        long startTime = System.currentTimeMillis();
        int n = cities.size();
        for (int k = 0; k < n; k++) { // Intermediate city
            for (int i = 0; i < n; i++) { // Start city
                for (int j = 0; j < n; j++) { // End city
                    // If path i -> k -> j is shorter than current path i -> j
                    if (distMatrix[i][k] != Double.POSITIVE_INFINITY &&
                        distMatrix[k][j] != Double.POSITIVE_INFINITY &&
                        distMatrix[i][k] + distMatrix[k][j] < distMatrix[i][j]) {
                        
                        distMatrix[i][j] = distMatrix[i][k] + distMatrix[k][j];
                        // The next hop from i to j is the same as the next hop from i to k
                        nextHopMatrix[i][j] = nextHopMatrix[i][k]; 
                    }
                }
            }
        }
        computed = true;
        long endTime = System.currentTimeMillis();
        System.out.println("Floyd-Warshall computation finished in " + (endTime - startTime) + " ms.");
    }

    /**
     * Gets the shortest distance between two cities.
     * computeAllPairsPaths() must have been called first.
     *
     * @param from The starting city.
     * @param to The destination city.
     * @return The shortest distance, or Double.POSITIVE_INFINITY if no path exists or not computed.
     */
    public double getShortestDistance(City from, City to) {
        if (!computed) {
            System.err.println("Error: All-pairs paths have not been computed. Call computeAllPairsPaths() first.");
            return Double.POSITIVE_INFINITY;
        }
        Integer u = cityToIndex.get(from);
        Integer v = cityToIndex.get(to);
        if (u == null || v == null) {
            System.err.println("Error: One or both cities not found in the precomputed data: " + from + ", " + to);
            return Double.POSITIVE_INFINITY;
        }
        return distMatrix[u][v];
    }

    /**
     * Reconstructs the shortest path between two cities.
     * computeAllPairsPaths() must have been called first.
     *
     * @param from The starting city.
     * @param to The destination city.
     * @return A list of cities representing the shortest path (including start and end), 
     *         or an empty list if no path exists, cities are invalid, or not computed.
     */
    public List<City> reconstructPath(City from, City to) {
        if (!computed) {
            System.err.println("Error: All-pairs paths have not been computed. Call computeAllPairsPaths() first.");
            return Collections.emptyList();
        }
        Integer uIdx = cityToIndex.get(from);
        Integer vIdx = cityToIndex.get(to);

        if (uIdx == null || vIdx == null) {
            System.err.println("Error: One or both cities not found for path reconstruction: " + from + ", " + to);
            return Collections.emptyList();
        }

        if (nextHopMatrix[uIdx][vIdx] == null || distMatrix[uIdx][vIdx] == Double.POSITIVE_INFINITY) {
             // Check distance as well, nextHop might be non-null for i==j case but still unreachable if graph disconnected
            return Collections.emptyList(); // No path exists
        }

        List<City> path = new ArrayList<>();
        City current = from;
        Integer currentIdx = uIdx;
        path.add(current);
        
        // Follow the nextHop pointers until we reach the destination
        while (!current.equals(to)) {
            City nextHop = nextHopMatrix[currentIdx][vIdx];
            if (nextHop == null) { 
                // Should not happen if initial check passed, but as safeguard
                 System.err.println("Error reconstructing path: Null next hop found unexpectedly between " + current.getName() + " and " + to.getName());
                 return Collections.emptyList(); 
            }
            path.add(nextHop);
            current = nextHop;
            currentIdx = cityToIndex.get(current);
            if(path.size() > cities.size()) { // Safety break for potential cycles if data/logic error
                System.err.println("Error reconstructing path: Path longer than number of cities, potential cycle detected.");
                return Collections.emptyList(); 
            }
        }
        return path;
    }
} 