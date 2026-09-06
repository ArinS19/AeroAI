package com.aeroai.backend.openmeteo;

import com.aeroai.backend.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Thin wrapper around the Open-Meteo current-weather API (Section 31).
 * Centralizes the external call so the rest of the codebase never sees raw
 * HTTP/JSON exceptions (Section 11) - failures degrade to Optional.empty().
 */
@Component
public class OpenMeteoClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoClient.class);

    private static final String CURRENT_PARAMS =
            "temperature_2m,relative_humidity_2m,precipitation,weather_code,cloud_cover,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m";

    private final RestClient restClient;
    private final AppProperties appProperties;
    private volatile boolean lastCallSucceeded = true;

    public OpenMeteoClient(RestClient openMeteoRestClient, AppProperties appProperties) {
        this.restClient = openMeteoRestClient;
        this.appProperties = appProperties;
    }

    public Optional<OpenMeteoResponse> fetchCurrent(double latitude, double longitude) {
        String url = appProperties.getWeather().getOpenMeteoBaseUrl()
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&current=" + CURRENT_PARAMS;
        try {
            OpenMeteoResponse response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(OpenMeteoResponse.class);
            lastCallSucceeded = true;
            return Optional.ofNullable(response);
        } catch (Exception ex) {
            lastCallSucceeded = false;
            log.warn("Open-Meteo request failed for ({}, {}): {}", latitude, longitude, ex.getMessage());
            return Optional.empty();
        }
    }

    /** Section 44.4 - lightweight status derived from the most recent real call, not a fresh probe request. */
    public boolean isLastCallHealthy() {
        return lastCallSucceeded;
    }
}
