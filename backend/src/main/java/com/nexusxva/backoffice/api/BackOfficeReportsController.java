package com.nexusxva.backoffice.api;

import com.nexusxva.backoffice.application.BackOfficeOperationsReportService;
import com.nexusxva.backoffice.application.BackOfficeOperationsReportService.BackOfficeOperationsReport;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.reporting.application.ReportSnapshotService;
import com.nexusxva.reporting.domain.ReportSnapshotScopeType;
import com.nexusxva.reporting.domain.ReportSnapshotType;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/back-office/reports")
public class BackOfficeReportsController {

    private final BackOfficeOperationsReportService service;
    private final ReportSnapshotService reportSnapshotService;

    public BackOfficeReportsController(BackOfficeOperationsReportService service, ReportSnapshotService reportSnapshotService) {
        this.service = service;
        this.reportSnapshotService = reportSnapshotService;
    }

    @GetMapping("/operations")
    public BackOfficeOperationsReport operations(HttpServletRequest request) {
        BackOfficeOperationsReport report = service.report();
        reportSnapshotService.record(
                currentSession(request),
                ReportSnapshotType.BO_OPERATIONS,
                "BO Operations Reporting",
                report.businessDate(),
                ReportSnapshotScopeType.BACK_OFFICE,
                null,
                "Back Office",
                Map.of("source", "OPERATIONS_REPORTING"),
                report,
                Map.of(
                        "portfolios", report.portfolios(),
                        "pendingTradeBookings", report.pendingTradeBookings(),
                        "pendingLifecycleRequests", report.pendingLifecycleRequests(),
                        "portfoliosWithoutTodayClose", report.portfoliosWithoutTodayClose(),
                        "failedPnlPortfolios", report.failedPnlPortfolios()
                )
        );
        return report;
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
