package com.aeroai.backend.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoMathTest {

    @Test
    void haversine_knownDistance_mumbaiToDelhi_isApproximatelyCorrect() {
        // Real-world great-circle distance Mumbai <-> Delhi is ~1150km (independently verifiable).
        double km = GeoMath.haversineKm(19.0760, 72.8777, 28.6139, 77.2090);
        assertTrue(km > 1100 && km < 1200, "Expected ~1150km, got " + km);
    }

    @Test
    void haversine_samePoint_isZero() {
        double km = GeoMath.haversineKm(19.0, 72.0, 19.0, 72.0);
        assertEquals(0.0, km, 0.0001);
    }

    @Test
    void tcpa_headOnCollisionCourse_isPositiveAndWithinHorizon() {
        // A at origin moving east, B 10km east of A moving west (head-on).
        GeoMath.Vector2 relativePosition = new GeoMath.Vector2(10_000, 0); // B is 10km east of A
        GeoMath.Vector2 relativeVelocity = new GeoMath.Vector2(-500, 0); // B approaching A at 500 m/s relative
        double tcpa = GeoMath.tcpaSeconds(relativePosition, relativeVelocity);
        assertEquals(20.0, tcpa, 0.001);
    }

    @Test
    void tcpa_divergingTracks_isNegative() {
        GeoMath.Vector2 relativePosition = new GeoMath.Vector2(10_000, 0);
        GeoMath.Vector2 relativeVelocity = new GeoMath.Vector2(500, 0); // moving further apart
        double tcpa = GeoMath.tcpaSeconds(relativePosition, relativeVelocity);
        assertTrue(tcpa < 0);
    }

    @Test
    void tcpa_parallelIdenticalVelocity_isUndefinedNaN() {
        GeoMath.Vector2 relativePosition = new GeoMath.Vector2(5_000, 5_000);
        GeoMath.Vector2 relativeVelocity = new GeoMath.Vector2(0, 0); // no relative motion
        double tcpa = GeoMath.tcpaSeconds(relativePosition, relativeVelocity);
        assertTrue(Double.isNaN(tcpa));
    }

    @Test
    void closureRate_approaching_isNegative() {
        GeoMath.Vector2 relativePosition = new GeoMath.Vector2(10_000, 0);
        GeoMath.Vector2 relativeVelocity = new GeoMath.Vector2(-500, 0);
        double closureRate = GeoMath.closureRateMs(relativePosition, relativeVelocity);
        assertTrue(closureRate < 0);
    }

    @Test
    void closureRate_departing_isPositive() {
        GeoMath.Vector2 relativePosition = new GeoMath.Vector2(10_000, 0);
        GeoMath.Vector2 relativeVelocity = new GeoMath.Vector2(500, 0);
        double closureRate = GeoMath.closureRateMs(relativePosition, relativeVelocity);
        assertTrue(closureRate > 0);
    }

    @Test
    void velocityFromSpeedHeading_northHeading_isPureNorthComponent() {
        GeoMath.Vector2 v = GeoMath.velocityFromSpeedHeading(100, 0);
        assertEquals(0.0, v.east(), 0.001);
        assertEquals(100.0, v.north(), 0.001);
    }

    @Test
    void velocityFromSpeedHeading_eastHeading_isPureEastComponent() {
        GeoMath.Vector2 v = GeoMath.velocityFromSpeedHeading(100, 90);
        assertEquals(100.0, v.east(), 0.001);
        assertEquals(0.0, v.north(), 0.001);
    }
}
