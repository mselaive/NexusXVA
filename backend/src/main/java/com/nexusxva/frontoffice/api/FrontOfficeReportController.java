package com.nexusxva.frontoffice.api;

import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.frontoffice.application.FrontOfficePnlReportService;
import com.nexusxva.frontoffice.application.FrontOfficePnlReportService.FrontOfficePnlReport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/front-office/reports")
public class FrontOfficeReportController {

    private final FrontOfficePnlReportService service;

    public FrontOfficeReportController(FrontOfficePnlReportService service) {
        this.service = service;
    }

    @GetMapping("/desk-pnl")
    public FrontOfficePnlReport deskPnl(HttpServletRequest request) {
        return service.report(currentSession(request), request);
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
