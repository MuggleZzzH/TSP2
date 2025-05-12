package com.cpt204.finalproject.services;

import com.cpt204.finalproject.model.Attraction;
import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.model.Road;
import com.cpt204.finalproject.model.RoadNetwork;

import java.util.*;

/**
 * An implementation of PathfindingService that uses Dijkstra's algorithm.
 * This basic version finds the shortest path between two cities without considering intermediate POIs.
 * Handling intermediate POIs typically requires more complex logic (e.g., combining with a POI optimizer).
 */
public class DijkstraPathfindingService implements PathfindingService {

    private static final String ALGORITHM_NAME = "Dijkstra";

    @Override
    public PathResult findShortestPath(
            RoadNetwork roadNetwork,
            City startCity,
            City endCity,
            List<Attraction> attractionsToVisit, // This basic version does not use attractionsToVisit
            boolean useTimeout, // This basic version does not implement a timeout for Dijkstra itself
            long timeoutMillis) {

        long startTime = System.nanoTime(); // Use nanoTime for higher precision

        if (roadNetwork == null || startCity == null || endCity == null) {
            System.err.println("Road network, start city, or end city cannot be null.");
            return PathResult.empty(ALGORITHM_NAME); // Or throw IllegalArgumentException
        }

        // Check if start and end cities exist in the network
        City actualStartCity = roadNetwork.getCityByName(startCity.getName());
        City actualEndCity = roadNetwork.getCityByName(endCity.getName());

        if (actualStartCity == null) {
            System.err.println("Start city " + startCity.getName() + " not found in the road network.");
            return PathResult.empty(ALGORITHM_NAME);
        }
        if (actualEndCity == null) {
            System.err.println("End city " + endCity.getName() + " not found in the road network.");
            return PathResult.empty(ALGORITHM_NAME);
        }
        
        // If attractionsToVisit is not empty, this simple Dijkstra can't handle it directly.
        // A more advanced service would integrate with PoiOptimizerService.
        // For now, we'll ignore attractionsToVisit and find direct path.
        if (attractionsToVisit != null && !attractionsToVisit.isEmpty()) {
            System.out.println("Warning: DijkstraPathfindingService (basic) does not process intermediate attractions. Finding direct path.");
            // Optionally, could return an error or a specific result indicating this limitation.
        }

        Map<City, Double> distances = new HashMap<>();
        Map<City, City> predecessors = new HashMap<>();
        PriorityQueue<CityDistancePair> priorityQueue = new PriorityQueue<>(Comparator.comparingDouble(CityDistancePair::getDistance));

        for (City city : roadNetwork.getAllCities()) {
            distances.put(city, Double.POSITIVE_INFINITY);
        }
        distances.put(actualStartCity, 0.0);
        priorityQueue.add(new CityDistancePair(actualStartCity, 0.0));

        while (!priorityQueue.isEmpty()) {
            CityDistancePair currentPair = priorityQueue.poll();
            City u = currentPair.getCity();

            if (currentPair.getDistance() > distances.get(u)) {
                continue; // Already found a shorter path
            }

            if (u.equals(actualEndCity)) {
                break; // Reached destination
            }

            List<Road> roadsFromU = roadNetwork.getRoadsFrom(u);
            if (roadsFromU == null) continue; // Should not happen in a well-formed RoadNetwork

            for (Road road : roadsFromU) {
                City v = road.getDestination();
                double weight = road.getDistance();
                if (!distances.containsKey(v)) {
                    System.err.println("Warning: Destination city " + v.getName() + " for road from " + u.getName() + " not found in distance map. Skipping road.");
                    continue;
                }
                
                if (distances.get(u) + weight < distances.get(v)) {
                    distances.put(v, distances.get(u) + weight);
                    predecessors.put(v, u);
                    priorityQueue.add(new CityDistancePair(v, distances.get(v)));
                }
            }
        }

        List<City> path = new ArrayList<>();
        City step = actualEndCity;
        if (predecessors.get(step) == null && !step.equals(actualStartCity)) {
             long endTime = System.nanoTime();
            return new PathResult(Collections.emptyList(), Double.POSITIVE_INFINITY, (endTime - startTime) / 1_000_000.0, false, ALGORITHM_NAME); // No path found
        }

        while (step != null) {
            path.add(step);
            if (step.equals(actualStartCity)) break;
            step = predecessors.get(step);
        }
        Collections.reverse(path);
        
        long endTime = System.nanoTime();
        double durationMillis = (endTime - startTime) / 1_000_000.0;
        double totalDistance = distances.get(actualEndCity);
        
        boolean pathFound = totalDistance != Double.POSITIVE_INFINITY && !path.isEmpty() && path.get(0).equals(actualStartCity);
        
        if (!pathFound) {
             return new PathResult(Collections.emptyList(), Double.POSITIVE_INFINITY, durationMillis, false, ALGORITHM_NAME);
        }

        return new PathResult(path, totalDistance, durationMillis, false, ALGORITHM_NAME);
    }

    // Helper class for Priority Queue
    private static class CityDistancePair {
        private final City city;
        private final double distance;

        public CityDistancePair(City city, double distance) {
            this.city = city;
            this.distance = distance;
        }

        public City getCity() {
            return city;
        }

        public double getDistance() {
            return distance;
        }
    }
} 