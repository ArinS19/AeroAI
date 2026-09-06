import type {
  AircraftDto,
  FlightListItemDto,
  FlightDetailDto,
  TrailResponseDto,
  TrailMinutes,
} from '../types';
import { apiClient } from '../api/client';

export const flightsService = {
  /**
   * GET /api/flights/current?airspaceId={cityId}
   * Primary map data source — raw telemetry per aircraft.
   */
  async getCurrent(airspaceId: string): Promise<AircraftDto[]> {
    const res = await apiClient.get<AircraftDto[]>('/flights/current', {
      params: { airspaceId },
    });
    return res.data;
  },

  /**
   * GET /api/flights?airspaceId={cityId}&status=...&airline=...
   * Flights page table rows.
   */
  async getList(params: {
    airspaceId?: string;
    status?: string;
    airline?: string;
  }): Promise<FlightListItemDto[]> {
    const res = await apiClient.get<FlightListItemDto[]>('/flights', { params });
    return res.data;
  },

  /**
   * GET /api/flights/search?q={query}
   */
  async search(q: string): Promise<FlightListItemDto[]> {
    const res = await apiClient.get<FlightListItemDto[]>('/flights/search', {
      params: { q },
    });
    return res.data;
  },

  /**
   * GET /api/flights/{icao24}
   * Full detail including weather, prediction, conflicts.
   * Sends JWT if available (affects `tracked` field).
   */
  async getDetail(icao24: string): Promise<FlightDetailDto> {
    const res = await apiClient.get<FlightDetailDto>(`/flights/${icao24}`);
    return res.data;
  },

  /**
   * GET /api/flights/{icao24}/trail?minutes=5|15|30|60
   */
  async getTrail(icao24: string, minutes: TrailMinutes): Promise<TrailResponseDto> {
    const res = await apiClient.get<TrailResponseDto>(`/flights/${icao24}/trail`, {
      params: { minutes },
    });
    return res.data;
  },
};
