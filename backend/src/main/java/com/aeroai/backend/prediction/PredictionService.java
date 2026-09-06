package com.aeroai.backend.prediction;

import com.aeroai.backend.config.AppProperties;
import com.aeroai.backend.prediction.dto.PredictionDto;
import com.aeroai.backend.prediction.dto.PredictionRequestDto;
import com.aeroai.backend.prediction.dto.PredictionResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Section 35 - ML refresh/cache. Predictions are cached in Redis
 * (`prediction:{id}`, TTL PREDICTION_CACHE_TTL_SECONDS) and only recomputed
 * on cache miss - never re-triggered by the ~15s live position pipeline
 * (Rule 7: these are deliberately separate code paths).
 */
@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);
    private static final String CACHE_KEY_PREFIX = "prediction:";

    private final PredictionClient predictionClient;
    private final PredictionRepository predictionRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public PredictionService(PredictionClient predictionClient,
                              PredictionRepository predictionRepository,
                              StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper,
                              AppProperties appProperties) {
        this.predictionClient = predictionClient;
        this.predictionRepository = predictionRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    /**
     * Returns the cached prediction for {@code identifier} if present and
     * fresh; otherwise calls FastAPI, persists a durable record, caches the
     * result, and returns it. Returns an "unavailable" DTO on any failure
     * (missing inputs, FastAPI down, etc.) - never propagates an exception.
     */
    public PredictionDto getOrCompute(String identifier, PredictionRequestDto request) {
        if (request == null) {
            return PredictionDto.unavailable();
        }

        String cacheKey = CACHE_KEY_PREFIX + identifier;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, PredictionDto.class);
            }
        } catch (Exception ex) {
            log.debug("Redis prediction cache read failed for {}: {}", identifier, ex.getMessage());
        }

        Optional<PredictionResponseDto> response = predictionClient.predict(request);
        PredictionDto result = response.map(PredictionDto::of).orElseGet(PredictionDto::unavailable);

        if (result.available()) {
            try {
                predictionRepository.save(new Prediction(identifier, result.predictedDelayMinutes(),
                        result.delayProbability(), result.category()));
            } catch (Exception ex) {
                log.warn("Failed to persist prediction record for {}: {}", identifier, ex.getMessage());
            }
        }

        cacheResult(cacheKey, result);
        return result;
    }

    private void cacheResult(String cacheKey, PredictionDto result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofSeconds(appProperties.getPrediction().getCacheTtlSeconds()));
        } catch (JsonProcessingException | RuntimeException ex) {
            log.debug("Failed to cache prediction result: {}", ex.getMessage());
        }
    }
}
