package com.nexusxva.tradelifecycle.api;

import com.nexusxva.audit.application.AuditEventCommand;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.auth.application.FeaturePermissionCode;
import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.portfolio.domain.CashEquityPosition;
import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.tradebooking.api.TradeBookingActorResolver;
import com.nexusxva.tradelifecycle.application.TradeLifecycleService;
import com.nexusxva.tradelifecycle.application.TradeLifecycleReport;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/front-office/lifecycle")
public class FrontOfficeLifecycleController {

    private final TradeLifecycleService service;
    private final UserAccessService userAccessService;
    private final AuditService auditService;

    public FrontOfficeLifecycleController(
            TradeLifecycleService service,
            UserAccessService userAccessService,
            AuditService auditService
    ) {
        this.service = service;
        this.userAccessService = userAccessService;
        this.auditService = auditService;
    }

    @PostMapping("/positions/{positionId}/amend")
    public TradeLifecycleResponse amend(
            @PathVariable UUID positionId,
            @Valid @RequestBody AmendPositionRequest body,
            HttpServletRequest request
    ) {
        userAccessService.requireFeature(request, FeaturePermissionCode.FO_REQUEST_LIFECYCLE);
        EuropeanOptionPosition position = service.position(positionId);
        userAccessService.requirePortfolioAccess(request, position.portfolioId());
        TradeLifecycleRequest lifecycleRequest = service.submitAmend(positionId, body.toCommand(), TradeBookingActorResolver.resolve(request));
        auditLifecycleSubmitted(request, lifecycleRequest, "LIFECYCLE_AMEND_REQUESTED", "REQUEST_AMEND");
        return TradeLifecycleResponse.from(lifecycleRequest);
    }

    @PostMapping("/positions/{positionId}/cancel")
    public TradeLifecycleResponse cancel(@PathVariable UUID positionId, HttpServletRequest request) {
        userAccessService.requireFeature(request, FeaturePermissionCode.FO_REQUEST_LIFECYCLE);
        EuropeanOptionPosition position = service.position(positionId);
        userAccessService.requirePortfolioAccess(request, position.portfolioId());
        TradeLifecycleRequest lifecycleRequest = service.submitCancel(positionId, TradeBookingActorResolver.resolve(request));
        auditLifecycleSubmitted(request, lifecycleRequest, "LIFECYCLE_CANCEL_REQUESTED", "REQUEST_CANCEL");
        return TradeLifecycleResponse.from(lifecycleRequest);
    }

    @PostMapping("/cash-equities/{positionId}/amend")
    public TradeLifecycleResponse amendCashEquity(
            @PathVariable UUID positionId,
            @Valid @RequestBody AmendCashEquityPositionRequest body,
            HttpServletRequest request
    ) {
        userAccessService.requireFeature(request, FeaturePermissionCode.FO_REQUEST_LIFECYCLE);
        CashEquityPosition position = service.cashEquityPosition(positionId);
        userAccessService.requirePortfolioAccess(request, position.portfolioId());
        TradeLifecycleRequest lifecycleRequest = service.submitCashEquityAmend(positionId, body.toCommand(), TradeBookingActorResolver.resolve(request));
        auditLifecycleSubmitted(request, lifecycleRequest, "LIFECYCLE_AMEND_REQUESTED", "REQUEST_AMEND");
        return TradeLifecycleResponse.from(lifecycleRequest);
    }

    @PostMapping("/cash-equities/{positionId}/cancel")
    public TradeLifecycleResponse cancelCashEquity(@PathVariable UUID positionId, HttpServletRequest request) {
        userAccessService.requireFeature(request, FeaturePermissionCode.FO_REQUEST_LIFECYCLE);
        CashEquityPosition position = service.cashEquityPosition(positionId);
        userAccessService.requirePortfolioAccess(request, position.portfolioId());
        TradeLifecycleRequest lifecycleRequest = service.submitCashEquityCancel(positionId, TradeBookingActorResolver.resolve(request));
        auditLifecycleSubmitted(request, lifecycleRequest, "LIFECYCLE_CANCEL_REQUESTED", "REQUEST_CANCEL");
        return TradeLifecycleResponse.from(lifecycleRequest);
    }

    @GetMapping("/mine")
    public List<TradeLifecycleResponse> mine(HttpServletRequest request) {
        return service.mine(TradeBookingActorResolver.resolve(request))
                .stream()
                .map(TradeLifecycleResponse::from)
                .toList();
    }

    @GetMapping("/report")
    public TradeLifecycleReport report(HttpServletRequest request) {
        return service.reportForFrontOffice(TradeBookingActorResolver.resolve(request));
    }

    private void auditLifecycleSubmitted(HttpServletRequest request, TradeLifecycleRequest lifecycleRequest, String eventType, String action) {
        auditService.record(AuditEventCommand.of(
                eventType,
                "FRONT_OFFICE",
                action,
                AuditOutcome.SUCCESS,
                currentSession(request),
                request,
                200,
                "TRADE_LIFECYCLE_REQUEST",
                lifecycleRequest.id(),
                "Lifecycle request submitted for BO validation",
                auditService.metadata(java.util.Map.of(
                        "portfolioId", lifecycleRequest.portfolioId(),
                        "positionId", lifecycleRequest.positionId(),
                        "instrumentType", lifecycleRequest.instrumentType().name(),
                        "requestType", lifecycleRequest.requestType().name(),
                        "symbol", lifecycleRequest.originalUnderlyingSymbol()
                ))
        ));
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
