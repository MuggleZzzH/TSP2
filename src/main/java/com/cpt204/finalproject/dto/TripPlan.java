package com.cpt204.finalproject.dto;

import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.services.PathfindingService;
import com.cpt204.finalproject.services.PoiOptimizerService;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

/**
 * Represents the final result of a trip planning request.
 * Contains the complete ordered list of cities to visit (including start and end),
 * the detailed path segments, total distance, calculation times, and potential warnings.
 */
public class TripPlan {

    private final List<City> fullPath;
    private final List<PathfindingService.PathResult> detailedSegments; // Optional: details for each leg
    private final double totalDistance;
    private final double poiOptimizationTimeMillis;
    private final double pathfindingTimeMillis;
    private final String optimizerAlgorithmName;
    private final String pathfinderAlgorithmName;
    private final boolean optimizerTimedOut;
    private final boolean pathfinderTimedOut; // Indicates if any segment pathfinding timed out
    private final String status;

    // Constructor (consider using a Builder pattern for more complex objects)
    public TripPlan(List<City> fullPath, List<PathfindingService.PathResult> detailedSegments,
                    double totalDistance, double poiOptimizationTimeMillis, double pathfindingTimeMillis, // Corrected parameter name
                    String optimizerAlgorithmName, String pathfinderAlgorithmName,
                    boolean optimizerTimedOut, boolean pathfinderTimedOut, String status) {
        this.fullPath = Collections.unmodifiableList(new ArrayList<>(fullPath));
        this.detailedSegments = Collections.unmodifiableList(new ArrayList<>(detailedSegments));
        this.totalDistance = totalDistance;
        this.poiOptimizationTimeMillis = poiOptimizationTimeMillis; // Corrected field name usage
        this.pathfindingTimeMillis = pathfindingTimeMillis;
        this.optimizerAlgorithmName = optimizerAlgorithmName;
        this.pathfinderAlgorithmName = pathfinderAlgorithmName;
        this.optimizerTimedOut = optimizerTimedOut;
        this.pathfinderTimedOut = pathfinderTimedOut;
        this.status = status;
    }
    
    // Static factory method for creating an error TripPlan
    public static TripPlan createErrorPlan(String errorMessage) {
        return new TripPlan(
            Collections.emptyList(),
            Collections.emptyList(),
            Double.POSITIVE_INFINITY,
            0.0, // poiOptimizationTimeMillis
            0.0, // pathfindingTimeMillis
            "N/A",
            "N/A",
            false,
            false,
            "Error: " + (errorMessage != null ? errorMessage : "Unknown error")
        );
    }

    // Static factory method for creating a failure TripPlan (e.g. path not found)
    public static TripPlan failure(String message) {
        return new TripPlan(
            Collections.emptyList(), 
            Collections.emptyList(), 
            Double.POSITIVE_INFINITY, 
            0.0, // poiOptimizationTimeMillis
            0.0, // pathfindingTimeMillis
            "N/A", 
            "N/A", 
            false, 
            false, 
            "Failure: " + message
        );
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

    public double getPoiOptimizationTimeMillis() {
        return poiOptimizationTimeMillis;
    }

    public double getPathfindingTimeMillis() {
        return pathfindingTimeMillis;
    }

    public String getOptimizerAlgorithmName() {
        return optimizerAlgorithmName;
    }

    public String getPathfinderAlgorithmName() {
        return pathfinderAlgorithmName;
    }

    public boolean isOptimizerTimedOut() {
        return optimizerTimedOut;
    }

    public boolean isPathfinderTimedOut() {
        return pathfinderTimedOut;
    }
    
    public String getStatus() {
        return status;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TripPlan{\n");
        sb.append("  Status: ").append(status).append("\n");
        sb.append("  Optimizer: ").append(optimizerAlgorithmName);
        if (optimizerTimedOut) sb.append(" (TIMED OUT)");
        sb.append(", Time: ").append(poiOptimizationTimeMillis).append(" ms\n");
        sb.append("  Pathfinder: ").append(pathfinderAlgorithmName);
        if (pathfinderTimedOut) sb.append(" (Segments TIMED OUT)");
        sb.append(", Total Time: ").append(String.format("%.3f", pathfindingTimeMillis)).append(" ms\n");
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