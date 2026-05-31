package dev.azhar.hishabi.calculators.tax.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * The full rule set backing an assessment year (PLAN.md #10): exemption + rebate configuration with
 * per-item investment caps, the paying slabs (#10.4), per-category tax-free thresholds (#10.3), and
 * per-location minimum-tax floors (#10.6). Read-only reference data for the calculator UI.
 *
 * <p>The synthesized 0% threshold band is intentionally absent: it is per-taxpayer (category +
 * disabled-child count) and computed at calculation time, not part of the stored rules.
 */
public record TaxRulesResponse(
        String assessmentYear,
        String ruleSetName,
        BigDecimal salaryExemptionCap,
        int salaryExemptionDivisor,
        BigDecimal disabledChildThresholdBonus,
        BigDecimal rebateTaxableFraction,
        BigDecimal rebateEligibleFraction,
        BigDecimal rebateCap,
        BigDecimal sanchayPatraCap,
        BigDecimal dpsCap,
        List<Slab> slabs,
        List<Threshold> categoryThresholds,
        List<Floor> minimumTaxFloors) {
    /** One paying slab (PLAN.md #10.4). {@code width} is null for the open-ended top slab. */
    public record Slab(int ordinal, BigDecimal width, BigDecimal rate) {}

    /** Tax-free threshold for a taxpayer category (PLAN.md #10.3). */
    public record Threshold(TaxpayerCategory category, BigDecimal amount) {}

    /** Minimum-tax floor for a location (PLAN.md #10.6). */
    public record Floor(Location location, BigDecimal amount) {}
}
