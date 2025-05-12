package com.cpt204.finalproject;

import com.cpt204.finalproject.dataloader.CsvDataLoader;
import com.cpt204.finalproject.dto.TripPlan;
import com.cpt204.finalproject.model.RoadNetwork;
import com.cpt204.finalproject.services.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

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

        // 3. Get Trip Parameters from User Input
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter start city name: ");
            String startCityName = scanner.nextLine();

            System.out.print("Enter end city name: ");
            String endCityName = scanner.nextLine();

            List<String> attractionsToVisit = new ArrayList<>();
            System.out.println("Enter names of attractions to visit, one per line (leave empty and press Enter when done):");
            while (true) {
                System.out.print("Attraction name (or press Enter to finish): ");
                String poiName = scanner.nextLine();
                if (poiName == null || poiName.trim().isEmpty()) {
                    break; // Exit loop if input is empty
                }
                attractionsToVisit.add(poiName.trim()); // Add trimmed name
            }

            // 4. Plan the Trip
            // You can adjust useTimeout and timeoutMillis here
            // long customTimeout = 60000; // 60 seconds
            TripPlan plan = tripPlanningService.planTrip(startCityName, endCityName, attractionsToVisit, true, 30000L); // Using default timeout
            
            // 5. Print Results
            System.out.println("\n--- Trip Plan Result ---");
            System.out.println(plan.toString());

        } catch (Exception e) {
            System.err.println("An error occurred during input or planning: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\nApplication finished.");
    }
} 