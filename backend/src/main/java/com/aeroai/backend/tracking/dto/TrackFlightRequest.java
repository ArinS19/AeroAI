package com.aeroai.backend.tracking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TrackFlightRequest(
        @NotBlank @Pattern(regexp = "^[0-9a-fA-F]{6}$", message = "must be a 6-character ICAO24 hex identifier") String icao24,
        String callsign
) {
}
