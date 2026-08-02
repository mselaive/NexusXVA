package com.nexusxva.marketdata.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.shared.error.ServiceUnavailableException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BlembergMarketDataCurveGatewayTest {

    private static final String BASE_URL = "https://blemberg.test";
    private static final LocalDate VALUATION_DATE = LocalDate.parse("2026-07-31");
    private static final String URI = BASE_URL
            + "/api/market-data/curves/discount?currency=USD&valuationDate=2026-07-31";
    private static final String CREDIT_URI = BASE_URL
            + "/api/market-data/curves/credit?creditRating=A+&currency=USD&valuationDate=2026-07-31&recoveryRate=0.4";

    @Test
    void mapsDiscountCurveAndLineage() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(validResponse(false), MediaType.APPLICATION_JSON));

        var curve = fixture.gateway.getDiscountCurve("usd", VALUATION_DATE);

        assertThat(curve.currency()).isEqualTo("USD");
        assertThat(curve.valuationDate()).isEqualTo(VALUATION_DATE);
        assertThat(curve.source()).isEqualTo("RISK_FREE_CACHE");
        assertThat(curve.method()).isEqualTo("ZERO_RATE_LINEAR_INTERPOLATION");
        assertThat(curve.stale()).isFalse();
        assertThat(curve.points()).hasSize(3);
        assertThat(curve.points().getFirst().discountFactor()).isEqualTo(0.9962);
        fixture.server.verify();
    }

    @Test
    void preservesStaleFlagForApplicationPolicy() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(URI))
                .andRespond(withSuccess(validResponse(true), MediaType.APPLICATION_JSON));

        assertThat(fixture.gateway.getDiscountCurve("USD", VALUATION_DATE).stale()).isTrue();
        fixture.server.verify();
    }

    @Test
    void rejectsIncreasingDiscountFactors() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(URI))
                .andRespond(withSuccess(validResponse(false).replace("0.9771", "0.9999"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.gateway.getDiscountCurve("USD", VALUATION_DATE))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Market data service unavailable");
        fixture.server.verify();
    }

    @Test
    void mapsNotFoundToCleanMissingCurveError() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(URI))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> fixture.gateway.getDiscountCurve("USD", VALUATION_DATE))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Market discount curve not found");
        fixture.server.verify();
    }

    @Test
    void mapsProviderFailureToServiceUnavailable() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(URI)).andRespond(withServerError());

        assertThatThrownBy(() -> fixture.gateway.getDiscountCurve("USD", VALUATION_DATE))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Market data service unavailable");
        fixture.server.verify();
    }

    @Test
    void mapsObservedBlembergCreditCurveAndLineage() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(CREDIT_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(validCreditResponse(false), MediaType.APPLICATION_JSON));

        var curve = fixture.gateway.getCreditCurve("a+", "usd", VALUATION_DATE, 0.40);

        assertThat(curve.creditRating()).isEqualTo("A+");
        assertThat(curve.ratingBucket()).isEqualTo("A");
        assertThat(curve.sourceSeriesId()).isEqualTo("BAMLC0A3CA");
        assertThat(curve.spread()).isEqualTo(0.0067);
        assertThat(curve.hazardRate()).isEqualTo(0.0111666667);
        assertThat(curve.marketProxy()).isTrue();
        assertThat(curve.points()).hasSize(7);
        assertThat(curve.points().getLast().cumulativeDefaultProbability()).isEqualTo(0.1057397581);
        fixture.server.verify();
    }

    @Test
    void rejectsMalformedCreditCurve() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(CREDIT_URI))
                .andRespond(withSuccess(validCreditResponse(false).replace("0.0221157082", "0.001"), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> fixture.gateway.getCreditCurve("A+", "USD", VALUATION_DATE, 0.40))
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage("Market data service unavailable");
        fixture.server.verify();
    }

    @Test
    void mapsMissingCreditCurveToCleanError() {
        Fixture fixture = fixture();
        fixture.server.expect(once(), requestTo(CREDIT_URI)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> fixture.gateway.getCreditCurve("A+", "USD", VALUATION_DATE, 0.40))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Market credit curve not found");
        fixture.server.verify();
    }

    private String validResponse(boolean stale) {
        return """
                {
                  "curveType": "DISCOUNT_FACTOR",
                  "currency": "USD",
                  "valuationDate": "2026-07-31",
                  "name": "USD Risk-Free Discount Curve",
                  "asOf": "2026-07-31T21:00:00Z",
                  "source": "RISK_FREE_CACHE",
                  "method": "ZERO_RATE_LINEAR_INTERPOLATION",
                  "stale": %s,
                  "points": [
                    { "date": "2026-08-31", "discountFactor": 0.9962 },
                    { "date": "2027-01-31", "discountFactor": 0.9771 },
                    { "date": "2027-07-31", "discountFactor": 0.9554 }
                  ]
                }
                """.formatted(stale);
    }

    private String validCreditResponse(boolean stale) {
        return """
                {
                  "name":"USD A Rating OAS Credit Proxy",
                  "curveType":"CUMULATIVE_DEFAULT_PROBABILITY",
                  "creditRating":"A+",
                  "ratingBucket":"A",
                  "currency":"USD",
                  "valuationDate":"2026-07-31",
                  "recoveryRate":0.40,
                  "spread":0.00670000,
                  "spreadUnit":"DECIMAL",
                  "hazardRate":0.0111666667,
                  "observationDate":"2026-07-30",
                  "asOf":"2026-08-02T04:27:07.027254Z",
                  "source":"FRED_ICE_BOFA_RATING_OAS",
                  "sourceSeriesId":"BAMLC0A3CA",
                  "method":"RATING_OAS_FLAT_HAZARD_PROXY",
                  "marketProxy":true,
                  "stale":%s,
                  "points":[
                    {"date":"2027-01-31","cumulativeDefaultProbability":0.0056134094},
                    {"date":"2027-07-31","cumulativeDefaultProbability":0.0111045509},
                    {"date":"2028-07-31","cumulativeDefaultProbability":0.0221157082},
                    {"date":"2029-07-31","cumulativeDefaultProbability":0.0329746741},
                    {"date":"2031-07-31","cumulativeDefaultProbability":0.0543321931},
                    {"date":"2033-07-31","cumulativeDefaultProbability":0.0752463064},
                    {"date":"2036-07-31","cumulativeDefaultProbability":0.1057397581}
                  ]
                }
                """.formatted(stale);
    }

    private Fixture fixture() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        return new Fixture(new BlembergMarketDataCurveGateway(builder.build()), server);
    }

    private record Fixture(BlembergMarketDataCurveGateway gateway, MockRestServiceServer server) {}
}
