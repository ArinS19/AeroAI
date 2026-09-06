import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { airportsService } from '../../services/airportsService';
import { LoadingSpinner, ErrorState, EmptyState } from '../../components/common';
import { formatCoords } from '../../utils/formatters';
import { extractErrorMessage } from '../../api/client';

type Tab = 'ARRIVALS' | 'DEPARTURES';

export function AirportDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [tab, setTab] = useState<Tab>('ARRIVALS');
  const airportId = Number(id);

  const { data: airport, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['airport', airportId],
    queryFn: () => airportsService.getDetail(airportId),
    enabled: Number.isFinite(airportId),
  });

  if (isLoading) {
    return <div className="max-w-3xl mx-auto px-4 py-10"><LoadingSpinner message="Loading airport…" className="justify-center" /></div>;
  }

  if (isError || !airport) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-10">
        <ErrorState message={extractErrorMessage(error) || 'Airport not found.'} onRetry={() => refetch()} />
      </div>
    );
  }

  function handleViewTraffic() {
    navigate(`/map?lat=${airport!.latitude}&lon=${airport!.longitude}&zoom=9`);
  }

  return (
    <div className="max-w-3xl mx-auto px-4 md:px-8 py-6">
      <Link to="/airports" className="text-sm text-aero-cyan hover:underline mb-4 inline-block">
        ← Back to Airports
      </Link>

      <div className="panel p-5 mb-5">
        <h1 className="text-xl font-semibold text-slate-100 mb-1">{airport.name}</h1>
        <p className="text-sm text-state-gray mb-4">
          {airport.city ?? 'Unavailable'}, {airport.country ?? 'Unavailable'}
        </p>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-5">
          <Field label="IATA" value={airport.iata} />
          <Field label="ICAO" value={airport.icao} />
          <Field label="Location" value={airport.location} />
          <Field label="Coordinates" value={formatCoords(airport.latitude, airport.longitude)} />
        </div>

        <button onClick={handleViewTraffic} className="btn-primary text-sm">
          View Live Traffic Around Airport
        </button>
      </div>

      <div className="panel p-5 mb-5">
        <h2 className="text-xs font-semibold tracking-widest text-state-gray mb-3">CURRENT WEATHER</h2>
        <p className="text-sm text-state-gray italic">Weather: Unavailable</p>
      </div>

      <div className="panel p-5">
        <div className="flex gap-1 mb-4 border-b border-aero-border">
          {(['ARRIVALS', 'DEPARTURES'] as Tab[]).map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`px-4 py-2 text-sm font-medium border-b-2 -mb-px transition-colors ${
                tab === t ? 'border-aero-cyan text-aero-cyan' : 'border-transparent text-state-gray hover:text-slate-200'
              }`}
            >
              {t}
            </button>
          ))}
        </div>
        <EmptyState
          message={
            tab === 'ARRIVALS'
              ? 'Live arrivals data is currently unavailable — no reliable route/schedule source is associated with this airport yet.'
              : 'Live departures data is currently unavailable — no reliable route/schedule source is associated with this airport yet.'
          }
        />
      </div>
    </div>
  );
}

function Field({ label, value }: { label: string; value: string | null }) {
  return (
    <div>
      <p className="text-xs text-state-gray mb-1">{label}</p>
      <p className="text-sm text-slate-200 font-mono">{value ?? 'Unavailable'}</p>
    </div>
  );
}
