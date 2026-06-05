package dev.azhar.hishabi.platform.history;

import static org.assertj.core.api.Assertions.assertThat;

import dev.azhar.hishabi.platform.auth.model.User;
import dev.azhar.hishabi.platform.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class CalculationPersistenceTest {

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

    @Autowired private UserRepository userRepo;
    @Autowired private CalculationRepository calcRepo;

    @Test
    void persistAndFetchByUser() {
        User alice =
                userRepo.saveAndFlush(
                        User.builder()
                                .email("alice@example.com")
                                .passwordHash("$2a$12$hashhash")
                                .build());

        calcRepo.saveAndFlush(
                Calculation.builder()
                        .user(alice)
                        .calculatorType(CalculatorType.TAX)
                        .requestJson("{\"income\":1000000}")
                        .responseJson("{\"netTax\":56820}")
                        .build());

        Page<Calculation> page =
                calcRepo.findByUserIdAndCalculatorTypeOrderByCreatedAtDesc(
                        alice.getId(), CalculatorType.TAX, PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getCalculatorType()).isEqualTo(CalculatorType.TAX);
        assertThat(page.getContent().get(0).getRequestJson()).contains("1000000");
        assertThat(page.getContent().get(0).getResponseJson()).contains("56820");
        assertThat(page.getContent().get(0).getCreatedAt()).isNotNull();
    }

    @Test
    void fetchByUserReturnsOnlyOwnRows() {
        User alice =
                userRepo.saveAndFlush(
                        User.builder()
                                .email("alice2@example.com")
                                .passwordHash("$2a$12$hash1")
                                .build());
        User bob =
                userRepo.saveAndFlush(
                        User.builder()
                                .email("bob@example.com")
                                .passwordHash("$2a$12$hash2")
                                .build());

        calcRepo.saveAndFlush(
                Calculation.builder()
                        .user(alice)
                        .calculatorType(CalculatorType.TAX)
                        .requestJson("{}")
                        .responseJson("{}")
                        .build());
        calcRepo.saveAndFlush(
                Calculation.builder()
                        .user(bob)
                        .calculatorType(CalculatorType.TAX)
                        .requestJson("{}")
                        .responseJson("{}")
                        .build());

        assertThat(
                        calcRepo.findByUserIdAndCalculatorTypeOrderByCreatedAtDesc(
                                        alice.getId(), CalculatorType.TAX, PageRequest.of(0, 10))
                                .getTotalElements())
                .isEqualTo(1);
        assertThat(
                        calcRepo.findByUserIdAndCalculatorTypeOrderByCreatedAtDesc(
                                        bob.getId(), CalculatorType.TAX, PageRequest.of(0, 10))
                                .getTotalElements())
                .isEqualTo(1);
    }

    @Test
    void consolidatedHistoryReturnsMostRecentFirst() {
        User user =
                userRepo.saveAndFlush(
                        User.builder()
                                .email("carol@example.com")
                                .passwordHash("$2a$12$hash3")
                                .build());

        Calculation first =
                calcRepo.saveAndFlush(
                        Calculation.builder()
                                .user(user)
                                .calculatorType(CalculatorType.TAX)
                                .requestJson("{\"seq\":1}")
                                .responseJson("{}")
                                .build());
        Calculation second =
                calcRepo.saveAndFlush(
                        Calculation.builder()
                                .user(user)
                                .calculatorType(CalculatorType.TAX)
                                .requestJson("{\"seq\":2}")
                                .responseJson("{}")
                                .build());

        Page<Calculation> page =
                calcRepo.findByUserIdAndCalculatorTypeOrderByCreatedAtDesc(
                        user.getId(), CalculatorType.TAX, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getId()).isEqualTo(second.getId());
        assertThat(page.getContent().get(1).getId()).isEqualTo(first.getId());
    }

    @Test
    void typeFilterReturnsOnlyMatchingCalculatorType() {
        User user =
                userRepo.saveAndFlush(
                        User.builder()
                                .email("dave@example.com")
                                .passwordHash("$2a$12$hash4")
                                .build());

        calcRepo.saveAndFlush(
                Calculation.builder()
                        .user(user)
                        .calculatorType(CalculatorType.TAX)
                        .requestJson("{}")
                        .responseJson("{}")
                        .build());
        calcRepo.saveAndFlush(
                Calculation.builder()
                        .user(user)
                        .calculatorType(CalculatorType.ZAKAT)
                        .requestJson("{}")
                        .responseJson("{}")
                        .build());

        Page<Calculation> taxOnly =
                calcRepo.findByUserIdAndCalculatorTypeOrderByCreatedAtDesc(
                        user.getId(), CalculatorType.TAX, PageRequest.of(0, 10));
        Page<Calculation> zakatOnly =
                calcRepo.findByUserIdAndCalculatorTypeOrderByCreatedAtDesc(
                        user.getId(), CalculatorType.ZAKAT, PageRequest.of(0, 10));

        assertThat(taxOnly.getTotalElements()).isEqualTo(1);
        assertThat(taxOnly.getContent().get(0).getCalculatorType()).isEqualTo(CalculatorType.TAX);
        assertThat(zakatOnly.getTotalElements()).isEqualTo(1);
        assertThat(zakatOnly.getContent().get(0).getCalculatorType())
                .isEqualTo(CalculatorType.ZAKAT);
    }
}
