package com.aeroai.backend.auth.dto;

import java.time.Instant;

public record UserDto(
        Long id,
        String email,
        Instant createdAt,
        long trackedFlightCount
) {
}
