package com.nexusxva.frontoffice.api;

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
import org.assertj.core.api.Assertions;
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
        "nexusxva.auth.bootstrap-admin.username=stress-admin",
        "nexusxva.auth.bootstrap-admin.password=stress-password",
        "nexusxva.auth.bootstrap-admin.display-name=Stress Admin",
        "nexusxva.market-data.provider=local",
        "nexusxva.market-data.validation.enabled=false"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FrontOfficeStressTestIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanBusinessState() {
        jdbcTemplate.update("DELETE FROM portfolio_position_eod_snapshots");
        jdbcTemplate.update("DELETE FROM portfolio_eod_runs");
        jdbcTemplate.update("DELETE FROM trade_booking_requests");
        jdbcTemplate.update("DELETE FROM portfolio_european_option_positions");
        jdbcTemplate.update("DELETE FROM portfolios");
        jdbcTemplate.update("DELETE FROM auth_user_feature_permission_overrides");
        jdbcTemplate.update("DELETE FROM auth_user_portfolio_access");
        jdbcTemplate.update("UPDATE auth_user_accounts SET portfolio_access_mode = 'ALL'");
    }

    @Test
    void foRunsScenarioMatrixForConfirmedPortfolio() throws Exception {
        AuthClient client = selectGroup(login(), "FO");
        String portfolioId = createPortfolio(client, "Stress Book");
        insertConfirmedEuropeanOptionPosition(
                UUID.fromString(portfolioId),
                "AAPL",
                "CALL",
                "190.0",
                "2027-06-01",
                "2.0"
        );

        MvcResult result = mockMvc.perform(post("/api/front-office/stress-tests/european-options")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portfolioId": "%s",
                                  "valuationDate": "2026-06-01",
                                  "scenarios": [
                                    {
                                      "name": "Base",
                                      "spotShockPercent": 0.0,
                                      "volatilityShockBps": 0.0,
                                      "riskFreeRateShockBps": 0.0,
                                      "dividendYieldShockBps": 0.0
                                    },
                                    {
                                      "name": "Spot Down Vol Up",
                                      "spotShockPercent": -0.10,
                                      "volatilityShockBps": 500.0,
                                      "riskFreeRateShockBps": 0.0,
                                      "dividendYieldShockBps": 0.0
                                    }
                                  ]
                                }
                                """.formatted(portfolioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value(portfolioId))
                .andExpect(jsonPath("$.model").value("BLACK_SCHOLES_STRESS_TEST_V1"))
                .andExpect(jsonPath("$.basePortfolio.totalPrice").isNumber())
                .andExpect(jsonPath("$.hypotheticalTrade").doesNotExist())
                .andExpect(jsonPath("$.scenarios", hasSize(2)))
                .andExpect(jsonPath("$.scenarios[0].impact.price").value(0.0))
                .andExpect(jsonPath("$.scenarios[1].positions[0].marketData.spot").value(171.0))
                .andExpect(jsonPath("$.scenarios[1].positions[0].marketData.volatility").value(0.27))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        double stressedImpact = body.get("scenarios").get(1).get("impact").get("price").asDouble();
        Assertions.assertThat(stressedImpact).isNotZero();
    }

    @Test
    void stressWithHypotheticalTradeDoesNotCreateBookingOrPosition() throws Exception {
        AuthClient client = selectGroup(login(), "FO");
        String portfolioId = createPortfolio(client, "Stress Hypo Book");

        mockMvc.perform(post("/api/front-office/stress-tests/european-options")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portfolioId": "%s",
                                  "valuationDate": "2026-06-01",
                                  "hypotheticalTrade": {
                                    "underlyingSymbol": "AAPL",
                                    "optionType": "CALL",
                                    "strike": 190,
                                    "maturityDate": "2027-06-01",
                                    "quantity": 2
                                  },
                                  "scenarios": [
                                    {
                                      "name": "Base",
                                      "spotShockPercent": 0.0,
                                      "volatilityShockBps": 0.0,
                                      "riskFreeRateShockBps": 0.0,
                                      "dividendYieldShockBps": 0.0
                                    }
                                  ]
                                }
                                """.formatted(portfolioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hypotheticalTrade.underlyingSymbol").value("AAPL"))
                .andExpect(jsonPath("$.scenarios[0].positions", hasSize(1)))
                .andExpect(jsonPath("$.scenarios[0].impact.price").isNumber());

        mockMvc.perform(get("/api/trade-bookings/mine").cookie(client.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        Integer positionCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM portfolio_european_option_positions WHERE portfolio_id = ?",
                Integer.class,
                UUID.fromString(portfolioId)
        );
        Assertions.assertThat(positionCount).isZero();
    }

    @Test
    void disabledPermissionBlocksFoStressTesting() throws Exception {
        AuthClient client = selectGroup(login(), "FO");
        String portfolioId = createPortfolio(client, "Blocked Stress Book");
        jdbcTemplate.update(
                """
                INSERT INTO auth_user_feature_permission_overrides (
                    user_id, permission_code, enabled, updated_at, updated_by_user_id, updated_by_username, updated_by_display_name
                )
                VALUES (?, 'FO_RUN_STRESS_TEST', FALSE, CURRENT_TIMESTAMP, ?, 'stress-admin', 'Stress Admin')
                """,
                client.userId(),
                client.userId()
        );

        mockMvc.perform(post("/api/front-office/stress-tests/european-options")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portfolioId": "%s",
                                  "valuationDate": "2026-06-01",
                                  "scenarios": [
                                    {
                                      "name": "Base",
                                      "spotShockPercent": 0.0,
                                      "volatilityShockBps": 0.0,
                                      "riskFreeRateShockBps": 0.0,
                                      "dividendYieldShockBps": 0.0
                                    }
                                  ]
                                }
                                """.formatted(portfolioId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("User is not allowed to use permission FO_RUN_STRESS_TEST"));
    }

    @Test
    void invalidShockReturnsBadRequest() throws Exception {
        AuthClient client = selectGroup(login(), "FO");
        String portfolioId = createPortfolio(client, "Invalid Stress Book");
        insertConfirmedEuropeanOptionPosition(
                UUID.fromString(portfolioId),
                "AAPL",
                "CALL",
                "190.0",
                "2027-06-01",
                "1.0"
        );

        mockMvc.perform(post("/api/front-office/stress-tests/european-options")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portfolioId": "%s",
                                  "valuationDate": "2026-06-01",
                                  "scenarios": [
                                    {
                                      "name": "Broken vol",
                                      "spotShockPercent": 0.0,
                                      "volatilityShockBps": -3000.0,
                                      "riskFreeRateShockBps": 0.0,
                                      "dividendYieldShockBps": 0.0
                                    }
                                  ]
                                }
                                """.formatted(portfolioId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Stressed volatility must be greater than zero"));
    }

    private AuthClient login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "stress-admin",
                                  "password": "stress-password"
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
