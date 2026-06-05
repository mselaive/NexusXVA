package com.nexusxva.simulation.api;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.AbstractPostgresIntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "nexusxva.market-data.provider=local")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ExposureSimulationControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void simulatesExposureForUsdPortfolio() throws Exception {
        String portfolioId = createdPortfolioId("Exposure API Book", "USD");
        createdPosition(portfolioId, "AAPL", "CALL", "190.0", "2027-06-05", "2.0");

        mockMvc.perform(post("/api/simulations/exposure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(portfolioId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value(portfolioId))
                .andExpect(jsonPath("$.valuationDate").value("2026-06-05"))
                .andExpect(jsonPath("$.model").value("GBM_BLACK_SCHOLES_EXPOSURE_V1"))
                .andExpect(jsonPath("$.paths").value(20))
                .andExpect(jsonPath("$.timeSteps").value(3))
                .andExpect(jsonPath("$.pfeConfidenceLevel").value(0.95))
                .andExpect(jsonPath("$.points", hasSize(3)))
                .andExpect(jsonPath("$.points[0].date").value("2026-07-05"))
                .andExpect(jsonPath("$.points[0].expectedExposure").isNumber())
                .andExpect(jsonPath("$.points[0].expectedNegativeExposure").isNumber())
                .andExpect(jsonPath("$.points[0].pfe").isNumber());
    }

    @Test
    void nonUsdPortfolioRejectsExposureSimulation() throws Exception {
        String portfolioId = createdPortfolioId("EUR Exposure Book", "EUR");

        mockMvc.perform(post("/api/simulations/exposure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(portfolioId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Exposure simulation V1 supports USD baseCurrency only"));
    }

    @Test
    void missingMarketDataReturnsBadRequest() throws Exception {
        String portfolioId = createdPortfolioId("Missing Exposure Market Data Book", "USD");
        createdPosition(portfolioId, "FAKE", "CALL", "100.0", "2027-06-05", "1.0");

        mockMvc.perform(post("/api/simulations/exposure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(portfolioId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Market data pricing inputs unavailable for underlyingSymbol"));
    }

    @Test
    void invalidRequestReturnsValidationError() throws Exception {
        String portfolioId = createdPortfolioId("Invalid Exposure Book", "USD");

        mockMvc.perform(post("/api/simulations/exposure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portfolioId": "%s",
                                  "valuationDate": "2026-06-05",
                                  "horizonDays": 90,
                                  "timeSteps": 3,
                                  "paths": 0,
                                  "seed": 12345,
                                  "pfeConfidenceLevel": 0.95
                                }
                                """.formatted(portfolioId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void timeStepsGreaterThanHorizonDaysReturnsBadRequest() throws Exception {
        String portfolioId = createdPortfolioId("Step Validation Exposure Book", "USD");

        mockMvc.perform(post("/api/simulations/exposure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "portfolioId": "%s",
                                  "valuationDate": "2026-06-05",
                                  "horizonDays": 2,
                                  "timeSteps": 3,
                                  "paths": 20,
                                  "seed": 12345,
                                  "pfeConfidenceLevel": 0.95
                                }
                                """.formatted(portfolioId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("timeSteps must be less than or equal to horizonDays"));
    }

    @Test
    void unknownPortfolioReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/simulations/exposure")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(java.util.UUID.randomUUID().toString())))
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

    private void createdPosition(
            String portfolioId,
            String symbol,
            String optionType,
            String strike,
            String maturityDate,
            String quantity
    ) throws Exception {
        mockMvc.perform(post("/api/portfolios/{portfolioId}/instruments/european-options", portfolioId)
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
                .andExpect(status().isCreated());
    }

    private String requestBody(String portfolioId) {
        return """
                {
                  "portfolioId": "%s",
                  "valuationDate": "2026-06-05",
                  "horizonDays": 90,
                  "timeSteps": 3,
                  "paths": 20,
                  "seed": 12345,
                  "pfeConfidenceLevel": 0.95
                }
                """.formatted(portfolioId);
    }
}
