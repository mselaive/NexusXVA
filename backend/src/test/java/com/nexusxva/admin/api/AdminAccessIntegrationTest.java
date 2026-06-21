package com.nexusxva.admin.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.AbstractPostgresIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "nexusxva.auth.enabled=true",
        "nexusxva.auth.bootstrap-admin.username=admin-access",
        "nexusxva.auth.bootstrap-admin.password=admin-access-password",
        "nexusxva.auth.bootstrap-admin.display-name=Admin Access"
})
@AutoConfigureMockMvc
class AdminAccessIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCannotRemoveOwnAdminGroup() throws Exception {
        AuthClient client = selectGroup(login(), "ADMIN");
        mockMvc.perform(put("/api/admin/users/{userId}/groups", client.userId())
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groups": ["FO"]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You cannot remove your own ADMIN group"));
    }

    @Test
    void adminDeletesPortfolioFromAdminEndpoint() throws Exception {
        AuthClient client = selectGroup(login(), "ADMIN");
        UUID portfolioId = insertPortfolio("Admin Delete Book");
        insertConfirmedEuropeanOptionPosition(portfolioId, "AAPL", "CALL", "190.0", "2027-12-31", "10.0");

        mockMvc.perform(delete("/api/admin/portfolios/{portfolioId}", portfolioId)
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken()))
                .andExpect(status().isNoContent());

        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM portfolios WHERE id = ?)",
                Boolean.class,
                portfolioId
        );
        org.assertj.core.api.Assertions.assertThat(exists).isFalse();
    }

    @Test
    void adminCannotDeletePortfolioWithPendingBooking() throws Exception {
        AuthClient client = selectGroup(login(), "ADMIN");
        UUID portfolioId = insertPortfolio("Pending Protected Book");
        insertPendingBooking(portfolioId, "Pending Protected Book");

        mockMvc.perform(delete("/api/admin/portfolios/{portfolioId}", portfolioId)
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Portfolio has pending trade bookings"));
    }

    @Test
    void demoUsersCanLoginWithExpectedGroups() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "fo.trader",
                                  "password": "fo12345"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.groups[0]").value("FO"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "bo.ops",
                                  "password": "bo12345"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.groups[0]").value("BO"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "raul",
                                  "password": "multi12345"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.groups.length()").value(3));
    }

    private AuthClient login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin-access",
                                  "password": "admin-access-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthClient(
                result.getResponse().getCookie("NEXUSXVA_SESSION"),
                body.get("csrfToken").asText(),
                UUID.fromString(body.get("user").get("id").asText())
        );
    }

    private AuthClient selectGroup(AuthClient client, String group) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/active-group")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "group": "%s"
                                }
                                """.formatted(group)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthClient(
                client.cookie(),
                body.get("csrfToken").asText(),
                client.userId()
        );
    }

    private UUID insertPortfolio(String name) {
        UUID portfolioId = UUID.randomUUID();
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                INSERT INTO portfolios (id, name, description, base_currency, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                portfolioId,
                name,
                "Admin integration test portfolio",
                "USD",
                Timestamp.from(now),
                Timestamp.from(now)
        );
        return portfolioId;
    }

    private void insertPendingBooking(UUID portfolioId, String portfolioName) {
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                INSERT INTO trade_booking_requests (
                    id, portfolio_id, portfolio_name, instrument_type, underlying_symbol,
                    option_type, strike, maturity_date, quantity, status,
                    submitted_by_username, submitted_by_display_name, submitted_at
                )
                VALUES (?, ?, ?, 'EUROPEAN_OPTION', 'AAPL', 'CALL', ?, ?, ?, 'PENDING_VALIDATION', ?, ?, ?)
                """,
                UUID.randomUUID(),
                portfolioId,
                portfolioName,
                java.math.BigDecimal.valueOf(190),
                Date.valueOf(LocalDate.parse("2027-12-31")),
                java.math.BigDecimal.TEN,
                "fo.trader",
                "Fiona Trader",
                Timestamp.from(now)
        );
    }

    private record AuthClient(Cookie cookie, String csrfToken, UUID userId) {
    }
}
