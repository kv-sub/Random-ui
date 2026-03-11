# High Level Design (HLD)
## Insurance Claim Submission System

**Version:** 1.3  
**Status:** Final  
**Authors:** Engineering Team  
**Last Updated:** May 2026

---

## Document History

| Version | Date       | Changes                                                                        |
|---------|------------|--------------------------------------------------------------------------------|
| 1.0     | 2026-01-05 | Initial HLD — system objectives, architecture overview, actor definitions       |
| 1.1     | 2026-02-02 | Added claim submission business flows and server-side validation rules (Sprint 3) |
| 1.2     | 2026-03-16 | Added admin adjudication flow and audit trail section (Sprint 6–7)             |
| 1.3     | 2026-05-11 | Added Synthetic Data Generation Agent service and env-based Docker Compose routing (Sprint 10) |

---

## Table of Contents
1. [Use Case Summary](#1-use-case-summary)
2. [System Objectives](#2-system-objectives)
3. [System Services & Boundaries](#3-system-services--boundaries)
4. [Architecture Overview](#4-architecture-overview)
5. [Key Architectural Decisions](#5-key-architectural-decisions)
6. [User Roles & Personas](#6-user-roles--personas)
7. [Core Business Flows](#7-core-business-flows)
8. [Technology Stack](#8-technology-stack)
9. [Non-Functional Requirements](#9-non-functional-requirements)
10. [Security Considerations](#10-security-considerations)
11. [Deployment Model](#11-deployment-model)
12. [Assumptions & Constraints](#12-assumptions--constraints)

---

## 1. Use Case Summary

The **Insurance Claim Submission System** is a web application that enables insurance customers to submit, track, and manage their insurance claims, while allowing internal reviewers (admins) to process, approve, or reject those claims.

### Primary Actors

| Actor | Description |
|---|---|
| **Customer** | Policyholder who submits and tracks claims |
| **Admin / Reviewer** | Insurance company employee who reviews and adjudicates claims |
| **System** | Automated validations, duplicate detection, audit trail recording |

### Core Business Scenarios

1. A customer with an active insurance policy submits a claim for a covered incident (e.g., medical, auto, home damage).
2. The system validates the claim against the policy (active status, coverage type, amount limits, duplicate check).
3. The claim is placed in `SUBMITTED` state and enters the review queue.
4. An admin reviewer looks up the policy, finds the pending claims, reviews the details and history, and approves or rejects the claim.
5. The customer tracks the claim status and sees the audit trail of all status changes.

---

## 2. System Objectives

| Objective | Description |
|---|---|
| **Claims Digitisation** | Replace manual paper-based claim submission with a fully digital, validated workflow |
| **Policy Enforcement** | Automate enforcement of policy terms — coverage types, amount limits, validity dates |
| **Duplicate Prevention** | Prevent fraudulent or accidental duplicate claim submissions within a 24-hour window |
| **Auditability** | Maintain a complete, immutable history of every claim status change with timestamps and reviewer notes |
| **Self-Service Tracking** | Allow customers to self-serve track claim status and history without calling support |
| **Admin Efficiency** | Provide admins a structured review interface to approve/reject claims with documented rationale |

---

## 3. System Services & Boundaries

The system is composed of two primary services:

### 3.1 Backend API Service (`insurance-claim-system`)

A single Spring Boot REST API server. It is responsible for:

- **Policy Service** — Lookup and validation of insurance policies and per-type coverage limits
- **Claim Service** — Business logic for claim submission, validation, status management, and retrieval
- **Claim History Service** — Audit trail recording and retrieval for every claim state transition
- **Validation Layer** — Custom Bean Validation annotations (`@ValidPolicyNumber`, `@ValidIncidentDate`)
- **Exception Handling** — Global REST exception handler mapping all domain errors to structured HTTP responses
- **API Documentation** — SpringDoc OpenAPI 3.0 (Swagger UI at `/swagger-ui.html`)

### 3.2 Frontend Application (`insurance-frontend`)

A React/TypeScript single-page application (SPA). It is responsible for:

- **Role-Based UI Routing** — Separate navigation paths for CUSTOMER and ADMIN roles
- **Claim Submission Form** — Multi-step validated form with real-time policy verification
- **Claim Tracking Page** — Search by claim ID, status display, history timeline
- **Admin Portal** — Policy search, claim list per policy, claim review modal with approve/reject
- **API Integration** — Axios-based HTTP client consuming all backend endpoints

### 3.3 Database (`PostgreSQL`)

A relational database (PostgreSQL 16) holding all persistent state:

- `policies` — Active insurance policies
- `policy_coverages` — Per-claim-type coverage limits linked to a policy
- `claims` — Submitted insurance claims
- `claim_history` — Immutable audit log of all claim status transitions
- `synthetic.*` — Mirror tables in the `synthetic` schema populated by the data agent (test/dev only)

### 3.4 Synthetic Data Generation Agent (`synthetic-agent`)

A Python/Streamlit single-page application that generates realistic test data directly from a live PostgreSQL schema. It is deployed as a Docker container and is **only active in the `test` profile** of Docker Compose.

Responsibilities:
- **Schema Discovery** — Introspect source schema via `information_schema` to build a full column-type map
- **LLM Spec Generation** — Send the schema to a configurable LLM endpoint (default: gpt-4.1 via AICafe) to receive a JSON generation plan with per-column strategies
- **Faker-based Row Generation** — Produce synthetic rows using [Faker](https://faker.readthedocs.io/) for text, numeric ranges, dates, UUIDs, booleans, and JSON columns
- **PostgreSQL Bulk Load** — Create a target schema (`synthetic`) and bulk-insert generated rows using `psycopg2.extras.execute_values`
- **Demo Mode** — Full workflow preview without any external DB or LLM connection; safe for UI demonstrations

---

## 4. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        User's Browser                               │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                 React SPA (Vite + TypeScript)                │   │
│  │  Customer: Submit Claim | Track Claim                        │   │
│  │  Admin:    Policy Search | Claims List | Claim Review        │   │
│  └─────────────────────┬────────────────────────────────────────┘   │
└────────────────────────│────────────────────────────────────────────┘
                         │ HTTP/REST  (JSON, /api/v1/*)
                         ▼
┌─────────────────────────────────────────────────────────────────────┐
│              Spring Boot REST API (Port 8080)                       │
│                                                                     │
│  ┌──────────────┐  ┌──────────────────┐  ┌───────────────────────┐ │
│  │PolicyController│ │ ClaimController  │  │ClaimHistoryController │ │
│  └──────┬───────┘  └────────┬─────────┘  └──────────┬────────────┘ │
│         │                   │                        │              │
│  ┌──────▼───────┐  ┌────────▼─────────┐  ┌──────────▼────────────┐ │
│  │PolicyService │  │  ClaimService    │  │  ClaimHistoryService  │ │
│  └──────┬───────┘  └────────┬─────────┘  └──────────┬────────────┘ │
│         │                   │                        │              │
│  ┌──────▼──────────────────▼────────────────────────▼────────────┐ │
│  │            JPA Repositories (Hibernate ORM)                   │ │
│  └──────────────────────────────┬─────────────────────────────── ┘ │
└─────────────────────────────────│───────────────────────────────────┘
                                  │ JDBC
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                  PostgreSQL 16 Database                             │
│                                                                     │
│   policies  ──< policy_coverages                                    │
│   policies  ──< claims ──< claim_history                            │
└─────────────────────────────────────────────────────────────────────┘
```

### Deployment (Docker Compose)

```
┌────────────────────────────────────────────────────────────┐
│              Docker Host  (test profile)               │
│                                                         │
│  ┌─────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │  app             │  │  db               │  │ synthetic    │  │
│  │  (Spring Boot)   ◄─│  (PostgreSQL 16)  │◄─│ agent        │  │
│  │  port 8080       │  │  port 5432       │  │ (Streamlit)  │  │
│  │                 │  │                 │  │ port 8501    │  │
│  └─────────────────┘  └──────────────────┘  └──────────────┘  │
│                                                         │
│  synthetic-agent is EXCLUDED in the prod profile         │
└────────────────────────────────────────────────────────────┘
```

---

## 5. Key Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| **Architecture style** | Monolithic REST API (single deployment unit) | System complexity and team size do not justify microservices; monolith is simpler to develop, test, and deploy |
| **Database** | PostgreSQL | ACID compliance required for financial claim amounts; relational model fits the entity relationships well |
| **ORM** | Spring Data JPA / Hibernate | Standard Spring Boot stack; derived queries and JPQL reduce boilerplate; `JOIN FETCH` for N+1 prevention |
| **Frontend framework** | React 19 + TypeScript | Industry-standard; strong type-safety with TypeScript; rich ecosystem |
| **Server state management** | TanStack Query (React Query) | Handles caching, re-fetching, loading/error states without Redux boilerplate |
| **Client state management** | Zustand | Lightweight; sufficient for simple auth state |
| **Form handling** | React Hook Form + Zod | Performant uncontrolled forms; Zod provides compile-time-safe schema validation |
| **API documentation** | SpringDoc OpenAPI 3.0 | Auto-generated from annotations; Swagger UI served directly from the app |
| **Duplicate detection** | Application-layer query (24-hour window) | Prevents fraud; 24-hour window balances strictness with operational flexibility |
| **Audit trail** | Dedicated `claim_history` table | Immutable append-only records enable full auditability without soft-delete complexity |
| **Error handling** | `@RestControllerAdvice` global handler | Centralised, consistent error response shape across all endpoints |
| **Build tool** | Maven (backend), Vite (frontend) | Maven is standard Spring Boot; Vite provides fast HMR and optimal builds for React |

---

## 6. User Roles & Personas

### Role: CUSTOMER

**Persona:** Alex Chen, 34, policyholder  
**Goals:** Submit claims quickly, check if they were approved, understand any rejections  
**Access:**
- Submit new claims (`/submit-claim`)
- Track existing claims by ID (`/track-claim`)

### Role: ADMIN / REVIEWER

**Persona:** Sarah Mitchell, 42, claims adjudicator  
**Goals:** Review pending claims efficiently, approve legitimate claims, reject fraudulent/ineligible claims with documented rationale  
**Access:**
- Search policies by policy number (`/admin/policies`)
- View all claims under a policy (`/admin/claims/:policyId`)
- View full claim detail and history (`/admin/claims/:policyId/:claimId`)
- Approve or reject a claim with reviewer notes

---

## 7. Core Business Flows

### 7.1 Claim Submission Flow

```
Customer enters policy number
        │
        ▼
System verifies policy (GET /api/v1/policies/{policyNumber})
  ├── Policy not found → show error
  ├── Policy not ACTIVE → block form
  └── Policy found + ACTIVE → show coverage types + limits
        │
        ▼
Customer fills: claim type, amount, incident date, description
        │
        ▼
Client-side validation (Zod schema)
  ├── Invalid policy number format → field error
  ├── Future incident date → field error
  └── Amount > coverage limit → field error
        │
        ▼
POST /api/v1/claims
        │
Server-side validation (Bean Validation + Business Rules)
  ├── Policy not found → 404
  ├── Policy not ACTIVE → 400 PolicyInactiveException
  ├── Today outside policy dates → 400
  ├── Claim type not covered → 400 InvalidClaimTypeException
  ├── Amount > coverage limit → 400 CoverageExceededException
  ├── Duplicate in 24h window → 409 DuplicateClaimException
  └── All checks pass → persist Claim (status=SUBMITTED)
                       + persist ClaimHistory (initial record)
                       → 201 ClaimResponse
```

### 7.2 Claim Review Flow

```
Admin searches for policy number
        │
        ▼
GET /api/v1/policies/{policyNumber}
        │
        ▼
Admin clicks "View Claims"
        │
        ▼
GET /api/v1/claims/policy/{policyId}
        │
        ▼
Admin selects a claim → GET /api/v1/claims/{claimId}
                        + GET /api/v1/claims/{claimId}/history
        │
        ▼
Admin clicks "Approve" or "Reject"
        │
        ▼
PATCH /api/v1/claims/{claimId}/review
  { action: "APPROVE" | "REJECT", reviewerNotes: "..." }
        │
Server validates claim exists, updates status, appends ClaimHistory
        │
        ▼
200 ClaimResponse (status updated)
```

### 7.3 Claim Tracking Flow

```
Customer enters Claim ID
        │
        ▼
GET /api/v1/claims/{claimId}         GET /api/v1/claims/{claimId}/history
        │                                        │
        ▼                                        ▼
Claim details card              Timeline of status changes (newest first)
(status badge, amounts,         with timestamps + reviewer notes
 dates, description)
```

---

## 8. Technology Stack

### Backend

| Component | Technology | Version |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 3.x |
| ORM | Spring Data JPA / Hibernate | 3.x |
| Database driver | PostgreSQL JDBC | 42.x |
| Validation | Jakarta Bean Validation (Hibernate Validator) | 3.x |
| API Docs | SpringDoc OpenAPI (Swagger UI) | 2.x |
| Build | Maven | 3.9+ |
| Lombok | Lombok | 1.18.x |
| Container | Docker / Docker Compose | — |

### Frontend

| Component | Technology | Version |
|---|---|---|
| Language | TypeScript | 5.9 |
| Framework | React | 19 |
| Routing | React Router DOM | 7 |
| Server state | TanStack Query (React Query) | 5 |
| Client state | Zustand | 5 |
| HTTP client | Axios | 1.x |
| Forms | React Hook Form + Zod | 7 + 4 |
| Styling | Tailwind CSS | 4 |
| Icons | Lucide React | — |
| Notifications | React Hot Toast | 2 |
| Build | Vite | 7 |
| Testing | Vitest + Testing Library | 3 |

### Database

| Component | Technology | Version |
|---|---|---|
| Engine | PostgreSQL | 16 |
| Schema management | Spring Boot DDL auto / `schema.sql` | — |

### Synthetic Data Agent

| Component | Technology | Version |
|---|---|---|
| Language | Python | 3.12 |
| UI framework | Streamlit | ≥ 1.35 |
| Data generation | Faker | ≥ 24.0 |
| LLM integration | OpenAI-compatible REST API (AICafe) | gpt-4.1 |
| DB connector | psycopg2-binary | ≥ 2.9 |
| Data handling | pandas, pydantic | 2.x |
| Containerisation | Docker (multi-stage, non-root) | — |

---

## 9. Non-Functional Requirements

| Category | Requirement | Target |
|---|---|---|
| **Performance** | API response time (p95) | < 500ms for all read endpoints |
| **Performance** | Claim submission response time | < 1000ms |
| **Availability** | System uptime | 99.5% |
| **Scalability** | Horizontal scaling | Backend is stateless — can be scaled behind a load balancer |
| **Reliability** | Idempotency | Duplicate detection prevents double-processing within 24h window |
| **Auditability** | Claim history retention | All transitions permanently retained, no deletion |
| **Data integrity** | Financial precision | `NUMERIC(15,2)` for all monetary values; no floating point |
| **Usability** | Policy verification speed | Debounce 500ms; immediate feedback with inline coverage display |
| **Testability** | Backend unit test coverage | > 80% line coverage (JaCoCo) |
| **Testability** | Backend integration test coverage | Full controller layer integration tests |
| **Security** | Role enforcement | Frontend routing guards + server-side role validation |
| **Observability** | Error logging | All exceptions logged; structured error responses with timestamp |

---

## 10. Security Considerations

| Area | Current Implementation | Notes |
|---|---|---|
| **Authentication** | Role-selection UI (demo) — no password/JWT | Production requires Auth provider (OAuth2/Keycloak) |
| **Authorisation** | Frontend route guards (ProtectedRoute) | Production requires server-side JWT claim validation |
| **Input validation** | Bean Validation (server) + Zod (client) | Two layers prevent injection and malformed data |
| **SQL Injection** | JPA/JPQL parameterised queries only | No raw SQL; all queries use bound parameters |
| **XSS** | React's JSX escaping (default) | All user content rendered via React; no `dangerouslySetInnerHTML` |
| **Duplicate fraud** | 24-hour duplicate claim detection | Application-layer guard; supplements manual review |
| **CORS** | Spring Boot default — needs configuration for production | Add explicit `@CrossOrigin` or `WebMvcConfigurer` CORS config |
| **Financial data** | `NUMERIC(15,2)` — no floating point | Prevents rounding/precision bugs in monetary handling |

---

## 11. Deployment Model

### Development

```bash
# Backend
cd insurance-claim-system
mvn spring-boot:run

# Frontend
cd insurance-frontend
npm run dev

# Database
docker-compose up db
```

### Docker (Production-like)

```bash
# Production (app + db only)
cd insurance-claim-system
docker compose --env-file .env.prod up --build

# Test / Dev (app + db + synthetic data agent)
docker compose --env-file .env.test --profile test up --build
```

The `docker-compose.yml` defines:
- `db` service: PostgreSQL 16, port 5432 — always started
- `app` service: Spring Boot JAR, port 8080, depends on `db` — always started
- `synthetic-agent` service: Streamlit Python app, port 8501 — **`test` profile only**

Environment is controlled via `.env.prod` or `.env.test` files; `APP_ENV` drives the Spring profile.

### Ports

| Service | Port |
|---|---|
| Spring Boot API | 8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | 5432 |
| Vite Dev Server | 5173 (proxies `/api` to 8080) |
| Synthetic Data Agent (test only) | 8501 |

---

## 12. Assumptions & Constraints

| # | Assumption / Constraint |
|---|---|
| 1 | Policies are pre-loaded into the database by an upstream policy management system (not in scope) |
| 2 | There is no customer registration or self-service policy creation in this system |
| 3 | The authentication mechanism is a demo implementation — production deployment must integrate with an enterprise IdP |
| 4 | The 24-hour duplicate detection window is configurable at the application level but currently hardcoded |
| 5 | A claim can only be reviewed (approved/rejected) when its status is `SUBMITTED` or `IN_REVIEW` |
| 6 | Coverage limits are enforced at the individual `ClaimType` level (per-type limits), not only at the aggregate `coverageLimit` |
| 7 | All monetary values are stored and processed in USD |
| 8 | Incident dates must not be in the future; historical dates are accepted without a lookback limit |
| 9 | The system is a single-region deployment — no multi-region HA requirement in current scope |
| 10 | Policy number format is fixed: `POL-` followed by exactly 5 uppercase alphanumeric characters (e.g., `POL-AB123`) |
| 11 | The Synthetic Data Agent is a developer/test-time tool only; it must never be deployed or reachable in production |
| 12 | The `synthetic` schema created by the agent is isolated from the `public` schema used by the live application |
| 13 | The agent's LLM call falls back to a local generation plan if the LLM endpoint is unavailable or SSL validation fails |
