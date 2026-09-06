package com.aeroai.backend.airport;

import com.aeroai.backend.airport.dto.AirportDto;
import com.aeroai.backend.common.exception.NotFoundException;
import com.aeroai.backend.common.exception.ValidationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AirportService {

    private static final int MAX_QUERY_LENGTH = 100;

    private final AirportRepository airportRepository;

    public AirportService(AirportRepository airportRepository) {
        this.airportRepository = airportRepository;
    }

    public Page<AirportDto> list(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return airportRepository.findAll(pageable).map(this::toDto);
    }

    public AirportDto getById(Long id) {
        Airport airport = airportRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Airport not found."));
        return toDto(airport);
    }

    public List<AirportDto> search(String query) {
        String trimmed = validateQuery(query);
        if (trimmed.isEmpty()) {
            return List.of();
        }
        return airportRepository.search(trimmed).stream()
                .distinct()
                .map(this::toDto)
                .limit(50)
                .toList();
    }

    public List<Airport> findByCity(String city) {
        if (city == null || city.isBlank()) {
            return List.of();
        }
        return airportRepository.findByCityIgnoreCase(city.trim());
    }

    public List<Airport> findWithinBoundingBox(double latMin, double latMax, double lonMin, double lonMax) {
        return airportRepository.findWithinBoundingBox(latMin, latMax, lonMin, lonMax);
    }

    Airport requireEntityById(Long id) {
        return airportRepository.findById(id).orElseThrow(() -> new NotFoundException("Airport not found."));
    }

    private String validateQuery(String query) {
        if (query == null) {
            throw new ValidationException("Search query is required.");
        }
        String trimmed = query.trim();
        if (trimmed.length() > MAX_QUERY_LENGTH) {
            throw new ValidationException("Search query is too long.");
        }
        return trimmed;
    }

    AirportDto toDto(Airport airport) {
        return new AirportDto(
                airport.getId(),
                airport.getName(),
                airport.getIata(),
                airport.getIcao(),
                airport.getCity(),
                airport.getLocation(),
                airport.getCountry(),
                airport.getLatitude(),
                airport.getLongitude()
        );
    }
}
