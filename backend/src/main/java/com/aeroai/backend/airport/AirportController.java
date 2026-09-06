package com.aeroai.backend.airport;

import com.aeroai.backend.airport.dto.AirportDto;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Section 29/30 - Airports directory + detail. Public, no auth required.
 */
@RestController
@RequestMapping("/api/airports")
public class AirportController {

    private final AirportService airportService;

    public AirportController(AirportService airportService) {
        this.airportService = airportService;
    }

    @GetMapping
    public Page<AirportDto> list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "50") int size) {
        return airportService.list(page, size);
    }

    @GetMapping("/search")
    public List<AirportDto> search(@RequestParam("q") String query) {
        return airportService.search(query);
    }

    @GetMapping("/{id}")
    public AirportDto getById(@PathVariable Long id) {
        return airportService.getById(id);
    }
}
