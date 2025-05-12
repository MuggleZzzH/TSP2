package com.cpt204.finalproject;

import com.cpt204.finalproject.dataloader.CsvDataLoader;
import com.cpt204.finalproject.dto.TripPlan;
import com.cpt204.finalproject.model.RoadNetwork;
import com.cpt204.finalproject.services.*;

import java.util.List;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        System.out.println("Initializing Trip Planner Application...");

        // 1. Load Data
        CsvDataLoader dataLoader = new CsvDataLoader();
        // Ensure these CSV files are in the src/main/resources/data/ directory
        // Adjust paths if your files are located elsewhere in the classpath.
        // Using the smaller dataset as requested
        String roadsCsvPath = "/data/roads.csv"; 
        String attractionsCsvPath = "/data/attractions.csv";
        
        RoadNetwork roadNetwork = dataLoader.loadData(roadsCsvPath, attractionsCsvPath);

        if (roadNetwork == null) {
            System.err.println("Failed to load road network. Exiting application.");
            return;
        }
        System.out.println("Road network loaded successfully with " + roadNetwork.getNumberOfCities() + " cities.");

        // 2. Initialize Services - NO Precomputation with ShortestPathInfo anymore
        // System.out.println("Initializing ShortestPathInfo and precomputing all-pairs shortest paths...");
        // ShortestPathInfo shortestPathInfo = new ShortestPathInfo(roadNetwork);
        // shortestPathInfo.computeAllPairsPaths(); // NO LONGER DOING THIS
        // System.out.println("All-pairs shortest paths precomputation complete.");

        // Use DijkstraPathfindingService for all pathfinding needs
        PathfindingService dijkstraService = new DijkstraPathfindingService();
        // PathfindingService matrixPathfinder = new MatrixBasedPathfindingService(shortestPathInfo); // REMOVED

        // Optimizers will use the live Dijkstra service
        PoiOptimizerService permutationOptimizer = new PermutationPoiOptimizerService(dijkstraService); 
        PoiOptimizerService dpOptimizer = new DynamicProgrammingPoiOptimizerService(dijkstraService); 

        TripPlanningService tripPlanningService = new TripPlanningService(
                roadNetwork, 
                dijkstraService, // Use Dijkstra service for final path segment reconstruction
                permutationOptimizer, 
                dpOptimizer
        );
        System.out.println("Services initialized using Dijkstra for on-demand pathfinding.");

        // 3. Define Trip Parameters (Example)
        // Phoenix AZ to San Antonio TX, visiting attractions from the smaller dataset
        String startCityName = "Phoenix AZ";
        String endCityName = "San Antonio TX";
        List<String> attractionsToVisit = Arrays.asList(
            "Desert Botanical Garden", // Located in Phoenix AZ
            "The Alamo" // Located in San Antonio TX
            // "The Sixth Floor Museum" // Located in Dallas TX - another option
        );
        
        // Example with fewer POIs (should use Permutation)
        // String startCityName = "Los Angeles CA";
        // String endCityName = "San Francisco CA";
        // List<String> attractionsToVisit = Arrays.asList("Disneyland");

        System.out.println("\nPlanning trip from: " + startCityName + " to: " + endCityName);
        if (!attractionsToVisit.isEmpty()) {
            System.out.println("Visiting attractions: " + String.join(", ", attractionsToVisit));
        } else {
            System.out.println("No intermediate attractions to visit.");
        }

        // 4. Plan the Trip
        // You can adjust useTimeout and timeoutMillis here
        // long customTimeout = 60000; // 60 seconds
        TripPlan plan = tripPlanningService.planTrip(startCityName, endCityName, attractionsToVisit, true, 30000L); // Using default timeout

        // 5. Print Results
        System.out.println("\n--- Trip Plan Result ---");
        System.out.println(plan.toString());

        System.out.println("\nApplication finished.");
    }
} 