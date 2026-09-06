package com.aeroai.backend.conflict;

import com.aeroai.backend.common.util.GeoMath;
import com.aeroai.backend.config.AppProperties;
import com.aeroai.backend.flight.AircraftState;
import com.aeroai.backend.flight.ConflictSnapshotStore;
import com.aeroai.backend.flight.dto.ConflictRiskDto;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Section 36/38 - basic mathematical/research conflict-risk detection. NOT
 * TCAS, official collision avoidance, or certified ATC (Rule 16). Run once
 * per ingestion cycle (Section 38), never per-request/per-render.
 */
@Component
public class ConflictEngine {

    private static final double GRID_CELL_DEGREES = 0.5;

    private final ConflictSnapshotStore snapshotStore;
    private final AppProperties props;

    public ConflictEngine(ConflictSnapshotStore snapshotStore, AppProperties props) {
        this.snapshotStore = snapshotStore;
        this.props = props;
    }

    /**
     * Computes all MEDIUM/HIGH-risk pairs among {@code states} and publishes
     * them to the {@link ConflictSnapshotStore} for this ingestion cycle.
     * LOW-risk pairs are not stored/reported at all (Section 37 - LOW is the
     * implicit "everything else" state, never individually rendered).
     */
    public void computeAndPublish(List<AircraftState> states) {
        List<AircraftState> candidates = states.stream()
                .filter(this::hasReliableKinematics)
                .toList();

        List<PairResult> results = new ArrayList<>();
        Set<String> processedPairs = new HashSet<>();

        for (AircraftState a : candidates) {
            for (AircraftState b : findCandidateNeighbors(a, candidates)) {
                if (a.getIcao24().equals(b.getIcao24())) {
                    continue;
                }
                String pairKey = pairKey(a.getIcao24(), b.getIcao24());
                if (!processedPairs.add(pairKey)) {
                    continue;
                }
                double roughKm = GeoMath.haversineKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
                if (roughKm > props.getConflict().getCandidateRadiusKm()) {
                    continue;
                }
                PairResult result = evaluatePair(a, b);
                if (!"LOW".equals(result.riskLevel)) {
                    results.add(result);
                }
            }
        }

        Map<String, List<ConflictRiskDto>> byIcao24 = new HashMap<>();
        List<ConflictRiskDto> allPairs = new ArrayList<>();
        for (PairResult r : results) {
            byIcao24.computeIfAbsent(r.aIcao24, k -> new ArrayList<>())
                    .add(ConflictRiskDto.of(r.bIcao24, r.bCallsign, r.riskLevel, r.horizontalDistanceKm,
                            r.altitudeDiffM, r.closureRateMs, r.dcpaHorizontalM, r.altitudeDiffAtTcpaM, r.tcpaSeconds, r.calculatedAt));
            byIcao24.computeIfAbsent(r.bIcao24, k -> new ArrayList<>())
                    .add(ConflictRiskDto.of(r.aIcao24, r.aCallsign, r.riskLevel, r.horizontalDistanceKm,
                            r.altitudeDiffM, r.closureRateMs, r.dcpaHorizontalM, r.altitudeDiffAtTcpaM, r.tcpaSeconds, r.calculatedAt));
            allPairs.add(ConflictRiskDto.of(r.bIcao24, r.bCallsign, r.riskLevel, r.horizontalDistanceKm,
                    r.altitudeDiffM, r.closureRateMs, r.dcpaHorizontalM, r.altitudeDiffAtTcpaM, r.tcpaSeconds, r.calculatedAt));
        }
        snapshotStore.replaceAll(byIcao24, allPairs);
    }

    private boolean hasReliableKinematics(AircraftState s) {
        return s.hasReliablePosition() && s.getAltitudeMeters() != null
                && s.getSpeedMs() != null && s.getHeadingDegrees() != null
                && Boolean.FALSE.equals(s.getOnGround());
    }

    private PairResult evaluatePair(AircraftState a, AircraftState b) {
        double horizontalDistanceKm = GeoMath.haversineKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
        double altitudeDiffM = Math.abs(a.getAltitudeMeters() - b.getAltitudeMeters());

        GeoMath.Vector2 vA = GeoMath.velocityFromSpeedHeading(a.getSpeedMs(), a.getHeadingDegrees());
        GeoMath.Vector2 vB = GeoMath.velocityFromSpeedHeading(b.getSpeedMs(), b.getHeadingDegrees());
        GeoMath.Vector2 relativePosition = GeoMath.relativePositionMeters(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
        GeoMath.Vector2 relativeVelocity = vB.subtract(vA);

        double closureRateMs = GeoMath.closureRateMs(relativePosition, relativeVelocity);
        double tcpa = GeoMath.tcpaSeconds(relativePosition, relativeVelocity);

        Double dcpaHorizontalM = null;
        Double altitudeDiffAtTcpaM = null;
        Double tcpaOut = null;
        String riskLevel = "LOW";

        boolean tcpaValid = !Double.isNaN(tcpa) && tcpa >= 0 && tcpa <= props.getConflict().getPredictionHorizonSeconds();
        if (tcpaValid) {
            tcpaOut = tcpa;
            GeoMath.Vector2 positionAtTcpa = relativePosition.add(relativeVelocity.scale(tcpa));
            dcpaHorizontalM = positionAtTcpa.magnitude();

            double vrateA = a.getVerticalRateMs() != null ? a.getVerticalRateMs() : 0.0;
            double vrateB = b.getVerticalRateMs() != null ? b.getVerticalRateMs() : 0.0;
            double altA = a.getAltitudeMeters() + vrateA * tcpa;
            double altB = b.getAltitudeMeters() + vrateB * tcpa;
            altitudeDiffAtTcpaM = Math.abs(altA - altB);

            if (dcpaHorizontalM <= props.getConflict().getHighHorizontalM() && altitudeDiffAtTcpaM <= props.getConflict().getHighVerticalM()) {
                riskLevel = "HIGH";
            } else if (dcpaHorizontalM <= props.getConflict().getMediumHorizontalM() && altitudeDiffAtTcpaM <= props.getConflict().getMediumVerticalM()) {
                riskLevel = "MEDIUM";
            }
        }

        PairResult result = new PairResult();
        result.aIcao24 = a.getIcao24();
        result.aCallsign = a.getCallsign();
        result.bIcao24 = b.getIcao24();
        result.bCallsign = b.getCallsign();
        result.horizontalDistanceKm = horizontalDistanceKm;
        result.altitudeDiffM = altitudeDiffM;
        result.closureRateMs = closureRateMs;
        result.dcpaHorizontalM = dcpaHorizontalM;
        result.altitudeDiffAtTcpaM = altitudeDiffAtTcpaM;
        result.tcpaSeconds = tcpaOut;
        result.riskLevel = riskLevel;
        result.calculatedAt = Instant.now();
        return result;
    }

    // ---------------------------------------------------------------- spatial candidate filtering (Section 36.2 Step 8)

    /**
     * Coarse-grained candidate filter: a bounding-box prefilter (equivalent
     * to checking an aircraft's own ~1 grid-cell neighborhood at
     * GRID_CELL_DEGREES resolution) followed by the exact haversine check
     * in {@link #computeAndPublish}. With aircraft counts in the low
     * hundreds (Section 38), this avoids naive O(N^2) full-precision math
     * without the added indirection of an explicit bucket map, which only
     * pays off at much larger N.
     */
    private List<AircraftState> findCandidateNeighbors(AircraftState subject, List<AircraftState> all) {
        double radiusDegrees = Math.max(props.getConflict().getCandidateRadiusKm() / 111.32, GRID_CELL_DEGREES);
        List<AircraftState> neighbors = new ArrayList<>();
        for (AircraftState other : all) {
            if (other == subject) {
                continue;
            }
            if (Math.abs(other.getLatitude() - subject.getLatitude()) <= radiusDegrees
                    && Math.abs(other.getLongitude() - subject.getLongitude()) <= radiusDegrees) {
                neighbors.add(other);
            }
        }
        return neighbors;
    }

    private String pairKey(String icao24A, String icao24B) {
        return icao24A.compareTo(icao24B) < 0 ? icao24A + "|" + icao24B : icao24B + "|" + icao24A;
    }

    private static class PairResult {
        String aIcao24;
        String aCallsign;
        String bIcao24;
        String bCallsign;
        double horizontalDistanceKm;
        double altitudeDiffM;
        double closureRateMs;
        Double dcpaHorizontalM;
        Double altitudeDiffAtTcpaM;
        Double tcpaSeconds;
        String riskLevel;
        Instant calculatedAt;
    }
}
