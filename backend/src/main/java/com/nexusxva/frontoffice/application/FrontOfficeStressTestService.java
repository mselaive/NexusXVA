package com.nexusxva.frontoffice.application;

import com.nexusxva.marketdata.application.MarketDataPricingInputService;
import com.nexusxva.marketdata.domain.MarketDataPricingInput;
import com.nexusxva.portfolio.application.AddEuropeanOptionPositionCommand;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingResult;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingService;
import com.nexusxva.portfolio.application.PortfolioGreeks;
import com.nexusxva.portfolio.application.PortfolioPositionMarketData;
import com.nexusxva.portfolio.application.PortfolioPositionPricingResult;
import com.nexusxva.portfolio.application.PortfolioPricingStatus;
import com.nexusxva.portfolio.application.PortfolioStore;
import com.nexusxva.portfolio.application.UnpriceablePortfolioPosition;
import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.portfolio.domain.Portfolio;
import com.nexusxva.portfolio.domain.PositionLifecycleStatus;
import com.nexusxva.pricing.application.EuropeanOptionPricingService;
import com.nexusxva.pricing.domain.BlackScholesInput;
import com.nexusxva.pricing.domain.BlackScholesResult;
import com.nexusxva.shared.error.ResourceNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FrontOfficeStressTestService {

    private static final String MODEL = "BLACK_SCHOLES_STRESS_TEST_V1";
    private static final String SUPPORTED_BASE_CURRENCY = "USD";

    private final PortfolioStore portfolioStore;
    private final PortfolioBlackScholesPricingService portfolioPricingService;
    private final MarketDataPricingInputService marketDataPricingInputService;
    private final EuropeanOptionPricingService pricingService;

    public FrontOfficeStressTestService(
            PortfolioStore portfolioStore,
            PortfolioBlackScholesPricingService portfolioPricingService,
            MarketDataPricingInputService marketDataPricingInputService,
            EuropeanOptionPricingService pricingService
    ) {
        this.portfolioStore = portfolioStore;
        this.portfolioPricingService = portfolioPricingService;
        this.marketDataPricingInputService = marketDataPricingInputService;
        this.pricingService = pricingService;
    }

    @Transactional(readOnly = true)
    public FrontOfficeStressTestResult run(
            UUID portfolioId,
            LocalDate valuationDate,
            AddEuropeanOptionPositionCommand hypotheticalTrade,
            List<StressScenario> scenarios
    ) {
        LocalDate resolvedValuationDate = valuationDate == null
                ? LocalDate.now(ZoneOffset.UTC)
                : valuationDate;
        Portfolio portfolio = portfolioStore.findPortfolio(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
        if (!SUPPORTED_BASE_CURRENCY.equals(portfolio.baseCurrency())) {
            throw new IllegalArgumentException("Portfolio pricing V1 supports USD baseCurrency only");
        }
        if (scenarios == null || scenarios.isEmpty()) {
            throw new IllegalArgumentException("At least one stress scenario is required");
        }

        PortfolioBlackScholesPricingResult base = portfolioPricingService.price(portfolioId, resolvedValuationDate);
        PortfolioPositionPricingResult resolvedHypotheticalTradePricing = null;
        EuropeanOptionPosition resolvedHypotheticalPosition = null;
        if (hypotheticalTrade != null) {
            if (!hypotheticalTrade.maturityDate().isAfter(base.valuationDate())) {
                throw new IllegalArgumentException("Hypothetical trade maturityDate must be after valuationDate for Black-Scholes stress testing");
            }
            resolvedHypotheticalTradePricing = portfolioPricingService.priceHypotheticalPosition(
                    portfolioId,
                    hypotheticalTrade,
                    base.valuationDate()
            );
            resolvedHypotheticalPosition = new EuropeanOptionPosition(
                    UUID.randomUUID(),
                    portfolioId,
                    hypotheticalTrade.underlyingSymbol(),
                    hypotheticalTrade.optionType(),
                    hypotheticalTrade.strike(),
                    hypotheticalTrade.maturityDate(),
                    hypotheticalTrade.quantity(),
                    PositionLifecycleStatus.ACTIVE,
                    Instant.EPOCH,
                    Instant.EPOCH
            );
        }
        PortfolioPositionPricingResult hypotheticalTradePricing = resolvedHypotheticalTradePricing;
        EuropeanOptionPosition hypotheticalPosition = resolvedHypotheticalPosition;

        List<EuropeanOptionPosition> confirmedPositions = portfolioStore.findActiveEuropeanOptionPositions(portfolioId);
        List<StressScenarioResult> scenarioResults = scenarios.stream()
                .map(scenario -> priceScenario(
                        scenario,
                        confirmedPositions,
                        hypotheticalPosition,
                        base.valuationDate(),
                        base.totalPrice(),
                        base.totalGreeks()
                ))
                .toList();

        return new FrontOfficeStressTestResult(
                portfolioId,
                base.valuationDate(),
                MODEL,
                portfolio.baseCurrency(),
                new StressPortfolioTotals(base.totalPrice(), base.totalGreeks()),
                hypotheticalTradePricing,
                scenarioResults,
                base.unpriceablePositions()
        );
    }

    private StressScenarioResult priceScenario(
            StressScenario scenario,
            List<EuropeanOptionPosition> confirmedPositions,
            EuropeanOptionPosition hypotheticalPosition,
            LocalDate valuationDate,
            double basePrice,
            PortfolioGreeks baseGreeks
    ) {
        List<PortfolioPositionPricingResult> positions = new ArrayList<>();
        double totalPrice = 0.0;
        PortfolioGreeks totalGreeks = PortfolioGreeks.zero();

        for (EuropeanOptionPosition position : confirmedPositions) {
            if (!position.maturityDate().isAfter(valuationDate)) {
                continue;
            }
            PortfolioPositionPricingResult priced = priceStressedPosition(position, valuationDate, scenario);
            positions.add(priced);
            totalPrice += priced.positionPrice();
            totalGreeks = totalGreeks.plus(priced.positionGreeks());
        }

        if (hypotheticalPosition != null) {
            PortfolioPositionPricingResult priced = priceStressedPosition(hypotheticalPosition, valuationDate, scenario);
            positions.add(priced);
            totalPrice += priced.positionPrice();
            totalGreeks = totalGreeks.plus(priced.positionGreeks());
        }

        return new StressScenarioResult(
                scenario,
                new StressPortfolioTotals(totalPrice, totalGreeks),
                new StressImpact(
                        totalPrice - basePrice,
                        totalGreeks.delta() - baseGreeks.delta(),
                        totalGreeks.gamma() - baseGreeks.gamma(),
                        totalGreeks.vega() - baseGreeks.vega(),
                        totalGreeks.theta() - baseGreeks.theta(),
                        totalGreeks.rho() - baseGreeks.rho()
                ),
                positions
        );
    }

    private PortfolioPositionPricingResult priceStressedPosition(
            EuropeanOptionPosition position,
            LocalDate valuationDate,
            StressScenario scenario
    ) {
        MarketDataPricingInput marketData = marketDataPricingInputService.europeanOptionPricingInput(
                position.underlyingSymbol(),
                position.maturityDate()
        );
        if (!SUPPORTED_BASE_CURRENCY.equals(marketData.currency())) {
            throw new IllegalArgumentException("Portfolio pricing V1 supports USD market data only");
        }

        double stressedSpot = marketData.spot() * (1.0 + scenario.spotShockPercent());
        double stressedVolatility = marketData.volatility() + basisPointsToDecimal(scenario.volatilityShockBps());
        double stressedRiskFreeRate = marketData.riskFreeRate() + basisPointsToDecimal(scenario.riskFreeRateShockBps());
        double stressedDividendYield = marketData.dividendYield() + basisPointsToDecimal(scenario.dividendYieldShockBps());

        validateStressedInput(stressedSpot, stressedVolatility, stressedDividendYield);

        double timeToMaturityYears = ChronoUnit.DAYS.between(valuationDate, position.maturityDate()) / 365.0;
        BlackScholesResult unitResult = pricingService.priceWithBlackScholes(new BlackScholesInput(
                position.optionType(),
                stressedSpot,
                position.strike().doubleValue(),
                timeToMaturityYears,
                stressedRiskFreeRate,
                stressedVolatility,
                stressedDividendYield
        ));

        double quantity = position.quantity().doubleValue();
        double stressedPositionValue = unitResult.price() * quantity;
        Double executionPrice = position.executionPrice() == null ? null : position.executionPrice().doubleValue();
        Double tradeValue = executionPrice == null ? null : executionPrice * quantity;
        Double unrealizedPnl = tradeValue == null ? null : stressedPositionValue - tradeValue;
        PortfolioGreeks unitGreeks = PortfolioGreeks.scaled(unitResult.greeks(), 1.0);
        PortfolioGreeks positionGreeks = PortfolioGreeks.scaled(unitResult.greeks(), quantity);
        return new PortfolioPositionPricingResult(
                position.id(),
                PortfolioPricingStatus.PRICED,
                position.underlyingSymbol(),
                quantity,
                unitResult.price(),
                stressedPositionValue,
                executionPrice,
                tradeValue,
                unrealizedPnl,
                unitGreeks,
                positionGreeks,
                new PortfolioPositionMarketData(
                        stressedSpot,
                        stressedVolatility,
                        stressedRiskFreeRate,
                        stressedDividendYield,
                        marketData.currency(),
                        marketData.asOf(),
                        marketData.source(),
                        marketData.stale()
                )
        );
    }

    private void validateStressedInput(double spot, double volatility, double dividendYield) {
        if (!Double.isFinite(spot) || spot <= 0.0) {
            throw new IllegalArgumentException("Stressed spot must be greater than zero");
        }
        if (!Double.isFinite(volatility) || volatility <= 0.0) {
            throw new IllegalArgumentException("Stressed volatility must be greater than zero");
        }
        if (!Double.isFinite(dividendYield) || dividendYield < 0.0) {
            throw new IllegalArgumentException("Stressed dividendYield must be greater than or equal to zero");
        }
    }

    private double basisPointsToDecimal(double basisPoints) {
        return basisPoints / 10_000.0;
    }

    public record FrontOfficeStressTestResult(
            UUID portfolioId,
            LocalDate valuationDate,
            String model,
            String baseCurrency,
            StressPortfolioTotals basePortfolio,
            PortfolioPositionPricingResult hypotheticalTrade,
            List<StressScenarioResult> scenarios,
            List<UnpriceablePortfolioPosition> unpriceablePositions
    ) {
    }

    public record StressScenario(
            String name,
            double spotShockPercent,
            double volatilityShockBps,
            double riskFreeRateShockBps,
            double dividendYieldShockBps
    ) {
    }

    public record StressScenarioResult(
            StressScenario scenario,
            StressPortfolioTotals totals,
            StressImpact impact,
            List<PortfolioPositionPricingResult> positions
    ) {
    }

    public record StressPortfolioTotals(
            double totalPrice,
            PortfolioGreeks totalGreeks
    ) {
    }

    public record StressImpact(
            double price,
            double delta,
            double gamma,
            double vega,
            double theta,
            double rho
    ) {
    }
}
