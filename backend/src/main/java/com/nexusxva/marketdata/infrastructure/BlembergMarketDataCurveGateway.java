package com.nexusxva.marketdata.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nexusxva.marketdata.application.MarketDataCurveGateway;
import com.nexusxva.marketdata.domain.MarketDiscountCurve;
import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.shared.error.ServiceUnavailableException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Repository
class BlembergMarketDataCurveGateway implements MarketDataCurveGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlembergMarketDataCurveGateway.class);
    private final RestClient client;

    BlembergMarketDataCurveGateway(@Qualifier("blembergRestClient") RestClient client) {
        this.client = client;
    }

    @Override
    public MarketDiscountCurve getDiscountCurve(String currency, LocalDate valuationDate) {
        String normalizedCurrency = currency == null ? "" : currency.trim().toUpperCase();
        try {
            Response response = client.get()
                    .uri(builder -> builder.path("/api/market-data/curves/discount")
                            .queryParam("currency", normalizedCurrency)
                            .queryParam("valuationDate", valuationDate)
                            .build())
                    .retrieve()
                    .body(Response.class);
            if (response == null) throw new ServiceUnavailableException("Market data service returned an empty curve");
            MarketDiscountCurve curve = response.toDomain();
            LOGGER.info("Blemberg discount curve loaded currency={} valuationDate={} asOf={} source={} method={} stale={} points={}",
                    curve.currency(), curve.valuationDate(), curve.asOf(), curve.source(), curve.method(), curve.stale(), curve.points().size());
            return curve;
        } catch (HttpClientErrorException.NotFound exception) {
            LOGGER.warn("Blemberg discount curve missing currency={} valuationDate={} status=404", normalizedCurrency, valuationDate);
            throw new ResourceNotFoundException("Market discount curve not found");
        } catch (HttpClientErrorException exception) {
            LOGGER.warn("Blemberg discount curve rejected currency={} valuationDate={} status={}", normalizedCurrency, valuationDate, exception.getStatusCode().value());
            throw new ServiceUnavailableException("Market data service unavailable");
        } catch (RestClientException | IllegalArgumentException exception) {
            LOGGER.warn("Blemberg discount curve failed currency={} valuationDate={} reason={}", normalizedCurrency, valuationDate, exception.getClass().getSimpleName());
            throw new ServiceUnavailableException("Market data service unavailable");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Response(String name, String currency, LocalDate valuationDate, Instant asOf, String source,
                            String method, Boolean stale, List<PointResponse> points) {
        MarketDiscountCurve toDomain() {
            return new MarketDiscountCurve(name, currency, valuationDate, asOf, source, method, Boolean.TRUE.equals(stale),
                    points == null ? List.of() : points.stream().map(PointResponse::toDomain).toList());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PointResponse(LocalDate date, Double discountFactor) {
        MarketDiscountCurve.Point toDomain() {
            if (date == null || discountFactor == null) throw new IllegalArgumentException("Market discount curve point is incomplete");
            return new MarketDiscountCurve.Point(date, discountFactor);
        }
    }
}
