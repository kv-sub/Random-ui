# Architecture Diagrams
## Insurance Claim Submission System

**Version:** 1.1  
**Date:** March 2026

---

## Document History

| Version | Date       | Changes                                                                          |
|---------|------------|----------------------------------------------------------------------------------|
| 1.0     | 2026-01-05 | Initial system context and container diagrams (Sprint 1)                         |
| 1.1     | 2026-02-09 | Updated component diagram — added claim submission and validation components (Sprint 3) |

---

## 1. System Context Diagram

```mermaid
graph TB
    subgraph Actors["Actors"]
        C["👤 Customer\nPolicyholder"]
        A["👤 Admin / Reviewer\nInsurance Employee"]
    end

    subgraph System["Insurance Claim System"]
        SPA["React SPA\n(Browser)"]
        API["Spring Boot REST API\n(Port 8080)"]
        DB[("PostgreSQL 16\n(Port 5432)")]
    end

    subgraph External["External Systems"]
        PM["Policy Management System\n(Upstream — seeds policy data)"]
    end

    C -->|"Uses browser"| SPA
    A -->|"Uses browser"| SPA
    SPA -->|"HTTP REST /api/v1/*"| API
    API -->|"JDBC / JPA"| DB
    PM -->|"Seeds policies + coverages"| DB

    style System fill:#dbeafe,stroke:#3b82f6
    style Actors fill:#f0fdf4,stroke:#22c55e
    style External fill:#fef9c3,stroke:#ca8a04
```

---

## 2. Container Diagram

```mermaid
graph TB
    subgraph Browser["User's Browser"]
        SPA["React SPA\nReact 19 + TypeScript + Vite\n────────────────────────\nCustomer: Submit & Track Claims\nAdmin: Policy Search & Claim Review\nAuth: Role-based (CUSTOMER/ADMIN)\nState: TanStack Query + Zustand"]
    end

    subgraph DockerHost["Docker Host"]
        subgraph AppContainer["app container"]
            API["Spring Boot REST API\nJava 21 + Spring Boot 3\n────────────────────────\nPOST /api/v1/claims\nGET  /api/v1/claims/{id}\nPATCH /api/v1/claims/{id}/review\nGET  /api/v1/claims/{id}/history\nGET  /api/v1/claims/policy/{id}\nGET  /api/v1/policies/{number}"]
            SWAGGER["Swagger UI\n/swagger-ui.html\n(SpringDoc OpenAPI 3.0)"]
        end
        subgraph DBContainer["db container"]
            PG[("PostgreSQL 16\nPort: 5432\n────────────\npolicies\npolicy_coverages\nclaims\nclaim_history")]
        end
    end

    Browser -->|"HTTP REST\nJSON /api/v1/*"| API
    Browser -->|"View API docs"| SWAGGER
    API -->|"JDBC / Hibernate ORM"| PG

    style Browser fill:#dbeafe,stroke:#3b82f6
    style DockerHost fill:#f0fdf4,stroke:#22c55e
    style AppContainer fill:#dcfce7,stroke:#16a34a
    style DBContainer fill:#fef9c3,stroke:#ca8a04
```

---

## 3. Backend Component Diagram

```mermaid
graph TB
    subgraph Controllers["Controllers (@RestController)"]
        PC["PolicyController\nGET /api/v1/policies/{no}"]
        CC["ClaimController\nPOST, GET, PATCH /api/v1/claims"]
        HC["ClaimHistoryController\nGET /api/v1/claims/{id}/history"]
    end

    subgraph Services["Services (@Service)"]
        PS["PolicyServiceImpl\ngetPolicy()"]
        CS["ClaimServiceImpl\nsubmitClaim()\ngetClaimStatus()\nreviewClaim()\ngetClaimsByPolicy()"]
        HS["ClaimHistoryService\ngetHistory()"]
    end

    subgraph Repos["Repositories (JPA)"]
        PR["PolicyRepository\nfindByPolicyNumber\nfindByPolicyNumberWithCoverages"]
        CR["ClaimRepository\nfindByPolicy_PolicyId\nfindDuplicateClaims"]
        CVR["PolicyCoverageRepository\nfindByPolicyIdAndClaimTypeAndIsActiveTrue"]
        HR["ClaimHistoryRepository\nfindByClaimClaimIdOrderByTimestampDesc"]
    end

    subgraph Cross["Cross-cutting"]
        GEH["GlobalExceptionHandler\n@RestControllerAdvice"]
        VAL["Validators\n@ValidPolicyNumber\n@ValidIncidentDate"]
        OA["OpenApiConfig\nSwagger UI"]
    end

    DB[("PostgreSQL 16")]

    PC --> PS
    CC --> CS
    HC --> HS

    PS --> PR
    CS --> PR
    CS --> CR
    CS --> CVR
    CS --> HR
    HS --> CR
    HS --> HR

    PR --> DB
    CR --> DB
    CVR --> DB
    HR --> DB

    style Controllers fill:#dbeafe,stroke:#3b82f6
    style Services fill:#dcfce7,stroke:#16a34a
    style Repos fill:#fef3c7,stroke:#f59e0b
    style Cross fill:#fce7f3,stroke:#ec4899
```

---

## 4. Claim Submission — Sequence Diagram

```mermaid
sequenceDiagram
    actor C as Customer
    participant F as React Frontend
    participant A as Spring Boot API
    participant D as PostgreSQL

    C->>F: Enter policy number (debounce 500ms)
    F->>A: GET /api/v1/policies/{policyNumber}
    A->>D: SELECT policy + coverages (JOIN FETCH)
    D-->>A: Policy with coverages
    A-->>F: PolicyResponse {coverageLimits}
    F-->>C: Show verified policy + per-type limits

    C->>F: Fill claim details + submit
    F->>F: Zod schema validation (client-side)
    F->>A: POST /api/v1/claims
    Note over A: Bean Validation (@Valid)
    A->>D: findByPolicyNumberWithCoverages
    D-->>A: Policy + Coverages
    Note over A: Assert ACTIVE + date range
    A->>D: findByPolicy_PolicyIdAndClaimTypeAndIsActiveTrue
    D-->>A: PolicyCoverage
    Note over A: Assert amount <= limitAmount
    A->>D: findDuplicateClaims(policyId, type, date, now-24h)
    D-->>A: [] (no duplicates)
    A->>D: INSERT INTO claims (status=SUBMITTED)
    A->>D: INSERT INTO claim_history (status=SUBMITTED)
    D-->>A: Saved Claim
    A-->>F: 201 ClaimResponse
    F-->>C: Success modal — "Claim Submitted!"
```

---

## 5. Claim Review — Sequence Diagram

```mermaid
sequenceDiagram
    actor Ad as Admin
    participant F as React Frontend
    participant A as Spring Boot API
    participant D as PostgreSQL

    Ad->>F: Search policy number
    F->>A: GET /api/v1/policies/{policyNumber}
    A-->>F: PolicyResponse
    F-->>Ad: Policy card + coverage grid

    Ad->>F: Click "View Claims"
    F->>A: GET /api/v1/claims/policy/{policyId}
    A-->>F: List<ClaimResponse>
    F-->>Ad: Claims list

    Ad->>F: Click a claim
    F->>A: GET /api/v1/claims/{claimId}
    F->>A: GET /api/v1/claims/{claimId}/history
    A-->>F: ClaimResponse + List<ClaimHistoryResponse>
    F-->>Ad: Detail card + history timeline

    Ad->>F: Click Approve / enter notes / confirm
    F->>A: PATCH /api/v1/claims/{claimId}/review {action: APPROVE, reviewerNotes}
    A->>D: SELECT claim WHERE claim_id = ?
    D-->>A: Claim
    A->>D: UPDATE claims SET status = APPROVED
    A->>D: INSERT INTO claim_history (status=APPROVED, reviewer_notes)
    D-->>A: Updated claim
    A-->>F: 200 ClaimResponse {status: APPROVED}
    F-->>Ad: Toast "Claim approved!"
```

---

## 6. Claim Status State Machine

```mermaid
stateDiagram-v2
    [*] --> SUBMITTED : Customer submits\nPOST /api/v1/claims

    SUBMITTED --> IN_REVIEW : Admin sets in review\nPATCH /review
    SUBMITTED --> APPROVED  : Admin approves directly\nPATCH /review action=APPROVE
    SUBMITTED --> REJECTED  : Admin rejects\nPATCH /review action=REJECT

    IN_REVIEW --> APPROVED  : Admin approves\nPATCH /review action=APPROVE
    IN_REVIEW --> REJECTED  : Admin rejects\nPATCH /review action=REJECT

    APPROVED --> [*] : Terminal state
    REJECTED --> [*] : Terminal state

    note right of SUBMITTED
        Initial state.
        ClaimHistory record created.
    end note
    note right of APPROVED
        Terminal state.
        ClaimHistory appended
        with reviewer_notes.
    end note
```

---

## 7. Frontend Component Tree

```mermaid
graph TD
    App["App\nQueryClientProvider + BrowserRouter"]

    App --> Navbar["Navbar\nRole-aware navigation"]
    App --> LoginPage["LoginPage /login\nRole selector"]
    App --> Home["Home /\nRole-aware dashboard"]
    App --> SubmitClaimPage["SubmitClaimPage\n/submit-claim\nCUSTOMER only"]
    App --> TrackClaimPage["TrackClaimPage\n/track-claim\nCUSTOMER only"]
    App --> AdminRoutes["AdminRoutes\n/admin/*\nADMIN only"]

    SubmitClaimPage --> ClaimSubmissionForm["ClaimSubmissionForm\nreact-hook-form + zod"]
    ClaimSubmissionForm --> PolicyVerify["Policy Verification\ndebounced GET /policies"]
    ClaimSubmissionForm --> CoverageDropdown["ClaimType Dropdown\ndynamic from coverageLimits"]
    ClaimSubmissionForm --> FormModal["Success / Error Modal"]

    TrackClaimPage --> ClaimCard["Claim Detail Card"]
    TrackClaimPage --> StatusBadge["Badge (status color)"]
    TrackClaimPage --> HistTimeline1["History Timeline"]

    AdminRoutes --> AdminPoliciesPage["AdminPoliciesPage\n/admin/policies\nPolicy search + coverage grid"]
    AdminRoutes --> ClaimListPage["ClaimListPage\n/admin/claims/:policyId\nClaims per policy"]
    AdminRoutes --> ClaimDetailPage["ClaimDetailPage\n/admin/claims/:policyId/:claimId"]

    ClaimDetailPage --> DetailCard["Claim Details Card"]
    ClaimDetailPage --> HistTimeline2["History Timeline"]
    ClaimDetailPage --> ReviewModal["Review Modal\nApprove / Reject + notes"]

    style LoginPage fill:#fce7f3,stroke:#ec4899
    style SubmitClaimPage fill:#dbeafe,stroke:#3b82f6
    style TrackClaimPage fill:#dbeafe,stroke:#3b82f6
    style AdminRoutes fill:#fef3c7,stroke:#f59e0b
    style AdminPoliciesPage fill:#fef3c7,stroke:#f59e0b
    style ClaimListPage fill:#fef3c7,stroke:#f59e0b
    style ClaimDetailPage fill:#fef3c7,stroke:#f59e0b
```

---

## 8. Data Flow Diagram

```mermaid
flowchart LR
    subgraph Client["Browser (React SPA)"]
        FORM["Claim\nSubmission\nForm"]
        TRACK["Track\nClaim\nPage"]
        ADMIN["Admin\nReview\nPortal"]
    end

    subgraph API["Spring Boot API"]
        PC["PolicyController"]
        CC["ClaimController"]
        HC["ClaimHistoryController"]
        PS["PolicyService"]
        CS["ClaimService"]
        HS["ClaimHistoryService"]
    end

    subgraph DB["PostgreSQL"]
        POL[("policies")]
        COV[("policy_coverages")]
        CLM[("claims")]
        HIS[("claim_history")]
    end

    FORM -->|"GET /policies/{no}"| PC
    FORM -->|"POST /claims"| CC
    TRACK -->|"GET /claims/{id}"| CC
    TRACK -->|"GET /claims/{id}/history"| HC
    ADMIN -->|"GET /policies/{no}"| PC
    ADMIN -->|"GET /claims/policy/{id}"| CC
    ADMIN -->|"PATCH /claims/{id}/review"| CC
    ADMIN -->|"GET /claims/{id}/history"| HC

    PC --> PS
    CC --> CS
    HC --> HS

    PS --> POL
    PS --> COV
    CS --> POL
    CS --> COV
    CS --> CLM
    CS --> HIS
    HS --> CLM
    HS --> HIS
```

---

## 9. Deployment Architecture

```mermaid
graph TB
    subgraph Internet["Internet / Intranet"]
        USER["End Users\n(Browser)"]
    end

    subgraph DockerCompose["Docker Compose Stack"]
        subgraph Net["docker network: insurance-network"]
            APP["app service\nSpring Boot JAR\nPort 8080:8080\nDepends on: db"]
            DB["db service\nPostgreSQL 16\nPort 5432:5432\nVolume: pgdata"]
        end
    end

    USER -->|"HTTP :8080"| APP
    APP -->|"JDBC :5432"| DB

    style Internet fill:#f1f5f9,stroke:#94a3b8
    style DockerCompose fill:#f0fdf4,stroke:#22c55e
    style Net fill:#dcfce7,stroke:#16a34a
```
