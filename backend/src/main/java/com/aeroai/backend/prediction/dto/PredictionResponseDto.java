package com.aeroai.backend.prediction.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Exact mirror of the FastAPI ML service's `POST /predict` response
 * (IMPLEMENT_v2.md Section 33.1).
 */
public record PredictionResponseDto(
        @JsonProperty("predicted_delay_minutes") double predictedDelayMinutes,
        @JsonProperty("delay_probability") double delayProbability,
        @JsonProperty("delay_over_15_minutes") boolean delayOver15Minutes,
        @JsonProperty("category") String category
) {
}
