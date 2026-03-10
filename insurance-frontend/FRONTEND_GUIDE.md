# Insurance Claims Frontend — Developer Guide

A React + TypeScript frontend for the Insurance Claim Submission System. Customers can submit and track claims; admins can review, approve, and reject them.

---

## Tech Stack

| Tool | Version | Purpose |
|------|---------|---------|
| React | 19 | UI framework |
| TypeScript | 5.9 | Type safety |
| Vite | 7 | Build tool & dev server |
| Tailwind CSS | 4 | Styling (via `@tailwindcss/vite`) |
| TanStack React Query | 5 | Server state & caching |
| React Router DOM | 7 | Client-side routing |
| React Hook Form | 7 | Form state management |
| Zod | 4 | Schema validation |
| Axios | 1 | HTTP client |
| Zustand | 5 | Auth state store |
| Lucide React | latest | Icons |
| React Hot Toast | 2 | Toast notifications |

---

## Prerequisites

- **Node.js 18+** (tested on v24)
- **npm 9+**
- **Backend running** at `http://localhost:8080`
  - See [`insurance-claim-system/`](../insurance-claim-system) for setup instructions
  - Backend must have the database seeded with at least one policy

---

## Installation

```bash
cd insurance-frontend
npm install
```

---

## Running the App

### Development server

```bash
npm run dev
```

Opens at **http://localhost:5173** with hot module replacement.

The dev server automatically proxies all `/api/v1` requests to `http://localhost:8080`, so no CORS configuration is needed.

### Production build

```bash
npm run build        # Type-check + bundle → dist/
npm run preview      # Serve dist/ locally
```

### Linting

```bash
npm run lint
```

---

## Environment Configuration

The app reads backend URL from `.env`:

```env
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

> In development, the Vite proxy takes precedence over this value. It is only used as a reference for production builds.

For production, create a `.env.production` file:

```env
VITE_API_BASE_URL=https://your-backend-domain.com/api/v1
```

---

## GitHub Codespaces Setup

1. Start the backend (port **8080**) and frontend (port **5173**).
2. Open the **Ports** panel in VS Code and ensure both ports are forwarded and set to **Public** (or at minimum accessible).
3. Open the forwarded URL for port `5173` to access the frontend — e.g.:
   ```
   https://<codespace-name>-5173.app.github.dev/
   ```
4. The Vite proxy routes `/api/v1` internally to `localhost:8080`, so the backend forwarded URL (e.g. `https://<codespace-name>-8080.app.github.dev/`) is only needed for direct API/Swagger access.

---

## Project Structure

```
src/
├── api/
│   ├── client.ts              # All HTTP calls via Axios
│   └── errors.ts              # ApiError class + HTTP status → message mapping
│
├── components/
│   ├── forms/
│   │   └── ClaimSubmissionForm.tsx   # Full claim submission form
│   ├── layout/
│   │   └── Navbar.tsx                # Top navigation (role-aware)
│   ├── ui/
│   │   ├── Badge.tsx          # Claim status pill badge
│   │   ├── Button.tsx         # Button with variant/size/loading props
│   │   ├── Card.tsx           # White card wrapper
│   │   └── Modal.tsx          # Overlay modal with title/actions
│   └── ProtectedRoute.tsx     # Redirects unauthenticated/wrong-role users
│
├── lib/
│   └── queryClient.ts         # React Query client (stale time, retry config)
│
├── pages/
│   ├── admin/
│   │   ├── AdminPoliciesPage.tsx     # Search policy by number
│   │   ├── ClaimListPage.tsx         # All claims for a policy
│   │   ├── ClaimDetailPage.tsx       # Claim detail + approve/reject
│   │   └── AdminRoutes.tsx           # Nested admin route config
│   ├── Home.tsx               # Role-aware dashboard
│   ├── LoginPage.tsx          # Role selector (no password required)
│   ├── SubmitClaimPage.tsx    # Wraps ClaimSubmissionForm
│   └── TrackClaimPage.tsx     # Look up claim by ID
│
├── stores/
│   └── authStore.ts           # Zustand store: user role + session
│
├── types/
│   └── index.ts               # All shared TypeScript types & const enums
│
├── utils/
│   └── formatters.ts          # formatCurrency, formatDate, formatDateTime
│
├── App.tsx                    # BrowserRouter + route definitions
├── index.css                  # Tailwind CSS v4 entry (@import "tailwindcss")
└── main.tsx                   # ReactDOM.createRoot entry point
```

---

## Routing

### Public

| Path | Component | Notes |
|------|-----------|-------|
| `/login` | `LoginPage` | Always accessible; redirects to `/` if already logged in |

### Customer (requires `CUSTOMER` role)

| Path | Component | Description |
|------|-----------|-------------|
| `/` | `Home` | Dashboard with quick-action links |
| `/submit-claim` | `SubmitClaimPage` | Submit a new claim |
| `/track-claim` | `TrackClaimPage` | Look up a claim by Claim ID |

### Admin (requires `ADMIN` role)

| Path | Component | Description |
|------|-----------|-------------|
| `/` | `Home` | Admin dashboard overview |
| `/admin/policies` | `AdminPoliciesPage` | Search a policy by number |
| `/admin/claims/:policyId` | `ClaimListPage` | All claims under a policy |
| `/admin/claims/:policyId/:claimId` | `ClaimDetailPage` | Full detail + review actions |

Unauthenticated users are redirected to `/login`. Wrong-role access redirects to `/`.

---

## Authentication

> **The backend has no `/auth/login` endpoint.** Authentication is implemented client-side only.

The login page shows two buttons: **Customer** and **Admin**. Clicking one stores the selected role in `localStorage` under `auth_user`. The Zustand auth store (`useAuthStore`) reads this on page load via `loadFromStorage()`.

To log out, click **Logout** in the navbar. This clears `localStorage` and redirects to `/login`.

No token or password is required. This is intentional — the backend API is open and does not enforce authentication.

---

## Backend API Reference

All requests go through `src/api/client.ts`. The base URL is `/api/v1` (proxied to `localhost:8080` in dev).

| Method | Path | Used In | Description |
|--------|------|---------|-------------|
| `GET` | `/policies/{policyNumber}` | Claim form, Admin search | Fetch policy details & coverage limits |
| `POST` | `/claims` | Claim form | Submit a new claim |
| `GET` | `/claims/{claimId}` | Track claim page | Get claim status |
| `GET` | `/claims/{claimId}/history` | Track page, Claim detail | Full status history |
| `GET` | `/claims/policy/{policyId}` | Admin claims list | All claims for a policy |
| `PATCH` | `/claims/{claimId}/review` | Admin claim detail | Approve or reject a claim |

### Error Handling

API errors are wrapped in `ApiError` (from `src/api/errors.ts`):

```ts
class ApiError extends Error {
  status: number;         // HTTP status code
  details?: Record<string, string>;  // Validation field errors
}
```

Key status codes handled:

| Code | Meaning |
|------|---------|
| 400 | Validation error (field details included) |
| 404 | Policy or claim not found |
| 409 | Duplicate claim within 24 hours |
| 500 | Server error |

---

## Claim Submission Flow

1. User types a policy number (`POL-XXXXX` format).
2. After 500ms debounce, the app calls `GET /policies/{policyNumber}`.
3. If the policy is `ACTIVE`, the claim type dropdown is populated from `coverageLimits`.
4. User selects claim type — per-type coverage limit is shown.
5. User fills in incident date (clamped to policy effective date → today), amount, and description.
6. On submit, `POST /claims` is called.
7. Success: modal shows the assigned Claim ID.
8. Error 409: modal shows duplicate claim warning.

---

## Admin Review Flow

1. Admin searches a policy by number on `/admin/policies`.
2. Clicks **View Claims** → navigates to `/admin/claims/:policyId`.
3. Clicks **Review** on any claim → navigates to `/admin/claims/:policyId/:claimId`.
4. Reviews claim details and full history timeline.
5. Clicks **Approve** or **Reject** → modal opens for optional reviewer notes.
6. On confirm, `PATCH /claims/{claimId}/review` is called.
7. React Query cache is updated immediately — no page reload needed.

---

## Key Implementation Notes

### Tailwind CSS v4
This project uses Tailwind CSS **v4** with the `@tailwindcss/vite` plugin (not the legacy PostCSS setup). The CSS entry point is:

```css
/* src/index.css */
@import "tailwindcss";
```

No `tailwind.config.js` is required.

### TypeScript Strict Mode
The `tsconfig.app.json` enables `verbatimModuleSyntax` and `erasableSyntaxOnly`. As a result:
- All type-only imports use `import type { ... }`
- Enums are replaced with `const` objects + union types (see `src/types/index.ts`)
- Constructor parameter properties are not used

### React Query Caching
- **staleTime**: 5 minutes — cached data is reused without refetching
- **gcTime**: 10 minutes — unused cache is kept for 10 minutes
- **retry**: 1 — failed requests retry once
- On claim review, `queryClient.setQueryData` updates the claim immediately and `invalidateQueries` refreshes history

---

## Testing Checklist

- [ ] `npm run build` completes with no TypeScript errors
- [ ] Login page: both Customer and Admin roles work
- [ ] Logout clears session and redirects to login
- [ ] Submit claim: policy validation shows green on valid policy number
- [ ] Submit claim: claim types and coverage limits load from policy
- [ ] Submit claim: amount exceeding limit blocks submission
- [ ] Submit claim: success modal shows Claim ID
- [ ] Submit claim: duplicate submission shows 409 error modal
- [ ] Track claim: valid Claim ID shows status and history timeline
- [ ] Track claim: invalid ID shows error message
- [ ] Admin: policy search returns policy details and coverage table
- [ ] Admin: claims list shows all claims with status badges
- [ ] Admin: approve/reject updates status immediately without reload
- [ ] Responsive layout on mobile (≤768px)
- [ ] React Query cache: navigate away and back — no loading flicker
