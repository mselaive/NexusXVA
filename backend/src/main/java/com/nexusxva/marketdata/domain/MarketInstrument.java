package com.nexusxva.marketdata.domain;

import java.util.Locale;

public record MarketInstrument(
        String symbol,
        boolean active,
        String name,
        String assetClass,
        String currency
) {

    public MarketInstrument {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("instrument symbol is required");
        }
        symbol = symbol.trim().toUpperCase(Locale.ROOT);
    }
}
