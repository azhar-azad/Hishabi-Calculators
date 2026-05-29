package dev.azhar.hishabi.calculators.tax.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.azhar.hishabi.calculators.tax.model.CategoryThreshold;
import dev.azhar.hishabi.calculators.tax.model.RuleSet;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationRequest.IncomeComponents;
import dev.azhar.hishabi.calculators.tax.model.TaxpayerCategory;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    @ParameterizedTest
    @CsvSource({
        "GENERAL,                      350000.00",
        "WOMAN,                        400000.00",
        "SENIOR_65_PLUS,               400000.00",
        "PHYSICALLY_MENTALLY_DISABLED, 475000.00",
        "GAZETTED_FREEDOM_FIGHTER,     500000.00",
        "THIRD_GENDER,                 475000.00"
    })
    void effectiveThresholdMatchesEachCategoryWitNoDisabledChildren(
            TaxpayerCategory category, String expected) {
        assertThat(service.effectiveFirstSlabThreshold(ruleSetWithCategories(), category, 0))
                .isEqualByComparingTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"0, 350000.00", "1, 400000.00", "3, 500000.00"})
    void effectiveThresholdAddsFiftyThousandPerDisabledChild(
            int disabledChildren, String expected) {
        // GENERAL base 350,000 + 50,000 * disabledChildren
        assertThat(
                        service.effectiveFirstSlabThreshold(
                                ruleSetWithCategories(),
                                TaxpayerCategory.GENERAL,
                                disabledChildren))
                .isEqualByComparingTo(expected);
    }

    private static RuleSet ruleSet() {
        RuleSet rs = new RuleSet();
        rs.setSalaryExemptionCap(new BigDecimal(("450000.00")));
        rs.setSalaryExemptionDivisor(3);
        return rs;
    }

    private static RuleSet ruleSetWithCategories() {
        RuleSet rs = new RuleSet();
        rs.setDisabledChildThresholdBonus(new BigDecimal("50000.00"));
        rs.getCategoryThresholds().add(categoryThreshold(TaxpayerCategory.GENERAL, "350000.00"));
        rs.getCategoryThresholds().add(categoryThreshold(TaxpayerCategory.WOMAN, "400000.00"));
        rs.getCategoryThresholds()
                .add(categoryThreshold(TaxpayerCategory.SENIOR_65_PLUS, "400000.00"));
        rs.getCategoryThresholds()
                .add(categoryThreshold(TaxpayerCategory.PHYSICALLY_MENTALLY_DISABLED, "475000.00"));
        rs.getCategoryThresholds()
                .add(categoryThreshold(TaxpayerCategory.GAZETTED_FREEDOM_FIGHTER, "500000.00"));
        rs.getCategoryThresholds()
                .add(categoryThreshold(TaxpayerCategory.THIRD_GENDER, "475000.00"));
        return rs;
    }

    private static CategoryThreshold categoryThreshold(TaxpayerCategory category, String amount) {
        CategoryThreshold t = new CategoryThreshold();
        t.setCategory(category);
        t.setAmount(new BigDecimal(amount));
        return t;
    }
}
