package com.aeroai.backend.flight.dto;

import java.time.Instant;

/**
 * Section 36/37 - one calculated conflict-risk pair involving the subject
 * aircraft. This is a research/decision-support calculation, never an
 * official ATC/TCAS output (Rule 16). Null fields render as "Unavailable".
 */
public record ConflictRiskDto(
        String otherIcao24,
        String otherCallsign,
        String riskLevel,
        Double horizontalDistanceKm,
        Double altitudeDifferenceM,
        Double closureRateMs,
        Double dcpaHorizontalM,
        Double altitudeDiffAtTcpaM,
        Double timeToClosestApproachSeconds,
        Instant calculatedAt,
        String disclaimer
) {
    private static final String DISCLAIMER =
            "Research/decision-support estimate only. Not an official ATC, TCAS, or collision-avoidance warning.";

    public static ConflictRiskDto of(String otherIcao24, String otherCallsign, String riskLevel,
                                      double horizontalDistanceKm, double altitudeDifferenceM, double closureRateMs,
                                      double dcpaHorizontalM, double altitudeDiffAtTcpaM, Double tcpaSeconds, Instant calculatedAt) {
        return new ConflictRiskDto(otherIcao24, otherCallsign, riskLevel, horizontalDistanceKm, altitudeDifferenceM,
                closureRateMs, dcpaHorizontalM, altitudeDiffAtTcpaM, tcpaSeconds, calculatedAt, DISCLAIMER);
    }
}
