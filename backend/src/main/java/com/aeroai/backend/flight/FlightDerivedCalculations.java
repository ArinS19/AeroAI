package com.aeroai.backend.flight;

import com.aeroai.backend.common.util.GeoMath;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Section 25 - deterministic derived-flight-data math (distance travelled,
 * distance remaining, AeroAI ETA). Pure functions, unit-testable
 * independently of Redis/Postgres wiring.
 */
public final class FlightDerivedCalculations {

    /** Section 25.4 - below this ground speed while airborne, treat the ETA input as noisy/invalid. */
    public static final double MIN_PLAUSIBLE_SPEED_KMH = 50.0;

    private FlightDerivedCalculations() {
    }

    /**
     * Section 25.2 - sums haversine distance across consecutive recorded
     * points. Returns null (Unavailable) if fewer than 2 points exist.
     */
    public static Double distanceTravelledKm(List<double[]> orderedLatLonPoints) {
        if (orderedLatLonPoints == null || orderedLatLonPoints.size() < 2) {
            return null;
        }
        double total = 0.0;
        for (int i = 1; i < orderedLatLonPoints.size(); i++) {
            double[] prev = orderedLatLonPoints.get(i - 1);
            double[] curr = orderedLatLonPoints.get(i);
            total += GeoMath.haversineKm(prev[0], prev[1], curr[0], curr[1]);
        }
        return total;
    }

    /**
     * Section 25.3 - great-circle distance from current position to
     * destination, rounded to the nearest 10km as a visual "estimate" cue.
     */
    public static double distanceRemainingKm(double lat, double lon, double destLat, double destLon) {
        double raw = GeoMath.haversineKm(lat, lon, destLat, destLon);
        return Math.round(raw / 10.0) * 10.0;
    }

    /** Section 25.4 - effective speed for ETA: average of recent valid samples, converted m/s -> km/h. */
    public static Double effectiveSpeedKmh(List<Double> recentSpeedSamplesMs) {
        if (recentSpeedSamplesMs == null || recentSpeedSamplesMs.isEmpty()) {
            return null;
        }
        double sum = 0;
        int count = 0;
        for (Double sample : recentSpeedSamplesMs) {
            if (sample != null && Double.isFinite(sample)) {
                sum += sample;
                count++;
            }
        }
        if (count == 0) {
            return null;
        }
        double avgMs = sum / count;
        return avgMs * 3.6;
    }

    /**
     * Section 25.4 - AeroAI ETA. Returns null (Unavailable) if the effective
     * speed is missing or below the sane minimum threshold while airborne.
     */
    public static Instant estimatedArrival(double remainingDistanceKm, Double effectiveSpeedKmh, Instant now) {
        if (effectiveSpeedKmh == null || effectiveSpeedKmh < MIN_PLAUSIBLE_SPEED_KMH) {
            return null;
        }
        double etaHours = remainingDistanceKm / effectiveSpeedKmh;
        return now.plus(Duration.ofMillis((long) (etaHours * 3600_000)));
    }
}
