import { useState, useMemo, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { flightsService } from '../../services/flightsService';
import { FlightTable } from '../../components/flights/FlightTable';
import { AirspaceSelector } from '../../components/map/AirspaceSelector';
import { LoadingSpinner, ErrorState, EmptyState } from '../../components/common';
import { AIRSPACE_DEFAULT_CITY_ID } from '../../utils/config';
import { useAirspaceDetail } from '../../hooks/useAirspaces';
import type { FlightStatus } from '../../types';
import { extractErrorMessage } from '../../api/client';

const STATUS_OPTIONS: FlightStatus[] = ['SCHEDULED', 'IN_FLIGHT', 'LANDED', 'UNKNOWN'];
const PAGE_SIZE = 50;

export function FlightsPage() {
  const [params, setParams] = useSearchParams();
  const cityId = params.get('airspace') || AIRSPACE_DEFAULT_CITY_ID;

  const [query, setQuery] = useState('');
  const [status, setStatus] = useState<FlightStatus | ''>('');
  const [airline, setAirline] = useState('');
  const [page, setPage] = useState(0);
  const [selectorOpen, setSelectorOpen] = useState(false);

  const { data: airspace } = useAirspaceDetail(cityId);

  const debounced = useDebounced(query, 300);

  const {
    data: flights,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['flights-list', cityId, status, airline, debounced],
    queryFn: () =>
      debounced.trim()
        ? flightsService.search(debounced.trim())
        : flightsService.getList({ airspaceId: cityId, status: status || undefined, airline: airline || undefined }),
    refetchInterval: 15_000,
  });

  useEffect(() => {
    setPage(0);
  }, [cityId, status, airline, debounced]);

  const paged = useMemo(() => {
    const list = flights ?? [];
    const start = page * PAGE_SIZE;
    return list.slice(start, start + PAGE_SIZE);
  }, [flights, page]);

  const totalPages = flights ? Math.max(1, Math.ceil(flights.length / PAGE_SIZE)) : 1;

  function clearAll() {
    setQuery('');
    setStatus('');
    setAirline('');
  }

  const hasActiveFilters = !!query || !!status || !!airline;

  return (
    <div className="max-w-6xl mx-auto px-4 md:px-8 py-6">
      <div className="flex flex-wrap items-center justify-between gap-3 mb-5">
        <div>
          <h1 className="text-xl font-semibold text-slate-100">Flights</h1>
          <p className="text-sm text-state-gray mt-0.5">
            {airspace ? `${airspace.cityName}, ${airspace.country}` : cityId}
          </p>
        </div>
        <button onClick={() => setSelectorOpen(true)} className="btn-secondary text-sm">
          Change airspace
        </button>
      </div>

      <div className="panel p-4 mb-5 flex flex-wrap gap-3 items-end">
        <div className="flex-1 min-w-[200px]">
          <label className="block text-xs text-state-gray mb-1.5">Search callsign / ICAO24</label>
          <input value={query} onChange={(e) => setQuery(e.target.value)} className="aero-input" placeholder="e.g. AIC9HA" />
        </div>
        <div className="min-w-[160px]">
          <label className="block text-xs text-state-gray mb-1.5">Status</label>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value as FlightStatus | '')}
            className="aero-input"
          >
            <option value="">All statuses</option>
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>{s.replace('_', ' ')}</option>
            ))}
          </select>
        </div>
        <div className="min-w-[160px]">
          <label className="block text-xs text-state-gray mb-1.5">Airline</label>
          <input value={airline} onChange={(e) => setAirline(e.target.value)} className="aero-input" placeholder="e.g. Air India" />
        </div>
        <button onClick={clearAll} disabled={!hasActiveFilters} className="text-xs text-state-gray hover:text-slate-200 disabled:opacity-40 pb-2.5">
          Clear all
        </button>
      </div>

      <div className="panel p-4">
        {isLoading && <LoadingSpinner message="Loading flights…" className="p-8 justify-center" />}
        {isError && <ErrorState message={extractErrorMessage(error) || 'Could not load flights.'} onRetry={() => refetch()} />}
        {!isLoading && !isError && (!flights || flights.length === 0) && (
          <EmptyState message="No aircraft found for the current filters." />
        )}
        {!isLoading && !isError && flights && flights.length > 0 && (
          <>
            <FlightTable flights={paged} />
            {totalPages > 1 && (
              <div className="flex items-center justify-between mt-4 pt-4 border-t border-aero-border">
                <span className="text-xs text-state-gray">
                  Page {page + 1} of {totalPages} · {flights.length} results
                </span>
                <div className="flex gap-2">
                  <button
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                    className="btn-secondary text-xs px-3 py-1.5 disabled:opacity-40"
                  >
                    Previous
                  </button>
                  <button
                    onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                    disabled={page >= totalPages - 1}
                    className="btn-secondary text-xs px-3 py-1.5 disabled:opacity-40"
                  >
                    Next
                  </button>
                </div>
              </div>
            )}
          </>
        )}
      </div>

      <AirspaceSelector
        open={selectorOpen}
        onClose={() => setSelectorOpen(false)}
        currentCityId={cityId}
        onSelect={(id) => setParams({ airspace: id })}
      />
    </div>
  );
}

function useDebounced(value: string, delay: number): string {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}
