package com.nexusxva.marketdata.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nexusxva.marketdata.domain.MarketDataPricingInput;
import com.nexusxva.shared.error.ServiceUnavailableException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.util.Optional;

class BlembergMarketDataPricingInputGatewayTest {

    private static final String BASE_URL = "https://blemberg.test";
    private static final LocalDate MATURITY_DATE = LocalDate.parse("2027-06-01");
    private static final String PRICING_INPUT_URI = BASE_URL
            + "/api/market-data/pricing-inputs/european-option?symbol=AAPL&maturityDate=2027-06-01";

    @Test
    void mapsPricingInputResponse() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(PRICING_INPUT_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "symbol": "AAPL",
                          "spot": 190.0,
                          "volatility": 0.22,
                          "volatilityMethod": "HISTORICAL_REALIZED_60D",
                          "riskFreeRate": 0.045,
                          "rateMethod": "LINEAR_INTERPOLATION",
                          "dividendYield": 0.005,
                          "dividendYieldMethod": "PROVIDER",
                          "currency": "USD",
                          "asOf": "2026-06-02T12:00:00Z",
                          "source": "BLEMBERG",
                          "stale": true
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<MarketDataPricingInput> input = fixture.gateway.findEuropeanOptionPricingInput("AAPL", MATURITY_DATE);

        assertThat(input).isPresent();
        assertThat(input.get().symbol()).isEqualTo("AAPL");
        assertThat(input.get().spot()).isEqualTo(190.0);
        assertThat(input.get().volatility()).isEqualTo(0.22);
        assertThat(input.get().riskFreeRate()).isEqualTo(0.045);
        assertThat(input.get().dividendYield()).isEqualTo(0.005);
        assertThat(input.get().currency()).isEqualTo("USD");
        assertThat(input.get().source()).isEqualTo("BLEMBERG");
        assertThat(input.get().stale()).isTrue();
        fixture.server.verify();
    }

    @Test
    void defaultsMissingDividendYieldToZero() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(PRICING_INPUT_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "symbol": "AAPL",
                          "spot": 190.0,
                          "volatility": 0.22,
                          "volatilityMethod": "HISTORICAL_REALIZED_60D",
                          "riskFreeRate": 0.045,
                          "rateMethod": "LINEAR_INTERPOLATION",
                          "currency": "USD",
                          "asOf": "2026-06-02T12:00:00Z",
                          "source": "BLEMBERG",
                          "stale": false
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<MarketDataPricingInput> input = fixture.gateway.findEuropeanOptionPricingInput("AAPL", MATURITY_DATE);

        assertThat(input).isPresent();
        assertThat(input.get().dividendYield()).isZero();
        fixture.server.verify();
    }

    @Test
    void mapsNotFoundToEmptyOptional() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(PRICING_INPUT_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(fixture.gateway.findEuropeanOptionPricingInput("AAPL", MATURITY_DATE)).isEmpty();
        fixture.server.verify();
    }

    @Test
    void mapsBadRequestToEmptyOptional() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(PRICING_INPUT_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThat(fixture.gateway.findEuropeanOptionPricingInput("AAPL", MATURITY_DATE)).isEmpty();
        fixture.server.verify();
    }

    @Test
    void mapsMissingRequiredPricingFieldToServiceUnavailable() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(PRICING_INPUT_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "symbol": "AAPL",
                          "volatility": 0.22,
                          "riskFreeRate": 0.045,
                          "dividendYield": 0.005,
                          "currency": "USD",
                          "asOf": "2026-06-02T12:00:00Z",
                          "source": "BLEMBERG",
                          "stale": false
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.gateway.findEuropeanOptionPricingInput("AAPL", MATURITY_DATE))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Market data service returned incomplete pricing inputs");
        fixture.server.verify();
    }

    @Test
    void mapsInvalidDividendYieldToServiceUnavailable() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(PRICING_INPUT_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "symbol": "AAPL",
                          "spot": 190.0,
                          "volatility": 0.22,
                          "riskFreeRate": 0.045,
                          "dividendYield": -0.01,
                          "currency": "USD",
                          "asOf": "2026-06-02T12:00:00Z",
                          "source": "BLEMBERG",
                          "stale": false
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.gateway.findEuropeanOptionPricingInput("AAPL", MATURITY_DATE))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Market data service unavailable");
        fixture.server.verify();
    }

    @Test
    void mapsProviderServerErrorToServiceUnavailable() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(PRICING_INPUT_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThatThrownBy(() -> fixture.gateway.findEuropeanOptionPricingInput("AAPL", MATURITY_DATE))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Market data service unavailable");
        fixture.server.verify();
    }

    @Test
    void mapsMalformedProviderResponseToServiceUnavailable() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(PRICING_INPUT_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.gateway.findEuropeanOptionPricingInput("AAPL", MATURITY_DATE))
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
        BlembergMarketDataPricingInputGateway gateway = new BlembergMarketDataPricingInputGateway(restClient);

        assertThatThrownBy(() -> gateway.findEuropeanOptionPricingInput("AAPL", MATURITY_DATE))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Market data service unavailable");
    }

    private Fixture fixture() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(new BlembergMarketDataPricingInputGateway(builder.build()), server);
    }

    private record Fixture(BlembergMarketDataPricingInputGateway gateway, MockRestServiceServer server) {
    }
}
