# Entity-Relationship Diagram
## Insurance Claim Submission System

**Version:** 1.2  
**Date:** March 2026

---

## Document History

| Version | Date       | Changes                                                                      |
|---------|------------|------------------------------------------------------------------------------|
| 1.0     | 2026-01-05 | Initial ER diagram — `policies` and `policy_coverages` entities (Sprint 1)   |
| 1.1     | 2026-02-02 | Added `claims` entity with policy FK and coverage-type relationship (Sprint 3) |
| 1.2     | 2026-03-02 | Added `claim_history` entity with claim FK (Sprint 5)                        |

---

## 1. ER Diagram (Mermaid)

```mermaid
erDiagram
    POLICIES {
        bigserial  policy_id      PK "Auto-increment PK"
        varchar20  policy_number  UK "NOT NULL — format POL-XXXXX"
        bigint     customer_id       "NOT NULL — FK to external customer system"
        varchar20  status            "ACTIVE|INACTIVE|EXPIRED|CANCELLED|PENDING"
        date       effective_date    "NOT NULL — policy start date"
        date       expiry_date       "NOT NULL — policy end date"
        numeric    coverage_limit    "NUMERIC(15,2) — aggregate ceiling"
    }

    POLICY_COVERAGES {
        bigserial  coverage_id    PK "Auto-increment PK"
        bigint     policy_id      FK "NOT NULL → policies(policy_id) CASCADE"
        varchar20  claim_type        "MEDICAL|DENTAL|VISION|LIFE|AUTO|HOME|DISABILITY"
        numeric    limit_amount      "NUMERIC(15,2) — per-type claimable limit"
        boolean    is_active         "NOT NULL DEFAULT true — soft disable"
    }

    CLAIMS {
        bigserial  claim_id       PK "Auto-increment PK"
        bigint     policy_id      FK "NOT NULL → policies(policy_id)"
        varchar20  claim_type        "MEDICAL|DENTAL|VISION|LIFE|AUTO|HOME|DISABILITY"
        numeric    claim_amount      "NUMERIC(15,2) — requested amount"
        date       incident_date     "NOT NULL — date of insured incident"
        text       description       "nullable — customer narrative (10-1000 chars)"
        varchar20  status            "SUBMITTED|IN_REVIEW|APPROVED|REJECTED"
        timestamp  created_at        "NOT NULL DEFAULT CURRENT_TIMESTAMP"
        timestamp  updated_at        "NOT NULL — auto-updated on change"
    }

    CLAIM_HISTORY {
        bigserial  history_id     PK "Auto-increment PK"
        bigint     claim_id       FK "NOT NULL → claims(claim_id) CASCADE"
        varchar20  status            "SUBMITTED|IN_REVIEW|APPROVED|REJECTED"
        timestamp  timestamp         "NOT NULL DEFAULT CURRENT_TIMESTAMP — immutable"
        text       reviewer_notes    "nullable — populated on APPROVE/REJECT"
    }

    POLICIES ||--o{ POLICY_COVERAGES : "has many per claim type"
    POLICIES ||--o{ CLAIMS           : "has many claims"
    CLAIMS   ||--o{ CLAIM_HISTORY    : "has many audit records"
```

---

## 2. Entity Relationship Overview

```mermaid
graph TD
    subgraph Core["Core Entities"]
        POL["POLICIES\n──────────\npolicy_id PK\npolicy_number UK\ncustomer_id\nstatus\neffective_date\nexpiry_date\ncoverage_limit"]
        COV["POLICY_COVERAGES\n──────────\ncoverage_id PK\npolicy_id FK\nclaim_type\nlimit_amount\nis_active"]
        CLM["CLAIMS\n──────────\nclaim_id PK\npolicy_id FK\nclaim_type\nclaim_amount\nincident_date\ndescription\nstatus\ncreated_at\nupdated_at"]
        HIS["CLAIM_HISTORY\n──────────\nhistory_id PK\nclaim_id FK\nstatus\ntimestamp\nreviewer_notes"]
    end

    POL -->|"1 : N\nCASCADE DELETE"| COV
    POL -->|"1 : N"| CLM
    CLM -->|"1 : N\nCASCADE DELETE"| HIS

    style POL fill:#dbeafe,stroke:#3b82f6
    style COV fill:#dcfce7,stroke:#16a34a
    style CLM fill:#fef3c7,stroke:#f59e0b
    style HIS fill:#fce7f3,stroke:#ec4899
```

---

## 3. Relationship Definitions

### `POLICIES` → `POLICY_COVERAGES` (One-to-Many)

| Aspect | Detail |
|---|---|
| **Cardinality** | 1 policy has 0..N coverages |
| **Meaning** | Each policy can cover multiple claim types (MEDICAL, AUTO, etc.) with individual dollar limits |
| **FK** | `policy_coverages.policy_id → policies.policy_id` |
| **Delete behaviour** | `ON DELETE CASCADE` — removing a policy removes its coverage entries |
| **Soft disable** | `is_active = false` deactivates a coverage type without deleting the record |

### `POLICIES` → `CLAIMS` (One-to-Many)

| Aspect | Detail |
|---|---|
| **Cardinality** | 1 policy has 0..N claims |
| **Meaning** | A policyholder can submit multiple claims against the same policy over its lifetime |
| **FK** | `claims.policy_id → policies.policy_id` |
| **Delete behaviour** | Restricted — claims are financial records and must not be cascade-deleted |

### `CLAIMS` → `CLAIM_HISTORY` (One-to-Many)

| Aspect | Detail |
|---|---|
| **Cardinality** | 1 claim has 1..N history records |
| **Meaning** | Every claim starts with one `SUBMITTED` record; each subsequent status change appends a new record |
| **FK** | `claim_history.claim_id → claims.claim_id` |
| **Delete behaviour** | `ON DELETE CASCADE` — if a claim is administratively deleted, its history is removed too |
| **Write behaviour** | **Append-only** — no UPDATE operations on `claim_history` rows |

---

## 4. Allowed Enum Values

```mermaid
graph LR
    subgraph PolicyStatus["PolicyStatus"]
        PS1["ACTIVE\n(accepts claims)"]
        PS2["INACTIVE\n(suspended)"]
        PS3["EXPIRED\n(term ended)"]
        PS4["CANCELLED"]
        PS5["PENDING\n(not yet active)"]
    end

    subgraph ClaimStatus["ClaimStatus"]
        CS1["SUBMITTED\n(initial)"]
        CS2["IN_REVIEW"]
        CS3["APPROVED\n(terminal)"]
        CS4["REJECTED\n(terminal)"]
        CS1 --> CS2
        CS1 --> CS3
        CS1 --> CS4
        CS2 --> CS3
        CS2 --> CS4
    end

    subgraph ClaimType["ClaimType"]
        CT1["MEDICAL"]
        CT2["DENTAL"]
        CT3["VISION"]
        CT4["LIFE"]
        CT5["AUTO"]
        CT6["HOME"]
        CT7["DISABILITY"]
    end
```

---

## 5. Index Strategy

| Index Name | Table | Column(s) | Purpose |
|---|---|---|---|
| *(implicit UNIQUE)* | `policies` | `policy_number` | Fast lookup by business key |
| `idx_claims_policy_id` | `claims` | `policy_id` | Join/filter claims by policy |
| `idx_claims_status` | `claims` | `status` | Admin review queue filtering |
| `idx_claims_created_at` | `claims` | `created_at` | Duplicate detection query (24h window) |
| `idx_claim_history_claim` | `claim_history` | `claim_id` | Fetch history ordered by timestamp |
| `idx_policy_coverages_pol` | `policy_coverages` | `policy_id` | Join coverages to policy |

---

## 6. Data Integrity Rules

```mermaid
graph TD
    subgraph Constraints["Data Integrity Constraints"]
        C1["policy_number UNIQUE\nNo two policies share a number"]
        C2["NUMERIC(15,2) monetary fields\nNo floating-point for financial data"]
        C3["claim_amount >= 0.01\nEnforced by app-layer validation"]
        C4["claimAmount <= coverage.limitAmount\nEnforced by ClaimService"]
        C5["No future incident_date\nEnforced by @ValidIncidentDate"]
        C6["Policy must be ACTIVE\nEnforced by ClaimService"]
        C7["Duplicate = same policy+type+date in 24h\nEnforced by findDuplicateClaims query"]
        C8["claim_history is append-only\nNo UPDATE queries on claim_history"]
    end
```
