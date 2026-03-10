# Instructions: Insurance Claim Submission System - Frontend UI Implementation

This document provides step-by-step implementation instructions for building the Insurance Claim Submission System frontend using React, TypeScript, Vite, Tailwind CSS, and React Query.

---

## Prerequisites

- Node.js 18+ with npm
- Backend running at `http://localhost:8080/api/v1`
- Familiarity with React, TypeScript, and REST APIs

---

## Phase 1: Project Initialization & Setup

### Step 1.1: Initialize Vite Project

```bash
npm create vite@latest insurance-frontend -- --template react-ts
cd insurance-frontend
```

### Step 1.2: Install Core Dependencies

```bash
npm install axios @tanstack/react-query@5 react-router-dom react-hook-form @hookform/resolvers zod lucide-react react-hot-toast zustand
```

### Step 1.3: Install Dev Dependencies

```bash
npm install -D tailwindcss postcss autoprefixer @types/react @types/react-dom
```

### Step 1.4: Configure Tailwind CSS

```bash
npx tailwindcss init -p
```

Edit `tailwind.config.js`:
```javascript
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {},
  },
  plugins: [],
}
```

Create `src/main.css`:
```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

Update `src/main.tsx` to import `main.css`:
```typescript
import './main.css'
```

### Step 1.5: Create Environment Files

Create `.env`:
```
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

Create `.env.example`:
```
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

### Step 1.6: Configure Vite (Optional - API Proxy)

Edit `vite.config.ts`:
```typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api/v1': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      }
    }
  }
})
```

---

## Phase 2: Type Definitions & API Client

### Step 2.1: Create Type Definitions

Create `src/types/index.ts`:

```typescript
// Enums
export enum ClaimStatus {
  SUBMITTED = 'SUBMITTED',
  IN_REVIEW = 'IN_REVIEW',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
}

export enum ClaimType {
  MEDICAL = 'MEDICAL',
  DENTAL = 'DENTAL',
  VISION = 'VISION',
  LIFE = 'LIFE',
  AUTO = 'AUTO',
  HOME = 'HOME',
  DISABILITY = 'DISABILITY',
}

export enum PolicyStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  EXPIRED = 'EXPIRED',
  CANCELLED = 'CANCELLED',
  PENDING = 'PENDING',
}

export enum ReviewAction {
  APPROVE = 'APPROVE',
  REJECT = 'REJECT',
}

// DTOs
export interface ClaimSubmissionRequest {
  policyNumber: string;
  claimType: ClaimType;
  claimAmount: number;
  incidentDate: string; // YYYY-MM-DD
  description: string;
}

export interface ClaimResponse {
  claimId: number;
  policyId: number;
  policyNumber: string;
  claimType: ClaimType;
  claimAmount: number;
  incidentDate: string; // YYYY-MM-DD
  description: string;
  status: ClaimStatus;
  createdAt: string; // ISO 8601 datetime
  updatedAt: string; // ISO 8601 datetime
}

export interface ClaimReviewRequest {
  action: ReviewAction;
  reviewerNotes?: string;
}

export interface ClaimHistoryResponse {
  historyId: number;
  claimId: number;
  status: ClaimStatus;
  timestamp: string; // ISO 8601 datetime
  reviewerNotes?: string;
}

export interface PolicyResponse {
  policyId: number;
  policyNumber: string;
  customerId: number;
  status: PolicyStatus;
  effectiveDate: string; // YYYY-MM-DD
  expiryDate: string; // YYYY-MM-DD
  coverageLimit: number;
  coverageLimits: Record<ClaimType, number>; // Per-type limits
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  userId: number;
  role: 'CUSTOMER' | 'ADMIN';
}

// Auth & User
export interface AuthUser {
  userId: number;
  role: 'CUSTOMER' | 'ADMIN';
}
```

### Step 2.2: Create Error Handling

Create `src/api/errors.ts`:

```typescript
export class ApiError extends Error {
  constructor(
    public status: number,
    public message: string,
    public details?: Record<string, string>
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

export const getErrorMessage = (error: unknown): string => {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'An unexpected error occurred';
};

export const mapHttpErrorToMessage = (status: number, message?: string): string => {
  switch (status) {
    case 400:
      return message || 'Invalid request. Please check your input.';
    case 401:
      return 'Unauthorized. Please log in again.';
    case 403:
      return 'Forbidden. You do not have permission to perform this action.';
    case 404:
      return message || 'Resource not found.';
    case 409:
      return message || 'Duplicate claim detected within 24 hours.';
    case 500:
      return 'Server error. Please try again later.';
    default:
      return message || 'An error occurred. Please try again.';
  }
};
```

### Step 2.3: Create API Client

Create `src/api/client.ts`:

```typescript
import axios, { AxiosInstance } from 'axios';
import {
  ClaimSubmissionRequest,
  ClaimResponse,
  ClaimReviewRequest,
  ClaimHistoryResponse,
  PolicyResponse,
  LoginRequest,
  LoginResponse,
} from '../types';
import { ApiError, mapHttpErrorToMessage } from './errors';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

class ApiClient {
  private axiosInstance: AxiosInstance;

  constructor() {
    this.axiosInstance = axios.create({
      baseURL: BASE_URL,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor: add auth token
    this.axiosInstance.interceptors.request.use((config) => {
      const token = localStorage.getItem('auth_token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    });

    // Response interceptor: handle errors
    this.axiosInstance.interceptors.response.use(
      (response) => response,
      (error) => {
        if (axios.isAxiosError(error) && error.response) {
          const status = error.response.status;
          const message = error.response.data?.message || mapHttpErrorToMessage(status);
          const details = error.response.data?.details || {};
          throw new ApiError(status, message, details);
        }
        throw error;
      }
    );
  }

  // Auth
  async login(username: string, password: string): Promise<LoginResponse> {
    const response = await this.axiosInstance.post<LoginResponse>('/auth/login', {
      username,
      password,
    } as LoginRequest);
    return response.data;
  }

  // Claims
  async submitClaim(payload: ClaimSubmissionRequest): Promise<ClaimResponse> {
    const response = await this.axiosInstance.post<ClaimResponse>('/claims', payload);
    return response.data;
  }

  async getClaim(claimId: number): Promise<ClaimResponse> {
    const response = await this.axiosInstance.get<ClaimResponse>(`/claims/${claimId}`);
    return response.data;
  }

  async reviewClaim(claimId: number, payload: ClaimReviewRequest): Promise<ClaimResponse> {
    const response = await this.axiosInstance.patch<ClaimResponse>(
      `/claims/${claimId}/review`,
      payload
    );
    return response.data;
  }

  async getClaimHistory(claimId: number): Promise<ClaimHistoryResponse[]> {
    const response = await this.axiosInstance.get<ClaimHistoryResponse[]>(
      `/claims/${claimId}/history`
    );
    return response.data;
  }

  async getClaimsByPolicy(policyId: number): Promise<ClaimResponse[]> {
    const response = await this.axiosInstance.get<ClaimResponse[]>(
      `/claims/policy/${policyId}`
    );
    return response.data;
  }

  // Policies
  async getPolicies(): Promise<PolicyResponse[]> {
    const response = await this.axiosInstance.get<PolicyResponse[]>('/policies');
    return response.data;
  }

  async getPolicy(policyNumber: string): Promise<PolicyResponse> {
    const response = await this.axiosInstance.get<PolicyResponse>(
      `/policies/${policyNumber}`
    );
    return response.data;
  }
}

export const client = new ApiClient();
```

---

## Phase 3: Authentication & Authorization

### Step 3.1: Create Auth Store

Create `src/stores/authStore.ts`:

```typescript
import { create } from 'zustand';
import { AuthUser } from '../types';
import { client } from '../api/client';

interface AuthStore {
  user: AuthUser | null;
  token: string | null;
  isLoading: boolean;
  error: string | null;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  loadFromStorage: () => void;
  isAuthenticated: () => boolean;
  isAdmin: () => boolean;
  isCustomer: () => boolean;
}

export const useAuthStore = create<AuthStore>((set, get) => ({
  user: null,
  token: null,
  isLoading: false,
  error: null,

  login: async (username: string, password: string) => {
    set({ isLoading: true, error: null });
    try {
      const response = await client.login(username, password);
      const { token, userId, role } = response;

      localStorage.setItem('auth_token', token);
      localStorage.setItem('auth_user', JSON.stringify({ userId, role }));

      set({
        token,
        user: { userId, role },
        isLoading: false,
      });
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Login failed';
      set({ error: message, isLoading: false });
      throw error;
    }
  },

  logout: () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_user');
    set({ user: null, token: null, error: null });
  },

  loadFromStorage: () => {
    const token = localStorage.getItem('auth_token');
    const userStr = localStorage.getItem('auth_user');

    if (token && userStr) {
      try {
        const user = JSON.parse(userStr);
        set({ token, user });
      } catch {
        // Invalid stored data
        localStorage.removeItem('auth_token');
        localStorage.removeItem('auth_user');
      }
    }
  },

  isAuthenticated: () => {
    const state = get();
    return !!state.token && !!state.user;
  },

  isAdmin: () => {
    const state = get();
    return state.user?.role === 'ADMIN';
  },

  isCustomer: () => {
    const state = get();
    return state.user?.role === 'CUSTOMER';
  },
}));
```

### Step 3.2: Create Protected Routes

Create `src/components/ProtectedRoute.tsx`:

```typescript
import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requireRole?: 'ADMIN' | 'CUSTOMER';
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requireRole,
}) => {
  const { isAuthenticated, user } = useAuthStore();

  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  if (requireRole && user?.role !== requireRole) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
};
```

### Step 3.3: Create Login Page

Create `src/pages/LoginPage.tsx`:

```typescript
import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import toast from 'react-hot-toast';

export const LoginPage: React.FC = () => {
  const navigate = useNavigate();
  const { login, isLoading, error } = useAuthStore();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      await login(username, password);
      toast.success('Login successful!');
      navigate('/');
    } catch (err) {
      toast.error(error || 'Login failed');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="max-w-md w-full bg-white p-8 rounded-lg shadow">
        <h1 className="text-2xl font-bold mb-6 text-center">Insurance Claims Portal</h1>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700">Username</label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="mt-1 w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
              placeholder="Enter username"
              required
              disabled={isLoading}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="mt-1 w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
              placeholder="Enter password"
              required
              disabled={isLoading}
            />
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className="w-full bg-blue-600 text-white py-2 rounded-lg hover:bg-blue-700 disabled:opacity-50"
          >
            {isLoading ? 'Logging in...' : 'Login'}
          </button>
        </form>

        <div className="mt-6 p-4 bg-blue-50 rounded text-sm text-gray-700">
          <p className="font-semibold mb-2">Demo Credentials:</p>
          <p>Customer: customer / password</p>
          <p>Admin: admin / password</p>
        </div>
      </div>
    </div>
  );
};
```

---

## Phase 4: Layout & Core Routing

### Step 4.1: Create Navbar Component

Create `src/components/layout/Navbar.tsx`:

```typescript
import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../stores/authStore';
import { LogOut, Menu, X } from 'lucide-react';
import { useState } from 'react';

export const Navbar: React.FC = () => {
  const navigate = useNavigate();
  const { user, isAuthenticated, logout, isAdmin } = useAuthStore();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  if (!isAuthenticated()) {
    return null;
  }

  return (
    <nav className="bg-blue-600 text-white shadow-lg">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Logo */}
          <Link to="/" className="text-xl font-bold">
            Insurance Claims
          </Link>

          {/* Desktop Menu */}
          <div className="hidden md:flex space-x-6">
            <Link to="/" className="hover:opacity-80">
              Dashboard
            </Link>
            <Link to="/policies" className="hover:opacity-80">
              Policies
            </Link>
            {!isAdmin() && (
              <Link to="/submit-claim" className="hover:opacity-80">
                Submit Claim
              </Link>
            )}
            {isAdmin() && (
              <Link to="/admin/policies" className="hover:opacity-80">
                Admin Dashboard
              </Link>
            )}
          </div>

          {/* User & Logout */}
          <div className="hidden md:flex items-center space-x-4">
            <span className="text-sm">
              {user?.role === 'ADMIN' ? 'Admin' : 'Customer'}
            </span>
            <button
              onClick={handleLogout}
              className="flex items-center space-x-2 hover:opacity-80"
            >
              <LogOut size={18} />
              <span>Logout</span>
            </button>
          </div>

          {/* Mobile Menu Toggle */}
          <button
            className="md:hidden"
            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
          >
            {mobileMenuOpen ? <X /> : <Menu />}
          </button>
        </div>

        {/* Mobile Menu */}
        {mobileMenuOpen && (
          <div className="md:hidden pb-4 space-y-2">
            <Link
              to="/"
              className="block px-4 py-2 hover:bg-blue-700 rounded"
              onClick={() => setMobileMenuOpen(false)}
            >
              Dashboard
            </Link>
            <Link
              to="/policies"
              className="block px-4 py-2 hover:bg-blue-700 rounded"
              onClick={() => setMobileMenuOpen(false)}
            >
              Policies
            </Link>
            {!isAdmin() && (
              <Link
                to="/submit-claim"
                className="block px-4 py-2 hover:bg-blue-700 rounded"
                onClick={() => setMobileMenuOpen(false)}
              >
                Submit Claim
              </Link>
            )}
            {isAdmin() && (
              <Link
                to="/admin/policies"
                className="block px-4 py-2 hover:bg-blue-700 rounded"
                onClick={() => setMobileMenuOpen(false)}
              >
                Admin Dashboard
              </Link>
            )}
            <button
              onClick={() => {
                handleLogout();
                setMobileMenuOpen(false);
              }}
              className="w-full text-left px-4 py-2 hover:bg-blue-700 rounded flex items-center space-x-2"
            >
              <LogOut size={18} />
              <span>Logout</span>
            </button>
          </div>
        )}
      </div>
    </nav>
  );
};
```

### Step 4.2: Create Home Page

Create `src/pages/Home.tsx`:

```typescript
import React from 'react';
import { useAuthStore } from '../stores/authStore';

export const Home: React.FC = () => {
  const { user } = useAuthStore();
  const isAdmin = user?.role === 'ADMIN';

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <h1 className="text-3xl font-bold mb-8">Welcome to Insurance Claims Portal</h1>

        {isAdmin ? (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="bg-white p-6 rounded-lg shadow">
              <h2 className="text-xl font-semibold mb-4">Admin Features</h2>
              <ul className="space-y-2 text-gray-700">
                <li>• View all policies and their active coverages</li>
                <li>• Review submitted claims</li>
                <li>• Approve or reject claims with notes</li>
                <li>• Track claim history and status changes</li>
              </ul>
            </div>
            <div className="bg-white p-6 rounded-lg shadow">
              <h3 className="text-lg font-semibold mb-4">Quick Actions</h3>
              <a href="/admin/policies" className="inline-block bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">
                Go to Admin Dashboard
              </a>
            </div>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="bg-white p-6 rounded-lg shadow">
              <h2 className="text-xl font-semibold mb-4">Customer Features</h2>
              <ul className="space-y-2 text-gray-700">
                <li>• Submit new insurance claims</li>
                <li>• Real-time policy verification</li>
                <li>• View claim status and history</li>
                <li>• Coverage limit validation</li>
              </ul>
            </div>
            <div className="bg-white p-6 rounded-lg shadow">
              <h3 className="text-lg font-semibold mb-4">Quick Actions</h3>
              <a href="/submit-claim" className="inline-block bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700">
                Submit a Claim
              </a>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
```

### Step 4.3: Create Router Configuration

Create `src/App.tsx`:

```typescript
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
          <Route path="/" element={<Home />} />

          {/* Customer & Admin Routes - Add below */}
          {/* Phase 5: <Route path="/submit-claim" element={...} /> */}
          {/* Phase 6: <Route path="/admin/*" element={...} /> */}

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>

        <Toaster position="top-right" />
      </BrowserRouter>
    </QueryClientProvider>
  );
};

export default App;
```

### Step 4.4: Create React Query Configuration

Create `src/lib/queryClient.ts`:

```typescript
import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes
      gcTime: 10 * 60 * 1000, // 10 minutes (formerly cacheTime)
      retry: 1,
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),
    },
    mutations: {
      retry: 1,
    },
  },
});
```

---

## Phase 5: Customer View - Claim Submission Form

### Step 5.1: Create Formatters Utility

Create `src/utils/formatters.ts`:

```typescript
export const formatCurrency = (amount: number): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount);
};

export const formatDate = (date: string | Date): string => {
  const d = typeof date === 'string' ? new Date(date) : date;
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  }).format(d);
};

export const formatDateTime = (dt: string | Date): string => {
  const d = typeof dt === 'string' ? new Date(dt) : dt;
  return new Intl.DateTimeFormat('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  }).format(d);
};

export const dateToInputFormat = (date: Date): string => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};
```

### Step 5.2: Create UI Components

Create `src/components/ui/Badge.tsx`:

```typescript
import React from 'react';
import { ClaimStatus } from '../../types';

const statusColors: Record<ClaimStatus, string> = {
  [ClaimStatus.SUBMITTED]: 'bg-yellow-100 text-yellow-800',
  [ClaimStatus.IN_REVIEW]: 'bg-yellow-100 text-yellow-800',
  [ClaimStatus.APPROVED]: 'bg-green-100 text-green-800',
  [ClaimStatus.REJECTED]: 'bg-red-100 text-red-800',
};

interface BadgeProps {
  status: ClaimStatus;
}

export const Badge: React.FC<BadgeProps> = ({ status }) => {
  return (
    <span className={`px-3 py-1 rounded-full text-sm font-medium ${statusColors[status]}`}>
      {status}
    </span>
  );
};
```

Create `src/components/ui/Button.tsx`:

```typescript
import React from 'react';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
}

const variantClasses = {
  primary: 'bg-blue-600 hover:bg-blue-700 text-white',
  secondary: 'bg-gray-200 hover:bg-gray-300 text-gray-800',
  danger: 'bg-red-600 hover:bg-red-700 text-white',
};

const sizeClasses = {
  sm: 'px-3 py-1 text-sm',
  md: 'px-4 py-2',
  lg: 'px-6 py-3 text-lg',
};

export const Button: React.FC<ButtonProps> = ({
  variant = 'primary',
  size = 'md',
  isLoading = false,
  disabled,
  children,
  ...props
}) => {
  return (
    <button
      className={`
        rounded-lg font-medium transition-colors
        ${variantClasses[variant]}
        ${sizeClasses[size]}
        ${isLoading || disabled ? 'opacity-50 cursor-not-allowed' : ''}
      `}
      disabled={isLoading || disabled}
      {...props}
    >
      {isLoading ? 'Loading...' : children}
    </button>
  );
};
```

Create `src/components/ui/Card.tsx`:

```typescript
import React from 'react';

interface CardProps {
  children: React.ReactNode;
  className?: string;
}

export const Card: React.FC<CardProps> = ({ children, className = '' }) => {
  return (
    <div className={`bg-white rounded-lg shadow p-6 ${className}`}>
      {children}
    </div>
  );
};
```

Create `src/components/ui/Modal.tsx`:

```typescript
import React from 'react';
import { X } from 'lucide-react';

interface ModalProps {
  isOpen: boolean;
  title: string;
  children: React.ReactNode;
  onClose: () => void;
  actions?: React.ReactNode;
}

export const Modal: React.FC<ModalProps> = ({
  isOpen,
  title,
  children,
  onClose,
  actions,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl max-w-lg w-full mx-4">
        <div className="flex justify-between items-center p-6 border-b">
          <h2 className="text-xl font-bold">{title}</h2>
          <button onClick={onClose} className="text-gray-500 hover:text-gray-700">
            <X size={24} />
          </button>
        </div>
        <div className="p-6">{children}</div>
        {actions && (
          <div className="flex justify-end gap-4 p-6 border-t bg-gray-50">
            {actions}
          </div>
        )}
      </div>
    </div>
  );
};
```

### Step 5.3: Create Claim Submission Form Component

Create `src/components/forms/ClaimSubmissionForm.tsx`:

```typescript
import React, { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import toast from 'react-hot-toast';
import { ClaimSubmissionRequest, ClaimType, PolicyResponse } from '../../types';
import { Button } from '../ui/Button';
import { Modal } from '../ui/Modal';
import { Card } from '../ui/Card';
import { client } from '../../api/client';
import { ApiError } from '../../api/errors';
import { formatCurrency, dateToInputFormat } from '../../utils/formatters';

const submissionSchema = z.object({
  policyNumber: z.string()
    .min(1, 'Policy number is required')
    .regex(/^POL-[A-Z0-9]{5}$/, 'Policy number must match format POL-XXXXX'),
  claimType: z.nativeEnum(ClaimType, { message: 'Please select a claim type' }),
  claimAmount: z.coerce.number()
    .positive('Claim amount must be positive')
    .min(0.01, 'Claim amount must be at least $0.01'),
  incidentDate: z.string()
    .min(1, 'Incident date is required'),
  description: z.string()
    .min(10, 'Description must be at least 10 characters')
    .max(1000, 'Description must not exceed 1000 characters'),
});

type SubmissionFormData = z.infer<typeof submissionSchema>;

export const ClaimSubmissionForm: React.FC = () => {
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
    reset,
  } = useForm<SubmissionFormData>({
    resolver: zodResolver(submissionSchema),
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [policy, setPolicy] = useState<PolicyResponse | null>(null);
  const [policyError, setPolicyError] = useState<string | null>(null);
  const [successModalOpen, setSuccessModalOpen] = useState(false);
  const [successClaimId, setSuccessClaimId] = useState<number | null>(null);
  const [errorModalOpen, setErrorModalOpen] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string>('');

  const policyNumber = watch('policyNumber');
  const claimType = watch('claimType');
  const claimAmount = watch('claimAmount');

  // Fetch and validate policy
  useEffect(() => {
    const validatePolicy = async () => {
      if (!policyNumber || policyNumber.length < 10) {
        setPolicy(null);
        setPolicyError(null);
        return;
      }

      try {
        const policyData = await client.getPolicy(policyNumber);
        if (policyData.status !== 'ACTIVE') {
          setPolicy(null);
          setPolicyError('Policy is not active');
        } else {
          setPolicy(policyData);
          setPolicyError(null);
        }
      } catch (error) {
        setPolicy(null);
        setPolicyError(error instanceof ApiError ? error.message : 'Policy not found');
      }
    };

    const timer = setTimeout(validatePolicy, 500);
    return () => clearTimeout(timer);
  }, [policyNumber]);

  const handleFormSubmit = async (data: SubmissionFormData) => {
    setIsSubmitting(true);
    try {
      const payload: ClaimSubmissionRequest = {
        policyNumber: data.policyNumber,
        claimType: data.claimType,
        claimAmount: parseFloat(data.claimAmount.toString()),
        incidentDate: data.incidentDate,
        description: data.description,
      };

      const response = await client.submitClaim(payload);
      setSuccessClaimId(response.claimId);
      setSuccessModalOpen(true);
      reset();
      setPolicy(null);
    } catch (error) {
      if (error instanceof ApiError) {
        if (error.status === 409) {
          setErrorMessage('Duplicate claim detected within 24 hours');
        } else if (error.status === 400) {
          setErrorMessage(error.message || 'Invalid claim submission');
        } else {
          setErrorMessage(error.message);
        }
      } else {
        setErrorMessage('Failed to submit claim. Please try again.');
      }
      setErrorModalOpen(true);
    } finally {
      setIsSubmitting(false);
    }
  };

  const maxDate = dateToInputFormat(new Date());
  const minDate = policy ? policy.effectiveDate : undefined;
  const coverageLimit = claimType && policy?.coverageLimits[claimType]
    ? policy.coverageLimits[claimType]
    : null;
  const isCoverageValid = !claimAmount || !coverageLimit || claimAmount <= coverageLimit;

  return (
    <>
      <div className="max-w-2xl mx-auto py-8 px-4">
        <Card>
          <h1 className="text-2xl font-bold mb-6">Submit Insurance Claim</h1>

          <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-6">
            {/* Policy Number */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Policy Number *
              </label>
              <input
                type="text"
                placeholder="POL-XXXXX"
                {...register('policyNumber')}
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
              />
              {errors.policyNumber && (
                <p className="text-red-600 text-sm mt-1">{errors.policyNumber.message}</p>
              )}
              {policyError && (
                <p className="text-red-600 text-sm mt-1">{policyError}</p>
              )}
              {policy && (
                <div className="mt-2 p-3 bg-green-50 border border-green-200 rounded">
                  <p className="text-sm text-green-700">✓ Policy verified and active</p>
                </div>
              )}
            </div>

            {/* Claim Type */}
            {policy && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Claim Type *
                </label>
                <select
                  {...register('claimType')}
                  className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Select claim type</option>
                  {Object.entries(policy.coverageLimits).map(([type]) => (
                    <option key={type} value={type}>
                      {type}
                    </option>
                  ))}
                </select>
                {errors.claimType && (
                  <p className="text-red-600 text-sm mt-1">{errors.claimType.message}</p>
                )}
              </div>
            )}

            {/* Incident Date */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Incident Date *
              </label>
              <input
                type="date"
                {...register('incidentDate')}
                max={maxDate}
                min={minDate}
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
              />
              {errors.incidentDate && (
                <p className="text-red-600 text-sm mt-1">{errors.incidentDate.message}</p>
              )}
            </div>

            {/* Claim Amount */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Claim Amount ($) *
              </label>
              <input
                type="number"
                step="0.01"
                placeholder="0.00"
                {...register('claimAmount')}
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
              />
              {errors.claimAmount && (
                <p className="text-red-600 text-sm mt-1">{errors.claimAmount.message}</p>
              )}
              {coverageLimit && (
                <p className="text-sm text-gray-600 mt-1">
                  Coverage limit: {formatCurrency(coverageLimit)}
                </p>
              )}
              {!isCoverageValid && (
                <p className="text-red-600 text-sm mt-1">Amount exceeds coverage limit</p>
              )}
            </div>

            {/* Description */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Description *
              </label>
              <textarea
                {...register('description')}
                rows={4}
                placeholder="Describe your claim (10-1000 characters)"
                className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500"
              />
              {errors.description && (
                <p className="text-red-600 text-sm mt-1">{errors.description.message}</p>
              )}
            </div>

            {/* Submit Button */}
            <div className="flex gap-4">
              <Button
                type="submit"
                variant="primary"
                size="lg"
                isLoading={isSubmitting}
                disabled={!policy || !isCoverageValid}
              >
                Submit Claim
              </Button>
              <Button
                type="button"
                variant="secondary"
                size="lg"
                onClick={() => {
                  reset();
                  setPolicy(null);
                }}
              >
                Reset
              </Button>
            </div>
          </form>
        </Card>
      </div>

      {/* Success Modal */}
      <Modal
        isOpen={successModalOpen}
        title="Claim Submitted Successfully"
        onClose={() => {
          setSuccessModalOpen(false);
          setSuccessClaimId(null);
        }}
        actions={
          <>
            <Button
              variant="secondary"
              onClick={() => {
                setSuccessModalOpen(false);
                setSuccessClaimId(null);
              }}
            >
              Submit Another
            </Button>
            <Button variant="primary">View Claim Details</Button>
          </>
        }
      >
        <div className="space-y-4">
          <p>Your claim has been submitted successfully!</p>
          <div className="bg-blue-50 p-4 rounded border border-blue-200">
            <p className="text-sm text-gray-600">Claim ID</p>
            <p className="text-2xl font-bold text-blue-600">{successClaimId}</p>
          </div>
          <p className="text-sm text-gray-600">
            Please save your claim ID for future reference. You can track the status of your claim anytime.
          </p>
        </div>
      </Modal>

      {/* Error Modal */}
      <Modal
        isOpen={errorModalOpen}
        title="Submission Error"
        onClose={() => setErrorModalOpen(false)}
        actions={
          <Button
            variant="primary"
            onClick={() => setErrorModalOpen(false)}
          >
            Close
          </Button>
        }
      >
        <p className="text-red-600">{errorMessage}</p>
      </Modal>
    </>
  );
};
```

### Step 5.4: Create Submit Claim Page

Create `src/pages/SubmitClaimPage.tsx`:

```typescript
import React from 'react';
import { ClaimSubmissionForm } from '../components/forms/ClaimSubmissionForm';

export const SubmitClaimPage: React.FC = () => {
  return <ClaimSubmissionForm />;
};
```

### Step 5.5: Update Router with Claim Routes

Update `src/App.tsx` to include:

```typescript
<Route
  path="/submit-claim"
  element={
    <ProtectedRoute requireRole="CUSTOMER">
      <SubmitClaimPage />
    </ProtectedRoute>
  }
/>
```

---

## Phase 6: Admin View - Policy List & Claim Management

### Step 6.1: Create Admin Pages (Overview)

Phase 6 involves creating three admin pages:
1. `AdminPoliciesPage.tsx` — List active policies, select to view claims
2. `ClaimListPage.tsx` — Show claims filtered by selected policy
3. `ClaimDetailPage.tsx` — Full claim detail + history timeline + review actions

### Step 6.2: Update Router with Admin Routes

Add to `src/App.tsx`:

```typescript
<Route
  path="/admin/*"
  element={
    <ProtectedRoute requireRole="ADMIN">
      <AdminRoutes />
    </ProtectedRoute>
  }
/>
```

Create `src/pages/admin/AdminRoutes.tsx`:

```typescript
import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { AdminPoliciesPage } from './AdminPoliciesPage';
import { ClaimListPage } from './ClaimListPage';
import { ClaimDetailPage } from './ClaimDetailPage';

export const AdminRoutes: React.FC = () => {
  return (
    <Routes>
      <Route path="/policies" element={<AdminPoliciesPage />} />
      <Route path="/claims/:policyId" element={<ClaimListPage />} />
      <Route path="/claims/:policyId/:claimId" element={<ClaimDetailPage />} />
      <Route path="*" element={<Navigate to="/admin/policies" replace />} />
    </Routes>
  );
};
```

---

## Phases 7-10: Remaining Implementation

The remaining phases (7-10) cover:
- **Phase 7**: Shared UI components (LoadingSpinner, ErrorAlert, etc.)
- **Phase 8**: React Query hooks (useClaims, usePolicies, etc.)
- **Phase 9**: Build configuration and final structure
- **Phase 10**: Error handling and toast notifications

These should be implemented incrementally following the same structure and patterns established in Phases 1-6.

---

## Development Workflow

### Start Development Server

```bash
npm run dev
```

The app will be available at `http://localhost:5173`

### Run Type Checking

```bash
npm run type-check
```

### Build for Production

```bash
npm run build
```

### Preview Production Build

```bash
npm run preview
```

---

## Testing Checklist

Before committing each phase:

- [ ] No TypeScript errors (`npm run type-check`)
- [ ] All routes accessible and render correctly
- [ ] Auth flow: login → home → logout
- [ ] Form validation works (Zod schemas enforced)
- [ ] API calls succeed with mock/real backend
- [ ] Error messages display correctly
- [ ] Toast notifications show for success/error
- [ ] Responsive design works on mobile/tablet/desktop
- [ ] React Query cache works (navigate away/back = instant load)

---

## Next Steps

1. **Backend verification**: Confirm `/api/v1/auth/login` and `/api/v1/policies` endpoints exist
2. **API mocking** (optional): Use MSW for development without backend
3. **Continue implementation**: Follow phases 5-10 in sequence
4. **Testing**: Run manual testing checklist after each phase
5. **Deployment**: Deploy to staging/production after all phases complete

Good luck with the implementation!
