package com.nexusxva.frontoffice.application;

import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.eod.application.PortfolioDailyPnl;
import com.nexusxva.eod.application.PortfolioDailyPnlService;
import com.nexusxva.eod.application.PortfolioEodService;
import com.nexusxva.portfolio.application.PortfolioService;
import com.nexusxva.portfolio.domain.PortfolioSummary;
import com.nexusxva.tradebooking.application.TradeBookingService;
import com.nexusxva.tradebooking.domain.BookingActor;
import com.nexusxva.tradebooking.domain.TradeBookingRequest;
import com.nexusxva.tradebooking.domain.TradeBookingStatus;
import com.nexusxva.tradelifecycle.application.TradeLifecycleService;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequest;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequestStatus;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FrontOfficePnlReportService {

    private final PortfolioService portfolioService;
    private final UserAccessService userAccessService;
    private final TradeBookingService tradeBookingService;
    private final TradeLifecycleService lifecycleService;
    private final PortfolioDailyPnlService pnlService;
    private final PortfolioEodService eodService;

    public FrontOfficePnlReportService(
            PortfolioService portfolioService,
            UserAccessService userAccessService,
            TradeBookingService tradeBookingService,
            TradeLifecycleService lifecycleService,
            PortfolioDailyPnlService pnlService,
            PortfolioEodService eodService
    ) {
        this.portfolioService = portfolioService;
        this.userAccessService = userAccessService;
        this.tradeBookingService = tradeBookingService;
        this.lifecycleService = lifecycleService;
        this.pnlService = pnlService;
        this.eodService = eodService;
    }

    @Transactional(readOnly = true)
    public FrontOfficePnlReport report(AuthSession session, HttpServletRequest request) {
        LocalDate valuationDate = LocalDate.now(ZoneOffset.UTC);
        BookingActor actor = actor(session);
        List<TradeBookingRequest> bookings = tradeBookingService.mine(actor);
        List<TradeLifecycleRequest> lifecycleRequests = lifecycleService.mine(actor);
        List<PortfolioSummary> portfolios = userAccessService.filterVisiblePortfolios(
                request,
                portfolioService.listPortfolios()
        );

        return new FrontOfficePnlReport(
                valuationDate,
                Instant.now(),
                portfolios.stream()
                        .map(portfolio -> row(portfolio, valuationDate, bookings, lifecycleRequests))
                        .toList()
        );
    }

    private FrontOfficePnlPortfolioRow row(
            PortfolioSummary portfolio,
            LocalDate valuationDate,
            List<TradeBookingRequest> bookings,
            List<TradeLifecycleRequest> lifecycleRequests
    ) {
        try {
            PortfolioDailyPnl pnl = pnlService.calculate(portfolio.id(), valuationDate);
            return FrontOfficePnlPortfolioRow.success(
                    portfolio,
                    eodService.latest(portfolio.id()).map(close -> close.businessDate()).orElse(null),
                    pnl,
                    countBookings(bookings, portfolio.id(), TradeBookingStatus.PENDING_VALIDATION),
                    countBookings(bookings, portfolio.id(), TradeBookingStatus.REJECTED),
                    countLifecycle(lifecycleRequests, portfolio.id(), TradeLifecycleRequestStatus.PENDING_VALIDATION),
                    countLifecycle(lifecycleRequests, portfolio.id(), TradeLifecycleRequestStatus.REJECTED)
            );
        } catch (RuntimeException exception) {
            return FrontOfficePnlPortfolioRow.failed(
                    portfolio,
                    eodService.latest(portfolio.id()).map(close -> close.businessDate()).orElse(null),
                    countBookings(bookings, portfolio.id(), TradeBookingStatus.PENDING_VALIDATION),
                    countBookings(bookings, portfolio.id(), TradeBookingStatus.REJECTED),
                    countLifecycle(lifecycleRequests, portfolio.id(), TradeLifecycleRequestStatus.PENDING_VALIDATION),
                    countLifecycle(lifecycleRequests, portfolio.id(), TradeLifecycleRequestStatus.REJECTED),
                    sanitizedMessage(exception)
            );
        }
    }

    private long countBookings(List<TradeBookingRequest> bookings, UUID portfolioId, TradeBookingStatus status) {
        return bookings.stream()
                .filter(booking -> portfolioId.equals(booking.portfolioId()))
                .filter(booking -> booking.status() == status)
                .count();
    }

    private long countLifecycle(List<TradeLifecycleRequest> requests, UUID portfolioId, TradeLifecycleRequestStatus status) {
        return requests.stream()
                .filter(lifecycle -> portfolioId.equals(lifecycle.portfolioId()))
                .filter(lifecycle -> lifecycle.status() == status)
                .count();
    }

    private BookingActor actor(AuthSession session) {
        if (session == null) {
            return BookingActor.system();
        }
        return new BookingActor(session.user().id(), session.user().username(), session.user().displayName());
    }

    private String sanitizedMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "P&L snapshot unavailable" : message;
    }

    public record FrontOfficePnlReport(
            LocalDate valuationDate,
            Instant generatedAt,
            List<FrontOfficePnlPortfolioRow> portfolios
    ) {
    }

    public record FrontOfficePnlPortfolioRow(
            UUID portfolioId,
            String portfolioName,
            String baseCurrency,
            long positionCount,
            LocalDate latestEodDate,
            String status,
            Double currentMarketValue,
            Double dailyPnl,
            Double sinceTradePnl,
            Double optionDailyPnl,
            Double cashEquityDailyPnl,
            Double optionSinceTradePnl,
            Double cashEquitySinceTradePnl,
            long pendingBookings,
            long rejectedBookings,
            long pendingLifecycleRequests,
            long rejectedLifecycleRequests,
            String errorMessage
    ) {
        static FrontOfficePnlPortfolioRow success(
                PortfolioSummary portfolio,
                LocalDate latestEodDate,
                PortfolioDailyPnl pnl,
                long pendingBookings,
                long rejectedBookings,
                long pendingLifecycleRequests,
                long rejectedLifecycleRequests
        ) {
            return new FrontOfficePnlPortfolioRow(
                    portfolio.id(),
                    portfolio.name(),
                    portfolio.baseCurrency(),
                    portfolio.positionCount(),
                    latestEodDate,
                    "OK",
                    pnl.currentMarketValue(),
                    pnl.dailyPnl(),
                    pnl.sinceTradePnl(),
                    pnl.optionDailyPnl(),
                    pnl.cashEquityDailyPnl(),
                    pnl.optionSinceTradePnl(),
                    pnl.cashEquitySinceTradePnl(),
                    pendingBookings,
                    rejectedBookings,
                    pendingLifecycleRequests,
                    rejectedLifecycleRequests,
                    null
            );
        }

        static FrontOfficePnlPortfolioRow failed(
                PortfolioSummary portfolio,
                LocalDate latestEodDate,
                long pendingBookings,
                long rejectedBookings,
                long pendingLifecycleRequests,
                long rejectedLifecycleRequests,
                String errorMessage
        ) {
            return new FrontOfficePnlPortfolioRow(
                    portfolio.id(),
                    portfolio.name(),
                    portfolio.baseCurrency(),
                    portfolio.positionCount(),
                    latestEodDate,
                    "FAILED",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    pendingBookings,
                    rejectedBookings,
                    pendingLifecycleRequests,
                    rejectedLifecycleRequests,
                    errorMessage
            );
        }
    }
}
