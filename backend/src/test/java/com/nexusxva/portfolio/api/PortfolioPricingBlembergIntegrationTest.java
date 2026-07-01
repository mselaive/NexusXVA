package com.nexusxva.portfolio.api;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.AbstractPostgresIntegrationTest;
import com.nexusxva.marketdata.application.MarketDataPricingInputGateway;
import com.nexusxva.marketdata.domain.MarketDataPricingInput;
import com.nexusxva.shared.error.ServiceUnavailableException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

@SpringBootTest(properties = "nexusxva.market-data.provider=blemberg")
@AutoConfigureMockMvc
class PortfolioPricingBlembergIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final Instant AS_OF = Instant.parse("2026-06-02T12:00:00Z");
    private static final LocalDate MATURITY_DATE = LocalDate.parse("2027-06-01");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MarketDataPricingInputGateway marketDataPricingInputGateway;

    @Test
    void pricesPortfolioWithBlembergPricingInputsAndReportsStaleFlag() throws Exception {
        when(marketDataPricingInputGateway.findEuropeanOptionPricingInput(eq("AAPL"), eq(MATURITY_DATE)))
                .thenReturn(Optional.of(pricingInput("AAPL", "USD", true)));
        String portfolioId = createdPortfolioId("Blemberg Pricing Book", "USD");
        String positionId = createdPositionId(portfolioId, "AAPL", "CALL", "190.0", "2027-06-01", "2.0");

        mockMvc.perform(post("/api/portfolios/{portfolioId}/pricing/black-scholes", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "valuationDate": "2026-06-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions[0].positionId").value(positionId))
                .andExpect(jsonPath("$.positions[0].marketData.source").value("BLEMBERG"))
                .andExpect(jsonPath("$.positions[0].marketData.stale").value(true))
                .andExpect(jsonPath("$.positions[0].marketData.dividendYield").value(0.005));
    }

    @Test
    void convertsNonUsdBlembergMarketDataToPortfolioBaseCurrency() throws Exception {
        when(marketDataPricingInputGateway.findEuropeanOptionPricingInput(eq("AAPL"), eq(MATURITY_DATE)))
                .thenReturn(Optional.of(pricingInput("AAPL", "EUR", false)));
        String portfolioId = createdPortfolioId("Non USD Market Data Book", "USD");
        createdPositionId(portfolioId, "AAPL", "CALL", "190.0", "2027-06-01", "1.0");

        mockMvc.perform(post("/api/portfolios/{portfolioId}/pricing/black-scholes", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "valuationDate": "2026-06-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.positions[0].marketData.currency").value("EUR"))
                .andExpect(jsonPath("$.positions[0].marketData.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.positions[0].marketData.fxRateToBase").isNumber());
    }

    @Test
    void mapsUnavailableBlembergPricingInputsToServiceUnavailable() throws Exception {
        when(marketDataPricingInputGateway.findEuropeanOptionPricingInput(eq("AAPL"), eq(MATURITY_DATE)))
                .thenThrow(new ServiceUnavailableException("Market data service unavailable"));
        String portfolioId = createdPortfolioId("Unavailable Blemberg Pricing Book", "USD");
        createdPositionId(portfolioId, "AAPL", "CALL", "190.0", "2027-06-01", "1.0");

        mockMvc.perform(post("/api/portfolios/{portfolioId}/pricing/black-scholes", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "valuationDate": "2026-06-01"
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("Market data service unavailable"));
    }

    @Test
    void portfolioPricingDoesNotPersistValuationResultsOnPositions() throws Exception {
        when(marketDataPricingInputGateway.findEuropeanOptionPricingInput(eq("AAPL"), eq(MATURITY_DATE)))
                .thenReturn(Optional.of(pricingInput("AAPL", "USD", false)));
        String portfolioId = createdPortfolioId("Stateless Pricing Book", "USD");
        String positionId = createdPositionId(portfolioId, "AAPL", "CALL", "190.0", "2027-06-01", "1.0");

        mockMvc.perform(post("/api/portfolios/{portfolioId}/pricing/black-scholes", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "valuationDate": "2026-06-01"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/portfolios/{portfolioId}/instruments/{positionId}", portfolioId, positionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(positionId))
                .andExpect(jsonPath("$.unitPrice").doesNotExist())
                .andExpect(jsonPath("$.positionPrice").doesNotExist())
                .andExpect(jsonPath("$.marketData").doesNotExist());
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

    private MarketDataPricingInput pricingInput(String symbol, String currency, boolean stale) {
        return new MarketDataPricingInput(
                symbol,
                190.0,
                0.22,
                0.045,
                0.005,
                currency,
                AS_OF,
                "BLEMBERG",
                stale
        );
    }
}
