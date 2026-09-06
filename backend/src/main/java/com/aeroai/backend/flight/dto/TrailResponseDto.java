package com.aeroai.backend.flight.dto;

import java.util.List;

public record TrailResponseDto(String icao24, int minutes, boolean available, List<TrailPoint> points) {
    public static TrailResponseDto unavailable(String icao24, int minutes) {
        return new TrailResponseDto(icao24, minutes, false, List.of());
    }
}
