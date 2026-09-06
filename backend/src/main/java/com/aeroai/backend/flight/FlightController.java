package com.aeroai.backend.flight;

import com.aeroai.backend.flight.dto.AircraftDto;
import com.aeroai.backend.flight.dto.FlightDetailDto;
import com.aeroai.backend.flight.dto.FlightListItemDto;
import com.aeroai.backend.flight.dto.TrailResponseDto;
import com.aeroai.backend.tracking.TrackedFlightRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Section 46 - flight REST endpoints. All public (Section 2.1) - tracking
 * status is included opportunistically when a valid JWT is present, but the
 * endpoints themselves never require authentication.
 */
@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightService flightService;
    private final TrackedFlightRepository trackedFlightRepository;

    public FlightController(FlightService flightService, TrackedFlightRepository trackedFlightRepository) {
        this.flightService = flightService;
        this.trackedFlightRepository = trackedFlightRepository;
    }

    @GetMapping
    public List<FlightListItemDto> list(@RequestParam(required = false) String airspaceId,
                                         @RequestParam(required = false) String status,
                                         @RequestParam(required = false) String airline) {
        return flightService.list(airspaceId, status, airline);
    }

    @GetMapping("/current")
    public List<AircraftDto> currentAircraft(@RequestParam(required = false) String airspaceId) {
        return flightService.getCurrentAircraft(airspaceId);
    }

    @GetMapping("/search")
    public List<FlightListItemDto> search(@RequestParam("q") String query) {
        return flightService.search(query);
    }

    @GetMapping("/{icao24}")
    public FlightDetailDto getDetail(@PathVariable String icao24, @AuthenticationPrincipal Long userId) {
        FlightDetailDto detail = flightService.getDetail(icao24);
        if (userId == null) {
            return detail;
        }
        boolean tracked = trackedFlightRepository.existsByUserIdAndAircraftIdentifierAndActiveTrue(userId, icao24.toLowerCase());
        return withTracked(detail, tracked);
    }

    @GetMapping("/{icao24}/trail")
    public TrailResponseDto getTrail(@PathVariable String icao24, @RequestParam(defaultValue = "15") int minutes) {
        return flightService.getTrail(icao24, minutes);
    }

    private FlightDetailDto withTracked(FlightDetailDto d, boolean tracked) {
        return new FlightDetailDto(d.icao24(), d.callsign(), d.airline(), d.aircraftRegistration(), d.aircraftType(),
                d.origin(), d.destination(), d.departureTime(), d.arrivalTime(), d.status(), d.currentPosition(),
                d.distanceTravelledKm(), d.distanceRemainingKm(), d.estimatedArrival(), d.trailAvailable(),
                d.weather(), d.prediction(), d.conflicts(), tracked);
    }
}
