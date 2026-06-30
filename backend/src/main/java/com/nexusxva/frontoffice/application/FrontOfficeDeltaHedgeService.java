package com.nexusxva.frontoffice.application;

import com.nexusxva.portfolio.application.CashEquityPositionPricingResult;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingResult;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingService;
import com.nexusxva.portfolio.application.PortfolioPositionPricingResult;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FrontOfficeDeltaHedgeService {

    private static final String MODEL = "DELTA_HEDGE_ANALYSIS_V1";

    private final PortfolioBlackScholesPricingService pricingService;

    public FrontOfficeDeltaHedgeService(PortfolioBlackScholesPricingService pricingService) {
        this.pricingService = pricingService;
    }

    public DeltaHedgeAnalysisResult run(
            UUID portfolioId,
            LocalDate valuationDate,
            Map<String, Double> targetDeltaBySymbol
    ) {
        PortfolioBlackScholesPricingResult pricing = pricingService.price(portfolioId, valuationDate);
        Map<String, DeltaAccumulator> bySymbol = new HashMap<>();

        for (PortfolioPositionPricingResult position : pricing.positions()) {
            bySymbol.computeIfAbsent(position.underlyingSymbol(), DeltaAccumulator::new)
                    .addOptionDelta(position.positionGreeks().delta(), position.marketData().spot());
        }

        for (CashEquityPositionPricingResult position : pricing.cashEquityPositions()) {
            bySymbol.computeIfAbsent(position.underlyingSymbol(), DeltaAccumulator::new)
                    .addCashDelta(position.positionGreeks().delta(), position.spot());
        }

        Map<String, Double> normalizedTargets = normalizeTargets(targetDeltaBySymbol);
        normalizedTargets.keySet().forEach(symbol -> bySymbol.computeIfAbsent(symbol, DeltaAccumulator::new));

        return new DeltaHedgeAnalysisResult(
                portfolioId,
                pricing.valuationDate(),
                MODEL,
                pricing.baseCurrency(),
                bySymbol.values()
                        .stream()
                        .map(accumulator -> accumulator.toRow(normalizedTargets.getOrDefault(accumulator.symbol(), 0.0)))
                        .sorted(Comparator.comparing(DeltaHedgeRow::symbol))
                        .toList()
        );
    }

    private Map<String, Double> normalizeTargets(Map<String, Double> targets) {
        if (targets == null || targets.isEmpty()) {
            return Map.of();
        }
        Map<String, Double> normalized = new HashMap<>();
        targets.forEach((symbol, target) -> {
            if (symbol != null && !symbol.isBlank()) {
                double value = target == null ? 0.0 : target;
                if (!Double.isFinite(value)) {
                    throw new IllegalArgumentException("targetDeltaBySymbol values must be finite");
                }
                normalized.put(symbol.trim().toUpperCase(), value);
            }
        });
        return normalized;
    }

    private static class DeltaAccumulator {
        private final String symbol;
        private double optionDelta;
        private double cashDelta;
        private double spot;

        private DeltaAccumulator(String symbol) {
            this.symbol = symbol;
        }

        private String symbol() {
            return symbol;
        }

        private void addOptionDelta(double delta, double spot) {
            this.optionDelta += delta;
            this.spot = spot;
        }

        private void addCashDelta(double delta, double spot) {
            this.cashDelta += delta;
            this.spot = spot;
        }

        private DeltaHedgeRow toRow(double targetDelta) {
            double netDelta = optionDelta + cashDelta;
            double suggestedQuantity = targetDelta - netDelta;
            return new DeltaHedgeRow(
                    symbol,
                    optionDelta,
                    cashDelta,
                    netDelta,
                    targetDelta,
                    suggestedQuantity,
                    spot,
                    suggestedQuantity * spot
            );
        }
    }
}
