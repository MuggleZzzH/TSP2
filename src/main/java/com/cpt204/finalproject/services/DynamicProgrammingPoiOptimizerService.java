package com.cpt204.finalproject.services;

import com.cpt204.finalproject.model.City;
import com.cpt204.finalproject.model.RoadNetwork;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implements PoiOptimizerService using Dynamic Programming based on the Held-Karp algorithm concept
 * for the Traveling Salesperson Problem (TSP), adapted for finding the best order of POIs between
 * a fixed start and end city.
 *
 * This approach is generally more efficient than permutation for a moderate number of POIs.
 */
public class DynamicProgrammingPoiOptimizerService implements PoiOptimizerService {

    private static final String ALGORITHM_NAME = "Dynamic Programming (Held-Karp variant)";
    private final RoadNetwork roadNetwork;
    private final PathfindingService fallbackPathfinder;

    public DynamicProgrammingPoiOptimizerService(RoadNetwork roadNetwork, PathfindingService pathfindingServiceForNoPoiCase) {
        this.roadNetwork = roadNetwork;
        this.fallbackPathfinder = pathfindingServiceForNoPoiCase;
    }

    /**
     * THIS IS THE OLD METHOD. It should be deprecated or adapted.
     * For now, it will delegate to a simple path calculation if POIs are empty,
     * otherwise, it will throw UnsupportedOperationException because it expects precomputed distances.
     */
    @Deprecated
    @Override
    public OptimizerResult findBestPoiOrder(
            RoadNetwork roadNetwork,
            City startCity,
            City endCity,
            List<City> poisToVisit,
            boolean useTimeout,
            long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        if (poisToVisit == null || poisToVisit.isEmpty()) {
            if (this.fallbackPathfinder == null) {
                 throw new IllegalStateException("FallbackPathfinder is null, cannot calculate path for 0 POIs using the old API.");
            }
            PathfindingService.PathResult directPathResult = fallbackPathfinder.findShortestPath(roadNetwork, startCity, endCity, Collections.emptyList(), useTimeout, timeoutMillis);
            long endTime = System.currentTimeMillis();
             if (directPathResult.isTimedOut()) {
                 return OptimizerResult.timedOut(ALGORITHM_NAME + " (Fallback)", endTime - startTime);
             }
             if (directPathResult.getTotalDistance() == Double.POSITIVE_INFINITY) {
                 return OptimizerResult.empty(ALGORITHM_NAME + " (Fallback)");
             }
            return new OptimizerResult(Collections.emptyList(), directPathResult.getTotalDistance(), endTime - startTime, false, ALGORITHM_NAME + " (Fallback)");
        }
        System.err.println("Warning: Deprecated findBestPoiOrder called on DP optimizer without precomputed distances for POIs. This is not supported.");
        throw new UnsupportedOperationException("DynamicProgrammingPoiOptimizerService with POIs requires precomputed distances via the new API method.");
    }

    @Override
    public OptimizerResult findBestPoiOrder(
            City startCity, City endCity, List<City> poisToVisitOriginal, // poisToVisitOriginal is the list of pure POIs
            Set<City> S, /* The full set of nodes (start, POIs, end) used for precomputation */
            double[][] shortestDistances, /* Precomputed: shortestDistances[idxInS1][idxInS2] */
            Map<City, Integer> nodeToIndexInS, /* Maps City from S to its index in shortestDistances */
            List<City> orderedNodesInS, /* orderedNodesInS.get(i) is the city for i-th row/col in shortestDistances */
            boolean useTimeout, long timeoutMillis) {

        final long startTimeNanos = System.nanoTime();
        final AtomicBoolean timeoutFlag = new AtomicBoolean(false);
        final long deadlineNanos = useTimeout ? startTimeNanos + timeoutMillis * 1_000_000 : Long.MAX_VALUE;

        // Filter out start/end cities from poisToVisitOriginal to get the actual list of intermediate POIs for DP states
        List<City> purePois = new ArrayList<>();
        if (poisToVisitOriginal != null) {
            for (City p : poisToVisitOriginal) {
                if (!p.equals(startCity) && !p.equals(endCity)) {
                    purePois.add(p);
                }
            }
        }

        // Handle 0 pure POIs case: direct path from start to end
        if (purePois.isEmpty()) {
            Integer startIndex = nodeToIndexInS.get(startCity);
            Integer endIndex = nodeToIndexInS.get(endCity);
            if (startIndex == null || endIndex == null) {
                return OptimizerResult.empty(ALGORITHM_NAME + " (Error: Start/End not in S)");
            }
            double dist = shortestDistances[startIndex][endIndex];
            final double durationMillis = (System.nanoTime() - startTimeNanos) / 1_000_000.0;
            return new OptimizerResult(Collections.emptyList(), dist, durationMillis, false, ALGORITHM_NAME + " (0 POIs)");
        }

        int K = purePois.size(); // Number of intermediate POIs for DP

        // dp[mask][i] = cost to visit POIs in mask, ending at purePois.get(i)
        double[][] dp = new double[1 << K][K];
        // parent[mask][i] = previous POI index in purePois list for path to purePois.get(i) with mask
        int[][] parent = new int[1 << K][K];

        for (double[] row : dp) Arrays.fill(row, Double.POSITIVE_INFINITY);
        for (int[] row : parent) Arrays.fill(row, -1);

        Integer startIndexInS = nodeToIndexInS.get(startCity);
        if (startIndexInS == null) return OptimizerResult.empty(ALGORITHM_NAME + " (Error: Start City not in S map)");

        // Base cases: from startCity to each pure POI k
        for (int k = 0; k < K; k++) {
            if (checkTimeout(deadlineNanos, timeoutFlag)) return OptimizerResult.timedOut(ALGORITHM_NAME, (System.nanoTime() - startTimeNanos) / 1_000_000.0);
            City poiK = purePois.get(k);
            Integer poiKIndexInS = nodeToIndexInS.get(poiK);
            if (poiKIndexInS == null) {
                System.err.println("Error: POI " + poiK.getName() + " not found in nodeToIndexInS map.");
                continue; // Or handle more gracefully
            }
            dp[1 << k][k] = shortestDistances[startIndexInS][poiKIndexInS];
            // Parent for base case is implicitly the start node, so -1 (or a special value) is fine for parent[1<<k][k]
        }

        // DP transitions
        for (int mask = 1; mask < (1 << K); mask++) {
            if (checkTimeout(deadlineNanos, timeoutFlag)) return OptimizerResult.timedOut(ALGORITHM_NAME, (System.nanoTime() - startTimeNanos) / 1_000_000.0);
            for (int i = 0; i < K; i++) { // Current last POI in path for this mask is purePois.get(i)
                if ((mask & (1 << i)) != 0) { // If purePois.get(i) is in the set specified by mask
                    if (dp[mask][i] == Double.POSITIVE_INFINITY) continue; // Skip unreachable states

                    Integer poiIIndexInS = nodeToIndexInS.get(purePois.get(i));
                    if (poiIIndexInS == null) continue; // Should not happen if base cases are set up

                    for (int j = 0; j < K; j++) { // Next POI to visit is purePois.get(j)
                        if ((mask & (1 << j)) == 0) { // If purePois.get(j) is NOT in the mask yet
                            Integer poiJIndexInS = nodeToIndexInS.get(purePois.get(j));
                            if (poiJIndexInS == null) continue;

                            double distItoJ = shortestDistances[poiIIndexInS][poiJIndexInS];
                            if (distItoJ == Double.POSITIVE_INFINITY) continue; // Cannot go from i to j

                            int nextMask = mask | (1 << j);
                            if (dp[mask][i] + distItoJ < dp[nextMask][j]) {
                                dp[nextMask][j] = dp[mask][i] + distItoJ;
                                parent[nextMask][j] = i; // purePois.get(i) is predecessor of purePois.get(j)
                            }
                        }
                    }
                }
            }
        }

        // Find best path to endCity from all states where all K POIs are visited
        double minTotalDistance = Double.POSITIVE_INFINITY;
        int lastPurePoiIndex = -1; // Index in purePois list
        int finalMask = (1 << K) - 1;

        Integer endIndexInS = nodeToIndexInS.get(endCity);
        if (endIndexInS == null) return OptimizerResult.empty(ALGORITHM_NAME + " (Error: End City not in S map)");

        for (int k = 0; k < K; k++) { // k is the index of the last pure POI visited
            if (checkTimeout(deadlineNanos, timeoutFlag)) return OptimizerResult.timedOut(ALGORITHM_NAME, (System.nanoTime() - startTimeNanos) / 1_000_000.0);
            if (dp[finalMask][k] == Double.POSITIVE_INFINITY) continue;

            Integer poiKIndexInS = nodeToIndexInS.get(purePois.get(k));
            if (poiKIndexInS == null) continue;

            double distKToEnd = shortestDistances[poiKIndexInS][endIndexInS];
            if (distKToEnd == Double.POSITIVE_INFINITY) continue;

            if (dp[finalMask][k] + distKToEnd < minTotalDistance) {
                minTotalDistance = dp[finalMask][k] + distKToEnd;
                lastPurePoiIndex = k;
            }
        }

        if (lastPurePoiIndex == -1 || minTotalDistance == Double.POSITIVE_INFINITY) {
            final double durationMillisOnFailure = (System.nanoTime() - startTimeNanos) / 1_000_000.0;
            if(timeoutFlag.get()) return OptimizerResult.timedOut(ALGORITHM_NAME, durationMillisOnFailure);
            System.err.println("DP: Could not find a path visiting all POIs and reaching the end city.");
            return OptimizerResult.empty(ALGORITHM_NAME + " (No path found)");
        }

        // Reconstruct path (list of purePois in order)
        List<City> bestPurePoiOrder = new ArrayList<>();
        int currentPurePoiIndex = lastPurePoiIndex;
        int currentMask = finalMask;
        while (currentPurePoiIndex != -1) {
            bestPurePoiOrder.add(purePois.get(currentPurePoiIndex));
            int prevPurePoiIndex = parent[currentMask][currentPurePoiIndex];
            currentMask ^= (1 << currentPurePoiIndex); // Remove current POI from mask
            currentPurePoiIndex = prevPurePoiIndex;
        }
        Collections.reverse(bestPurePoiOrder);

        final double durationMillis = (System.nanoTime() - startTimeNanos) / 1_000_000.0;
        if (timeoutFlag.get()) {
            return OptimizerResult.timedOut(ALGORITHM_NAME, durationMillis);
        }
        return new OptimizerResult(bestPurePoiOrder, minTotalDistance, durationMillis, false, ALGORITHM_NAME);
    }

    /** Helper to check timeout */
    private boolean checkTimeout(long deadlineNanos, AtomicBoolean timeoutOccurred) {
        if (System.nanoTime() > deadlineNanos) {
            timeoutOccurred.set(true);
            return true;
        }
        return false;
    }
    
    // Removed old getDistance and handleFailureOrTimeout as they are not used by the new method
    // The old findBestPoiOrder (marked @Deprecated) handles its own simple failures/timeouts.

    // The existing main findBestPoiOrder that uses Maps for DP states should be removed or fully refactored.
    // For now, I am assuming the @Override for the precomputed distances is the primary one to implement.
    // The old implementation from line 45 to 199 in the original file needs to be deleted.
} 