import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { flightsService } from '../../services/flightsService';
import { airportsService } from '../../services/airportsService';
import { airspacesService } from '../../services/airspacesService';
import type { FlightListItemDto, AirportDto, AirspaceDto } from '../../types';

function useDebounce<T>(value: T, delay: number): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const t = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(t);
  }, [value, delay]);
  return debounced;
}

export function GlobalSearch({ onClose }: { onClose?: () => void }) {
  const [query, setQuery] = useState('');
  const [flights, setFlights] = useState<FlightListItemDto[]>([]);
  const [airports, setAirports] = useState<AirportDto[]>([]);
  const [airspaces, setAirspaces] = useState<AirspaceDto[]>([]);
  const [loading, setLoading] = useState(false);
  const debouncedQuery = useDebounce(query, 300);
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  useEffect(() => {
    if (!debouncedQuery.trim() || debouncedQuery.length < 2) {
      setFlights([]);
      setAirports([]);
      setAirspaces([]);
      return;
    }
    setLoading(true);
    const q = debouncedQuery.trim();
    Promise.all([
      flightsService.search(q).catch(() => [] as FlightListItemDto[]),
      airportsService.search(q).catch(() => [] as AirportDto[]),
      airspacesService.getList(q).catch(() => [] as AirspaceDto[]),
    ]).then(([f, a, s]) => {
      setFlights(f.slice(0, 5));
      setAirports(a.slice(0, 5));
      setAirspaces(s.slice(0, 5));
      setLoading(false);
    });
  }, [debouncedQuery]);

  const hasResults = flights.length > 0 || airports.length > 0 || airspaces.length > 0;
  const showEmpty = debouncedQuery.length >= 2 && !loading && !hasResults;

  return (
    <div className="relative">
      <div className="relative">
        <svg className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-state-gray" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
        <input
          ref={inputRef}
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search flights, airports, cities…"
          className="aero-input pl-9 pr-4"
        />
        {loading && (
          <svg className="absolute right-3 top-1/2 -translate-y-1/2 w-4 h-4 animate-spin text-aero-cyan" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
        )}
      </div>

      {(hasResults || showEmpty) && (
        <div className="absolute top-full left-0 right-0 mt-1 bg-aero-surface border border-aero-border rounded-lg shadow-2xl z-50 max-h-96 overflow-y-auto animate-fade-in">
          {showEmpty && (
            <div className="px-4 py-6 text-center text-sm text-state-gray">
              No matching flights or airports found.
            </div>
          )}

          {airspaces.length > 0 && (
            <div>
              <div className="px-4 py-2 text-xs text-state-gray tracking-widest font-semibold border-b border-aero-border">AIRSPACES</div>
              {airspaces.map((a) => (
                <button
                  key={a.cityId}
                  onClick={() => { navigate(`/map?airspace=${a.cityId}`); onClose?.(); }}
                  className="w-full flex items-center gap-3 px-4 py-2.5 hover:bg-aero-muted transition-colors text-left"
                >
                  <svg className="w-4 h-4 text-aero-cyan shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  <div>
                    <div className="text-sm font-medium text-slate-200">{a.cityName}</div>
                    <div className="text-xs text-state-gray">{a.country}</div>
                  </div>
                </button>
              ))}
            </div>
          )}

          {flights.length > 0 && (
            <div>
              <div className="px-4 py-2 text-xs text-state-gray tracking-widest font-semibold border-b border-aero-border">FLIGHTS</div>
              {flights.map((f) => (
                <button
                  key={f.icao24}
                  onClick={() => { navigate(`/flights/${f.icao24}`); onClose?.(); }}
                  className="w-full flex items-center justify-between px-4 py-2.5 hover:bg-aero-muted transition-colors"
                >
                  <div className="flex items-center gap-3">
                    <svg className="w-4 h-4 text-aero-blue shrink-0" fill="currentColor" viewBox="0 0 24 24">
                      <path d="M21 16v-2l-8-5V3.5c0-.83-.67-1.5-1.5-1.5S10 2.67 10 3.5V9l-8 5v2l8-2.5V19l-2 1.5V22l3.5-1 3.5 1v-1.5L13 19v-5.5l8 2.5z"/>
                    </svg>
                    <div>
                      <div className="text-sm font-medium text-slate-200">{f.callsign ?? f.icao24}</div>
                      <div className="text-xs text-state-gray">{f.airline ?? f.icao24}</div>
                    </div>
                  </div>
                  <span className={`text-xs px-1.5 py-0.5 rounded ${
                    f.status === 'IN_FLIGHT' ? 'text-aero-blue bg-aero-blue/10' :
                    f.status === 'LANDED' ? 'text-state-green bg-state-green/10' : 'text-state-gray bg-state-gray/10'
                  }`}>{f.status}</span>
                </button>
              ))}
            </div>
          )}

          {airports.length > 0 && (
            <div>
              <div className="px-4 py-2 text-xs text-state-gray tracking-widest font-semibold border-b border-aero-border">AIRPORTS</div>
              {airports.map((a) => (
                <button
                  key={a.id}
                  onClick={() => { navigate(`/airports/${a.id}`); onClose?.(); }}
                  className="w-full flex items-center gap-3 px-4 py-2.5 hover:bg-aero-muted transition-colors text-left"
                >
                  <svg className="w-4 h-4 text-state-green shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-2 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" />
                  </svg>
                  <div>
                    <div className="text-sm font-medium text-slate-200">
                      {a.name} {a.iata ? `(${a.iata})` : ''}
                    </div>
                    <div className="text-xs text-state-gray">{a.city}, {a.country}</div>
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
