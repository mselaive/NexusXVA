package com.nexusxva.marketrisk.application;

import com.nexusxva.marketdata.application.FxRateService;
import com.nexusxva.marketdata.application.HistoricalMarketDataGateway;
import com.nexusxva.marketdata.application.MarketDataPricingInputService;
import com.nexusxva.marketdata.domain.HistoricalPriceSeries;
import com.nexusxva.marketdata.domain.MarketDataPricingInput;
import com.nexusxva.portfolio.application.PortfolioStore;
import com.nexusxva.portfolio.domain.CashEquityPosition;
import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.portfolio.domain.Portfolio;
import com.nexusxva.pricing.application.EuropeanOptionPricingService;
import com.nexusxva.pricing.domain.BlackScholesInput;
import com.nexusxva.shared.error.ResourceNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoricalVarService {
    public static final String MODEL = "HISTORICAL_FULL_REVALUATION_SPOT_V1";
    private static final int REQUESTED_CLOSES = 260;
    private static final int MINIMUM_ALIGNED_RETURNS = 250;
    private static final double CONFIDENCE_LEVEL = 0.99;

    private final PortfolioStore portfolioStore;
    private final HistoricalMarketDataGateway historicalGateway;
    private final MarketDataPricingInputService pricingInputService;
    private final FxRateService fxRateService;
    private final EuropeanOptionPricingService pricingService;

    public HistoricalVarService(PortfolioStore portfolioStore, HistoricalMarketDataGateway historicalGateway,
                                MarketDataPricingInputService pricingInputService, FxRateService fxRateService,
                                EuropeanOptionPricingService pricingService) {
        this.portfolioStore = portfolioStore;
        this.historicalGateway = historicalGateway;
        this.pricingInputService = pricingInputService;
        this.fxRateService = fxRateService;
        this.pricingService = pricingService;
    }

    @Transactional(readOnly = true)
    public HistoricalVarResult calculate(UUID portfolioId, LocalDate valuationDate) {
        Portfolio portfolio = portfolioStore.findPortfolio(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
        List<EuropeanOptionPosition> options = portfolioStore.findActiveEuropeanOptionPositions(portfolioId).stream()
                .filter(position -> position.maturityDate().isAfter(valuationDate)).toList();
        List<CashEquityPosition> equities = portfolioStore.findActiveCashEquityPositions(portfolioId);
        Set<String> symbols = new HashSet<>();
        options.forEach(position -> symbols.add(position.underlyingSymbol()));
        equities.forEach(position -> symbols.add(position.underlyingSymbol()));
        if (symbols.isEmpty()) return empty(portfolio, valuationDate);

        Map<String, HistoricalPriceSeries> history = historicalGateway.dailyCloses(symbols, REQUESTED_CLOSES).stream()
                .collect(Collectors.toMap(HistoricalPriceSeries::symbol, value -> value));
        List<String> missing = symbols.stream().filter(symbol -> !history.containsKey(symbol)).sorted().toList();
        if (!missing.isEmpty()) throw new IllegalArgumentException("Historical market data missing for symbols: " + String.join(", ", missing));

        Map<String, Map<LocalDate, Double>> returns = new LinkedHashMap<>();
        Map<String, Integer> observationsBySymbol = new LinkedHashMap<>();
        for (String symbol : symbols.stream().sorted().toList()) {
            Map<LocalDate, Double> symbolReturns = logReturns(history.get(symbol));
            returns.put(symbol, symbolReturns);
            observationsBySymbol.put(symbol, symbolReturns.size());
        }
        Set<LocalDate> aligned = new HashSet<>(returns.values().iterator().next().keySet());
        returns.values().forEach(series -> aligned.retainAll(series.keySet()));
        List<LocalDate> scenarioDates = aligned.stream().sorted().toList();
        if (scenarioDates.size() < MINIMUM_ALIGNED_RETURNS) {
            throw new IllegalArgumentException("Historical VaR requires at least 250 aligned returns; available=" + scenarioDates.size());
        }

        Map<String, MarketDataPricingInput> inputs = currentInputs(symbols, options, valuationDate);
        List<String> currencyMismatches = symbols.stream()
                .filter(symbol -> !history.get(symbol).currency().equals(inputs.get(symbol).currency()))
                .sorted()
                .toList();
        if (!currencyMismatches.isEmpty()) {
            throw new IllegalArgumentException("Historical and current market data currencies differ for symbols: "
                    + String.join(", ", currencyMismatches));
        }
        Map<String, Double> fx = inputs.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                entry -> fxRateService.rate(entry.getValue().currency(), portfolio.baseCurrency()).rate()));
        List<ScenarioValue> scenarios = new ArrayList<>();
        for (LocalDate scenarioDate : scenarioDates) {
            Map<String, Double> shockedSpots = new HashMap<>();
            for (String symbol : symbols) shockedSpots.put(symbol, inputs.get(symbol).spot() * Math.exp(returns.get(symbol).get(scenarioDate)));
            Map<String, Double> symbolPnl = symbolPnl(options, equities, valuationDate, inputs, fx, shockedSpots);
            double pnl = symbolPnl.values().stream().mapToDouble(Double::doubleValue).sum();
            scenarios.add(new ScenarioValue(scenarioDate, pnl, -pnl, symbolPnl));
        }
        scenarios.sort(Comparator.comparingDouble(ScenarioValue::loss));
        int quantileIndex = Math.max(0, (int) Math.ceil(CONFIDENCE_LEVEL * scenarios.size()) - 1);
        ScenarioValue varScenario = scenarios.get(quantileIndex);
        double var = Math.max(0.0, varScenario.loss());
        List<ScenarioValue> tail = scenarios.stream().filter(scenario -> scenario.loss() >= varScenario.loss()).toList();
        double es = Math.max(var, tail.stream().mapToDouble(ScenarioValue::loss).average().orElse(var));
        ScenarioValue worst = scenarios.get(scenarios.size() - 1);
        List<ScenarioValue> byWorst = scenarios.stream().sorted(Comparator.comparingDouble(ScenarioValue::loss).reversed()).toList();

        Instant asOf = history.values().stream().map(HistoricalPriceSeries::asOf).min(Instant::compareTo).orElseThrow();
        boolean stale = history.values().stream().anyMatch(HistoricalPriceSeries::stale)
                || inputs.values().stream().anyMatch(MarketDataPricingInput::stale);
        return new HistoricalVarResult(portfolioId, valuationDate, MODEL, portfolio.baseCurrency(), CONFIDENCE_LEVEL,
                scenarios.size(), var, es, Math.max(0.0, worst.loss()), worst.date(), varScenario.date(), asOf, stale,
                histogram(scenarios), byWorst.stream().limit(10)
                        .map(value -> new HistoricalVarResult.HistoricalScenario(value.date(), value.pnl(), value.loss())).toList(),
                varScenario.symbolPnl().entrySet().stream().sorted(Map.Entry.comparingByKey())
                        .map(entry -> new HistoricalVarResult.SymbolContribution(entry.getKey(), entry.getValue(), -entry.getValue())).toList(),
                observationsBySymbol, List.of("VOLATILITY", "RATES", "DIVIDEND_YIELD", "FX"));
    }

    private HistoricalVarResult empty(Portfolio portfolio, LocalDate valuationDate) {
        return new HistoricalVarResult(portfolio.id(), valuationDate, MODEL, portfolio.baseCurrency(), CONFIDENCE_LEVEL,
                0, 0, 0, 0, null, null, null, false, List.of(), List.of(), List.of(), Map.of(),
                List.of("VOLATILITY", "RATES", "DIVIDEND_YIELD", "FX"));
    }

    private Map<LocalDate, Double> logReturns(HistoricalPriceSeries series) {
        Map<String, HistoricalPriceSeries.DailyClose> unique = series.closes().stream().collect(Collectors.toMap(
                close -> close.date().toString(), close -> close, (left, right) -> right, LinkedHashMap::new));
        List<HistoricalPriceSeries.DailyClose> closes = unique.values().stream().sorted(Comparator.comparing(HistoricalPriceSeries.DailyClose::date)).toList();
        Map<LocalDate, Double> result = new LinkedHashMap<>();
        for (int i = 1; i < closes.size(); i++) result.put(closes.get(i).date(), Math.log(closes.get(i).close() / closes.get(i - 1).close()));
        return result;
    }

    private Map<String, MarketDataPricingInput> currentInputs(Set<String> symbols, List<EuropeanOptionPosition> options, LocalDate valuationDate) {
        Map<String, LocalDate> maturities = options.stream().collect(Collectors.toMap(EuropeanOptionPosition::underlyingSymbol,
                EuropeanOptionPosition::maturityDate, (a, b) -> a.isAfter(b) ? a : b));
        Map<String, MarketDataPricingInput> inputs = new HashMap<>();
        for (String symbol : symbols) inputs.put(symbol, pricingInputService.europeanOptionPricingInput(symbol,
                maturities.getOrDefault(symbol, valuationDate.plusYears(1))));
        return Map.copyOf(inputs);
    }

    private Map<String, Double> symbolPnl(List<EuropeanOptionPosition> options, List<CashEquityPosition> equities,
                                          LocalDate valuationDate, Map<String, MarketDataPricingInput> inputs,
                                          Map<String, Double> fx, Map<String, Double> shockedSpots) {
        Map<String, Double> baseBySymbol = valuesBySymbol(options, equities, valuationDate, inputs, fx, Map.of());
        Map<String, Double> stressedBySymbol = valuesBySymbol(options, equities, valuationDate, inputs, fx, shockedSpots);
        return baseBySymbol.keySet().stream().collect(Collectors.toMap(symbol -> symbol,
                symbol -> stressedBySymbol.getOrDefault(symbol, 0.0) - baseBySymbol.getOrDefault(symbol, 0.0)));
    }

    private Map<String, Double> valuesBySymbol(List<EuropeanOptionPosition> options, List<CashEquityPosition> equities,
                                               LocalDate valuationDate, Map<String, MarketDataPricingInput> inputs,
                                               Map<String, Double> fx, Map<String, Double> shockedSpots) {
        Map<String, Double> values = new HashMap<>();
        for (EuropeanOptionPosition position : options) {
            MarketDataPricingInput input = inputs.get(position.underlyingSymbol());
            double spot = shockedSpots.getOrDefault(position.underlyingSymbol(), input.spot());
            double years = ChronoUnit.DAYS.between(valuationDate, position.maturityDate()) / 365.0;
            double price = pricingService.priceWithBlackScholes(new BlackScholesInput(position.optionType(), spot,
                    position.strike().doubleValue(), years, input.riskFreeRate(), input.volatility(), input.dividendYield())).price();
            values.merge(position.underlyingSymbol(), price * position.quantity().doubleValue() * fx.get(position.underlyingSymbol()), Double::sum);
        }
        for (CashEquityPosition position : equities) {
            double spot = shockedSpots.getOrDefault(position.underlyingSymbol(), inputs.get(position.underlyingSymbol()).spot());
            values.merge(position.underlyingSymbol(), spot * position.quantity().doubleValue() * fx.get(position.underlyingSymbol()), Double::sum);
        }
        return values;
    }

    private List<HistoricalVarResult.HistogramBin> histogram(List<ScenarioValue> scenarios) {
        double min = scenarios.stream().mapToDouble(ScenarioValue::pnl).min().orElse(0);
        double max = scenarios.stream().mapToDouble(ScenarioValue::pnl).max().orElse(0);
        if (min == max) return List.of(new HistoricalVarResult.HistogramBin(min, max, scenarios.size()));
        int bins = 20;
        double width = (max - min) / bins;
        int[] counts = new int[bins];
        scenarios.forEach(scenario -> counts[Math.min(bins - 1, (int) ((scenario.pnl() - min) / width))]++);
        List<HistoricalVarResult.HistogramBin> result = new ArrayList<>();
        for (int i = 0; i < bins; i++) result.add(new HistoricalVarResult.HistogramBin(min + i * width, min + (i + 1) * width, counts[i]));
        return result;
    }

    private record ScenarioValue(LocalDate date, double pnl, double loss, Map<String, Double> symbolPnl) {}
}
