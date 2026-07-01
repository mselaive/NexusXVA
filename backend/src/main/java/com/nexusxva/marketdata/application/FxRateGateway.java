package com.nexusxva.marketdata.application;

import com.nexusxva.marketdata.domain.FxRate;
import java.util.Optional;

public interface FxRateGateway {
    Optional<FxRate> findRate(String sourceCurrency, String targetCurrency);
}
