package com.aeroai.backend.prediction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {
    // Note: a derived-query method name here would be ambiguous because the
    // property name itself contains "Or" (flightOrAircraftIdentifier), which
    // Spring Data's method-name parser would otherwise split on - hence an
    // explicit @Query instead of findByFlightOrAircraftIdentifierOrderBy...
    @Query("SELECT p FROM Prediction p WHERE p.flightOrAircraftIdentifier = :identifier ORDER BY p.createdAt DESC")
    List<Prediction> findByIdentifierOrderByCreatedAtDesc(@Param("identifier") String identifier);

    long deleteByCreatedAtBefore(Instant cutoff);

    long countByCreatedAtGreaterThanEqual(Instant since);

    @Query("SELECT AVG(p.predictedDelayMinutes) FROM Prediction p WHERE p.createdAt >= :since")
    Double averageDelayMinutesSince(@Param("since") Instant since);

    @Query("SELECT p.category, COUNT(p) FROM Prediction p WHERE p.createdAt >= :since GROUP BY p.category")
    List<Object[]> categoryDistributionSince(@Param("since") Instant since);
}
