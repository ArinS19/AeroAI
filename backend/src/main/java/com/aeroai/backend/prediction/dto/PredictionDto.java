package com.aeroai.backend.prediction.dto;

/**
 * API-facing prediction result (Section 34). `available=false` means
 * "AI Delay Prediction: Unavailable" (Section 34.2) - never a fabricated guess.
 */
public record PredictionDto(
        boolean available,
        Double predictedDelayMinutes,
        Double delayProbability,
        Boolean delayOver15Minutes,
        String category,
        String disclaimer
) {
    private static final String DISCLAIMER = "Machine-learning estimate, not an airline guarantee.";

    public static PredictionDto unavailable() {
        return new PredictionDto(false, null, null, null, null, DISCLAIMER);
    }

    public static PredictionDto of(PredictionResponseDto response) {
        return new PredictionDto(true, response.predictedDelayMinutes(), response.delayProbability(),
                response.delayOver15Minutes(), response.category(), DISCLAIMER);
    }
}
