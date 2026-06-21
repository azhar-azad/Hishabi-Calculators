-- V9: Zakat rule set + initial metal price snapshot (PLAN.md §13).
-- Silver price (340.87 BDT/g) calibrated so silver nisab ≈ BDT 173,250
-- (As-Sunnah published figure, 20-Jun-2026). Metal prices are refreshed
-- daily by BajusPriceService once Slice 8.2 deploys.

INSERT INTO zakat_rule_set (id, name, nisab_silver_grams, nisab_gold_grams,
                            resale_factor, rate_lunar, rate_solar)
VALUES (1, 'Zakat rules (As-Sunnah calibration, 2026)',
        612.3600, 87.4800, 0.8300, 0.0250, 0.0260);

ALTER TABLE zakat_rule_set ALTER COLUMN id RESTART WITH 2;

INSERT INTO metal_price (metal, carat, price_per_gram, source, fetched_at) VALUES
    ('SILVER', 'STANDARD', 340.87, 'V9 seed (BAJUS 2026-06-21)', '2026-06-21 00:00:00+00'),
    ('GOLD',   '22',      9700.00, 'V9 seed (BAJUS 2026-06-21)', '2026-06-21 00:00:00+00'),
    ('GOLD',   '21',      9275.00, 'V9 seed (BAJUS 2026-06-21)', '2026-06-21 00:00:00+00'),
    ('GOLD',   '18',      7950.00, 'V9 seed (BAJUS 2026-06-21)', '2026-06-21 00:00:00+00');