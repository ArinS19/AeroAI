package com.aeroai.backend.opensky;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * One raw OpenSky `states/all` state vector row (Section 18.1). Field
 * indices exactly match the OpenSky REST API's documented array positions.
 */
public record OpenSkyStateVector(
        String icao24,
        String callsign,
        String originCountry,
        Long timePosition,
        Long lastContact,
        Double longitude,
        Double latitude,
        Double baroAltitudeMeters,
        Boolean onGround,
        Double velocityMs,
        Double trueTrackDegrees,
        Double verticalRateMs,
        Double geoAltitudeMeters
) {
    public static OpenSkyStateVector fromJsonArray(JsonNode row) {
        return new OpenSkyStateVector(
                textOrNull(row, 0),
                trimmedTextOrNull(row, 1),
                textOrNull(row, 2),
                longOrNull(row, 3),
                longOrNull(row, 4),
                doubleOrNull(row, 5),
                doubleOrNull(row, 6),
                doubleOrNull(row, 7),
                boolOrNull(row, 8),
                doubleOrNull(row, 9),
                doubleOrNull(row, 10),
                doubleOrNull(row, 11),
                doubleOrNull(row, 13)
        );
    }

    /** Section 18.1 - prefer barometric altitude, fall back to geometric only if barometric is null. */
    public Double effectiveAltitudeMeters() {
        return baroAltitudeMeters != null ? baroAltitudeMeters : geoAltitudeMeters;
    }

    private static String textOrNull(JsonNode row, int index) {
        JsonNode node = row.get(index);
        return (node == null || node.isNull()) ? null : node.asText();
    }

    private static String trimmedTextOrNull(JsonNode row, int index) {
        String text = textOrNull(row, index);
        return text != null ? text.trim() : null;
    }

    private static Double doubleOrNull(JsonNode row, int index) {
        JsonNode node = row.get(index);
        return (node == null || node.isNull()) ? null : node.asDouble();
    }

    private static Long longOrNull(JsonNode row, int index) {
        JsonNode node = row.get(index);
        return (node == null || node.isNull()) ? null : node.asLong();
    }

    private static Boolean boolOrNull(JsonNode row, int index) {
        JsonNode node = row.get(index);
        return (node == null || node.isNull()) ? null : node.asBoolean();
    }
}
