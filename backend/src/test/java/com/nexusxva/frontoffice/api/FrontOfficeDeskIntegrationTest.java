package com.nexusxva.frontoffice.api;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.AbstractPostgresIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "nexusxva.auth.enabled=true",
        "nexusxva.auth.bootstrap-admin.username=fo-desk-admin",
        "nexusxva.auth.bootstrap-admin.password=fo-desk-password",
        "nexusxva.auth.bootstrap-admin.display-name=FO Desk Admin",
        "nexusxva.market-data.validation.enabled=false"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FrontOfficeDeskIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanBusinessState() {
        jdbcTemplate.update("DELETE FROM trade_booking_requests");
        jdbcTemplate.update("DELETE FROM portfolio_european_option_positions");
        jdbcTemplate.update("DELETE FROM portfolios");
        jdbcTemplate.update("DELETE FROM auth_user_feature_permission_overrides");
        jdbcTemplate.update("DELETE FROM auth_user_portfolio_access");
        jdbcTemplate.update("UPDATE auth_user_accounts SET portfolio_access_mode = 'ALL'");
    }

    @Test
    void foDeskAggregatesVisiblePortfoliosAndOwnBookings() throws Exception {
        AuthClient client = selectGroup(login(), "FO");
        String portfolioId = createPortfolio(client, "FO Desk Book");
        submitBooking(client, portfolioId, "AAPL");
        String confirmedBookingId = submitBooking(client, portfolioId, "MSFT");

        client = selectGroup(client, "BO");
        mockMvc.perform(post("/api/back-office/trade-bookings/{bookingId}/approve", confirmedBookingId)
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken()))
                .andExpect(status().isOk());

        client = selectGroup(client, "FO");
        mockMvc.perform(get("/api/front-office/desk").cookie(client.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("fo-desk-admin"))
                .andExpect(jsonPath("$.bookingCounts.pendingValidation").value(1))
                .andExpect(jsonPath("$.bookingCounts.confirmed").value(1))
                .andExpect(jsonPath("$.bookingCounts.rejected").value(0))
                .andExpect(jsonPath("$.bookingCounts.total").value(2))
                .andExpect(jsonPath("$.portfolios[?(@.id == '" + portfolioId + "')]", not(empty())))
                .andExpect(jsonPath("$.bookings", hasSize(2)))
                .andExpect(jsonPath("$.bookings[?(@.underlyingSymbol == 'AAPL')].portfolioVisible").value(true));
    }

    @Test
    void onlyFoCanReadDesk() throws Exception {
        AuthClient client = selectGroup(login(), "BO");
        mockMvc.perform(get("/api/front-office/desk").cookie(client.cookie()))
                .andExpect(status().isForbidden());

        client = selectGroup(client, "ADMIN");
        mockMvc.perform(get("/api/front-office/desk").cookie(client.cookie()))
                .andExpect(status().isForbidden());
    }

    @Test
    void hiddenPortfolioDoesNotDisappearFromBookingHistory() throws Exception {
        AuthClient client = selectGroup(login(), "FO");
        String portfolioId = createPortfolio(client, "Hidden History Book");
        submitBooking(client, portfolioId, "QQQ");

        jdbcTemplate.update(
                "UPDATE auth_user_accounts SET portfolio_access_mode = 'SELECTED' WHERE id = ?",
                client.userId()
        );

        mockMvc.perform(get("/api/front-office/desk").cookie(client.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolios", hasSize(0)))
                .andExpect(jsonPath("$.bookings", hasSize(1)))
                .andExpect(jsonPath("$.bookings[0].portfolioId").value(portfolioId))
                .andExpect(jsonPath("$.bookings[0].portfolioVisible").value(false));
    }

    private AuthClient login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "fo-desk-admin",
                                  "password": "fo-desk-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
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
                                { "group": "%s" }
                                """.formatted(group)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeGroup").value(group))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthClient(client.cookie(), body.get("csrfToken").asText(), client.userId());
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
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private record AuthClient(Cookie cookie, String csrfToken, UUID userId) {
    }
}
