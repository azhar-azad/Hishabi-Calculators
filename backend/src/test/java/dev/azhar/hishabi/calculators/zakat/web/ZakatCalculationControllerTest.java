package dev.azhar.hishabi.calculators.zakat.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.azhar.hishabi.calculators.zakat.model.CalendarType;
import dev.azhar.hishabi.calculators.zakat.model.NisabBasis;
import dev.azhar.hishabi.calculators.zakat.model.ZakatCalculationRequest;
import dev.azhar.hishabi.calculators.zakat.model.ZakatCalculationRequest.Assets;
import dev.azhar.hishabi.calculators.zakat.model.ZakatCalculationRequest.Deductions;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ZakatCalculationControllerTest {

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
    void configEndpointReturnsSeededRuleValues() throws Exception {
        mockMvc.perform(get("/api/calculators/zakat/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nisabSilverGrams").value(612.36))
                .andExpect(jsonPath("$.nisabGoldGrams").value(87.48))
                .andExpect(jsonPath("$.resaleFactor").value(0.83))
                .andExpect(jsonPath("$.rateLunar").value(0.025))
                .andExpect(jsonPath("$.rateSolar").value(0.026))
                .andExpect(jsonPath("$.nisabSilver").exists())
                .andExpect(jsonPath("$.nisabGold").exists())
                .andExpect(jsonPath("$.priceFetchedAt").exists());
    }

    @Test
    void calculateAboveNisabEnglishCalendarProducesZakat() throws Exception {
        // cash 500,000 > silver nisab (~173,250) → 500,000 × 0.026 = 13,000
        String body = objectMapper.writeValueAsString(
                request(NisabBasis.SILVER, CalendarType.ENGLISH, "500000"));

        mockMvc.perform(
                        post("/api/calculators/zakat/calculate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aboveNisab").value(true))
                .andExpect(jsonPath("$.netWealth").value(500000.00))
                .andExpect(jsonPath("$.rateApplied").value(0.026))
                .andExpect(jsonPath("$.zakatPayable").value(13000.00));
    }

    @Test
    void calculateBelowNisabProducesZeroZakat() throws Exception {
        // cash 100,000 < silver nisab (~173,250) → zakat = 0
        String body = objectMapper.writeValueAsString(
                request(NisabBasis.SILVER, CalendarType.HIJRI, "100000"));

        mockMvc.perform(
                        post("/api/calculators/zakat/calculate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aboveNisab").value(false))
                .andExpect(jsonPath("$.zakatPayable").value(0.00));
    }

    @Test
    void calculateValidationRejectsNullNisabBasis() throws Exception {
        String body = """
                {"nisabBasis":null,"calendarType":"ENGLISH",
                 "assets":{"gold":0,"silver":0,"cashInHand":0,"foreignCurrency":0,
                   "bankDeposits":0,"savingsInstruments":0,"refundableInsurancePremium":0,
                   "optionalProvidentFund":0,"recoverableLoans":0,"depositsPlacedWithOthers":0,
                   "refundableRentSecurity":0,"refundableOtherSecurity":0,"businessCash":0,
                   "customerReceivables":0,"tradeStock":0,"tradeAssetsForResale":0,
                   "mudarabaShareNetKnown":0,"mudarabaPrincipal":0,"shareMarketCapitalGain":0,
                   "shareDividendNetKnown":0,"shareDividendMarketValue":0},
                 "deductions":{"personalLoanDueWithinYear":0,"businessLoanDueWithinYear":0,
                   "installmentPurchasesDueWithinYear":0,"unpaidDowryDueWithinYear":0,
                   "unpaidEmployeeWages":0,"payableBillsAndTaxes":0,"priorYearUnpaidZakat":0}}
                """;

        mockMvc.perform(
                        post("/api/calculators/zakat/calculate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
    }

    // ── fixtures ────────────────────────────────────────────────────────────

    private static ZakatCalculationRequest request(
            NisabBasis basis, CalendarType calendar, String cashInHand) {
        BigDecimal z = BigDecimal.ZERO;
        BigDecimal cash = new BigDecimal(cashInHand);
        Assets assets = new Assets(z, z, cash, z, z, z, z, z, z, z, z, z, z, z, z, z, z, z, z, z, z);
        Deductions deductions = new Deductions(z, z, z, z, z, z, z);
        return new ZakatCalculationRequest(basis, calendar, null, assets, deductions);
    }
}