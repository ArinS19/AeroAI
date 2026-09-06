import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { authService } from '../../services/authService';
import { trackingService } from '../../services/trackingService';
import { useAuthStore } from '../../store/authStore';
import { extractErrorMessage } from '../../api/client';

interface TrackButtonProps {
  icao24: string;
  callsign: string | null;
  tracked: boolean;
  onTrackedChange?: (tracked: boolean) => void;
}

export function TrackButton({ icao24, callsign, tracked, onTrackedChange }: TrackButtonProps) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const qc = useQueryClient();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showPrompt, setShowPrompt] = useState(false);

  // Lazily resolve the numeric TrackedFlightDto.id for untrack — the flight
  // detail DTO only exposes a boolean `tracked` flag, not the tracking id.
  const { data: myFlights } = useQuery({
    queryKey: ['my-flights'],
    queryFn: () => authService.getMyFlights(),
    enabled: isAuthenticated && tracked,
    staleTime: 30_000,
  });

  async function handleTrack() {
    if (!isAuthenticated) {
      setShowPrompt(true);
      return;
    }
    setError(null);
    setBusy(true);
    try {
      await trackingService.track(icao24, callsign ?? undefined);
      onTrackedChange?.(true);
      qc.invalidateQueries({ queryKey: ['my-flights'] });
      qc.invalidateQueries({ queryKey: ['flight', icao24] });
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function handleUntrack() {
    const record = myFlights?.find((f) => f.icao24 === icao24);
    if (!record) return;
    setError(null);
    setBusy(true);
    try {
      await trackingService.untrack(record.id);
      onTrackedChange?.(false);
      qc.invalidateQueries({ queryKey: ['my-flights'] });
      qc.invalidateQueries({ queryKey: ['flight', icao24] });
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      {tracked ? (
        <button onClick={handleUntrack} disabled={busy} className="btn-secondary w-full">
          <span className="text-state-green">✓</span> Tracking — click to untrack
        </button>
      ) : (
        <button onClick={handleTrack} disabled={busy} className="btn-primary w-full">
          {busy ? 'Tracking…' : '＋ Track Flight'}
        </button>
      )}

      {error && <p className="text-xs text-state-red mt-2">{error}</p>}

      {showPrompt && !isAuthenticated && (
        <div className="mt-2 p-3 rounded-md bg-aero-muted border border-aero-border text-sm">
          <p className="text-slate-300 mb-2">Create an account to track flights.</p>
          <div className="flex gap-2">
            <Link to="/login" className="btn-secondary text-xs px-3 py-1.5">Log in</Link>
            <Link to="/register" className="btn-primary text-xs px-3 py-1.5">Sign up</Link>
          </div>
        </div>
      )}
    </div>
  );
}
