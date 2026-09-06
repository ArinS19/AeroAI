package com.aeroai.backend.prediction;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Durable prediction record (Section 22.4), distinct from the transient
 * Redis prediction cache. A row is written only when a new prediction is
 * actually computed (cache miss/refresh), never on every read.
 */
@Entity
@Table(name = "predictions")
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flight_or_aircraft_identifier", nullable = false)
    private String flightOrAircraftIdentifier;

    @Column(name = "predicted_delay_minutes", nullable = false)
    private double predictedDelayMinutes;

    @Column(name = "delay_probability", nullable = false)
    private double delayProbability;

    @Column(name = "category", nullable = false)
    private String category;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Prediction() {
    }

    public Prediction(String flightOrAircraftIdentifier, double predictedDelayMinutes, double delayProbability, String category) {
        this.flightOrAircraftIdentifier = flightOrAircraftIdentifier;
        this.predictedDelayMinutes = predictedDelayMinutes;
        this.delayProbability = delayProbability;
        this.category = category;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getFlightOrAircraftIdentifier() { return flightOrAircraftIdentifier; }
    public double getPredictedDelayMinutes() { return predictedDelayMinutes; }
    public double getDelayProbability() { return delayProbability; }
    public String getCategory() { return category; }
    public Instant getCreatedAt() { return createdAt; }
}
