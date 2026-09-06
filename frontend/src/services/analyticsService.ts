import type { AnalyticsDto } from '../types';
import { apiClient } from '../api/client';

export const analyticsService = {
  async get(range: 'today' | '7d' = 'today', airspaceId?: string): Promise<AnalyticsDto> {
    const res = await apiClient.get<AnalyticsDto>('/analytics', {
      params: { range, ...(airspaceId ? { airspaceId } : {}) },
    });
    return res.data;
  },
};
