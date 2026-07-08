package com.nexusxva.frontoffice.api;

import com.nexusxva.auth.application.FeaturePermissionCode;
import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.audit.application.AuditEventCommand;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.frontoffice.application.FrontOfficeDeltaHedgeService;
import com.nexusxva.operationalcontrol.application.OperationalControlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/front-office/delta-hedge")
public class FrontOfficeDeltaHedgeController {

    private final FrontOfficeDeltaHedgeService service;
    private final UserAccessService userAccessService;
    private final AuditService auditService;
    private final OperationalControlService operationalControlService;

    public FrontOfficeDeltaHedgeController(
            FrontOfficeDeltaHedgeService service,
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
    public DeltaHedgeAnalysisResponse run(
            @Valid @RequestBody DeltaHedgeAnalysisRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requireFeature(servletRequest, FeaturePermissionCode.FO_RUN_DELTA_HEDGE);
        userAccessService.requirePortfolioAccess(servletRequest, request.portfolioId());
        operationalControlService.ensureRiskRunOpen("RUN_DELTA_HEDGE", currentSession(servletRequest), servletRequest);
        DeltaHedgeAnalysisResponse response = DeltaHedgeAnalysisResponse.from(service.run(
                request.portfolioId(),
                request.valuationDate(),
                request.targetDeltaBySymbol()
        ));
        auditService.record(AuditEventCommand.of(
                "DELTA_HEDGE_RUN",
                "FRONT_OFFICE",
                "RUN_DELTA_HEDGE",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "PORTFOLIO",
                request.portfolioId(),
                "Delta hedge analysis requested",
                auditService.metadata(Map.of(
                        "valuationDate", request.valuationDate(),
                        "targetSymbols", request.targetDeltaBySymbol() == null ? 0 : request.targetDeltaBySymbol().size()
                ))
        ));
        return response;
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
