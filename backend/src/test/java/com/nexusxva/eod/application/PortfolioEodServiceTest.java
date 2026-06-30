package com.nexusxva.eod.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexusxva.eod.domain.EodRunStatus;
import com.nexusxva.eod.domain.PortfolioEodSnapshot;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingResult;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingService;
import com.nexusxva.portfolio.application.PortfolioGreeks;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioEodServiceTest {

    private static final UUID RUN_ID = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID PORTFOLIO_ID = UUID.fromString("20000000-0000-4000-8000-000000000001");
    private static final UUID USER_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final LocalDate BUSINESS_DATE = LocalDate.parse("2026-06-22");

    @Mock
    private PortfolioEodStore store;

    @Mock
    private PortfolioBlackScholesPricingService pricingService;

    @Test
    void voidRunRequiresActiveSnapshotAndStoresReason() {
        PortfolioEodSnapshot active = activeRun();
        PortfolioEodSnapshot voided = new PortfolioEodSnapshot(
                active.id(),
                active.portfolioId(),
                active.businessDate(),
                active.baseCurrency(),
                active.totalMarketValue(),
                active.totalTradeValue(),
                active.totalUnrealizedPnl(),
                active.positionsWithoutExecutionPrice(),
                active.capturedAt(),
                active.source(),
                EodRunStatus.VOIDED,
                Instant.parse("2026-06-22T21:05:00Z"),
                USER_ID,
                "Wrong market data",
                null,
                List.of()
        );
        when(store.find(RUN_ID)).thenReturn(Optional.of(active), Optional.of(voided));

        PortfolioEodSnapshot result = service().voidRun(RUN_ID, USER_ID, " Wrong market data ");

        assertThat(result.status()).isEqualTo(EodRunStatus.VOIDED);
        verify(store).voidRun(RUN_ID, USER_ID, "Wrong market data");
    }

    @Test
    void recaptureSupersedesOriginalAndCreatesLinkedActiveRun() {
        when(store.find(RUN_ID)).thenReturn(Optional.of(activeRun()));
        when(store.exists(PORTFOLIO_ID, BUSINESS_DATE)).thenReturn(false);
        when(pricingService.price(PORTFOLIO_ID, BUSINESS_DATE)).thenReturn(pricingResult());
        when(store.create(any(PortfolioEodSnapshot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PortfolioEodSnapshot result = service().recapture(RUN_ID, USER_ID, "Correct closing inputs");

        assertThat(result.status()).isEqualTo(EodRunStatus.ACTIVE);
        assertThat(result.correctionOfRunId()).isEqualTo(RUN_ID);
        assertThat(result.source()).isEqualTo("MANUAL_BO_CORRECTION");
        verify(store).supersedeRun(RUN_ID, USER_ID, "Correct closing inputs");
        ArgumentCaptor<PortfolioEodSnapshot> captor = ArgumentCaptor.forClass(PortfolioEodSnapshot.class);
        verify(store).create(captor.capture());
        assertThat(captor.getValue().correctionOfRunId()).isEqualTo(RUN_ID);
    }

    @Test
    void rejectsCorrectionForNonActiveRun() {
        PortfolioEodSnapshot voided = new PortfolioEodSnapshot(
                RUN_ID,
                PORTFOLIO_ID,
                BUSINESS_DATE,
                "USD",
                0.0,
                0.0,
                0.0,
                0,
                Instant.parse("2026-06-22T21:00:00Z"),
                "TEST",
                EodRunStatus.VOIDED,
                Instant.parse("2026-06-22T21:05:00Z"),
                USER_ID,
                "Wrong market data",
                null,
                List.of()
        );
        when(store.find(RUN_ID)).thenReturn(Optional.of(voided));

        assertThatThrownBy(() -> service().recapture(RUN_ID, USER_ID, "Try again"))
                .hasMessage("Only ACTIVE EOD snapshots can be corrected");
    }

    private PortfolioEodService service() {
        return new PortfolioEodService(store, pricingService, false);
    }

    private PortfolioEodSnapshot activeRun() {
        return new PortfolioEodSnapshot(
                RUN_ID,
                PORTFOLIO_ID,
                BUSINESS_DATE,
                "USD",
                100.0,
                80.0,
                20.0,
                0,
                Instant.parse("2026-06-22T21:00:00Z"),
                "MANUAL_BO",
                List.of()
        );
    }

    private PortfolioBlackScholesPricingResult pricingResult() {
        return new PortfolioBlackScholesPricingResult(
                PORTFOLIO_ID,
                BUSINESS_DATE,
                "BLACK_SCHOLES",
                "USD",
                100.0,
                80.0,
                20.0,
                0,
                PortfolioGreeks.zero(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
