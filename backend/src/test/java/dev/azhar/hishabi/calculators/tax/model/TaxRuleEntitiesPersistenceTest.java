package dev.azhar.hishabi.calculators.tax.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class TaxRuleEntitiesPersistenceTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // Flyway owns the schema; Hibernate only validates the entities map onto it.
        // This closes the entity-vs-migration drift seam: if V1 SQL and the @Entity
        // mappings disagree, context startup fails.
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add(
                "spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired private EntityManager em;

    @Test
    void ruleSetWithSlabsThresholdsFloorsRoundtrips() {
        RuleSet rs = newRuleSet();
        rs.getSlabs().add(slab(rs, 1, new BigDecimal("100000.00"), "0.0500"));
        rs.getSlabs().add(slab(rs, 2, new BigDecimal("400000.00"), "0.1000"));
        rs.getSlabs().add(slab(rs, 3, new BigDecimal("500000.00"), "0.1500"));
        rs.getSlabs().add(slab(rs, 4, new BigDecimal("500000.00"), "0.2000"));
        rs.getSlabs().add(slab(rs, 5, new BigDecimal("2000000.00"), "0.2500"));
        rs.getSlabs().add(slab(rs, 6, null, "0.3000"));
        rs.getCategoryThresholds().add(threshold(rs, TaxpayerCategory.GENERAL, "350000.00"));
        rs.getCategoryThresholds().add(threshold(rs, TaxpayerCategory.WOMAN, "400000.00"));
        rs.getMinimumTaxFloors().add(floor(rs, Location.DHAKA_CHITTAGONG_CITY_CORP, "5000.00"));
        rs.getMinimumTaxFloors().add(floor(rs, Location.OTHER, "3000.00"));

        em.persist(rs);

        AssessmentYear ay = new AssessmentYear();
        ay.setLabel("2025-26");
        ay.setRuleSet(rs);
        em.persist(ay);

        em.flush();
        em.clear();

        AssessmentYear loaded = em.find(AssessmentYear.class, ay.getId());
        assertThat(loaded.getLabel()).isEqualTo("2025-26");

        RuleSet loadedRs = loaded.getRuleSet();
        assertThat(loadedRs.getSalaryExemptionCap()).isEqualByComparingTo("450000.00");
        assertThat(loadedRs.getSalaryExemptionDivisor()).isEqualTo(3);

        List<TaxSlab> slabs = loadedRs.getSlabs();
        assertThat(slabs).hasSize(6);
        assertThat(slabs.get(0).getOrdinal()).isEqualTo(1);
        assertThat(slabs.get(0).getRate()).isEqualByComparingTo("0.0500");
        assertThat(slabs.get(5).getOrdinal()).isEqualTo(6);
        assertThat(slabs.get(5).getWidth()).isNull(); // "(rest)" top slab
        assertThat(slabs.get(5).getRate()).isEqualByComparingTo("0.3000");

        assertThat(loadedRs.getCategoryThresholds()).hasSize(2);
        assertThat(loadedRs.getMinimumTaxFloors()).hasSize(2);
    }

    @Test
    void multipleAssessmentYearsCanShareOneRuleSet() {
        RuleSet rs = newRuleSet();
        em.persist(rs);

        AssessmentYear ay1 = new AssessmentYear();
        ay1.setLabel("2024-25");
        ay1.setRuleSet(rs);
        AssessmentYear ay2 = new AssessmentYear();
        ay2.setLabel("2025-26");
        ay2.setRuleSet(rs);
        em.persist(ay1);
        em.persist(ay2);
        em.flush();
        em.clear();

        AssessmentYear loaded1 = em.find(AssessmentYear.class, ay1.getId());
        AssessmentYear loaded2 = em.find(AssessmentYear.class, ay2.getId());
        assertThat(loaded1.getRuleSet().getId()).isEqualTo(loaded2.getRuleSet().getId());
    }

    private static RuleSet newRuleSet() {
        RuleSet rs = new RuleSet();
        rs.setName("AY 2025-26 test");
        rs.setSalaryExemptionCap(new BigDecimal("450000.00"));
        rs.setSalaryExemptionDivisor(3);
        rs.setDisabledChildThresholdBonus(new BigDecimal("50000.00"));
        rs.setRebateTaxableFraction(new BigDecimal("0.0300"));
        rs.setRebateEligibleFraction(new BigDecimal("0.1500"));
        rs.setRebateCap(new BigDecimal("1000000.00"));
        return rs;
    }

    private static TaxSlab slab(RuleSet rs, int ord, BigDecimal width, String rate) {
        TaxSlab s = new TaxSlab();
        s.setRuleSet(rs);
        s.setOrdinal(ord);
        s.setWidth(width);
        s.setRate(new BigDecimal(rate));
        return s;
    }

    private static CategoryThreshold threshold(RuleSet rs, TaxpayerCategory cat, String amt) {
        CategoryThreshold t = new CategoryThreshold();
        t.setRuleSet(rs);
        t.setCategory(cat);
        t.setAmount(new BigDecimal(amt));
        return t;
    }

    private static MinimumTaxFloor floor(RuleSet rs, Location loc, String amt) {
        MinimumTaxFloor f = new MinimumTaxFloor();
        f.setRuleSet(rs);
        f.setLocation(loc);
        f.setAmount(new BigDecimal(amt));
        return f;
    }
}
