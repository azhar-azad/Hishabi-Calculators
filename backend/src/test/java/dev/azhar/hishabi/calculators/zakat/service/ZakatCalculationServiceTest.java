package dev.azhar.hishabi.calculators.zakat.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.azhar.hishabi.calculators.zakat.model.*;
import dev.azhar.hishabi.calculators.zakat.model.ZakatCalculationRequest.Assets;
import dev.azhar.hishabi.calculators.zakat.model.ZakatCalculationRequest.Deductions;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ZakatCalculationServiceTest {

    private final ZakatCalculationService service = new ZakatCalculationService();

    // Fixture prices for clean arithmetic
    private static final BigDecimal SILVER_PRICE = new BigDecimal("400.00");
    private static final BigDecimal GOLD_PRICE = new BigDecimal("10000.00");
    // silver nisab = 612.36 × 400 × 0.83 = 203,303.52
    // gold   nisab = 87.48  × 10000 × 0.83 = 726,084.00

    @Test
    void sumAssetsAddsAll21Fields() {
        // Every field = 1,000 → total 21,000
        Assets a = assets("1000", "1000", "1000", "1000", "1000", "1000", "1000",
                "1000", "1000", "1000", "1000", "1000", "1000", "1000", "1000",
                "1000", "1000", "1000", "1000", "1000", "1000");
        assertThat(service.sumAssets(a)).isEqualByComparingTo("21000.00");
    }

    @Test
    void sumDeductionsAddsAll7Fields() {
        Deductions d = deductions("1000", "1000", "1000", "1000", "1000", "1000", "1000");
        assertThat(service.sumDeductions(d)).isEqualByComparingTo("7000.00");
    }

    @Test
    void computeNisabSilverBasis() {
        assertThat(service.computeNisab(rules(), NisabBasis.SILVER, SILVER_PRICE, GOLD_PRICE))
                .isEqualByComparingTo("203303.52");
    }

    @Test
    void computeNisabGoldBasis() {
        assertThat(service.computeNisab(rules(), NisabBasis.GOLD, SILVER_PRICE, GOLD_PRICE))
                .isEqualByComparingTo("726084.00");
    }

    @Test
    void belowSilverNisabProducesZeroZakat() {
        ZakatCalculationResponse r = service.calculate(
                rules(), request(NisabBasis.SILVER, CalendarType.ENGLISH,
                        cashOnly("100000"), noDeductions()),
                SILVER_PRICE, GOLD_PRICE, Instant.EPOCH, false);
        assertThat(r.netWealth()).isEqualByComparingTo("100000.00");
        assertThat(r.aboveNisab()).isFalse();
        assertThat(r.zakatPayable()).isEqualByComparingTo("0.00");
    }

    @Test
    void aboveNisabEnglishCalendarApplies2_6Percent() {
        // 500,000 × 0.026 = 13,000
        ZakatCalculationResponse r = service.calculate(
                rules(), request(NisabBasis.SILVER, CalendarType.ENGLISH,
                        cashOnly("500000"), noDeductions()),
                SILVER_PRICE, GOLD_PRICE, Instant.EPOCH, false);
        assertThat(r.aboveNisab()).isTrue();
        assertThat(r.rateApplied()).isEqualByComparingTo("0.0260");
        assertThat(r.zakatPayable()).isEqualByComparingTo("13000.00");
    }

    @Test
    void aboveNisabHijriCalendarApplies2_5Percent() {
        // 500,000 × 0.025 = 12,500
        ZakatCalculationResponse r = service.calculate(
                rules(), request(NisabBasis.SILVER, CalendarType.HIJRI,
                        cashOnly("500000"), noDeductions()),
                SILVER_PRICE, GOLD_PRICE, Instant.EPOCH, false);
        assertThat(r.aboveNisab()).isTrue();
        assertThat(r.rateApplied()).isEqualByComparingTo("0.0250");
        assertThat(r.zakatPayable()).isEqualByComparingTo("12500.00");
    }

    @Test
    void goldNisabBasisBelowGoldNisab() {
        // 500,000 < 726,084 → no zakat
        ZakatCalculationResponse r = service.calculate(
                rules(), request(NisabBasis.GOLD, CalendarType.HIJRI,
                        cashOnly("500000"), noDeductions()),
                SILVER_PRICE, GOLD_PRICE, Instant.EPOCH, false);
        assertThat(r.nisabUsed()).isEqualByComparingTo("726084.00");
        assertThat(r.aboveNisab()).isFalse();
        assertThat(r.zakatPayable()).isEqualByComparingTo("0.00");
    }

    @Test
    void goldNisabBasisAboveGoldNisab() {
        // 800,000 > 726,084 → 800,000 × 0.025 = 20,000
        ZakatCalculationResponse r = service.calculate(
                rules(), request(NisabBasis.GOLD, CalendarType.HIJRI,
                        cashOnly("800000"), noDeductions()),
                SILVER_PRICE, GOLD_PRICE, Instant.EPOCH, false);
        assertThat(r.aboveNisab()).isTrue();
        assertThat(r.zakatPayable()).isEqualByComparingTo("20000.00");
    }

    @Test
    void deductionsReduceNetWealth() {
        // assets=1,000,000  deductions=300,000 → net=700,000 × 0.025 = 17,500
        ZakatCalculationResponse r = service.calculate(
                rules(), request(NisabBasis.SILVER, CalendarType.HIJRI,
                        cashOnly("1000000"), loanDeduction("300000")),
                SILVER_PRICE, GOLD_PRICE, Instant.EPOCH, false);
        assertThat(r.totalAssets()).isEqualByComparingTo("1000000.00");
        assertThat(r.totalDeductions()).isEqualByComparingTo("300000.00");
        assertThat(r.netWealth()).isEqualByComparingTo("700000.00");
        assertThat(r.zakatPayable()).isEqualByComparingTo("17500.00");
    }

    @Test
    void netWealthFlooredAtZeroWhenDeductionsExceedAssets() {
        // assets=100,000  deductions=200,000 → net=0 → zakat=0
        ZakatCalculationResponse r = service.calculate(
                rules(), request(NisabBasis.SILVER, CalendarType.ENGLISH,
                        cashOnly("100000"), loanDeduction("200000")),
                SILVER_PRICE, GOLD_PRICE, Instant.EPOCH, false);
        assertThat(r.netWealth()).isEqualByComparingTo("0.00");
        assertThat(r.zakatPayable()).isEqualByComparingTo("0.00");
    }

    @Test
    void priceStalePassedThroughToResponse() {
        ZakatCalculationResponse r = service.calculate(
                rules(), request(NisabBasis.SILVER, CalendarType.ENGLISH,
                        cashOnly("0"), noDeductions()),
                SILVER_PRICE, GOLD_PRICE, Instant.EPOCH, true);
        assertThat(r.priceStale()).isTrue();
        assertThat(r.priceFetchedAt()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void atNisabBoundaryProducesZakat() {
        // netWealth == nisab exactly → aboveNisab must be true (>= not >)
        // 203,303.52 × 0.026 = 5285.89 (HALF_UP)
        ZakatCalculationResponse r = service.calculate(
                rules(), request(NisabBasis.SILVER, CalendarType.ENGLISH,
                        cashOnly("203303.52"), noDeductions()),
                SILVER_PRICE, GOLD_PRICE, Instant.EPOCH, false);
        assertThat(r.aboveNisab()).isTrue();
        assertThat(r.zakatPayable()).isGreaterThan(BigDecimal.ZERO);
    }

    // ── fixtures ────────────────────────────────────────────────────────────

    private static ZakatRuleSet rules() {
        ZakatRuleSet rs = new ZakatRuleSet();
        rs.setNisabSilverGrams(new BigDecimal("612.3600"));
        rs.setNisabGoldGrams(new BigDecimal("87.4800"));
        rs.setResaleFactor(new BigDecimal("0.8300"));
        rs.setRateLunar(new BigDecimal("0.0250"));
        rs.setRateSolar(new BigDecimal("0.0260"));
        return rs;
    }

    private static ZakatCalculationRequest request(
            NisabBasis basis, CalendarType calendar, Assets assets, Deductions deductions) {
        return new ZakatCalculationRequest(basis, calendar, null, assets, deductions);
    }

    private static Assets cashOnly(String cash) {
        BigDecimal z = BigDecimal.ZERO;
        return new Assets(z, z, new BigDecimal(cash), z, z, z, z, z, z, z, z, z, z, z, z, z, z, z, z, z, z);
    }

    private static Assets assets(
            String gold, String silver, String cash, String fx, String bank, String savings,
            String insurance, String opf, String loans, String deposits, String rentSec,
            String otherSec, String bizCash, String receivables, String stock, String resale,
            String mudarabaNet, String mudarabaPrinc, String capGain, String divNet, String divMkt) {
        return new Assets(
                new BigDecimal(gold), new BigDecimal(silver), new BigDecimal(cash),
                new BigDecimal(fx), new BigDecimal(bank), new BigDecimal(savings),
                new BigDecimal(insurance), new BigDecimal(opf), new BigDecimal(loans),
                new BigDecimal(deposits), new BigDecimal(rentSec), new BigDecimal(otherSec),
                new BigDecimal(bizCash), new BigDecimal(receivables), new BigDecimal(stock),
                new BigDecimal(resale), new BigDecimal(mudarabaNet), new BigDecimal(mudarabaPrinc),
                new BigDecimal(capGain), new BigDecimal(divNet), new BigDecimal(divMkt));
    }

    private static Deductions noDeductions() {
        BigDecimal z = BigDecimal.ZERO;
        return new Deductions(z, z, z, z, z, z, z);
    }

    private static Deductions loanDeduction(String amount) {
        BigDecimal z = BigDecimal.ZERO;
        return new Deductions(new BigDecimal(amount), z, z, z, z, z, z);
    }

    private static Deductions deductions(
            String d1, String d2, String d3, String d4, String d5, String d6, String d7) {
        return new Deductions(
                new BigDecimal(d1), new BigDecimal(d2), new BigDecimal(d3),
                new BigDecimal(d4), new BigDecimal(d5), new BigDecimal(d6),
                new BigDecimal(d7));
    }
}