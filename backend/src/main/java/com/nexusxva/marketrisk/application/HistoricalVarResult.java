package com.nexusxva.marketrisk.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record HistoricalVarResult(
        UUID portfolioId,
        LocalDate valuationDate,
        String model,
        String baseCurrency,
        double confidenceLevel,
        int observations,
        double valueAtRisk,
        double expectedShortfall,
        double worstLoss,
        LocalDate worstScenarioDate,
        LocalDate varScenarioDate,
        Instant marketDataAsOf,
        boolean staleMarketData,
        List<HistogramBin> pnlDistribution,
        List<HistoricalScenario> worstScenarios,
        List<SymbolContribution> varScenarioContributions,
        Map<String, Integer> observationsBySymbol,
        List<String> unmodeledRiskFactors
) {
    public record HistogramBin(double fromPnl, double toPnl, int count) {}
    public record HistoricalScenario(LocalDate date, double pnl, double loss) {}
    public record SymbolContribution(String symbol, double pnl, double loss) {}
}
