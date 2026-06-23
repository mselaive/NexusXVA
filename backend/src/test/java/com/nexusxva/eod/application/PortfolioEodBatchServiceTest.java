package com.nexusxva.eod.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexusxva.portfolio.application.PortfolioStore;
import com.nexusxva.portfolio.domain.PortfolioSummary;
import com.nexusxva.shared.error.ConflictException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioEodBatchServiceTest {

    private static final LocalDate BUSINESS_DATE = LocalDate.parse("2026-06-22");

    @Mock
    private PortfolioStore portfolioStore;

    @Mock
    private PortfolioEodService eodService;

    @Test
    void processesEveryPortfolioAndReportsIndependentResults() {
        PortfolioSummary captured = portfolio("Captured Book");
        PortfolioSummary skipped = portfolio("Already Closed Book");
        PortfolioSummary failed = portfolio("Failed Book");
        when(portfolioStore.listPortfolioSummaries()).thenReturn(List.of(captured, skipped, failed));
        doAnswer(invocation -> {
            UUID portfolioId = invocation.getArgument(0);
            if (portfolioId.equals(skipped.id())) {
                throw new ConflictException("EOD snapshot already exists");
            }
            if (portfolioId.equals(failed.id())) {
                throw new IllegalArgumentException("Market data is stale");
            }
            return null;
        }).when(eodService).capture(any(UUID.class), eq(BUSINESS_DATE), eq("TEST"));

        EodBatchResult result = new PortfolioEodBatchService(portfolioStore, eodService, "America/New_York")
                .captureAll(BUSINESS_DATE, "TEST");

        assertThat(result.portfolios()).extracting(EodBatchPortfolioResult::status)
                .containsExactly("CAPTURED", "SKIPPED", "FAILED");
        assertThat(result.totalPortfolios()).isEqualTo(3);
        assertThat(result.captured()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        verify(eodService).capture(captured.id(), BUSINESS_DATE, "TEST");
        verify(eodService).capture(skipped.id(), BUSINESS_DATE, "TEST");
        verify(eodService).capture(failed.id(), BUSINESS_DATE, "TEST");
    }

    private PortfolioSummary portfolio(String name) {
        Instant now = Instant.parse("2026-06-22T20:00:00Z");
        return new PortfolioSummary(UUID.randomUUID(), name, null, "USD", now, now, 0);
    }
}
