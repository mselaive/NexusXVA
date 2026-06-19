package com.nexusxva;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractPostgresIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    protected UUID insertConfirmedEuropeanOptionPosition(
            UUID portfolioId,
            String underlyingSymbol,
            String optionType,
            String strike,
            String maturityDate,
            String quantity
    ) {
        UUID positionId = UUID.randomUUID();
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                INSERT INTO portfolio_european_option_positions (
                    id, portfolio_id, underlying_symbol, option_type, strike,
                    maturity_date, quantity, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                positionId,
                portfolioId,
                underlyingSymbol.toUpperCase(),
                optionType,
                new BigDecimal(strike),
                Date.valueOf(LocalDate.parse(maturityDate)),
                new BigDecimal(quantity),
                Timestamp.from(now),
                Timestamp.from(now)
        );
        return positionId;
    }
}
