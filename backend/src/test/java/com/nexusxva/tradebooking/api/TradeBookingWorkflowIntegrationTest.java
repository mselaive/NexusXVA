package com.nexusxva.tradebooking.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.AbstractPostgresIntegrationTest;
import com.nexusxva.shared.error.ConflictException;
import com.nexusxva.tradebooking.application.TradeBookingService;
import com.nexusxva.tradebooking.domain.BookingActor;
import jakarta.servlet.http.Cookie;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "nexusxva.auth.enabled=true",
        "nexusxva.auth.bootstrap-admin.username=workflow-admin",
        "nexusxva.auth.bootstrap-admin.password=workflow-password",
        "nexusxva.auth.bootstrap-admin.display-name=Workflow Admin",
        "nexusxva.market-data.validation.enabled=false"
})
@AutoConfigureMockMvc
class TradeBookingWorkflowIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TradeBookingService tradeBookingService;

    @Test
    void activeGroupPersistsAndControlsAvailableApis() throws Exception {
        AuthClient client = login("workflow-admin", "workflow-password");

        client = selectGroup(client, "FO");
        mockMvc.perform(get("/api/auth/me").cookie(client.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeGroup").value("FO"));

        mockMvc.perform(get("/api/back-office/trade-bookings")
                        .cookie(client.cookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Active group is not authorized for this resource"));

        client = selectGroup(client, "BO");
        mockMvc.perform(get("/api/portfolios").cookie(client.cookie()))
                .andExpect(status().isForbidden());
    }

    @Test
    void pendingBookingBecomesExactlyOneConfirmedPositionAfterBoApproval() throws Exception {
        AuthClient client = selectGroup(login("workflow-admin", "workflow-password"), "FO");
        String portfolioId = createPortfolio(client, "Approval Workflow Book");
        String bookingId = submitBooking(client, portfolioId, "AAPL");

        mockMvc.perform(get("/api/portfolios/{portfolioId}", portfolioId).cookie(client.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions", hasSize(0)));

        mockMvc.perform(post("/api/back-office/trade-bookings/{bookingId}/approve", bookingId)
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken()))
                .andExpect(status().isForbidden());

        client = selectGroup(client, "BO");
        mockMvc.perform(post("/api/back-office/trade-bookings/{bookingId}/approve", bookingId)
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedPositionId", notNullValue()));

        mockMvc.perform(post("/api/back-office/trade-bookings/{bookingId}/approve", bookingId)
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Trade booking has already been reviewed"));

        client = selectGroup(client, "FO");
        mockMvc.perform(get("/api/portfolios/{portfolioId}", portfolioId).cookie(client.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions", hasSize(1)))
                .andExpect(jsonPath("$.positions[0].underlyingSymbol").value("AAPL"));
    }

    @Test
    void rejectedBookingKeepsPortfolioUnchangedAndRequiresReason() throws Exception {
        AuthClient client = selectGroup(login("workflow-admin", "workflow-password"), "FO");
        String portfolioId = createPortfolio(client, "Rejected Workflow Book");
        String bookingId = submitBooking(client, portfolioId, "MSFT");
        client = selectGroup(client, "BO");

        mockMvc.perform(post("/api/back-office/trade-bookings/{bookingId}/reject", bookingId)
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "rejectionReason": "" }
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/back-office/trade-bookings/{bookingId}/reject", bookingId)
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "rejectionReason": "Strike requires FO confirmation" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReason").value("Strike requires FO confirmation"));

        client = selectGroup(client, "FO");
        mockMvc.perform(get("/api/portfolios/{portfolioId}", portfolioId).cookie(client.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions", hasSize(0)));

        mockMvc.perform(get("/api/trade-bookings/mine").cookie(client.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == '" + bookingId + "')].status").value("REJECTED"));
    }

    @Test
    void portfolioWithPendingBookingCannotBeDeleted() throws Exception {
        AuthClient client = selectGroup(login("workflow-admin", "workflow-password"), "FO");
        String portfolioId = createPortfolio(client, "Pending Delete Guard Book");
        submitBooking(client, portfolioId, "QQQ");

        mockMvc.perform(delete("/api/portfolios/{portfolioId}", portfolioId)
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Portfolio has pending trade bookings"));
    }

    @Test
    void userCannotSelectGroupWithoutMembership() throws Exception {
        createFoOnlyUser();
        AuthClient client = login("fo-only", "fo-password");

        mockMvc.perform(post("/api/auth/active-group")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "group": "BO" }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is not assigned to the requested group"));
    }

    @Test
    void concurrentApprovalCreatesOnlyOneConfirmedPosition() throws Exception {
        AuthClient client = selectGroup(login("workflow-admin", "workflow-password"), "FO");
        String portfolioId = createPortfolio(client, "Concurrent Approval Book");
        String bookingId = submitBooking(client, portfolioId, "NVDA");
        CountDownLatch start = new CountDownLatch(1);
        BookingActor reviewer = BookingActor.system();
        Callable<String> approval = () -> {
            start.await();
            try {
                tradeBookingService.approve(UUID.fromString(bookingId), reviewer);
                return "APPROVED";
            } catch (ConflictException exception) {
                return "CONFLICT";
            }
        };

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<String> first = executor.submit(approval);
            Future<String> second = executor.submit(approval);
            start.countDown();

            List<String> outcomes = List.of(first.get(), second.get());
            assertThat(outcomes).containsExactlyInAnyOrder("APPROVED", "CONFLICT");
        }

        Integer positionCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM portfolio_european_option_positions WHERE portfolio_id = ?",
                Integer.class,
                UUID.fromString(portfolioId)
        );
        assertThat(positionCount).isEqualTo(1);
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
                .andExpect(jsonPath("$.authenticated").value(true))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthClient(
                result.getResponse().getCookie("NEXUSXVA_SESSION"),
                body.get("csrfToken").asText()
        );
    }

    private AuthClient selectGroup(AuthClient client, String group) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/active-group")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "group": "%s" }
                                """.formatted(group)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeGroup").value(group))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthClient(client.cookie(), body.get("csrfToken").asText());
    }

    private String createPortfolio(AuthClient client, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/portfolios")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "%s" }
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String submitBooking(AuthClient client, String portfolioId, String symbol) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/portfolios/{portfolioId}/trade-bookings/european-options", portfolioId)
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "underlyingSymbol": "%s",
                                  "optionType": "CALL",
                                  "strike": 100.0,
                                  "maturityDate": "2027-12-31",
                                  "quantity": 10.0
                                }
                                """.formatted(symbol)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_VALIDATION"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void createFoOnlyUser() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                INSERT INTO auth_user_accounts (
                    id, username, display_name, password_hash, active, created_at, updated_at
                )
                VALUES (?, 'fo-only', 'FO Only User', ?, TRUE, ?, ?)
                """,
                userId,
                new BCryptPasswordEncoder(12).encode("fo-password"),
                Timestamp.from(now),
                Timestamp.from(now)
        );
        jdbcTemplate.update(
                """
                INSERT INTO auth_user_group_memberships (user_id, group_id)
                SELECT ?, id FROM auth_groups WHERE code = 'FO'
                """,
                userId
        );
    }

    private record AuthClient(Cookie cookie, String csrfToken) {
    }
}
