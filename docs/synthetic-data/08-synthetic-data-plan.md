# Synthetic Data Generation Plan
## Insurance Claim Submission System

**Version:** 1.1  
**Date:** March 2026

---

## Document History

| Version | Date       | Changes                                                                           |
|---------|------------|-----------------------------------------------------------------------------------|
| 1.0     | 2026-02-16 | Initial synthetic data plan — seed personas, boundary cases, edge datasets (Sprint 4) |
| 1.1     | 2026-04-13 | Extended with load-test data sets and high-volume performance personas (Sprint 8)  |

---

## 1. Purpose

This document defines the synthetic test data strategy for the Insurance Claim Submission System.
It covers:
- **Seed data** loaded into the database for development and integration testing
- **Test personas** representing different customer and policy profiles
- **Boundary and edge case data sets** to validate all business rules
- **SQL seed scripts** ready to execute

---

## 2. Test Personas

### Customer Personas

| Persona ID | Name | Customer ID | Description |
|---|---|---|---|
| P-CUST-01 | Alex Chen | 1001 | Standard customer with active comprehensive policy |
| P-CUST-02 | Maria Garcia | 1002 | Customer with an expired policy |
| P-CUST-03 | James Wilson | 1003 | Customer with a cancelled policy |
| P-CUST-04 | Lisa Park | 1004 | Customer with a pending policy (not yet active) |
| P-CUST-05 | David Brown | 1005 | Customer with active policy but limited coverages |

### Admin Personas

| Persona ID | Name | Description |
|---|---|---|
| A-ADMIN-01 | Sarah Mitchell | Senior claims adjudicator — handles all claim reviews |
| A-ADMIN-02 | Tom Johnson | Junior reviewer — observes in-review claims |

---

## 3. Policy Seed Data

### Policy Matrix

| Policy Number | Customer ID | Status | Effective Date | Expiry Date | Coverage Limit | Purpose |
|---|---|---|---|---|---|---|
| `POL-AB123` | 1001 | ACTIVE | 2025-01-01 | 2027-12-31 | 50,000.00 | Standard active policy — happy path |
| `POL-CD456` | 1001 | ACTIVE | 2025-06-01 | 2028-05-31 | 25,000.00 | Active policy — limited coverages |
| `POL-EF789` | 1002 | EXPIRED | 2020-01-01 | 2024-12-31 | 30,000.00 | Expired policy — rejection testing |
| `POL-GH012` | 1003 | CANCELLED | 2023-01-01 | 2025-12-31 | 40,000.00 | Cancelled policy — rejection testing |
| `POL-IJ345` | 1004 | PENDING | 2026-06-01 | 2028-06-01 | 60,000.00 | Pending policy — not yet active |
| `POL-KL678` | 1005 | ACTIVE | 2024-01-01 | 2026-12-31 | 15,000.00 | Active policy — boundary amount testing |
| `POL-MN901` | 1001 | INACTIVE | 2022-01-01 | 2025-01-01 | 20,000.00 | Inactive policy — rejection testing |

### Coverage Matrix for `POL-AB123` (Comprehensive)

| Claim Type | Limit Amount | Is Active |
|---|---|---|
| MEDICAL | 10,000.00 | true |
| DENTAL | 2,000.00 | true |
| VISION | 1,000.00 | false ← inactive on purpose |
| LIFE | 50,000.00 | true |
| AUTO | 20,000.00 | true |
| HOME | 30,000.00 | true |
| DISABILITY | 5,000.00 | true |

### Coverage Matrix for `POL-CD456` (Limited)

| Claim Type | Limit Amount | Is Active |
|---|---|---|
| MEDICAL | 5,000.00 | true |
| DENTAL | 1,000.00 | true |

### Coverage Matrix for `POL-KL678` (Boundary Testing)

| Claim Type | Limit Amount | Is Active |
|---|---|---|
| MEDICAL | 0.01 | true ← minimum boundary |
| AUTO | 15,000.00 | true |

---

## 4. Claim Seed Data

### Claim States Coverage

| Claim ID | Policy | Type | Amount | Incident Date | Status | Description |
|---|---|---|---|---|---|---|
| 1 | POL-AB123 | MEDICAL | 1,500.00 | 2026-03-01 | SUBMITTED | Awaiting initial review |
| 2 | POL-AB123 | DENTAL | 800.00 | 2026-02-15 | IN_REVIEW | Under active review |
| 3 | POL-AB123 | AUTO | 5,000.00 | 2026-01-10 | APPROVED | Approved claim |
| 4 | POL-AB123 | HOME | 12,000.00 | 2025-12-01 | REJECTED | Rejected claim |
| 5 | POL-CD456 | MEDICAL | 4,999.99 | 2026-03-05 | SUBMITTED | Just under limit |
| 6 | POL-AB123 | LIFE | 50,000.00 | 2026-02-01 | SUBMITTED | Max coverage amount |
| 7 | POL-KL678 | MEDICAL | 0.01 | 2026-03-08 | SUBMITTED | Minimum boundary amount |

### Claim History Seed (for claim audit trail testing)

| History ID | Claim ID | Status | Timestamp | Reviewer Notes |
|---|---|---|---|---|
| 1 | 1 | SUBMITTED | 2026-03-01 09:00:00 | — |
| 2 | 2 | SUBMITTED | 2026-02-15 08:30:00 | — |
| 3 | 2 | IN_REVIEW | 2026-02-16 10:00:00 | Requesting additional documentation |
| 4 | 3 | SUBMITTED | 2026-01-10 14:00:00 | — |
| 5 | 3 | IN_REVIEW | 2026-01-11 09:00:00 | Vehicle inspection report received |
| 6 | 3 | APPROVED | 2026-01-12 11:00:00 | All documentation verified. Claim approved. |
| 7 | 4 | SUBMITTED | 2025-12-01 10:00:00 | — |
| 8 | 4 | REJECTED | 2025-12-03 15:00:00 | Claim amount exceeds documented damage. Rejected. |

---

## 5. Boundary Value Test Cases

### Amount Boundaries

| Test Case | Policy | Type | Amount | Expected Result |
|---|---|---|---|---|
| BC-01 | POL-AB123 | MEDICAL | 0.00 | REJECT — amount must be > 0 |
| BC-02 | POL-AB123 | MEDICAL | 0.01 | ACCEPT — minimum valid amount |
| BC-03 | POL-AB123 | MEDICAL | 9,999.99 | ACCEPT — just under limit |
| BC-04 | POL-AB123 | MEDICAL | 10,000.00 | ACCEPT — exactly at limit |
| BC-05 | POL-AB123 | MEDICAL | 10,000.01 | REJECT — just over limit |
| BC-06 | POL-AB123 | MEDICAL | -1.00 | REJECT — negative amount |
| BC-07 | POL-KL678 | MEDICAL | 0.01 | ACCEPT — policy with 0.01 limit |

### Date Boundaries

| Test Case | Incident Date | Policy Effective | Policy Expiry | Expected Result |
|---|---|---|---|---|
| BD-01 | today - 1 day | past | future | ACCEPT |
| BD-02 | today | past | future | ACCEPT |
| BD-03 | today + 1 day | past | future | REJECT — future date |
| BD-04 | policy effectiveDate | past | future | ACCEPT — exactly on start date |
| BD-05 | policy expiryDate | past | future | ACCEPT — exactly on end date |
| BD-06 | 2000-01-01 (very old) | past | future | ACCEPT — no lookback limit |

### Description Boundaries

| Test Case | Description Length | Expected Result |
|---|---|---|
| DD-01 | 9 chars | REJECT — too short |
| DD-02 | 10 chars | ACCEPT — minimum |
| DD-03 | 500 chars | ACCEPT |
| DD-04 | 1000 chars | ACCEPT — maximum |
| DD-05 | 1001 chars | REJECT — too long |
| DD-06 | blank | REJECT — @NotBlank |

### Policy Number Format

| Test Case | Policy Number | Expected Result |
|---|---|---|
| PN-01 | `POL-AB123` | ACCEPT — valid |
| PN-02 | `POL-ab123` | REJECT — lowercase |
| PN-03 | `POL-AB12` | REJECT — only 4 chars |
| PN-04 | `POL-AB1234` | REJECT — 6 chars |
| PN-05 | `ABC-AB123` | REJECT — wrong prefix |
| PN-06 | `POL-AB123 ` | REJECT — trailing space |
| PN-07 | `POL-AB!@#` | REJECT — special chars |

---

## 6. SQL Seed Script

```sql
-- ============================================================
-- SEED SCRIPT: Insurance Claim Submission System Test Data
-- Version: 1.0 | Date: March 2026
-- Run AFTER schema creation
-- ============================================================

-- ----------------------------------------------------------
-- POLICIES
-- ----------------------------------------------------------
INSERT INTO policies (policy_number, customer_id, status, effective_date, expiry_date, coverage_limit) VALUES
('POL-AB123', 1001, 'ACTIVE',    '2025-01-01', '2027-12-31', 50000.00),
('POL-CD456', 1001, 'ACTIVE',    '2025-06-01', '2028-05-31', 25000.00),
('POL-EF789', 1002, 'EXPIRED',   '2020-01-01', '2024-12-31', 30000.00),
('POL-GH012', 1003, 'CANCELLED', '2023-01-01', '2025-12-31', 40000.00),
('POL-IJ345', 1004, 'PENDING',   '2026-06-01', '2028-06-01', 60000.00),
('POL-KL678', 1005, 'ACTIVE',    '2024-01-01', '2026-12-31', 15000.00),
('POL-MN901', 1001, 'INACTIVE',  '2022-01-01', '2025-01-01', 20000.00);

-- ----------------------------------------------------------
-- POLICY COVERAGES for POL-AB123 (comprehensive)
-- ----------------------------------------------------------
INSERT INTO policy_coverages (policy_id, claim_type, limit_amount, is_active)
SELECT p.policy_id, v.claim_type, v.limit_amount, v.is_active
FROM policies p,
     (VALUES
       ('MEDICAL',    10000.00, TRUE),
       ('DENTAL',      2000.00, TRUE),
       ('VISION',      1000.00, FALSE),  -- intentionally inactive
       ('LIFE',       50000.00, TRUE),
       ('AUTO',       20000.00, TRUE),
       ('HOME',       30000.00, TRUE),
       ('DISABILITY',  5000.00, TRUE)
     ) AS v(claim_type, limit_amount, is_active)
WHERE p.policy_number = 'POL-AB123';

-- POLICY COVERAGES for POL-CD456 (limited)
INSERT INTO policy_coverages (policy_id, claim_type, limit_amount, is_active)
SELECT p.policy_id, v.claim_type, v.limit_amount, v.is_active
FROM policies p,
     (VALUES
       ('MEDICAL', 5000.00, TRUE),
       ('DENTAL',  1000.00, TRUE)
     ) AS v(claim_type, limit_amount, is_active)
WHERE p.policy_number = 'POL-CD456';

-- POLICY COVERAGES for POL-KL678 (boundary testing)
INSERT INTO policy_coverages (policy_id, claim_type, limit_amount, is_active)
SELECT p.policy_id, v.claim_type, v.limit_amount, v.is_active
FROM policies p,
     (VALUES
       ('MEDICAL', 0.01, TRUE),       -- minimum boundary
       ('AUTO',    15000.00, TRUE)
     ) AS v(claim_type, limit_amount, is_active)
WHERE p.policy_number = 'POL-KL678';

-- ----------------------------------------------------------
-- CLAIMS
-- ----------------------------------------------------------
INSERT INTO claims (policy_id, claim_type, claim_amount, incident_date, description, status, created_at, updated_at)
SELECT p.policy_id,
       v.claim_type, v.claim_amount, v.incident_date::date,
       v.description, v.status, v.created_at::timestamp, v.updated_at::timestamp
FROM policies p,
     (VALUES
       ('POL-AB123', 'MEDICAL',  1500.00, '2026-03-01', 'Medical treatment after workplace accident requiring hospitalisation.',    'SUBMITTED', '2026-03-01 09:00:00', '2026-03-01 09:00:00'),
       ('POL-AB123', 'DENTAL',    800.00, '2026-02-15', 'Emergency dental surgery following trauma. Required immediate treatment.',  'IN_REVIEW', '2026-02-15 08:30:00', '2026-02-16 10:00:00'),
       ('POL-AB123', 'AUTO',     5000.00, '2026-01-10', 'Vehicle rear-ended at traffic light. Repairs completed at approved garage.', 'APPROVED',  '2026-01-10 14:00:00', '2026-01-12 11:00:00'),
       ('POL-AB123', 'HOME',    12000.00, '2025-12-01', 'Water damage to basement. Plumber inspection report attached.',             'REJECTED',  '2025-12-01 10:00:00', '2025-12-03 15:00:00'),
       ('POL-CD456', 'MEDICAL',  4999.99, '2026-03-05', 'Specialist consultation and follow-up treatment. Just under coverage limit.','SUBMITTED', '2026-03-05 12:00:00', '2026-03-05 12:00:00'),
       ('POL-AB123', 'LIFE',    50000.00, '2026-02-01', 'Life insurance benefit claim for critical illness diagnosis by specialist.',  'SUBMITTED', '2026-02-01 09:00:00', '2026-02-01 09:00:00'),
       ('POL-KL678', 'MEDICAL',     0.01, '2026-03-08', 'Minimal claim at boundary amount for system boundary testing purposes.',     'SUBMITTED', '2026-03-08 10:00:00', '2026-03-08 10:00:00')
     ) AS v(policy_number, claim_type, claim_amount, incident_date, description, status, created_at, updated_at)
WHERE p.policy_number = v.policy_number;

-- ----------------------------------------------------------
-- CLAIM HISTORY
-- ----------------------------------------------------------
INSERT INTO claim_history (claim_id, status, timestamp, reviewer_notes)
VALUES
-- Claim 1: SUBMITTED only
(1, 'SUBMITTED', '2026-03-01 09:00:00', NULL),
-- Claim 2: SUBMITTED → IN_REVIEW
(2, 'SUBMITTED', '2026-02-15 08:30:00', NULL),
(2, 'IN_REVIEW', '2026-02-16 10:00:00', 'Requesting additional documentation from claimant.'),
-- Claim 3: SUBMITTED → IN_REVIEW → APPROVED
(3, 'SUBMITTED', '2026-01-10 14:00:00', NULL),
(3, 'IN_REVIEW', '2026-01-11 09:00:00', 'Vehicle inspection report received and under assessment.'),
(3, 'APPROVED',  '2026-01-12 11:00:00', 'All documentation verified. Repair costs within coverage limit. Claim approved.'),
-- Claim 4: SUBMITTED → REJECTED
(4, 'SUBMITTED', '2025-12-01 10:00:00', NULL),
(4, 'REJECTED',  '2025-12-03 15:00:00', 'Claim amount exceeds documented damage repair estimate. Pre-existing damage noted. Claim rejected.'),
-- Claims 5, 6, 7: SUBMITTED only
(5, 'SUBMITTED', '2026-03-05 12:00:00', NULL),
(6, 'SUBMITTED', '2026-02-01 09:00:00', NULL),
(7, 'SUBMITTED', '2026-03-08 10:00:00', NULL);
```

---

## 7. Negative Test Data Set

| Test ID | Scenario | Data |
|---|---|---|
| NEG-01 | Submit claim with null policy number | `policyNumber: null` |
| NEG-02 | Submit claim with empty string policy number | `policyNumber: ""` |
| NEG-03 | Submit claim with null claim type | `claimType: null` |
| NEG-04 | Submit claim with zero amount | `claimAmount: 0` |
| NEG-05 | Submit claim with negative amount | `claimAmount: -100` |
| NEG-06 | Submit claim with null incident date | `incidentDate: null` |
| NEG-07 | Submit claim with null description | `description: null` |
| NEG-08 | Review claim with null action | `action: null` |
| NEG-09 | GET /claims/-1 | Negative claim ID |
| NEG-10 | GET /claims/abc | Non-numeric claim ID |
| NEG-11 | GET /policies/INVALID | Bad policy number format |
| NEG-12 | PATCH /claims/42/review twice | Second review on terminal-state claim |

---

## 8. Load/Volume Data Guidelines

| Entity | Dev Seed | Integration Test | Load Test |
|---|---|---|---|
| Policies | 7 | 50 | 10,000 |
| PolicyCoverages | ~30 | ~200 | 50,000 |
| Claims | 7 | 200 | 100,000 |
| ClaimHistory | 11 | 500 | 250,000 |

For load testing, use sequential policy numbers: `POL-AA001` through `POL-ZZ999` and generate claims via a data generation script.
