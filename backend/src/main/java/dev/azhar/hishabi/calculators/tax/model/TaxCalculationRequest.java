package dev.azhar.hishabi.calculators.tax.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Request payload for POST /api/calculators/tax/calculate (PLAN.md #10.1).
 *
 * <p>assessmentYear is optional - null lets the controller resolve the latest available year.
 * advanceIncomeTaxPaid is the AIT withholding credit (#10.7). All money fields are annual BDT.
 */
public record TaxCalculationRequest(
        String assessmentYear,
        @NotNull TaxpayerCategory category,
        @NotNull Location location,
        @NotNull @PositiveOrZero Integer disabledChildren,
        @NotNull @Valid IncomeComponents income,
        @NotNull @Valid Investments investments,
        @NotNull @PositiveOrZero BigDecimal advanceIncomeTaxPaid) {

    /** Annual salary income components in BDT (PLAN.md #10.1). */
    public record IncomeComponents(
            @NotNull @PositiveOrZero BigDecimal basic,
            @NotNull @PositiveOrZero BigDecimal houseRent,
            @NotNull @PositiveOrZero BigDecimal conveyance,
            @NotNull @PositiveOrZero BigDecimal medicalAllowance,
            @NotNull @PositiveOrZero BigDecimal leaveEncashment,
            @NotNull @PositiveOrZero BigDecimal performanceBonus,
            @NotNull @PositiveOrZero BigDecimal yearlyBonus,
            @NotNull @PositiveOrZero BigDecimal festivalBonus,
            @NotNull @PositiveOrZero BigDecimal overtime,
            @NotNull @PositiveOrZero BigDecimal transportation) {}

    /** Investment amounts by instrument in BDT; per-item caps applied server-side (#10.5). */
    public record Investments(
            @NotNull @PositiveOrZero BigDecimal sanchayPatra,
            @NotNull @PositiveOrZero BigDecimal dps,
            @NotNull @PositiveOrZero BigDecimal mutualFund,
            @NotNull @PositiveOrZero BigDecimal treasuryBond,
            @NotNull @PositiveOrZero BigDecimal providentFundEmployee,
            @NotNull @PositiveOrZero BigDecimal providentFundEmployer,
            @NotNull @PositiveOrZero BigDecimal stock) {}
}
