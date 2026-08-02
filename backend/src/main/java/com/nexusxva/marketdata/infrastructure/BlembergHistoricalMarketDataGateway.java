package com.nexusxva.marketdata.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nexusxva.marketdata.application.HistoricalMarketDataGateway;
import com.nexusxva.marketdata.domain.HistoricalPriceSeries;
import com.nexusxva.shared.error.ServiceUnavailableException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Repository
class BlembergHistoricalMarketDataGateway implements HistoricalMarketDataGateway {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlembergHistoricalMarketDataGateway.class);
    private final RestClient client;

    BlembergHistoricalMarketDataGateway(@Qualifier("blembergHistoricalRestClient") RestClient client) {
        this.client = client;
    }

    @Override
    public List<HistoricalPriceSeries> dailyCloses(Collection<String> symbols, int observations) {
        List<String> normalized = symbols == null ? List.of() : new LinkedHashSet<>(symbols.stream()
                .filter(java.util.Objects::nonNull).map(String::trim).map(String::toUpperCase)
                .filter(symbol -> !symbol.isBlank()).toList()).stream().toList();
        if (normalized.isEmpty()) return List.of();
        try {
            BatchResponse response = client.post().uri("/api/market-data/daily-bars/batch")
                    .body(new BatchRequest(normalized, observations)).retrieve().body(BatchResponse.class);
            if (response == null || response.series() == null) {
                throw new IllegalArgumentException("Historical response is empty");
            }
            if (response.missingSymbols() != null && !response.missingSymbols().isEmpty()) {
                LOGGER.warn("Blemberg historical batch incomplete requested={} missing={}", normalized, response.missingSymbols());
            }
            Instant asOf = response.asOf();
            String source = response.source();
            return response.series().stream().map(series -> series.toDomain(asOf, source)).toList();
        } catch (RestClientResponseException exception) {
            LOGGER.warn("Blemberg historical batch failed symbols={} observations={} status={} body={}",
                    normalized, observations, exception.getStatusCode().value(), sanitizedBody(exception.getResponseBodyAsString()));
            throw new ServiceUnavailableException("Historical market data unavailable");
        } catch (RestClientException | IllegalArgumentException exception) {
            LOGGER.warn("Blemberg historical batch failed symbols={} observations={} reason={}",
                    normalized, observations, exception.getClass().getSimpleName());
            throw new ServiceUnavailableException("Historical market data unavailable");
        }
    }

    private String sanitizedBody(String body) {
        if (body == null || body.isBlank()) return "<empty>";
        String sanitized = body.replaceAll("[\\r\\n\\t]+", " ").replaceAll("\\s{2,}", " ").trim();
        return sanitized.substring(0, Math.min(300, sanitized.length()));
    }

    private record BatchRequest(List<String> symbols, int observations) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BatchResponse(List<SeriesResponse> series, List<String> missingSymbols, Instant asOf, String source) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeriesResponse(String symbol, String currency, Boolean stale, List<BarResponse> bars) {
        HistoricalPriceSeries toDomain(Instant asOf, String source) {
            return new HistoricalPriceSeries(symbol, currency, Boolean.TRUE.equals(stale), asOf, source,
                    bars == null ? List.of() : bars.stream().map(BarResponse::toDomain).toList());
        }
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BarResponse(LocalDate date, Double close) {
        HistoricalPriceSeries.DailyClose toDomain() {
            if (date == null || close == null) throw new IllegalArgumentException("Historical bar is incomplete");
            return new HistoricalPriceSeries.DailyClose(date, close);
        }
    }
}
