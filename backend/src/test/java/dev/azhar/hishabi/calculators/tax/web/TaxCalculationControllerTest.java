package dev.azhar.hishabi.calculators.tax.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.azhar.hishabi.calculators.tax.model.Location;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationRequest;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationRequest.IncomeComponents;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationRequest.Investments;
import dev.azhar.hishabi.calculators.tax.model.TaxCalculationResponse;
import dev.azhar.hishabi.calculators.tax.model.TaxpayerCategory;
import java.math.BigDecimal;
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
class TaxCalculationControllerTest {

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
    void calculateWorkedExampleReturnsNetTax56820() throws Exception {
        // §10.8 inputs, assessmentYear omitted → resolves to the latest (2025-26)
        String body = objectMapper.writeValueAsString(workedExampleRequest(null));

        MvcResult result =
                mockMvc.perform(
                                post("/api/calculators/tax/calculate")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isOk())
                        .andReturn();

        TaxCalculationResponse resp =
                objectMapper.readValue(
                        result.getResponse().getContentAsString(), TaxCalculationResponse.class);
        assertThat(resp.netTax()).isEqualByComparingTo("56820.00");
        assertThat(resp.assessmentYear()).isEqualTo("2025-26");
    }

    @Test
    void unknownAssessmentYearReturns404() throws Exception {
        String body = objectMapper.writeValueAsString(workedExampleRequest("1999-00"));

        mockMvc.perform(
                        post("/api/calculators/tax/calculate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidRequestReturns400WithGlobalErrorShape() throws Exception {
        BigDecimal z = BigDecimal.ZERO;
        TaxCalculationRequest invalid =
                new TaxCalculationRequest(
                        "2025-26", null, Location.OTHER, 0, income("0"), investments("0", "0"), z);
        String body = objectMapper.writeValueAsString(invalid);

        mockMvc.perform(
                        post("/api/calculators/tax/calculate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    private static TaxCalculationRequest workedExampleRequest(String assessmentYear) {
        return new TaxCalculationRequest(
                assessmentYear,
                TaxpayerCategory.GENERAL,
                Location.DHAKA_CHITTAGONG_CITY_CORP,
                0,
                income("1611000"),
                investments("200000", "120000"),
                BigDecimal.ZERO);
    }

    private static IncomeComponents income(String basic) {
        BigDecimal z = BigDecimal.ZERO;
        return new IncomeComponents(new BigDecimal(basic), z, z, z, z, z, z, z, z, z);
    }

    private static Investments investments(String sanchayPatra, String dps) {
        BigDecimal z = BigDecimal.ZERO;
        return new Investments(new BigDecimal(sanchayPatra), new BigDecimal(dps), z, z, z, z, z);
    }
}
