import type { AlertDto, UnreadCountDto } from '../types';
import { apiClient } from '../api/client';

export const alertsService = {
  async getAll(): Promise<AlertDto[]> {
    const res = await apiClient.get<AlertDto[]>('/alerts');
    return res.data;
  },

  async getUnreadCount(): Promise<UnreadCountDto> {
    const res = await apiClient.get<UnreadCountDto>('/alerts/unread-count');
    return res.data;
  },

  async markRead(id: number): Promise<AlertDto> {
    const res = await apiClient.patch<AlertDto>(`/alerts/${id}/read`);
    return res.data;
  },

  async markAllRead(): Promise<void> {
    await apiClient.patch('/alerts/read-all');
  },
};
