# Changelog
## Insurance Claim Submission System

All notable changes to this project are documented in this file.  
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

---

## [Sprint 10] — 2026-05-11 to 2026-05-22 · Synthetic Data Generation Agent

### Added
- **Synthetic Data Agent** (`main.py` + `synthetic-agent/`) — Streamlit Python application that introspects a live PostgreSQL schema, calls an LLM to produce a Faker-based generation plan, generates realistic synthetic rows, and bulk-inserts them into a `synthetic` schema
- `synthetic-agent/Dockerfile` — multi-stage Python 3.12-slim image, non-root user, Streamlit entry point
- `synthetic-agent/requirements.txt` — pinned Python dependencies (Streamlit, Faker, psycopg2-binary, pydantic, pandas, requests)
- `insurance-claim-system/.env.prod` and `.env.test` — environment files for Docker Compose profile selection
- `synthetic-agent` service in `docker-compose.yml` with `profiles: [test]` — only starts when `--profile test` is passed

### Changed
- `insurance-claim-system/docker-compose.yml` — restructured to support env-based profile routing (`APP_ENV=prod` / `APP_ENV=test`); all connection params now driven by env vars with sensible defaults
- `main.py` sidebar — DB connection fields now default to `PG_HOST`, `PG_DATABASE`, `PG_USER`, `PG_PASSWORD`, `SRC_SCHEMA`, and `TGT_SCHEMA` environment variables so Docker Compose can pre-populate them
- API key handling — no longer unconditionally overwrites `AICAFE_API_KEY`; only sets the fallback if the variable is absent

### Docs Updated
- `HLD.md` → **v1.3** (section 3.4 Synthetic Data Agent, updated arch diagram, tech stack, deployment model)
- `LLD.md` → **v1.4** (section 15 Synthetic Data Agent component design, generation strategies, LLM integration, Docker wiring, security notes)
- `01-architecture-diagram.md` → **v1.2** (container diagram + deployment diagram updated; new section 10 Synthetic Agent data flow)
- `02-service-decomposition.md` → **v1.2** (service map updated; section 9 Synthetic Agent service boundary; section 10 scalability renumbered)
- `09-sprint-plan.md` → **v1.3** (Epic 7 added; Sprint 10 tasks + DoD; capacity table and risk register updated)

---

## [Sprint 9] — 2026-04-27 to 2026-05-02 · Production Hardening

### Changed
- Production environment hardening — security headers, connection pool tuning, Dockerfile optimisation
- Final documentation review and sign-off across all SDLC artefacts

### Docs Updated
- All docs reviewed; no version bumps required — Sprint 9 is a hardening-only sprint

---

## [Sprint 8] — 2026-04-13 to 2026-04-24 · Integration & Performance Testing

### Added
- Full integration test suite — `ClaimControllerIT`, `ClaimHistoryControllerIT`, `PolicyControllerIT`
- Load and performance test execution against staging environment
- JaCoCo code coverage reports (unit + integration merged)

### Changed
- Synthetic data extended with high-volume load-test personas and datasets

### Docs Updated
- `08-synthetic-data-plan.md` → **v1.1** (load-test data sets added)
- `09-sprint-plan.md` → **v1.2** (Sprints 1–7 marked complete; Sprint 8–9 scope confirmed)

---

## [Sprint 7] — 2026-03-30 to 2026-04-09 · Audit Trail & Error Handling

### Added
- **US-11** — Admin can view full audit trail of claim status changes
- `ClaimHistoryService` audit query implementation
- `GlobalExceptionHandler` edge-case coverage (malformed JSON, constraint violations)
- `ClaimHistoryControllerIT` integration tests

### Docs Updated
- `07-gherkin-scenarios.md` → **v1.3** (US-11 scenarios + expanded edge-case tags)
- `LLD.md` → **v1.3** (audit trail service design, exception handler detail)
- `06-user-stories.md` → **v1.2** (DoD checklist updates, Sprint 7 velocity notes)

---

## [Sprint 6] — 2026-03-16 to 2026-03-27 · Admin Review & Adjudication

### Added
- **US-08** — Admin can view full claim detail (`GET /api/claims/{id}`)
- **US-09** — Admin can approve a claim (`PATCH /api/claims/{id}/approve` → SUBMITTED → APPROVED)
- **US-10** — Admin can reject a claim with reason (`PATCH /api/claims/{id}/reject` → SUBMITTED → REJECTED)
- `ClaimDetail` React page for admin with approve/reject action buttons
- Status badge rendering for all claim states

### Docs Updated
- `HLD.md` → **v1.2** (admin adjudication flow and audit trail section added)

---

## [Sprint 5] — 2026-03-02 to 2026-03-13 · Claim History

### Added
- **US-06** — Customer can view claim history timeline (`GET /api/claims/{id}/history`)
- **US-07** — Admin can list all claims for a policy (`GET /api/claims?policyNumber=POL-XXXXX`)
- `ClaimHistory` JPA entity and `claim_history` database table
- `ClaimHistoryPage` React component with chronological timeline

### Docs Updated
- `03-er-diagram.md` → **v1.2** (`claim_history` entity added)
- `04-database-model.md` → **v1.2** (`claim_history` DDL + FK constraint added)
- `LLD.md` → **v1.2** (`ClaimHistoryService` and timeline query design)
- `02-service-decomposition.md` → **v1.1** (`ClaimHistoryService` service boundary)

---

## [Sprint 4] — 2026-02-16 to 2026-02-27 · Duplicate Prevention & Claim Tracking

### Added
- **US-04** — Duplicate claim prevention (same policy, type, incident date within 24 h)
- **US-05** — Customer can track claim status by claim ID (`GET /api/claims/{id}`)
- `DuplicateClaimValidator` custom Jakarta constraint + validator
- `ClaimStatusPage` React component showing current status and last updated time
- Seed: synthetic data plan baseline created

### Changed
- US-04 acceptance criteria refined after Sprint 3 retrospective (edge: same date, different time)

### Docs Updated
- `07-gherkin-scenarios.md` → **v1.1** (US-04 duplicate prevention scenarios)
- `06-user-stories.md` → **v1.1** (US-04 AC tightened post-retro)
- `08-synthetic-data-plan.md` → **v1.0** (initial plan created this sprint)

---

## [Sprint 3] — 2026-02-02 to 2026-02-13 · Claim Submission

### Added
- **US-02** — Customer can submit an insurance claim
  - `Claim` JPA entity, `ClaimService`, `ClaimController`
  - `POST /api/claims` with full policy validation, coverage limit enforcement, date validation
  - `ClaimForm` React component with Zod schema validation
  - Success confirmation with returned `claimId`
- `ClaimRepository` with duplicate-check query
- `CoverageExceededException`, `PolicyNotActiveException` — custom domain exceptions

### Docs Updated
- `HLD.md` → **v1.1** (claim submission business flows and validation rules)
- `LLD.md` → **v1.1** (`ClaimService` design, DTO definitions, validator contracts)
- `03-er-diagram.md` → **v1.1** (`claims` entity + policy FK relationship)
- `04-database-model.md` → **v1.1** (`claims` table DDL + composite duplicate-check index)
- `01-architecture-diagram.md` → **v1.1** (component diagram updated for claim components)

---

## [Sprint 2] — 2026-01-19 to 2026-01-30 · Policy Management

### Added
- **US-01** — Admin/Customer can look up an insurance policy by policy number
- **US-03** — Real-time policy verification within the claim submission form
- `Policy` and `PolicyCoverage` JPA entities
- `GET /api/policies/{policyNumber}` REST endpoint
- `PolicyLookup` React component with TanStack Query integration
- Zod validation on `POL-XXXXX` policy number format before API call
- Per-type coverage grid with active/inactive status badges
- `PolicyControllerIT` integration test class

---

## [Sprint 1] — 2026-01-05 to 2026-01-16 · Foundation & Authentication

### Added
- Maven project structure with Spring Boot 3.2 and Java 17
- PostgreSQL Docker Compose setup (`docker-compose.yml`)
- `schema.sql` — all 4 tables defined: `policies`, `policy_coverages`, `claims`, `claim_history`
- Vite + React + TypeScript frontend project
- **US-12** — Role-based login: CUSTOMER / ADMIN role selector on `LoginPage`
- **US-13** — Protected route navigation: `ProtectedRoute` and `AdminRoute` components
- Zustand auth store with `persist` middleware (session survives page refresh)
- GitHub Actions CI pipeline (build + unit test on every push)

### Docs Baselined (v1.0)
- `HLD.md` — system objectives, architecture overview, actor definitions
- `LLD.md` — package structure, entity design, base service contracts
- `01-architecture-diagram.md` — system context and container diagrams
- `02-service-decomposition.md` — PolicyService and Auth/Navigation service map
- `03-er-diagram.md` — `policies` and `policy_coverages` ER diagram
- `04-database-model.md` — `policies` and `policy_coverages` DDL
- `05-openapi.yaml` — OpenAPI spec for policy endpoints
- `06-user-stories.md` — all 13 user stories across 6 epics
- `07-gherkin-scenarios.md` — initial Gherkin scenarios for US-01, US-02, US-12, US-13
- `09-sprint-plan.md` — 9 sprints mapped with story point estimates
