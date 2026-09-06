package com.aeroai.backend.alert;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tracked_flight_id")
    private Long trackedFlightId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read", nullable = false)
    private boolean read;

    protected Alert() {
    }

    public Alert(Long userId, Long trackedFlightId, String type, String message) {
        this.userId = userId;
        this.trackedFlightId = trackedFlightId;
        this.type = type;
        this.message = message;
        this.createdAt = Instant.now();
        this.read = false;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getTrackedFlightId() { return trackedFlightId; }
    public String getType() { return type; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
