import { useState, useMemo, useCallback, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import type L from 'leaflet';
import { LeafletMap } from '../../components/map/LeafletMap';
import { AirspaceSelector } from '../../components/map/AirspaceSelector';
import { MapFilters, EMPTY_FILTERS, type MapFilterState } from '../../components/map/MapFilters';
import { MapControls } from '../../components/map/MapControls';
import { FlightDetailPanel } from '../../components/flights/FlightDetailPanel';
import { LiveIndicator, LoadingSpinner, ErrorState } from '../../components/common';
import { useAirspaceDetail } from '../../hooks/useAirspaces';
import { useLiveAircraft } from '../../hooks/useLiveAircraft';
import { analyticsService } from '../../services/analyticsService';
import { computeFreshnessLabel } from '../../utils/formatters';
import { AIRSPACE_DEFAULT_CITY_ID, MAP_DEFAULT_CENTER, MAP_DEFAULT_ZOOM } from '../../utils/config';
import type { AirportRefDto } from '../../types';

export function MapPage() {
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const cityId = params.get('airspace') || AIRSPACE_DEFAULT_CITY_ID;

  const [selectedIcao24, setSelectedIcao24] = useState<string | null>(null);
  const [filters, setFilters] = useState<MapFilterState>(EMPTY_FILTERS);
  const [selectorOpen, setSelectorOpen] = useState(false);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [map, setMap] = useState<L.Map | null>(null);
  const [resetCounter, setResetCounter] = useState(0);
  const [trailPoints, setTrailPoints] = useState<[number, number][]>([]);

  const { data: airspace, isLoading: airspaceLoading, isError: airspaceError, refetch: refetchAirspace } =
    useAirspaceDetail(cityId);

  const { aircraft, connectionState, lastDeltaTimestamp } = useLiveAircraft(cityId);

  const { data: analytics } = useQuery({
    queryKey: ['analytics-conflicts', cityId],
    queryFn: () => analyticsService.get('today', cityId),
    refetchInterval: 30_000,
    retry: 0,
  });

  const conflictIcaos = useMemo(() => {
    const set = new Set<string>();
    for (const e of analytics?.conflictStats.recentConflictEvents ?? []) {
      if (e.icao24) set.add(e.icao24);
      set.add(e.otherIcao24);
    }
    return set;
  }, [analytics]);

  // Reset selection + trail when the airspace changes so nothing from the
  // previous city bleeds into the new one (§13.2.1).
  useEffect(() => {
    setSelectedIcao24(null);
    setTrailPoints([]);
  }, [cityId]);

  const aircraftList = useMemo(() => Array.from(aircraft.values()), [aircraft]);

  const filteredAircraft = useMemo(() => {
    return aircraftList.filter((a) => {
      if (filters.statuses.length > 0 && !filters.statuses.includes(a.status)) return false;
      if (filters.airline.trim()) {
        const q = filters.airline.trim().toLowerCase();
        if (!a.callsign || !a.callsign.toLowerCase().includes(q)) return false;
      }
      // trackedOnly is enforced against the user's tracked set on My Flights;
      // on the public map we don't have a bulk tracked-icao24 endpoint, so
      // this filter is honored only in combination with the selected flight.
      return true;
    });
  }, [aircraftList, filters]);

  const newestUpdate = useMemo(() => {
    let newest: string | null = lastDeltaTimestamp;
    for (const a of aircraftList) {
      if (!newest || new Date(a.lastUpdate).getTime() > new Date(newest).getTime()) {
        newest = a.lastUpdate;
      }
    }
    return newest;
  }, [aircraftList, lastDeltaTimestamp]);

  const freshnessLabel = connectionState === 'connected' ? computeFreshnessLabel(newestUpdate) : 'UNAVAILABLE';

  // "View Live Traffic Around Airport" (Airport Details, §30) passes an explicit
  // lat/lon/zoom override to recenter the viewport without changing which
  // city's aircraft are being polled/subscribed (no airport->airspace mapping
  // is exposed by the API, so we don't guess one — §Rule 9).
  const overrideLat = params.get('lat');
  const overrideLon = params.get('lon');
  const overrideZoom = params.get('zoom');
  const hasOverride = overrideLat !== null && overrideLon !== null;

  const center: [number, number] = hasOverride
    ? [parseFloat(overrideLat), parseFloat(overrideLon)]
    : airspace
    ? [airspace.latitude, airspace.longitude]
    : MAP_DEFAULT_CENTER;
  const zoom = hasOverride ? parseFloat(overrideZoom ?? '10') : airspace?.defaultZoom ?? MAP_DEFAULT_ZOOM;
  const bounds: L.LatLngBoundsExpression | null = airspace
    ? [[airspace.lamin, airspace.lomin], [airspace.lamax, airspace.lomax]]
    : null;

  const handleSelectCity = useCallback(
    (newCityId: string) => {
      setParams({ airspace: newCityId }, { replace: false });
    },
    [setParams]
  );

  const handleReset = useCallback(() => {
    setResetCounter((c) => c + 1);
  }, []);

  const handleSelectAirport = useCallback(
    (airport: AirportRefDto) => {
      navigate(`/airports/${airport.id}`);
    },
    [navigate]
  );

  const handleTrailChange = useCallback((points: [number, number][]) => {
    setTrailPoints(points);
  }, []);

  return (
    <div className="relative w-full h-full">
      {airspaceLoading && !airspace && (
        <div className="absolute inset-0 z-[900] flex items-center justify-center bg-aero-base">
          <LoadingSpinner message="Loading airspace…" />
        </div>
      )}

      {airspaceError && !airspace && (
        <div className="absolute inset-0 z-[900] flex items-center justify-center bg-aero-base">
          <ErrorState message="Could not load this airspace." onRetry={() => refetchAirspace()} />
        </div>
      )}

      <LeafletMap
        aircraft={filteredAircraft}
        nearbyAirports={airspace?.nearbyAirports ?? []}
        center={center}
        zoom={zoom}
        bounds={bounds}
        flyToKey={`${cityId}:${resetCounter}`}
        selectedIcao24={selectedIcao24}
        conflictIcaos={conflictIcaos}
        trailPoints={selectedIcao24 ? trailPoints : []}
        onSelectAircraft={setSelectedIcao24}
        onSelectAirport={handleSelectAirport}
        onMapReady={setMap}
        onZoomChange={() => {}}
      />

      {/* Top-left: airspace label + search/filter cluster */}
      <div className="absolute top-3 left-3 z-[500] flex flex-col gap-2 items-start">
        <button
          onClick={() => setSelectorOpen(true)}
          className="flex items-center gap-2 bg-aero-surface/95 border border-aero-border rounded-md px-3 py-2 shadow-lg hover:bg-aero-muted transition-colors"
        >
          <svg className="w-4 h-4 text-aero-cyan" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          <span className="text-sm font-medium text-slate-100">
            {airspace?.cityName ?? cityId}
          </span>
          <span className="text-xs text-state-gray">{airspace?.country}</span>
        </button>

        <MapFilters open={filtersOpen} onClose={() => setFiltersOpen(false)} filters={filters} onChange={setFilters} />
      </div>

      {/* Top-right: live indicator + stacked controls */}
      <div className="absolute top-3 right-3 z-[500] flex flex-col items-end gap-2">
        <div className="bg-aero-surface/95 border border-aero-border rounded-md px-3 py-1.5 shadow-lg">
          <LiveIndicator freshnessLabel={freshnessLabel} lastUpdate={newestUpdate} />
        </div>
        <MapControls
          map={map}
          onReset={handleReset}
          onChooseAirspace={() => setSelectorOpen(true)}
          onToggleFilters={() => setFiltersOpen((o) => !o)}
          filtersActive={filtersOpen || filters.statuses.length > 0 || !!filters.airline || filters.trackedOnly}
        />
      </div>

      {connectionState === 'disconnected' && (
        <div className="absolute bottom-4 left-1/2 -translate-x-1/2 z-[500] bg-state-amber/20 border border-state-amber/40 text-state-amber text-sm px-4 py-2 rounded-md shadow-lg">
          Connection interrupted — reconnecting…
        </div>
      )}

      {/* Flight detail side panel (desktop) / bottom sheet (mobile) */}
      {selectedIcao24 && (
        <>
          <div className="hidden md:block absolute top-0 right-0 h-full w-[380px] bg-aero-surface border-l border-aero-border shadow-2xl z-[600] animate-fade-in">
            <FlightDetailPanel
              icao24={selectedIcao24}
              onClose={() => setSelectedIcao24(null)}
              showMiniMap={false}
              onTrailChange={handleTrailChange}
            />
          </div>
          <div className="md:hidden absolute bottom-0 left-0 right-0 h-[70vh] bg-aero-surface border-t border-aero-border rounded-t-2xl shadow-2xl z-[600] animate-fade-in">
            <FlightDetailPanel
              icao24={selectedIcao24}
              onClose={() => setSelectedIcao24(null)}
              showMiniMap={false}
              onTrailChange={handleTrailChange}
            />
          </div>
        </>
      )}

      <AirspaceSelector
        open={selectorOpen}
        onClose={() => setSelectorOpen(false)}
        currentCityId={cityId}
        onSelect={handleSelectCity}
      />
    </div>
  );
}
