package com.nexusxva.exposure.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.marketdata.application.FxRateService;
import com.nexusxva.marketdata.application.MarketDataPricingInputService;
import com.nexusxva.marketdata.domain.FxRate;
import com.nexusxva.marketdata.domain.MarketDataPricingInput;
import com.nexusxva.portfolio.application.AddCashEquityPositionCommand;
import com.nexusxva.portfolio.application.AddEuropeanOptionPositionCommand;
import com.nexusxva.portfolio.application.CreatePortfolioCommand;
import com.nexusxva.portfolio.application.PortfolioStore;
import com.nexusxva.portfolio.application.UpdatePortfolioCommand;
import com.nexusxva.portfolio.domain.CashEquityPosition;
import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.portfolio.domain.Portfolio;
import com.nexusxva.portfolio.domain.PortfolioSummary;
import com.nexusxva.pricing.application.EuropeanOptionPricingService;
import com.nexusxva.shared.error.ResourceNotFoundException;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class ExposureSimulationServiceTest {

    private static final UUID PORTFOLIO_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-06-05T12:00:00Z");

    private final FakePortfolioStore portfolioStore = new FakePortfolioStore();

    @Test
    void emptyPortfolioReturnsZeroExposurePoints() {
        ExposureSimulationService service = service((symbol, maturityDate) -> {
            throw new AssertionError("market data should not be called for an empty portfolio");
        });
        portfolioStore.portfolio = portfolio("USD");
        portfolioStore.activeOptionPositions = List.of();

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
        portfolioStore.portfolio = portfolio("USD");
        portfolioStore.activeOptionPositions = List.of(
                position("AAPL", OptionType.CALL, "100.0", "2026-06-05", "1.0")
        );

        ExposureSimulationResult result = service.simulate(command());

        assertThat(result.points()).hasSize(2);
        assertThat(result.points()).allSatisfy(point -> assertThat(point.expectedExposure()).isZero());
    }

    @Test
    void simulatesPositiveExposureForPriceablePosition() {
        ExposureSimulationService service = service((symbol, maturityDate) -> Optional.of(pricingInput(symbol, "USD")));
        portfolioStore.portfolio = portfolio("USD");
        portfolioStore.activeOptionPositions = List.of(
                position("AAPL", OptionType.CALL, "100.0", "2027-06-05", "1.0")
        );

        ExposureSimulationResult result = service.simulate(command());

        assertThat(result.model()).isEqualTo("GBM_BLACK_SCHOLES_EXPOSURE_V1");
        assertThat(result.points()).hasSize(2);
        assertThat(result.points().getFirst().expectedExposure()).isPositive();
    }

    @Test
    void simulatesExposureForNonUsdPortfolioByConvertingToBaseCurrency() {
        ExposureSimulationService service = service((symbol, maturityDate) -> Optional.of(pricingInput(symbol, "USD")));
        portfolioStore.portfolio = portfolio("EUR");
        portfolioStore.activeOptionPositions = List.of(
                position("AAPL", OptionType.CALL, "100.0", "2027-06-05", "1.0")
        );

        ExposureSimulationResult result = service.simulate(command());

        assertThat(result.baseCurrency()).isEqualTo("EUR");
        assertThat(result.points().getFirst().expectedExposure()).isPositive();
    }

    @Test
    void simulatesExposureForNonUsdMarketDataByConvertingToPortfolioCurrency() {
        ExposureSimulationService service = service((symbol, maturityDate) -> Optional.of(pricingInput(symbol, "EUR")));
        portfolioStore.portfolio = portfolio("USD");
        portfolioStore.activeOptionPositions = List.of(
                position("AAPL", OptionType.CALL, "100.0", "2027-06-05", "1.0")
        );

        ExposureSimulationResult result = service.simulate(command());

        assertThat(result.baseCurrency()).isEqualTo("USD");
        assertThat(result.points().getFirst().expectedExposure()).isPositive();
    }

    @Test
    void unknownPortfolioReturnsNotFound() {
        ExposureSimulationService service = service((symbol, maturityDate) -> Optional.of(pricingInput(symbol, "USD")));
        portfolioStore.portfolio = null;

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
                new FxRateService((sourceCurrency, targetCurrency) -> Optional.of(new FxRate(
                        sourceCurrency,
                        targetCurrency,
                        sourceCurrency.equals(targetCurrency) ? 1.0 : sourceCurrency.equals("USD") ? 1.0 / 1.09 : 1.09,
                        NOW,
                        "TEST_FX",
                        false
                ))),
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

    private static class FakePortfolioStore implements PortfolioStore {
        private Portfolio portfolio;
        private List<EuropeanOptionPosition> activeOptionPositions = new ArrayList<>();

        @Override
        public Portfolio createPortfolio(CreatePortfolioCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<PortfolioSummary> listPortfolioSummaries() {
            return List.of();
        }

        @Override
        public Optional<Portfolio> findPortfolio(UUID portfolioId) {
            return Optional.ofNullable(portfolio);
        }

        @Override
        public boolean existsPortfolio(UUID portfolioId) {
            return portfolio != null;
        }

        @Override
        public Portfolio updatePortfolio(UUID portfolioId, UpdatePortfolioCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void archivePortfolio(UUID portfolioId, UUID archivedByUserId, String reason) {
            throw new UnsupportedOperationException();
        }

        @Override
        public EuropeanOptionPosition addEuropeanOptionPosition(UUID portfolioId, AddEuropeanOptionPositionCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CashEquityPosition addCashEquityPosition(UUID portfolioId, AddCashEquityPositionCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<EuropeanOptionPosition> findEuropeanOptionPositions(UUID portfolioId) {
            return activeOptionPositions;
        }

        @Override
        public List<EuropeanOptionPosition> findActiveEuropeanOptionPositions(UUID portfolioId) {
            return activeOptionPositions;
        }

        @Override
        public List<CashEquityPosition> findCashEquityPositions(UUID portfolioId) {
            return List.of();
        }

        @Override
        public List<CashEquityPosition> findActiveCashEquityPositions(UUID portfolioId) {
            return List.of();
        }

        @Override
        public Optional<EuropeanOptionPosition> findEuropeanOptionPosition(UUID portfolioId, UUID positionId) {
            return Optional.empty();
        }

        @Override
        public Optional<EuropeanOptionPosition> findEuropeanOptionPosition(UUID positionId) {
            return Optional.empty();
        }

        @Override
        public void markPositionCancelled(UUID positionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void markPositionAmended(UUID positionId) {
            throw new UnsupportedOperationException();
        }
    }
}
