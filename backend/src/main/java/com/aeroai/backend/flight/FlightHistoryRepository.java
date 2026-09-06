package com.aeroai.backend.flight;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface FlightHistoryRepository extends JpaRepository<FlightHistory, Long> {

    List<FlightHistory> findByAircraftIdentifierAndTimestampGreaterThanEqualOrderByTimestampAsc(
            String aircraftIdentifier, Instant since);

    @Query("SELECT COUNT(DISTINCT h.aircraftIdentifier) FROM FlightHistory h WHERE h.timestamp >= :since")
    long countDistinctAircraftSince(@Param("since") Instant since);

    @Query("SELECT COUNT(DISTINCT h.aircraftIdentifier) FROM FlightHistory h WHERE h.timestamp >= :since AND h.status = :status")
    long countDistinctAircraftWithStatusSince(@Param("since") Instant since, @Param("status") String status);

    long deleteByTimestampBefore(Instant cutoff);

    long countByTimestampGreaterThanEqual(Instant since);
}
