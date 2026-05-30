-- V3: per-item investment caps for the rebate calc (PLAN.md §10.5). Only Sanchay Patra and DPS
-- are capped; the other five instruments are uncapped. Added as RuleSet scalars so they live as
-- data alongside the rebate fractions (rule 6: tax rules are data, not code).
-- Add nullable, backfill the seeded rule set, then enforce NOT NULL.
ALTER TABLE tax_rule_set ADD COLUMN sanchay_patra_cap NUMERIC(15, 2);
ALTER TABLE tax_rule_set ADD COLUMN dps_cap NUMERIC(15, 2);

UPDATE tax_rule_set SET sanchay_patra_cap = 500000.00, dps_cap = 120000.00;

ALTER TABLE tax_rule_set ALTER COLUMN sanchay_patra_cap SET NOT NULL;
ALTER TABLE tax_rule_set ALTER COLUMN dps_cap SET NOT NULL;