package com.aeroai.backend.opensky;

import com.aeroai.backend.flight.AircraftState;
import com.aeroai.backend.flight.FlightStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Section 18.1/18.2/24 - maps a validated raw OpenSky state vector plus the
 * aircraft's previous known state into the normalized {@link AircraftState},
 * including the Landed/In-Flight/Unknown status-transition rules.
 */
@Component
public class AircraftNormalizer {

    /** Consecutive on-ground polling cycles required before flipping status to LANDED (Section 24, ~30s at 15s interval). */
    private static final int LANDED_CONFIRMATION_CYCLES = 2;

    public AircraftState normalize(OpenSkyStateVector raw, AircraftState previous, String airspaceId, Instant now) {
        AircraftState state = new AircraftState();
        state.setIcao24(raw.icao24());
        state.setCallsign(blankToNull(raw.callsign()));
        state.setOriginCountry(raw.originCountry());
        state.setLatitude(raw.latitude());
        state.setLongitude(raw.longitude());
        state.setAltitudeMeters(raw.effectiveAltitudeMeters());
        state.setSpeedMs(raw.velocityMs());
        state.setHeadingDegrees(raw.trueTrackDegrees());
        state.setVerticalRateMs(raw.verticalRateMs());
        state.setOnGround(raw.onGround());
        state.setLastContact(raw.lastContact() != null ? Instant.ofEpochSecond(raw.lastContact()) : now);
        state.setAirspaceId(airspaceId);

        boolean airborne = isAirborne(raw);

        if (airborne) {
            state.setOnGroundStreak(0);
            state.setStatus(FlightStatus.IN_FLIGHT);
            boolean continuingSession = previous != null && previous.getStatus() == FlightStatus.IN_FLIGHT
                    && previous.getSessionStartEpoch() != null;
            state.setSessionStartEpoch(continuingSession ? previous.getSessionStartEpoch() : now.getEpochSecond());
            return state;
        }

        // Not airborne (on ground, or ambiguous with no evidence either way).
        if (previous == null) {
            // Section 24 - never fabricate Landed from a first-ever sighting
            // with no prior airborne evidence; Unknown is the honest state.
            state.setStatus(FlightStatus.UNKNOWN);
            state.setOnGroundStreak(Boolean.TRUE.equals(raw.onGround()) ? 1 : 0);
            state.setSessionStartEpoch(null);
            return state;
        }

        int streak = Boolean.TRUE.equals(raw.onGround()) ? previous.getOnGroundStreak() + 1 : 0;
        state.setOnGroundStreak(streak);

        if (previous.getStatus() == FlightStatus.LANDED) {
            state.setStatus(FlightStatus.LANDED);
            state.setSessionStartEpoch(null);
        } else if ((previous.getStatus() == FlightStatus.IN_FLIGHT || previous.getStatus() == FlightStatus.UNKNOWN)
                && Boolean.TRUE.equals(raw.onGround()) && streak >= LANDED_CONFIRMATION_CYCLES) {
            state.setStatus(FlightStatus.LANDED);
            state.setSessionStartEpoch(null);
        } else {
            // Not yet confirmed either way - keep the previous status rather than guessing.
            state.setStatus(previous.getStatus());
            state.setSessionStartEpoch(previous.getSessionStartEpoch());
        }
        return state;
    }

    /** Section 24 "In Flight" - on_ground=false, or absent-but-inferred-airborne from altitude/speed. */
    private boolean isAirborne(OpenSkyStateVector raw) {
        if (Boolean.FALSE.equals(raw.onGround())) {
            return true;
        }
        if (raw.onGround() == null) {
            Double altitude = raw.effectiveAltitudeMeters();
            Double speed = raw.velocityMs();
            return altitude != null && altitude > 50 && speed != null && speed > 20;
        }
        return false;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
