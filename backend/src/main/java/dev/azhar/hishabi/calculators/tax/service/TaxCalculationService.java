package dev.azhar.hishabi.calculators.tax.service;

import dev.azhar.hishabi.calculators.tax.model.CategoryThreshold;
import dev.azhar.hishabi.calculators.tax.model.RuleSet;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationRequest.IncomeComponents;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationResponse.SlabTax;
import dev.azhar.hishabi.calculators.tax.model.TaxSlab;
import dev.azhar.hishabi.calculators.tax.model.TaxpayerCategory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Pure-function Bangladeshi income-tax calculation (PLAN.md #10). Operates on a resolved {@link
 * RuleSet} (loaded by the controller) plus the request DTO; performs no database access itself.
 *
 * <p>Built up step-by-step across slices 3.6-3.11. This slice covers step 1 (salary exemption).
 */
@Service
public class TaxCalculationService {

    /** Sum of all annual salary income components (PLAN.md #10.1), at monetary scale. */
    BigDecimal totalEarnings(IncomeComponents income) {
        return Money.scale(
                income.basic()
                        .add(income.houseRent())
                        .add(income.conveyance())
                        .add(income.medicalAllowance())
                        .add(income.leaveEncashment())
                        .add(income.performanceBonus())
                        .add(income.yearlyBonus())
                        .add(income.festivalBonus())
                        .add(income.overtime())
                        .add(income.transportation()));
    }

    /**
     * Step 1 (PLAN.md #10.2): {@code taxFreeSalary = MIN(totalEarnings / divisor, cap)}. The
     * post-2023 unified exemption; the old per-component exemptions no longer apply individually.
     */
    BigDecimal salaryExemption(RuleSet ruleSet, BigDecimal totalEarnings) {
        BigDecimal byFormula =
                Money.divide(
                        totalEarnings, BigDecimal.valueOf(ruleSet.getSalaryExemptionDivisor()));
        return byFormula.min(ruleSet.getSalaryExemptionCap());
    }

    /**
     * Step 2 (PLAN.md #10.3): the effective first-slab (tax-free) threshold for this taxpayer =
     * {@code categoryThreshold + disabledChildThresholdBonus * disabledChildren}.
     */
    BigDecimal effectiveFirstSlabThreshold(
            RuleSet ruleSet, TaxpayerCategory category, int disabledChildren) {
        BigDecimal base = categoryThreshold(ruleSet, category);
        BigDecimal bonus =
                ruleSet.getDisabledChildThresholdBonus()
                        .multiply(BigDecimal.valueOf(disabledChildren));
        return Money.scale(base.add(bonus));
    }

    /**
     * Step 3 (PLAN.md #10.4): walk the slab ladder in order, taxing {@code MIN(remaining, width) *
     * rate}. Band 0 is the synthesized 0% tax-free band (width = effective first-slab threshold);
     * the sored paying slabs follow. The top slab has a null width (open-ended) and absorbs the
     * reminder. Returns the per-slab breakdown (band 0 through the top slab) and the summed gross
     * tax. A non-positive taxable income yields an all-zero breakdown.
     */
    SlabWalkResult walkSlabs(
            RuleSet ruleSet, BigDecimal effectiveThreshold, BigDecimal taxableIncome) {

        List<SlabTax> breakdown = new ArrayList<>();
        BigDecimal grossTax = BigDecimal.ZERO;
        BigDecimal remaining = taxableIncome.max(BigDecimal.ZERO);

        // Band 0 - the tax-free 0% band; width = effective first-slab threshold.
        BigDecimal zeroBandAmount = remaining.min(effectiveThreshold);
        breakdown.add(
                new SlabTax(
                        0,
                        BigDecimal.ZERO,
                        Money.scale(zeroBandAmount),
                        Money.scale(BigDecimal.ZERO)));
        remaining = remaining.subtract(zeroBandAmount);

        // Paying slabs in ordinal order; a null width marks the open-ended top slab.
        for (TaxSlab slab : ruleSet.getSlabs()) {
            BigDecimal amountInSlab =
                    (slab.getWidth() == null ? remaining : remaining.min(slab.getWidth()));
            BigDecimal tax = Money.scale(amountInSlab.multiply(slab.getRate()));
            breakdown.add(
                    new SlabTax(slab.getOrdinal(), slab.getRate(), Money.scale(amountInSlab), tax));
            grossTax = grossTax.add(tax);
            remaining = remaining.subtract(amountInSlab);
        }
        return new SlabWalkResult(Money.scale(grossTax), breakdown);
    }

    /** Look up a category's tax-free threshold within the rule set (PLAN.md #10.3). */
    private BigDecimal categoryThreshold(RuleSet ruleSet, TaxpayerCategory category) {
        return ruleSet.getCategoryThresholds().stream()
                .filter(t -> t.getCategory() == category)
                .findFirst()
                .map(CategoryThreshold::getAmount)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "No category threshold configured for " + category));
    }

    /** Result of the slab walk: gross tax plus the per-slab breakdown (band 0 through top slab). */
    record SlabWalkResult(BigDecimal grossTax, List<SlabTax> slabs) {}
}
