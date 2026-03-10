import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { Toaster } from 'react-hot-toast';
import { useAuthStore } from './stores/authStore';
import { queryClient } from './lib/queryClient';
import { Navbar } from './components/layout/Navbar';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginPage } from './pages/LoginPage';
import { Home } from './pages/Home';
import { SubmitClaimPage } from './pages/SubmitClaimPage';
import { TrackClaimPage } from './pages/TrackClaimPage';
import { AdminRoutes } from './pages/admin/AdminRoutes';

export const App: React.FC = () => {
  const { loadFromStorage } = useAuthStore();

  useEffect(() => {
    loadFromStorage();
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Navbar />
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Home />
              </ProtectedRoute>
            }
          />
          <Route
            path="/submit-claim"
            element={
              <ProtectedRoute requireRole="CUSTOMER">
                <SubmitClaimPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/track-claim"
            element={
              <ProtectedRoute requireRole="CUSTOMER">
                <TrackClaimPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/admin/*"
            element={
              <ProtectedRoute requireRole="ADMIN">
                <AdminRoutes />
              </ProtectedRoute>
            }
          />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
        <Toaster position="top-right" />
      </BrowserRouter>
    </QueryClientProvider>
  );
};

export default App;
