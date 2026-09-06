import { Link } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { authService } from '../../services/authService';
import { trackingService } from '../../services/trackingService';
import { LoadingSpinner, ErrorState, EmptyState, StatusBadge, UnavailableText, AiBadge } from '../../components/common';
import { formatDelay, timeAgo } from '../../utils/formatters';
import { extractErrorMessage } from '../../api/client';

export function MyFlightsPage() {
  const qc = useQueryClient();

  const { data: flights, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['my-flights'],
    queryFn: () => authService.getMyFlights(),
    refetchInterval: 20_000,
  });

  async function handleUntrack(id: number) {
    try {
      await trackingService.untrack(id);
      qc.invalidateQueries({ queryKey: ['my-flights'] });
    } catch {
      // Non-fatal — surfaced implicitly by the list not updating; a full
      // error toast system is out of scope for this simple action.
    }
  }

  return (
    <div className="max-w-4xl mx-auto px-4 md:px-8 py-6">
      <h1 className="text-xl font-semibold text-slate-100 mb-1">My Flights</h1>
      <p className="text-sm text-state-gray mb-5">Flights you're tracking, with live status and AI delay predictions.</p>

      {isLoading && <LoadingSpinner message="Loading your tracked flights…" className="p-10 justify-center" />}
      {isError && <ErrorState message={extractErrorMessage(error) || 'Could not load your flights.'} onRetry={() => refetch()} />}

      {!isLoading && !isError && (!flights || flights.length === 0) && (
        <div className="panel p-10">
          <EmptyState message="No tracked flights yet." />
          <div className="text-center mt-2">
            <Link to="/map" className="btn-primary text-sm">Explore Live Traffic</Link>
          </div>
        </div>
      )}

      {!isLoading && !isError && flights && flights.length > 0 && (
        <div className="space-y-3">
          {flights.map((f) => (
            <div key={f.id} className="panel p-4 flex flex-wrap items-center justify-between gap-4">
              <div className="min-w-[140px]">
                <p className="text-sm font-semibold text-slate-100 font-mono">{f.callsign ?? f.icao24}</p>
                <p className="text-xs text-state-gray mt-0.5">{f.icao24}</p>
              </div>

              <div className="min-w-[100px]">
                {f.dataAvailable ? <StatusBadge status={f.status} /> : (
                  <span className="text-xs text-state-amber italic">Data temporarily unavailable</span>
                )}
              </div>

              <div className="min-w-[160px]">
                {f.predictedDelayMinutes !== null ? (
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-mono text-slate-200">{formatDelay(f.predictedDelayMinutes)}</span>
                    {f.delayCategory && <span className="text-xs text-state-gray">{f.delayCategory}</span>}
                    <AiBadge disclaimer="Machine-learning estimate, not an airline guarantee." />
                  </div>
                ) : (
                  <UnavailableText />
                )}
              </div>

              <div className="min-w-[100px] text-xs text-state-gray">
                {timeAgo(f.lastUpdate) || 'Unavailable'}
              </div>

              <div className="flex gap-2 ml-auto">
                <Link to={`/flights/${f.icao24}`} className="btn-secondary text-xs px-3 py-1.5">
                  View Flight
                </Link>
                <button onClick={() => handleUntrack(f.id)} className="text-xs text-state-red hover:underline px-1">
                  Untrack
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
