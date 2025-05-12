package com.cpt204.finalproject.dataloader;

import com.cpt204.finalproject.model.Attraction;
import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.model.Road;
import com.cpt204.finalproject.model.RoadNetwork;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads road network data from CSV files.
 */
public class CsvDataLoader {

    private static final String CSV_DELIMITER = ",";

    /**
     * Loads city, road, and attraction data from specified CSV file paths.
     * Uses classpath resources for loading files.
     *
     * @param roadsCsvPath Path to the roads CSV file (e.g., "/data/roads_extended.csv").
     * @param attractionsCsvPath Path to the attractions CSV file (e.g., "/data/attractions_extended.csv").
     * @return A RoadNetwork object populated with data, or null if a critical error occurs.
     */
    public RoadNetwork loadData(String roadsCsvPath, String attractionsCsvPath) {
        Map<String, City> citiesMap = new HashMap<>();
        List<Road> roads = new ArrayList<>();
        List<Attraction> attractions = new ArrayList<>();

        try {
            // Load attractions first to discover all cities from both files
            // This ensures City objects are created for all cities mentioned in attractions or roads.
            loadAttractions(attractionsCsvPath, citiesMap, attractions);
            loadRoads(roadsCsvPath, citiesMap, roads);
            
            return new RoadNetwork(citiesMap.values(), roads, attractions);

        } catch (IOException e) {
            System.err.println("Error loading data from CSV files: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private void loadAttractions(String attractionsCsvPath, Map<String, City> citiesMap, List<Attraction> attractions) throws IOException {
        try (InputStream is = CsvDataLoader.class.getResourceAsStream(attractionsCsvPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            if (is == null) {
                throw new IOException("Cannot find attractions CSV file: " + attractionsCsvPath + " on classpath.");
            }

            String line = reader.readLine(); // Skip header
            if (line == null) {
                System.err.println("Warning: Attractions CSV file is empty or header is missing: " + attractionsCsvPath);
                return;
            }

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(CSV_DELIMITER);
                if (parts.length >= 2) {
                    String attractionName = parts[0].trim();
                    String cityName = parts[1].trim();

                    citiesMap.putIfAbsent(cityName, new City(cityName));
                    attractions.add(new Attraction(attractionName, cityName));
                } else {
                    System.err.println("Warning: Skipping malformed line in attractions CSV: " + line);
                }
            }
        }
    }

    private void loadRoads(String roadsCsvPath, Map<String, City> citiesMap, List<Road> roads) throws IOException {
        try (InputStream is = CsvDataLoader.class.getResourceAsStream(roadsCsvPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            
            if (is == null) {
                throw new IOException("Cannot find roads CSV file: " + roadsCsvPath + " on classpath.");
            }

            String line = reader.readLine(); // Skip header
            if (line == null) {
                System.err.println("Warning: Roads CSV file is empty or header is missing: " + roadsCsvPath);
                return;
            }

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(CSV_DELIMITER);
                if (parts.length >= 3) {
                    String cityAName = parts[0].trim();
                    String cityBName = parts[1].trim();
                    try {
                        double distance = Double.parseDouble(parts[2].trim());

                        City cityA = citiesMap.computeIfAbsent(cityAName, City::new);
                        City cityB = citiesMap.computeIfAbsent(cityBName, City::new);

                        // Add roads in both directions as per original experiment logic if data implies symmetric paths
                        // If your roads.csv defines one-way roads, this should be adjusted.
                        // Assuming symmetric for now, as is common in such datasets unless specified.
                        roads.add(new Road(cityA, cityB, distance));
                        roads.add(new Road(cityB, cityA, distance)); 

                    } catch (NumberFormatException e) {
                        System.err.println("Warning: Skipping road due to invalid distance format: " + line + " -> " + e.getMessage());
                    }
                } else {
                    System.err.println("Warning: Skipping malformed line in roads CSV: " + line);
                }
            }
        }
    }
} 