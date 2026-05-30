package dev.azhar.hishabi.calculators.tax.service;

import dev.azhar.hishabi.calculators.tax.model.*;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationRequest.IncomeComponents;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationRequest.Investments;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationResponse.SlabTax;
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

    /**
     * Step 4a (PLAN.md #10.5): eligible investment = sum of each instrument, with Sanchay Patra and
     * DPS capped at their per-item limits first. The other five instruments are uncapped.
     */
    BigDecimal eligibleInvestment(RuleSet ruleSet, Investments inv) {
        BigDecimal sanchayPatra = inv.sanchayPatra().min(ruleSet.getSanchayPatraCap());
        BigDecimal dps = inv.dps().min(ruleSet.getDpsCap());
        return Money.scale(
                sanchayPatra
                        .add(dps)
                        .add(inv.mutualFund())
                        .add(inv.treasuryBond())
                        .add(inv.providentFundEmployee())
                        .add(inv.providentFundEmployer())
                        .add(inv.stock()));
    }

    /**
     * Step 4b (PLAN.md #10.5): rebate = MIN(rebateTaxableFraction x taxableIncome,
     * rebateEligibleFraction x eligibleInvestment, rebateCap). Zero when taxable income is
     * non-positive.
     */
    BigDecimal investmentRebate(
            RuleSet ruleSet, BigDecimal taxableIncome, BigDecimal eligibleInvestment) {
        if (taxableIncome.signum() <= 0) {
            return Money.scale(BigDecimal.ZERO);
        }
        BigDecimal byTaxable =
                Money.scale(taxableIncome.multiply(ruleSet.getRebateTaxableFraction()));
        BigDecimal byEligible =
                Money.scale(eligibleInvestment.multiply(ruleSet.getRebateEligibleFraction()));
        return byTaxable.min(byEligible).min(ruleSet.getRebateCap());
    }

    /** Step 5a (PLAN.md #10.6): {@code afterRebate = MAX(0, grossTax - rebate)}. */
    BigDecimal afterRebate(BigDecimal grossTax, BigDecimal rebate) {
        return Money.scale(grossTax.subtract(rebate).max(BigDecimal.ZERO));
    }

    /**
     * Step 5b (PLAN.md #10.6): apply the location's minimum tax floor - {@code withFloor =
     * (taxableIncome > 0) ? MAX(afterRebate, floor) : 0}. A taxpayer with no taxable income owes
     * nothing, even below the floor.
     */
    MinimumTaxResult applyMinimumTaxFloor(
            RuleSet ruleSet, Location location, BigDecimal taxableIncome, BigDecimal afterRebate) {

        BigDecimal floor = minimumTaxFloor(ruleSet, location);
        if (taxableIncome.signum() <= 0) {
            return new MinimumTaxResult(Money.scale(BigDecimal.ZERO), floor, false);
        }
        boolean applied = floor.compareTo(afterRebate) > 0;
        return new MinimumTaxResult(Money.scale(afterRebate.max(floor)), floor, applied);
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

    /** Look up a location's minimum tax floor within the rule set (PLAN.md #10.6) */
    private static BigDecimal minimumTaxFloor(RuleSet ruleSet, Location location) {
        return ruleSet.getMinimumTaxFloors().stream()
                .filter(f -> f.getLocation() == location)
                .findFirst()
                .map(MinimumTaxFloor::getAmount)
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "No minimum tax floor configured for " + location));
    }

    /** Result of the slab walk: gross tax plus the per-slab breakdown (band 0 through top slab). */
    record SlabWalkResult(BigDecimal grossTax, List<SlabTax> slabs) {}

    /**
     * Result of the minimum-tax-floor step: the floored tax, the floor value, and whether it bound.
     */
    record MinimumTaxResult(BigDecimal taxAfterFloor, BigDecimal floor, boolean applied) {}
}
