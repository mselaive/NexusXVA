package com.nexusxva.frontoffice.api;

import com.nexusxva.auth.application.FeaturePermissionCode;
import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.audit.application.AuditEventCommand;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.frontoffice.application.FrontOfficeStressTestService;
import com.nexusxva.operationalcontrol.application.OperationalControlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/front-office/stress-tests")
public class FrontOfficeStressTestController {

    private final FrontOfficeStressTestService service;
    private final UserAccessService userAccessService;
    private final AuditService auditService;
    private final OperationalControlService operationalControlService;

    public FrontOfficeStressTestController(
            FrontOfficeStressTestService service,
            UserAccessService userAccessService,
            AuditService auditService,
            OperationalControlService operationalControlService
    ) {
        this.service = service;
        this.userAccessService = userAccessService;
        this.auditService = auditService;
        this.operationalControlService = operationalControlService;
    }

    @PostMapping("/european-options")
    public FrontOfficeStressTestResponse run(
            @Valid @RequestBody FrontOfficeStressTestRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requireFeature(servletRequest, FeaturePermissionCode.FO_RUN_STRESS_TEST);
        userAccessService.requirePortfolioAccess(servletRequest, request.portfolioId());
        operationalControlService.ensureRiskRunOpen("RUN_STRESS_TEST", currentSession(servletRequest), servletRequest);
        FrontOfficeStressTestResponse response = FrontOfficeStressTestResponse.from(service.run(
                request.portfolioId(),
                request.valuationDate(),
                request.hypotheticalTradeCommand(),
                request.scenarioCommands()
        ));
        auditService.record(AuditEventCommand.of(
                "STRESS_TEST_RUN",
                "FRONT_OFFICE",
                "RUN_STRESS_TEST",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "PORTFOLIO",
                request.portfolioId(),
                "Stress test requested",
                auditService.metadata(Map.of(
                        "valuationDate", request.valuationDate(),
                        "scenarioCount", request.scenarios().size(),
                        "includesHypotheticalTrade", request.hypotheticalTrade() != null
                ))
        ));
        return response;
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
