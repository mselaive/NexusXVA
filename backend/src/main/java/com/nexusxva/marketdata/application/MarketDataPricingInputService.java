package com.nexusxva.marketdata.application;

import com.nexusxva.marketdata.domain.MarketDataPricingInput;

import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class MarketDataPricingInputService {

    private final MarketDataPricingInputGateway pricingInputGateway;

    public MarketDataPricingInputService(MarketDataPricingInputGateway pricingInputGateway) {
        this.pricingInputGateway = pricingInputGateway;
    }

    public MarketDataPricingInput europeanOptionPricingInput(String symbol, LocalDate maturityDate) {
        return pricingInputGateway.findEuropeanOptionPricingInput(symbol, maturityDate)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Market data pricing inputs unavailable for underlyingSymbol"
                ));
    }
}
