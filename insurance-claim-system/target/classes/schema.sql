-- ============================================================
-- Insurance Claim Submission System - PostgreSQL Schema
-- ============================================================

CREATE TABLE IF NOT EXISTS policies (
    policy_id      BIGSERIAL PRIMARY KEY,
    policy_number  VARCHAR(20)    NOT NULL UNIQUE,
    customer_id    BIGINT         NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    effective_date DATE           NOT NULL,
    expiry_date    DATE           NOT NULL,
    coverage_limit NUMERIC(15, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS policy_coverages (
    coverage_id  BIGSERIAL PRIMARY KEY,
    policy_id    BIGINT         NOT NULL REFERENCES policies (policy_id) ON DELETE CASCADE,
    claim_type   VARCHAR(20)    NOT NULL,
    limit_amount NUMERIC(15, 2) NOT NULL,
    is_active    BOOLEAN        NOT NULL DEFAULT TRUE,
    UNIQUE (policy_id, claim_type)
);

CREATE TABLE IF NOT EXISTS claims (
    claim_id      BIGSERIAL PRIMARY KEY,
    policy_id     BIGINT         NOT NULL REFERENCES policies (policy_id) ON DELETE RESTRICT,
    claim_type    VARCHAR(20)    NOT NULL,
    claim_amount  NUMERIC(15, 2) NOT NULL,
    incident_date DATE           NOT NULL,
    description   TEXT,
    status        VARCHAR(20)    NOT NULL DEFAULT 'SUBMITTED',
    created_at    TIMESTAMP      NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP      NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_claims_policy_id ON claims (policy_id);
CREATE INDEX IF NOT EXISTS idx_claims_status ON claims (status);
CREATE INDEX IF NOT EXISTS idx_claims_duplicate ON claims (policy_id, claim_type, incident_date, created_at);

CREATE TABLE IF NOT EXISTS claim_history (
    history_id     BIGSERIAL PRIMARY KEY,
    claim_id       BIGINT      NOT NULL REFERENCES claims (claim_id) ON DELETE CASCADE,
    status         VARCHAR(20) NOT NULL,
    timestamp      TIMESTAMP   NOT NULL DEFAULT NOW(),
    reviewer_notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_claim_history_claim_id ON claim_history (claim_id);

-- ============================================================
-- Sample seed data (5 policies for demo/testing)
-- ============================================================

INSERT INTO policies (policy_number, customer_id, status, effective_date, expiry_date, coverage_limit)
VALUES
    ('POL-10001', 1001, 'ACTIVE', '2025-01-01', '2026-12-31', 100000.00),
    ('POL-10002', 1002, 'ACTIVE', '2025-03-01', '2027-02-28', 75000.00),
    ('POL-10003', 1003, 'ACTIVE', '2024-06-01', '2026-05-31', 50000.00),
    ('POL-10004', 1004, 'INACTIVE', '2023-01-01', '2024-12-31', 80000.00),
    ('POL-10005', 1005, 'ACTIVE', '2025-07-01', '2027-06-30', 200000.00)
ON CONFLICT (policy_number) DO NOTHING;

INSERT INTO policy_coverages (policy_id, claim_type, limit_amount, is_active)
SELECT p.policy_id, 'MEDICAL',    50000.00, TRUE FROM policies p WHERE p.policy_number = 'POL-10001'
UNION ALL
SELECT p.policy_id, 'DENTAL',     10000.00, TRUE FROM policies p WHERE p.policy_number = 'POL-10001'
UNION ALL
SELECT p.policy_id, 'VISION',      5000.00, TRUE FROM policies p WHERE p.policy_number = 'POL-10001'
UNION ALL
SELECT p.policy_id, 'MEDICAL',    40000.00, TRUE FROM policies p WHERE p.policy_number = 'POL-10002'
UNION ALL
SELECT p.policy_id, 'DENTAL',      8000.00, TRUE FROM policies p WHERE p.policy_number = 'POL-10002'
UNION ALL
SELECT p.policy_id, 'MEDICAL',    30000.00, TRUE FROM policies p WHERE p.policy_number = 'POL-10003'
UNION ALL
SELECT p.policy_id, 'AUTO',       20000.00, TRUE FROM policies p WHERE p.policy_number = 'POL-10003'
UNION ALL
SELECT p.policy_id, 'MEDICAL',    50000.00, TRUE FROM policies p WHERE p.policy_number = 'POL-10004'
UNION ALL
SELECT p.policy_id, 'MEDICAL',   100000.00, TRUE FROM policies p WHERE p.policy_number = 'POL-10005'
UNION ALL
SELECT p.policy_id, 'DENTAL',     15000.00, TRUE FROM policies p WHERE p.policy_number = 'POL-10005'
UNION ALL
SELECT p.policy_id, 'VISION',     10000.00, TRUE FROM policies p WHERE p.policy_number = 'POL-10005'
UNION ALL
SELECT p.policy_id, 'HOME',       80000.00, TRUE FROM policies p WHERE p.policy_number = 'POL-10005'
ON CONFLICT (policy_id, claim_type) DO NOTHING;
