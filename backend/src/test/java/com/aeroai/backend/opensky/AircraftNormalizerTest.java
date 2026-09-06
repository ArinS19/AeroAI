package com.aeroai.backend.opensky;

import com.aeroai.backend.flight.AircraftState;
import com.aeroai.backend.flight.FlightStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Section 24 - status transition rules unit-tested directly.
 */
class AircraftNormalizerTest {

    private final AircraftNormalizer normalizer = new AircraftNormalizer();

    private OpenSkyStateVector vector(String icao24, Boolean onGround, Double altitude, Double speed) {
        return new OpenSkyStateVector(icao24, "TEST123", "India", 1000L, 1000L,
                72.8777, 19.0760, altitude, onGround, speed, 90.0, 0.0, altitude);
    }

    @Test
    void firstSightingOnGround_isUnknownNotLanded() {
        AircraftState result = normalizer.normalize(vector("abc123", true, 0.0, 0.0), null, "mumbai", Instant.now());
        assertEquals(FlightStatus.UNKNOWN, result.getStatus(),
                "A first-ever sighting that happens to be on-ground must never be fabricated as Landed.");
    }

    @Test
    void airborne_isInFlightAndStartsSession() {
        Instant now = Instant.now();
        AircraftState result = normalizer.normalize(vector("abc123", false, 10000.0, 200.0), null, "mumbai", now);
        assertEquals(FlightStatus.IN_FLIGHT, result.getStatus());
        assertNotNull(result.getSessionStartEpoch());
        assertEquals(now.getEpochSecond(), result.getSessionStartEpoch());
    }

    @Test
    void continuingInFlightSession_preservesOriginalSessionStart() {
        AircraftState previous = normalizer.normalize(vector("abc123", false, 10000.0, 200.0), null, "mumbai", Instant.ofEpochSecond(1000));
        AircraftState next = normalizer.normalize(vector("abc123", false, 10500.0, 210.0), previous, "mumbai", Instant.ofEpochSecond(1015));
        assertEquals(previous.getSessionStartEpoch(), next.getSessionStartEpoch());
    }

    @Test
    void singleOnGroundSampleAfterInFlight_doesNotYetFlipToLanded() {
        AircraftState previous = normalizer.normalize(vector("abc123", false, 10000.0, 200.0), null, "mumbai", Instant.ofEpochSecond(1000));
        AircraftState next = normalizer.normalize(vector("abc123", true, 0.0, 5.0), previous, "mumbai", Instant.ofEpochSecond(1015));
        assertEquals(FlightStatus.IN_FLIGHT, next.getStatus(),
                "A single on-ground sample must not immediately flip status to Landed (needs 2 consecutive cycles).");
        assertEquals(1, next.getOnGroundStreak());
    }

    @Test
    void twoConsecutiveOnGroundSamplesAfterInFlight_flipsToLanded() {
        AircraftState cycle1 = normalizer.normalize(vector("abc123", false, 10000.0, 200.0), null, "mumbai", Instant.ofEpochSecond(1000));
        AircraftState cycle2 = normalizer.normalize(vector("abc123", true, 0.0, 5.0), cycle1, "mumbai", Instant.ofEpochSecond(1015));
        AircraftState cycle3 = normalizer.normalize(vector("abc123", true, 0.0, 2.0), cycle2, "mumbai", Instant.ofEpochSecond(1030));
        assertEquals(FlightStatus.LANDED, cycle3.getStatus());
        assertNull(cycle3.getSessionStartEpoch(), "Session must end once Landed.");
    }

    @Test
    void alreadyLanded_staysLandedWhileOnGround() {
        AircraftState cycle1 = normalizer.normalize(vector("abc123", false, 10000.0, 200.0), null, "mumbai", Instant.ofEpochSecond(1000));
        AircraftState cycle2 = normalizer.normalize(vector("abc123", true, 0.0, 5.0), cycle1, "mumbai", Instant.ofEpochSecond(1015));
        AircraftState cycle3 = normalizer.normalize(vector("abc123", true, 0.0, 2.0), cycle2, "mumbai", Instant.ofEpochSecond(1030));
        AircraftState cycle4 = normalizer.normalize(vector("abc123", true, 0.0, 0.0), cycle3, "mumbai", Instant.ofEpochSecond(1045));
        assertEquals(FlightStatus.LANDED, cycle4.getStatus());
    }

    @Test
    void backToAirborneAfterLanded_startsNewSession() {
        AircraftState cycle1 = normalizer.normalize(vector("abc123", false, 10000.0, 200.0), null, "mumbai", Instant.ofEpochSecond(1000));
        AircraftState cycle2 = normalizer.normalize(vector("abc123", true, 0.0, 5.0), cycle1, "mumbai", Instant.ofEpochSecond(1015));
        AircraftState landed = normalizer.normalize(vector("abc123", true, 0.0, 2.0), cycle2, "mumbai", Instant.ofEpochSecond(1030));
        AircraftState airborneAgain = normalizer.normalize(vector("abc123", false, 500.0, 60.0), landed, "mumbai", Instant.ofEpochSecond(5000));
        assertEquals(FlightStatus.IN_FLIGHT, airborneAgain.getStatus());
        assertEquals(5000L, airborneAgain.getSessionStartEpoch());
    }
}
