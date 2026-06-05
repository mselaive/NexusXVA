package com.nexusxva.marketdata.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nexusxva.marketdata.application.MarketDataPricingInputGateway;
import com.nexusxva.marketdata.domain.MarketDataPricingInput;
import com.nexusxva.shared.error.ServiceUnavailableException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

@Repository
@ConditionalOnProperty(prefix = "nexusxva.market-data", name = "provider", havingValue = "blemberg", matchIfMissing = true)
class BlembergMarketDataPricingInputGateway implements MarketDataPricingInputGateway {

    private final RestClient blembergRestClient;

    BlembergMarketDataPricingInputGateway(RestClient blembergRestClient) {
        this.blembergRestClient = blembergRestClient;
    }

    @Override
    public Optional<MarketDataPricingInput> findEuropeanOptionPricingInput(String symbol, LocalDate maturityDate) {
        try {
            BlembergPricingInputResponse response = blembergRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/market-data/pricing-inputs/european-option")
                            .queryParam("symbol", symbol)
                            .queryParam("maturityDate", maturityDate)
                            .build())
                    .retrieve()
                    .body(BlembergPricingInputResponse.class);

            if (response == null) {
                return Optional.empty();
            }

            return Optional.of(response.toDomain(symbol));
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (HttpClientErrorException.BadRequest exception) {
            return Optional.empty();
        } catch (ResourceAccessException exception) {
            throw new ServiceUnavailableException("Market data service unavailable");
        } catch (IllegalArgumentException exception) {
            throw new ServiceUnavailableException("Market data service unavailable");
        } catch (RestClientException exception) {
            throw new ServiceUnavailableException("Market data service unavailable");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BlembergPricingInputResponse(
            String symbol,
            Double spot,
            Double volatility,
            String volatilityMethod,
            Double riskFreeRate,
            String rateMethod,
            Double dividendYield,
            String dividendYieldMethod,
            String currency,
            Instant asOf,
            String source,
            Boolean stale
    ) {

        MarketDataPricingInput toDomain(String fallbackSymbol) {
            String resolvedSymbol = symbol == null || symbol.isBlank() ? fallbackSymbol : symbol;
            return new MarketDataPricingInput(
                    resolvedSymbol,
                    required(spot),
                    required(volatility),
                    required(riskFreeRate),
                    optional(dividendYield),
                    currency,
                    asOf,
                    source == null || source.isBlank() ? "BLEMBERG" : source,
                    Boolean.TRUE.equals(stale)
            );
        }

        private double required(Double value) {
            if (value == null) {
                throw new ServiceUnavailableException("Market data service returned incomplete pricing inputs");
            }
            return value;
        }

        private double optional(Double value) {
            return value == null ? 0.0 : value;
        }
    }
}
