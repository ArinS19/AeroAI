import type {
  AuthResponse,
  RegisterRequest,
  LoginRequest,
  UserDto,
  TrackedFlightDto,
} from '../types';
import { apiClient } from '../api/client';

export const authService = {
  async register(data: RegisterRequest): Promise<AuthResponse> {
    const res = await apiClient.post<AuthResponse>('/auth/register', data);
    return res.data;
  },

  async login(data: LoginRequest): Promise<AuthResponse> {
    const res = await apiClient.post<AuthResponse>('/auth/login', data);
    return res.data;
  },

  async logout(): Promise<void> {
    try {
      await apiClient.post('/auth/logout');
    } finally {
      // Always clear local state regardless of backend response
      localStorage.removeItem('aeroai_token');
      localStorage.removeItem('aeroai_user');
    }
  },

  async getMe(): Promise<UserDto> {
    const res = await apiClient.get<UserDto>('/me');
    return res.data;
  },

  async getMyFlights(): Promise<TrackedFlightDto[]> {
    const res = await apiClient.get<TrackedFlightDto[]>('/me/flights');
    return res.data;
  },
};
