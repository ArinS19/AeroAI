package com.aeroai.backend.openmeteo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps the subset of Open-Meteo's `current` response block AeroAI uses
 * (Section 31). Any field Open-Meteo's free tier doesn't return for a given
 * location stays null and is surfaced as Unavailable further up the stack.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoResponse(
        Double latitude,
        Double longitude,
        @JsonProperty("current") Current current
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Current(
            @JsonProperty("temperature_2m") Double temperature2m,
            @JsonProperty("relative_humidity_2m") Double relativeHumidity2m,
            @JsonProperty("precipitation") Double precipitation,
            @JsonProperty("weather_code") Integer weatherCode,
            @JsonProperty("cloud_cover") Double cloudCover,
            @JsonProperty("pressure_msl") Double pressureMsl,
            @JsonProperty("surface_pressure") Double surfacePressure,
            @JsonProperty("wind_speed_10m") Double windSpeed10m,
            @JsonProperty("wind_direction_10m") Double windDirection10m
    ) {
    }
}
