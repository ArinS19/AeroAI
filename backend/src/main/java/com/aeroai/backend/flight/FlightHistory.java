package com.aeroai.backend.flight;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Section 22.3 - durable position history, bounded to HISTORY_RETENTION_DAYS
 * (Section 23). One row per aircraft per ingestion cycle (or downsampled).
 */
@Entity
@Table(name = "flight_history")
public class FlightHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aircraft_identifier", nullable = false)
    private String aircraftIdentifier;

    @Column(name = "callsign")
    private String callsign;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "altitude")
    private Double altitude;

    @Column(name = "speed")
    private Double speed;

    @Column(name = "heading")
    private Double heading;

    @Column(name = "vertical_rate")
    private Double verticalRate;

    @Column(name = "on_ground")
    private Boolean onGround;

    @Column(name = "status")
    private String status;

    protected FlightHistory() {
    }

    public FlightHistory(String aircraftIdentifier, String callsign, Instant timestamp, double latitude, double longitude,
                          Double altitude, Double speed, Double heading, Double verticalRate, Boolean onGround, String status) {
        this.aircraftIdentifier = aircraftIdentifier;
        this.callsign = callsign;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speed = speed;
        this.heading = heading;
        this.verticalRate = verticalRate;
        this.onGround = onGround;
        this.status = status;
    }

    public Long getId() { return id; }
    public String getAircraftIdentifier() { return aircraftIdentifier; }
    public String getCallsign() { return callsign; }
    public Instant getTimestamp() { return timestamp; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Double getAltitude() { return altitude; }
    public Double getSpeed() { return speed; }
    public Double getHeading() { return heading; }
    public Double getVerticalRate() { return verticalRate; }
    public Boolean getOnGround() { return onGround; }
    public String getStatus() { return status; }
}
