package com.aeroai.backend.airspace.dto;

import java.util.List;

/**
 * Section 13.2.2 city airspace model. Used for both the list (Section 46
 * `GET /api/airspaces`) and detail (`GET /api/airspaces/{cityId}`) endpoints -
 * the detail response additionally includes nearbyAirports.
 */
public record AirspaceDto(
        String cityId,
        String cityName,
        String country,
        double latitude,
        double longitude,
        int defaultZoom,
        double lamin,
        double lomin,
        double lamax,
        double lomax,
        String displayRegion,
        List<String> aliases,
        List<NearbyAirportRef> nearbyAirports
) {
    public record NearbyAirportRef(Long id, String name, String iata, String icao, double latitude, double longitude) {
    }
}
