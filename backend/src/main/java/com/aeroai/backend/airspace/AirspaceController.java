package com.aeroai.backend.airspace;

import com.aeroai.backend.airspace.dto.AirspaceDto;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Section 46 - GET /api/airspaces, GET /api/airspaces/{cityId}.
 */
@RestController
@RequestMapping("/api/airspaces")
public class AirspaceController {

    private final AirspaceService airspaceService;

    public AirspaceController(AirspaceService airspaceService) {
        this.airspaceService = airspaceService;
    }

    @GetMapping
    public List<AirspaceDto> list(@RequestParam(value = "q", required = false) String query) {
        return (query == null || query.isBlank()) ? airspaceService.list() : airspaceService.search(query);
    }

    @GetMapping("/{cityId}")
    public AirspaceDto getByCityId(@PathVariable String cityId) {
        return airspaceService.getBySlug(cityId);
    }
}
