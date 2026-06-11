package dev.azhar.hishabi.platform.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.azhar.hishabi.platform.auth.model.User;
import dev.azhar.hishabi.platform.auth.repository.UserRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
class UserPersistenceTest {

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

    @Autowired private UserRepository repo;

    @Test
    void persistAndFindByEmail() {
        OffsetDateTime before = OffsetDateTime.now().minusSeconds(2);
        User saved =
                repo.saveAndFlush(
                        User.builder()
                                .email("alice@example.com")
                                .passwordHash("$2a$12$hashhash")
                                .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isAfter(before);

        User found = repo.findByEmailIgnoreCase("ALICE@EXAMPLE.COM").orElseThrow();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getPasswordHash()).isEqualTo("$2a$12$hashhash");
    }

    @Test
    void duplicateEmailIsRejected() {
        repo.saveAndFlush(
                User.builder().email("bob@example.com").passwordHash("$2a$12$hash1").build());

        assertThatThrownBy(
                        () ->
                                repo.saveAndFlush(
                                        User.builder()
                                                .email("bob@example.com")
                                                .passwordHash("$2a$12$hash2")
                                                .build()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_users_email");
    }

    @Test
    void duplicateEmailIsCaseInsensitive() {
        repo.saveAndFlush(
                User.builder().email("carol@example.com").passwordHash("$2a$12$hash3").build());

        assertThatThrownBy(
                        () ->
                                repo.saveAndFlush(
                                        User.builder()
                                                .email("CAROL@EXAMPLE.COM")
                                                .passwordHash("$2a$12$hash4")
                                                .build()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_users_email");
    }

    @Test
    void existsByEmailIgnoreCase() {
        repo.saveAndFlush(
                User.builder().email("dave@example.com").passwordHash("$2a$12$hash5").build());

        assertThat(repo.existsByEmailIgnoreCase("dave@example.com")).isTrue();
        assertThat(repo.existsByEmailIgnoreCase("DAVE@EXAMPLE.COM")).isTrue();
        assertThat(repo.existsByEmailIgnoreCase("nobody@example.com")).isFalse();
    }

    @Test
    void missingEmailReturnsEmpty() {
        assertThat(repo.findByEmailIgnoreCase("nobody@example.com")).isEmpty();
    }
}
