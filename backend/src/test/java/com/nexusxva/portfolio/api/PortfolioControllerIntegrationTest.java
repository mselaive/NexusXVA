package com.nexusxva.portfolio.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
class PortfolioControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsAndFetchesPortfolio() throws Exception {
        MvcResult created = createPortfolio("Demo Portfolio")
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("/api/portfolios/")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("Demo Portfolio"))
                .andExpect(jsonPath("$.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.createdAt", notNullValue()))
                .andExpect(jsonPath("$.updatedAt", notNullValue()))
                .andExpect(jsonPath("$.positions", hasSize(0)))
                .andReturn();

        String portfolioId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/portfolios/{portfolioId}", portfolioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(portfolioId))
                .andExpect(jsonPath("$.name").value("Demo Portfolio"))
                .andExpect(jsonPath("$.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.positions", hasSize(0)));
    }

    @Test
    void listsPortfolioSummaries() throws Exception {
        String portfolioId = createdPortfolioId("Summary Book", "Listed portfolio", "eur");
        addPosition(portfolioId, "aapl", "CALL", "100.0", "2027-12-31", "10.0");

        MvcResult result = mockMvc.perform(get("/api/portfolios"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode portfolio = findPortfolioInArray(
                objectMapper.readTree(result.getResponse().getContentAsString()),
                portfolioId
        );

        assertThat(portfolio.get("name").asText()).isEqualTo("Summary Book");
        assertThat(portfolio.get("description").asText()).isEqualTo("Listed portfolio");
        assertThat(portfolio.get("baseCurrency").asText()).isEqualTo("EUR");
        assertThat(portfolio.get("positionCount").asLong()).isEqualTo(1);
    }

    @Test
    void updatesPortfolioMetadata() throws Exception {
        String portfolioId = createdPortfolioId("Original Book");

        mockMvc.perform(patch("/api/portfolios/{portfolioId}", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Book",
                                  "description": "Updated description",
                                  "baseCurrency": "gbp"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(portfolioId))
                .andExpect(jsonPath("$.name").value("Updated Book"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.baseCurrency").value("GBP"))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));

        mockMvc.perform(get("/api/portfolios/{portfolioId}", portfolioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Book"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.baseCurrency").value("GBP"));
    }

    @Test
    void deletesPortfolioAndPositions() throws Exception {
        String portfolioId = createdPortfolioId("Delete Book");
        addPosition(portfolioId, "AAPL", "CALL", "100.0", "2027-12-31", "10.0");

        mockMvc.perform(delete("/api/portfolios/{portfolioId}", portfolioId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/portfolios/{portfolioId}", portfolioId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Portfolio not found"));
    }

    @Test
    void addsEuropeanOptionPositionAndListsIt() throws Exception {
        String portfolioId = createdPortfolioId("Equity Options");

        MvcResult createdPosition = addPosition(portfolioId, "aapl", "CALL", "100.0", "2027-12-31", "10.0")
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, startsWith("/api/portfolios/" + portfolioId + "/instruments/")))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.portfolioId").value(portfolioId))
                .andExpect(jsonPath("$.underlyingSymbol").value("AAPL"))
                .andExpect(jsonPath("$.optionType").value("CALL"))
                .andExpect(jsonPath("$.strike").value(100.0))
                .andExpect(jsonPath("$.maturityDate").value("2027-12-31"))
                .andExpect(jsonPath("$.quantity").value(10.0))
                .andReturn();

        String positionId = objectMapper.readTree(createdPosition.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/portfolios/{portfolioId}/instruments", portfolioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(positionId))
                .andExpect(jsonPath("$[0].underlyingSymbol").value("AAPL"))
                .andExpect(jsonPath("$[0].optionType").value("CALL"));

        mockMvc.perform(get("/api/portfolios/{portfolioId}", portfolioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.positions", hasSize(1)))
                .andExpect(jsonPath("$.positions[0].id").value(positionId));
    }

    @Test
    void getsEuropeanOptionPositionById() throws Exception {
        String portfolioId = createdPortfolioId("Get Position Book");
        String positionId = createdPositionId(portfolioId);

        mockMvc.perform(get("/api/portfolios/{portfolioId}/instruments/{positionId}", portfolioId, positionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(positionId))
                .andExpect(jsonPath("$.portfolioId").value(portfolioId))
                .andExpect(jsonPath("$.underlyingSymbol").value("AAPL"));
    }

    @Test
    void updatesEuropeanOptionPosition() throws Exception {
        String portfolioId = createdPortfolioId("Update Position Book");
        String positionId = createdPositionId(portfolioId);

        mockMvc.perform(patch("/api/portfolios/{portfolioId}/instruments/european-options/{positionId}", portfolioId, positionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "underlyingSymbol": "msft",
                                  "optionType": "PUT",
                                  "strike": 95.5,
                                  "maturityDate": "2028-01-15",
                                  "quantity": -3.0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(positionId))
                .andExpect(jsonPath("$.underlyingSymbol").value("MSFT"))
                .andExpect(jsonPath("$.optionType").value("PUT"))
                .andExpect(jsonPath("$.strike").value(95.5))
                .andExpect(jsonPath("$.maturityDate").value("2028-01-15"))
                .andExpect(jsonPath("$.quantity").value(-3.0));

        mockMvc.perform(get("/api/portfolios/{portfolioId}/instruments/{positionId}", portfolioId, positionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.underlyingSymbol").value("MSFT"))
                .andExpect(jsonPath("$.optionType").value("PUT"));
    }

    @Test
    void deletesEuropeanOptionPosition() throws Exception {
        String portfolioId = createdPortfolioId("Delete Position Book");
        String positionId = createdPositionId(portfolioId);

        mockMvc.perform(delete("/api/portfolios/{portfolioId}/instruments/{positionId}", portfolioId, positionId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/portfolios/{portfolioId}/instruments", portfolioId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void unknownPortfolioReturnsApiError() throws Exception {
        UUID missingPortfolioId = UUID.randomUUID();

        mockMvc.perform(get("/api/portfolios/{portfolioId}", missingPortfolioId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Portfolio not found"))
                .andExpect(jsonPath("$.path").value("/api/portfolios/" + missingPortfolioId));
    }

    @Test
    void positionFromAnotherPortfolioReturnsNotFound() throws Exception {
        String firstPortfolioId = createdPortfolioId("First Book");
        String secondPortfolioId = createdPortfolioId("Second Book");
        String positionId = createdPositionId(firstPortfolioId);

        mockMvc.perform(get("/api/portfolios/{portfolioId}/instruments/{positionId}", secondPortfolioId, positionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Position not found"));
    }

    @Test
    void invalidPortfolioInputReturnsValidationErrorShape() throws Exception {
        mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    void invalidBaseCurrencyReturnsValidationErrorShape() throws Exception {
        mockMvc.perform(post("/api/portfolios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Currency Book",
                                  "baseCurrency": "US1"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0].field").value("baseCurrency"));
    }

    @Test
    void invalidEuropeanOptionInputReturnsValidationErrorShape() throws Exception {
        String portfolioId = createdPortfolioId("Validation Book");

        mockMvc.perform(post("/api/portfolios/{portfolioId}/instruments/european-options", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "underlyingSymbol": "AAPL",
                                  "optionType": "CALL",
                                  "strike": 0.0,
                                  "maturityDate": "2027-12-31",
                                  "quantity": 10.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0].field").value("strike"));
    }

    @Test
    void invalidUnderlyingSymbolReturnsValidationErrorShape() throws Exception {
        String portfolioId = createdPortfolioId("Symbol Book");

        mockMvc.perform(post("/api/portfolios/{portfolioId}/instruments/european-options", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "underlyingSymbol": "BAD SYMBOL",
                                  "optionType": "CALL",
                                  "strike": 100.0,
                                  "maturityDate": "2027-12-31",
                                  "quantity": 10.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details[0].field").value("underlyingSymbol"));
    }

    @Test
    void zeroQuantityReturnsBadRequest() throws Exception {
        String portfolioId = createdPortfolioId("Quantity Book");

        mockMvc.perform(post("/api/portfolios/{portfolioId}/instruments/european-options", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "underlyingSymbol": "AAPL",
                                  "optionType": "CALL",
                                  "strike": 100.0,
                                  "maturityDate": "2027-12-31",
                                  "quantity": 0.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("quantity must be non-zero"));
    }

    @Test
    void zeroQuantityUpdateReturnsBadRequest() throws Exception {
        String portfolioId = createdPortfolioId("Update Quantity Book");
        String positionId = createdPositionId(portfolioId);

        mockMvc.perform(patch("/api/portfolios/{portfolioId}/instruments/european-options/{positionId}", portfolioId, positionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 0.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("quantity must be non-zero"));
    }

    @Test
    void invalidOptionTypeReturnsCleanApiError() throws Exception {
        String portfolioId = createdPortfolioId("Enum Book");

        mockMvc.perform(post("/api/portfolios/{portfolioId}/instruments/european-options", portfolioId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "underlyingSymbol": "AAPL",
                                  "optionType": "DIGITAL",
                                  "strike": 100.0,
                                  "maturityDate": "2027-12-31",
                                  "quantity": 10.0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid optionType. Accepted values are CALL and PUT"));
    }

    private org.springframework.test.web.servlet.ResultActions createPortfolio(String name) throws Exception {
        return mockMvc.perform(post("/api/portfolios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "%s"
                        }
                        """.formatted(name)));
    }

    private org.springframework.test.web.servlet.ResultActions createPortfolio(
            String name,
            String description,
            String baseCurrency
    ) throws Exception {
        return mockMvc.perform(post("/api/portfolios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "%s",
                          "description": "%s",
                          "baseCurrency": "%s"
                        }
                        """.formatted(name, description, baseCurrency)));
    }

    private String createdPortfolioId(String name) throws Exception {
        MvcResult result = createPortfolio(name)
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asText();
    }

    private String createdPortfolioId(String name, String description, String baseCurrency) throws Exception {
        MvcResult result = createPortfolio(name, description, baseCurrency)
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asText();
    }

    private org.springframework.test.web.servlet.ResultActions addPosition(
            String portfolioId,
            String underlyingSymbol,
            String optionType,
            String strike,
            String maturityDate,
            String quantity
    ) throws Exception {
        return mockMvc.perform(post("/api/portfolios/{portfolioId}/instruments/european-options", portfolioId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "underlyingSymbol": "%s",
                          "optionType": "%s",
                          "strike": %s,
                          "maturityDate": "%s",
                          "quantity": %s
                        }
                        """.formatted(underlyingSymbol, optionType, strike, maturityDate, quantity)));
    }

    private String createdPositionId(String portfolioId) throws Exception {
        MvcResult result = addPosition(portfolioId, "AAPL", "CALL", "100.0", "2027-12-31", "10.0")
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asText();
    }

    private JsonNode findPortfolioInArray(JsonNode portfolios, String portfolioId) {
        for (JsonNode portfolio : portfolios) {
            if (portfolioId.equals(portfolio.get("id").asText())) {
                return portfolio;
            }
        }
        throw new AssertionError("Portfolio not found in response: " + portfolioId);
    }
}
