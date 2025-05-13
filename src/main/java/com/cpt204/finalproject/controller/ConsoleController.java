package com.cpt204.finalproject.controller;

import com.cpt204.finalproject.dto.TripPlan;
import com.cpt204.finalproject.services.TripPlanningService;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Handles console input and output for the Trip Planner application.
 * Interacts with the user and delegates planning tasks to the TripPlanningService.
 */
public class ConsoleController {

    private final TripPlanningService tripPlanningService;

    public ConsoleController(TripPlanningService tripPlanningService) {
        if (tripPlanningService == null) {
            throw new IllegalArgumentException("TripPlanningService cannot be null.");
        }
        this.tripPlanningService = tripPlanningService;
    }

    /**
     * Starts the main interactive loop of the application.
     */
    public void run() {
        try (Scanner scanner = new Scanner(System.in)) { // Use try-with-resources for Scanner
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

            System.out.println("\nPlanning trip from: " + startCityName + " to: " + endCityName);
            if (!attractionsToVisit.isEmpty()) {
                System.out.println("Visiting attractions: " + String.join(", ", attractionsToVisit));
            } else {
                System.out.println("No intermediate attractions to visit.");
            }

            // Plan the Trip
            // You can adjust useTimeout and timeoutMillis here if needed
            // Using default timeout defined in TripPlanningService or a fixed one here
            long timeoutMillis = 30000L; // 30 seconds, adjust as needed
            TripPlan plan = tripPlanningService.planTrip(startCityName, endCityName, attractionsToVisit, true, timeoutMillis);
            
             // Print Results
             System.out.println("\n--- Trip Plan Result ---");
             System.out.println(plan.toString());

        } catch (Exception e) {
            System.err.println("An error occurred during interaction or planning: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 