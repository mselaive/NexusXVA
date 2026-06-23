package com.nexusxva.portfolio.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.marketdata.application.MarketDataPricingInputService;
import com.nexusxva.marketdata.domain.MarketDataPricingInput;
import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.portfolio.domain.Portfolio;
import com.nexusxva.pricing.application.EuropeanOptionPricingService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class PortfolioBlackScholesPricingServiceTest {

    private static final UUID PORTFOLIO_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-06-01T12:00:00Z");

    @Mock
    private PortfolioStore portfolioStore;

    @Test
    void scalesPriceAndGreeksByQuantity() {
        EuropeanOptionPosition position = position("AAPL", OptionType.CALL, "100.0", "2027-06-01", "2.0");
        PortfolioBlackScholesPricingService service = service(symbol -> Optional.of(pricingInput(symbol)));
        when(portfolioStore.findPortfolio(PORTFOLIO_ID)).thenReturn(Optional.of(portfolio("USD")));
        when(portfolioStore.findActiveEuropeanOptionPositions(PORTFOLIO_ID)).thenReturn(List.of(position));

        PortfolioBlackScholesPricingResult result = service.price(PORTFOLIO_ID, LocalDate.parse("2026-06-01"));

        assertThat(result.positions()).hasSize(1);
        PortfolioPositionPricingResult pricedPosition = result.positions().getFirst();
        assertThat(pricedPosition.unitPrice()).isCloseTo(10.4506, tolerance());
        assertThat(pricedPosition.positionPrice()).isCloseTo(20.9012, tolerance());
        assertThat(pricedPosition.positionGreeks().delta())
                .isCloseTo(pricedPosition.unitGreeks().delta() * 2.0, tolerance());
        assertThat(result.totalPrice()).isCloseTo(pricedPosition.positionPrice(), tolerance());
        assertThat(result.totalGreeks().vega()).isCloseTo(pricedPosition.positionGreeks().vega(), tolerance());
    }

    @Test
    void shortQuantityProducesNegativePositionValue() {
        EuropeanOptionPosition position = position("AAPL", OptionType.PUT, "100.0", "2027-06-01", "-3.0");
        PortfolioBlackScholesPricingService service = service(symbol -> Optional.of(pricingInput(symbol)));
        when(portfolioStore.findPortfolio(PORTFOLIO_ID)).thenReturn(Optional.of(portfolio("USD")));
        when(portfolioStore.findActiveEuropeanOptionPositions(PORTFOLIO_ID)).thenReturn(List.of(position));

        PortfolioBlackScholesPricingResult result = service.price(PORTFOLIO_ID, LocalDate.parse("2026-06-01"));

        PortfolioPositionPricingResult pricedPosition = result.positions().getFirst();
        assertThat(pricedPosition.unitPrice()).isPositive();
        assertThat(pricedPosition.positionPrice()).isNegative();
        assertThat(result.totalPrice()).isEqualTo(pricedPosition.positionPrice());
    }

    @Test
    void calculatesTradeValueAndUnrealizedPnlFromExecutionPrice() {
        EuropeanOptionPosition position = new EuropeanOptionPosition(
                UUID.randomUUID(),
                PORTFOLIO_ID,
                "AAPL",
                OptionType.CALL,
                new BigDecimal("100.0"),
                LocalDate.parse("2027-06-01"),
                new BigDecimal("2.0"),
                new BigDecimal("8.0"),
                com.nexusxva.portfolio.domain.PositionLifecycleStatus.ACTIVE,
                NOW,
                NOW
        );
        PortfolioBlackScholesPricingService service = service(symbol -> Optional.of(pricingInput(symbol)));
        when(portfolioStore.findPortfolio(PORTFOLIO_ID)).thenReturn(Optional.of(portfolio("USD")));
        when(portfolioStore.findActiveEuropeanOptionPositions(PORTFOLIO_ID)).thenReturn(List.of(position));

        PortfolioBlackScholesPricingResult result = service.price(PORTFOLIO_ID, LocalDate.parse("2026-06-01"));

        PortfolioPositionPricingResult pricedPosition = result.positions().getFirst();
        assertThat(pricedPosition.tradeValue()).isEqualTo(16.0);
        assertThat(pricedPosition.unrealizedPnl())
                .isCloseTo(pricedPosition.positionPrice() - 16.0, tolerance());
        assertThat(result.totalTradeValue()).isEqualTo(16.0);
        assertThat(result.totalUnrealizedPnl()).isEqualTo(pricedPosition.unrealizedPnl());
        assertThat(result.positionsWithoutExecutionPrice()).isZero();
    }

    @Test
    void expiredPositionsAreUnpriceableAndExcludedFromTotals() {
        EuropeanOptionPosition expired = position("AAPL", OptionType.CALL, "100.0", "2026-06-01", "2.0");
        PortfolioBlackScholesPricingService service = service(symbol -> Optional.of(pricingInput(symbol)));
        when(portfolioStore.findPortfolio(PORTFOLIO_ID)).thenReturn(Optional.of(portfolio("USD")));
        when(portfolioStore.findActiveEuropeanOptionPositions(PORTFOLIO_ID)).thenReturn(List.of(expired));

        PortfolioBlackScholesPricingResult result = service.price(PORTFOLIO_ID, LocalDate.parse("2026-06-01"));

        assertThat(result.positions()).isEmpty();
        assertThat(result.unpriceablePositions()).hasSize(1);
        assertThat(result.unpriceablePositions().getFirst().status()).isEqualTo(PortfolioPricingStatus.UNPRICEABLE_EXPIRED);
        assertThat(result.totalPrice()).isZero();
        assertThat(result.totalGreeks()).isEqualTo(PortfolioGreeks.zero());
    }

    @Test
    void rejectsNonUsdPortfolioForV1() {
        PortfolioBlackScholesPricingService service = service(symbol -> Optional.of(pricingInput(symbol)));
        when(portfolioStore.findPortfolio(PORTFOLIO_ID)).thenReturn(Optional.of(portfolio("EUR")));

        assertThatThrownBy(() -> service.price(PORTFOLIO_ID, LocalDate.parse("2026-06-01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Portfolio pricing V1 supports USD baseCurrency only");
    }

    @Test
    void rejectsMissingMarketDataPricingInputs() {
        EuropeanOptionPosition position = position("FAKE", OptionType.CALL, "100.0", "2027-06-01", "1.0");
        PortfolioBlackScholesPricingService service = service(symbol -> Optional.empty());
        when(portfolioStore.findPortfolio(PORTFOLIO_ID)).thenReturn(Optional.of(portfolio("USD")));
        when(portfolioStore.findActiveEuropeanOptionPositions(PORTFOLIO_ID)).thenReturn(List.of(position));

        assertThatThrownBy(() -> service.price(PORTFOLIO_ID, LocalDate.parse("2026-06-01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Market data pricing inputs unavailable for underlyingSymbol");
    }

    private PortfolioBlackScholesPricingService service(
            java.util.function.Function<String, Optional<MarketDataPricingInput>> pricingInputs
    ) {
        MarketDataPricingInputService pricingInputService = new MarketDataPricingInputService(
                (symbol, maturityDate) -> pricingInputs.apply(symbol)
        );
        return new PortfolioBlackScholesPricingService(
                portfolioStore,
                pricingInputService,
                new EuropeanOptionPricingService()
        );
    }

    private Portfolio portfolio(String baseCurrency) {
        return new Portfolio(PORTFOLIO_ID, "Pricing Book", null, baseCurrency, NOW, NOW, List.of());
    }

    private EuropeanOptionPosition position(
            String symbol,
            OptionType optionType,
            String strike,
            String maturityDate,
            String quantity
    ) {
        return new EuropeanOptionPosition(
                UUID.randomUUID(),
                PORTFOLIO_ID,
                symbol,
                optionType,
                new BigDecimal(strike),
                LocalDate.parse(maturityDate),
                new BigDecimal(quantity),
                NOW,
                NOW
        );
    }

    private MarketDataPricingInput pricingInput(String symbol) {
        return new MarketDataPricingInput(symbol, 100.0, 0.2, 0.05, "USD", NOW, "LOCAL", false);
    }

    private org.assertj.core.data.Offset<Double> tolerance() {
        return org.assertj.core.data.Offset.offset(1.0e-4);
    }
}
