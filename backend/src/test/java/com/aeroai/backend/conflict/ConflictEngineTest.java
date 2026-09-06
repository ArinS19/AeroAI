package com.aeroai.backend.conflict;

import com.aeroai.backend.config.AppProperties;
import com.aeroai.backend.flight.AircraftState;
import com.aeroai.backend.flight.ConflictSnapshotStore;
import com.aeroai.backend.flight.dto.ConflictRiskDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Section 75 - conflict calculations unit-tested against known
 * independently-computed input/output pairs: two aircraft on a head-on
 * collision course vs. two aircraft on diverging tracks.
 */
class ConflictEngineTest {

    private AircraftState aircraft(String icao24, double lat, double lon, double altitudeM, double speedMs, double headingDeg) {
        AircraftState state = new AircraftState();
        state.setIcao24(icao24);
        state.setCallsign(icao24.toUpperCase());
        state.setLatitude(lat);
        state.setLongitude(lon);
        state.setAltitudeMeters(altitudeM);
        state.setSpeedMs(speedMs);
        state.setHeadingDegrees(headingDeg);
        state.setVerticalRateMs(0.0);
        state.setOnGround(false);
        return state;
    }

    @Test
    void headOnCollisionCourse_isClassifiedHigh() {
        ConflictSnapshotStore store = new ConflictSnapshotStore();
        ConflictEngine engine = new ConflictEngine(store, new AppProperties());

        // A moving due east at 200 m/s; B ~2.1km east of A moving due west at 200 m/s -> closing head-on.
        AircraftState a = aircraft("aaaaaa", 19.0, 72.0, 10000, 200, 90);
        AircraftState b = aircraft("bbbbbb", 19.0, 72.02, 10000, 200, 270);

        engine.computeAndPublish(List.of(a, b));

        List<ConflictRiskDto> forA = store.getFor("aaaaaa");
        assertEquals(1, forA.size());
        assertEquals("HIGH", forA.get(0).riskLevel());
        assertEquals("bbbbbb", forA.get(0).otherIcao24());
        assertTrue(forA.get(0).timeToClosestApproachSeconds() > 0);
        assertTrue(forA.get(0).dcpaHorizontalM() < 3000);
    }

    @Test
    void divergingTracks_isNotReportedAsConflict() {
        ConflictSnapshotStore store = new ConflictSnapshotStore();
        ConflictEngine engine = new ConflictEngine(store, new AppProperties());

        // Same starting geometry as the collision case, but headings reversed -> moving apart.
        AircraftState a = aircraft("aaaaaa", 19.0, 72.0, 10000, 200, 270);
        AircraftState b = aircraft("bbbbbb", 19.0, 72.02, 10000, 200, 90);

        engine.computeAndPublish(List.of(a, b));

        assertTrue(store.getFor("aaaaaa").isEmpty(), "Diverging aircraft must not be reported as a conflict pair (LOW is implicit).");
        assertTrue(store.getFor("bbbbbb").isEmpty());
    }

    @Test
    void aircraftFarApart_isNotReportedAsConflict() {
        ConflictSnapshotStore store = new ConflictSnapshotStore();
        ConflictEngine engine = new ConflictEngine(store, new AppProperties());

        AircraftState a = aircraft("aaaaaa", 19.0, 72.0, 10000, 200, 90);
        AircraftState b = aircraft("cccccc", 30.0, 80.0, 10000, 200, 270); // far away, outside candidate radius

        engine.computeAndPublish(List.of(a, b));

        assertTrue(store.getFor("aaaaaa").isEmpty());
    }

    @Test
    void largeAltitudeSeparation_downgradesRiskDespiteHorizontalProximity() {
        ConflictSnapshotStore store = new ConflictSnapshotStore();
        ConflictEngine engine = new ConflictEngine(store, new AppProperties());

        // Same horizontal collision geometry as the HIGH test, but 2000m vertical separation.
        AircraftState a = aircraft("aaaaaa", 19.0, 72.0, 8000, 200, 90);
        AircraftState b = aircraft("bbbbbb", 19.0, 72.02, 10000, 200, 270);

        engine.computeAndPublish(List.of(a, b));

        assertTrue(store.getFor("aaaaaa").isEmpty(), "2000m vertical separation should be classified LOW, not HIGH/MEDIUM.");
    }
}
