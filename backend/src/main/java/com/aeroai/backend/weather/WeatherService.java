package com.aeroai.backend.weather;

import com.aeroai.backend.config.CacheConfig;
import com.aeroai.backend.openmeteo.OpenMeteoClient;
import com.aeroai.backend.openmeteo.OpenMeteoResponse;
import com.aeroai.backend.weather.dto.WeatherDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Section 31 - weather is supporting data only, normalized from Open-Meteo
 * and cached per-location (Section 53) so it isn't refetched on every
 * concurrent request for the same airport/flight position.
 */
@Service
public class WeatherService {

    private final OpenMeteoClient openMeteoClient;

    public WeatherService(OpenMeteoClient openMeteoClient) {
        this.openMeteoClient = openMeteoClient;
    }

    @Cacheable(cacheNames = CacheConfig.WEATHER_CACHE,
            key = "T(String).format('%.2f,%.2f', #latitude, #longitude)")
    public WeatherDto getCurrentWeather(double latitude, double longitude) {
        return openMeteoClient.fetchCurrent(latitude, longitude)
                .map(this::normalize)
                .orElse(WeatherDto.unavailable());
    }

    private WeatherDto normalize(OpenMeteoResponse response) {
        OpenMeteoResponse.Current current = response.current();
        if (current == null) {
            return WeatherDto.unavailable();
        }
        Double pressure = current.pressureMsl() != null ? current.pressureMsl() : current.surfacePressure();
        return new WeatherDto(
                current.temperature2m(),
                WmoWeatherCodes.describe(current.weatherCode()),
                current.windSpeed10m(),
                current.windDirection10m(),
                current.relativeHumidity2m(),
                null, // Open-Meteo's free `current` block does not include visibility.
                pressure,
                current.cloudCover(),
                current.precipitation()
        );
    }
}
