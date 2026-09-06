package com.aeroai.backend.airport.dto;

public record AirportDto(
        Long id,
        String name,
        String iata,
        String icao,
        String city,
        String location,
        String country,
        double latitude,
        double longitude
) {
}
