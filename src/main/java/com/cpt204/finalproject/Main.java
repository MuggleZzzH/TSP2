package com.cpt204.finalproject;

import com.cpt204.finalproject.dataloader.CsvDataLoader;
import com.cpt204.finalproject.controller.ConsoleController;
import com.cpt204.finalproject.dto.TripPlan;
import com.cpt204.finalproject.model.RoadNetwork;
import com.cpt204.finalproject.services.*;


public class Main {
    
    public static void main(String[] args) {
        System.out.println("Initializing Trip Planner Application...");

        // 1. Load Data
        CsvDataLoader dataLoader = new CsvDataLoader();
        // Ensure these CSV files are in the src/main/resources/data/ directory
        // Adjust paths if your files are located elsewhere in the classpath.
        // Using the smaller dataset as requested
        String roadsCsvPath = "/data/roads_extend.csv"; 
        String attractionsCsvPath = "/data/attractions.csv";
        
        RoadNetwork roadNetwork = dataLoader.loadData(roadsCsvPath, attractionsCsvPath);

        if (roadNetwork == null) {
            System.err.println("Failed to load road network. Exiting application.");
            return;
        }
        System.out.println("Road network loaded successfully with " + roadNetwork.getNumberOfCities() + " cities.");

        // 2. Initialize Services for the new architecture
        // PathfindingService generalPathfinder = new DijkstraPathfindingService(); // Old: using standard Dijkstra
        DenseDijkstraService denseDijkstraInstance = new DenseDijkstraService(); 
        PathfindingService generalPathfinder = denseDijkstraInstance; // New: using DenseDijkstra globally for pathfinding
        DistanceCache distanceCache = new DistanceCache();

        // Optimizers should be typed as the interface PoiOptimizerService for the TripPlanningService constructor
        PoiOptimizerService permutationOptimizer = new PermutationPoiOptimizerService(denseDijkstraInstance); 
        PoiOptimizerService dpOptimizer = new DynamicProgrammingPoiOptimizerService(roadNetwork, denseDijkstraInstance);

        // Match the constructor: TripPlanningService(RoadNetwork, PathfindingService, PoiOptimizerService, PoiOptimizerService, ShortestPathInfo, DistanceCache, PathfindingService)
        TripPlanningService tripPlanningService = new TripPlanningService(
                roadNetwork,                     // RoadNetwork
                generalPathfinder,               // PathfindingService (for segments, now DenseDijkstra)
                permutationOptimizer,            // PoiOptimizerService (permutation, uses DenseDijkstra)
                dpOptimizer,                     // PoiOptimizerService (DP, fallback uses DenseDijkstra)
                distanceCache,                   // DistanceCache
                denseDijkstraInstance            // PathfindingService (for precomputation by DistanceCache, must be DenseDijkstra)
        );
        System.out.println("Services initialized with new architecture (DenseDijkstra as global pathfinder).");

        // 3. Initialize Controller and Run Application Logic
        try {
            ConsoleController consoleController = new ConsoleController(tripPlanningService);
            consoleController.run(); // Start the user interaction loop
        } catch (Exception e) {
             System.err.println("An unexpected error occurred during application setup or run: " + e.getMessage());
             e.printStackTrace();
        }
       
        System.out.println("\nApplication finished.");
    }
} 