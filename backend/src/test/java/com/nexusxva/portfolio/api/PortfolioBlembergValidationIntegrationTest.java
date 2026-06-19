package com.nexusxva.portfolio.api;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.AbstractPostgresIntegrationTest;
import com.nexusxva.marketdata.application.MarketDataInstrumentGateway;
import com.nexusxva.marketdata.domain.MarketInstrument;
import com.nexusxva.shared.error.ServiceUnavailableException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

@SpringBootTest(properties = "nexusxva.market-data.validation.enabled=true")
@AutoConfigureMockMvc
class PortfolioBlembergValidationIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MarketDataInstrumentGateway marketDataInstrumentGateway;

    @Test
    void createsPendingBookingWhenBlembergKnowsActiveSymbol() throws Exception {
        when(marketDataInstrumentGateway.findInstrument("AAPL"))
                .thenReturn(Optional.of(activeInstrument("AAPL")));
        String portfolioId = createdPortfolioId("Validated Book");

        addPosition(portfolioId, "aapl")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.underlyingSymbol").value("AAPL"))
                .andExpect(jsonPath("$.status").value("PENDING_VALIDATION"));
    }

    @Test
    void rejectsPositionWhenBlembergDoesNotKnowSymbol() throws Exception {
        when(marketDataInstrumentGateway.findInstrument("FAKE"))
                .thenReturn(Optional.empty());
        String portfolioId = createdPortfolioId("Unknown Symbol Book");

        addPosition(portfolioId, "FAKE")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Unknown underlyingSymbol"));
    }

    @Test
    void rejectsPositionWhenBlembergInstrumentIsInactive() throws Exception {
        when(marketDataInstrumentGateway.findInstrument("AAPL"))
                .thenReturn(Optional.of(new MarketInstrument("AAPL", false, "Apple Inc.", "EQUITY", "USD")));
        String portfolioId = createdPortfolioId("Inactive Symbol Book");

        addPosition(portfolioId, "AAPL")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Unknown underlyingSymbol"));
    }

    @Test
    void returnsServiceUnavailableWhenBlembergCannotBeReached() throws Exception {
        when(marketDataInstrumentGateway.findInstrument("AAPL"))
                .thenThrow(new ServiceUnavailableException("Market data service unavailable"));
        String portfolioId = createdPortfolioId("Unavailable Market Data Book");

        addPosition(portfolioId, "AAPL")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Service Unavailable"))
                .andExpect(jsonPath("$.message").value("Market data service unavailable"));
    }

    private org.springframework.test.web.servlet.ResultActions addPosition(
            String portfolioId,
            String underlyingSymbol
    ) throws Exception {
        return mockMvc.perform(post("/api/portfolios/{portfolioId}/trade-bookings/european-options", portfolioId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "underlyingSymbol": "%s",
                          "optionType": "CALL",
                          "strike": 100.0,
                          "maturityDate": "2027-12-31",
                          "quantity": 10.0
                        }
                        """.formatted(underlyingSymbol)));
    }

    private String createdPortfolioId(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s"
                                }
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asText();
    }

    private MarketInstrument activeInstrument(String symbol) {
        return new MarketInstrument(symbol, true, symbol + " Inc.", "EQUITY", "USD");
    }
}
