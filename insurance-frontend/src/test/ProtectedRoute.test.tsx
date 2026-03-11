import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { describe, it, expect, beforeEach } from 'vitest';
import { ProtectedRoute } from '../components/ProtectedRoute';
import { useAuthStore } from '../stores/authStore';

function ChildContent() {
  return <div>Protected Content</div>;
}

function MockLoginPage() {
  return <div>Login Page</div>;
}

function MockHomePage() {
  return <div>Home Page</div>;
}

function renderRoutes(initialPath: string = '/') {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/login" element={<MockLoginPage />} />
        <Route path="/" element={<MockHomePage />} />
        <Route
          path="/protected"
          element={
            <ProtectedRoute>
              <ChildContent />
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin"
          element={
            <ProtectedRoute requireRole="ADMIN">
              <ChildContent />
            </ProtectedRoute>
          }
        />
        <Route
          path="/customer"
          element={
            <ProtectedRoute requireRole="CUSTOMER">
              <ChildContent />
            </ProtectedRoute>
          }
        />
      </Routes>
    </MemoryRouter>
  );
}

describe('ProtectedRoute', () => {
  beforeEach(() => {
    useAuthStore.getState().logout();
    localStorage.clear();
  });

  it('redirects unauthenticated user to login page', () => {
    renderRoutes('/protected');

    expect(screen.getByText('Login Page')).toBeInTheDocument();
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });

  it('renders children for authenticated user with no role requirement', () => {
    useAuthStore.getState().login('CUSTOMER');
    renderRoutes('/protected');

    expect(screen.getByText('Protected Content')).toBeInTheDocument();
    expect(screen.queryByText('Login Page')).not.toBeInTheDocument();
  });

  it('renders children for authenticated ADMIN with no role requirement', () => {
    useAuthStore.getState().login('ADMIN');
    renderRoutes('/protected');

    expect(screen.getByText('Protected Content')).toBeInTheDocument();
  });

  it('redirects CUSTOMER trying to access ADMIN route', () => {
    useAuthStore.getState().login('CUSTOMER');
    renderRoutes('/admin');

    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
    expect(screen.getByText('Home Page')).toBeInTheDocument();
  });

  it('allows ADMIN to access ADMIN-required route', () => {
    useAuthStore.getState().login('ADMIN');
    renderRoutes('/admin');

    expect(screen.getByText('Protected Content')).toBeInTheDocument();
  });

  it('redirects unauthenticated user from ADMIN route to login', () => {
    renderRoutes('/admin');

    expect(screen.getByText('Login Page')).toBeInTheDocument();
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });

  it('allows CUSTOMER to access CUSTOMER-required route', () => {
    useAuthStore.getState().login('CUSTOMER');
    renderRoutes('/customer');

    expect(screen.getByText('Protected Content')).toBeInTheDocument();
  });

  it('redirects ADMIN trying to access CUSTOMER-only route', () => {
    useAuthStore.getState().login('ADMIN');
    renderRoutes('/customer');

    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
    expect(screen.getByText('Home Page')).toBeInTheDocument();
  });
});
