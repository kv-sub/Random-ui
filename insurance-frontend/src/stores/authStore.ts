import { create } from 'zustand';
import type { AuthUser } from '../types';

interface AuthStore {
  user: AuthUser | null;
  login: (role: 'CUSTOMER' | 'ADMIN') => void;
  logout: () => void;
  loadFromStorage: () => void;
  isAuthenticated: () => boolean;
  isAdmin: () => boolean;
  isCustomer: () => boolean;
}

export const useAuthStore = create<AuthStore>((set, get) => ({
  user: null,

  login: (role: 'CUSTOMER' | 'ADMIN') => {
    const user: AuthUser = { role };
    localStorage.setItem('auth_user', JSON.stringify(user));
    set({ user });
  },

  logout: () => {
    localStorage.removeItem('auth_user');
    set({ user: null });
  },

  loadFromStorage: () => {
    const userStr = localStorage.getItem('auth_user');
    if (userStr) {
      try {
        const user = JSON.parse(userStr) as AuthUser;
        set({ user });
      } catch {
        localStorage.removeItem('auth_user');
      }
    }
  },

  isAuthenticated: () => !!get().user,
  isAdmin: () => get().user?.role === 'ADMIN',
  isCustomer: () => get().user?.role === 'CUSTOMER',
}));
