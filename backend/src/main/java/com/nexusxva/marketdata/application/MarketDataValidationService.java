package com.nexusxva.marketdata.application;

import org.springframework.stereotype.Service;

@Service
public class MarketDataValidationService {

    private final MarketDataValidationProperties properties;
    private final MarketDataInstrumentGateway instrumentGateway;

    public MarketDataValidationService(
            MarketDataValidationProperties properties,
            MarketDataInstrumentGateway instrumentGateway
    ) {
        this.properties = properties;
        this.instrumentGateway = instrumentGateway;
    }

    public void validateUnderlyingSymbol(String symbol) {
        if (!properties.isEnabled()) {
            return;
        }

        instrumentGateway.findInstrument(symbol)
                .filter(instrument -> instrument.active())
                .orElseThrow(() -> new IllegalArgumentException("Unknown underlyingSymbol"));
    }
}
