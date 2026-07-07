package com.nexusxva.backoffice.application;

import com.nexusxva.eod.application.PortfolioEodService;
import com.nexusxva.eod.application.PortfolioDailyPnl;
import com.nexusxva.eod.application.PortfolioDailyPnlService;
import com.nexusxva.eod.domain.EodRunStatus;
import com.nexusxva.eod.domain.PortfolioEodSnapshot;
import com.nexusxva.portfolio.application.PortfolioService;
import com.nexusxva.portfolio.domain.PortfolioSummary;
import com.nexusxva.tradebooking.application.TradeBookingService;
import com.nexusxva.tradebooking.domain.TradeBookingRequest;
import com.nexusxva.tradebooking.domain.TradeBookingStatus;
import com.nexusxva.tradelifecycle.application.TradeLifecycleService;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequest;
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
    private final PortfolioDailyPnlService pnlService;
    private final TradeBookingService tradeBookingService;
    private final TradeLifecycleService lifecycleService;

    public BackOfficeOperationsReportService(
            PortfolioService portfolioService,
            PortfolioEodService eodService,
            PortfolioDailyPnlService pnlService,
            TradeBookingService tradeBookingService,
            TradeLifecycleService lifecycleService
    ) {
        this.portfolioService = portfolioService;
        this.eodService = eodService;
        this.pnlService = pnlService;
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
        var pendingTradeBookingPage = tradeBookingService.search(TradeBookingStatus.PENDING_VALIDATION, null, null, 0, 100);
        var pendingLifecyclePage = lifecycleService.search(TradeLifecycleRequestStatus.PENDING_VALIDATION, null, null, 0, 100);
        long pendingTradeBookings = pendingTradeBookingPage.getTotalElements();
        long pendingLifecycleRequests = pendingLifecyclePage.getTotalElements();

        return new BackOfficeOperationsReport(
                businessDate,
                Instant.now(),
                portfolios.size(),
                pendingTradeBookings,
                pendingLifecycleRequests,
                eodStatuses.stream().filter(BackOfficeEodPortfolioStatus::missingTodayClose).count(),
                eodStatuses.stream().mapToInt(BackOfficeEodPortfolioStatus::correctedRuns).sum(),
                oldestTradeBookingSubmittedAt(pendingTradeBookingPage.getContent()),
                oldestLifecycleSubmittedAt(pendingLifecyclePage.getContent()),
                eodStatuses.stream().filter(status -> "FAILED".equals(status.pnlStatus())).count(),
                eodStatuses.stream().filter(BackOfficeEodPortfolioStatus::latestCloseCorrected).count(),
                eodStatuses.stream().filter(BackOfficeEodPortfolioStatus::noCloseEver).count(),
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
        boolean latestCloseCorrected = latestByDate != null
                && (latestByDate.status() == EodRunStatus.VOIDED || latestByDate.status() == EodRunStatus.SUPERSEDED);
        PnlStatus pnlStatus = pnlStatus(portfolio, businessDate);

        return new BackOfficeEodPortfolioStatus(
                portfolio.id().toString(),
                portfolio.name(),
                portfolio.baseCurrency(),
                portfolio.positionCount(),
                latestEodDate,
                latestStatus,
                missingTodayClose,
                correctedRuns,
                latestCloseCorrected,
                latestByDate == null,
                pnlStatus.currentMarketValue(),
                pnlStatus.dailyPnl(),
                pnlStatus.sinceTradePnl(),
                pnlStatus.optionDailyPnl(),
                pnlStatus.cashEquityDailyPnl(),
                pnlStatus.positionsWithoutReference(),
                pnlStatus.positionsWithoutExecutionPrice(),
                pnlStatus.status(),
                pnlStatus.errorMessage()
        );
    }

    private PnlStatus pnlStatus(PortfolioSummary portfolio, LocalDate businessDate) {
        try {
            PortfolioDailyPnl pnl = pnlService.calculate(portfolio.id(), businessDate);
            return new PnlStatus(
                    pnl.currentMarketValue(),
                    pnl.dailyPnl(),
                    pnl.sinceTradePnl(),
                    pnl.optionDailyPnl(),
                    pnl.cashEquityDailyPnl(),
                    pnl.positionsWithoutReference(),
                    pnl.positionsWithoutExecutionPrice(),
                    "OK",
                    null
            );
        } catch (RuntimeException exception) {
            return new PnlStatus(null, null, null, null, null, null, null, "FAILED", sanitizedMessage(exception));
        }
    }

    private Instant oldestTradeBookingSubmittedAt(List<TradeBookingRequest> requests) {
        return requests.stream()
                .map(TradeBookingRequest::submittedAt)
                .min(Instant::compareTo)
                .orElse(null);
    }

    private Instant oldestLifecycleSubmittedAt(List<TradeLifecycleRequest> requests) {
        return requests.stream()
                .map(TradeLifecycleRequest::submittedAt)
                .min(Instant::compareTo)
                .orElse(null);
    }

    private String sanitizedMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "P&L unavailable" : message;
    }

    public record BackOfficeOperationsReport(
            LocalDate businessDate,
            Instant generatedAt,
            int portfolios,
            long pendingTradeBookings,
            long pendingLifecycleRequests,
            long portfoliosWithoutTodayClose,
            long correctedEodRuns,
            Instant oldestPendingTradeBookingSubmittedAt,
            Instant oldestPendingLifecycleSubmittedAt,
            long failedPnlPortfolios,
            long portfoliosWithCorrectedLatestClose,
            long portfoliosWithNoCloseEver,
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
            int correctedRuns,
            boolean latestCloseCorrected,
            boolean noCloseEver,
            Double currentMarketValue,
            Double dailyPnl,
            Double sinceTradePnl,
            Double optionDailyPnl,
            Double cashEquityDailyPnl,
            Integer positionsWithoutReference,
            Integer positionsWithoutExecutionPrice,
            String pnlStatus,
            String pnlErrorMessage
    ) {
    }

    private record PnlStatus(
            Double currentMarketValue,
            Double dailyPnl,
            Double sinceTradePnl,
            Double optionDailyPnl,
            Double cashEquityDailyPnl,
            Integer positionsWithoutReference,
            Integer positionsWithoutExecutionPrice,
            String status,
            String errorMessage
    ) {
    }
}
