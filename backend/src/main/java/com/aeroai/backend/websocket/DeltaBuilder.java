package com.aeroai.backend.websocket;

import com.aeroai.backend.flight.AircraftState;
import com.aeroai.backend.flight.dto.AircraftDto;
import com.aeroai.backend.websocket.dto.AircraftDeltaMessage;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Section 19/47 - builds the added/updated/removed delta for one ingestion
 * cycle. "Updated" only includes aircraft whose reportable fields actually
 * changed (Section 53 - "update only changed aircraft").
 */
@Component
public class DeltaBuilder {

    public AircraftDeltaMessage build(Map<String, AircraftState> previousCycle,
                                       Map<String, AircraftState> currentCycle,
                                       List<String> removedIcao24s,
                                       Function<AircraftState, AircraftDto> toDto) {
        List<AircraftDto> added = new ArrayList<>();
        List<AircraftDto> updated = new ArrayList<>();

        for (Map.Entry<String, AircraftState> entry : currentCycle.entrySet()) {
            AircraftState previous = previousCycle.get(entry.getKey());
            if (previous == null) {
                added.add(toDto.apply(entry.getValue()));
            } else if (hasChanged(previous, entry.getValue())) {
                updated.add(toDto.apply(entry.getValue()));
            }
        }

        return new AircraftDeltaMessage(Instant.now(), added, updated, removedIcao24s);
    }

    private boolean hasChanged(AircraftState a, AircraftState b) {
        return !Objects.equals(a.getLatitude(), b.getLatitude())
                || !Objects.equals(a.getLongitude(), b.getLongitude())
                || !Objects.equals(a.getAltitudeMeters(), b.getAltitudeMeters())
                || !Objects.equals(a.getSpeedMs(), b.getSpeedMs())
                || !Objects.equals(a.getHeadingDegrees(), b.getHeadingDegrees())
                || !Objects.equals(a.getVerticalRateMs(), b.getVerticalRateMs())
                || !Objects.equals(a.getOnGround(), b.getOnGround())
                || a.getStatus() != b.getStatus()
                || !Objects.equals(a.getCallsign(), b.getCallsign());
    }
}
