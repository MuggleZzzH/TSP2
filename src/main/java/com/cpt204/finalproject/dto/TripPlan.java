package com.cpt204.finalproject.dto;

import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.services.PathfindingService;
import com.cpt204.finalproject.services.PoiOptimizerService;

import java.util.List;

/**
 * Represents the final result of a trip planning request.
 * Contains the complete ordered list of cities to visit (including start and end),
 * the detailed path segments, total distance, calculation times, and potential warnings.
 */
public class TripPlan {

    private final List<City> fullPath;
    private final List<PathfindingService.PathResult> detailedSegments; // Optional: details for each leg
    private final double totalDistance;
    private final long poiOptimizationTimeMillis;
    private final double totalPathfindingTimeMillis; // Changed to double
    private final String poiOptimizerAlgorithmName;
    private final String pathfindingAlgorithmName;
    private final boolean optimizerTimedOut;
    private final boolean pathfinderTimedOut; // Indicates if any segment pathfinding timed out
    private final String statusMessage;

    // Constructor (consider using a Builder pattern for more complex objects)
    public TripPlan(List<City> fullPath, List<PathfindingService.PathResult> detailedSegments,
                    double totalDistance, long poiOptimizationTimeMillis, double totalPathfindingTimeMillis, // Changed to double
                    String poiOptimizerAlgorithmName, String pathfindingAlgorithmName, 
                    boolean optimizerTimedOut, boolean pathfinderTimedOut, String statusMessage) {
        this.fullPath = fullPath;
        this.detailedSegments = detailedSegments;
        this.totalDistance = totalDistance;
        this.poiOptimizationTimeMillis = poiOptimizationTimeMillis;
        this.totalPathfindingTimeMillis = totalPathfindingTimeMillis;
        this.poiOptimizerAlgorithmName = poiOptimizerAlgorithmName;
        this.pathfindingAlgorithmName = pathfindingAlgorithmName;
        this.optimizerTimedOut = optimizerTimedOut;
        this.pathfinderTimedOut = pathfinderTimedOut;
        this.statusMessage = statusMessage;
    }
    
    // Static factory for failed plans
    public static TripPlan failure(String message) {
        return new TripPlan(List.of(), List.of(), Double.POSITIVE_INFINITY, 0, 0, "N/A", "N/A", false, false, message);
    }

    // Getters
    public List<City> getFullPath() {
        return fullPath;
    }

    public List<PathfindingService.PathResult> getDetailedSegments() {
        return detailedSegments;
    }

    public double getTotalDistance() {
        return totalDistance;
    }

    public long getPoiOptimizationTimeMillis() {
        return poiOptimizationTimeMillis;
    }

    public double getTotalPathfindingTimeMillis() { // Changed to double
        return totalPathfindingTimeMillis;
    }

    public String getPoiOptimizerAlgorithmName() {
        return poiOptimizerAlgorithmName;
    }

    public String getPathfindingAlgorithmName() {
        return pathfindingAlgorithmName;
    }

    public boolean isOptimizerTimedOut() {
        return optimizerTimedOut;
    }

    public boolean isPathfinderTimedOut() {
        return pathfinderTimedOut;
    }
    
    public String getStatusMessage() {
        return statusMessage;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TripPlan{\n");
        sb.append("  Status: ").append(statusMessage).append("\n");
        sb.append("  Optimizer: ").append(poiOptimizerAlgorithmName);
        if (optimizerTimedOut) sb.append(" (TIMED OUT)");
        sb.append(", Time: ").append(poiOptimizationTimeMillis).append(" ms\n");
        sb.append("  Pathfinder: ").append(pathfindingAlgorithmName);
        if (pathfinderTimedOut) sb.append(" (Segments TIMED OUT)");
        sb.append(", Total Time: ").append(String.format("%.3f", totalPathfindingTimeMillis)).append(" ms\n");
        sb.append("  Total Distance: ").append(String.format("%.2f", totalDistance)).append("\n");
        sb.append("  Full Path: ");
        if (fullPath == null || fullPath.isEmpty()) {
            sb.append("N/A\n");
        } else {
            for (int i = 0; i < fullPath.size(); i++) {
                sb.append(fullPath.get(i).getName());
                if (i < fullPath.size() - 1) {
                    sb.append(" -> ");
                }
            }
            sb.append("\n");
        }
        // Optionally add detailed segments if needed
        // sb.append("  Detailed Segments: ").append(detailedSegments).append("\n");
        sb.append("}");
        return sb.toString();
    }
} 