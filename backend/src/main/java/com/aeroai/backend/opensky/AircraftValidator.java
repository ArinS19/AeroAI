package com.aeroai.backend.opensky;

import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Section 56 - ingestion validation. A single invalid record is
 * dropped/logged; it never aborts the rest of the batch.
 */
@Component
public class AircraftValidator {

    private static final double MAX_SPEED_MS = 1500.0 / 3.6; // Section 14 sanity ceiling, ~416.7 m/s
    private static final long FUTURE_TOLERANCE_SECONDS = 120;

    public boolean isValid(OpenSkyStateVector v) {
        if (v.icao24() == null || !v.icao24().matches("^[0-9a-f]{6}$")) {
            return false;
        }
        if (v.latitude() == null || v.longitude() == null) {
            // Position-less rows are common (e.g. ground vehicles at some
            // airports) - not "invalid" per se, but not usable for the map.
            return false;
        }
        if (!isFinite(v.latitude()) || v.latitude() < -90 || v.latitude() > 90) {
            return false;
        }
        if (!isFinite(v.longitude()) || v.longitude() < -180 || v.longitude() > 180) {
            return false;
        }
        if (v.lastContact() == null) {
            return false;
        }
        long now = Instant.now().getEpochSecond();
        if (v.lastContact() > now + FUTURE_TOLERANCE_SECONDS) {
            return false;
        }
        if (v.velocityMs() != null && (!isFinite(v.velocityMs()) || v.velocityMs() < 0 || v.velocityMs() > MAX_SPEED_MS)) {
            return false;
        }
        if (v.trueTrackDegrees() != null && (!isFinite(v.trueTrackDegrees()) || v.trueTrackDegrees() < 0 || v.trueTrackDegrees() > 360)) {
            return false;
        }
        if (v.effectiveAltitudeMeters() != null && !isFinite(v.effectiveAltitudeMeters())) {
            return false;
        }
        if (v.verticalRateMs() != null && !isFinite(v.verticalRateMs())) {
            return false;
        }
        return true;
    }

    private boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
