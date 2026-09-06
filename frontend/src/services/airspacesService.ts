import type { AirspaceDto } from '../types';
import { apiClient } from '../api/client';

export const airspacesService = {
  async getList(q?: string): Promise<AirspaceDto[]> {
    const res = await apiClient.get<AirspaceDto[]>('/airspaces', {
      params: q ? { q } : undefined,
    });
    return res.data;
  },

  async getDetail(cityId: string): Promise<AirspaceDto> {
    const res = await apiClient.get<AirspaceDto>(`/airspaces/${cityId}`);
    return res.data;
  },
};
