package dev.azhar.hishabi.calculators.tax.service;

import dev.azhar.hishabi.calculators.tax.model.RuleSet;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationRequest.IncomeComponents;
import java.math.BigDecimal;
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
}
