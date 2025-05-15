package com.cpt204.finalproject.services;

import com.cpt204.finalproject.model.Attraction;
import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.model.RoadNetwork;

import java.util.Arrays;
import java.util.List;

/**
 * @implNote O(V²) version optimized for dense graphs
 */
public final class DenseDijkstraService implements PathfindingService {
    @Override
    public PathResult findShortestPath(
            RoadNetwork network, City src, City dst,
            List<Attraction> ignorePois, boolean useTimeout, long timeoutMillis) { // Renamed parameters for clarity

        final long startTime = System.nanoTime();
        final int V = network.getNumberOfCities();
        double[] dist = new double[V];
        int[] prev = new int[V]; // For path reconstruction
        boolean[] visited = new boolean[V];

        Arrays.fill(dist, Double.POSITIVE_INFINITY);
        Arrays.fill(prev, -1); // Initialize prev array

        Integer srcIndexInteger = network.getCityIndex(src);
        if (srcIndexInteger == null) {
            // Handle case where source city is not in the network (should ideally not happen with valid input)
            return PathResult.empty("DenseDijkstra");
        }
        int srcIndex = srcIndexInteger;
        dist[srcIndex] = 0;

        for (int step = 0; step < V; step++) {
            int u = -1;
            double best = Double.POSITIVE_INFINITY;
            for (int i = 0; i < V; i++) {
                if (!visited[i] && dist[i] < best) {
                    best = dist[i];
                    u = i;
                }
            }

            if (u == -1 || dist[u] == Double.POSITIVE_INFINITY) { // No path or remaining nodes are unreachable
                break;
            }
            visited[u] = true;

            // Optimization: if we have visited the destination, we can stop if only this path is needed.
            // However, for DistanceCache, we need all distances from src.
            // So, we continue until all reachable nodes are processed or V steps are done.

            double[][] adjMatrix = network.getDistanceMatrix(); // Assuming this returns direct distances, INF if no direct road
            for (int v = 0; v < V; v++) {
                // Check for direct edge existence (not INF) and if path through u is shorter
                if (adjMatrix[u][v] != Double.POSITIVE_INFINITY && !visited[v] && dist[u] + adjMatrix[u][v] < dist[v]) {
                    dist[v] = dist[u] + adjMatrix[u][v];
                    prev[v] = u; // Store predecessor for path reconstruction
                }
            }
             // Timeout check
            if (useTimeout && (System.nanoTime() - startTime) / 1_000_000 > timeoutMillis) {
                return PathResult.timedOut("DenseDijkstra", (System.nanoTime() - startTime) / 1_000_000.0);
            }
        }

        Integer dstIndexInteger = network.getCityIndex(dst);
        double finalDistance;
        List<City> path = List.of(); // Default to empty path

        if (dstIndexInteger != null) {
            int dstIndex = dstIndexInteger;
            finalDistance = dist[dstIndex];
            if (finalDistance != Double.POSITIVE_INFINITY) {
                 path = PathReconstructionHelper.reconstructPath(network, prev, srcIndex, dstIndex);
            }
        } else {
            // If dst is null (which is a case for DistanceCache precomputation),
            // finalDistance is not well-defined for a single target.
            // The full dist[] array is what's important.
            // We can set finalDistance to 0 or NaN, but it won't be used by DistanceCache.
            finalDistance = Double.NaN; // Or 0.0, or some other indicator
        }
        
        final double duration = (System.nanoTime() - startTime) / 1_000_000.0;

        // The PathResult now expects the full dist array for all nodes from src.
        // And the specific distance/path to 'dst' if 'dst' is not null.
        return new PathResult(path, finalDistance, dist, duration, false, "DenseDijkstra");
    }
} 