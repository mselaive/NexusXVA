package com.nexusxva.tradelifecycle.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.AbstractPostgresIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "nexusxva.auth.enabled=true",
        "nexusxva.market-data.provider=local",
        "nexusxva.market-data.validation.enabled=true"
})
@AutoConfigureMockMvc
class TradeLifecycleIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void clean() {
        jdbcTemplate.update("DELETE FROM user_notifications");
        jdbcTemplate.update("DELETE FROM trade_lifecycle_requests");
        jdbcTemplate.update("DELETE FROM trade_booking_requests");
        jdbcTemplate.update("DELETE FROM portfolio_european_option_positions");
        jdbcTemplate.update("DELETE FROM portfolios");
        jdbcTemplate.update("DELETE FROM auth_user_feature_permission_overrides");
    }

    @Test
    void foRequestsCancelAndBoApprovalExcludesPositionFromPricing() throws Exception {
        AuthClient fo = selectGroup(login("fo.trader", "fo12345"), "FO");
        AuthClient bo = selectGroup(login("bo.ops", "bo12345"), "BO");
        UUID portfolioId = insertPortfolio("Cancel Book");
        UUID positionId = insertConfirmedEuropeanOptionPosition(portfolioId, "AAPL", "CALL", "190.0", "2027-12-31", "10.0");

        UUID requestId = submitCancel(fo, positionId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestType").value("CANCEL"))
                .andExpect(jsonPath("$.status").value("PENDING_VALIDATION"))
                .andReturnId();

        mockMvc.perform(post("/api/back-office/lifecycle-requests/{requestId}/approve", requestId)
                        .cookie(bo.cookie())
                        .header("X-CSRF-Token", bo.csrfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/portfolios/{portfolioId}", portfolioId).cookie(fo.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions[0].lifecycleStatus").value("CANCELLED"));

        mockMvc.perform(post("/api/portfolios/{portfolioId}/pricing/black-scholes", portfolioId)
                        .cookie(fo.cookie())
                        .header("X-CSRF-Token", fo.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "valuationDate": "2026-06-21"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions.length()").value(0))
                .andExpect(jsonPath("$.totalPrice").value(0.0));
    }

    @Test
    void lifecycleReviewCreatesNotificationsForBoAndFo() throws Exception {
        AuthClient fo = selectGroup(login("fo.trader", "fo12345"), "FO");
        AuthClient bo = selectGroup(login("bo.ops", "bo12345"), "BO");
        UUID portfolioId = insertPortfolio("Notification Lifecycle Book");
        UUID positionId = insertConfirmedEuropeanOptionPosition(portfolioId, "AAPL", "CALL", "190.0", "2027-12-31", "10.0");

        UUID requestId = submitCancel(fo, positionId)
                .andExpect(status().isOk())
                .andReturnId();

        mockMvc.perform(get("/api/notifications").cookie(bo.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Lifecycle request awaiting BO"));

        mockMvc.perform(post("/api/back-office/lifecycle-requests/{requestId}/approve", requestId)
                        .cookie(bo.cookie())
                        .header("X-CSRF-Token", bo.csrfToken()))
                .andExpect(status().isOk());

        MvcResult inbox = mockMvc.perform(get("/api/notifications").cookie(fo.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Lifecycle request approved"))
                .andReturn();
        UUID notificationId = UUID.fromString(objectMapper.readTree(inbox.getResponse().getContentAsString())
                .get("items")
                .get(0)
                .get("id")
                .asText());

        mockMvc.perform(post("/api/notifications/{notificationId}/read", notificationId)
                        .cookie(fo.cookie())
                        .header("X-CSRF-Token", fo.csrfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").value(false));

        mockMvc.perform(get("/api/notifications").cookie(fo.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    void boApprovalOfAmendMarksOriginalAndCreatesReplacement() throws Exception {
        AuthClient fo = selectGroup(login("fo.trader", "fo12345"), "FO");
        AuthClient bo = selectGroup(login("bo.ops", "bo12345"), "BO");
        UUID portfolioId = insertPortfolio("Amend Book");
        UUID positionId = insertConfirmedEuropeanOptionPosition(portfolioId, "AAPL", "CALL", "190.0", "2027-12-31", "10.0");

        UUID requestId = submitAmend(fo, positionId, "MSFT", "PUT", "425.0", "2028-01-21", "-5.0")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestType").value("AMEND"))
                .andReturnId();

        mockMvc.perform(post("/api/back-office/lifecycle-requests/{requestId}/approve", requestId)
                        .cookie(bo.cookie())
                        .header("X-CSRF-Token", bo.csrfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.resultingPositionId").isNotEmpty());

        mockMvc.perform(get("/api/portfolios/{portfolioId}", portfolioId).cookie(fo.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions.length()").value(2))
                .andExpect(jsonPath("$.positions[?(@.id == '%s')].lifecycleStatus".formatted(positionId)).value("AMENDED"))
                .andExpect(jsonPath("$.positions[?(@.underlyingSymbol == 'MSFT')].lifecycleStatus").value("ACTIVE"));
    }

    @Test
    void duplicatePendingLifecycleRequestRejectsWithConflict() throws Exception {
        AuthClient fo = selectGroup(login("fo.trader", "fo12345"), "FO");
        UUID portfolioId = insertPortfolio("Duplicate Lifecycle Book");
        UUID positionId = insertConfirmedEuropeanOptionPosition(portfolioId, "AAPL", "CALL", "190.0", "2027-12-31", "10.0");

        submitCancel(fo, positionId).andExpect(status().isOk());

        submitCancel(fo, positionId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Position already has a pending lifecycle request"));
    }

    @Test
    void disabledLifecyclePermissionBlocksFo() throws Exception {
        AuthClient admin = selectGroup(login("raul", "multi12345"), "ADMIN");
        AuthClient fo = selectGroup(login("fo.trader", "fo12345"), "FO");
        UUID portfolioId = insertPortfolio("Permission Book");
        UUID positionId = insertConfirmedEuropeanOptionPosition(portfolioId, "AAPL", "CALL", "190.0", "2027-12-31", "10.0");

        mockMvc.perform(put("/api/admin/users/{userId}/permissions", fo.userId())
                        .cookie(admin.cookie())
                        .header("X-CSRF-Token", admin.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "permissions": {
                                    "FO_REQUEST_LIFECYCLE": false
                                  }
                                }
                                """))
                .andExpect(status().isOk());

        submitCancel(fo, positionId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is not allowed to use permission FO_REQUEST_LIFECYCLE"));
    }

    private ResultActionsWithId submitCancel(AuthClient client, UUID positionId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/front-office/lifecycle/positions/{positionId}/cancel", positionId)
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken()))
                .andReturn();
        return new ResultActionsWithId(result);
    }

    private ResultActionsWithId submitAmend(
            AuthClient client,
            UUID positionId,
            String symbol,
            String optionType,
            String strike,
            String maturityDate,
            String quantity
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/front-office/lifecycle/positions/{positionId}/amend", positionId)
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "underlyingSymbol": "%s",
                                  "optionType": "%s",
                                  "strike": %s,
                                  "maturityDate": "%s",
                                  "quantity": %s
                                }
                                """.formatted(symbol, optionType, strike, maturityDate, quantity)))
                .andReturn();
        return new ResultActionsWithId(result);
    }

    private AuthClient login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
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
        return new AuthClient(client.cookie(), body.get("csrfToken").asText(), client.userId());
    }

    private UUID insertPortfolio(String name) {
        UUID portfolioId = UUID.randomUUID();
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                INSERT INTO portfolios (id, name, description, base_currency, created_at, updated_at)
                VALUES (?, ?, ?, 'USD', ?, ?)
                """,
                portfolioId,
                name,
                "Lifecycle integration portfolio",
                Timestamp.from(now),
                Timestamp.from(now)
        );
        return portfolioId;
    }

    private record AuthClient(Cookie cookie, String csrfToken, UUID userId) {
    }

    private class ResultActionsWithId {
        private final MvcResult result;

        private ResultActionsWithId(MvcResult result) {
            this.result = result;
        }

        ResultActionsWithId andExpect(org.springframework.test.web.servlet.ResultMatcher matcher) throws Exception {
            matcher.match(result);
            return this;
        }

        UUID andReturnId() throws Exception {
            assertThat(result.getResponse().getStatus()).isBetween(200, 299);
            return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
        }
    }
}
