import type { TrackedFlightDto } from '../types';
import { apiClient } from '../api/client';

export const trackingService = {
  /**
   * POST /api/tracked-flights
   * Idempotent — returns existing record if already tracking.
   */
  async track(icao24: string, callsign?: string): Promise<TrackedFlightDto> {
    const res = await apiClient.post<TrackedFlightDto>('/tracked-flights', {
      icao24,
      callsign,
    });
    return res.data;
  },

  /**
   * DELETE /api/tracked-flights/{id}
   * id is the numeric TrackedFlightDto.id, NOT the icao24.
   */
  async untrack(id: number): Promise<void> {
    await apiClient.delete(`/tracked-flights/${id}`);
  },
};
