# Service Decomposition
## Mini Digital Insurance Claim System

---

| Field | Value |
|---|---|
| **Project** | Mini Digital Insurance Claim System |
| **Document Type** | Service Decomposition |
| **Version** | v1.0 |
| **Date** | 2026-03-11 |
| **Author** | Architect Agent |

---

## Table of Contents

1. [Service Map](#1-service-map)
2. [API Surface](#2-api-surface)
3. [Communication Patterns](#3-communication-patterns)
4. [Data Ownership](#4-data-ownership)
5. [Scalability Model](#5-scalability-model)

---

## 1. Service Map

The system is a **layered Spring Boot monolith** — not a microservices architecture. The "services" below are logical Spring `@Service` beans within a single deployable unit. This is the correct choice for the 3-hour hackathon scope.

### 1.1 Spring Boot Application Modules

| Module / Package | Type | Responsibility |
|---|---|---|
| `controller` | Spring `@RestController` | HTTP entry point — request routing, parameter binding, `@Valid` enforcement, HTTP status codes |
| `service` | Spring `@Service` | All business logic, orchestration, transaction management |
| `repository` | Spring Data `JpaRepository` | Data access — CRUD operations, custom JPQL queries, no SQL boilerplate |
| `domain` | JPA `@Entity` + Enums | Persistence model — table mapping, column constraints, enum types |
| `dto` | Java Records / POJOs | API contract — request/response shapes, decoupled from JPA entities |
| `exception` | `@ControllerAdvice` + custom exceptions | Cross-cutting error handling — maps exceptions to HTTP responses |
| `config` | Spring `@Configuration` | CORS configuration, OpenAPI metadata, bean overrides |

---

### 1.2 Service Bean Responsibilities

#### ClaimService
**Package:** `com.insurance.claims.service`

| Responsibility | Method Signature (indicative) |
|---|---|
| Submit a new claim with full validation | `ClaimResponse submitClaim(ClaimRequest request)` |
| Validate policy exists and is active | `Policy validatePolicy(String policyNumber)` (internal) |
| Validate claim type matches policy | `void validateClaimType(Policy policy, ClaimType type)` (internal) |
| Validate claim amount within coverage limit | `void validateCoverageLimit(Policy policy, BigDecimal amount)` (internal) |
| Detect duplicate claims | `void checkDuplicate(Long policyId, ClaimType type)` (internal) |
| Retrieve all claims for a policy/customer | `List<ClaimResponse> getClaimsByPolicy(String policyNumber)` |
| Retrieve single claim detail | `ClaimResponse getClaimById(Long claimId)` |

#### ReviewService
**Package:** `com.insurance.claims.service`

| Responsibility | Method Signature (indicative) |
|---|---|
| Retrieve all pending/submitted claims | `List<ClaimSummary> getPendingClaims()` |
| Retrieve claim detail (triggers UNDER_REVIEW) | `ClaimDetail getClaimForReview(Long claimId)` |
| Process officer approve/reject decision | `ClaimResponse processReview(Long claimId, ReviewRequest request)` |
| Validate legal status transitions | `void validateTransition(ClaimStatus from, ClaimStatus to)` (internal) |

#### PolicyService
**Package:** `com.insurance.claims.service`

| Responsibility | Method Signature (indicative) |
|---|---|
| Look up policy by policy number | `Policy findByPolicyNumber(String policyNumber)` |
| List all active policies | `List<PolicyResponse> getAllActivePolicies()` |

#### AuditService
**Package:** `com.insurance.claims.service`

| Responsibility | Method Signature (indicative) |
|---|---|
| Record a claim status change event | `void recordEvent(Claim claim, ClaimStatus previous, ClaimStatus next, String changedBy, String notes)` |
| Retrieve full audit trail for a claim | `List<AuditEntry> getAuditTrail(Long claimId)` |

---

### 1.3 Controller Responsibilities

#### ClaimController — `/api/claims`

Serves the **Customer** role. Handles claim submission and self-service tracking.

#### OfficerController — `/api/officer/claims`

Serves the **Claims Officer** role. Handles review queue and decision endpoints.

#### PolicyController — `/api/policies`

Serves both roles. Exposes seeded policy data for form population and lookups.

---

## 2. API Surface

All endpoints produce and consume `application/json`. Base path: `/api`.

### 2.1 Claims Resource — `/api/claims`

| Method | Path | Role | Description | Request Body | Response |
|---|---|---|---|---|---|
| `POST` | `/api/claims` | Customer | Submit a new insurance claim | `ClaimRequest` | `201 ClaimResponse` |
| `GET` | `/api/claims` | Customer | List claims (filter by `?policyNumber=`) | — | `200 ClaimResponse[]` |
| `GET` | `/api/claims/{id}` | Customer | Get claim detail by ID | — | `200 ClaimResponse` |
| `GET` | `/api/claims/{id}/status` | Customer | Get current status of a claim | — | `200 StatusResponse` |
| `GET` | `/api/claims/{id}/audit` | Customer | Get audit trail for a claim | — | `200 AuditEntry[]` |

**ClaimRequest schema:**
```json
{
  "policyNumber": "POL-001234",
  "claimType": "HEALTH",
  "claimAmount": 1500.00,
  "description": "Hospital admission 2026-03-10"
}
```

**ClaimResponse schema:**
```json
{
  "id": 42,
  "policyNumber": "POL-001234",
  "claimType": "HEALTH",
  "claimAmount": 1500.00,
  "description": "Hospital admission 2026-03-10",
  "status": "SUBMITTED",
  "submittedAt": "2026-03-11T10:30:00Z",
  "updatedAt": "2026-03-11T10:30:00Z"
}
```

---

### 2.2 Officer / Review Resource — `/api/officer/claims`

| Method | Path | Role | Description | Request Body | Response |
|---|---|---|---|---|---|
| `GET` | `/api/officer/claims` | Officer | List all claims (filter by `?status=SUBMITTED`) | — | `200 ClaimSummary[]` |
| `GET` | `/api/officer/claims/{id}` | Officer | Get full claim detail (sets status → UNDER_REVIEW) | — | `200 ClaimDetail` |
| `PUT` | `/api/officer/claims/{id}/review` | Officer | Approve or reject a claim | `ReviewRequest` | `200 ClaimResponse` |

**ReviewRequest schema:**
```json
{
  "decision": "APPROVED",
  "notes": "All documents verified, claim is valid."
}
```

```json
{
  "decision": "REJECTED",
  "reason": "Claim type does not match policy coverage."
}
```

---

### 2.3 Policies Resource — `/api/policies`

| Method | Path | Role | Description | Request Body | Response |
|---|---|---|---|---|---|
| `GET` | `/api/policies` | Both | List all active policies | — | `200 PolicyResponse[]` |
| `GET` | `/api/policies/{policyNumber}` | Both | Look up policy by number | — | `200 PolicyResponse` |

**PolicyResponse schema:**
```json
{
  "id": 1,
  "policyNumber": "POL-001234",
  "holderName": "Jane Smith",
  "claimType": "HEALTH",
  "coverageLimit": 10000.00,
  "status": "ACTIVE",
  "startDate": "2025-01-01",
  "endDate": "2027-01-01"
}
```

---

### 2.4 Error Response Schema

All error responses follow a consistent structure:

```json
{
  "timestamp": "2026-03-11T10:35:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Policy POL-999999 not found or is not active",
  "path": "/api/claims"
}
```

| HTTP Status | Scenario |
|---|---|
| `400 Bad Request` | Bean Validation failure (missing/invalid fields) |
| `400 Bad Request` | Policy not found or inactive |
| `400 Bad Request` | Claim type mismatch with policy |
| `404 Not Found` | Claim ID not found |
| `409 Conflict` | Duplicate claim detected |
| `422 Unprocessable Entity` | Claim amount exceeds coverage limit |
| `500 Internal Server Error` | Unexpected server error (message sanitised) |

---

### 2.5 OpenAPI / Swagger

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
- All controller methods annotated with `@Operation`, `@ApiResponse`, `@Parameter` for full documentation

---

## 3. Communication Patterns

### 3.1 Synchronous REST (Primary Pattern)

The hackathon scope uses **synchronous HTTP REST only**. No messaging queues, no event streaming, no WebSockets.

```
Browser (React SPA)
      │
      │  HTTP REST (JSON)
      ▼
Spring Boot API  ──JDBC──►  Database (H2 / PostgreSQL)
```

**Request-Response Cycle:**
1. SPA makes HTTP request (Axios/Fetch)
2. Spring Boot controller receives request, delegates to service layer
3. Service layer executes business logic, interacts with repositories
4. Repositories execute JPA queries against the database
5. Response data mapped to DTOs and returned to SPA
6. SPA renders the response

### 3.2 Internal Communication (Within Spring Boot)

All internal calls are **in-process Java method calls** — no inter-service HTTP, no message bus:

```
Controller → Service → Repository → Database
    ↕                 ↕
  DTOs            Entities (JPA)
```

Transaction boundaries are managed by Spring `@Transactional` on service methods.

### 3.3 Future Communication Patterns (Post-Hackathon)

When the system grows beyond a monolith, the following patterns would be introduced:

| Scenario | Recommended Pattern |
|---|---|
| Claim status notification to customer | Async event (e.g. Kafka/RabbitMQ) → Email/SMS service |
| Policy data from external Policy Management System | REST client (OpenFeign) or async event consumer |
| Audit log sink to external SIEM | Log streaming (Fluentd / Logstash) |
| High-volume claim processing | CQRS — separate read/write models |

---

## 4. Data Ownership

Within the monolith, logical data ownership is assigned per service bean to establish clear boundaries (important for future decomposition into microservices).

### 4.1 Ownership Table

| Data Entity | Owner Service | Can Read | Can Write | Notes |
|---|---|---|---|---|
| `Policy` | `PolicyService` | All services | `PolicyService` only | Seeded at startup; no write endpoint in hackathon scope |
| `Claim` | `ClaimService` | All services | `ClaimService` (create), `ReviewService` (status update) | Dual ownership on status field is intentional — review is a distinct workflow |
| `ClaimAudit` | `AuditService` | All services | `AuditService` only | Immutable log — no updates or deletes ever |

### 4.2 Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         Spring Boot API                         │
│                                                                 │
│  ClaimService                                                   │
│    │ reads ──────────────────────────────► Policy (PolicyRepo)  │
│    │ creates ────────────────────────────► Claim  (ClaimRepo)   │
│    │ delegates ──► AuditService                                 │
│                       │ creates ────────► ClaimAudit (AuditRepo)│
│                                                                 │
│  ReviewService                                                  │
│    │ reads ──────────────────────────────► Claim  (ClaimRepo)   │
│    │ updates status ─────────────────────► Claim  (ClaimRepo)   │
│    │ delegates ──► AuditService                                 │
│                       │ creates ────────► ClaimAudit (AuditRepo)│
└─────────────────────────────────────────────────────────────────┘
```

### 4.3 Database Schema Overview

```sql
-- Policy table (seed data loaded via data.sql)
CREATE TABLE policy (
    id              BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    policy_number   VARCHAR(50)    NOT NULL UNIQUE,
    holder_name     VARCHAR(200)   NOT NULL,
    claim_type      VARCHAR(50)    NOT NULL,  -- enum: HEALTH, AUTO, PROPERTY, LIFE
    coverage_limit  DECIMAL(15,2)  NOT NULL,
    status          VARCHAR(20)    NOT NULL,  -- enum: ACTIVE, INACTIVE
    start_date      DATE           NOT NULL,
    end_date        DATE           NOT NULL
);

-- Claim table
CREATE TABLE claim (
    id              BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    policy_id       BIGINT         NOT NULL REFERENCES policy(id),
    claim_type      VARCHAR(50)    NOT NULL,
    claim_amount    DECIMAL(15,2)  NOT NULL,
    description     TEXT,
    status          VARCHAR(20)    NOT NULL DEFAULT 'SUBMITTED',
    submitted_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP      NOT NULL DEFAULT NOW()
);

-- Claim audit table (append-only)
CREATE TABLE claim_audit (
    id               BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    claim_id         BIGINT         NOT NULL REFERENCES claim(id),
    previous_status  VARCHAR(20),
    new_status       VARCHAR(20)    NOT NULL,
    changed_by       VARCHAR(100),  -- "customer" or "officer"
    notes            TEXT,
    event_timestamp  TIMESTAMP      NOT NULL DEFAULT NOW()
);
```

---

## 5. Scalability Model

### 5.1 Current Model — Single Instance (Hackathon)

The hackathon deployment is a **single-instance monolith** on a single Docker host. This is intentional and appropriate for the use case.

```
[Browser] ──► [Nginx :3000] ──► [Spring Boot :8080] ──► [PostgreSQL :5432]
                (1 instance)       (1 instance)             (1 instance)
```

**Capacity for hackathon demo:**
- Handles concurrent demo users (< 10 simultaneous)
- H2 in-memory DB for dev — no connection pooling constraints
- PostgreSQL with HikariCP default pool (10 connections) — adequate for demo

### 5.2 Bottlenecks Identified

| Component | Bottleneck | Mitigation (future) |
|---|---|---|
| Spring Boot instance | Single JVM — vertical scaling only | Horizontal scaling with load balancer (see §5.3) |
| PostgreSQL | Single instance — no read replicas | Add read replica; route read queries to replica |
| Claim validation queries | Synchronous DB queries on each submission | Cache active policies in application memory (Caffeine/Redis) |
| Audit trail reads | Full table scan for large claim histories | Add index on `claim_audit(claim_id, event_timestamp)` |

### 5.3 Horizontal Scaling Path (Post-Hackathon)

When load requires multiple backend instances:

```
[Browser]
    │
    ▼
[Load Balancer — Nginx / AWS ALB]
    │
    ├──► [Spring Boot Instance 1 :8080]
    ├──► [Spring Boot Instance 2 :8080]
    └──► [Spring Boot Instance 3 :8080]
              │
              ▼
    [PostgreSQL Primary :5432]
              │
              ▼
    [PostgreSQL Read Replica :5432]  ← read-only queries
```

**Requirements for stateless horizontal scaling:**
- No in-memory session state (already satisfied — no auth/session in hackathon)
- Database connection pool per instance (HikariCP — already configured)
- Shared PostgreSQL accessible from all instances (Docker network or managed DB service)

### 5.4 Microservices Decomposition Path (Future Architecture)

If the system requires independent scaling of components, the natural service boundaries are:

| Future Microservice | Extracted From | Data Store |
|---|---|---|
| `policy-service` | `PolicyService` + `PolicyController` | `policy` table (dedicated DB) |
| `claims-service` | `ClaimService` + `ClaimController` | `claim` table (dedicated DB) |
| `review-service` | `ReviewService` + `OfficerController` | Shared `claim` table or event-driven state |
| `audit-service` | `AuditService` | `claim_audit` table (dedicated DB, append-only) |
| `notification-service` | (new) | No database — async event consumer → email/SMS |

**Key insight:** The current monolith package structure (`controller`, `service`, `repository`, `domain`) is deliberately aligned with these future service boundaries. Decomposition requires primarily: extracting packages into separate Spring Boot projects, replacing in-process calls with REST clients (OpenFeign) or event messages (Kafka), and splitting the single database into service-owned schemas.

---

*End of Service Decomposition — Mini Digital Insurance Claim System v1.0*
