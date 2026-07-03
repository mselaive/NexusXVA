package com.nexusxva.tradelifecycle.api;

import com.nexusxva.audit.application.AuditEventCommand;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.tradebooking.api.TradeBookingActorResolver;
import com.nexusxva.tradelifecycle.application.TradeLifecycleService;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequest;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequestStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/back-office/lifecycle-requests")
public class BackOfficeLifecycleController {

    private final TradeLifecycleService service;
    private final AuditService auditService;

    public BackOfficeLifecycleController(TradeLifecycleService service, AuditService auditService) {
        this.service = service;
        this.auditService = auditService;
    }

    @GetMapping
    public TradeLifecyclePageResponse search(
            @RequestParam(required = false) TradeLifecycleRequestStatus status,
            @RequestParam(required = false) UUID portfolioId,
            @RequestParam(required = false) String symbol,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return TradeLifecyclePageResponse.from(service.search(status, portfolioId, symbol, page, size));
    }

    @GetMapping("/{requestId}")
    public TradeLifecycleResponse get(@PathVariable UUID requestId) {
        return TradeLifecycleResponse.from(service.get(requestId));
    }

    @PostMapping("/{requestId}/approve")
    public TradeLifecycleResponse approve(@PathVariable UUID requestId, HttpServletRequest request) {
        TradeLifecycleRequest lifecycleRequest = service.approve(requestId, TradeBookingActorResolver.resolve(request));
        auditLifecycleReviewed(request, lifecycleRequest, "LIFECYCLE_APPROVED", "APPROVE_LIFECYCLE", "Lifecycle request approved");
        return TradeLifecycleResponse.from(lifecycleRequest);
    }

    @PostMapping("/{requestId}/reject")
    public TradeLifecycleResponse reject(
            @PathVariable UUID requestId,
            @Valid @RequestBody RejectLifecycleRequest body,
            HttpServletRequest request
    ) {
        TradeLifecycleRequest lifecycleRequest = service.reject(requestId, TradeBookingActorResolver.resolve(request), body.rejectionReason());
        auditLifecycleReviewed(request, lifecycleRequest, "LIFECYCLE_REJECTED", "REJECT_LIFECYCLE", "Lifecycle request rejected");
        return TradeLifecycleResponse.from(lifecycleRequest);
    }

    private void auditLifecycleReviewed(HttpServletRequest request, TradeLifecycleRequest lifecycleRequest, String eventType, String action, String message) {
        auditService.record(AuditEventCommand.of(
                eventType,
                "BACK_OFFICE",
                action,
                AuditOutcome.SUCCESS,
                currentSession(request),
                request,
                200,
                "TRADE_LIFECYCLE_REQUEST",
                lifecycleRequest.id(),
                message,
                auditService.metadata(java.util.Map.of(
                        "portfolioId", lifecycleRequest.portfolioId(),
                        "positionId", lifecycleRequest.positionId(),
                        "status", lifecycleRequest.status().name(),
                        "requestType", lifecycleRequest.requestType().name(),
                        "instrumentType", lifecycleRequest.instrumentType().name()
                ))
        ));
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
