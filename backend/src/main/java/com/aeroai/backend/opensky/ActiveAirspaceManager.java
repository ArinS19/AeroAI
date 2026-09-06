package com.aeroai.backend.opensky;

import com.aeroai.backend.config.AppProperties;
import com.aeroai.backend.flight.AircraftStateStore;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Section 18.4 - bounded active-airspace scheduler. The default city
 * (AIRSPACE_DEFAULT_CITY_ID) is always polled; if AIRSPACE_MAX_ACTIVE_CITY_COUNT
 * allows more than one slot, the remaining slots are filled with the most
 * recently requested other cities (Section 26/46 endpoints record a request
 * via {@link AircraftStateStore#markAirspaceActive}). This bounds OpenSky
 * request volume regardless of catalogue size (Rule 10).
 */
@Component
public class ActiveAirspaceManager {

    private final AircraftStateStore stateStore;
    private final AppProperties appProperties;

    public ActiveAirspaceManager(AircraftStateStore stateStore, AppProperties appProperties) {
        this.stateStore = stateStore;
        this.appProperties = appProperties;
    }

    public List<String> getActiveAirspaceSlugs() {
        int maxCount = Math.max(appProperties.getAirspace().getMaxActiveCityCount(), 1);
        String defaultCity = appProperties.getAirspace().getDefaultCityId();

        Set<String> active = new LinkedHashSet<>();
        active.add(defaultCity);

        if (maxCount > 1) {
            for (String candidate : stateStore.getMostRecentlyRequestedAirspaces(maxCount)) {
                if (active.size() >= maxCount) {
                    break;
                }
                active.add(candidate);
            }
        }
        return List.copyOf(active);
    }
}
