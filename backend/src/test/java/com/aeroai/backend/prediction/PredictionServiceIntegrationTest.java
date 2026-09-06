package com.aeroai.backend.prediction;

import com.aeroai.backend.prediction.dto.PredictionDto;
import com.aeroai.backend.prediction.dto.PredictionRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Section 75/77/83 - Spring Boot -> FastAPI ML integration. Exercises the
 * REAL local FastAPI service (must be running at aeroai.ml.service-url,
 * default http://127.0.0.1:8000) rather than mocking the HTTP call, plus
 * Section 51's "FastAPI fails" graceful-degradation path.
 */
@SpringBootTest
@ActiveProfiles("test")
class PredictionServiceIntegrationTest {

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private PredictionClient predictionClient;

    private PredictionRequestDto sampleRequest() {
        return new PredictionRequestDto(
                "BOM", "DEL", "Air India", 1150.0,
                0.85, 3.5, 4.0, 0.2,
                "2026-09-10", "14:30", "16:45",
                12.5, "Clear", 0.0, 60.0, 10.0, 1013.0, 20.0
        );
    }

    @Test
    void realFastApiHealthCheck_succeeds() {
        assumeMlServiceReachable();
        assertTrue(predictionClient.isHealthy(), "FastAPI /health must respond ok when the ML service is running.");
    }

    @Test
    void getOrCompute_realFastApiCall_returnsPlausiblePrediction() {
        assumeMlServiceReachable();
        String identifier = "tst" + (System.nanoTime() % 1_000_000); // must fit VARCHAR(24)
        PredictionDto result = predictionService.getOrCompute(identifier, sampleRequest());

        assertTrue(result.available(), "Expected a real prediction from the running FastAPI service.");
        assertNotNull(result.predictedDelayMinutes());
        assertTrue(result.predictedDelayMinutes() >= 0, "Section 32.3 - predicted delay must be clamped non-negative.");
        assertNotNull(result.delayProbability());
        assertTrue(result.delayProbability() >= 0.0 && result.delayProbability() <= 1.0);
        assertNotNull(result.category());
        assertTrue(result.category().equals("Low") || result.category().equals("Moderate") || result.category().equals("High"));
    }

    @Test
    void getOrCompute_missingRequiredInputs_isUnavailableWithoutCallingFastApi() {
        PredictionDto result = predictionService.getOrCompute("test-null-request", null);
        assertFalse(result.available());
        assertNull(result.predictedDelayMinutes());
    }

    @Test
    void predictionClient_unreachableService_degradesGracefullyToEmpty() {
        // Section 51 "FastAPI fails" - point at a definitely-unreachable port
        // and confirm the client returns Optional.empty() rather than throwing.
        RestClient unreachableClient = RestClient.builder().baseUrl("http://127.0.0.1:1").build();
        PredictionClient brokenClient = new PredictionClient(unreachableClient);
        assertTrue(brokenClient.predict(sampleRequest()).isEmpty());
        assertFalse(brokenClient.isHealthy());
    }

    private void assumeMlServiceReachable() {
        org.junit.jupiter.api.Assumptions.assumeTrue(predictionClient.isHealthy(),
                "Skipping: FastAPI ML service is not reachable at the configured aeroai.ml.service-url.");
    }
}
