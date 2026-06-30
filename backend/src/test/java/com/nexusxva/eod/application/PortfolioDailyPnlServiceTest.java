package com.nexusxva.eod.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.nexusxva.eod.domain.PortfolioEodSnapshot;
import com.nexusxva.eod.domain.PositionEodSnapshot;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingResult;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingService;
import com.nexusxva.portfolio.application.PortfolioGreeks;
import com.nexusxva.portfolio.application.PortfolioPositionMarketData;
import com.nexusxva.portfolio.application.PortfolioPositionPricingResult;
import com.nexusxva.portfolio.application.PortfolioPricingStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioDailyPnlServiceTest {

    private static final UUID PORTFOLIO_ID = UUID.randomUUID();
    private static final UUID EXISTING_POSITION = UUID.randomUUID();
    private static final UUID NEW_POSITION = UUID.randomUUID();
    private static final UUID UNKNOWN_POSITION = UUID.randomUUID();
    private static final LocalDate VALUATION_DATE = LocalDate.parse("2026-06-22");

    @Mock
    private PortfolioEodStore store;

    @Mock
    private PortfolioBlackScholesPricingService pricingService;

    @Test
    void usesPriorEodForExistingPositionsAndExecutionForNewTrades() {
        when(pricingService.price(PORTFOLIO_ID, VALUATION_DATE)).thenReturn(pricing());
        when(store.latest(PORTFOLIO_ID)).thenReturn(Optional.of(previousEod()));

        PortfolioDailyPnl result = new PortfolioDailyPnlService(store, pricingService)
                .calculate(PORTFOLIO_ID, VALUATION_DATE);

        assertThat(result.dailyPnl()).isEqualTo(23.0);
        assertThat(result.positionsWithoutReference()).isEqualTo(1);
        assertThat(result.positions()).extracting(PositionDailyPnl::referenceMethod)
                .containsExactly("PRIOR_EOD", "EXECUTION", "UNAVAILABLE");
        assertThat(result.positions().get(0).dailyPnl()).isEqualTo(20.0);
        assertThat(result.positions().get(1).dailyPnl()).isEqualTo(3.0);
        assertThat(result.positions().get(2).dailyPnl()).isNull();
    }

    private PortfolioBlackScholesPricingResult pricing() {
        return new PortfolioBlackScholesPricingResult(
                PORTFOLIO_ID,
                VALUATION_DATE,
                "BLACK_SCHOLES",
                "USD",
                143.0,
                108.0,
                35.0,
                1,
                PortfolioGreeks.zero(),
                List.of(
                        priced(EXISTING_POSITION, "AAPL", 120.0, 100.0, 20.0),
                        priced(NEW_POSITION, "MSFT", 18.0, 15.0, 3.0),
                        priced(UNKNOWN_POSITION, "NVDA", 5.0, null, null)
                ),
                List.of(),
                List.of()
        );
    }

    private PortfolioPositionPricingResult priced(
            UUID positionId,
            String symbol,
            double marketValue,
            Double tradeValue,
            Double unrealizedPnl
    ) {
        return new PortfolioPositionPricingResult(
                positionId,
                PortfolioPricingStatus.PRICED,
                symbol,
                1.0,
                marketValue,
                marketValue,
                tradeValue,
                tradeValue,
                unrealizedPnl,
                PortfolioGreeks.zero(),
                PortfolioGreeks.zero(),
                new PortfolioPositionMarketData(
                        100.0,
                        0.2,
                        0.04,
                        0.0,
                        "USD",
                        Instant.parse("2026-06-22T20:00:00Z"),
                        "TEST",
                        false
                )
        );
    }

    private PortfolioEodSnapshot previousEod() {
        return new PortfolioEodSnapshot(
                UUID.randomUUID(),
                PORTFOLIO_ID,
                VALUATION_DATE.minusDays(1),
                "USD",
                100.0,
                90.0,
                10.0,
                0,
                Instant.parse("2026-06-21T21:15:00Z"),
                "TEST",
                List.of(new PositionEodSnapshot(
                        EXISTING_POSITION,
                        "AAPL",
                        1.0,
                        100.0,
                        100.0,
                        90.0,
                        90.0,
                        10.0,
                        Instant.parse("2026-06-21T20:00:00Z"),
                        "TEST",
                        false
                ))
        );
    }
}
