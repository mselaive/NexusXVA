package com.nexusxva.marketdata.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nexusxva.marketdata.application.MarketDataInstrumentGateway;
import com.nexusxva.marketdata.domain.MarketInstrument;
import com.nexusxva.shared.error.ServiceUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Repository
@ConditionalOnProperty(prefix = "nexusxva.market-data", name = "provider", havingValue = "blemberg", matchIfMissing = true)
class BlembergMarketDataInstrumentGateway implements MarketDataInstrumentGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlembergMarketDataInstrumentGateway.class);
    private static final String ENDPOINT = "GET /api/instruments/{symbol}";
    private static final int MAX_LOG_BODY_LENGTH = 500;

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
            logHttpError(symbol, exception);
            return Optional.empty();
        } catch (HttpStatusCodeException exception) {
            logHttpError(symbol, exception);
            throw new ServiceUnavailableException("Market data service unavailable");
        } catch (ResourceAccessException exception) {
            LOGGER.warn("Blemberg instrument lookup failed endpoint={} symbol={} reason={}",
                    ENDPOINT,
                    symbol,
                    exception.getClass().getSimpleName());
            throw new ServiceUnavailableException("Market data service unavailable");
        } catch (RestClientException exception) {
            LOGGER.warn("Blemberg instrument lookup failed endpoint={} symbol={} reason={}",
                    ENDPOINT,
                    symbol,
                    exception.getClass().getSimpleName());
            throw new ServiceUnavailableException("Market data service unavailable");
        }
    }

    private void logHttpError(String symbol, HttpStatusCodeException exception) {
        LOGGER.warn(
                "Blemberg instrument lookup returned error endpoint={} symbol={} status={} body={}",
                ENDPOINT,
                symbol,
                exception.getStatusCode().value(),
                sanitizedBody(exception.getResponseBodyAsString())
        );
    }

    private String sanitizedBody(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String oneLine = body.replaceAll("\\s+", " ").trim();
        if (oneLine.length() <= MAX_LOG_BODY_LENGTH) {
            return oneLine;
        }
        return oneLine.substring(0, MAX_LOG_BODY_LENGTH) + "...";
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
