package com.aeroai.backend.websocket;

import com.aeroai.backend.websocket.dto.AircraftDeltaMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Section 19 - publishes each ingestion cycle's delta to the city-scoped
 * STOMP topic `/topic/aircraft/{airspaceId}`.
 */
@Service
public class BroadcastService {

    private static final Logger log = LoggerFactory.getLogger(BroadcastService.class);

    private final SimpMessagingTemplate messagingTemplate;

    public BroadcastService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastDelta(String airspaceId, AircraftDeltaMessage message) {
        if (message.added().isEmpty() && message.updated().isEmpty() && message.removed().isEmpty()) {
            return; // Nothing changed this cycle - do not spam an empty delta.
        }
        try {
            messagingTemplate.convertAndSend("/topic/aircraft/" + airspaceId, message);
        } catch (Exception ex) {
            log.warn("Failed to broadcast aircraft delta for airspace {}: {}", airspaceId, ex.getMessage());
        }
    }
}
