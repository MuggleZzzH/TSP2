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
        String roadsCsvPath = "/data/roads.csv"; 
        String attractionsCsvPath = "/data/attractions.csv";
        
        RoadNetwork roadNetwork = dataLoader.loadData(roadsCsvPath, attractionsCsvPath);

        if (roadNetwork == null) {
            System.err.println("Failed to load road network. Exiting application.");
            return;
        }
        System.out.println("Road network loaded successfully with " + roadNetwork.getNumberOfCities() + " cities.");

        // 2. Initialize Services
        PathfindingService dijkstraService = new DijkstraPathfindingService();
        PoiOptimizerService permutationOptimizer = new PermutationPoiOptimizerService(dijkstraService); 
        PoiOptimizerService dpOptimizer = new DynamicProgrammingPoiOptimizerService(dijkstraService); 

        TripPlanningService tripPlanningService = new TripPlanningService(
                roadNetwork, 
                dijkstraService, 
                permutationOptimizer, 
                dpOptimizer
        );
        System.out.println("Services initialized.");

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