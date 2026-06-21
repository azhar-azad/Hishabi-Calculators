package dev.azhar.hishabi.calculators.zakat.model;

import static org.assertj.core.api.Assertions.assertThat;

import dev.azhar.hishabi.calculators.zakat.repository.MetalPriceRepository;
import dev.azhar.hishabi.calculators.zakat.repository.ZakatRuleSetRepository;
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
class ZakatRuleEntityPersistenceTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add(
                "spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired private ZakatRuleSetRepository ruleSetRepository;
    @Autowired private MetalPriceRepository metalPriceRepository;

    @Test
    void seededRuleSetMatchesPlanSection13() {
        ZakatRuleSet rs =
                ruleSetRepository.findFirstByOrderByIdAsc().orElseThrow();

        assertThat(rs.getNisabSilverGrams()).isEqualByComparingTo("612.3600");
        assertThat(rs.getNisabGoldGrams()).isEqualByComparingTo("87.4800");
        assertThat(rs.getResaleFactor()).isEqualByComparingTo("0.8300");
        assertThat(rs.getRateLunar()).isEqualByComparingTo("0.0250");
        assertThat(rs.getRateSolar()).isEqualByComparingTo("0.0260");
    }

    @Test
    void seededMetalPricesHaveFourRows() {
        List<MetalPrice> prices = metalPriceRepository.findAll();
        assertThat(prices).hasSize(4);
    }

    @Test
    void seededSilverPriceIsPresent() {
        MetalPrice p =
                metalPriceRepository
                        .findByMetalAndCarat(Metal.SILVER, "STANDARD")
                        .orElseThrow();
        assertThat(p.getPricePerGram()).isEqualByComparingTo("340.87");
    }

    @Test
    void seededGold22PriceIsPresent() {
        MetalPrice p =
                metalPriceRepository
                        .findByMetalAndCarat(Metal.GOLD, "22")
                        .orElseThrow();
        assertThat(p.getPricePerGram()).isEqualByComparingTo("9700.00");
    }
}