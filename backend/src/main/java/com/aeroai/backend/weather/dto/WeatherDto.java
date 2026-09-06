package com.aeroai.backend.weather.dto;

/**
 * Section 31 - normalized current-weather snapshot. Every field is
 * independently nullable; null renders as "Unavailable" in the UI, never a
 * fabricated/default value (Section 3.1).
 */
public record WeatherDto(
        Double temperatureCelsius,
        String conditionDescription,
        Double windSpeedKmh,
        Double windDirectionDegrees,
        Double humidityPercent,
        Double visibilityKm,
        Double pressureHpa,
        Double cloudCoverPercent,
        Double precipitationMm
) {
    public static WeatherDto unavailable() {
        return new WeatherDto(null, null, null, null, null, null, null, null, null);
    }
}
