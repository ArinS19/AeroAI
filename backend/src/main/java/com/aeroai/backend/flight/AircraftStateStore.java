package com.aeroai.backend.flight;

import com.aeroai.backend.config.AppProperties;
import com.aeroai.backend.flight.dto.TrailPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis-backed current-state/trail store (Section 20). Concrete key scheme:
 *
 * aircraft:{airspaceId}:{icao24}   HASH   current normalized state, TTL = AIRCRAFT_STALE_THRESHOLD_SECONDS
 * airspace:{airspaceId}:meta       HASH   airspace metadata + last successful ingestion timestamp
 * airspace:{airspaceId}:aircraft   SET    currently-live ICAO24 identifiers for that airspace
 * trail:{airspaceId}:{icao24}      ZSET   score=epoch seconds, member=JSON {lat,lon,timestamp}, trimmed to TRAIL_RETENTION_MINUTES
 */
@Repository
public class AircraftStateStore {

    private static final Logger log = LoggerFactory.getLogger(AircraftStateStore.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public AircraftStateStore(StringRedisTemplate redis, ObjectMapper objectMapper, AppProperties appProperties) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    private String aircraftKey(String airspaceId, String icao24) {
        return "aircraft:" + airspaceId + ":" + icao24;
    }

    private String metaKey(String airspaceId) {
        return "airspace:" + airspaceId + ":meta";
    }

    private String setKey(String airspaceId) {
        return "airspace:" + airspaceId + ":aircraft";
    }

    private String trailKey(String airspaceId, String icao24) {
        return "trail:" + airspaceId + ":" + icao24;
    }

    private String aircraftIndexKey(String icao24) {
        return "aircraft-index:" + icao24;
    }

    private static final String ACTIVE_AIRSPACES_KEY = "active-airspaces";

    public Optional<AircraftState> get(String airspaceId, String icao24) {
        Map<Object, Object> raw = redis.opsForHash().entries(aircraftKey(airspaceId, icao24));
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        Map<String, String> hash = raw.entrySet().stream()
                .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue())));
        return Optional.ofNullable(AircraftState.fromHash(hash));
    }

    public void save(AircraftState state) {
        String key = aircraftKey(state.getAirspaceId(), state.getIcao24());
        Map<String, String> hash = state.toHash();
        redis.opsForHash().putAll(key, hash);
        Duration ttl = Duration.ofSeconds(appProperties.getFreshness().getAircraftStaleThresholdSeconds());
        redis.expire(key, ttl);
        redis.opsForSet().add(setKey(state.getAirspaceId()), state.getIcao24());

        String indexKey = aircraftIndexKey(state.getIcao24());
        redis.opsForValue().set(indexKey, state.getAirspaceId(), ttl);
    }

    /** Reverse index: which airspace is currently tracking this ICAO24 (Section 26 search / trail lookups). */
    public Optional<String> findAirspaceForAircraft(String icao24) {
        String value = redis.opsForValue().get(aircraftIndexKey(icao24));
        return Optional.ofNullable(value);
    }

    /**
     * Records that {@code airspaceId} was just requested (e.g. a browser
     * fetched `GET /api/flights?airspaceId=X`). Stored as a ZSET scored by
     * request recency so the ingestion scheduler (Section 18.4) can promote
     * the most recently requested cities into its bounded active-polling set
     * without the opensky package needing its own separate Redis access.
     */
    public void markAirspaceActive(String airspaceId) {
        redis.opsForZSet().add(ACTIVE_AIRSPACES_KEY, airspaceId, Instant.now().getEpochSecond());
    }

    public Set<String> getActiveAirspaceIds() {
        Set<String> members = redis.opsForZSet().range(ACTIVE_AIRSPACES_KEY, 0, -1);
        return members != null ? members : Set.of();
    }

    /** The N most recently requested airspaces, most-recent first (Section 18.4 bounded active set). */
    public List<String> getMostRecentlyRequestedAirspaces(int limit) {
        Set<String> members = redis.opsForZSet().reverseRange(ACTIVE_AIRSPACES_KEY, 0, Math.max(limit - 1, 0));
        return members != null ? new ArrayList<>(members) : new ArrayList<>();
    }

    public Set<String> getActiveIcao24s(String airspaceId) {
        Set<String> members = redis.opsForSet().members(setKey(airspaceId));
        return members != null ? members : Set.of();
    }

    public void removeFromActiveSet(String airspaceId, String icao24) {
        redis.opsForSet().remove(setKey(airspaceId), icao24);
    }

    public boolean exists(String airspaceId, String icao24) {
        return Boolean.TRUE.equals(redis.hasKey(aircraftKey(airspaceId, icao24)));
    }

    public List<AircraftState> getAllCurrent(String airspaceId) {
        Set<String> icao24s = getActiveIcao24s(airspaceId);
        List<AircraftState> result = new ArrayList<>();
        for (String icao24 : icao24s) {
            get(airspaceId, icao24).ifPresent(result::add);
        }
        return result;
    }

    public void setLastSuccessfulUpdate(String airspaceId, Instant timestamp) {
        redis.opsForHash().put(metaKey(airspaceId), "lastSuccessfulUpdate", String.valueOf(timestamp.getEpochSecond()));
    }

    public Optional<Instant> getLastSuccessfulUpdate(String airspaceId) {
        Object value = redis.opsForHash().get(metaKey(airspaceId), "lastSuccessfulUpdate");
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Instant.ofEpochSecond(Long.parseLong(String.valueOf(value))));
    }

    public void appendTrailPoint(String airspaceId, String icao24, TrailPoint point) {
        try {
            String key = trailKey(airspaceId, icao24);
            String json = objectMapper.writeValueAsString(point);
            redis.opsForZSet().add(key, json, point.timestamp());
            long cutoff = point.timestamp() - Duration.ofMinutes(appProperties.getTrail().getRetentionMinutes()).toSeconds();
            redis.opsForZSet().removeRangeByScore(key, Double.NEGATIVE_INFINITY, cutoff);
            redis.expire(key, Duration.ofMinutes(appProperties.getTrail().getRetentionMinutes() + 5));
        } catch (Exception ex) {
            log.warn("Failed to append trail point for {}/{}: {}", airspaceId, icao24, ex.getMessage());
        }
    }

    public List<TrailPoint> getTrail(String airspaceId, String icao24, int minutes) {
        String key = trailKey(airspaceId, icao24);
        long now = Instant.now().getEpochSecond();
        long from = now - Duration.ofMinutes(minutes).toSeconds();
        Set<String> members = redis.opsForZSet().rangeByScore(key, from, now);
        if (members == null) {
            return List.of();
        }
        List<TrailPoint> points = new ArrayList<>();
        for (String member : members) {
            try {
                points.add(objectMapper.readValue(member, TrailPoint.class));
            } catch (Exception ex) {
                log.debug("Skipping malformed trail point for {}/{}: {}", airspaceId, icao24, ex.getMessage());
            }
        }
        return points;
    }
}
