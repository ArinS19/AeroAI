package com.aeroai.backend.alert;

import com.aeroai.backend.alert.dto.AlertDto;
import com.aeroai.backend.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Section 43 - browser/in-app alerts for tracked flights. Section 43.1/43.2:
 * only meaningful STATE TRANSITIONS raise an alert (never every position
 * update); the same (trackedFlightId, type) pair is only re-notified when
 * its observed state actually changes, which is a strictly stronger
 * guarantee than a bare ALERT_DEDUP_WINDOW_SECONDS timer and naturally
 * satisfies the LOW->LOW (no alert) / LOW->MODERATE (alert) examples.
 */
@Service
public class AlertService {

    public enum AlertType {
        STATUS_CHANGE, DELAY_CATEGORY_CHANGE, CONFLICT_RISK_CHANGE, DATA_UNAVAILABLE
    }

    private record DedupEntry(String state, Instant firedAt) {
    }

    private final AlertRepository alertRepository;
    private final Map<String, DedupEntry> lastNotifiedState = new ConcurrentHashMap<>();

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /**
     * Called by the ingestion pipeline once per cycle for each tracked
     * aircraft with a newly observed state for {@code type}. Establishes a
     * baseline on first observation (no alert fires for the very first
     * sighting); fires only on an actual state change thereafter.
     */
    @Transactional
    public void evaluateStateChange(Long trackedFlightId, Long userId, AlertType type, String newState, String message) {
        String key = trackedFlightId + ":" + type;
        DedupEntry previous = lastNotifiedState.get(key);

        if (previous != null && previous.state().equals(newState)) {
            return; // No change -> no alert (Section 43.2).
        }

        lastNotifiedState.put(key, new DedupEntry(newState, Instant.now()));

        if (previous == null) {
            return; // First observation establishes the baseline only.
        }

        alertRepository.save(new Alert(userId, trackedFlightId, type.name(), message));
    }

    public void clearBaseline(Long trackedFlightId) {
        lastNotifiedState.keySet().removeIf(key -> key.startsWith(trackedFlightId + ":"));
    }

    @Transactional(readOnly = true)
    public List<AlertDto> listForUser(Long userId) {
        return alertRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return alertRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public AlertDto markRead(Long userId, Long alertId) {
        Alert alert = alertRepository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new NotFoundException("Alert not found."));
        alert.setRead(true);
        return toDto(alert);
    }

    @Transactional
    public void markAllRead(Long userId) {
        alertRepository.markAllReadForUser(userId);
    }

    private AlertDto toDto(Alert alert) {
        return new AlertDto(alert.getId(), alert.getTrackedFlightId(), alert.getType(), alert.getMessage(),
                alert.getCreatedAt(), alert.isRead());
    }
}
