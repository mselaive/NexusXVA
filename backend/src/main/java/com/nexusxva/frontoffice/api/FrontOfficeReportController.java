package com.nexusxva.frontoffice.api;

import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.frontoffice.application.FrontOfficePnlReportService;
import com.nexusxva.frontoffice.application.FrontOfficePnlReportService.FrontOfficePnlReport;
import com.nexusxva.reporting.application.ReportSnapshotService;
import com.nexusxva.reporting.domain.ReportSnapshotScopeType;
import com.nexusxva.reporting.domain.ReportSnapshotType;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/front-office/reports")
public class FrontOfficeReportController {

    private final FrontOfficePnlReportService service;
    private final ReportSnapshotService reportSnapshotService;

    public FrontOfficeReportController(FrontOfficePnlReportService service, ReportSnapshotService reportSnapshotService) {
        this.service = service;
        this.reportSnapshotService = reportSnapshotService;
    }

    @GetMapping("/desk-pnl")
    public FrontOfficePnlReport deskPnl(HttpServletRequest request) {
        AuthSession session = currentSession(request);
        FrontOfficePnlReport report = service.report(session, request);
        reportSnapshotService.record(
                session,
                ReportSnapshotType.FO_PNL_SNAPSHOT,
                "FO P&L Snapshot",
                LocalDate.now(),
                ReportSnapshotScopeType.USER,
                session == null ? null : session.user().id(),
                session == null ? null : session.user().displayName(),
                Map.of("source", "FO_DESK"),
                report,
                Map.of(
                        "portfolioCount", report.portfolios().size(),
                        "dailyPnl", report.portfolios().stream()
                                .map(FrontOfficePnlReportService.FrontOfficePnlPortfolioRow::dailyPnl)
                                .filter(java.util.Objects::nonNull)
                                .mapToDouble(Double::doubleValue)
                                .sum(),
                        "sinceTradePnl", report.portfolios().stream()
                                .map(FrontOfficePnlReportService.FrontOfficePnlPortfolioRow::sinceTradePnl)
                                .filter(java.util.Objects::nonNull)
                                .mapToDouble(Double::doubleValue)
                                .sum()
                )
        );
        return report;
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
