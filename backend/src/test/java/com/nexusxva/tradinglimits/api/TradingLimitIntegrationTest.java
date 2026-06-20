package com.nexusxva.tradinglimits.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.AbstractPostgresIntegrationTest;
import com.nexusxva.tradebooking.application.CreateEuropeanOptionBookingCommand;
import com.nexusxva.tradebooking.application.TradeBookingService;
import com.nexusxva.tradebooking.domain.BookingActor;
import com.nexusxva.tradinglimits.application.TradingLimitExceededException;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
        "nexusxva.auth.bootstrap-admin.username=limits-admin",
        "nexusxva.auth.bootstrap-admin.password=limits-password",
        "nexusxva.auth.bootstrap-admin.display-name=Limits Admin",
        "nexusxva.market-data.validation.enabled=false"
})
@AutoConfigureMockMvc
class TradingLimitIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TradeBookingService tradeBookingService;

    @BeforeEach
    void clearLimitUsage() {
        jdbcTemplate.update("DELETE FROM trade_booking_requests");
        jdbcTemplate.update("DELETE FROM trading_limit_policies");
    }

    @Test
    void boCanConfigurePolicyAndFoCanReadOwnRemainingCapacity() throws Exception {
        AuthClient client = login();
        client = selectGroup(client, "BO");

        mockMvc.perform(put("/api/back-office/trading-limits/users/{userId}", client.userId())
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson(2, 10, "1000", "5000", true, null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.policy.notionalCurrency").value("USD"))
                .andExpect(jsonPath("$.policy.version").value(0))
                .andExpect(jsonPath("$.remaining.tradesThisHour").value(2));

        client = selectGroup(client, "FO");
        mockMvc.perform(get("/api/trading-limits/me").cookie(client.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(client.userId().toString()))
                .andExpect(jsonPath("$.remaining.notionalToday").value(5000));

        mockMvc.perform(get("/api/back-office/trading-limits/users").cookie(client.cookie()))
                .andExpect(status().isForbidden());
    }

    @Test
    void exceededTradeLimitReturnsMetadataAndDoesNotPersistSecondBooking() throws Exception {
        AuthClient client = selectGroup(login(), "BO");
        configurePolicy(client, 1, 1, null, null, true);
        client = selectGroup(client, "FO");
        String portfolioId = createPortfolio(client, "Limit Breach Book", "USD");

        submitBooking(client, portfolioId, "AAPL", "100", "10")
                .andExpect(status().isCreated());
        submitBooking(client, portfolioId, "MSFT", "120", "5")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Trading limit exceeded: TRADES_PER_HOUR"))
                .andExpect(jsonPath("$.metadata.limitType").value("TRADES_PER_HOUR"))
                .andExpect(jsonPath("$.metadata.maximum").value(1))
                .andExpect(jsonPath("$.metadata.currentUsage").value(1))
                .andExpect(jsonPath("$.metadata.requested").value(1))
                .andExpect(jsonPath("$.metadata.periodEndsAt", notNullValue()));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM trade_booking_requests WHERE portfolio_id = ?",
                Integer.class,
                UUID.fromString(portfolioId)
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    void rejectedBookingStillConsumesLimitAndDisabledPolicyStopsBlocking() throws Exception {
        AuthClient client = selectGroup(login(), "BO");
        configurePolicy(client, 1, 1, null, null, true);
        client = selectGroup(client, "FO");
        String portfolioId = createPortfolio(client, "Rejected Usage Book", "USD");
        String bookingId = bookingId(submitBooking(client, portfolioId, "AAPL", "100", "1")
                .andExpect(status().isCreated())
                .andReturn());

        client = selectGroup(client, "BO");
        mockMvc.perform(post("/api/back-office/trade-bookings/{bookingId}/reject", bookingId)
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectionReason\":\"Terms need correction\"}"))
                .andExpect(status().isOk());

        client = selectGroup(client, "FO");
        submitBooking(client, portfolioId, "MSFT", "100", "1")
                .andExpect(status().isConflict());

        client = selectGroup(client, "BO");
        configurePolicy(client, 1, 1, null, null, false);
        client = selectGroup(client, "FO");
        submitBooking(client, portfolioId, "MSFT", "100", "1")
                .andExpect(status().isCreated());
    }

    @Test
    void activeNotionalLimitRejectsNonUsdPortfolio() throws Exception {
        AuthClient client = selectGroup(login(), "BO");
        configurePolicy(client, null, null, "1000", "5000", true);
        client = selectGroup(client, "FO");
        String portfolioId = createPortfolio(client, "EUR Limit Book", "EUR");

        submitBooking(client, portfolioId, "AAPL", "100", "1")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Active notional limits support USD portfolios only"));
    }

    @Test
    void concurrentBookingsCannotJointlyExceedPolicy() throws Exception {
        AuthClient client = selectGroup(login(), "BO");
        configurePolicy(client, 1, 1, null, null, true);
        client = selectGroup(client, "FO");
        UUID portfolioId = UUID.fromString(createPortfolio(client, "Concurrent Limits Book", "USD"));
        BookingActor actor = new BookingActor(client.userId(), "limits-admin", "Limits Admin");
        CountDownLatch start = new CountDownLatch(1);

        Callable<String> submit = () -> {
            start.await();
            try {
                tradeBookingService.submitEuropeanOption(
                        portfolioId,
                        new CreateEuropeanOptionBookingCommand(
                                "AAPL",
                                com.nexusxva.instruments.domain.OptionType.CALL,
                                new BigDecimal("100"),
                                LocalDate.parse("2027-12-31"),
                                BigDecimal.ONE
                        ),
                        actor
                );
                return "CREATED";
            } catch (TradingLimitExceededException exception) {
                return "CONFLICT";
            }
        };

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<String> first = executor.submit(submit);
            Future<String> second = executor.submit(submit);
            start.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder("CREATED", "CONFLICT");
        }
    }

    private AuthClient login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "limits-admin",
                                  "password": "limits-password"
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
                        .content("{\"group\":\"" + group + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthClient(client.cookie(), body.get("csrfToken").asText(), client.userId());
    }

    private void configurePolicy(
            AuthClient client,
            Integer hourlyTrades,
            Integer dailyTrades,
            String hourlyNotional,
            String dailyNotional,
            boolean active
    ) throws Exception {
        Long version = jdbcTemplate.query(
                "SELECT version FROM trading_limit_policies WHERE user_id = ?",
                rs -> rs.next() ? rs.getLong(1) : null,
                client.userId()
        );
        mockMvc.perform(put("/api/back-office/trading-limits/users/{userId}", client.userId())
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(policyJson(hourlyTrades, dailyTrades, hourlyNotional, dailyNotional, active, version)))
                .andExpect(status().isOk());
    }

    private String createPortfolio(AuthClient client, String name, String currency) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/portfolios")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"baseCurrency\":\"" + currency + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private org.springframework.test.web.servlet.ResultActions submitBooking(
            AuthClient client,
            String portfolioId,
            String symbol,
            String strike,
            String quantity
    ) throws Exception {
        return mockMvc.perform(post("/api/portfolios/{portfolioId}/trade-bookings/european-options", portfolioId)
                .cookie(client.cookie())
                .header("X-CSRF-Token", client.csrfToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "underlyingSymbol": "%s",
                          "optionType": "CALL",
                          "strike": %s,
                          "maturityDate": "2027-12-31",
                          "quantity": %s
                        }
                        """.formatted(symbol, strike, quantity)));
    }

    private String bookingId(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String policyJson(
            Integer hourlyTrades,
            Integer dailyTrades,
            String hourlyNotional,
            String dailyNotional,
            boolean active,
            Long version
    ) {
        return """
                {
                  "maxTradesPerHour": %s,
                  "maxTradesPerDay": %s,
                  "maxNotionalPerHour": %s,
                  "maxNotionalPerDay": %s,
                  "active": %s,
                  "version": %s
                }
                """.formatted(
                jsonNumber(hourlyTrades),
                jsonNumber(dailyTrades),
                jsonNumber(hourlyNotional),
                jsonNumber(dailyNotional),
                active,
                jsonNumber(version)
        );
    }

    private String jsonNumber(Object value) {
        return value == null ? "null" : value.toString();
    }

    private record AuthClient(Cookie cookie, String csrfToken, UUID userId) {
    }
}
