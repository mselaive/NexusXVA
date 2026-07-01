package com.nexusxva.portfolio.application;

import com.nexusxva.marketdata.application.FxRateService;
import com.nexusxva.marketdata.application.MarketDataPricingInputService;
import com.nexusxva.marketdata.domain.FxRate;
import com.nexusxva.marketdata.domain.MarketDataPricingInput;
import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.portfolio.domain.CashEquityPosition;
import com.nexusxva.portfolio.domain.Portfolio;
import com.nexusxva.portfolio.domain.PositionLifecycleStatus;
import com.nexusxva.pricing.application.EuropeanOptionPricingService;
import com.nexusxva.pricing.domain.BlackScholesInput;
import com.nexusxva.pricing.domain.BlackScholesResult;
import com.nexusxva.shared.error.ResourceNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PortfolioBlackScholesPricingService {

    private static final String MODEL = "BLACK_SCHOLES";

    private final PortfolioStore portfolioStore;
    private final MarketDataPricingInputService marketDataPricingInputService;
    private final FxRateService fxRateService;
    private final EuropeanOptionPricingService pricingService;

    public PortfolioBlackScholesPricingService(
            PortfolioStore portfolioStore,
            MarketDataPricingInputService marketDataPricingInputService,
            FxRateService fxRateService,
            EuropeanOptionPricingService pricingService
    ) {
        this.portfolioStore = portfolioStore;
        this.marketDataPricingInputService = marketDataPricingInputService;
        this.fxRateService = fxRateService;
        this.pricingService = pricingService;
    }

    @Transactional(readOnly = true)
    public PortfolioBlackScholesPricingResult price(UUID portfolioId, LocalDate valuationDate) {
        LocalDate resolvedValuationDate = valuationDate == null
                ? LocalDate.now(ZoneOffset.UTC)
                : valuationDate;
        Portfolio portfolio = portfolio(portfolioId);

        List<PortfolioPositionPricingResult> pricedPositions = new ArrayList<>();
        List<CashEquityPositionPricingResult> pricedCashEquities = new ArrayList<>();
        List<UnpriceablePortfolioPosition> unpriceablePositions = new ArrayList<>();
        double totalPrice = 0.0;
        double totalTradeValue = 0.0;
        double totalUnrealizedPnl = 0.0;
        int positionsWithoutExecutionPrice = 0;
        PortfolioGreeks totalGreeks = PortfolioGreeks.zero();

        for (EuropeanOptionPosition position : portfolioStore.findActiveEuropeanOptionPositions(portfolioId)) {
            if (!position.maturityDate().isAfter(resolvedValuationDate)) {
                unpriceablePositions.add(expiredPosition(position));
                continue;
            }

            PortfolioPositionPricingResult pricedPosition = pricePosition(position, portfolio.baseCurrency(), resolvedValuationDate);
            pricedPositions.add(pricedPosition);
            totalPrice += pricedPosition.positionPrice();
            if (pricedPosition.tradeValue() == null) {
                positionsWithoutExecutionPrice++;
            } else {
                totalTradeValue += pricedPosition.tradeValue();
                totalUnrealizedPnl += pricedPosition.unrealizedPnl();
            }
            totalGreeks = totalGreeks.plus(pricedPosition.positionGreeks());
        }

        for (CashEquityPosition position : portfolioStore.findActiveCashEquityPositions(portfolioId)) {
            CashEquityPositionPricingResult pricedPosition = priceCashEquityPosition(position, portfolio.baseCurrency(), resolvedValuationDate);
            pricedCashEquities.add(pricedPosition);
            totalPrice += pricedPosition.marketValue();
            if (pricedPosition.tradeValue() == null) {
                positionsWithoutExecutionPrice++;
            } else {
                totalTradeValue += pricedPosition.tradeValue();
                totalUnrealizedPnl += pricedPosition.unrealizedPnl();
            }
            totalGreeks = totalGreeks.plus(pricedPosition.positionGreeks());
        }

        return new PortfolioBlackScholesPricingResult(
                portfolioId,
                resolvedValuationDate,
                MODEL,
                portfolio.baseCurrency(),
                totalPrice,
                totalTradeValue,
                totalUnrealizedPnl,
                positionsWithoutExecutionPrice,
                totalGreeks,
                pricedPositions,
                pricedCashEquities,
                unpriceablePositions
        );
    }

    @Transactional(readOnly = true)
    public PortfolioPositionPricingResult priceHypotheticalPosition(
            UUID portfolioId,
            AddEuropeanOptionPositionCommand command,
            LocalDate valuationDate
    ) {
        LocalDate resolvedValuationDate = valuationDate == null
                ? LocalDate.now(ZoneOffset.UTC)
                : valuationDate;
        Portfolio portfolio = portfolio(portfolioId);
        if (!command.maturityDate().isAfter(resolvedValuationDate)) {
            throw new IllegalArgumentException("Hypothetical trade maturityDate must be after valuationDate for Black-Scholes what-if");
        }
        return pricePosition(new EuropeanOptionPosition(
                UUID.randomUUID(),
                portfolioId,
                command.underlyingSymbol(),
                command.optionType(),
                command.strike(),
                command.maturityDate(),
                command.quantity(),
                PositionLifecycleStatus.ACTIVE,
                Instant.EPOCH,
                Instant.EPOCH
        ), portfolio.baseCurrency(), resolvedValuationDate);
    }

    private Portfolio portfolio(UUID portfolioId) {
        return portfolioStore.findPortfolio(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
    }

    private PortfolioPositionPricingResult pricePosition(
            EuropeanOptionPosition position,
            String baseCurrency,
            LocalDate valuationDate
    ) {
        MarketDataPricingInput marketData = marketDataPricingInputService.europeanOptionPricingInput(
                position.underlyingSymbol(),
                position.maturityDate()
        );
        FxRate fxRate = fxRateService.rate(marketData.currency(), baseCurrency);
        double fxRateToBase = fxRate.rate();

        double timeToMaturityYears = ChronoUnit.DAYS.between(valuationDate, position.maturityDate()) / 365.0;
        BlackScholesResult unitResult = pricingService.priceWithBlackScholes(new BlackScholesInput(
                position.optionType(),
                marketData.spot(),
                position.strike().doubleValue(),
                timeToMaturityYears,
                marketData.riskFreeRate(),
                marketData.volatility(),
                marketData.dividendYield()
        ));

        double quantity = position.quantity().doubleValue();
        double unitPrice = unitResult.price() * fxRateToBase;
        double positionPrice = unitPrice * quantity;
        Double executionPrice = position.executionPrice() == null ? null : position.executionPrice().doubleValue();
        Double executionPriceBase = executionPrice == null ? null : executionPrice * fxRateToBase;
        Double tradeValue = executionPriceBase == null ? null : executionPriceBase * quantity;
        Double unrealizedPnl = tradeValue == null ? null : positionPrice - tradeValue;
        PortfolioGreeks unitGreeks = PortfolioGreeks.scaled(unitResult.greeks(), 1.0).monetaryGreeksConverted(fxRateToBase);
        PortfolioGreeks positionGreeks = PortfolioGreeks.scaled(unitResult.greeks(), quantity).monetaryGreeksConverted(fxRateToBase);

        return new PortfolioPositionPricingResult(
                position.id(),
                PortfolioPricingStatus.PRICED,
                position.underlyingSymbol(),
                quantity,
                unitPrice,
                positionPrice,
                executionPriceBase,
                tradeValue,
                unrealizedPnl,
                unitGreeks,
                positionGreeks,
                new PortfolioPositionMarketData(
                        marketData.spot(),
                        marketData.volatility(),
                        marketData.riskFreeRate(),
                        marketData.dividendYield(),
                        marketData.currency(),
                        baseCurrency,
                        fxRateToBase,
                        marketData.asOf(),
                        marketDataSource(marketData, baseCurrency, fxRate),
                        marketDataStale(marketData, baseCurrency, fxRate)
                )
        );
    }

    private CashEquityPositionPricingResult priceCashEquityPosition(
            CashEquityPosition position,
            String baseCurrency,
            LocalDate valuationDate
    ) {
        MarketDataPricingInput marketData = marketDataPricingInputService.europeanOptionPricingInput(
                position.underlyingSymbol(),
                valuationDate.plusYears(1)
        );
        FxRate fxRate = fxRateService.rate(marketData.currency(), baseCurrency);
        double fxRateToBase = fxRate.rate();

        double quantity = position.quantity().doubleValue();
        double spot = marketData.spot() * fxRateToBase;
        double marketValue = spot * quantity;
        Double executionPrice = position.executionPrice() == null ? null : position.executionPrice().doubleValue();
        Double executionPriceBase = executionPrice == null ? null : executionPrice * fxRateToBase;
        Double tradeValue = executionPriceBase == null ? null : executionPriceBase * quantity;
        Double unrealizedPnl = tradeValue == null ? null : marketValue - tradeValue;
        PortfolioGreeks positionGreeks = new PortfolioGreeks(quantity, 0.0, 0.0, 0.0, 0.0);

        return new CashEquityPositionPricingResult(
                position.id(),
                PortfolioPricingStatus.PRICED,
                position.underlyingSymbol(),
                quantity,
                spot,
                marketValue,
                executionPriceBase,
                tradeValue,
                unrealizedPnl,
                positionGreeks,
                new PortfolioPositionMarketData(
                        marketData.spot(),
                        marketData.volatility(),
                        marketData.riskFreeRate(),
                        marketData.dividendYield(),
                        marketData.currency(),
                        baseCurrency,
                        fxRateToBase,
                        marketData.asOf(),
                        marketDataSource(marketData, baseCurrency, fxRate),
                        marketDataStale(marketData, baseCurrency, fxRate)
                )
        );
    }

    private String marketDataSource(MarketDataPricingInput marketData, String baseCurrency, FxRate fxRate) {
        if (marketData.currency().equals(baseCurrency)) {
            return marketData.source();
        }
        return marketData.source() + "+" + fxRate.source();
    }

    private boolean marketDataStale(MarketDataPricingInput marketData, String baseCurrency, FxRate fxRate) {
        return marketData.currency().equals(baseCurrency)
                ? marketData.stale()
                : marketData.stale() || fxRate.stale();
    }

    private UnpriceablePortfolioPosition expiredPosition(EuropeanOptionPosition position) {
        return new UnpriceablePortfolioPosition(
                position.id(),
                PortfolioPricingStatus.UNPRICEABLE_EXPIRED,
                "Position maturityDate must be after valuationDate for Black-Scholes pricing"
        );
    }
}
