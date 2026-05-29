package dev.azhar.hishabi.calculators.tax.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.azhar.hishabi.calculators.tax.model.RuleSet;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationRequest.IncomeComponents;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class TaxCalculationServiceTest {

    private final TaxCalculationService service = new TaxCalculationService();

    @Test
    void totalEarningsSumsAllTenComponents() {
        IncomeComponents income =
                new IncomeComponents(
                        new BigDecimal("1000000"),
                        new BigDecimal("200000"),
                        new BigDecimal("50000"),
                        new BigDecimal("30000"),
                        new BigDecimal("10000"),
                        new BigDecimal("5000"),
                        new BigDecimal("100000"),
                        new BigDecimal("50000"),
                        new BigDecimal("20000"),
                        new BigDecimal("15000"));

        assertThat(service.totalEarnings(income)).isEqualByComparingTo("1480000.00");
    }

    @Test
    void salaryExemptionUsesOneThirdWhenBelowCap() {
        // 900,000 / 3 = 300,000 < 450,000 cap -> exemption = 300,000
        assertThat(service.salaryExemption(ruleSet(), new BigDecimal("900000.00")))
                .isEqualByComparingTo("300000.00");
    }

    @Test
    void salaryExemptionCapsWhenOneThirdExceedsCap() {
        // 1,611,000 / 3 = 537,000 > 450,000 -> exemption = 450,000
        assertThat(service.salaryExemption(ruleSet(), new BigDecimal("1611000.00")))
                .isEqualByComparingTo("450000.00");
    }

    @Test
    void salaryExemptionRoundsOneThirdToTwoDecimalHalfUp() {
        // 1,000,000 / 3 = 333,333,333... -> HALF_UP at 2dp -> 333,333.33 (still below cap)
        assertThat(service.salaryExemption(ruleSet(), new BigDecimal("1000000.00")))
                .isEqualByComparingTo("333333.33");

        // 129,630.7101 / 3 = 43210.2367 -> HALF_UP at 2dp -> 43210.24 (above cap)
        assertThat(service.salaryExemption(ruleSet(), new BigDecimal("129630.7101")))
                .isEqualByComparingTo("43210.24");
    }

    private static RuleSet ruleSet() {
        RuleSet rs = new RuleSet();
        rs.setSalaryExemptionCap(new BigDecimal(("450000.00")));
        rs.setSalaryExemptionDivisor(3);
        return rs;
    }
}
