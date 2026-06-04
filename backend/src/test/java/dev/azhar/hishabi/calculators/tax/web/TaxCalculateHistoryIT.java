package dev.azhar.hishabi.calculators.tax.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.azhar.hishabi.platform.auth.model.User;
import dev.azhar.hishabi.platform.auth.repository.UserRepository;
import dev.azhar.hishabi.platform.auth.service.JwtService;
import dev.azhar.hishabi.platform.history.Calculation;
import dev.azhar.hishabi.platform.history.CalculationRepository;
import dev.azhar.hishabi.platform.history.CalculatorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TaxCalculateHistoryIT {

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

    private static final String CALCULATE_URL = "/api/calculators/tax/calculate";
    private static final String USER_EMAIL = "history-it@example.com";

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired CalculationRepository calculationRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        calculationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void anonymousCalculate_doesNotPersist() throws Exception {
        mockMvc.perform(
                        post(CALCULATE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequestBody()))
                .andExpect(status().isOk());

        assertThat(calculationRepository.count()).isZero();
    }

    @Test
    void authenticatedCalculate_persistsCalculationForUser() throws Exception {
        User user =
                userRepository.save(
                        User.builder()
                                .email(USER_EMAIL)
                                .passwordHash(passwordEncoder.encode("Secret1pass"))
                                .build());
        String token = jwtService.generateToken(USER_EMAIL);

        mockMvc.perform(
                        post(CALCULATE_URL)
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validRequestBody()))
                .andExpect(status().isOk());

        Page<Calculation> history =
                calculationRepository.findByUserIdOrderByCreatedAtDesc(
                        user.getId(), Pageable.unpaged());
        Calculation saved = history.getContent().getFirst();
        assertThat(history.getTotalElements()).isOne();
        assertThat(saved.getCalculatorType()).isEqualTo(CalculatorType.TAX);
        assertThat(saved.getRequestJson()).isNotBlank();
        assertThat(saved.getResponseJson()).isNotBlank();
    }

    private static String validRequestBody() {
        return """
                {
                  "assessmentYear": "2025-26",
                  "category": "GENERAL",
                  "location": "DHAKA_CHITTAGONG_CITY_CORP",
                  "disabledChildren": 0,
                  "income": {
                    "basic": 1000000,
                    "houseRent": 0,
                    "conveyance": 0,
                    "medicalAllowance": 0,
                    "leaveEncashment": 0,
                    "performanceBonus": 0,
                    "yearlyBonus": 0,
                    "festivalBonus": 0,
                    "overtime": 0,
                    "transportation": 0
                  },
                  "investments": {
                    "sanchayPatra": 0,
                    "dps": 0,
                    "mutualFund": 0,
                    "treasuryBond": 0,
                    "providentFundEmployee": 0,
                    "providentFundEmployer": 0,
                    "stock": 0
                  },
                  "advanceIncomeTaxPaid": 0
                }
                """;
    }
}
