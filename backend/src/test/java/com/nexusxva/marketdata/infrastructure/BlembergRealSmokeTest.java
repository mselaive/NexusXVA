package com.nexusxva.marketdata.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

class BlembergRealSmokeTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SMOKE_ENV = "RUN_REAL_BLEMBERG_SMOKE";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Test
    void verifiesRealBlembergContractWhenEnabled() throws Exception {
        assumeTrue("true".equalsIgnoreCase(System.getenv(SMOKE_ENV)),
                "Set RUN_REAL_BLEMBERG_SMOKE=true to run the real Blemberg smoke test");

        String baseUrl = baseUrl();

        JsonNode health = json(get(baseUrl, "/actuator/health"));
        assertThat(health.path("status").asText()).isEqualTo("UP");

        HttpResult refresh = post(baseUrl, "/api/admin/market-data/refresh");
        assertThat(refresh.statusCode()).isEqualTo(200);
        JsonNode refreshBody = json(refresh);
        assertThat(refreshBody.path("jobSummaries").isArray()).isTrue();
        assertThat(refreshBody.path("jobSummaries")).isNotEmpty();

        assertThat(get(baseUrl, "/api/admin/market-data/refresh-runs").statusCode()).isEqualTo(200);
        assertThat(get(baseUrl, "/v3/api-docs").statusCode()).isEqualTo(501);

        JsonNode snapshots = json(get(
                baseUrl,
                "/api/market-data/snapshots?symbols=" + encode("AAPL,SPY,QQQ,MSFT")
        ));
        assertThat(snapshots.path("snapshots").isArray()).isTrue();
        assertThat(snapshots.path("missingSymbols").isArray()).isTrue();
        assertThat(snapshots.path("snapshots")).isNotEmpty();

        assertCompletePricingInputs(pricingInputs(baseUrl, "AAPL"));
        assertAmznPricingInputsIfAvailable(baseUrl);
    }

    private JsonNode pricingInputs(String baseUrl, String symbol) throws Exception {
        HttpResult result = get(
                baseUrl,
                "/api/market-data/pricing-inputs/european-option?symbol="
                        + encode(symbol)
                        + "&maturityDate=2027-06-01"
        );
        assertThat(result.statusCode()).isEqualTo(200);
        return json(result);
    }

    private void assertAmznPricingInputsIfAvailable(String baseUrl) throws Exception {
        HttpResult result = get(
                baseUrl,
                "/api/market-data/pricing-inputs/european-option?symbol=AMZN&maturityDate=2027-06-01"
        );
        if (result.statusCode() == 404) {
            JsonNode body = json(result);
            assertThat(body.path("message").asText()).containsIgnoringCase("Pricing inputs");
            System.out.println("AMZN pricing inputs unavailable, likely because refresh did not reach AMZN before rate limit.");
            return;
        }
        assertThat(result.statusCode()).isEqualTo(200);
        assertCompletePricingInputs(json(result));
    }

    private void assertCompletePricingInputs(JsonNode input) {
        assertThat(input.path("symbol").asText()).isNotBlank();
        assertThat(input.path("spot").asDouble()).isPositive();
        assertThat(input.path("volatility").asDouble()).isPositive();
        assertThat(input.path("volatilityMethod").asText()).isNotBlank();
        assertThat(Double.isFinite(input.path("riskFreeRate").asDouble())).isTrue();
        assertThat(input.path("rateMethod").asText()).isNotBlank();
        assertThat(input.has("dividendYield")).isTrue();
        assertThat(input.path("dividendYield").asDouble()).isGreaterThanOrEqualTo(0.0);
        assertThat(input.path("dividendYieldMethod").asText()).isNotBlank();
        assertThat(input.path("currency").asText()).isEqualTo("USD");
        assertThat(input.path("asOf").asText()).isNotBlank();
        assertThat(input.path("source").asText()).isNotBlank();
        assertThat(input.path("stale").isBoolean()).isTrue();
    }

    private HttpResult get(String baseUrl, String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        return send(request);
    }

    private HttpResult post(String baseUrl, String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return send(request);
    }

    private HttpResult send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new HttpResult(response.statusCode(), response.body());
    }

    private JsonNode json(HttpResult result) throws IOException {
        return OBJECT_MAPPER.readTree(result.body());
    }

    private String baseUrl() {
        String configured = System.getenv().getOrDefault("BLEMBERG_BASE_URL", "http://localhost:8081");
        if (configured.endsWith("/")) {
            return configured.substring(0, configured.length() - 1);
        }
        return configured;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private record HttpResult(int statusCode, String body) {
    }
}
