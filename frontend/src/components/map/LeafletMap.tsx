import { useEffect, useMemo, useRef, useState } from 'react';
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
  bounds: L.LatLngBoundsExpression | null;
  flyToKey: string; // changes when the selected city changes (or reset is clicked) -> triggers flyTo
  selectedIcao24: string | null;
  conflictIcaos: Set<string>;
  trailPoints: [number, number][];
  onSelectAircraft: (icao24: string) => void;
  onSelectAirport: (airport: AirportRefDto) => void;
  onMapReady: (map: L.Map) => void;
  onZoomChange: (zoom: number) => void;
}

function MapController({
  center,
  zoom,
  bounds,
  flyToKey,
  onMapReady,
  onZoomChange,
}: {
  center: [number, number];
  zoom: number;
  bounds: L.LatLngBoundsExpression | null;
  flyToKey: string;
  onMapReady: (map: L.Map) => void;
  onZoomChange: (zoom: number) => void;
}) {
  const map = useMap();
  const initialized = useRef(false);

  useEffect(() => {
    onMapReady(map);
  }, [map, onMapReady]);

  useEffect(() => {
    if (!initialized.current) {
      initialized.current = true;
      return; // MapContainer already applied initial center/zoom
    }
    if (bounds) {
      map.flyToBounds(bounds, { padding: [40, 40], duration: 0.8 });
    } else {
      map.flyTo(center, zoom, { duration: 0.8 });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [flyToKey]);

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
  bounds,
  flyToKey,
  selectedIcao24,
  conflictIcaos,
  trailPoints,
  onSelectAircraft,
  onSelectAirport,
  onMapReady,
  onZoomChange,
}: LeafletMapProps) {
  const [currentZoom, setCurrentZoomState] = useState(zoom);

  function handleZoomChange(z: number) {
    setCurrentZoomState(z);
    onZoomChange(z);
  }

  const airportIcon = useMemo(() => buildAirportIcon(), []);

  return (
    <MapContainer
      center={center}
      zoom={zoom}
      zoomControl={false}
      className="w-full h-full"
      worldCopyJump
    >
      <TileLayer
        url={MAP_TILE_URL}
        attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        maxZoom={19}
      />

      <MapController
        center={center}
        zoom={zoom}
        bounds={bounds}
        flyToKey={flyToKey}
        onMapReady={onMapReady}
        onZoomChange={handleZoomChange}
      />

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

        if (a.latitude === null || a.longitude === null) return null;

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

