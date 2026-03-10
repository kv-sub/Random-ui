# Insurance Claims Frontend

A React + TypeScript frontend for the Insurance Claim Submission System. Customers can submit and track claims; admins can review and approve/reject them.

---

## Tech Stack

| Tool | Version | Purpose |
|------|---------|---------|
| React | 19 | UI framework |
| TypeScript | 5.9 | Type safety |
| Vite | 7 | Build tool & dev server |
| Tailwind CSS | 4 | Styling |
| TanStack React Query | 5 | Server state & caching |
| React Router DOM | 7 | Client-side routing |
| React Hook Form | 7 | Form state management |
| Zod | 4 | Schema validation |
| Axios | 1 | HTTP client |
| Zustand | 5 | Auth state store |
| Lucide React | — | Icons |
| React Hot Toast | — | Notifications |

---

## Prerequisites

- **Node.js 18+** (tested on v24)
- **npm 9+**
- **Backend running** at `http://localhost:8080` — see [`insurance-claim-system/`](../insurance-claim-system)

---

## Getting Started

### 1. Install dependencies

```bash
cd insurance-frontend
npm install
```

### 2. Configure environment

The project ships with a `.env` file pointing to the backend:

```
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

> **GitHub Codespaces**: The Vite dev server proxies `/api/v1` → `http://localhost:8080` automatically, so no changes are needed. Access the app via the forwarded port URL for port `5173`.

### 3. Start the development server

```bash
npm run dev
```

App runs at **http://localhost:5173**

---

## Available Scripts

| Command | Description |
|---------|-------------|
| `npm run dev` | Start development server with HMR |
| `npm run build` | Type-check and build for production (`dist/`) |
| `npm run preview` | Serve the production build locally |
| `npm run lint` | Run ESLint |

---

## Project Structure

```
insurance-frontend/
├── public/                  # Static assets
├── src/
│   ├── api/
│   │   ├── client.ts        # Axios API client (all backend calls)
│   │   └── errors.ts        # ApiError class & HTTP error mapping
│   ├── components/
│   │   ├── forms/
│   │   │   └── ClaimSubmissionForm.tsx  # Claim submission with policy validation
│   │   ├── layout/
│   │   │   └── Navbar.tsx   # Top navigation bar
│   │   ├── ui/
│   │   │   ├── Badge.tsx    # Claim status badge
│   │   │   ├── Button.tsx   # Reusable button with variants
│   │   │   ├── Card.tsx     # Card container
│   │   │   └── Modal.tsx    # Modal dialog
│   │   └── ProtectedRoute.tsx  # Role-based route guard
│   ├── lib/
│   │   └── queryClient.ts   # React Query client configuration
│   ├── pages/
│   │   ├── admin/
│   │   │   ├── AdminPoliciesPage.tsx  # Search policies by number
│   │   │   ├── ClaimListPage.tsx      # All claims for a policy
│   │   │   ├── ClaimDetailPage.tsx    # Claim detail + review actions
│   │   │   └── AdminRoutes.tsx        # Admin route definitions
│   │   ├── Home.tsx            # Dashboard / landing page
│   │   ├── LoginPage.tsx       # Role selector login page
│   │   ├── SubmitClaimPage.tsx # Claim submission page
│   │   └── TrackClaimPage.tsx  # Track claim by ID
│   ├── stores/
│   │   └── authStore.ts     # Zustand auth store (role, session)
│   ├── types/
│   │   └── index.ts         # All TypeScript types & const enums
│   ├── utils/
│   │   └── formatters.ts    # Currency, date, datetime formatters
│   ├── App.tsx              # Root component with routing
│   ├── index.css            # Tailwind CSS entry point
│   └── main.tsx             # React entry point
├── .env                     # API base URL configuration
├── vite.config.ts           # Vite config with API proxy
└── tsconfig.app.json        # TypeScript config
```

---

## Application Routes

### Customer Routes

| Path | Page | Description |
|------|------|-------------|
| `/login` | LoginPage | Role selector — pick Customer or Admin |
| `/` | Home | Dashboard with feature overview |
| `/submit-claim` | SubmitClaimPage | Submit a new insurance claim |
| `/track-claim` | TrackClaimPage | Track claim status by Claim ID |

### Admin Routes

| Path | Page | Description |
|------|------|-------------|
| `/admin/policies` | AdminPoliciesPage | Search policy by number |
| `/admin/claims/:policyId` | ClaimListPage | All claims for a policy |
| `/admin/claims/:policyId/:claimId` | ClaimDetailPage | Claim detail + approve/reject |

All routes except `/login` are protected and redirect to `/login` if unauthenticated. Customer-only and admin-only routes redirect to `/` if the wrong role accesses them.

---

## Authentication

> **No backend auth endpoint exists.** Authentication is handled client-side via a role selector on the login page.

On the login page, click **Customer** or **Admin** to enter the portal. The selected role is persisted in `localStorage` so the session survives page refreshes.

To switch roles, click **Logout** in the navbar and select a different role.

---

## Backend API Integration

The frontend communicates with the Spring Boot backend at `/api/v1`. All calls go through `src/api/client.ts`.

### Endpoints Used

| Method | Endpoint | Used By |
|--------|----------|---------|
| `GET` | `/api/v1/policies/{policyNumber}` | Policy verification on claim form; admin policy search |
| `POST` | `/api/v1/claims` | Submit new claim |
| `GET` | `/api/v1/claims/{claimId}` | Track claim status |
| `GET` | `/api/v1/claims/{claimId}/history` | Claim status history timeline |
| `GET` | `/api/v1/claims/policy/{policyId}` | List all claims for a policy (admin) |
| `PATCH` | `/api/v1/claims/{claimId}/review` | Approve or reject a claim (admin) |

### API Proxy (Development)

The Vite dev server proxies all `/api/v1` requests to `http://localhost:8080`:

```ts
// vite.config.ts
server: {
  proxy: {
    '/api/v1': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    }
  }
}
```

This means the frontend uses relative URLs (`/api/v1/...`) and works in GitHub Codespaces without CORS issues.

---

## Key Features

### Claim Submission (Customer)
- Real-time policy number validation with 500ms debounce
- Claim type dropdown populated from the policy's `coverageLimits` map
- Per-type coverage limit shown; amount validated against it
- Incident date restricted to policy effective date → today
- Zod schema validation on all fields
- Success modal with the assigned Claim ID
- Error modal for API errors including 409 duplicate detection

### Claim Tracking (Customer)
- Search by numeric Claim ID
- Shows full claim details and current status badge
- Visual timeline of all status history entries with reviewer notes

### Admin Dashboard
- Search policies by policy number (e.g. `POL-AB123`)
- View per-type coverage breakdown
- List all claims for a policy with status badges
- Approve / Reject via modal with optional reviewer notes
- React Query cache updated immediately on review — no full page reload needed

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `VITE_API_BASE_URL` | `http://localhost:8080/api/v1` | Backend base URL (reference only in dev; Vite proxy is used instead) |

---

## GitHub Codespaces Notes

1. Ensure **port 8080** (backend) and **port 5173** (frontend) are both forwarded in the **Ports** panel.
2. The Vite proxy routes `/api/v1` to `localhost:8080` internally — the frontend URL stays on the codespace domain with no CORS issues.
3. Access the frontend at the forwarded URL for port `5173` (e.g. `https://<codespace-name>-5173.app.github.dev/`).
4. The backend forwarded URL (e.g. `https://<codespace-name>-8080.app.github.dev/`) is for API/Swagger access only; the frontend uses the internal proxy.

---

## Building for Production

```bash
npm run build
```

Output is placed in `dist/`. Serve it with any static file server or configure the backend to serve it directly.

```bash
# Preview the production build locally
npm run preview
```

> For production deployments, set `VITE_API_BASE_URL` to the actual backend URL and configure CORS on the backend to allow your frontend's origin.

