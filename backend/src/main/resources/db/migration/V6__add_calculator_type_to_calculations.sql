-- V6: add calculator_type discriminator; drop tax-specific assessment_year.
-- assessment_year is retained in request_json (DTO snapshot), so no data is lost.
-- Statements split for H2 compatibility (H2 does not support combined ADD+DROP in one ALTER).
ALTER TABLE calculations ADD COLUMN calculator_type VARCHAR(20) NOT NULL DEFAULT 'TAX';
ALTER TABLE calculations ALTER COLUMN calculator_type DROP DEFAULT;
ALTER TABLE calculations DROP COLUMN assessment_year;