package com.aeroai.backend.alert.dto;

import java.time.Instant;

public record AlertDto(
        Long id,
        Long trackedFlightId,
        String type,
        String message,
        Instant createdAt,
        boolean read
) {
}
