package com.aeroai.backend.prediction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Exact mirror of the FastAPI ML service's `POST /predict` Pydantic input
 * model (IMPLEMENT_v2.md Section 33). Field names/types/casing must match
 * byte-for-byte - this is the one and only place this mapping happens.
 */
public record PredictionRequestDto(
        @JsonProperty("from_airport") String fromAirport,
        @JsonProperty("to_airport") String toAirport,
        @JsonProperty("airline") String airline,
        @JsonProperty("distance") double distance,
        @JsonProperty("passenger_load_factor") Double passengerLoadFactor,
        @JsonProperty("airline_rating") Double airlineRating,
        @JsonProperty("airport_rating") Double airportRating,
        @JsonProperty("market_share") Double marketShare,
        @JsonProperty("scheduled_date") String scheduledDate,
        @JsonProperty("scheduled_departure") String scheduledDeparture,
        @JsonProperty("scheduled_arrival") String scheduledArrival,
        @JsonProperty("windspeed") Double windspeed,
        @JsonProperty("weather_description") String weatherDescription,
        @JsonProperty("precipitation") Double precipitation,
        @JsonProperty("humidity") Double humidity,
        @JsonProperty("visibility") Double visibility,
        @JsonProperty("pressure") Double pressure,
        @JsonProperty("cloudcover") Double cloudcover
) {
}
