package com.aeroai.backend.flight;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Normalized live aircraft state (Section 18.1/18.2). This is the shared,
 * neutral in-memory/Redis-hash representation written by the OpenSky
 * ingestion pipeline and read by the flight, conflict, websocket, tracking
 * and alert packages. Every field is independently nullable - a null OpenSky
 * field is carried through as null, never defaulted to 0/false (Section 18.1).
 */
public class AircraftState {

    private String icao24;
    private String callsign;
    private String originCountry;
    private Double latitude;
    private Double longitude;
    private Double altitudeMeters;
    private Double speedMs;
    private Double headingDegrees;
    private Double verticalRateMs;
    private Boolean onGround;
    private Instant lastContact;
    private FlightStatus status = FlightStatus.UNKNOWN;
    private String airspaceId;

    /** Epoch-seconds timestamp when the current IN_FLIGHT session began (Section 25.2), null if not currently in a session. */
    private Long sessionStartEpoch;

    /** Consecutive polling cycles the aircraft has reported on_ground=true (Section 24 Landed rule). */
    private int onGroundStreak;

    public String getIcao24() { return icao24; }
    public void setIcao24(String icao24) { this.icao24 = icao24; }
    public String getCallsign() { return callsign; }
    public void setCallsign(String callsign) { this.callsign = callsign; }
    public String getOriginCountry() { return originCountry; }
    public void setOriginCountry(String originCountry) { this.originCountry = originCountry; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Double getAltitudeMeters() { return altitudeMeters; }
    public void setAltitudeMeters(Double altitudeMeters) { this.altitudeMeters = altitudeMeters; }
    public Double getSpeedMs() { return speedMs; }
    public void setSpeedMs(Double speedMs) { this.speedMs = speedMs; }
    public Double getHeadingDegrees() { return headingDegrees; }
    public void setHeadingDegrees(Double headingDegrees) { this.headingDegrees = headingDegrees; }
    public Double getVerticalRateMs() { return verticalRateMs; }
    public void setVerticalRateMs(Double verticalRateMs) { this.verticalRateMs = verticalRateMs; }
    public Boolean getOnGround() { return onGround; }
    public void setOnGround(Boolean onGround) { this.onGround = onGround; }
    public Instant getLastContact() { return lastContact; }
    public void setLastContact(Instant lastContact) { this.lastContact = lastContact; }
    public FlightStatus getStatus() { return status; }
    public void setStatus(FlightStatus status) { this.status = status; }
    public String getAirspaceId() { return airspaceId; }
    public void setAirspaceId(String airspaceId) { this.airspaceId = airspaceId; }
    public Long getSessionStartEpoch() { return sessionStartEpoch; }
    public void setSessionStartEpoch(Long sessionStartEpoch) { this.sessionStartEpoch = sessionStartEpoch; }
    public int getOnGroundStreak() { return onGroundStreak; }
    public void setOnGroundStreak(int onGroundStreak) { this.onGroundStreak = onGroundStreak; }

    public boolean hasReliablePosition() {
        return latitude != null && longitude != null;
    }

    // --- Redis hash (de)serialization -------------------------------------------------

    public Map<String, String> toHash() {
        Map<String, String> map = new HashMap<>();
        putIfNotNull(map, "icao24", icao24);
        putIfNotNull(map, "callsign", callsign);
        putIfNotNull(map, "originCountry", originCountry);
        putIfNotNull(map, "latitude", latitude);
        putIfNotNull(map, "longitude", longitude);
        putIfNotNull(map, "altitudeMeters", altitudeMeters);
        putIfNotNull(map, "speedMs", speedMs);
        putIfNotNull(map, "headingDegrees", headingDegrees);
        putIfNotNull(map, "verticalRateMs", verticalRateMs);
        putIfNotNull(map, "onGround", onGround);
        putIfNotNull(map, "lastContact", lastContact != null ? lastContact.getEpochSecond() : null);
        putIfNotNull(map, "status", status != null ? status.name() : null);
        putIfNotNull(map, "airspaceId", airspaceId);
        putIfNotNull(map, "sessionStartEpoch", sessionStartEpoch);
        map.put("onGroundStreak", String.valueOf(onGroundStreak));
        return map;
    }

    public static AircraftState fromHash(Map<String, String> hash) {
        if (hash == null || hash.isEmpty()) {
            return null;
        }
        AircraftState state = new AircraftState();
        state.icao24 = hash.get("icao24");
        state.callsign = hash.get("callsign");
        state.originCountry = hash.get("originCountry");
        state.latitude = parseDouble(hash.get("latitude"));
        state.longitude = parseDouble(hash.get("longitude"));
        state.altitudeMeters = parseDouble(hash.get("altitudeMeters"));
        state.speedMs = parseDouble(hash.get("speedMs"));
        state.headingDegrees = parseDouble(hash.get("headingDegrees"));
        state.verticalRateMs = parseDouble(hash.get("verticalRateMs"));
        state.onGround = hash.get("onGround") != null ? Boolean.parseBoolean(hash.get("onGround")) : null;
        String lastContactStr = hash.get("lastContact");
        state.lastContact = lastContactStr != null ? Instant.ofEpochSecond(Long.parseLong(lastContactStr)) : null;
        String statusStr = hash.get("status");
        state.status = statusStr != null ? FlightStatus.valueOf(statusStr) : FlightStatus.UNKNOWN;
        state.airspaceId = hash.get("airspaceId");
        String sessionStr = hash.get("sessionStartEpoch");
        state.sessionStartEpoch = sessionStr != null ? Long.parseLong(sessionStr) : null;
        String streakStr = hash.get("onGroundStreak");
        state.onGroundStreak = streakStr != null ? Integer.parseInt(streakStr) : 0;
        return state;
    }

    private static void putIfNotNull(Map<String, String> map, String key, Object value) {
        if (value != null) {
            map.put(key, String.valueOf(value));
        }
    }

    private static Double parseDouble(String value) {
        return value != null ? Double.parseDouble(value) : null;
    }
}
