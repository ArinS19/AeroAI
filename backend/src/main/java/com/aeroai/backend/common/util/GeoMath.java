package com.aeroai.backend.common.util;

/**
 * Deterministic geographic/kinematic math shared by flight derived-data
 * (Section 25) and conflict-risk detection (Section 36.2). Never re-derived
 * independently by the frontend - this is the single source of truth.
 */
public final class GeoMath {

    /** Mean Earth radius in km, per Section 25.2. */
    public static final double EARTH_RADIUS_KM = 6371.0088;

    /** Meters per degree of latitude, used for the local equirectangular approximation (Section 36.2 Step 4). */
    public static final double METERS_PER_DEGREE_LAT = 111320.0;

    private GeoMath() {
    }

    /**
     * Great-circle distance between two points in kilometers (Section 25.2 haversine formula).
     */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaPhi = Math.toRadians(lat2 - lat1);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /** 2D vector, used for local east/north tangent-plane representations. */
    public record Vector2(double east, double north) {
        public double magnitude() {
            return Math.sqrt(east * east + north * north);
        }

        public double dot(Vector2 other) {
            return this.east * other.east + this.north * other.north;
        }

        public Vector2 scale(double factor) {
            return new Vector2(east * factor, north * factor);
        }

        public Vector2 add(Vector2 other) {
            return new Vector2(east + other.east, north + other.north);
        }

        public Vector2 subtract(Vector2 other) {
            return new Vector2(east - other.east, north - other.north);
        }
    }

    /**
     * Converts (speed, heading) into a local East-North velocity vector (Section 36.2 Step 3).
     * Heading is degrees clockwise from true north; speed in any consistent unit (m/s here).
     */
    public static Vector2 velocityFromSpeedHeading(double speedMs, double headingDegrees) {
        double headingRad = Math.toRadians(headingDegrees);
        double east = speedMs * Math.sin(headingRad);
        double north = speedMs * Math.cos(headingRad);
        return new Vector2(east, north);
    }

    /**
     * Approximate local Cartesian offset (meters, east/north) of point B relative to point A,
     * using an equirectangular approximation valid over conflict-check distances (Section 36.2 Step 4).
     */
    public static Vector2 relativePositionMeters(double latA, double lonA, double latB, double lonB) {
        double avgLatRad = Math.toRadians((latA + latB) / 2.0);
        double deltaLatM = (latB - latA) * METERS_PER_DEGREE_LAT;
        double deltaLonM = (lonB - lonA) * METERS_PER_DEGREE_LAT * Math.cos(avgLatRad);
        return new Vector2(deltaLonM, deltaLatM);
    }

    /**
     * Time to closest point of approach in seconds (Section 36.2 Step 5).
     * Returns Double.NaN if the relative velocity is ~zero (parallel/identical tracks) -
     * callers must treat that as "no convergence, use current separation only".
     */
    public static double tcpaSeconds(Vector2 relativePosition, Vector2 relativeVelocity) {
        double speedSquared = relativeVelocity.magnitude() * relativeVelocity.magnitude();
        if (speedSquared < 1e-6) {
            return Double.NaN;
        }
        return -(relativePosition.dot(relativeVelocity)) / speedSquared;
    }

    /**
     * Closure rate in m/s at the current instant (Section 36.2 Step 7).
     * Negative = closing, positive = opening. Returns 0 if aircraft are co-located (degenerate).
     */
    public static double closureRateMs(Vector2 relativePosition, Vector2 relativeVelocity) {
        double distance = relativePosition.magnitude();
        if (distance < 1e-6) {
            return 0.0;
        }
        return relativePosition.dot(relativeVelocity) / distance;
    }
}
