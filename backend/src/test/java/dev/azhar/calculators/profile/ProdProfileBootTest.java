package dev.azhar.calculators.profile;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        properties = {
            "DB_URL=jdbc:h2:mem:prod-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "DB_USER=sa",
            "DB_PASSWORD=",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
            "spring.jpa.hibernate.ddl-auto=create-drop"
        })
@ActiveProfiles("prod")
class ProdProfileBootTest {

    @Test
    void prodProfileContextLoads() {}
}
