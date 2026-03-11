# Service Decomposition
## Insurance Claim Submission System

**Version:** 1.2  
**Date:** May 2026

---

## Document History

| Version | Date       | Changes                                                                                |
|---------|------------|----------------------------------------------------------------------------------------|
| 1.0     | 2026-01-05 | Initial service map — PolicyService and Auth/Navigation boundary defined (Sprint 1)    |
| 1.1     | 2026-03-02 | Added `ClaimHistoryService` decomposition and audit trail service boundary (Sprint 5)   |
| 1.2     | 2026-05-11 | Added Synthetic Data Agent service boundary and Docker profile isolation (Sprint 10)     |

---

## Overview

The system is built as a **modular monolith** — a single deployable Spring Boot application internally divided into clearly separated service modules, each owning a distinct business domain. The frontend is a separate SPA communicating exclusively over REST. The **Synthetic Data Agent** is a standalone Python/Streamlit tool that runs in isolation in the `test` Docker Compose profile, touching only a dedicated `synthetic` schema.

---

## 1. Service Map

```mermaid
graph LR
    subgraph Frontend["React SPA (Browser)"]
        UI_C["Customer UI\nSubmit Claim\nTrack Claim"]
        UI_A["Admin UI\nPolicy Lookup\nClaim Review"]
    end

    subgraph Backend["Spring Boot API"]
        direction TB
        POLICY["Policy Service\nLookup + validate\npolicies & coverages"]
        CLAIM["Claim Service\nSubmit · Get · Review\nList by policy"]
        HISTORY["Claim History Service\nAudit trail retrieval"]
        VALIDATION["Validation Layer\n@ValidPolicyNumber\n@ValidIncidentDate\n@RestControllerAdvice"]
    end

    subgraph DB["PostgreSQL 16"]
        T1[("policies")]
        T2[("policy_coverages")]
        T3[("claims")]
        T4[("claim_history")]
        T5[("synthetic.*\n(agent target)")]
    end

    subgraph Agent["Synthetic Data Agent (test profile only)"]
        SA["Streamlit Python App\nPort 8501\nDiscover → LLM Spec → Faker → Load"]
    end

    UI_C -->|REST| POLICY
    UI_C -->|REST| CLAIM
    UI_C -->|REST| HISTORY
    UI_A -->|REST| POLICY
    UI_A -->|REST| CLAIM
    UI_A -->|REST| HISTORY

    CLAIM -.->|reads via repo| POLICY
    POLICY --> T1
    POLICY --> T2
    CLAIM --> T3
    CLAIM --> T4
    HISTORY --> T3
    HISTORY --> T4

    SA -->|reads public schema| T1
    SA -->|reads public schema| T3
    SA -->|writes synthetic schema| T5
```

---

## 2. Service Responsibility Matrix

| Service | Owns | Does NOT own |
|---|---|---|
| **Policy Service** | Policy lookup, coverage map building, policy status validity | Claim creation, claim status management |
| **Claim Service** | Claim lifecycle (submit → review → terminal), 7-step validation chain, duplicate detection | Policy data management, history presentation |
| **Claim History Service** | Audit trail retrieval | Writing history records (done by Claim Service) |
| **Validation Layer** | Input format/type validation (Bean Validation), global error mapping | Business logic validation (lives in services) |
| **Synthetic Data Agent** | Schema introspection, LLM spec generation, Faker row generation, synthetic schema load | Live claim processing, user authentication, production data |

---

## 3. Policy Service

**Class:** `PolicyServiceImpl` implements `PolicyService`  
**Endpoint:** `GET /api/v1/policies/{policyNumber}`

### Responsibilities
- Look up a policy and its `PolicyCoverage` records in a single `JOIN FETCH` query
- Build `coverageLimits` map (`Map<ClaimType, BigDecimal>`) filtered to active coverages only
- Return `PolicyResponse`

### Key Design Decision
`JOIN FETCH` eliminates N+1: coverages are loaded together with the policy in one SQL query.

```mermaid
flowchart TD
    IN([GET /api/v1/policies/{policyNumber}]) --> Q1{Policy exists?}
    Q1 -->|No| E1[404 PolicyNotFoundException]
    Q1 -->|Yes| MAP[Build coverageLimits map\nfrom active PolicyCoverages]
    MAP --> OUT([200 PolicyResponse])
```

---

## 4. Claim Service

**Class:** `ClaimServiceImpl` implements `ClaimService`

### Submit Claim — 7-Step Validation Chain

```mermaid
flowchart TD
    A([POST /api/v1/claims]) --> B{Policy exists?}
    B -->|No| E1[404 PolicyNotFoundException]
    B -->|Yes| C{Policy ACTIVE?}
    C -->|No| E2[400 PolicyInactiveException]
    C -->|Yes| D{Today within\neffective–expiry range?}
    D -->|No| E3[400 PolicyInactiveException]
    D -->|Yes| E{Active coverage\nfor ClaimType exists?}
    E -->|No| E4[400 InvalidClaimTypeException]
    E -->|Yes| F{claimAmount\n<= limitAmount?}
    F -->|No| E5[400 CoverageExceededException]
    F -->|Yes| G{Duplicate claim\nin last 24h?}
    G -->|Yes| E6[409 DuplicateClaimException]
    G -->|No| H[Save Claim + ClaimHistory\nstatus = SUBMITTED]
    H --> OUT([201 ClaimResponse])
```

### Review Claim

```mermaid
flowchart TD
    A([PATCH /api/v1/claims/{id}/review]) --> B{Claim exists?}
    B -->|No| E1[404 ClaimNotFoundException]
    B -->|Yes| C{action?}
    C -->|APPROVE| D1[Set status = APPROVED]
    C -->|REJECT| D2[Set status = REJECTED]
    D1 --> E[Append ClaimHistory record\nwith reviewerNotes]
    D2 --> E
    E --> OUT([200 ClaimResponse])
```

---

## 5. Claim History Service

**Class:** `ClaimHistoryService` (concrete `@Service`)  
**Endpoint:** `GET /api/v1/claims/{claimId}/history`

### Design
- **Read-only** from this service — writes happen only in `ClaimServiceImpl`
- Guards with `existsById()` before querying history to provide a proper 404 for missing claims
- Returns records newest-first (`ORDER BY timestamp DESC`)

---

## 6. Validation Layer

### Two-Layer Validation Strategy

```mermaid
graph LR
    USER["User Input"] --> CL["Client-side\nZod Schema\n(React Frontend)"]
    CL -->|HTTP Request| SV["Server-side\nBean Validation\n(@Valid + custom annotations)"]
    SV -->|Business rules| BL["Service Layer\n(7-step claim validation)"]
    BL -->|Domain exceptions| GEH["GlobalExceptionHandler\n→ ErrorResponse JSON"]
```

| Layer | Technology | What it validates |
|---|---|---|
| Client (Zod) | TypeScript schema | Policy number format, non-future date, amount > 0, description length, coverage limit check |
| Server (Bean Validation) | Jakarta `@Valid` + custom | Same format rules + custom annotations |
| Service (business logic) | Java service methods | Policy status, date range, coverage existence, amount vs limit, duplicate detection |
| Global handler | `@RestControllerAdvice` | Maps all failures to uniform `ErrorResponse` |

---

## 7. Inter-Service Dependency Graph

```mermaid
graph TD
    CC[ClaimController] --> CS[ClaimService]
    PC[PolicyController] --> PS[PolicyService]
    HC[ClaimHistoryController] --> HS[ClaimHistoryService]

    CS -->|reads via PolicyRepository| PS_DB[(policies + coverages)]
    CS -->|reads/writes| CR[(claims)]
    CS -->|reads/writes| HR[(claim_history)]

    PS -->|reads| PR[(policies + coverages)]
    HS -->|reads| CR2[(claims)]
    HS -->|reads| HR2[(claim_history)]

    style CS fill:#dcfce7,stroke:#16a34a
    style PS fill:#dbeafe,stroke:#3b82f6
    style HS fill:#fef3c7,stroke:#f59e0b
```

> Note: `ClaimService` accesses `PolicyRepository` directly (not via `PolicyService`) to keep both operations within a single `@Transactional` boundary and avoid cross-service circular dependency.

---

## 9. Synthetic Data Agent — Service Boundary

**Runtime:** Python 3.12 + Streamlit  
**Docker profile:** `test` only (never deployed to prod)  
**Port:** 8501  
**Endpoint accessed by:** developers / QA engineers (browser)

### Responsibilities
- Introspect the source schema via `information_schema.columns`
- Call an OpenAI-compatible LLM endpoint to receive a per-column generation plan (`GenSpec`)
- Fall back to a rule-based spec when the LLM is unreachable
- Produce rows using Faker providers, categorical choices, numeric ranges, temporal generators, UUIDs, booleans, and JSON
- Bulk-load generated rows into the `synthetic` schema of the configured PostgreSQL instance

### Boundary Rules
- Reads only from `information_schema` and the specified source schema — **never writes to `public`**
- All generated data lands in the **`synthetic`** schema (separate from live data)
- DB credentials are injected at runtime via Docker Compose environment variables; they are never hardcoded
- The `synthetic-agent` Docker service `depends_on: db (service_healthy)` — it cannot start before the database is ready

### Isolation Guarantee

```mermaid
graph TD
    PROD["prod profile\ndocker compose up"] --> APP["app (Spring Boot)"]
    PROD --> DB["db (PostgreSQL)"]

    TEST["test profile\ndocker compose --profile test up"] --> APP2["app (Spring Boot)"]
    TEST --> DB2["db (PostgreSQL)"]
    TEST --> SA["synthetic-agent (Streamlit)"]

    SA -->|writes only| SYN[("synthetic schema")]
    APP2 -->|reads/writes| PUB[("public schema")]

    style SA fill:#fce7f3,stroke:#ec4899
    style SYN fill:#fdf2f8,stroke:#ec4899
    style PROD fill:#dcfce7,stroke:#16a34a
    style TEST fill:#fce7f3,stroke:#ec4899
```

---

## 10. Scalability Pattern

The API is **stateless by design** — no HTTP session, no in-memory cache. All state lives in PostgreSQL. This allows horizontal scaling behind a load balancer:

```mermaid
graph LR
    LB["Load Balancer\n(e.g., Nginx)"]
    LB --> I1["Spring Boot Instance 1\n:8080"]
    LB --> I2["Spring Boot Instance 2\n:8080"]
    LB --> I3["Spring Boot Instance N\n:8080"]
    I1 --> DB[("PostgreSQL Primary\n:5432")]
    I2 --> DB
    I3 --> DB
```
