package com.aeroai.backend.alert;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadFalse(Long userId);

    Optional<Alert> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE Alert a SET a.read = true WHERE a.userId = :userId AND a.read = false")
    int markAllReadForUser(@Param("userId") Long userId);

    long deleteByReadTrueAndCreatedAtBefore(Instant cutoff);
}
