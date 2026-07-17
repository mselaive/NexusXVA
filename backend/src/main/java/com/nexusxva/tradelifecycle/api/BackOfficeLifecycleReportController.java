package com.nexusxva.tradelifecycle.api;

import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.reporting.application.ReportSnapshotService;
import com.nexusxva.reporting.domain.ReportSnapshotScopeType;
import com.nexusxva.reporting.domain.ReportSnapshotType;
import com.nexusxva.tradelifecycle.application.TradeLifecycleReport;
import com.nexusxva.tradelifecycle.application.TradeLifecycleService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/back-office/lifecycle-report")
public class BackOfficeLifecycleReportController {

    private final TradeLifecycleService service;
    private final ReportSnapshotService reportSnapshotService;

    public BackOfficeLifecycleReportController(TradeLifecycleService service, ReportSnapshotService reportSnapshotService) {
        this.service = service;
        this.reportSnapshotService = reportSnapshotService;
    }

    @GetMapping
    public TradeLifecycleReport report(HttpServletRequest request) {
        TradeLifecycleReport report = service.reportForBackOffice();
        reportSnapshotService.record(
                currentSession(request),
                ReportSnapshotType.BO_LIFECYCLE,
                "BO Lifecycle Reporting",
                LocalDate.now(),
                ReportSnapshotScopeType.BACK_OFFICE,
                null,
                "Back Office",
                Map.of("source", "LIFECYCLE_REPORTING"),
                report,
                Map.of(
                        "total", report.total(),
                        "pendingValidation", report.pendingValidation(),
                        "approved", report.approved(),
                        "rejected", report.rejected()
                )
        );
        return report;
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
