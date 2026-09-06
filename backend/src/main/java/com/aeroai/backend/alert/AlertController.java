package com.aeroai.backend.alert;

import com.aeroai.backend.alert.dto.AlertDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Section 43.3/46 - notification panel endpoints. All require
 * authentication and are scoped to the current user only (Section 40).
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<AlertDto> list(@AuthenticationPrincipal Long userId) {
        return alertService.listForUser(userId);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal Long userId) {
        return Map.of("unreadCount", alertService.unreadCount(userId));
    }

    @PatchMapping("/{id}/read")
    public AlertDto markRead(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return alertService.markRead(userId, id);
    }

    @PatchMapping("/read-all")
    public void markAllRead(@AuthenticationPrincipal Long userId) {
        alertService.markAllRead(userId);
    }
}
