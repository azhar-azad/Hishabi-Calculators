-- V2: Seed the NBR individual income-tax rule set, shared by AY 2024-25 and
-- AY 2025-26 (NBR left the individual schedule unchanged — PLAN.md §10.0).
-- Every value is transcribed from PLAN.md §10. This is static reference data:
-- the rule set uses a fixed id (1) so the seed is deterministic and auditable.

INSERT INTO tax_rule_set (
    id, name,
    salary_exemption_cap, salary_exemption_divisor,   -- §10.2: MIN(total/3, 450,000)
    disabled_child_threshold_bonus,                    -- §10.3: +50,000 per disabled child
    rebate_taxable_fraction, rebate_eligible_fraction, rebate_cap -- §10.5
) VALUES (
             1, 'NBR individual schedule — AY 2024-25 & 2025-26 (unchanged)',
             450000.00, 3,
             50000.00,
             0.0300, 0.1500, 1000000.00
         );

-- Reference data is seeded with an explicit id; advance the identity sequence so
-- future IDENTITY inserts (e.g. in tests) don't collide with id = 1.
ALTER TABLE tax_rule_set ALTER COLUMN id RESTART WITH 2;

-- §10.4 — the 6 paying slabs (rows 2-7). The 0% threshold band (row 1) is
-- per-taxpayer (category + disabled children), computed at calc time, not stored.
-- ordinal 6 has NULL width = the open-ended "(rest)" top slab.
INSERT INTO tax_slab (rule_set_id, ordinal, width, rate) VALUES
                                                             (1, 1,  100000.00, 0.0500),   -- 5%
                                                             (1, 2,  400000.00, 0.1000),   -- 10%
                                                             (1, 3,  500000.00, 0.1500),   -- 15%
                                                             (1, 4,  500000.00, 0.2000),   -- 20%
                                                             (1, 5, 2000000.00, 0.2500),   -- 25%
                                                             (1, 6,       NULL, 0.3000);   -- 30% (rest)

-- §10.3 — first-slab (tax-free) threshold by taxpayer category.
INSERT INTO tax_category_threshold (rule_set_id, category, amount) VALUES
                                                                       (1, 'GENERAL',                      350000.00),
                                                                       (1, 'WOMAN',                        400000.00),
                                                                       (1, 'SENIOR_65_PLUS',               400000.00),
                                                                       (1, 'PHYSICALLY_MENTALLY_DISABLED', 475000.00),
                                                                       (1, 'GAZETTED_FREEDOM_FIGHTER',     500000.00),
                                                                       (1, 'THIRD_GENDER',                 475000.00);

-- §10.6 — minimum tax floor by location.
INSERT INTO tax_minimum_tax_floor (rule_set_id, location, amount) VALUES
                                                                      (1, 'DHAKA_CHITTAGONG_CITY_CORP', 5000.00),
                                                                      (1, 'OTHER_CITY_CORP',            4000.00),
                                                                      (1, 'OTHER',                      3000.00);

-- §10.0 — both assessment years reference the same rule set.
INSERT INTO tax_assessment_year (label, rule_set_id) VALUES
                                                         ('2024-25', 1),
                                                         ('2025-26', 1);