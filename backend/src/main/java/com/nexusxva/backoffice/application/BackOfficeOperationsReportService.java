package com.nexusxva.backoffice.application;

import com.nexusxva.eod.application.PortfolioEodService;
import com.nexusxva.eod.domain.EodRunStatus;
import com.nexusxva.eod.domain.PortfolioEodSnapshot;
import com.nexusxva.portfolio.application.PortfolioService;
import com.nexusxva.portfolio.domain.PortfolioSummary;
import com.nexusxva.tradebooking.application.TradeBookingService;
import com.nexusxva.tradebooking.domain.TradeBookingStatus;
import com.nexusxva.tradelifecycle.application.TradeLifecycleService;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequestStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BackOfficeOperationsReportService {

    private final PortfolioService portfolioService;
    private final PortfolioEodService eodService;
    private final TradeBookingService tradeBookingService;
    private final TradeLifecycleService lifecycleService;

    public BackOfficeOperationsReportService(
            PortfolioService portfolioService,
            PortfolioEodService eodService,
            TradeBookingService tradeBookingService,
            TradeLifecycleService lifecycleService
    ) {
        this.portfolioService = portfolioService;
        this.eodService = eodService;
        this.tradeBookingService = tradeBookingService;
        this.lifecycleService = lifecycleService;
    }

    @Transactional(readOnly = true)
    public BackOfficeOperationsReport report() {
        LocalDate businessDate = LocalDate.now(ZoneOffset.UTC);
        List<PortfolioSummary> portfolios = portfolioService.listPortfolios();
        List<BackOfficeEodPortfolioStatus> eodStatuses = portfolios.stream()
                .map(portfolio -> eodStatus(portfolio, businessDate))
                .toList();
        long pendingTradeBookings = tradeBookingService
                .search(TradeBookingStatus.PENDING_VALIDATION, null, null, 0, 100)
                .getTotalElements();
        long pendingLifecycleRequests = lifecycleService
                .search(TradeLifecycleRequestStatus.PENDING_VALIDATION, null, null, 0, 100)
                .getTotalElements();

        return new BackOfficeOperationsReport(
                businessDate,
                Instant.now(),
                portfolios.size(),
                pendingTradeBookings,
                pendingLifecycleRequests,
                eodStatuses.stream().filter(BackOfficeEodPortfolioStatus::missingTodayClose).count(),
                eodStatuses.stream().mapToInt(BackOfficeEodPortfolioStatus::correctedRuns).sum(),
                eodStatuses
        );
    }

    private BackOfficeEodPortfolioStatus eodStatus(PortfolioSummary portfolio, LocalDate businessDate) {
        Optional<PortfolioEodSnapshot> latest = eodService.latest(portfolio.id());
        List<PortfolioEodSnapshot> history = eodService.history(portfolio.id(), 60);
        int correctedRuns = (int) history.stream()
                .filter(snapshot -> snapshot.status() == EodRunStatus.VOIDED || snapshot.status() == EodRunStatus.SUPERSEDED)
                .count();
        PortfolioEodSnapshot latestByDate = history.stream()
                .max(Comparator.comparing(PortfolioEodSnapshot::businessDate)
                        .thenComparing(PortfolioEodSnapshot::capturedAt))
                .orElse(latest.orElse(null));
        LocalDate latestEodDate = latestByDate == null ? null : latestByDate.businessDate();
        String latestStatus = latestByDate == null ? "MISSING" : latestByDate.status().name();
        boolean missingTodayClose = latest
                .map(snapshot -> snapshot.businessDate().isBefore(businessDate))
                .orElse(true);

        return new BackOfficeEodPortfolioStatus(
                portfolio.id().toString(),
                portfolio.name(),
                portfolio.baseCurrency(),
                portfolio.positionCount(),
                latestEodDate,
                latestStatus,
                missingTodayClose,
                correctedRuns
        );
    }

    public record BackOfficeOperationsReport(
            LocalDate businessDate,
            Instant generatedAt,
            int portfolios,
            long pendingTradeBookings,
            long pendingLifecycleRequests,
            long portfoliosWithoutTodayClose,
            long correctedEodRuns,
            List<BackOfficeEodPortfolioStatus> eodPortfolios
    ) {
    }

    public record BackOfficeEodPortfolioStatus(
            String portfolioId,
            String portfolioName,
            String baseCurrency,
            long positionCount,
            LocalDate latestEodDate,
            String latestEodStatus,
            boolean missingTodayClose,
            int correctedRuns
    ) {
    }
}
