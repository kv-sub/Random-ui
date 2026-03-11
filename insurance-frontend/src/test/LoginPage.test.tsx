import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { LoginPage } from '../pages/LoginPage';
import { useAuthStore } from '../stores/authStore';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

function renderLoginPage() {
  return render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    mockNavigate.mockReset();
    useAuthStore.getState().logout();
    localStorage.clear();
  });

  it('renders the portal title', () => {
    renderLoginPage();

    expect(screen.getByText('Insurance Claims Portal')).toBeInTheDocument();
  });

  it('renders Customer and Admin login buttons', () => {
    renderLoginPage();

    expect(screen.getByText('Customer')).toBeInTheDocument();
    expect(screen.getByText('Admin / Reviewer')).toBeInTheDocument();
  });

  it('renders demo portal info text', () => {
    renderLoginPage();

    expect(screen.getByText(/demo portal/i)).toBeInTheDocument();
  });

  it('clicking Customer button logs in as CUSTOMER and navigates to home', async () => {
    const user = userEvent.setup();
    renderLoginPage();

    await user.click(screen.getByText('Customer'));

    expect(useAuthStore.getState().isAuthenticated()).toBe(true);
    expect(useAuthStore.getState().isCustomer()).toBe(true);
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('clicking Admin button logs in as ADMIN and navigates to home', async () => {
    const user = userEvent.setup();
    renderLoginPage();

    await user.click(screen.getByText('Admin / Reviewer'));

    expect(useAuthStore.getState().isAuthenticated()).toBe(true);
    expect(useAuthStore.getState().isAdmin()).toBe(true);
    expect(mockNavigate).toHaveBeenCalledWith('/');
  });

  it('CUSTOMER login persists to localStorage', async () => {
    const user = userEvent.setup();
    renderLoginPage();

    await user.click(screen.getByText('Customer'));

    const stored = localStorage.getItem('auth_user');
    expect(stored).not.toBeNull();
    expect(JSON.parse(stored!).role).toBe('CUSTOMER');
  });

  it('ADMIN login persists to localStorage', async () => {
    const user = userEvent.setup();
    renderLoginPage();

    await user.click(screen.getByText('Admin / Reviewer'));

    const stored = localStorage.getItem('auth_user');
    expect(stored).not.toBeNull();
    expect(JSON.parse(stored!).role).toBe('ADMIN');
  });
});
