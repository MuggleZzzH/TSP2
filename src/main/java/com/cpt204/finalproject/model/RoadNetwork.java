package com.cpt204.finalproject.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays; // Added for Arrays.fill

/**
 * Represents the road network, containing cities and the roads connecting them.
 * Optimized for dense/complete graphs using an adjacency matrix for direct distances.
 */
public class RoadNetwork {
    private final List<City> cities; // Ordered list of cities corresponding to matrix indices
    private final Map<String, City> citiesByName; // For quick lookup by name
    private final Map<City, Integer> cityToIndex; // Map city object to its matrix index
    private final double[][] distanceMatrix; // Stores direct distances between cities
    // Adjacency list is removed
    private final Map<String, Set<Attraction>> attractionsByCity;

    /**
     * Constructs a RoadNetwork using an adjacency matrix approach.
     *
     * @param allCities A collection of all unique City objects in the network.
     * @param allRoads A collection of all Road objects defining direct connections.
     * @param allAttractions A collection of all Attraction objects.
     */
    public RoadNetwork(Collection<City> allCities, Collection<Road> allRoads, Collection<Attraction> allAttractions) {
        this.cities = new ArrayList<>(allCities);
        this.citiesByName = new HashMap<>();
        this.cityToIndex = new HashMap<>();
        for (int i = 0; i < this.cities.size(); i++) {
            City city = this.cities.get(i);
            this.citiesByName.put(city.getName(), city);
            this.cityToIndex.put(city, i);
        }

        int numCities = this.cities.size();
        this.distanceMatrix = new double[numCities][numCities];
        for (int i = 0; i < numCities; i++) {
            Arrays.fill(this.distanceMatrix[i], Double.POSITIVE_INFINITY);
            this.distanceMatrix[i][i] = 0; // Distance to self is 0
        }

        if (allRoads != null) {
            for (Road road : allRoads) {
                if (road != null) {
                    Integer u = this.cityToIndex.get(road.getSource());
                    Integer v = this.cityToIndex.get(road.getDestination());
                    if (u != null && v != null) {
                        // Store the direct distance. If multiple roads exist, CsvDataLoader should handle this
                        // or this assumes CsvDataLoader provides unique, shortest direct roads if duplicates exist.
                         this.distanceMatrix[u][v] = road.getDistance();
                    } else {
                        System.err.println("Warning: Road contains city not mapped to index: " + road);
                    }
                }
            }
        }

        this.attractionsByCity = new HashMap<>();
        if (allAttractions != null) {
             for (Attraction attraction : allAttractions) {
                 if (attraction != null && this.citiesByName.containsKey(attraction.getCityName())) {
                      this.attractionsByCity.computeIfAbsent(attraction.getCityName(), k -> new HashSet<>()).add(attraction);
                 } else if (attraction != null) {
                      System.err.println("Warning: Skipping attraction due to unknown city: " + attraction);
                 }
             }
        }
    }

    /**
     * Gets a city by its name. Performs case-insensitive and trimmed matching.
     * @param name The name of the city.
     * @return The City object, or null if not found.
     */
    public City getCityByName(String name) {
        if (name == null) {
            return null;
        }
        String normalizedInput = name.trim().toLowerCase();
        // Iterate through the cities Map for case-insensitive comparison
        for (Map.Entry<String, City> entry : citiesByName.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.trim().toLowerCase().equals(normalizedInput)) {
                return entry.getValue();
            }
        }
        // Fallback or alternative: If citiesByName keys were already normalized during construction
        // return citiesByName.get(normalizedInput);
        return null; // Not found
    }

    /**
     * Gets all cities in the network (maintains insertion order from construction).
     * @return An unmodifiable list of all cities.
     */
    public List<City> getAllCities() {
        return Collections.unmodifiableList(cities);
    }
    
    /**
     * Gets the city at a specific index in the internal list.
     * Useful for matrix operations if direct index access is needed externally.
     * @param index The index of the city.
     * @return The City object at that index, or null if index is out of bounds.
     */
    public City getCityByIndex(int index) {
        if (index >= 0 && index < cities.size()) {
            return cities.get(index);
        }
        return null;
    }

    /**
     * Gets the index of a given City object.
     * @param city The city object.
     * @return The index of the city, or null if not found.
     */
    public Integer getIndexByCity(City city) {
        return cityToIndex.get(city);
    }

    /**
     * Gets the direct distance between two cities as stored in the adjacency matrix.
     * This is the direct road distance, not necessarily the shortest path if the graph wasn't complete.
     * @param fromCity The source city.
     * @param toCity The destination city.
     * @return The direct distance, or Double.POSITIVE_INFINITY if no direct road or cities are invalid.
     */
    public double getDirectDistance(City fromCity, City toCity) {
        Integer u = cityToIndex.get(fromCity);
        Integer v = cityToIndex.get(toCity);
        if (u != null && v != null) {
            return distanceMatrix[u][v];
        }
        return Double.POSITIVE_INFINITY;
    }
    
    /**
     * Provides access to the raw distance matrix. Use with caution.
     * @return A copy of the distance matrix to prevent external modification.
     */
    public double[][] getDistanceMatrix() {
        // Return a copy to prevent external modification of the internal state
        double[][] copy = new double[distanceMatrix.length][];
        for (int i = 0; i < distanceMatrix.length; i++) {
            copy[i] = Arrays.copyOf(distanceMatrix[i], distanceMatrix[i].length);
        }
        return copy;
    }

    /**
     * Gets all road segments originating from a given city. 
     * For an adjacency matrix representation, this dynamically reconstructs Road objects.
     * This can be less efficient than directly working with the matrix if only distances are needed.
     * @param city The source city.
     * @return An unmodifiable list of roads starting from the city.
     */
    public List<Road> getRoadsFrom(City city) {
        Integer u = cityToIndex.get(city);
        if (u == null) {
            return Collections.emptyList();
        }
        List<Road> outgoingRoads = new ArrayList<>();
        for (int v = 0; v < cities.size(); v++) {
            if (distanceMatrix[u][v] != Double.POSITIVE_INFINITY && u != v) {
                outgoingRoads.add(new Road(city, cities.get(v), distanceMatrix[u][v]));
            }
        }
        return Collections.unmodifiableList(outgoingRoads);
    }
    
    // getAllRoads() can be implemented similarly to getRoadsFrom by iterating all possible pairs
    public Collection<Road> getAllRoads() {
        List<Road> allRoads = new ArrayList<>();
        for (int u = 0; u < cities.size(); u++) {
            City sourceCity = cities.get(u);
            for (int v = 0; v < cities.size(); v++) {
                if (u == v) continue; // No self-loops for roads in this context
                if (distanceMatrix[u][v] != Double.POSITIVE_INFINITY) {
                    allRoads.add(new Road(sourceCity, cities.get(v), distanceMatrix[u][v]));
                }
            }
        }
        return Collections.unmodifiableCollection(allRoads);
    }

    /**
     * Gets the number of cities in the network.
     * @return The total number of cities.
     */
    public int getNumberOfCities() {
        return cities.size();
    }

    /**
     * Gets attractions for a specific city.
     * @param cityName The name of the city.
     * @return An unmodifiable set of attractions in the specified city, or an empty set if the city
     *         is not found or has no attractions. The search is case-insensitive.
     */
    public Set<Attraction> getAttractionsInCity(String cityName) {
        if (cityName == null) {
            return Collections.emptySet();
        }
        String normalizedCityName = cityName.toLowerCase().trim();
        Set<Attraction> cityAttractions = attractionsByCity.get(normalizedCityName);
        if (cityAttractions == null) {
            cityAttractions = attractionsByCity.get(cityName);
        }
        return cityAttractions != null ? Collections.unmodifiableSet(cityAttractions) : Collections.emptySet();
    }

    /**
     * Gets all attractions across all cities.
     * @return An unmodifiable collection of all attractions.
     */
     public Collection<Attraction> getAllAttractions() {
         Set<Attraction> all = new HashSet<>();
         for(Set<Attraction> cityAttractions : attractionsByCity.values()){
             all.addAll(cityAttractions);
         }
         return Collections.unmodifiableSet(all);
     }
} 