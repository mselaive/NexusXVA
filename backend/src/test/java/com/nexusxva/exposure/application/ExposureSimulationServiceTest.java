package com.nexusxva.exposure.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.marketdata.application.MarketDataPricingInputService;
import com.nexusxva.marketdata.domain.MarketDataPricingInput;
import com.nexusxva.portfolio.application.PortfolioStore;
import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.portfolio.domain.Portfolio;
import com.nexusxva.pricing.application.EuropeanOptionPricingService;
import com.nexusxva.shared.error.ResourceNotFoundException;

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
class ExposureSimulationServiceTest {

    private static final UUID PORTFOLIO_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-06-05T12:00:00Z");

    @Mock
    private PortfolioStore portfolioStore;

    @Test
    void emptyPortfolioReturnsZeroExposurePoints() {
        ExposureSimulationService service = service((symbol, maturityDate) -> {
            throw new AssertionError("market data should not be called for an empty portfolio");
        });
        when(portfolioStore.findPortfolio(PORTFOLIO_ID)).thenReturn(Optional.of(portfolio("USD")));
        when(portfolioStore.findActiveEuropeanOptionPositions(PORTFOLIO_ID)).thenReturn(List.of());

        ExposureSimulationResult result = service.simulate(command());

        assertThat(result.points()).hasSize(2);
        assertThat(result.points()).allSatisfy(point -> {
            assertThat(point.expectedExposure()).isZero();
            assertThat(point.expectedNegativeExposure()).isZero();
            assertThat(point.pfe()).isZero();
        });
    }

    @Test
    void expiredPositionsAreExcludedWithoutMarketDataLookup() {
        ExposureSimulationService service = service((symbol, maturityDate) -> {
            throw new AssertionError("market data should not be called for expired positions");
        });
        when(portfolioStore.findPortfolio(PORTFOLIO_ID)).thenReturn(Optional.of(portfolio("USD")));
        when(portfolioStore.findActiveEuropeanOptionPositions(PORTFOLIO_ID)).thenReturn(List.of(
                position("AAPL", OptionType.CALL, "100.0", "2026-06-05", "1.0")
        ));

        ExposureSimulationResult result = service.simulate(command());

        assertThat(result.points()).hasSize(2);
        assertThat(result.points()).allSatisfy(point -> assertThat(point.expectedExposure()).isZero());
    }

    @Test
    void simulatesPositiveExposureForPriceablePosition() {
        ExposureSimulationService service = service((symbol, maturityDate) -> Optional.of(pricingInput(symbol, "USD")));
        when(portfolioStore.findPortfolio(PORTFOLIO_ID)).thenReturn(Optional.of(portfolio("USD")));
        when(portfolioStore.findActiveEuropeanOptionPositions(PORTFOLIO_ID)).thenReturn(List.of(
                position("AAPL", OptionType.CALL, "100.0", "2027-06-05", "1.0")
        ));

        ExposureSimulationResult result = service.simulate(command());

        assertThat(result.model()).isEqualTo("GBM_BLACK_SCHOLES_EXPOSURE_V1");
        assertThat(result.points()).hasSize(2);
        assertThat(result.points().getFirst().expectedExposure()).isPositive();
    }

    @Test
    void rejectsNonUsdPortfolio() {
        ExposureSimulationService service = service((symbol, maturityDate) -> Optional.of(pricingInput(symbol, "USD")));
        when(portfolioStore.findPortfolio(PORTFOLIO_ID)).thenReturn(Optional.of(portfolio("EUR")));

        assertThatThrownBy(() -> service.simulate(command()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Exposure simulation V1 supports USD baseCurrency only");
    }

    @Test
    void rejectsNonUsdMarketData() {
        ExposureSimulationService service = service((symbol, maturityDate) -> Optional.of(pricingInput(symbol, "EUR")));
        when(portfolioStore.findPortfolio(PORTFOLIO_ID)).thenReturn(Optional.of(portfolio("USD")));
        when(portfolioStore.findActiveEuropeanOptionPositions(PORTFOLIO_ID)).thenReturn(List.of(
                position("AAPL", OptionType.CALL, "100.0", "2027-06-05", "1.0")
        ));

        assertThatThrownBy(() -> service.simulate(command()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Exposure simulation V1 supports USD market data only");
    }

    @Test
    void unknownPortfolioReturnsNotFound() {
        ExposureSimulationService service = service((symbol, maturityDate) -> Optional.of(pricingInput(symbol, "USD")));
        when(portfolioStore.findPortfolio(PORTFOLIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.simulate(command()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Portfolio not found");
    }

    private ExposureSimulationService service(
            java.util.function.BiFunction<String, LocalDate, Optional<MarketDataPricingInput>> pricingInputs
    ) {
        return new ExposureSimulationService(
                portfolioStore,
                new MarketDataPricingInputService(pricingInputs::apply),
                new EuropeanOptionPricingService()
        );
    }

    private ExposureSimulationCommand command() {
        return new ExposureSimulationCommand(
                PORTFOLIO_ID,
                LocalDate.parse("2026-06-05"),
                60,
                2,
                4,
                12345L,
                0.95
        );
    }

    private Portfolio portfolio(String baseCurrency) {
        return new Portfolio(PORTFOLIO_ID, "Exposure Book", null, baseCurrency, NOW, NOW, List.of());
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

    private MarketDataPricingInput pricingInput(String symbol, String currency) {
        return new MarketDataPricingInput(
                symbol,
                100.0,
                0.20,
                0.05,
                0.01,
                currency,
                NOW,
                "TEST",
                false
        );
    }
}
