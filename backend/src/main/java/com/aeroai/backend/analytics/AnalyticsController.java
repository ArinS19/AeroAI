package com.aeroai.backend.analytics;

import com.aeroai.backend.analytics.dto.AnalyticsDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Section 44/46 - GET /api/analytics?range={today|7d}&airspaceId={cityId}. Public.
 */
@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/api/analytics")
    public AnalyticsDto getAnalytics(@RequestParam(defaultValue = "today") String range,
                                      @RequestParam(required = false) String airspaceId) {
        return analyticsService.getAnalytics(range, airspaceId);
    }
}
