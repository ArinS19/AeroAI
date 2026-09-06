package com.aeroai.backend.maintenance;

import com.aeroai.backend.alert.AlertRepository;
import com.aeroai.backend.config.AppProperties;
import com.aeroai.backend.flight.FlightHistoryRepository;
import com.aeroai.backend.prediction.PredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Section 67 - scheduled cleanup so no table receiving ongoing writes
 * (flight_history, predictions, alerts) grows unbounded. Redis keys are
 * self-cleaning via TTL/ZSET trim (Section 20) and need no separate job.
 */
@Component
public class RetentionCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionCleanupJob.class);

    private final FlightHistoryRepository historyRepository;
    private final PredictionRepository predictionRepository;
    private final AlertRepository alertRepository;
    private final AppProperties appProperties;

    public RetentionCleanupJob(FlightHistoryRepository historyRepository, PredictionRepository predictionRepository,
                                AlertRepository alertRepository, AppProperties appProperties) {
        this.historyRepository = historyRepository;
        this.predictionRepository = predictionRepository;
        this.alertRepository = alertRepository;
        this.appProperties = appProperties;
    }

    /** Nightly at 02:30 server time. */
    @Scheduled(cron = "0 30 2 * * *")
    @Transactional
    public void runNightlyCleanup() {
        cleanupFlightHistory();
        cleanupPredictions();
        cleanupAlerts();
    }

    @Transactional
    public void cleanupFlightHistory() {
        Instant cutoff = Instant.now().minusSeconds(appProperties.getHistory().getRetentionDays() * 24L * 3600);
        try {
            long deleted = historyRepository.deleteByTimestampBefore(cutoff);
            log.info("Retention cleanup: removed {} flight_history rows older than {} days.", deleted,
                    appProperties.getHistory().getRetentionDays());
        } catch (Exception ex) {
            log.warn("flight_history retention cleanup failed: {}", ex.getMessage());
        }
    }

    @Transactional
    public void cleanupPredictions() {
        Instant cutoff = Instant.now().minusSeconds(appProperties.getPrediction().getRetentionDays() * 24L * 3600);
        try {
            long deleted = predictionRepository.deleteByCreatedAtBefore(cutoff);
            log.info("Retention cleanup: removed {} prediction rows older than {} days.", deleted,
                    appProperties.getPrediction().getRetentionDays());
        } catch (Exception ex) {
            log.warn("predictions retention cleanup failed: {}", ex.getMessage());
        }
    }

    @Transactional
    public void cleanupAlerts() {
        Instant cutoff = Instant.now().minusSeconds(appProperties.getAlert().getRetentionDays() * 24L * 3600);
        try {
            long deleted = alertRepository.deleteByReadTrueAndCreatedAtBefore(cutoff);
            log.info("Retention cleanup: removed {} read alert rows older than {} days.", deleted,
                    appProperties.getAlert().getRetentionDays());
        } catch (Exception ex) {
            log.warn("alerts retention cleanup failed: {}", ex.getMessage());
        }
    }
}
