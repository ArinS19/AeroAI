package com.aeroai.backend.tracking;

import com.aeroai.backend.common.exception.NotFoundException;
import com.aeroai.backend.flight.FlightService;
import com.aeroai.backend.flight.dto.AircraftDto;
import com.aeroai.backend.tracking.dto.TrackFlightRequest;
import com.aeroai.backend.tracking.dto.TrackedFlightDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Section 41/42 - Track/untrack a flight, My Flights. Section 40 - every
 * method here is scoped by the authenticated user's ID only, never a
 * client-supplied user ID.
 */
@Service
@Transactional
public class TrackingService {

    private final TrackedFlightRepository trackedFlightRepository;
    private final FlightService flightService;

    public TrackingService(TrackedFlightRepository trackedFlightRepository, FlightService flightService) {
        this.trackedFlightRepository = trackedFlightRepository;
        this.flightService = flightService;
    }

    public TrackedFlightDto track(Long userId, TrackFlightRequest request) {
        String icao24 = request.icao24().toLowerCase();

        Optional<TrackedFlight> existing = trackedFlightRepository.findByUserIdAndActiveTrue(userId).stream()
                .filter(tf -> tf.getAircraftIdentifier().equalsIgnoreCase(icao24))
                .findFirst();
        TrackedFlight trackedFlight = existing.orElseGet(() ->
                trackedFlightRepository.save(new TrackedFlight(userId, icao24, request.callsign())));

        return toDto(trackedFlight);
    }

    public void untrack(Long userId, Long trackedFlightId) {
        TrackedFlight trackedFlight = trackedFlightRepository.findByIdAndUserId(trackedFlightId, userId)
                .orElseThrow(() -> new NotFoundException("Tracked flight not found."));
        trackedFlightRepository.delete(trackedFlight);
    }

    @Transactional(readOnly = true)
    public List<TrackedFlightDto> listMyFlights(Long userId) {
        return trackedFlightRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(this::toDto)
                .toList();
    }

    private TrackedFlightDto toDto(TrackedFlight trackedFlight) {
        Optional<AircraftDto> current = flightService.findCurrentByIcao24(trackedFlight.getAircraftIdentifier());
        if (current.isEmpty()) {
            return new TrackedFlightDto(
                    trackedFlight.getId(),
                    trackedFlight.getAircraftIdentifier(),
                    trackedFlight.getCallsign(),
                    "UNKNOWN",
                    null,
                    null,
                    null,
                    false
            );
        }
        AircraftDto aircraft = current.get();
        return new TrackedFlightDto(
                trackedFlight.getId(),
                trackedFlight.getAircraftIdentifier(),
                aircraft.callsign() != null ? aircraft.callsign() : trackedFlight.getCallsign(),
                aircraft.status(),
                null, // predicted delay - Unavailable (no reliable schedule source, see FlightService.getDetail)
                null,
                aircraft.lastUpdate(),
                true
        );
    }
}
