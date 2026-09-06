package com.aeroai.backend.flight.dto;

import java.time.Instant;

/** Section 25.1 - current position block of a flight detail response. */
public record CurrentPositionDto(
        Double latitude,
        Double longitude,
        Double altitude,
        Double speed,
        Double heading,
        Double verticalRate,
        Instant lastUpdate
) {
}
