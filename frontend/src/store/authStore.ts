import { create } from 'zustand';
import type { UserDto } from '../types';

interface AuthState {
  token: string | null;
  user: UserDto | null;
  isAuthenticated: boolean;
  setAuth: (token: string, user: UserDto) => void;
  clearAuth: () => void;
}

function loadFromStorage(): { token: string | null; user: UserDto | null } {
  try {
    const token = localStorage.getItem('aeroai_token');
    const userStr = localStorage.getItem('aeroai_user');
    const user = userStr ? (JSON.parse(userStr) as UserDto) : null;
    return { token, user };
  } catch {
    return { token: null, user: null };
  }
}

const initial = loadFromStorage();

export const useAuthStore = create<AuthState>((set) => ({
  token: initial.token,
  user: initial.user,
  isAuthenticated: !!initial.token,

  setAuth: (token, user) => {
    localStorage.setItem('aeroai_token', token);
    localStorage.setItem('aeroai_user', JSON.stringify(user));
    set({ token, user, isAuthenticated: true });
  },

  clearAuth: () => {
    localStorage.removeItem('aeroai_token');
    localStorage.removeItem('aeroai_user');
    set({ token: null, user: null, isAuthenticated: false });
  },
}));
