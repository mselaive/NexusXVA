package com.nexusxva.frontoffice.api;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasSize;
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
        "nexusxva.auth.bootstrap-admin.username=what-if-admin",
        "nexusxva.auth.bootstrap-admin.password=what-if-password",
        "nexusxva.auth.bootstrap-admin.display-name=What If Admin",
        "nexusxva.market-data.provider=local",
        "nexusxva.market-data.validation.enabled=false"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FrontOfficeWhatIfIntegrationTest extends AbstractPostgresIntegrationTest {

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
    void foRunsWhatIfWithoutCreatingBookingOrPosition() throws Exception {
        AuthClient client = selectGroup(login(), "FO");
        String portfolioId = createPortfolio(client, "What If Book");
        insertConfirmedEuropeanOptionPosition(
                UUID.fromString(portfolioId),
                "MSFT",
                "CALL",
                "400.0",
                "2027-06-01",
                "1.0"
        );

        MvcResult result = mockMvc.perform(post("/api/front-office/what-if/european-option")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portfolioId": "%s",
                                  "valuationDate": "2026-06-01",
                                  "trade": {
                                    "underlyingSymbol": "AAPL",
                                    "optionType": "CALL",
                                    "strike": 190,
                                    "maturityDate": "2027-06-01",
                                    "quantity": 2
                                  }
                                }
                                """.formatted(portfolioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value(portfolioId))
                .andExpect(jsonPath("$.valuationDate").value("2026-06-01"))
                .andExpect(jsonPath("$.model").value("BLACK_SCHOLES_PRE_TRADE_WHAT_IF_V1"))
                .andExpect(jsonPath("$.basePortfolio.totalPrice").isNumber())
                .andExpect(jsonPath("$.hypotheticalTrade.underlyingSymbol").value("AAPL"))
                .andExpect(jsonPath("$.hypotheticalTrade.quantity").value(2.0))
                .andExpect(jsonPath("$.hypotheticalTrade.marketData.spot").value(190.0))
                .andExpect(jsonPath("$.impact.price").isNumber())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        double basePrice = body.get("basePortfolio").get("totalPrice").asDouble();
        double impactPrice = body.get("impact").get("price").asDouble();
        mockMvc.perform(post("/api/front-office/what-if/european-option")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portfolioId": "%s",
                                  "valuationDate": "2026-06-01",
                                  "trade": {
                                    "underlyingSymbol": "AAPL",
                                    "optionType": "CALL",
                                    "strike": 190,
                                    "maturityDate": "2027-06-01",
                                    "quantity": 2
                                  }
                                }
                                """.formatted(portfolioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.withTradePortfolio.totalPrice", closeTo(basePrice + impactPrice, 0.000001)));

        mockMvc.perform(get("/api/trade-bookings/mine").cookie(client.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        Integer positionCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM portfolio_european_option_positions WHERE portfolio_id = ?",
                Integer.class,
                UUID.fromString(portfolioId)
        );
        org.assertj.core.api.Assertions.assertThat(positionCount).isEqualTo(1);
    }

    @Test
    void boAndAdminCannotRunWhatIf() throws Exception {
        AuthClient client = selectGroup(login(), "BO");
        mockMvc.perform(post("/api/front-office/what-if/european-option")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        client = selectGroup(client, "ADMIN");
        mockMvc.perform(post("/api/front-office/what-if/european-option")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void disabledPermissionBlocksFoWhatIf() throws Exception {
        AuthClient client = selectGroup(login(), "FO");
        String portfolioId = createPortfolio(client, "Blocked What If Book");
        jdbcTemplate.update(
                """
                INSERT INTO auth_user_feature_permission_overrides (
                    user_id, permission_code, enabled, updated_at, updated_by_user_id, updated_by_username, updated_by_display_name
                )
                VALUES (?, 'FO_RUN_WHAT_IF', FALSE, CURRENT_TIMESTAMP, ?, 'what-if-admin', 'What If Admin')
                """,
                client.userId(),
                client.userId()
        );

        mockMvc.perform(post("/api/front-office/what-if/european-option")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portfolioId": "%s",
                                  "valuationDate": "2026-06-01",
                                  "trade": {
                                    "underlyingSymbol": "AAPL",
                                    "optionType": "CALL",
                                    "strike": 190,
                                    "maturityDate": "2027-06-01",
                                    "quantity": 2
                                  }
                                }
                                """.formatted(portfolioId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is not allowed to use permission FO_RUN_WHAT_IF"));
    }

    @Test
    void expiredHypotheticalTradeReturnsBadRequest() throws Exception {
        AuthClient client = selectGroup(login(), "FO");
        String portfolioId = createPortfolio(client, "Expired What If Book");

        mockMvc.perform(post("/api/front-office/what-if/european-option")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portfolioId": "%s",
                                  "valuationDate": "2026-06-01",
                                  "trade": {
                                    "underlyingSymbol": "AAPL",
                                    "optionType": "CALL",
                                    "strike": 190,
                                    "maturityDate": "2026-06-01",
                                    "quantity": 2
                                  }
                                }
                                """.formatted(portfolioId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Hypothetical trade maturityDate must be after valuationDate for Black-Scholes what-if"));
    }

    private AuthClient login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "what-if-admin",
                                  "password": "what-if-password"
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

    private record AuthClient(Cookie cookie, String csrfToken, UUID userId) {
    }
}
