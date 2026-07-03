package com.nexusxva.audit.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.AbstractPostgresIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "nexusxva.auth.enabled=true",
        "nexusxva.auth.bootstrap-admin.username=audit-admin",
        "nexusxva.auth.bootstrap-admin.password=audit-password",
        "nexusxva.auth.bootstrap-admin.display-name=Audit Admin"
})
@AutoConfigureMockMvc
class AuditTrailIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginFailureCreatesSanitizedAuditEvent() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "audit-admin",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized());

        AuthClient admin = selectGroup(login(), "ADMIN");
        mockMvc.perform(get("/api/admin/audit-events")
                        .queryParam("eventType", "AUTH_LOGIN_FAILURE")
                        .cookie(admin.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].eventType").value("AUTH_LOGIN_FAILURE"))
                .andExpect(jsonPath("$.items[0].outcome").value("FAILURE"))
                .andExpect(jsonPath("$.items[0].metadata.username").value("audit-admin"))
                .andExpect(jsonPath("$.items[0].metadata.password").doesNotExist());
    }

    @Test
    void deniedRequestCreatesAuditEventAndAuditApiRequiresAdmin() throws Exception {
        AuthClient admin = selectGroup(login(), "ADMIN");

        mockMvc.perform(get("/api/front-office/desk")
                        .cookie(admin.cookie()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/audit-events")
                        .queryParam("eventType", "ACCESS_DENIED")
                        .queryParam("outcome", "DENIED")
                        .cookie(admin.cookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].eventType").value("ACCESS_DENIED"))
                .andExpect(jsonPath("$.items[0].outcome").value("DENIED"))
                .andExpect(jsonPath("$.items[0].activeGroup").value("ADMIN"))
                .andExpect(jsonPath("$.items[0].correlationId").isNotEmpty());

        AuthClient fo = selectGroup(login(), "FO");
        mockMvc.perform(get("/api/admin/audit-events")
                        .cookie(fo.cookie()))
                .andExpect(status().isForbidden());
    }

    private AuthClient login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "audit-admin",
                                  "password": "audit-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new AuthClient(
                result.getResponse().getCookie("NEXUSXVA_SESSION"),
                body.get("csrfToken").asText()
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
        return new AuthClient(client.cookie(), body.get("csrfToken").asText());
    }

    private record AuthClient(Cookie cookie, String csrfToken) {
    }
}
