package com.nexusxva.xva.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.AbstractPostgresIntegrationTest;
import com.nexusxva.marketdata.application.MarketDataCurveGateway;
import com.nexusxva.marketdata.domain.MarketCreditCurve;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "nexusxva.auth.enabled=true",
        "nexusxva.auth.bootstrap-admin.username=xva-admin",
        "nexusxva.auth.bootstrap-admin.password=xva-admin-password",
        "nexusxva.auth.bootstrap-admin.display-name=XVA Admin"
})
@AutoConfigureMockMvc
class XvaReferenceDataControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MarketDataCurveGateway marketDataCurveGateway;

    @Test
    void adminCreatesCounterparty() throws Exception {
        AuthClient client = selectGroup(login(), "ADMIN");

        mockMvc.perform(post("/api/xva/counterparties")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Demo Prime Broker",
                                  "externalId": "DPB-001",
                                  "creditRating": "A"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Demo Prime Broker"))
                .andExpect(jsonPath("$.externalId").value("DPB-001"))
                .andExpect(jsonPath("$.creditRating").value("A"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void duplicateCounterpartyReturnsConflict() throws Exception {
        AuthClient client = selectGroup(login(), "ADMIN");
        String body = """
                {
                  "name": "Duplicate Prime Broker",
                  "externalId": "DPB-DUP",
                  "creditRating": "A"
                }
                """;

        mockMvc.perform(post("/api/xva/counterparties")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/xva/counterparties")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Counterparty already exists"));
    }

    @Test
    void adminCreatesNettingSetForCounterparty() throws Exception {
        AuthClient client = selectGroup(login(), "ADMIN");
        UUID counterpartyId = createCounterparty(client, "Netting Demo Broker", "NDB-001");

        mockMvc.perform(post("/api/xva/netting-sets")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "counterpartyId": "%s",
                                  "name": "Netting Demo Broker USD CSA",
                                  "baseCurrency": "USD",
                                  "collateralAmount": 250000,
                                  "collateralCurrency": "USD"
                                }
                                """.formatted(counterpartyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counterpartyId").value(counterpartyId.toString()))
                .andExpect(jsonPath("$.counterpartyName").value("Netting Demo Broker"))
                .andExpect(jsonPath("$.name").value("Netting Demo Broker USD CSA"))
                .andExpect(jsonPath("$.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.collateralAmount").value(250000))
                .andExpect(jsonPath("$.collateralCurrency").value("USD"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void adminImportsMarketCreditCurveAsTraceableDraft() throws Exception {
        AuthClient client = selectGroup(login(), "ADMIN");
        UUID counterpartyId = createCounterparty(client, "Market Curve Broker", "MCB-001");
        LocalDate valuationDate = LocalDate.parse("2026-08-02");
        when(marketDataCurveGateway.getCreditCurve("A", "USD", valuationDate, 0.40))
                .thenReturn(marketCreditCurve(valuationDate));

        mockMvc.perform(post("/api/xva/credit-curves/imports/market-data")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "counterpartyId": "%s",
                                  "valuationDate": "2026-08-02",
                                  "recoveryRate": 0.40,
                                  "name": null,
                                  "allowStale": false
                                }
                                """.formatted(counterpartyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counterpartyId").value(counterpartyId.toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.source").value("MARKET_DATA"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.sourceReference").value("FRED_ICE_BOFA_RATING_OAS"))
                .andExpect(jsonPath("$.sourceSeriesId").value("BAMLC0A3CA"))
                .andExpect(jsonPath("$.sourceRatingBucket").value("A"))
                .andExpect(jsonPath("$.sourceSpread").value(0.0067))
                .andExpect(jsonPath("$.sourceHazardRate").value(0.0111666667))
                .andExpect(jsonPath("$.sourceObservationDate").value("2026-07-30"))
                .andExpect(jsonPath("$.marketProxy").value(true))
                .andExpect(jsonPath("$.points.length()").value(7));
    }

    private MarketCreditCurve marketCreditCurve(LocalDate valuationDate) {
        return new MarketCreditCurve(
                "USD A Rating OAS Credit Proxy", "CUMULATIVE_DEFAULT_PROBABILITY", "A", "A", "USD",
                valuationDate, 0.40, 0.0067, "DECIMAL", 0.0111666667,
                LocalDate.parse("2026-07-30"), Instant.parse("2026-08-02T04:27:07Z"),
                "FRED_ICE_BOFA_RATING_OAS", "BAMLC0A3CA", "RATING_OAS_FLAT_HAZARD_PROXY", true, false,
                List.of(6, 12, 24, 36, 60, 84, 120).stream()
                        .map(months -> new MarketCreditCurve.Point(valuationDate.plusMonths(months), months / 1200.0))
                        .toList());
    }

    private AuthClient login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "xva-admin",
                                  "password": "xva-admin-password"
                                }
                                """))
                .andExpect(status().isOk())
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
                                {
                                  "group": "%s"
                                }
                                """.formatted(group)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthClient(
                client.cookie(),
                body.get("csrfToken").asText(),
                client.userId()
        );
    }

    private UUID createCounterparty(AuthClient client, String name, String externalId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/xva/counterparties")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "externalId": "%s",
                                  "creditRating": "A"
                                }
                                """.formatted(name, externalId)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    private record AuthClient(Cookie cookie, String csrfToken, UUID userId) {
    }
}
