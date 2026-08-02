package com.nexusxva.marketdata.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nexusxva.marketdata.application.MarketDataCurveGateway;
import com.nexusxva.marketdata.domain.MarketDiscountCurve;
import com.nexusxva.marketdata.domain.MarketCreditCurve;
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

    @Override
    public MarketCreditCurve getCreditCurve(String creditRating, String currency, LocalDate valuationDate, double recoveryRate) {
        String normalizedRating = creditRating == null ? "" : creditRating.trim().toUpperCase();
        String normalizedCurrency = currency == null ? "" : currency.trim().toUpperCase();
        try {
            CreditResponse response = client.get()
                    .uri(builder -> builder.path("/api/market-data/curves/credit")
                            .queryParam("creditRating", normalizedRating)
                            .queryParam("currency", normalizedCurrency)
                            .queryParam("valuationDate", valuationDate)
                            .queryParam("recoveryRate", recoveryRate)
                            .build())
                    .retrieve()
                    .body(CreditResponse.class);
            if (response == null) throw new ServiceUnavailableException("Market data service returned an empty curve");
            MarketCreditCurve curve = response.toDomain();
            LOGGER.info("Blemberg credit curve loaded rating={} bucket={} currency={} valuationDate={} observationDate={} sourceSeries={} stale={} points={}",
                    curve.creditRating(), curve.ratingBucket(), curve.currency(), curve.valuationDate(), curve.observationDate(),
                    curve.sourceSeriesId(), curve.stale(), curve.points().size());
            return curve;
        } catch (HttpClientErrorException.NotFound exception) {
            LOGGER.warn("Blemberg credit curve missing rating={} currency={} valuationDate={} status=404",
                    normalizedRating, normalizedCurrency, valuationDate);
            throw new ResourceNotFoundException("Market credit curve not found");
        } catch (HttpClientErrorException exception) {
            LOGGER.warn("Blemberg credit curve rejected rating={} currency={} valuationDate={} status={}",
                    normalizedRating, normalizedCurrency, valuationDate, exception.getStatusCode().value());
            throw new ServiceUnavailableException("Market data service unavailable");
        } catch (RestClientException | IllegalArgumentException exception) {
            LOGGER.warn("Blemberg credit curve failed rating={} currency={} valuationDate={} reason={}",
                    normalizedRating, normalizedCurrency, valuationDate, exception.getClass().getSimpleName());
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CreditResponse(
            String name, String curveType, String creditRating, String ratingBucket, String currency,
            LocalDate valuationDate, Double recoveryRate, Double spread, String spreadUnit, Double hazardRate,
            LocalDate observationDate, Instant asOf, String source, String sourceSeriesId, String method,
            Boolean marketProxy, Boolean stale, List<CreditPointResponse> points
    ) {
        MarketCreditCurve toDomain() {
            if (recoveryRate == null || spread == null || hazardRate == null) {
                throw new IllegalArgumentException("Market credit curve economics are incomplete");
            }
            return new MarketCreditCurve(name, curveType, creditRating, ratingBucket, currency, valuationDate,
                    recoveryRate, spread, spreadUnit, hazardRate, observationDate, asOf, source, sourceSeriesId,
                    method, Boolean.TRUE.equals(marketProxy), Boolean.TRUE.equals(stale),
                    points == null ? List.of() : points.stream().map(CreditPointResponse::toDomain).toList());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CreditPointResponse(LocalDate date, Double cumulativeDefaultProbability) {
        MarketCreditCurve.Point toDomain() {
            if (date == null || cumulativeDefaultProbability == null) {
                throw new IllegalArgumentException("Market credit curve point is incomplete");
            }
            return new MarketCreditCurve.Point(date, cumulativeDefaultProbability);
        }
    }
}
