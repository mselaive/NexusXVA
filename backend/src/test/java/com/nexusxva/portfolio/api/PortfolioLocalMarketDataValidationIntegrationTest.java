package com.nexusxva.portfolio.api;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "nexusxva.market-data.validation.enabled=true",
        "nexusxva.market-data.provider=local"
})
@AutoConfigureMockMvc
class PortfolioLocalMarketDataValidationIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void localWatchlistAllowsKnownSymbol() throws Exception {
        String portfolioId = createdPortfolioId("Local Watchlist Book");

        addPosition(portfolioId, "qqq")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.underlyingSymbol").value("QQQ"));
    }

    @Test
    void localWatchlistRejectsUnknownSymbol() throws Exception {
        String portfolioId = createdPortfolioId("Local Unknown Book");

        addPosition(portfolioId, "FAKE")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Unknown underlyingSymbol"));
    }

    private org.springframework.test.web.servlet.ResultActions addPosition(
            String portfolioId,
            String underlyingSymbol
    ) throws Exception {
        return mockMvc.perform(post("/api/portfolios/{portfolioId}/instruments/european-options", portfolioId)
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
}
