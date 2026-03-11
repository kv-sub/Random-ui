import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore } from '../stores/authStore';

describe('authStore', () => {
  beforeEach(() => {
    useAuthStore.getState().logout();
    localStorage.clear();
  });

  it('initial state is unauthenticated with no user', () => {
    expect(useAuthStore.getState().isAuthenticated()).toBe(false);
    expect(useAuthStore.getState().user).toBeNull();
    expect(useAuthStore.getState().isAdmin()).toBe(false);
    expect(useAuthStore.getState().isCustomer()).toBe(false);
  });

  it('login as CUSTOMER sets user and persists to localStorage', () => {
    useAuthStore.getState().login('CUSTOMER');

    expect(useAuthStore.getState().isAuthenticated()).toBe(true);
    expect(useAuthStore.getState().isCustomer()).toBe(true);
    expect(useAuthStore.getState().isAdmin()).toBe(false);
    expect(useAuthStore.getState().user?.role).toBe('CUSTOMER');

    const stored = localStorage.getItem('auth_user');
    expect(stored).not.toBeNull();
    const parsed = JSON.parse(stored!);
    expect(parsed.role).toBe('CUSTOMER');
  });

  it('login as ADMIN sets admin role correctly', () => {
    useAuthStore.getState().login('ADMIN');

    expect(useAuthStore.getState().isAuthenticated()).toBe(true);
    expect(useAuthStore.getState().isAdmin()).toBe(true);
    expect(useAuthStore.getState().isCustomer()).toBe(false);
    expect(useAuthStore.getState().user?.role).toBe('ADMIN');
  });

  it('logout clears user and removes from localStorage', () => {
    useAuthStore.getState().login('CUSTOMER');
    useAuthStore.getState().logout();

    expect(useAuthStore.getState().isAuthenticated()).toBe(false);
    expect(useAuthStore.getState().user).toBeNull();
    expect(localStorage.getItem('auth_user')).toBeNull();
  });

  it('loadFromStorage restores CUSTOMER from localStorage', () => {
    localStorage.setItem('auth_user', JSON.stringify({ role: 'CUSTOMER' }));

    useAuthStore.getState().loadFromStorage();

    expect(useAuthStore.getState().isAuthenticated()).toBe(true);
    expect(useAuthStore.getState().isCustomer()).toBe(true);
  });

  it('loadFromStorage restores ADMIN from localStorage', () => {
    localStorage.setItem('auth_user', JSON.stringify({ role: 'ADMIN' }));

    useAuthStore.getState().loadFromStorage();

    expect(useAuthStore.getState().isAdmin()).toBe(true);
  });

  it('loadFromStorage with invalid JSON clears localStorage and leaves unauthenticated', () => {
    localStorage.setItem('auth_user', 'not-valid-json{{{');

    useAuthStore.getState().loadFromStorage();

    expect(useAuthStore.getState().isAuthenticated()).toBe(false);
    expect(localStorage.getItem('auth_user')).toBeNull();
  });

  it('loadFromStorage with empty localStorage leaves unauthenticated', () => {
    useAuthStore.getState().loadFromStorage();

    expect(useAuthStore.getState().isAuthenticated()).toBe(false);
  });

  it('login then logout then loadFromStorage leaves unauthenticated', () => {
    useAuthStore.getState().login('ADMIN');
    useAuthStore.getState().logout();
    useAuthStore.getState().loadFromStorage();

    expect(useAuthStore.getState().isAuthenticated()).toBe(false);
  });
});
