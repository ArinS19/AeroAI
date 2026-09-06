package com.aeroai.backend.flight;

import com.aeroai.backend.airspace.AirspaceService;
import com.aeroai.backend.common.exception.NotFoundException;
import com.aeroai.backend.common.exception.ValidationException;
import com.aeroai.backend.config.AppProperties;
import com.aeroai.backend.flight.dto.*;
import com.aeroai.backend.prediction.PredictionService;
import com.aeroai.backend.prediction.dto.PredictionDto;
import com.aeroai.backend.prediction.dto.PredictionRequestDto;
import com.aeroai.backend.weather.WeatherService;
import com.aeroai.backend.weather.dto.WeatherDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Section 11 - flight domain service. Composes live Redis state, PostgreSQL
 * history, weather, prediction, and conflict data into the DTOs defined in
 * Section 48. All derived math (Section 25) happens here, exactly once.
 */
@Service
public class FlightService {

    private static final int MAX_QUERY_LENGTH = 100;
    private static final List<String> VALID_STATUSES = List.of("SCHEDULED", "IN_FLIGHT", "LANDED", "UNKNOWN");

    private final AircraftStateStore stateStore;
    private final FlightHistoryRepository historyRepository;
    private final AirspaceService airspaceService;
    private final WeatherService weatherService;
    private final PredictionService predictionService;
    private final ConflictSnapshotStore conflictSnapshotStore;
    private final AppProperties appProperties;

    public FlightService(AircraftStateStore stateStore,
                          FlightHistoryRepository historyRepository,
                          AirspaceService airspaceService,
                          WeatherService weatherService,
                          PredictionService predictionService,
                          ConflictSnapshotStore conflictSnapshotStore,
                          AppProperties appProperties) {
        this.stateStore = stateStore;
        this.historyRepository = historyRepository;
        this.airspaceService = airspaceService;
        this.weatherService = weatherService;
        this.predictionService = predictionService;
        this.conflictSnapshotStore = conflictSnapshotStore;
        this.appProperties = appProperties;
    }

    // ------------------------------------------------------------------ list/search

    public List<AircraftDto> getCurrentAircraft(String airspaceIdRaw) {
        String airspaceId = resolveAirspaceId(airspaceIdRaw);
        return stateStore.getAllCurrent(airspaceId).stream()
                .filter(AircraftState::hasReliablePosition)
                .sorted(Comparator.comparing(s -> Optional.ofNullable(s.getCallsign()).orElse("")))
                .map(this::toAircraftDto)
                .toList();
    }

    public List<FlightListItemDto> list(String airspaceIdRaw, String statusFilter, String airlineFilter) {
        String airspaceId = resolveAirspaceId(airspaceIdRaw);
        validateStatusFilter(statusFilter);

        return stateStore.getAllCurrent(airspaceId).stream()
                .filter(AircraftState::hasReliablePosition)
                .map(this::toListItemDto)
                .filter(item -> statusFilter == null || statusFilter.equalsIgnoreCase(item.status()))
                .filter(item -> airlineFilter == null || (item.airline() != null
                        && item.airline().toLowerCase(Locale.ROOT).contains(airlineFilter.toLowerCase(Locale.ROOT))))
                .sorted(Comparator.comparing(i -> Optional.ofNullable(i.callsign()).orElse("")))
                .toList();
    }

    public List<FlightListItemDto> search(String query) {
        String trimmed = validateQuery(query);
        if (trimmed.isEmpty()) {
            return List.of();
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);

        return stateStore.getActiveAirspaceIds().stream()
                .flatMap(airspaceId -> stateStore.getAllCurrent(airspaceId).stream())
                .filter(state -> matches(state, upper))
                .distinct()
                .map(this::toListItemDto)
                .sorted(Comparator.comparing((FlightListItemDto item) -> exactMatchRank(item, upper))
                        .thenComparing(item -> Optional.ofNullable(item.callsign()).orElse("")))
                .limit(50)
                .toList();
    }

    private boolean matches(AircraftState state, String upperQuery) {
        String callsign = state.getCallsign() != null ? state.getCallsign().toUpperCase(Locale.ROOT) : "";
        String icao24 = state.getIcao24() != null ? state.getIcao24().toUpperCase(Locale.ROOT) : "";
        return callsign.contains(upperQuery) || icao24.contains(upperQuery);
    }

    private int exactMatchRank(FlightListItemDto item, String upperQuery) {
        boolean exact = (item.callsign() != null && item.callsign().equalsIgnoreCase(upperQuery))
                || (item.icao24() != null && item.icao24().equalsIgnoreCase(upperQuery));
        return exact ? 0 : 1;
    }

    /** Lightweight current-state lookup used by the tracking domain (My Flights, Section 42). */
    public Optional<AircraftDto> findCurrentByIcao24(String icao24Raw) {
        String icao24 = validateIcao24(icao24Raw);
        return stateStore.findAirspaceForAircraft(icao24)
                .flatMap(airspaceId -> stateStore.get(airspaceId, icao24))
                .map(this::toAircraftDto);
    }

    // ------------------------------------------------------------------ trail

    public TrailResponseDto getTrail(String icao24Raw, int minutes) {
        String icao24 = validateIcao24(icao24Raw);
        validateTrailMinutes(minutes);

        Optional<String> airspaceId = stateStore.findAirspaceForAircraft(icao24);
        if (airspaceId.isEmpty()) {
            return TrailResponseDto.unavailable(icao24, minutes);
        }
        List<TrailPoint> points = stateStore.getTrail(airspaceId.get(), icao24, minutes);
        if (points.isEmpty()) {
            return TrailResponseDto.unavailable(icao24, minutes);
        }
        return new TrailResponseDto(icao24, minutes, true, points);
    }

    private void validateTrailMinutes(int minutes) {
        if (minutes != 5 && minutes != 15 && minutes != 30 && minutes != 60) {
            throw new ValidationException("minutes must be one of 5, 15, 30, 60.");
        }
    }

    // ------------------------------------------------------------------ detail

    @Transactional(readOnly = true)
    public FlightDetailDto getDetail(String icao24Raw) {
        String icao24 = validateIcao24(icao24Raw);
        String airspaceId = stateStore.findAirspaceForAircraft(icao24)
                .orElseThrow(() -> new NotFoundException("Flight not found or no longer live."));
        AircraftState state = stateStore.get(airspaceId, icao24)
                .orElseThrow(() -> new NotFoundException("Flight not found or no longer live."));

        CurrentPositionDto position = new CurrentPositionDto(
                state.getLatitude(), state.getLongitude(), state.getAltitudeMeters(),
                state.getSpeedMs(), state.getHeadingDegrees(), state.getVerticalRateMs(), state.getLastContact());

        Double distanceTravelledKm = calculateDistanceTravelled(state);

        // Section 18.2 - no reliable destination/schedule source is available from
        // OpenSky's live state vector alone, so distance-remaining/ETA/schedule
        // fields are honestly Unavailable rather than guessed. The composition
        // below is written so that if a reliable destination source is added in
        // future, these fields populate automatically without further changes.
        Double distanceRemainingKm = null;
        Instant estimatedArrival = null;

        WeatherDto weather = state.hasReliablePosition()
                ? weatherService.getCurrentWeather(state.getLatitude(), state.getLongitude())
                : WeatherDto.unavailable();

        PredictionDto prediction = PredictionDto.unavailable(); // no reliable schedule inputs available (see above)

        boolean trailAvailable = !stateStore.getTrail(airspaceId, icao24, 5).isEmpty();

        String airline = AirlineCallsignLookup.lookup(state.getCallsign());

        return new FlightDetailDto(
                state.getIcao24(),
                state.getCallsign(),
                airline,
                null, // aircraft registration - not present in OpenSky state vectors (Section 18.2)
                null, // aircraft type - same
                null, // origin
                null, // destination
                null, // departureTime
                null, // arrivalTime
                state.getStatus() != null ? state.getStatus().name() : FlightStatus.UNKNOWN.name(),
                position,
                distanceTravelledKm,
                distanceRemainingKm,
                estimatedArrival,
                trailAvailable,
                weather,
                prediction,
                conflictSnapshotStore.getFor(icao24),
                false // tracked flag is set by the controller layer, which knows the authenticated user
        );
    }

    /** Used by callers (e.g. the FastAPI prediction path) that DO have reliable schedule/route inputs. */
    public PredictionDto computePredictionIfPossible(String identifier, PredictionRequestDto request) {
        return predictionService.getOrCompute(identifier, request);
    }

    private Double calculateDistanceTravelled(AircraftState state) {
        if (state.getSessionStartEpoch() == null) {
            return null;
        }
        Instant sessionStart = Instant.ofEpochSecond(state.getSessionStartEpoch());
        List<FlightHistory> points = historyRepository
                .findByAircraftIdentifierAndTimestampGreaterThanEqualOrderByTimestampAsc(state.getIcao24(), sessionStart);
        if (points.size() < 2) {
            return null;
        }
        List<double[]> latLon = points.stream().map(p -> new double[]{p.getLatitude(), p.getLongitude()}).toList();
        return FlightDerivedCalculations.distanceTravelledKm(latLon);
    }

    // ------------------------------------------------------------------ mapping helpers

    AircraftDto toAircraftDto(AircraftState state) {
        return new AircraftDto(
                state.getIcao24(),
                state.getCallsign(),
                state.getLatitude(),
                state.getLongitude(),
                state.getAltitudeMeters(),
                state.getSpeedMs(),
                state.getHeadingDegrees(),
                state.getVerticalRateMs(),
                state.getOnGround(),
                state.getStatus() != null ? state.getStatus().name() : FlightStatus.UNKNOWN.name(),
                state.getLastContact()
        );
    }

    private FlightListItemDto toListItemDto(AircraftState state) {
        return new FlightListItemDto(
                state.getIcao24(),
                state.getCallsign(),
                AirlineCallsignLookup.lookup(state.getCallsign()),
                null,
                null,
                state.getStatus() != null ? state.getStatus().name() : FlightStatus.UNKNOWN.name(),
                state.getAltitudeMeters(),
                state.getSpeedMs(),
                state.getLastContact()
        );
    }

    private String resolveAirspaceId(String airspaceIdRaw) {
        String resolved;
        if (airspaceIdRaw == null || airspaceIdRaw.isBlank()) {
            resolved = appProperties.getAirspace().getDefaultCityId();
        } else {
            // Validate the airspace actually exists (throws NotFoundException otherwise).
            airspaceService.requireBySlug(airspaceIdRaw);
            resolved = airspaceIdRaw.trim().toLowerCase(Locale.ROOT);
        }
        // Section 18.4 - record this as a request so the bounded ingestion
        // scheduler can promote it into the active-polling set.
        stateStore.markAirspaceActive(resolved);
        return resolved;
    }

    private void validateStatusFilter(String statusFilter) {
        if (statusFilter != null && !VALID_STATUSES.contains(statusFilter.toUpperCase(Locale.ROOT))) {
            throw new ValidationException("status must be one of " + VALID_STATUSES);
        }
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

    private String validateIcao24(String icao24) {
        if (icao24 == null || !icao24.trim().toLowerCase(Locale.ROOT).matches("^[0-9a-f]{6}$")) {
            throw new ValidationException("icao24 must be a 6-character hex identifier.");
        }
        return icao24.trim().toLowerCase(Locale.ROOT);
    }
}
