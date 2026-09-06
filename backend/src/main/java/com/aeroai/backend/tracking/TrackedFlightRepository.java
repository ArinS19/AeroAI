package com.aeroai.backend.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrackedFlightRepository extends JpaRepository<TrackedFlight, Long> {

    List<TrackedFlight> findByUserIdAndActiveTrue(Long userId);

    long countByUserIdAndActiveTrue(Long userId);

    Optional<TrackedFlight> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndAircraftIdentifierAndActiveTrue(Long userId, String aircraftIdentifier);

    /** Reverse lookup for alert fan-out (Section 22.2): who is tracking this aircraft right now. */
    List<TrackedFlight> findByAircraftIdentifierAndActiveTrue(String aircraftIdentifier);
}
