package com.nexusxva.frontoffice.api;

import com.nexusxva.auth.application.FeaturePermissionCode;
import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.audit.application.AuditEventCommand;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.frontoffice.application.FrontOfficeWhatIfService;
import com.nexusxva.operationalcontrol.application.OperationalControlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/front-office/what-if")
public class FrontOfficeWhatIfController {

    private final FrontOfficeWhatIfService service;
    private final UserAccessService userAccessService;
    private final AuditService auditService;
    private final OperationalControlService operationalControlService;

    public FrontOfficeWhatIfController(
            FrontOfficeWhatIfService service,
            UserAccessService userAccessService,
            AuditService auditService,
            OperationalControlService operationalControlService
    ) {
        this.service = service;
        this.userAccessService = userAccessService;
        this.auditService = auditService;
        this.operationalControlService = operationalControlService;
    }

    @PostMapping("/european-option")
    public FrontOfficeWhatIfResponse run(
            @Valid @RequestBody FrontOfficeWhatIfRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requireFeature(servletRequest, FeaturePermissionCode.FO_RUN_WHAT_IF);
        userAccessService.requirePortfolioAccess(servletRequest, request.portfolioId());
        operationalControlService.ensureOpen("RUN_PRE_TRADE_ANALYSIS", currentSession(servletRequest), servletRequest);
        FrontOfficeWhatIfResponse response = FrontOfficeWhatIfResponse.from(
                service.run(request.portfolioId(), request.valuationDate(), request.trade().toCommand())
        );
        auditService.record(AuditEventCommand.of(
                "PRE_TRADE_ANALYSIS_RUN",
                "FRONT_OFFICE",
                "RUN_PRE_TRADE_ANALYSIS",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "PORTFOLIO",
                request.portfolioId(),
                "Pre-trade analysis requested",
                auditService.metadata(Map.of(
                        "valuationDate", request.valuationDate(),
                        "symbol", request.trade().underlyingSymbol(),
                        "optionType", request.trade().optionType(),
                        "quantity", request.trade().quantity()
                ))
        ));
        return response;
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
