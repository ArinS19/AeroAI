package com.aeroai.backend.flight.dto;

/**
 * One real, actually-observed trail point (Section 14). Never a
 * synthesized/interpolated point - interpolation is a client-side display
 * concern only (Section 13.5).
 */
public record TrailPoint(double lat, double lon, long timestamp) {
}
