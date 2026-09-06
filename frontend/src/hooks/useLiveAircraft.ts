import { useState, useCallback, useRef, useEffect } from 'react';
import type { AircraftDto, AircraftDeltaMessage } from '../types';
import { useWebSocket } from './useWebSocket';

export type ConnectionState = 'connecting' | 'connected' | 'disconnected';

interface UseLiveAircraftResult {
  aircraft: Map<string, AircraftDto>; // keyed by icao24
  connectionState: ConnectionState;
  lastDeltaTimestamp: string | null;
}

/**
 * useLiveAircraft — combines REST snapshot + WebSocket deltas into a
 * stable Map<icao24, AircraftDto>.
 *
 * Keying by icao24 ensures markers update in-place rather than remounting.
 * City switching is handled by remounting (cityId dep change clears state).
 */
export function useLiveAircraft(cityId: string | null): UseLiveAircraftResult {
  const [aircraft, setAircraft] = useState<Map<string, AircraftDto>>(new Map());
  const [connectionState, setConnectionState] = useState<ConnectionState>('connecting');
  const [lastDeltaTimestamp, setLastDeltaTimestamp] = useState<string | null>(null);

  // Use ref to avoid stale closure issues in WebSocket callbacks
  const setAircraftRef = useRef(setAircraft);
  setAircraftRef.current = setAircraft;

  const handleSnapshot = useCallback((snapshot: AircraftDto[]) => {
    // Atomic city switch — replace entire map at once
    const map = new Map<string, AircraftDto>();
    for (const a of snapshot) {
      map.set(a.icao24, a);
    }
    setAircraftRef.current(map);
  }, []);

  const handleDelta = useCallback((delta: AircraftDeltaMessage) => {
    setAircraftRef.current((prev: Map<string, AircraftDto>) => {
      const next = new Map(prev);

      // Added — insert new aircraft
      for (const a of delta.added) {
        next.set(a.icao24, a);
      }

      // Updated — replace full record (not partial diff per contract §V)
      for (const a of delta.updated) {
        next.set(a.icao24, a);
      }

      // Removed — delete by icao24
      for (const icao24 of delta.removed) {
        next.delete(icao24);
      }

      return next;
    });
    setLastDeltaTimestamp(delta.timestamp);
  }, []);

  // Clear the aircraft map immediately when the city changes so markers
  // from the previous city never bleed into the new city's view while
  // the incoming REST snapshot is in-flight (§13.2.1).
  useEffect(() => {
    setAircraftRef.current(new Map());
  }, [cityId]);

  useWebSocket({
    cityId,
    onSnapshot: handleSnapshot,
    onDelta: handleDelta,
    onConnectionChange: setConnectionState,
  });

  return { aircraft, connectionState, lastDeltaTimestamp };
}
