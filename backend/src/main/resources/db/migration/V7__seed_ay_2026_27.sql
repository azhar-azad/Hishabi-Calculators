-- V7: Seed the AY 2026-27 NBR individual income-tax rule set (PLAN.md §12).
-- AY 2026-27 differs from AY 2024-25 / 2025-26 (id=1, §10): the General
-- threshold rises to 375,000, the slab ladder changes (no 5% band, 35% top
-- rate), the rebate eligible-investment fraction drops to 10% and the rebate
-- cap drops to 750,000. It therefore gets its own rule set (id=2).

INSERT INTO tax_rule_set (
    id, name,
    salary_exemption_cap, salary_exemption_divisor,    -- §10.2: MIN(total/3, 450,000)
    disabled_child_threshold_bonus,                     -- §10.3: +50,000 per disabled child
    rebate_taxable_fraction, rebate_eligible_fraction, rebate_cap, -- §12.1
    sanchay_patra_cap, dps_cap                          -- §10.5 (added in V3 — must set explicitly)
) VALUES (
             2, 'NBR individual schedule — AY 2026-27',
             450000.00, 3,
             50000.00,
             0.0300, 0.1000, 750000.00,
             500000.00, 120000.00
         );

-- Reference data seeded with an explicit id; advance the identity sequence past id = 2
-- so future IDENTITY inserts (e.g. in tests) don't collide with id = 2.
ALTER TABLE tax_rule_set ALTER COLUMN id RESTART WITH 3;

-- §12.2 — the 6 paying slabs. The 0% threshold band is per-taxpayer, computed at
-- calc time, not stored. ordinal 6 has NULL width = the open-ended "(rest)" top slab.
INSERT INTO tax_slab (rule_set_id, ordinal, width, rate) VALUES
                                                             (2, 1,  300000.00, 0.1000),   -- 10%
                                                             (2, 2,  400000.00, 0.1500),   -- 15%
                                                             (2, 3,  500000.00, 0.2000),   -- 20%
                                                             (2, 4,  425000.00, 0.2500),   -- 25%
                                                             (2, 5, 2000000.00, 0.3000),   -- 30%
                                                             (2, 6,       NULL, 0.3500);   -- 35% (rest)

-- §10.3 / §12.1 — first-slab (tax-free) threshold by taxpayer category (General 375,000).
INSERT INTO tax_category_threshold (rule_set_id, category, amount) VALUES
                                                                       (2, 'GENERAL',                      375000.00),
                                                                       (2, 'WOMAN',                        400000.00),
                                                                       (2, 'SENIOR_65_PLUS',               400000.00),
                                                                       (2, 'PHYSICALLY_MENTALLY_DISABLED', 475000.00),
                                                                       (2, 'GAZETTED_FREEDOM_FIGHTER',     500000.00),
                                                                       (2, 'THIRD_GENDER',                 475000.00);

-- §10.6 — minimum tax floor by location (unchanged from AY 2025-26).
INSERT INTO tax_minimum_tax_floor (rule_set_id, location, amount) VALUES
                                                                      (2, 'DHAKA_CHITTAGONG_CITY_CORP', 5000.00),
                                                                      (2, 'OTHER_CITY_CORP',            4000.00),
                                                                      (2, 'OTHER',                      3000.00);

-- §12 — AY 2026-27 references its own rule set (id=2).
INSERT INTO tax_assessment_year (label, rule_set_id) VALUES
                                                         ('2026-27', 2);