import type { AirportDto, SpringPage } from '../types';
import { apiClient } from '../api/client';

export const airportsService = {
  async getPage(page = 0, size = 50): Promise<SpringPage<AirportDto>> {
    const res = await apiClient.get<SpringPage<AirportDto>>('/airports', {
      params: { page, size: Math.min(size, 100) },
    });
    return res.data;
  },

  async search(q: string): Promise<AirportDto[]> {
    const res = await apiClient.get<AirportDto[]>('/airports/search', {
      params: { q },
    });
    return res.data;
  },

  async getDetail(id: number): Promise<AirportDto> {
    const res = await apiClient.get<AirportDto>(`/airports/${id}`);
    return res.data;
  },
};
