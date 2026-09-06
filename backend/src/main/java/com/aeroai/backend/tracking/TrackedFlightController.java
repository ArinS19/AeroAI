package com.aeroai.backend.tracking;

import com.aeroai.backend.tracking.dto.TrackFlightRequest;
import com.aeroai.backend.tracking.dto.TrackedFlightDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Section 41/42/46 - Track/untrack + My Flights. All endpoints require
 * authentication (Section 50) - identity comes only from the JWT principal.
 */
@RestController
public class TrackedFlightController {

    private final TrackingService trackingService;

    public TrackedFlightController(TrackingService trackingService) {
        this.trackingService = trackingService;
    }

    @PostMapping("/api/tracked-flights")
    public ResponseEntity<TrackedFlightDto> track(@AuthenticationPrincipal Long userId,
                                                    @Valid @RequestBody TrackFlightRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trackingService.track(userId, request));
    }

    @DeleteMapping("/api/tracked-flights/{id}")
    public ResponseEntity<Void> untrack(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        trackingService.untrack(userId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/me/flights")
    public List<TrackedFlightDto> myFlights(@AuthenticationPrincipal Long userId) {
        return trackingService.listMyFlights(userId);
    }
}
