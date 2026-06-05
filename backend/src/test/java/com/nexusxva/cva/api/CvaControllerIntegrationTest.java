package com.nexusxva.cva.api;

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
class CvaControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void calculatesCvaForUsdPortfolio() throws Exception {
        String portfolioId = createdPortfolioId("CVA API Book", "USD");
        createdPosition(portfolioId, "AAPL", "CALL", "190.0", "2027-06-05", "2.0");

        mockMvc.perform(post("/api/risk/cva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(portfolioId, "0.60", "0.02")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioId").value(portfolioId))
                .andExpect(jsonPath("$.valuationDate").value("2026-06-05"))
                .andExpect(jsonPath("$.model").value("SIMPLIFIED_CVA_V1"))
                .andExpect(jsonPath("$.exposureModel").value("GBM_BLACK_SCHOLES_EXPOSURE_V1"))
                .andExpect(jsonPath("$.paths").value(20))
                .andExpect(jsonPath("$.timeSteps").value(3))
                .andExpect(jsonPath("$.lossGivenDefault").value(0.60))
                .andExpect(jsonPath("$.counterpartyHazardRate").value(0.02))
                .andExpect(jsonPath("$.discountRate").value(0.05))
                .andExpect(jsonPath("$.cva").isNumber())
                .andExpect(jsonPath("$.points", hasSize(3)))
                .andExpect(jsonPath("$.points[0].date").value("2026-07-05"))
                .andExpect(jsonPath("$.points[0].expectedExposure").isNumber())
                .andExpect(jsonPath("$.points[0].discountFactor").isNumber())
                .andExpect(jsonPath("$.points[0].survivalProbability").isNumber())
                .andExpect(jsonPath("$.points[0].defaultProbabilityIncrement").isNumber())
                .andExpect(jsonPath("$.points[0].cvaContribution").isNumber());
    }

    @Test
    void zeroHazardRateReturnsZeroCva() throws Exception {
        String portfolioId = createdPortfolioId("Zero Hazard CVA Book", "USD");
        createdPosition(portfolioId, "AAPL", "CALL", "190.0", "2027-06-05", "2.0");

        mockMvc.perform(post("/api/risk/cva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(portfolioId, "0.60", "0.0")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cva").value(0.0))
                .andExpect(jsonPath("$.points[0].defaultProbabilityIncrement").value(0.0));
    }

    @Test
    void nonUsdPortfolioRejectsCvaCalculation() throws Exception {
        String portfolioId = createdPortfolioId("EUR CVA Book", "EUR");

        mockMvc.perform(post("/api/risk/cva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(portfolioId, "0.60", "0.02")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Exposure simulation V1 supports USD baseCurrency only"));
    }

    @Test
    void invalidCreditParametersReturnValidationError() throws Exception {
        String portfolioId = createdPortfolioId("Invalid CVA Book", "USD");

        mockMvc.perform(post("/api/risk/cva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(portfolioId, "1.5", "0.02")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void unknownPortfolioReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/risk/cva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(java.util.UUID.randomUUID().toString(), "0.60", "0.02")))
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

    private String requestBody(String portfolioId, String lossGivenDefault, String counterpartyHazardRate) {
        return """
                {
                  "portfolioId": "%s",
                  "valuationDate": "2026-06-05",
                  "horizonDays": 90,
                  "timeSteps": 3,
                  "paths": 20,
                  "seed": 12345,
                  "pfeConfidenceLevel": 0.95,
                  "lossGivenDefault": %s,
                  "counterpartyHazardRate": %s,
                  "discountRate": 0.05
                }
                """.formatted(portfolioId, lossGivenDefault, counterpartyHazardRate);
    }
}
