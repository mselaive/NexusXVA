package com.nexusxva.marketdata.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nexusxva.marketdata.application.MarketDataInstrumentGateway;
import com.nexusxva.marketdata.domain.MarketInstrument;
import com.nexusxva.shared.error.ServiceUnavailableException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Repository
@ConditionalOnProperty(prefix = "nexusxva.market-data", name = "provider", havingValue = "blemberg", matchIfMissing = true)
class BlembergMarketDataInstrumentGateway implements MarketDataInstrumentGateway {

    private final RestClient blembergRestClient;

    BlembergMarketDataInstrumentGateway(RestClient blembergRestClient) {
        this.blembergRestClient = blembergRestClient;
    }

    @Override
    public Optional<MarketInstrument> findInstrument(String symbol) {
        try {
            BlembergInstrumentResponse response = blembergRestClient.get()
                    .uri("/api/instruments/{symbol}", symbol)
                    .retrieve()
                    .body(BlembergInstrumentResponse.class);

            if (response == null) {
                return Optional.empty();
            }

            return Optional.of(response.toDomain(symbol));
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (ResourceAccessException exception) {
            throw new ServiceUnavailableException("Market data service unavailable");
        } catch (RestClientException exception) {
            throw new ServiceUnavailableException("Market data service unavailable");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BlembergInstrumentResponse(
            String symbol,
            Boolean active,
            String name,
            String assetClass,
            String exchange,
            String provider,
            String providerSymbol,
            String currency
    ) {

        MarketInstrument toDomain(String fallbackSymbol) {
            String resolvedSymbol = symbol == null || symbol.isBlank() ? fallbackSymbol : symbol;
            return new MarketInstrument(
                    resolvedSymbol,
                    Boolean.TRUE.equals(active),
                    name,
                    assetClass,
                    currency
            );
        }
    }

}
