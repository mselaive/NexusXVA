package com.nexusxva.marketdata.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record HistoricalPriceSeries(
        String symbol,
        String currency,
        boolean stale,
        Instant asOf,
        String source,
        List<DailyClose> closes
) {
    public HistoricalPriceSeries {
        symbol = symbol == null ? "" : symbol.trim().toUpperCase();
        currency = currency == null ? "" : currency.trim().toUpperCase();
        closes = closes == null ? List.of() : closes.stream().sorted(java.util.Comparator.comparing(DailyClose::date)).toList();
        if (symbol.isBlank() || currency.isBlank() || asOf == null || source == null || source.isBlank()) {
            throw new IllegalArgumentException("Historical price series metadata is incomplete");
        }
        if (closes.stream().anyMatch(close -> close == null || close.date() == null
                || !Double.isFinite(close.close()) || close.close() <= 0.0)) {
            throw new IllegalArgumentException("Historical price series contains an invalid close");
        }
    }

    public record DailyClose(LocalDate date, double close) {
    }
}
