package com.aeroai.backend.flight;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightDerivedCalculationsTest {

    @Test
    void distanceTravelled_fewerThanTwoPoints_isUnavailable() {
        assertNull(FlightDerivedCalculations.distanceTravelledKm(List.of(new double[]{19.0, 72.0})));
        assertNull(FlightDerivedCalculations.distanceTravelledKm(null));
    }

    @Test
    void distanceTravelled_sumsSegmentsCorrectly() {
        // Mumbai -> Pune -> Mumbai: two legs, each ~120-150km, sum should reflect both legs (not the shortcut).
        List<double[]> points = List.of(
                new double[]{19.0760, 72.8777}, // Mumbai
                new double[]{18.5204, 73.8567}, // Pune
                new double[]{19.0760, 72.8777}  // back to Mumbai
        );
        Double total = FlightDerivedCalculations.distanceTravelledKm(points);
        assertNotNull(total);
        assertTrue(total > 200 && total < 320, "Expected ~240-280km round trip, got " + total);
    }

    @Test
    void distanceRemaining_roundsToNearest10km() {
        double km = FlightDerivedCalculations.distanceRemainingKm(19.0760, 72.8777, 28.6139, 77.2090);
        assertEquals(0.0, km % 10.0, 0.0001);
    }

    @Test
    void estimatedArrival_belowMinimumSpeed_isUnavailable() {
        Instant result = FlightDerivedCalculations.estimatedArrival(500, 30.0, Instant.now());
        assertNull(result);
    }

    @Test
    void estimatedArrival_nullSpeed_isUnavailable() {
        assertNull(FlightDerivedCalculations.estimatedArrival(500, null, Instant.now()));
    }

    @Test
    void estimatedArrival_plausibleSpeed_returnsFutureTimestamp() {
        Instant now = Instant.now();
        Instant eta = FlightDerivedCalculations.estimatedArrival(500, 500.0, now); // 1 hour at 500km/h
        assertNotNull(eta);
        assertTrue(eta.isAfter(now));
        long minutes = (eta.getEpochSecond() - now.getEpochSecond()) / 60;
        assertEquals(60L, minutes);
    }

    @Test
    void effectiveSpeedKmh_averagesValidSamplesAndConvertsUnits() {
        Double avgKmh = FlightDerivedCalculations.effectiveSpeedKmh(List.of(100.0, 200.0));
        assertNotNull(avgKmh);
        assertEquals(150.0 * 3.6, avgKmh, 0.001);
    }

    @Test
    void effectiveSpeedKmh_emptyList_isUnavailable() {
        assertNull(FlightDerivedCalculations.effectiveSpeedKmh(List.of()));
        assertNull(FlightDerivedCalculations.effectiveSpeedKmh(null));
    }
}
