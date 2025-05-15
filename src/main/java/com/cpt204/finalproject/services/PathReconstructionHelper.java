package com.cpt204.finalproject.services;

import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.model.RoadNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PathReconstructionHelper {

    /**
     * Reconstructs the path from a source to a destination city using the predecessor array.
     *
     * @param network The road network, used to get City objects from indices.
     * @param prev The predecessor array where prev[i] is the index of the city before city i in the path.
     * @param srcIndex The index of the source city.
     * @param dstIndex The index of the destination city.
     * @return A list of City objects representing the path from src to dst. Returns an empty list if no path exists.
     */
    public static List<City> reconstructPath(RoadNetwork network, int[] prev, int srcIndex, int dstIndex) {
        List<City> path = new ArrayList<>();
        // Check if a path to dstIndex even exists (i.e., it was reached)
        // This can be inferred if prev[dstIndex] is set or if dist[dstIndex] was not infinity.
        // For simplicity, we rely on the prev array having valid predecessors for reachable nodes.
        // If dstIndex is srcIndex itself and it's a valid start, it should be part of the path.

        if (prev[dstIndex] == -1 && srcIndex != dstIndex) { // No path to dst, unless dst is src
            // If srcIndex == dstIndex, dist[srcIndex] should be 0, path is just [src]
            // If srcIndex is valid and dst is also src, but prev[dstIndex] is -1, it means the path is just the source itself.
             if (network.getCityByIndex(srcIndex) != null && srcIndex == dstIndex) {
                 path.add(network.getCityByIndex(srcIndex));
                 return path; // Path is just the source city itself
             }
            return Collections.emptyList(); // No path found or destination is unreachable
        }

        int currentIndex = dstIndex;
        while (currentIndex != -1) {
            City city = network.getCityByIndex(currentIndex); 
            if (city == null) {
                // This should not happen if indices are valid and network is consistent
                System.err.println("Error: City not found for index " + currentIndex + " during path reconstruction.");
                return Collections.emptyList(); // Or throw an exception
            }
            path.add(city);
            if (currentIndex == srcIndex) {
                break; // Reached the source city
            }
            currentIndex = prev[currentIndex];
            if (path.size() > network.getNumberOfCities()) { // Safety break for cycles if prev array is corrupted
                System.err.println("Error: Path reconstruction seems to be in a loop.");
                return Collections.emptyList(); // Avoid infinite loop
            }
        }

        Collections.reverse(path);
        
        // Ensure the path starts with the source and ends with the destination if a path was found
        if (path.isEmpty() && srcIndex == dstIndex && network.getCityByIndex(srcIndex) != null) {
            // Case: src == dst, path is just the city itself
            path.add(network.getCityByIndex(srcIndex));
        } else if (!path.isEmpty() && (network.getCityIndex(path.get(0)) != srcIndex || network.getCityIndex(path.get(path.size()-1)) != dstIndex)) {
            // This check indicates an issue if the reconstructed path doesn't start/end correctly
            // However, if dst was unreachable, path would be empty, which is handled.
            // If src was unreachable (dist[srcIndex] was INF from start), then it's also an issue.
            // But Dijkstra starts with dist[srcIndex]=0. So, this mostly catches logic errors in prev array population or reconstruction.
            // For now, we trust the prev array leads to a correct path if dst is reachable.
        }

        return path;
    }
} 