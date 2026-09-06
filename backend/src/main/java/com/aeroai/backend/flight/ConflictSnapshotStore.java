package com.aeroai.backend.flight;

import com.aeroai.backend.flight.dto.ConflictRiskDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the latest conflict-risk calculation results (Section 36/38),
 * recomputed once per ingestion cycle by the conflict engine and read by
 * flight details / analytics. In-memory (single backend instance, matching
 * the spec's single-scheduler-instance assumption in Section 18.3) rather
 * than Redis, since this is a derived cache rebuilt every ~15s regardless.
 */
@Component
public class ConflictSnapshotStore {

    private final Map<String, List<ConflictRiskDto>> byIcao24 = new ConcurrentHashMap<>();
    private volatile List<ConflictRiskDto> allActivePairs = List.of();

    public void replaceAll(Map<String, List<ConflictRiskDto>> newSnapshot, List<ConflictRiskDto> allPairs) {
        byIcao24.clear();
        byIcao24.putAll(newSnapshot);
        allActivePairs = allPairs;
    }

    public List<ConflictRiskDto> getFor(String icao24) {
        return byIcao24.getOrDefault(icao24, List.of());
    }

    /** All currently MEDIUM/HIGH risk pairs across the active airspace (Section 44.3 analytics). */
    public List<ConflictRiskDto> getAllActivePairs() {
        return allActivePairs;
    }
}
