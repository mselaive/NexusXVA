package com.nexusxva.marketdata.application;

import com.nexusxva.marketdata.domain.MarketDataPricingInput;

import java.time.LocalDate;
import java.util.Optional;

public interface MarketDataPricingInputGateway {

    Optional<MarketDataPricingInput> findEuropeanOptionPricingInput(String symbol, LocalDate maturityDate);
}
