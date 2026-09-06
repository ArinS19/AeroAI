package com.aeroai.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Weather cache (Section 31) - per-location TTL cache so many concurrent
 * users viewing the same airport don't each trigger a fresh Open-Meteo call.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String WEATHER_CACHE = "weatherCache";

    @Bean
    public CacheManager cacheManager(AppProperties appProperties) {
        CaffeineCacheManager manager = new CaffeineCacheManager(WEATHER_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(appProperties.getWeather().getCacheTtlMinutes()))
                .maximumSize(1000));
        return manager;
    }
}
