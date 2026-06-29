package com.nexusxva.portfolio.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.AbstractPostgresIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "nexusxva.market-data.provider=local")
@AutoConfigureMockMvc
class PortfolioPricingControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void pricesPortfolioWithLocalMarketDataInputs() throws Exception {
        String portfolioId = createdPortfolioId("Portfolio Pricing Book", "USD");
        String positionId = createdPositionId(portfolioId, "AAPL", "CALL", "190.0", "2027-06-01", "2.0");

        mockMvc.perform(post("/api/portfolios/{portfolioId}/pricing/black-scholes", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "valuationDate": "2026-06-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value(portfolioId))
                .andExpect(jsonPath("$.valuationDate").value("2026-06-01"))
                .andExpect(jsonPath("$.model").value("BLACK_SCHOLES"))
                .andExpect(jsonPath("$.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.totalPrice").isNumber())
                .andExpect(jsonPath("$.totalGreeks.delta").isNumber())
                .andExpect(jsonPath("$.positions", hasSize(1)))
                .andExpect(jsonPath("$.positions[0].positionId").value(positionId))
                .andExpect(jsonPath("$.positions[0].status").value("PRICED"))
                .andExpect(jsonPath("$.positions[0].underlyingSymbol").value("AAPL"))
                .andExpect(jsonPath("$.positions[0].quantity").value(2.0))
                .andExpect(jsonPath("$.positions[0].unitPrice").isNumber())
                .andExpect(jsonPath("$.positions[0].positionPrice").isNumber())
                .andExpect(jsonPath("$.positions[0].unitGreeks.vega").isNumber())
                .andExpect(jsonPath("$.positions[0].positionGreeks.vega").isNumber())
                .andExpect(jsonPath("$.positions[0].marketData.spot").value(190.0))
                .andExpect(jsonPath("$.positions[0].marketData.volatility").value(0.22))
                .andExpect(jsonPath("$.positions[0].marketData.riskFreeRate").value(0.045))
                .andExpect(jsonPath("$.positions[0].marketData.dividendYield").value(0.005))
                .andExpect(jsonPath("$.positions[0].marketData.currency").value("USD"))
                .andExpect(jsonPath("$.positions[0].marketData.source").value("LOCAL"))
                .andExpect(jsonPath("$.positions[0].marketData.asOf", notNullValue()))
                .andExpect(jsonPath("$.positions[0].marketData.stale").value(false))
                .andExpect(jsonPath("$.unpriceablePositions", hasSize(0)));

        Integer runCount = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM valuation_runs
                WHERE portfolio_id = ?
                  AND run_type = 'PRICING'
                  AND status = 'SUCCESS'
                  AND input_json->>'valuationDate' = '2026-06-01'
                  AND summary_json->>'pricedPositions' = '1'
                """,
                Integer.class,
                java.util.UUID.fromString(portfolioId)
        );
        assertThat(runCount).isEqualTo(1);
    }

    @Test
    void aggregatesPricedPositionsAndReportsExpiredPositionsSeparately() throws Exception {
        String portfolioId = createdPortfolioId("Mixed Pricing Book", "USD");
        createdPositionId(portfolioId, "AAPL", "CALL", "190.0", "2027-06-01", "2.0");
        createdPositionId(portfolioId, "MSFT", "PUT", "400.0", "2027-06-01", "-1.0");
        String expiredPositionId = createdPositionId(portfolioId, "QQQ", "CALL", "440.0", "2026-06-01", "5.0");

        mockMvc.perform(post("/api/portfolios/{portfolioId}/pricing/black-scholes", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "valuationDate": "2026-06-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions", hasSize(2)))
                .andExpect(jsonPath("$.positions[0].status").value("PRICED"))
                .andExpect(jsonPath("$.positions[1].status").value("PRICED"))
                .andExpect(jsonPath("$.positions[1].quantity").value(-1.0))
                .andExpect(jsonPath("$.unpriceablePositions", hasSize(1)))
                .andExpect(jsonPath("$.unpriceablePositions[0].positionId").value(expiredPositionId))
                .andExpect(jsonPath("$.unpriceablePositions[0].status").value("UNPRICEABLE_EXPIRED"))
                .andExpect(jsonPath("$.unpriceablePositions[0].reason")
                        .value("Position maturityDate must be after valuationDate for Black-Scholes pricing"));
    }

    @Test
    void emptyPortfolioReturnsZeroTotals() throws Exception {
        String portfolioId = createdPortfolioId("Empty Pricing Book", "USD");

        mockMvc.perform(post("/api/portfolios/{portfolioId}/pricing/black-scholes", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPrice").value(0.0))
                .andExpect(jsonPath("$.totalGreeks.delta").value(0.0))
                .andExpect(jsonPath("$.positions", hasSize(0)))
                .andExpect(jsonPath("$.unpriceablePositions", hasSize(0)));
    }

    @Test
    void nonUsdPortfolioReturnsBadRequest() throws Exception {
        String portfolioId = createdPortfolioId("EUR Pricing Book", "EUR");

        mockMvc.perform(post("/api/portfolios/{portfolioId}/pricing/black-scholes", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "valuationDate": "2026-06-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Portfolio pricing V1 supports USD baseCurrency only"));
    }

    @Test
    void missingMarketDataInputsReturnBadRequest() throws Exception {
        String portfolioId = createdPortfolioId("Missing Market Data Book", "USD");
        String positionId = createdPositionId(portfolioId, "AAPL", "CALL", "190.0", "2027-06-01", "1.0");
        patchPositionSymbol(portfolioId, positionId, "FAKE");

        mockMvc.perform(post("/api/portfolios/{portfolioId}/pricing/black-scholes", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "valuationDate": "2026-06-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Market data pricing inputs unavailable for underlyingSymbol"));
    }

    @Test
    void unknownPortfolioReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/portfolios/{portfolioId}/pricing/black-scholes", java.util.UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "valuationDate": "2026-06-01"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Portfolio not found"));
    }

    private String createdPortfolioId(String name, String baseCurrency) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "baseCurrency": "%s"
                                }
                                """.formatted(name, baseCurrency)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asText();
    }

    private String createdPositionId(
            String portfolioId,
            String underlyingSymbol,
            String optionType,
            String strike,
            String maturityDate,
            String quantity
    ) {
        return insertConfirmedEuropeanOptionPosition(
                java.util.UUID.fromString(portfolioId),
                underlyingSymbol,
                optionType,
                strike,
                maturityDate,
                quantity
        ).toString();
    }

    private void patchPositionSymbol(String portfolioId, String positionId, String underlyingSymbol) {
        jdbcTemplate.update(
                """
                UPDATE portfolio_european_option_positions
                SET underlying_symbol = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND portfolio_id = ?
                """,
                underlyingSymbol,
                java.util.UUID.fromString(positionId),
                java.util.UUID.fromString(portfolioId)
        );
    }
}
