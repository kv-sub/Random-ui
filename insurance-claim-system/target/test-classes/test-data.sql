-- Test seed data for integration tests
INSERT INTO policies (policy_number, customer_id, status, effective_date, expiry_date, coverage_limit)
VALUES
    ('POL-10001', 1001, 'ACTIVE', '2025-01-01', '2026-12-31', 100000.00),
    ('POL-10002', 1002, 'ACTIVE', '2025-03-01', '2027-02-28', 75000.00),
    ('POL-10004', 1004, 'INACTIVE', '2023-01-01', '2024-12-31', 80000.00);

INSERT INTO policy_coverages (policy_id, claim_type, limit_amount, is_active)
SELECT p.policy_id, 'MEDICAL', 50000.00, TRUE FROM policies p WHERE p.policy_number = 'POL-10001'
UNION ALL
SELECT p.policy_id, 'DENTAL',  10000.00, TRUE FROM policies p WHERE p.policy_number = 'POL-10001'
UNION ALL
SELECT p.policy_id, 'MEDICAL', 40000.00, TRUE FROM policies p WHERE p.policy_number = 'POL-10002'
UNION ALL
SELECT p.policy_id, 'MEDICAL', 50000.00, TRUE FROM policies p WHERE p.policy_number = 'POL-10004';
