package dev.azhar.hishabi.profile;

import static org.assertj.core.api.Assertions.assertThat;

import dev.azhar.hishabi.calculators.tax.repository.AssessmentYearRepository;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class DevProfileBootTest {

    @Autowired DataSource dataSource;
    @Autowired AssessmentYearRepository assessmentYears;

    @Test
    void devProfileWiresH2DataSource() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            assertThat(conn.getMetaData().getURL()).contains("jdbc:h2:mem");
        }
    }

    @Test
    void devProfileIsSeededByFlyway() {
        // Flyway runs V1-V3 on H2 (PostgreSQL mode), so the dev DB has the AY 2025-26 rule set.
        assertThat(assessmentYears.findByLabel("2025-26")).isPresent();
    }
}
