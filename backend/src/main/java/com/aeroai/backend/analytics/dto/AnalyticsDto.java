package com.aeroai.backend.analytics.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Section 44 - Analytics DTO. Every number here is computed from AeroAI's
 * own stored/calculated data (PostgreSQL history/predictions or the live
 * Redis/in-memory snapshot) - never a claim about what OpenSky itself
 * reports historically (Section 68).
 */
public record AnalyticsDto(
        String range,
        String airspaceId,
        LiveTraffic liveTraffic,
        FlightStats flightStats,
        ConflictStats conflictStats,
        SystemHealth systemHealth
) {
    public record LiveTraffic(
            long totalAircraftVisible,
            Map<String, Long> byAltitudeBand,
            Map<String, Long> byStatus,
            long arrivalsInRange,
            long departuresInRange
    ) {
    }

    public record FlightStats(
            long flightsMonitored,
            long activeFlights,
            long completedFlights,
            Double averagePredictedDelayMinutes,
            Map<String, Long> delayCategoryDistribution
    ) {
    }

    public record ConflictStats(
            long potentialConflicts,
            long mediumRiskCount,
            long highRiskCount,
            List<ConflictEvent> recentConflictEvents
    ) {
        public record ConflictEvent(String icao24, String otherIcao24, String riskLevel, Instant calculatedAt) {
        }
    }

    public record SystemHealth(
            Instant lastOpenSkyUpdate,
            String freshnessLabel,
            String openSkyStatus,
            String openMeteoStatus,
            String mlServiceStatus,
            long totalTrackedFlights
    ) {
    }
}
