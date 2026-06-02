package com.nexusxva.marketdata.infrastructure;

import com.nexusxva.marketdata.application.MarketDataInstrumentGateway;
import com.nexusxva.marketdata.application.MarketDataPricingInputGateway;
import com.nexusxva.marketdata.domain.MarketDataPricingInput;
import com.nexusxva.marketdata.domain.MarketInstrument;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@ConditionalOnProperty(prefix = "nexusxva.market-data", name = "provider", havingValue = "local")
class LocalWatchlistMarketDataInstrumentGateway implements MarketDataInstrumentGateway, MarketDataPricingInputGateway {

    private static final Set<String> EQUITIES = Set.of(
            "AAPL", "MSFT", "NVDA", "AMZN", "GOOGL", "META", "TSLA", "AVGO", "ORCL", "AMD",
            "JPM", "BAC", "GS", "MS", "C", "WFC"
    );
    private static final Set<String> ETFS = Set.of("SPY", "QQQ", "DIA", "IWM", "VTI", "TLT");
    private static final Set<String> METAL_ETFS = Set.of("GLD", "SLV", "CPER");
    private static final Map<String, String> ASSET_CLASSES = assetClasses();
    private static final Map<String, LocalPricingInput> PRICING_INPUTS = pricingInputs();

    @Override
    public Optional<MarketInstrument> findInstrument(String symbol) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) {
            return Optional.empty();
        }
        String assetClass = ASSET_CLASSES.get(normalized);
        if (assetClass == null) {
            return Optional.empty();
        }

        return Optional.of(new MarketInstrument(
                normalized,
                true,
                normalized + " local watchlist instrument",
                assetClass,
                "USD"
        ));
    }

    @Override
    public Optional<MarketDataPricingInput> findEuropeanOptionPricingInput(String symbol, LocalDate maturityDate) {
        String normalized = normalizeSymbol(symbol);
        if (normalized == null) {
            return Optional.empty();
        }

        LocalPricingInput input = PRICING_INPUTS.get(normalized);
        if (input == null) {
            return Optional.empty();
        }

        return Optional.of(new MarketDataPricingInput(
                normalized,
                input.spot(),
                input.volatility(),
                input.riskFreeRate(),
                input.dividendYield(),
                "USD",
                Instant.now(),
                "LOCAL",
                false
        ));
    }

    private static Map<String, String> assetClasses() {
        Map<String, String> equities = EQUITIES.stream()
                .collect(Collectors.toMap(symbol -> symbol, symbol -> "EQUITY"));
        Map<String, String> etfs = ETFS.stream()
                .collect(Collectors.toMap(symbol -> symbol, symbol -> "ETF"));
        Map<String, String> metalEtfs = METAL_ETFS.stream()
                .collect(Collectors.toMap(symbol -> symbol, symbol -> "METAL_ETF"));

        java.util.HashMap<String, String> assetClasses = new java.util.HashMap<>();
        assetClasses.putAll(equities);
        assetClasses.putAll(etfs);
        assetClasses.putAll(metalEtfs);
        return Map.copyOf(assetClasses);
    }

    private static Map<String, LocalPricingInput> pricingInputs() {
        return Map.ofEntries(
                Map.entry("AAPL", new LocalPricingInput(190.0, 0.22, 0.045, 0.005)),
                Map.entry("MSFT", new LocalPricingInput(425.0, 0.21, 0.045, 0.008)),
                Map.entry("NVDA", new LocalPricingInput(115.0, 0.42, 0.045, 0.000)),
                Map.entry("AMZN", new LocalPricingInput(185.0, 0.28, 0.045, 0.000)),
                Map.entry("GOOGL", new LocalPricingInput(170.0, 0.25, 0.045, 0.000)),
                Map.entry("META", new LocalPricingInput(480.0, 0.30, 0.045, 0.004)),
                Map.entry("TSLA", new LocalPricingInput(180.0, 0.55, 0.045, 0.000)),
                Map.entry("AVGO", new LocalPricingInput(1500.0, 0.35, 0.045, 0.012)),
                Map.entry("ORCL", new LocalPricingInput(120.0, 0.24, 0.045, 0.013)),
                Map.entry("AMD", new LocalPricingInput(160.0, 0.40, 0.045, 0.000)),
                Map.entry("JPM", new LocalPricingInput(200.0, 0.23, 0.045, 0.024)),
                Map.entry("BAC", new LocalPricingInput(38.0, 0.27, 0.045, 0.026)),
                Map.entry("GS", new LocalPricingInput(450.0, 0.26, 0.045, 0.025)),
                Map.entry("MS", new LocalPricingInput(100.0, 0.25, 0.045, 0.035)),
                Map.entry("C", new LocalPricingInput(60.0, 0.28, 0.045, 0.034)),
                Map.entry("WFC", new LocalPricingInput(58.0, 0.26, 0.045, 0.027)),
                Map.entry("SPY", new LocalPricingInput(520.0, 0.18, 0.045, 0.013)),
                Map.entry("QQQ", new LocalPricingInput(445.0, 0.22, 0.045, 0.006)),
                Map.entry("DIA", new LocalPricingInput(390.0, 0.16, 0.045, 0.017)),
                Map.entry("IWM", new LocalPricingInput(205.0, 0.24, 0.045, 0.012)),
                Map.entry("VTI", new LocalPricingInput(255.0, 0.18, 0.045, 0.014)),
                Map.entry("TLT", new LocalPricingInput(92.0, 0.20, 0.045, 0.038)),
                Map.entry("GLD", new LocalPricingInput(215.0, 0.17, 0.045, 0.000)),
                Map.entry("SLV", new LocalPricingInput(26.0, 0.32, 0.045, 0.000)),
                Map.entry("CPER", new LocalPricingInput(28.0, 0.29, 0.045, 0.000))
        );
    }

    private static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    private record LocalPricingInput(double spot, double volatility, double riskFreeRate, double dividendYield) {
    }
}
