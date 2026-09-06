package com.aeroai.backend.flight.dto;

import java.time.Instant;

/**
 * Section 27 - Flights page table row. Any column may be null/"Unavailable"
 * per-row without the column itself being removed (Section 3.1).
 */
public record FlightListItemDto(
        String icao24,
        String callsign,
        String airline,
        AirportRefDto origin,
        AirportRefDto destination,
        String status,
        Double altitude,
        Double speed,
        Instant lastUpdate
) {
}
