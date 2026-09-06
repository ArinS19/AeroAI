import { useState, useEffect, useRef } from 'react';
import { useAirspaceList } from '../../hooks/useAirspaces';
import { LoadingSpinner, ErrorState, EmptyState } from '../common';
import type { AirspaceDto } from '../../types';

interface AirspaceSelectorProps {
  open: boolean;
  onClose: () => void;
  currentCityId: string | null;
  onSelect: (cityId: string) => void;
}

const POPULAR_CITY_IDS = [
  'mumbai', 'new-delhi', 'bengaluru', 'chennai', 'kolkata', 'hyderabad',
  'dubai', 'singapore', 'london', 'new-york', 'tokyo', 'abu-dhabi',
];

export function AirspaceSelector({ open, onClose, currentCityId, onSelect }: AirspaceSelectorProps) {
  const [query, setQuery] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);
  const { data: airspaces, isLoading, isError, refetch } = useAirspaceList(query.trim() || undefined);

  useEffect(() => {
    if (open) {
      setQuery('');
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [open]);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose();
    }
    if (open) document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open, onClose]);

  if (!open) return null;

  const showPopular = !query.trim();
  const popular = showPopular
    ? (airspaces ?? []).filter((a) => POPULAR_CITY_IDS.includes(a.cityId))
    : [];
  const list = showPopular ? popular : airspaces ?? [];

  function handleSelect(a: AirspaceDto) {
    onSelect(a.cityId);
    onClose();
  }

  return (
    <div className="fixed inset-0 z-[1000] bg-aero-base/80 backdrop-blur-sm flex items-start justify-center pt-16 sm:pt-24 px-4">
      <div className="w-full max-w-lg bg-aero-surface border border-aero-border rounded-lg shadow-2xl overflow-hidden animate-fade-in">
        <div className="p-4 border-b border-aero-border">
          <div className="flex items-center justify-between mb-3">
            <h2 className="text-sm font-semibold text-slate-100 tracking-wide">CHOOSE AIRSPACE</h2>
            <button onClick={onClose} className="text-state-gray hover:text-slate-200 text-sm">
              Close
            </button>
          </div>
          <input
            ref={inputRef}
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search city, country, or alias…"
            className="aero-input"
          />
        </div>

        <div className="max-h-96 overflow-y-auto">
          {isLoading && <LoadingSpinner message="Loading airspaces…" className="p-6 justify-center" />}
          {isError && <ErrorState message="Could not load the airspace catalogue." onRetry={() => refetch()} />}

          {!isLoading && !isError && (
            <>
              {showPopular && (
                <div className="px-4 pt-3 text-xs text-state-gray tracking-widest font-semibold">
                  POPULAR
                </div>
              )}
              {list.length === 0 && (
                <EmptyState message="No city matches your search." />
              )}
              {list.map((a) => (
                <button
                  key={a.cityId}
                  onClick={() => handleSelect(a)}
                  className={`w-full flex items-center justify-between gap-3 px-4 py-3 hover:bg-aero-muted transition-colors text-left border-b border-aero-border/50 ${
                    a.cityId === currentCityId ? 'bg-aero-cyan/5' : ''
                  }`}
                >
                  <div>
                    <div className="text-sm font-medium text-slate-200">{a.cityName}</div>
                    <div className="text-xs text-state-gray">{a.displayRegion ?? a.country}</div>
                  </div>
                  {a.cityId === currentCityId && (
                    <span className="text-xs text-aero-cyan font-semibold">SELECTED</span>
                  )}
                </button>
              ))}
            </>
          )}
        </div>
      </div>
    </div>
  );
}
