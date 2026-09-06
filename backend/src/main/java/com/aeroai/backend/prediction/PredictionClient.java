package com.aeroai.backend.prediction;

import com.aeroai.backend.prediction.dto.PredictionRequestDto;
import com.aeroai.backend.prediction.dto.PredictionResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Section 33.2 - the ONLY place Spring Boot talks to the FastAPI ML service.
 * Never propagates a raw client exception to callers (Section 51 "FastAPI
 * fails") - returns Optional.empty() so the caller can render "Unavailable".
 */
@Component
public class PredictionClient {

    private static final Logger log = LoggerFactory.getLogger(PredictionClient.class);

    private final RestClient predictionRestClient;
    private volatile boolean lastCallSucceeded = true;

    public PredictionClient(RestClient predictionRestClient) {
        this.predictionRestClient = predictionRestClient;
    }

    public Optional<PredictionResponseDto> predict(PredictionRequestDto request) {
        try {
            PredictionResponseDto response = predictionRestClient.post()
                    .uri("/predict")
                    .body(request)
                    .retrieve()
                    .body(PredictionResponseDto.class);
            lastCallSucceeded = true;
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            lastCallSucceeded = false;
            log.warn("FastAPI /predict call failed: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    /** Performs a real (cheap) probe request - used by health-check/analytics code paths only. */
    public boolean isHealthy() {
        try {
            predictionRestClient.get().uri("/health").retrieve().toBodilessEntity();
            lastCallSucceeded = true;
            return true;
        } catch (Exception ex) {
            lastCallSucceeded = false;
            return false;
        }
    }
}
