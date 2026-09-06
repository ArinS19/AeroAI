import axios, { type AxiosError } from 'axios';
import type { ApiError } from '../types';

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

export const apiClient = axios.create({
  baseURL: `${API_BASE}/api`,
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
});

// ─── Request interceptor: attach JWT ──────────────────────────────────────────
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('aeroai_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ─── Response interceptor: 401 → clear token + redirect ──────────────────────
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('aeroai_token');
      localStorage.removeItem('aeroai_user');
      // Navigate to login — use window.location so we don't need router context here
      const current = window.location.pathname + window.location.search;
      const returnTo = encodeURIComponent(current);
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = `/login?returnTo=${returnTo}`;
      }
    }
    return Promise.reject(error);
  }
);

/**
 * Extract a user-readable message from an Axios error.
 * Branch on `error` (machine code) never on `message` text per contract §AD.
 */
export function extractErrorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as ApiError | undefined;
    if (data?.message) return data.message;
    if (err.message) return err.message;
  }
  return 'An unexpected error occurred.';
}

export function extractErrorCode(err: unknown): string | null {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as ApiError | undefined;
    return data?.error ?? null;
  }
  return null;
}
