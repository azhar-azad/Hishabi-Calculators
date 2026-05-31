package dev.azhar.hishabi.calculators.tax.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full tax calculation breakdown (PLAN.md #10.8)
 *
 * <p>Mirrors the worked-example columns: earnings -> salary exemption -> taxable income -> per-slab
 * tax -> gross tax -> rebate -> after-rebate -> minimum-tax floor AIT credit -> net tax.
 */
public record TaxCalculationResponse(
        String assessmentYear,
        BigDecimal totalEarnings,
        BigDecimal taxFreeSalaryExemption,
        BigDecimal taxableIncome,
        BigDecimal effectiveFirstSlabThreshold,
        List<SlabTax> slabs,
        BigDecimal grossTax,
        BigDecimal eligibleInvestment,
        BigDecimal rebate,
        BigDecimal afterRebate,
        BigDecimal minimumTaxFloor,
        boolean minimumTaxApplied,
        BigDecimal taxAfterFloor,
        BigDecimal advanceIncomeTaxPaid,
        BigDecimal netTax) {
    /**
     * One row of the slab-by-slab breakdown (PLAN.md #10.4 / #10.8). Includes the synthesized 0%
     * threshold band as ordinal 0 (rate 0, tax 0) so the UI can show the full ladder.
     */
    public record SlabTax(
            int ordinal, BigDecimal rate, BigDecimal taxableAmountInSlab, BigDecimal tax) {}
}
