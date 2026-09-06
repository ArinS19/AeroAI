package com.aeroai.backend.airspace;

import com.aeroai.backend.airport.Airport;
import com.aeroai.backend.airport.AirportService;
import com.aeroai.backend.airspace.dto.AirspaceDto;
import com.aeroai.backend.common.exception.NotFoundException;
import com.aeroai.backend.common.exception.ValidationException;
import com.aeroai.backend.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Section 13.2 - major-city airspace reference data. Reference-data driven
 * per Rule 9: adding a supported city is a seed/migration change, never a
 * frontend component rewrite.
 */
@Service
@Transactional(readOnly = true)
public class AirspaceService {

    private static final int MAX_QUERY_LENGTH = 100;
    private static final int NEARBY_AIRPORT_LIMIT = 8;

    private final AirspaceRepository airspaceRepository;
    private final AirportService airportService;
    private final AppProperties appProperties;

    public AirspaceService(AirspaceRepository airspaceRepository, AirportService airportService, AppProperties appProperties) {
        this.airspaceRepository = airspaceRepository;
        this.airportService = airportService;
        this.appProperties = appProperties;
    }

    public List<AirspaceDto> list() {
        return airspaceRepository.findAll().stream()
                .sorted(Comparator.comparing(Airspace::getCityName))
                .map(a -> toDto(a, false))
                .toList();
    }

    public List<AirspaceDto> search(String query) {
        String trimmed = validateQuery(query);
        if (trimmed.isEmpty()) {
            return list();
        }
        return airspaceRepository.search(trimmed).stream()
                .map(a -> toDto(a, false))
                .toList();
    }

    public AirspaceDto getBySlug(String slug) {
        Airspace airspace = requireBySlug(slug);
        return toDto(airspace, true);
    }

    public Airspace requireBySlug(String slug) {
        return airspaceRepository.findBySlug(normalizeSlug(slug))
                .orElseThrow(() -> new NotFoundException("Airspace '" + slug + "' was not found."));
    }

    public Airspace requireDefaultAirspace() {
        String defaultSlug = appProperties.getAirspace().getDefaultCityId();
        return requireBySlug(defaultSlug);
    }

    private String normalizeSlug(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new ValidationException("Airspace id is required.");
        }
        return slug.trim().toLowerCase();
    }

    private String validateQuery(String query) {
        if (query == null) {
            return "";
        }
        String trimmed = query.trim();
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            throw new ValidationException("Search query is too long.");
        }
        return trimmed;
    }

    private AirspaceDto toDto(Airspace airspace, boolean includeNearbyAirports) {
        List<String> aliases = airspace.getAliases() == null || airspace.getAliases().isBlank()
                ? List.of()
                : List.of(airspace.getAliases().split("\\s*,\\s*"));

        List<AirspaceDto.NearbyAirportRef> nearby = includeNearbyAirports ? findNearbyAirports(airspace) : List.of();

        return new AirspaceDto(
                airspace.getSlug(),
                airspace.getCityName(),
                airspace.getCountry(),
                airspace.getLatitude(),
                airspace.getLongitude(),
                airspace.getDefaultZoom(),
                airspace.getLamin(),
                airspace.getLomin(),
                airspace.getLamax(),
                airspace.getLomax(),
                airspace.getDisplayRegion(),
                aliases,
                nearby
        );
    }

    /**
     * Section 13.2.3 - nearby airports for the selected city. No explicit
     * nearby-airport relationship table is seeded, so this derives the set
     * deterministically from real seeded airport coordinates: first by exact
     * city-name match, then (if that finds nothing) by falling inside the
     * airspace's own configured bounding box. Never invents an airport.
     */
    private List<AirspaceDto.NearbyAirportRef> findNearbyAirports(Airspace airspace) {
        List<Airport> byCity = airportService.findByCity(airspace.getCityName());
        List<Airport> candidates = byCity.isEmpty()
                ? airportService.findWithinBoundingBox(airspace.getLamin(), airspace.getLamax(), airspace.getLomin(), airspace.getLomax())
                : byCity;

        return candidates.stream()
                .sorted(Comparator.comparingDouble(a -> distanceSquared(a, airspace)))
                .limit(NEARBY_AIRPORT_LIMIT)
                .map(a -> new AirspaceDto.NearbyAirportRef(a.getId(), a.getName(), a.getIata(), a.getIcao(), a.getLatitude(), a.getLongitude()))
                .collect(Collectors.toList());
    }

    private double distanceSquared(Airport airport, Airspace airspace) {
        double dLat = airport.getLatitude() - airspace.getLatitude();
        double dLon = airport.getLongitude() - airspace.getLongitude();
        return dLat * dLat + dLon * dLon;
    }
}
