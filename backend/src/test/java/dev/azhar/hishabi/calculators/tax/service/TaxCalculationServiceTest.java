package dev.azhar.hishabi.calculators.tax.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.azhar.hishabi.calculators.tax.model.*;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationRequest.IncomeComponents;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationRequest.Investments;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationResponse.SlabTax;
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

    @Test
    void slabWalkLowIncomeStaysInZeroPercentBand() {
        // taxable 200,000 < 300,000 threshold -> entirely in band 0, no tax
        var r =
                service.walkSlabs(
                        slabRuleSet(), new BigDecimal("350000.00"), new BigDecimal("200000.00"));
        assertThat(r.grossTax()).isEqualByComparingTo("0.00");
        assertThat(r.slabs().getFirst().taxableAmountInSlab()).isEqualByComparingTo("200000.00");
    }

    @Test
    void slabWalkWorkedExampleMidIncome() {
        // taxable 1,161,000, threshold 350,000 -> gross 91,650
        var r =
                service.walkSlabs(
                        slabRuleSet(), new BigDecimal("350000.00"), new BigDecimal("1161000.00"));

        assertThat(r.grossTax()).isEqualByComparingTo("91650.00");
        assertSlab(r.slabs().get(0), 0, "350000.00", "0.00"); // 0% band
        assertSlab(r.slabs().get(1), 1, "100000.00", "5000.00"); // 5%
        assertSlab(r.slabs().get(2), 2, "400000.00", "40000.00"); // 10%
        assertSlab(r.slabs().get(3), 3, "311000.00", "46650.00"); // 15% (partial)
        assertThat(r.slabs().get(4).taxableAmountInSlab()).isEqualByComparingTo("0.00");
    }

    @Test
    void slabWalkHighIncomeSpansAllSlabs() {
        // taxable 5,000,000, threshold 350,000 → gross 1,065,000; top slab absorbs the rest
        var r =
                service.walkSlabs(
                        slabRuleSet(), new BigDecimal("350000.00"), new BigDecimal("5000000.00"));
        assertThat(r.grossTax()).isEqualByComparingTo("1065000.00");
        assertSlab(r.slabs().get(6), 6, "1150000.00", "345000.00"); // (rest) @ 30%
    }

    @Test
    void slabWalkZeroTaxableIncomeProducesNoTax() {
        var r = service.walkSlabs(slabRuleSet(), new BigDecimal("350000.00"), BigDecimal.ZERO);
        assertThat(r.grossTax()).isEqualByComparingTo("0.00");
    }

    @Test
    void eligibleInvestmentAppliesPerItemCaps() {
        // Sanchay 600k -> capped 500k; DPS 200k -> capped 120k; others uncapped
        Investments inv =
                investments("600000", "200000", "50000", "30000", "10000", "20000", "40000");
        // 500000 + 120000 + 50000 + 30000 + 10000 + 20000 + 40000 = 770000
        assertThat(service.eligibleInvestment(rebateRuleSet(), inv))
                .isEqualByComparingTo("770000.00");
    }

    @Test
    void rebateThreePercentLegBinds() {
        // §10.8: taxable 1,161,000 (3% = 34,830), eligible 320,000 (15% = 48,000) -> 34,830
        assertThat(
                        service.investmentRebate(
                                rebateRuleSet(),
                                new BigDecimal("1161000.00"),
                                new BigDecimal("320000.00")))
                .isEqualByComparingTo("34830.00");
    }

    @Test
    void rebateFifteenPercentLegBinds() {
        // taxable 1,161,000 (3% = 34,830), eligible 100,000 (15% = 15,000) -> 15,000
        assertThat(
                        service.investmentRebate(
                                rebateRuleSet(),
                                new BigDecimal("1161000.00"),
                                new BigDecimal("100000.00")))
                .isEqualByComparingTo("15000.00");
    }

    @Test
    void rebateOneMillionCapBinds() {
        // taxable 100,000,000 (3% = 3,000,000), eligible 50,000,000 (15% = 7,500,000) -> cap
        // 1,000,000
        assertThat(
                        service.investmentRebate(
                                rebateRuleSet(),
                                new BigDecimal("100000000.00"),
                                new BigDecimal("50000000.00")))
                .isEqualByComparingTo("1000000.00");
    }

    @Test
    void rebateIsZeroWhenTaxableIncomeNonPositive() {
        assertThat(
                        service.investmentRebate(
                                rebateRuleSet(), BigDecimal.ZERO, new BigDecimal("320000.00")))
                .isEqualByComparingTo("0.00");
        assertThat(
                        service.investmentRebate(
                                rebateRuleSet(),
                                new BigDecimal("-50000.00"),
                                new BigDecimal("320000.00")))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void afterRebateSubtractsRebateAndFloorsAtZero() {
        // worked example: gross 91,650 − rebate 34,830 = 56,820
        assertThat(service.afterRebate(new BigDecimal("91650.00"), new BigDecimal("34830.00")))
                .isEqualByComparingTo("56820.00");
        // a rebate exceeding gross tax cannot push tax negative
        assertThat(service.afterRebate(new BigDecimal("3000.00"), new BigDecimal("5000.00")))
                .isEqualByComparingTo("0.00");
    }

    @ParameterizedTest
    @CsvSource({
        "DHAKA_CHITTAGONG_CITY_CORP, 5000.00",
        "OTHER_CITY_CORP,            4000.00",
        "OTHER,                      3000.00"
    })
    void minimumTaxFloorBumpsBelowFloorTaxPerLocation(Location location, String expectedFloor) {
        // afterRebate 1,000 is below every floor; taxable > 0 → bumped up to the location floor
        var r =
                service.applyMinimumTaxFloor(
                        floorRuleSet(),
                        location,
                        new BigDecimal("1000000.00"),
                        new BigDecimal("1000.00"));
        assertThat(r.taxAfterFloor()).isEqualByComparingTo(expectedFloor);
        assertThat(r.applied()).isTrue();
    }

    @Test
    void minimumTaxFloorNotAppliedWhenAfterRebateExceedsFloor() {
        // worked example: afterRebate 56,820 > Dhaka floor 5,000 → unchanged
        var r =
                service.applyMinimumTaxFloor(
                        floorRuleSet(),
                        Location.DHAKA_CHITTAGONG_CITY_CORP,
                        new BigDecimal("1161000.00"),
                        new BigDecimal("56820.00"));
        assertThat(r.taxAfterFloor()).isEqualByComparingTo("56820.00");
        assertThat(r.applied()).isFalse();
    }

    @Test
    void minimumTaxFloorNotAppliedWhenNoTaxableIncome() {
        // taxable 0 → 0 owed, regardless of afterRebate
        var r =
                service.applyMinimumTaxFloor(
                        floorRuleSet(),
                        Location.DHAKA_CHITTAGONG_CITY_CORP,
                        BigDecimal.ZERO,
                        new BigDecimal("1000.00"));
        assertThat(r.taxAfterFloor()).isEqualByComparingTo("0.00");
        assertThat(r.applied()).isFalse();
    }

    @Test
    void netTaxSubtractsAitCredit() {
        // AIT below tax: net = tax − AIT
        assertThat(service.netTax(new BigDecimal("56820.00"), new BigDecimal("20000.00")))
                .isEqualByComparingTo("36820.00");
    }

    @Test
    void netTaxIsZeroWhenAitExceedsTaxNoRefund() {
        // AIT above tax: net = 0 (no refund modeled)
        assertThat(service.netTax(new BigDecimal("56820.00"), new BigDecimal("80000.00")))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void calculateWiresAllStepsTogether() {
        // Simple scenario (full §10.8 reproduction is slice 3.12): basic 900,000, GENERAL, OTHER,
        // no children, no investments, no AIT.
        // exemption 300,000 → taxable 600,000; band0 350k@0 + 100k@5%(5,000) + 150k@10%(15,000) =
        // gross 20,000; no rebate; floor 3,000 not binding; net 20,000.
        TaxCalculationRequest request =
                new TaxCalculationRequest(
                        "2025-26",
                        TaxpayerCategory.GENERAL,
                        Location.OTHER,
                        0,
                        basicOnly("900000"),
                        noInvestments(),
                        BigDecimal.ZERO);

        TaxCalculationResponse resp = service.calculate(fullRuleSet(), "2025-26", request);

        assertThat(resp.assessmentYear()).isEqualTo("2025-26");
        assertThat(resp.totalEarnings()).isEqualByComparingTo("900000.00");
        assertThat(resp.taxFreeSalaryExemption()).isEqualByComparingTo("300000.00");
        assertThat(resp.taxableIncome()).isEqualByComparingTo("600000.00");
        assertThat(resp.effectiveFirstSlabThreshold()).isEqualByComparingTo("350000.00");
        assertThat(resp.grossTax()).isEqualByComparingTo("20000.00");
        assertThat(resp.rebate()).isEqualByComparingTo("0.00");
        assertThat(resp.afterRebate()).isEqualByComparingTo("20000.00");
        assertThat(resp.minimumTaxApplied()).isFalse();
        assertThat(resp.netTax()).isEqualByComparingTo("20000.00");
        assertThat(resp.slabs()).hasSize(7); // band 0 + 6 paying slabs
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

    private static RuleSet slabRuleSet() {
        RuleSet rs = new RuleSet();
        rs.getSlabs().add(slab(1, "100000.00", "0.0500"));
        rs.getSlabs().add(slab(2, "400000.00", "0.1000"));
        rs.getSlabs().add(slab(3, "500000.00", "0.1500"));
        rs.getSlabs().add(slab(4, "500000.00", "0.2000"));
        rs.getSlabs().add(slab(5, "2000000.00", "0.2500"));
        rs.getSlabs().add(slab(6, null, "0.3000"));
        return rs;
    }

    private static RuleSet rebateRuleSet() {
        RuleSet rs = new RuleSet();
        rs.setRebateTaxableFraction(new BigDecimal("0.0300"));
        rs.setRebateEligibleFraction(new BigDecimal("0.1500"));
        rs.setRebateCap(new BigDecimal("1000000.00"));
        rs.setSanchayPatraCap(new BigDecimal("500000.00"));
        rs.setDpsCap(new BigDecimal("120000.00"));
        return rs;
    }

    private static RuleSet floorRuleSet() {
        RuleSet rs = new RuleSet();
        rs.getMinimumTaxFloors().add(floor(Location.DHAKA_CHITTAGONG_CITY_CORP, "5000.00"));
        rs.getMinimumTaxFloors().add(floor(Location.OTHER_CITY_CORP, "4000.00"));
        rs.getMinimumTaxFloors().add(floor(Location.OTHER, "3000.00"));
        return rs;
    }

    private static RuleSet fullRuleSet() {
        RuleSet rs = new RuleSet();
        rs.setSalaryExemptionCap(new BigDecimal("450000.00"));
        rs.setSalaryExemptionDivisor(3);
        rs.setDisabledChildThresholdBonus(new BigDecimal("50000.00"));
        rs.setRebateTaxableFraction(new BigDecimal("0.0300"));
        rs.setRebateEligibleFraction(new BigDecimal("0.1500"));
        rs.setRebateCap(new BigDecimal("1000000.00"));
        rs.setSanchayPatraCap(new BigDecimal("500000.00"));
        rs.setDpsCap(new BigDecimal("120000.00"));
        rs.getSlabs().add(slab(1, "100000.00", "0.0500"));
        rs.getSlabs().add(slab(2, "400000.00", "0.1000"));
        rs.getSlabs().add(slab(3, "500000.00", "0.1500"));
        rs.getSlabs().add(slab(4, "500000.00", "0.2000"));
        rs.getSlabs().add(slab(5, "2000000.00", "0.2500"));
        rs.getSlabs().add(slab(6, null, "0.3000"));
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
        rs.getMinimumTaxFloors().add(floor(Location.DHAKA_CHITTAGONG_CITY_CORP, "5000.00"));
        rs.getMinimumTaxFloors().add(floor(Location.OTHER_CITY_CORP, "4000.00"));
        rs.getMinimumTaxFloors().add(floor(Location.OTHER, "3000.00"));
        return rs;
    }

    private static CategoryThreshold categoryThreshold(TaxpayerCategory category, String amount) {
        CategoryThreshold t = new CategoryThreshold();
        t.setCategory(category);
        t.setAmount(new BigDecimal(amount));
        return t;
    }

    private static TaxSlab slab(int ordinal, String width, String rate) {
        TaxSlab s = new TaxSlab();
        s.setOrdinal(ordinal);
        s.setWidth(width == null ? null : new BigDecimal(width));
        s.setRate(new BigDecimal(rate));
        return s;
    }

    private static void assertSlab(SlabTax slab, int ordinal, String taxableAmount, String tax) {
        assertThat(slab.ordinal()).isEqualTo(ordinal);
        assertThat(slab.taxableAmountInSlab()).isEqualByComparingTo(taxableAmount);
        assertThat(slab.tax()).isEqualByComparingTo(tax);
    }

    private static MinimumTaxFloor floor(Location location, String amount) {
        MinimumTaxFloor f = new MinimumTaxFloor();
        f.setLocation(location);
        f.setAmount(new BigDecimal(amount));
        return f;
    }

    private static IncomeComponents basicOnly(String basic) {
        BigDecimal z = BigDecimal.ZERO;
        return new IncomeComponents(new BigDecimal(basic), z, z, z, z, z, z, z, z, z);
    }

    private static Investments noInvestments() {
        BigDecimal z = BigDecimal.ZERO;
        return new Investments(z, z, z, z, z, z, z);
    }

    private static Investments investments(
            String sanchayPatra,
            String dps,
            String mutualFund,
            String treasuryBond,
            String pfEmployee,
            String pfEmployer,
            String stock) {
        return new Investments(
                new BigDecimal(sanchayPatra),
                new BigDecimal(dps),
                new BigDecimal(mutualFund),
                new BigDecimal(treasuryBond),
                new BigDecimal(pfEmployee),
                new BigDecimal(pfEmployer),
                new BigDecimal(stock));
    }
}
