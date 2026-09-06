package com.aeroai.backend.opensky;

import com.aeroai.backend.airspace.Airspace;
import com.aeroai.backend.airspace.AirspaceService;
import com.aeroai.backend.alert.AlertService;
import com.aeroai.backend.config.AppProperties;
import com.aeroai.backend.conflict.ConflictEngine;
import com.aeroai.backend.flight.*;
import com.aeroai.backend.flight.dto.AircraftDto;
import com.aeroai.backend.flight.dto.ConflictRiskDto;
import com.aeroai.backend.flight.dto.TrailPoint;
import com.aeroai.backend.tracking.TrackedFlight;
import com.aeroai.backend.tracking.TrackedFlightRepository;
import com.aeroai.backend.websocket.BroadcastService;
import com.aeroai.backend.websocket.DeltaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Section 55 - the single centralized OpenSky ingestion pipeline:
 * FETCH -> VALIDATE -> NORMALIZE -> DEDUPLICATE -> CALCULATE DERIVED STATE ->
 * REDIS CURRENT STATE -> POSTGRES HISTORY -> CONFLICT ENGINE ->
 * (PREDICTION CACHE WHEN NEEDED - a separate, lazily-triggered code path,
 * see Section 35/Rule 7) -> WEBSOCKET DELTA.
 *
 * Runs as a single {@code @Scheduled} entry point once per
 * OPENSKY_POLL_INTERVAL_SECONDS, never triggered by a browser request
 * (Section 18).
 */
@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private static final int BACKOFF_FAILURE_THRESHOLD = 3;

    private final ActiveAirspaceManager activeAirspaceManager;
    private final AirspaceService airspaceService;
    private final OpenSkyClient openSkyClient;
    private final AircraftValidator validator;
    private final AircraftNormalizer normalizer;
    private final AircraftStateStore stateStore;
    private final FlightHistoryRepository historyRepository;
    private final ConflictEngine conflictEngine;
    private final ConflictSnapshotStore conflictSnapshotStore;
    private final DeltaBuilder deltaBuilder;
    private final BroadcastService broadcastService;
    private final TrackedFlightRepository trackedFlightRepository;
    private final AlertService alertService;
    private final AppProperties appProperties;

    private final Map<String, Integer> consecutiveFailures = new ConcurrentHashMap<>();
    private final Map<String, Long> cycleCounters = new ConcurrentHashMap<>();

    public IngestionService(ActiveAirspaceManager activeAirspaceManager, AirspaceService airspaceService,
                             OpenSkyClient openSkyClient, AircraftValidator validator, AircraftNormalizer normalizer,
                             AircraftStateStore stateStore, FlightHistoryRepository historyRepository,
                             ConflictEngine conflictEngine, ConflictSnapshotStore conflictSnapshotStore,
                             DeltaBuilder deltaBuilder, BroadcastService broadcastService,
                             TrackedFlightRepository trackedFlightRepository, AlertService alertService,
                             AppProperties appProperties) {
        this.activeAirspaceManager = activeAirspaceManager;
        this.airspaceService = airspaceService;
        this.openSkyClient = openSkyClient;
        this.validator = validator;
        this.normalizer = normalizer;
        this.stateStore = stateStore;
        this.historyRepository = historyRepository;
        this.conflictEngine = conflictEngine;
        this.conflictSnapshotStore = conflictSnapshotStore;
        this.deltaBuilder = deltaBuilder;
        this.broadcastService = broadcastService;
        this.trackedFlightRepository = trackedFlightRepository;
        this.alertService = alertService;
        this.appProperties = appProperties;
    }

    @Scheduled(fixedDelayString = "${aeroai.opensky.poll-interval-seconds}000", initialDelayString = "2000")
    public void ingestActiveAirspaces() {
        for (String airspaceId : activeAirspaceManager.getActiveAirspaceSlugs()) {
            try {
                ingestOne(airspaceId);
            } catch (Exception ex) {
                // Section 56/18.5 - one airspace's failure must never crash the
                // scheduler or affect any other airspace's ingestion.
                log.error("Unexpected error ingesting airspace '{}': {}", airspaceId, ex.getMessage(), ex);
            }
        }
    }

    private void ingestOne(String airspaceId) {
        if (shouldSkipThisCycleDueToBackoff(airspaceId)) {
            return;
        }

        Airspace airspace = airspaceService.requireBySlug(airspaceId);

        Map<String, AircraftState> previousStates = new HashMap<>();
        for (AircraftState s : stateStore.getAllCurrent(airspaceId)) {
            previousStates.put(s.getIcao24(), s);
        }
        Set<String> previousActiveIcao24s = stateStore.getActiveIcao24s(airspaceId);

        OpenSkyClient.FetchResult fetchResult = openSkyClient.fetchStates(airspace);
        if (!fetchResult.success()) {
            recordFailure(airspaceId);
            log.warn("OpenSky fetch failed for airspace '{}' (consecutive failures: {}). Preserving last known state.",
                    airspaceId, consecutiveFailures.getOrDefault(airspaceId, 0));
            return;
        }
        recordSuccess(airspaceId);

        Instant now = Instant.now();

        // VALIDATE + DEDUPLICATE (Section 56/55) - keep the last row per icao24.
        Map<String, OpenSkyStateVector> deduped = new LinkedHashMap<>();
        for (OpenSkyStateVector raw : fetchResult.states()) {
            if (validator.isValid(raw)) {
                deduped.put(raw.icao24(), raw);
            }
        }

        // NORMALIZE + CALCULATE DERIVED STATE (Section 55/24/18.2).
        Map<String, AircraftState> currentStates = new HashMap<>();
        for (OpenSkyStateVector raw : deduped.values()) {
            AircraftState previous = previousStates.get(raw.icao24());
            AircraftState normalized = normalizer.normalize(raw, previous, airspaceId, now);
            currentStates.put(normalized.getIcao24(), normalized);
        }

        // REDIS CURRENT STATE.
        for (AircraftState state : currentStates.values()) {
            stateStore.save(state);
            appendTrailPointIfMeaningful(airspaceId, state, previousStates.get(state.getIcao24()));
        }
        stateStore.setLastSuccessfulUpdate(airspaceId, now);

        // Detect aircraft whose Redis hash expired since the previous cycle (Section 57).
        List<String> removed = new ArrayList<>();
        for (String icao24 : previousActiveIcao24s) {
            if (!previousStates.containsKey(icao24)) {
                // Was in the SET but its hash had already expired before this cycle even started.
                stateStore.removeFromActiveSet(airspaceId, icao24);
                removed.add(icao24);
            } else if (!currentStates.containsKey(icao24) && !stateStore.exists(airspaceId, icao24)) {
                stateStore.removeFromActiveSet(airspaceId, icao24);
                removed.add(icao24);
            }
        }

        // POSTGRES HISTORY (Section 22.3/23/55) - one row per aircraft per cycle.
        persistHistory(currentStates.values());

        // CONFLICT ENGINE (Section 36/38/55) - runs once per cycle over ALL currently-live aircraft.
        List<AircraftState> allCurrent = stateStore.getAllCurrent(airspaceId);
        conflictEngine.computeAndPublish(allCurrent);

        // WEBSOCKET DELTA (Section 19/47/55).
        var delta = deltaBuilder.build(previousStates, currentStates, removed, this::toAircraftDto);
        broadcastService.broadcastDelta(airspaceId, delta);

        // Alerts for tracked aircraft (Section 43) - evaluated once per cycle, deduplicated by state transition.
        evaluateTrackedAircraftAlerts(currentStates.values());
    }

    private AircraftDto toAircraftDto(AircraftState state) {
        return new AircraftDto(state.getIcao24(), state.getCallsign(), state.getLatitude(), state.getLongitude(),
                state.getAltitudeMeters(), state.getSpeedMs(), state.getHeadingDegrees(), state.getVerticalRateMs(),
                state.getOnGround(), state.getStatus() != null ? state.getStatus().name() : FlightStatus.UNKNOWN.name(),
                state.getLastContact());
    }

    private void appendTrailPointIfMeaningful(String airspaceId, AircraftState current, AircraftState previous) {
        if (!current.hasReliablePosition() || current.getLastContact() == null) {
            return;
        }
        long timestamp = current.getLastContact().getEpochSecond();

        if (previous != null && previous.hasReliablePosition() && previous.getLastContact() != null) {
            double movedKm = com.aeroai.backend.common.util.GeoMath.haversineKm(
                    previous.getLatitude(), previous.getLongitude(), current.getLatitude(), current.getLongitude());
            long elapsedSeconds = timestamp - previous.getLastContact().getEpochSecond();
            // Section 14 - skip negligible movement observed in a very short interval.
            if (movedKm < 0.05 && elapsedSeconds < 5) {
                return;
            }
            if (elapsedSeconds <= 0) {
                return; // Not newer than the last stored point.
            }
        }
        stateStore.appendTrailPoint(airspaceId, current.getIcao24(),
                new TrailPoint(current.getLatitude(), current.getLongitude(), timestamp));
    }

    private void persistHistory(Collection<AircraftState> states) {
        for (AircraftState state : states) {
            if (!state.hasReliablePosition()) {
                continue;
            }
            try {
                historyRepository.save(new FlightHistory(
                        state.getIcao24(), state.getCallsign(),
                        state.getLastContact() != null ? state.getLastContact() : Instant.now(),
                        state.getLatitude(), state.getLongitude(), state.getAltitudeMeters(), state.getSpeedMs(),
                        state.getHeadingDegrees(), state.getVerticalRateMs(), state.getOnGround(),
                        state.getStatus() != null ? state.getStatus().name() : FlightStatus.UNKNOWN.name()));
            } catch (Exception ex) {
                // Section 51 - PostgreSQL failures must not break the live/Redis hot path.
                log.warn("Failed to persist flight_history row for {}: {}", state.getIcao24(), ex.getMessage());
            }
        }
    }

    private void evaluateTrackedAircraftAlerts(Collection<AircraftState> states) {
        for (AircraftState state : states) {
            List<TrackedFlight> trackers = trackedFlightRepository.findByAircraftIdentifierAndActiveTrue(state.getIcao24());
            if (trackers.isEmpty()) {
                continue;
            }
            String status = state.getStatus() != null ? state.getStatus().name() : FlightStatus.UNKNOWN.name();
            List<ConflictRiskDto> conflicts = conflictSnapshotStore.getFor(state.getIcao24());
            String riskLevel = conflicts.stream()
                    .map(ConflictRiskDto::riskLevel)
                    .max(Comparator.comparingInt(this::riskRank))
                    .orElse("LOW");

            for (TrackedFlight tracked : trackers) {
                alertService.evaluateStateChange(tracked.getId(), tracked.getUserId(), AlertService.AlertType.STATUS_CHANGE,
                        status, "Flight " + displayName(tracked, state) + " status changed to " + status + ".");
                alertService.evaluateStateChange(tracked.getId(), tracked.getUserId(), AlertService.AlertType.CONFLICT_RISK_CHANGE,
                        riskLevel, "Conflict risk for " + displayName(tracked, state) + " is now " + riskLevel + ".");
            }
        }

        // Section 43.1/57 - flag tracked aircraft that have gone stale/unavailable.
        for (TrackedFlight tracked : allActiveTrackedFlights()) {
            boolean stillPresent = states.stream().anyMatch(s -> s.getIcao24().equalsIgnoreCase(tracked.getAircraftIdentifier()));
            if (!stillPresent) {
                boolean anyLive = stateStore.findAirspaceForAircraft(tracked.getAircraftIdentifier()).isPresent();
                if (!anyLive) {
                    alertService.evaluateStateChange(tracked.getId(), tracked.getUserId(), AlertService.AlertType.DATA_UNAVAILABLE,
                            "UNAVAILABLE", "Live data for " + displayName(tracked, null) + " is temporarily unavailable.");
                }
            }
        }
    }

    private List<TrackedFlight> allActiveTrackedFlights() {
        // Bounded by the number of currently tracked flights across all users - acceptable
        // per-cycle cost at this project's scale (Section 38's "low hundreds" scale note).
        return trackedFlightRepository.findAll().stream().filter(TrackedFlight::isActive).toList();
    }

    private String displayName(TrackedFlight tracked, AircraftState state) {
        String callsign = state != null && state.getCallsign() != null ? state.getCallsign() : tracked.getCallsign();
        return callsign != null ? callsign : tracked.getAircraftIdentifier();
    }

    private int riskRank(String level) {
        return switch (level) {
            case "HIGH" -> 2;
            case "MEDIUM" -> 1;
            default -> 0;
        };
    }

    // ------------------------------------------------------------------ failure backoff (Section 18.5)

    private boolean shouldSkipThisCycleDueToBackoff(String airspaceId) {
        int failures = consecutiveFailures.getOrDefault(airspaceId, 0);
        if (failures < BACKOFF_FAILURE_THRESHOLD) {
            return false;
        }
        long cycle = cycleCounters.merge(airspaceId, 1L, Long::sum);
        // Cap backoff at 2x the normal interval: skip every other cycle once sustained failures occur.
        return cycle % 2 == 0;
    }

    private void recordFailure(String airspaceId) {
        consecutiveFailures.merge(airspaceId, 1, Integer::sum);
    }

    private void recordSuccess(String airspaceId) {
        consecutiveFailures.remove(airspaceId);
        cycleCounters.remove(airspaceId);
    }
}
