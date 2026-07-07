package com.nexusxva.backoffice.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.AbstractPostgresIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "nexusxva.auth.enabled=true",
        "nexusxva.market-data.provider=local",
        "nexusxva.market-data.validation.enabled=true"
})
@AutoConfigureMockMvc
class BackOfficeReportsControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void boCanReadOperationsReportWithPnlAndCorrectionFields() throws Exception {
        AuthClient bo = selectGroup(login("bo.ops", "bo12345"), "BO");

        mockMvc.perform(get("/api/back-office/reports/operations").cookie(bo.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessDate").isNotEmpty())
                .andExpect(jsonPath("$.oldestPendingTradeBookingSubmittedAt").hasJsonPath())
                .andExpect(jsonPath("$.oldestPendingLifecycleSubmittedAt").hasJsonPath())
                .andExpect(jsonPath("$.failedPnlPortfolios").isNumber())
                .andExpect(jsonPath("$.portfoliosWithCorrectedLatestClose").isNumber())
                .andExpect(jsonPath("$.portfoliosWithNoCloseEver").isNumber())
                .andExpect(jsonPath("$.eodPortfolios").isArray())
                .andExpect(jsonPath("$.eodPortfolios[0].pnlStatus").exists())
                .andExpect(jsonPath("$.eodPortfolios[0].latestCloseCorrected").isBoolean())
                .andExpect(jsonPath("$.eodPortfolios[0].noCloseEver").isBoolean());
    }

    @Test
    void frontOfficeCannotReadOperationsReport() throws Exception {
        AuthClient fo = selectGroup(login("fo.trader", "fo12345"), "FO");

        mockMvc.perform(get("/api/back-office/reports/operations").cookie(fo.cookie()))
                .andExpect(status().isForbidden());
    }

    private AuthClient login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthClient(
                UUID.fromString(json.get("user").get("id").asText()),
                json.get("csrfToken").asText(),
                result.getResponse().getCookie("NEXUSXVA_SESSION")
        );
    }

    private AuthClient selectGroup(AuthClient client, String groupCode) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/active-group")
                        .cookie(client.cookie())
                        .header("X-CSRF-Token", client.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "group": "%s"
                                }
                                """.formatted(groupCode)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthClient(
                UUID.fromString(json.get("user").get("id").asText()),
                json.get("csrfToken").asText(),
                client.cookie()
        );
    }

    private record AuthClient(UUID userId, String csrfToken, Cookie cookie) {
    }
}
