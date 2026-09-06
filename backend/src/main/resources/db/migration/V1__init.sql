-- AeroAI initial schema (IMPLEMENT_v2.md Section 22).
-- Column types/names follow the spec; slug/aliases added to airspaces as the
-- stable cityId + optional alternate names referenced in Section 13.2.2.

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE airspaces (
    id              BIGSERIAL PRIMARY KEY,
    city_name       VARCHAR(255) NOT NULL,
    country         VARCHAR(255) NOT NULL,
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    default_zoom    INTEGER NOT NULL,
    lamin           DOUBLE PRECISION NOT NULL,
    lomin           DOUBLE PRECISION NOT NULL,
    lamax           DOUBLE PRECISION NOT NULL,
    lomax           DOUBLE PRECISION NOT NULL,
    display_region  VARCHAR(255),
    aliases         VARCHAR(500),
    slug            VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE airports (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    iata        VARCHAR(3),
    icao        VARCHAR(4),
    city        VARCHAR(255),
    "location"  VARCHAR(255),
    country     VARCHAR(255),
    latitude    DOUBLE PRECISION NOT NULL,
    longitude   DOUBLE PRECISION NOT NULL
);

CREATE TABLE tracked_flights (
    id                    BIGSERIAL PRIMARY KEY,
    user_id               BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    aircraft_identifier   VARCHAR(24) NOT NULL,
    callsign              VARCHAR(16),
    tracked_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    active                BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE flight_history (
    id                    BIGSERIAL PRIMARY KEY,
    aircraft_identifier   VARCHAR(24) NOT NULL,
    callsign              VARCHAR(16),
    "timestamp"           TIMESTAMPTZ NOT NULL,
    latitude              DOUBLE PRECISION NOT NULL,
    longitude             DOUBLE PRECISION NOT NULL,
    altitude              DOUBLE PRECISION,
    speed                 DOUBLE PRECISION,
    heading               DOUBLE PRECISION,
    vertical_rate         DOUBLE PRECISION,
    on_ground             BOOLEAN,
    status                VARCHAR(16)
);

CREATE TABLE predictions (
    id                              BIGSERIAL PRIMARY KEY,
    flight_or_aircraft_identifier   VARCHAR(24) NOT NULL,
    predicted_delay_minutes         DOUBLE PRECISION NOT NULL,
    delay_probability               DOUBLE PRECISION NOT NULL,
    category                        VARCHAR(16) NOT NULL,
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE alerts (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tracked_flight_id    BIGINT REFERENCES tracked_flights(id) ON DELETE CASCADE,
    type                 VARCHAR(32) NOT NULL,
    message              TEXT NOT NULL,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    read                 BOOLEAN NOT NULL DEFAULT false
);

-- Section 54 - Database indexes, each mapped to a named query pattern.
CREATE INDEX idx_tracked_flights_user_active ON tracked_flights (user_id, active);
CREATE INDEX idx_tracked_flights_aircraft_identifier ON tracked_flights (aircraft_identifier);

CREATE INDEX idx_flight_history_aircraft_timestamp ON flight_history (aircraft_identifier, "timestamp");
CREATE INDEX idx_flight_history_timestamp ON flight_history ("timestamp");

CREATE INDEX idx_alerts_user_read_created ON alerts (user_id, read, created_at DESC);

CREATE UNIQUE INDEX idx_airports_iata ON airports (iata) WHERE iata IS NOT NULL;
CREATE UNIQUE INDEX idx_airports_icao ON airports (icao) WHERE icao IS NOT NULL;
CREATE INDEX idx_airports_name ON airports (lower(name));
CREATE INDEX idx_airports_city ON airports (lower(city));

CREATE INDEX idx_airspaces_slug ON airspaces (slug);
CREATE INDEX idx_predictions_identifier ON predictions (flight_or_aircraft_identifier);
