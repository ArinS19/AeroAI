package com.aeroai.backend.tracking.dto;

import java.time.Instant;

/**
 * Section 42 - My Flights row. `dataAvailable=false` renders
 * "Data temporarily unavailable" (the tracked aircraft has dropped out of
 * live state, but the tracking relationship itself is preserved).
 */
public record TrackedFlightDto(
        Long id,
        String icao24,
        String callsign,
        String status,
        Double predictedDelayMinutes,
        String delayCategory,
        Instant lastUpdate,
        boolean dataAvailable
) {
}
