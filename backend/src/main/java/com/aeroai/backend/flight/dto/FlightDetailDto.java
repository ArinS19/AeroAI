package com.aeroai.backend.flight.dto;

import com.aeroai.backend.prediction.dto.PredictionDto;
import com.aeroai.backend.weather.dto.WeatherDto;

import java.time.Instant;
import java.util.List;

/**
 * Section 28 - Flight details panel/page DTO. Every derived field
 * (distanceTravelledKm, distanceRemainingKm, estimatedArrival) is computed
 * exactly once, server-side (Section 25) - the frontend renders it as-is.
 */
public record FlightDetailDto(
        String icao24,
        String callsign,
        String airline,
        String aircraftRegistration,
        String aircraftType,
        AirportRefDto origin,
        AirportRefDto destination,
        Instant departureTime,
        Instant arrivalTime,
        String status,
        CurrentPositionDto currentPosition,
        Double distanceTravelledKm,
        Double distanceRemainingKm,
        Instant estimatedArrival,
        boolean trailAvailable,
        WeatherDto weather,
        PredictionDto prediction,
        List<ConflictRiskDto> conflicts,
        boolean tracked
) {
}
