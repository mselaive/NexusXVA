package com.nexusxva.marketdata.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nexusxva.shared.error.ServiceUnavailableException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BlembergHistoricalMarketDataGatewayTest {
    private static final String BASE_URL = "https://blemberg.test";

    @Test
    void mapsBatchHistoryAndSendsNormalizedSymbols() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(BASE_URL + "/api/market-data/daily-bars/batch"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"symbols":["AAPL","MSFT"],"observations":260}
                        """))
                .andRespond(withSuccess("""
                        {
                          "series": [
                            {
                              "symbol": "AAPL",
                              "currency": "USD",
                              "stale": false,
                              "bars": [
                                {"date": "2026-07-30", "close": 208.10},
                                {"date": "2026-07-31", "close": 210.15}
                              ]
                            }
                          ],
                          "missingSymbols": ["MSFT"],
                          "asOf": "2026-08-02T00:00:00Z",
                          "source": "TWELVE_DATA"
                        }
                        """, MediaType.APPLICATION_JSON));

        var result = fixture.gateway.dailyCloses(List.of(" aapl ", "MSFT", "AAPL"), 260);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().symbol()).isEqualTo("AAPL");
        assertThat(result.getFirst().currency()).isEqualTo("USD");
        assertThat(result.getFirst().asOf()).isEqualTo(Instant.parse("2026-08-02T00:00:00Z"));
        assertThat(result.getFirst().closes()).extracting(close -> close.date())
                .containsExactly(LocalDate.parse("2026-07-30"), LocalDate.parse("2026-07-31"));
        fixture.server.verify();
    }

    @Test
    void rejectsIncompleteHistoricalMetadata() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(BASE_URL + "/api/market-data/daily-bars/batch"))
                .andRespond(withSuccess("""
                        {"series":[{"symbol":"AAPL","currency":"USD","stale":false,"bars":[]}],
                         "missingSymbols":[],"source":"TWELVE_DATA"}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.gateway.dailyCloses(List.of("AAPL"), 260))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Historical market data unavailable");
        fixture.server.verify();
    }

    @Test
    void acceptsSuccessfulResponseWhenEveryRequestedSeriesIsMissing() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(BASE_URL + "/api/market-data/daily-bars/batch"))
                .andRespond(withSuccess("""
                        {"series":[],"missingSymbols":["AAPL"],"asOf":null,"source":"TWELVE_DATA"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(fixture.gateway.dailyCloses(List.of("AAPL"), 260)).isEmpty();
        fixture.server.verify();
    }

    @Test
    void mapsProviderFailureToCleanServiceError() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(BASE_URL + "/api/market-data/daily-bars/batch"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> fixture.gateway.dailyCloses(List.of("AAPL"), 260))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Historical market data unavailable");
        fixture.server.verify();
    }

    private Fixture fixture() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(new BlembergHistoricalMarketDataGateway(builder.build()), server);
    }

    private record Fixture(BlembergHistoricalMarketDataGateway gateway, MockRestServiceServer server) {}
}
