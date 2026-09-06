import { MapContainer, TileLayer, Polyline, CircleMarker } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { MAP_TILE_URL } from '../../utils/config';

interface TrailMiniMapProps {
  points: [number, number][];
}

/** Small, non-interactive-feeling trail preview map for the flight detail panel/page. */
export function TrailMiniMap({ points }: TrailMiniMapProps) {
  if (points.length === 0) return null;
  const last = points[points.length - 1];

  return (
    <div className="h-48 rounded-md overflow-hidden border border-aero-border">
      <MapContainer center={last} zoom={7} zoomControl={false} attributionControl={false} className="w-full h-full">
        <TileLayer url={MAP_TILE_URL} maxZoom={19} />
        {points.length > 1 && (
          <Polyline positions={points} pathOptions={{ color: '#22D3EE', weight: 2, opacity: 0.8 }} />
        )}
        <CircleMarker center={last} radius={5} pathOptions={{ color: '#F8FAFC', fillColor: '#22D3EE', fillOpacity: 1, weight: 2 }} />
      </MapContainer>
    </div>
  );
}
