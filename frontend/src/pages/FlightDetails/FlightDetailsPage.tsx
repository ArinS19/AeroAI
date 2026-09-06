import { useParams, Link } from 'react-router-dom';
import { FlightDetailPanel } from '../../components/flights/FlightDetailPanel';

export function FlightDetailsPage() {
  const { icao24 } = useParams<{ icao24: string }>();

  if (!icao24) return null;

  return (
    <div className="max-w-2xl mx-auto px-4 md:px-8 py-6">
      <Link to="/flights" className="text-sm text-aero-cyan hover:underline mb-4 inline-block">
        ← Back to Flights
      </Link>
      <div className="panel overflow-hidden">
        <FlightDetailPanel icao24={icao24} showMiniMap />
      </div>
    </div>
  );
}
