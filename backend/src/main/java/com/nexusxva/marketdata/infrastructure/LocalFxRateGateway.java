package com.nexusxva.marketdata.infrastructure;

import com.nexusxva.marketdata.application.FxRateGateway;
import com.nexusxva.marketdata.domain.FxRate;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class LocalFxRateGateway implements FxRateGateway {

    private static final Map<String, Double> USD_PER_CURRENCY = Map.of(
            "USD", 1.0,
            "EUR", 1.09,
            "GBP", 1.27,
            "CAD", 0.73,
            "MXN", 0.058,
            "JPY", 0.0064
    );

    @Override
    public Optional<FxRate> findRate(String sourceCurrency, String targetCurrency) {
        String source = normalize(sourceCurrency);
        String target = normalize(targetCurrency);
        if (source == null || target == null) {
            return Optional.empty();
        }
        Double sourceUsd = USD_PER_CURRENCY.get(source);
        Double targetUsd = USD_PER_CURRENCY.get(target);
        if (sourceUsd == null || targetUsd == null) {
            return Optional.empty();
        }
        return Optional.of(new FxRate(
                source,
                target,
                sourceUsd / targetUsd,
                Instant.now(),
                "LOCAL_FX",
                false
        ));
    }

    private static String normalize(String currency) {
        if (currency == null || currency.isBlank()) {
            return null;
        }
        return currency.trim().toUpperCase(Locale.ROOT);
    }
}
