package dev.azhar.hishabi.calculators.tax.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class TaxHistoryListIT {

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

    private static final String HISTORY_URL = "/api/calculators/tax/history";

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;
    @Autowired UserRepository userRepository;
    @Autowired CalculationRepository calculationRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        calculationRepository.deleteAll();
        userRepository.deleteAll();

        userA =
                userRepository.save(
                        User.builder()
                                .email("hist-a@example.com")
                                .passwordHash(passwordEncoder.encode("Secret1pass"))
                                .build());
        userB =
                userRepository.save(
                        User.builder()
                                .email("hist-b@example.com")
                                .passwordHash(passwordEncoder.encode("Secret1pass"))
                                .build());

        calculationRepository.save(row(userA, "{\"i\":1}", "{\"tax\":100}"));
        calculationRepository.save(row(userA, "{\"i\":2}", "{\"tax\":200}"));
        calculationRepository.save(row(userB, "{\"i\":3}", "{\"tax\":300}"));
    }

    @Test
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(HISTORY_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUser_seesOwnRowsOnly() throws Exception {
        mockMvc.perform(
                        get(HISTORY_URL)
                                .header(
                                        "Authorization",
                                        "Bearer " + jwtService.generateToken(userA.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void otherUser_doesNotSeeRowsCrossUser() throws Exception {
        mockMvc.perform(
                        get(HISTORY_URL)
                                .header(
                                        "Authorization",
                                        "Bearer " + jwtService.generateToken(userB.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void pagination_respectsPageAndSize() throws Exception {
        mockMvc.perform(
                        get(HISTORY_URL)
                                .param("page", "0")
                                .param("size", "1")
                                .header(
                                        "Authorization",
                                        "Bearer " + jwtService.generateToken(userA.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    private static Calculation row(User user, String req, String res) {
        return Calculation.builder()
                .user(user)
                .calculatorType(CalculatorType.TAX)
                .requestJson(req)
                .responseJson(res)
                .build();
    }
}
