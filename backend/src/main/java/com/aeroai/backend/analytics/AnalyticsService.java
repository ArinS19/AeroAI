package com.aeroai.backend.analytics;

import com.aeroai.backend.analytics.dto.AnalyticsDto;
import com.aeroai.backend.common.exception.ValidationException;
import com.aeroai.backend.config.AppProperties;
import com.aeroai.backend.flight.AircraftState;
import com.aeroai.backend.flight.AircraftStateStore;
import com.aeroai.backend.flight.ConflictSnapshotStore;
import com.aeroai.backend.flight.FlightHistoryRepository;
import com.aeroai.backend.flight.dto.ConflictRiskDto;
import com.aeroai.backend.openmeteo.OpenMeteoClient;
import com.aeroai.backend.prediction.PredictionClient;
import com.aeroai.backend.prediction.PredictionRepository;
import com.aeroai.backend.tracking.TrackedFlightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Section 44/68 - analytics computed exclusively from AeroAI's own
 * stored/calculated data (PostgreSQL history/predictions, live Redis
 * snapshot, in-memory conflict snapshot) - never a claim about what OpenSky
 * itself reports historically.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final AircraftStateStore stateStore;
    private final FlightHistoryRepository historyRepository;
    private final PredictionRepository predictionRepository;
    private final ConflictSnapshotStore conflictSnapshotStore;
    private final TrackedFlightRepository trackedFlightRepository;
    private final OpenMeteoClient openMeteoClient;
    private final PredictionClient predictionClient;
    private final AppProperties appProperties;

    public AnalyticsService(AircraftStateStore stateStore, FlightHistoryRepository historyRepository,
                             PredictionRepository predictionRepository, ConflictSnapshotStore conflictSnapshotStore,
                             TrackedFlightRepository trackedFlightRepository, OpenMeteoClient openMeteoClient,
                             PredictionClient predictionClient, AppProperties appProperties) {
        this.stateStore = stateStore;
        this.historyRepository = historyRepository;
        this.predictionRepository = predictionRepository;
        this.conflictSnapshotStore = conflictSnapshotStore;
        this.trackedFlightRepository = trackedFlightRepository;
        this.openMeteoClient = openMeteoClient;
        this.predictionClient = predictionClient;
        this.appProperties = appProperties;
    }

    public AnalyticsDto getAnalytics(String range, String airspaceIdRaw) {
        Instant since = resolveRangeStart(range);
        String airspaceId = (airspaceIdRaw == null || airspaceIdRaw.isBlank())
                ? appProperties.getAirspace().getDefaultCityId()
                : airspaceIdRaw.trim().toLowerCase();

        List<AircraftState> current = stateStore.getAllCurrent(airspaceId);

        AnalyticsDto.LiveTraffic liveTraffic = buildLiveTraffic(current, since);
        AnalyticsDto.FlightStats flightStats = buildFlightStats(since);
        AnalyticsDto.ConflictStats conflictStats = buildConflictStats();
        AnalyticsDto.SystemHealth systemHealth = buildSystemHealth(airspaceId);

        return new AnalyticsDto(range, airspaceId, liveTraffic, flightStats, conflictStats, systemHealth);
    }

    private Instant resolveRangeStart(String range) {
        if (range == null || range.equalsIgnoreCase("today")) {
            ZoneId zone = ZoneId.of(appProperties.getAnalytics().getTimezone());
            return ZonedDateTime.now(zone).toLocalDate().atStartOfDay(zone).toInstant();
        }
        if (range.equalsIgnoreCase("7d")) {
            return Instant.now().minusSeconds(7L * 24 * 3600);
        }
        throw new ValidationException("range must be 'today' or '7d'.");
    }

    private AnalyticsDto.LiveTraffic buildLiveTraffic(List<AircraftState> current, Instant since) {
        Map<String, Long> byAltitude = current.stream()
                .collect(Collectors.groupingBy(this::altitudeBand, Collectors.counting()));
        Map<String, Long> byStatus = current.stream()
                .collect(Collectors.groupingBy(s -> s.getStatus() != null ? s.getStatus().name() : "UNKNOWN", Collectors.counting()));

        // Section 18.2/68 - arrivals/departures activity is approximated from
        // flight_history status transitions within the range (distinct
        // aircraft observed LANDED / IN_FLIGHT), since no authoritative
        // schedule source exists to count "true" arrivals/departures.
        long arrivals = historyRepository.countDistinctAircraftWithStatusSince(since, "LANDED");
        long departures = historyRepository.countDistinctAircraftWithStatusSince(since, "IN_FLIGHT");

        return new AnalyticsDto.LiveTraffic(current.size(), byAltitude, byStatus, arrivals, departures);
    }

    private String altitudeBand(AircraftState state) {
        Double altitude = state.getAltitudeMeters();
        if (altitude == null) {
            return "UNAVAILABLE";
        }
        double feet = altitude / 0.3048;
        if (feet < 10_000) return "0-10000ft";
        if (feet < 30_000) return "10000-30000ft";
        return "30000ft+";
    }

    private AnalyticsDto.FlightStats buildFlightStats(Instant since) {
        long monitored = predictionRepository.countByCreatedAtGreaterThanEqual(since);
        Double avgDelay = predictionRepository.averageDelayMinutesSince(since);
        Map<String, Long> distribution = predictionRepository.categoryDistributionSince(since).stream()
                .collect(Collectors.toMap(row -> String.valueOf(row[0]), row -> (Long) row[1]));

        long activeFlights = stateStore.getActiveAirspaceIds().stream()
                .mapToLong(id -> stateStore.getAllCurrent(id).size())
                .sum();

        return new AnalyticsDto.FlightStats(monitored, activeFlights, 0L, avgDelay, distribution);
    }

    private AnalyticsDto.ConflictStats buildConflictStats() {
        List<ConflictRiskDto> pairs = conflictSnapshotStore.getAllActivePairs();
        long medium = pairs.stream().filter(p -> "MEDIUM".equals(p.riskLevel())).count();
        long high = pairs.stream().filter(p -> "HIGH".equals(p.riskLevel())).count();
        List<AnalyticsDto.ConflictStats.ConflictEvent> recent = pairs.stream()
                .limit(20)
                .map(p -> new AnalyticsDto.ConflictStats.ConflictEvent(null, p.otherIcao24(), p.riskLevel(), p.calculatedAt()))
                .toList();
        return new AnalyticsDto.ConflictStats(pairs.size(), medium, high, recent);
    }

    private AnalyticsDto.SystemHealth buildSystemHealth(String airspaceId) {
        Instant lastUpdate = stateStore.getLastSuccessfulUpdate(airspaceId).orElse(null);
        String freshness = freshnessLabel(lastUpdate);
        String openSkyStatus = lastUpdate != null ? "UP" : "UNKNOWN";
        String openMeteoStatus = openMeteoClient.isLastCallHealthy() ? "UP" : "DEGRADED";
        String mlStatus = predictionClient.isHealthy() ? "UP" : "DEGRADED";
        long totalTracked = trackedFlightRepository.count();
        return new AnalyticsDto.SystemHealth(lastUpdate, freshness, openSkyStatus, openMeteoStatus, mlStatus, totalTracked);
    }

    private String freshnessLabel(Instant lastUpdate) {
        if (lastUpdate == null) {
            return "UNAVAILABLE";
        }
        long ageSeconds = Instant.now().getEpochSecond() - lastUpdate.getEpochSecond();
        if (ageSeconds <= appProperties.getFreshness().getLiveThresholdSeconds()) {
            return "LIVE";
        }
        if (ageSeconds <= appProperties.getFreshness().getUnavailableThresholdSeconds()) {
            return "DELAYED";
        }
        return "UNAVAILABLE";
    }
}
