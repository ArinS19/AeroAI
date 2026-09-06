package com.aeroai.backend.flight;

import com.aeroai.backend.maintenance.RetentionCleanupJob;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Section 67/75 - retention cleanup, verified end-to-end against the REAL
 * local PostgreSQL database and the actual production
 * {@link RetentionCleanupJob}, not a mock.
 */
@SpringBootTest
@ActiveProfiles("test")
class RetentionCleanupIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RetentionCleanupJob retentionCleanupJob;

    private String identifier;

    @AfterEach
    void cleanup() {
        if (identifier != null) {
            jdbcTemplate.update("DELETE FROM flight_history WHERE aircraft_identifier = ?", identifier);
        }
    }

    @Test
    void oldFlightHistoryRows_areDeletedByRetentionCutoff_recentRowsSurvive() {
        identifier = "retest" + (System.nanoTime() % 1_000_000);
        Instant old = Instant.now().minus(10, ChronoUnit.DAYS); // older than the 7-day default retention
        Instant recent = Instant.now().minus(1, ChronoUnit.HOURS);

        jdbcTemplate.update(
                "INSERT INTO flight_history (aircraft_identifier, callsign, \"timestamp\", latitude, longitude, status) VALUES (?, ?, ?, ?, ?, ?)",
                identifier, "TESTOLD", java.sql.Timestamp.from(old), 19.0, 72.0, "IN_FLIGHT");
        jdbcTemplate.update(
                "INSERT INTO flight_history (aircraft_identifier, callsign, \"timestamp\", latitude, longitude, status) VALUES (?, ?, ?, ?, ?, ?)",
                identifier, "TESTRECENT", java.sql.Timestamp.from(recent), 19.0, 72.0, "IN_FLIGHT");

        Integer beforeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flight_history WHERE aircraft_identifier = ?", Integer.class, identifier);
        assertEquals(2, beforeCount);

        retentionCleanupJob.cleanupFlightHistory();

        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flight_history WHERE aircraft_identifier = ?", Integer.class, identifier);
        assertEquals(1, remaining, "Only the recent row should remain after retention cleanup.");

        String remainingCallsign = jdbcTemplate.queryForObject(
                "SELECT callsign FROM flight_history WHERE aircraft_identifier = ?", String.class, identifier);
        assertEquals("TESTRECENT", remainingCallsign);
    }
}
