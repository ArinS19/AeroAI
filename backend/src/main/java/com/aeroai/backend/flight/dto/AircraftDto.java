package com.aeroai.backend.flight.dto;

import java.time.Instant;

/**
 * Section 47 - the exact per-aircraft shape used both in REST list
 * responses and WebSocket added/updated delta entries. Units are exactly
 * OpenSky-native: altitude in meters, speed/verticalRate in m/s, heading in
 * degrees clockwise from true north. Any null field means the underlying
 * OpenSky value was unavailable (Section 18.1) - never defaulted to 0/false.
 */
public record AircraftDto(
        String icao24,
        String callsign,
        Double latitude,
        Double longitude,
        Double altitude,
        Double speed,
        Double heading,
        Double verticalRate,
        Boolean onGround,
        String status,
        Instant lastUpdate
) {
}
