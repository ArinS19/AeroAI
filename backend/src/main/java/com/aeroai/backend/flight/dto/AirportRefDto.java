package com.aeroai.backend.flight.dto;

public record AirportRefDto(Long id, String name, String iata, String icao, double latitude, double longitude) {
}
