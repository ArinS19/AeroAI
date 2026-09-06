import { Link } from 'react-router-dom';
import type { FlightStatus } from '../../types';
import { useAuthStore } from '../../store/authStore';

export interface MapFilterState {
  statuses: FlightStatus[];
  airline: string;
  trackedOnly: boolean;
}

export const EMPTY_FILTERS: MapFilterState = { statuses: [], airline: '', trackedOnly: false };

const STATUS_OPTIONS: FlightStatus[] = ['SCHEDULED', 'IN_FLIGHT', 'LANDED', 'UNKNOWN'];

interface MapFiltersProps {
  open: boolean;
  onClose: () => void;
  filters: MapFilterState;
  onChange: (filters: MapFilterState) => void;
}

export function MapFilters({ open, onClose, filters, onChange }: MapFiltersProps) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  if (!open) return null;

  function toggleStatus(status: FlightStatus) {
    const has = filters.statuses.includes(status);
    onChange({
      ...filters,
      statuses: has ? filters.statuses.filter((s) => s !== status) : [...filters.statuses, status],
    });
  }

  const hasActiveFilters =
    filters.statuses.length > 0 || filters.airline.trim() !== '' || filters.trackedOnly;

  return (
    <div className="absolute top-14 left-3 z-[500] w-72 panel p-4 shadow-2xl animate-fade-in">
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-xs font-semibold tracking-widest text-slate-200">TRAFFIC FILTERS</h3>
        <button onClick={onClose} className="text-state-gray hover:text-slate-200 text-xs">✕</button>
      </div>

      <div className="mb-4">
        <p className="text-xs text-state-gray mb-2 font-medium">Status</p>
        <div className="flex flex-wrap gap-1.5">
          {STATUS_OPTIONS.map((s) => (
            <button
              key={s}
              onClick={() => toggleStatus(s)}
              className={`text-xs px-2 py-1 rounded border transition-colors ${
                filters.statuses.includes(s)
                  ? 'bg-aero-cyan/20 border-aero-cyan/40 text-aero-cyan'
                  : 'bg-aero-muted border-aero-border text-state-gray'
              }`}
            >
              {s.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      <div className="mb-4">
        <p className="text-xs text-state-gray mb-2 font-medium">Airline / callsign</p>
        <input
          value={filters.airline}
          onChange={(e) => onChange({ ...filters, airline: e.target.value })}
          placeholder="e.g. Air India"
          className="aero-input text-sm"
        />
      </div>

      <div className="mb-4">
        <label className="flex items-center gap-2 cursor-pointer">
          <input
            type="checkbox"
            checked={filters.trackedOnly}
            disabled={!isAuthenticated}
            onChange={(e) => onChange({ ...filters, trackedOnly: e.target.checked })}
            className="accent-aero-cyan"
          />
          <span className="text-sm text-slate-300">Tracked flights only</span>
        </label>
        {!isAuthenticated && filters.trackedOnly === false && (
          <p className="text-xs text-state-gray mt-1.5">
            <Link to="/login" className="text-aero-cyan hover:underline">Sign in</Link> to view tracked flights.
          </p>
        )}
      </div>

      <button
        onClick={() => onChange(EMPTY_FILTERS)}
        disabled={!hasActiveFilters}
        className="text-xs text-state-gray hover:text-slate-200 disabled:opacity-40"
      >
        Clear all
      </button>
    </div>
  );
}
