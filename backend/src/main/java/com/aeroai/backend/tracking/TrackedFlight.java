package com.aeroai.backend.tracking;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Section 22.2 - a relationship, not a data snapshot. `callsign` is a
 * convenience/display cache captured at tracking time; current live
 * information always comes from the live/derived state, never this row.
 */
@Entity
@Table(name = "tracked_flights")
public class TrackedFlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "aircraft_identifier", nullable = false)
    private String aircraftIdentifier;

    @Column(name = "callsign")
    private String callsign;

    @Column(name = "tracked_at", nullable = false)
    private Instant trackedAt;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected TrackedFlight() {
    }

    public TrackedFlight(Long userId, String aircraftIdentifier, String callsign) {
        this.userId = userId;
        this.aircraftIdentifier = aircraftIdentifier;
        this.callsign = callsign;
        this.trackedAt = Instant.now();
        this.active = true;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getAircraftIdentifier() { return aircraftIdentifier; }
    public String getCallsign() { return callsign; }
    public Instant getTrackedAt() { return trackedAt; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
