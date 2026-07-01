package com.nexusxva.marketdata.application;

import com.nexusxva.marketdata.domain.FxRate;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class FxRateService {

    private final FxRateGateway gateway;

    public FxRateService(FxRateGateway gateway) {
        this.gateway = gateway;
    }

    public FxRate rate(String sourceCurrency, String targetCurrency) {
        String source = normalize(sourceCurrency, "sourceCurrency");
        String target = normalize(targetCurrency, "targetCurrency");
        return gateway.findRate(source, target)
                .orElseThrow(() -> new IllegalArgumentException("FX rate unavailable for currency pair"));
    }

    public double convert(double amount, String sourceCurrency, String targetCurrency) {
        FxRate fxRate = rate(sourceCurrency, targetCurrency);
        return amount * fxRate.rate();
    }

    private static String normalize(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(field + " must be a 3-letter currency code");
        }
        return normalized;
    }
}
