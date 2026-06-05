package com.nexusxva.marketdata.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nexusxva.marketdata.domain.MarketInstrument;
import com.nexusxva.shared.error.ServiceUnavailableException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;

import java.net.SocketTimeoutException;
import java.util.Optional;

class BlembergMarketDataInstrumentGatewayTest {

    private static final String BASE_URL = "https://blemberg.test";

    @Test
    void mapsActiveInstrumentResponse() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(BASE_URL + "/api/instruments/AAPL"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "symbol": "AAPL",
                          "active": true,
                          "name": "Apple Inc.",
                          "assetClass": "EQUITY",
                          "exchange": "NASDAQ",
                          "provider": "TWELVE_DATA",
                          "providerSymbol": "AAPL",
                          "currency": "USD"
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<MarketInstrument> instrument = fixture.gateway.findInstrument("AAPL");

        assertThat(instrument).isPresent();
        assertThat(instrument.get().symbol()).isEqualTo("AAPL");
        assertThat(instrument.get().active()).isTrue();
        assertThat(instrument.get().name()).isEqualTo("Apple Inc.");
        assertThat(instrument.get().assetClass()).isEqualTo("EQUITY");
        assertThat(instrument.get().currency()).isEqualTo("USD");
        fixture.server.verify();
    }

    @Test
    void mapsUnknownInstrumentToEmptyOptional() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(BASE_URL + "/api/instruments/FAKE"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(fixture.gateway.findInstrument("FAKE")).isEmpty();
        fixture.server.verify();
    }

    @Test
    void keepsInactiveInstrumentAsDomainResponse() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(BASE_URL + "/api/instruments/AAPL"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "symbol": "AAPL",
                          "active": false,
                          "name": "Apple Inc.",
                          "assetClass": "EQUITY",
                          "exchange": "NASDAQ",
                          "provider": "TWELVE_DATA",
                          "providerSymbol": "AAPL",
                          "currency": "USD"
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<MarketInstrument> instrument = fixture.gateway.findInstrument("AAPL");

        assertThat(instrument).isPresent();
        assertThat(instrument.get().active()).isFalse();
        fixture.server.verify();
    }

    @Test
    void mapsProviderServerErrorToServiceUnavailable() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(BASE_URL + "/api/instruments/AAPL"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> fixture.gateway.findInstrument("AAPL"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Market data service unavailable");
        fixture.server.verify();
    }

    @Test
    void mapsMalformedProviderResponseToServiceUnavailable() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(BASE_URL + "/api/instruments/AAPL"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.gateway.findInstrument("AAPL"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Market data service unavailable");
        fixture.server.verify();
    }

    @Test
    void mapsTimeoutToServiceUnavailable() {
        RestClient restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory((uri, httpMethod) -> {
                    throw new SocketTimeoutException("timeout");
                })
                .build();
        BlembergMarketDataInstrumentGateway gateway = new BlembergMarketDataInstrumentGateway(restClient);

        assertThatThrownBy(() -> gateway.findInstrument("AAPL"))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Market data service unavailable");
    }

    private Fixture fixture() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(new BlembergMarketDataInstrumentGateway(builder.build()), server);
    }

    private record Fixture(BlembergMarketDataInstrumentGateway gateway, MockRestServiceServer server) {
    }
}
