package dev.azhar.hishabi.calculators.tax.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.azhar.hishabi.calculators.tax.model.Location;
import dev.azhar.hishabi.calculators.tax.model.TaxRulesResponse;
import dev.azhar.hishabi.calculators.tax.model.TaxpayerCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TaxRulesControllerTest {

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

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void returnsFullRuleSetForAy2025_26() throws Exception {
        MvcResult result =
                mockMvc.perform(get("/api/calculators/tax/rules/{ay}", "2025-26"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                        .andReturn();

        TaxRulesResponse rules =
                objectMapper.readValue(
                        result.getResponse().getContentAsString(), TaxRulesResponse.class);

        // §10.2 / §10.3 / §10.5 scalar config + per-item caps
        assertThat(rules.assessmentYear()).isEqualTo("2025-26");
        assertThat(rules.salaryExemptionCap()).isEqualByComparingTo("450000");
        assertThat(rules.salaryExemptionDivisor()).isEqualTo(3);
        assertThat(rules.disabledChildThresholdBonus()).isEqualByComparingTo("50000");
        assertThat(rules.rebateTaxableFraction()).isEqualByComparingTo("0.03");
        assertThat(rules.rebateEligibleFraction()).isEqualByComparingTo("0.15");
        assertThat(rules.rebateCap()).isEqualByComparingTo("1000000");
        assertThat(rules.sanchayPatraCap()).isEqualByComparingTo("500000");
        assertThat(rules.dpsCap()).isEqualByComparingTo("120000");

        // §10.4 — 6 stored paying slabs, ordinal 1 = 100k@5% ... ordinal 6 = null-width@30%
        assertThat(rules.slabs()).hasSize(6);
        assertThat(rules.slabs().get(0).ordinal()).isEqualTo(1);
        assertThat(rules.slabs().get(0).width()).isEqualByComparingTo("100000");
        assertThat(rules.slabs().get(0).rate()).isEqualByComparingTo("0.05");
        assertThat(rules.slabs().get(5).ordinal()).isEqualTo(6);
        assertThat(rules.slabs().get(5).width()).isNull();
        assertThat(rules.slabs().get(5).rate()).isEqualByComparingTo("0.30");

        // §10.3 — 6 category thresholds, sorted by category enum order (GENERAL first)
        assertThat(rules.categoryThresholds()).hasSize(6);
        assertThat(rules.categoryThresholds().get(0).category())
                .isEqualTo(TaxpayerCategory.GENERAL);
        assertThat(rules.categoryThresholds().get(0).amount()).isEqualByComparingTo("350000");

        // §10.6 — 3 minimum-tax floors, sorted by location enum order (Dhaka/Ctg first)
        assertThat(rules.minimumTaxFloors()).hasSize(3);
        assertThat(rules.minimumTaxFloors().get(0).location())
                .isEqualTo(Location.DHAKA_CHITTAGONG_CITY_CORP);
        assertThat(rules.minimumTaxFloors().get(0).amount()).isEqualByComparingTo("5000");
    }

    @Test
    void unknownYearReturns404() throws Exception {
        mockMvc.perform(get("/api/calculators/tax/rules/{ay}", "1999-00"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }
}
