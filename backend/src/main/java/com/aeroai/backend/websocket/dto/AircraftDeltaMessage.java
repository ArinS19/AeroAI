package com.aeroai.backend.websocket.dto;

import com.aeroai.backend.flight.dto.AircraftDto;

import java.time.Instant;
import java.util.List;

/**
 * Section 47 - binding WebSocket delta wire format, published to
 * `/topic/aircraft/{airspaceId}`. `added`/`updated` use the full AircraftDto
 * shape; `removed` is a flat array of ICAO24 strings.
 */
public record AircraftDeltaMessage(
        Instant timestamp,
        List<AircraftDto> added,
        List<AircraftDto> updated,
        List<String> removed
) {
}
