import { useMemo, useRef, useState } from 'react';
import { MapContainer, TileLayer, Marker, Polyline, useMap, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import type { AircraftDto, AirportRefDto } from '../../types';
import { buildAircraftIcon, buildAirportIcon } from './aircraftIcon';
import { MAP_TILE_URL, ZOOM_LABELS_SELECTED_ONLY, ZOOM_LABELS_ALL } from '../../utils/config';

interface LeafletMapProps {
  aircraft: AircraftDto[];
  nearbyAirports: AirportRefDto[];
  center: [number, number];
  zoom: number;
  selectedIcao24: string | null;
  conflictIcaos: Set<string>;
  trailPoints: [number, number][];
  onSelectAircraft: (icao24: string) => void;
  onSelectAirport: (airport: AirportRefDto) => void;
  onMapReady: (map: L.Map) => void;
  onZoomChange: (zoom: number) => void;
}

/**
 * MapController — purely provides the map instance to the parent via onMapReady
 * and tracks zoom changes. Centering/fly-to is handled imperatively in MapPage
 * once both the map instance and the target airspace data are available.
 */
function MapController({
  onMapReady,
  onZoomChange,
}: {
  onMapReady: (map: L.Map) => void;
  onZoomChange: (zoom: number) => void;
}) {
  const map = useMap();
  const reported = useRef(false);

  // Report the map instance to the parent exactly once per mount.
  if (!reported.current) {
    reported.current = true;
    // Schedule via microtask so React's render cycle has committed before
    // the parent attempts to call map methods.
    Promise.resolve().then(() => onMapReady(map));
  }

  useMapEvents({
    zoomend: () => onZoomChange(map.getZoom()),
  });

  return null;
}

export function LeafletMap({
  aircraft,
  nearbyAirports,
  center,
  zoom,
  selectedIcao24,
  conflictIcaos,
  trailPoints,
  onSelectAircraft,
  onSelectAirport,
  onMapReady,
  onZoomChange,
}: LeafletMapProps) {
  const [currentZoom, setCurrentZoom] = useState(zoom);
  const airportIcon = useMemo(() => buildAirportIcon(), []);

  function handleZoomChange(z: number) {
    setCurrentZoom(z);
    onZoomChange(z);
  }

  return (
    <MapContainer
      center={center}
      zoom={zoom}
      zoomControl={false}
      className="w-full h-full"
      style={{ height: '100%', width: '100%' }}
      worldCopyJump
    >
      <TileLayer
        url={MAP_TILE_URL}
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        maxZoom={19}
      />

      <MapController onMapReady={onMapReady} onZoomChange={handleZoomChange} />

      {nearbyAirports.map((a) => (
        <Marker
          key={`airport-${a.id}`}
          position={[a.latitude, a.longitude]}
          icon={airportIcon}
          eventHandlers={{ click: () => onSelectAirport(a) }}
        />
      ))}

      {trailPoints.length > 1 && (
        <Polyline
          positions={trailPoints}
          pathOptions={{ color: '#22D3EE', weight: 2, opacity: 0.6, dashArray: '4 4' }}
        />
      )}

      {aircraft.map((a) => {
        if (a.latitude === null || a.longitude === null) return null;

        const isSelected = a.icao24 === selectedIcao24;
        const inConflict = conflictIcaos.has(a.icao24);
        const showLabel =
          isSelected ||
          (currentZoom >= ZOOM_LABELS_SELECTED_ONLY && currentZoom < ZOOM_LABELS_ALL && inConflict) ||
          currentZoom >= ZOOM_LABELS_ALL;

        const color = isSelected ? 'selected' : inConflict ? 'high' : 'default';
        const icon = buildAircraftIcon({
          heading: a.heading,
          color,
          label: a.callsign ?? a.icao24,
          showLabel: currentZoom > 6 ? showLabel : false,
          onGround: a.onGround,
        });

        return (
          <Marker
            key={a.icao24}
            position={[a.latitude, a.longitude]}
            icon={icon}
            eventHandlers={{ click: () => onSelectAircraft(a.icao24) }}
            zIndexOffset={isSelected ? 1000 : 0}
          />
        );
      })}
    </MapContainer>
  );
}
