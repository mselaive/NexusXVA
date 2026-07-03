package com.nexusxva.tradinglimits.api;

import com.nexusxva.audit.application.AuditEventCommand;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.tradinglimits.application.TradingLimitService;
import com.nexusxva.tradinglimits.domain.TradingLimitSnapshot;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/back-office/trading-limits/users")
public class BackOfficeTradingLimitController {

    private final TradingLimitService service;
    private final AuditService auditService;

    public BackOfficeTradingLimitController(TradingLimitService service, AuditService auditService) {
        this.service = service;
        this.auditService = auditService;
    }

    @GetMapping
    public TradingLimitUserPageResponse search(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return TradingLimitUserPageResponse.from(service.search(query, page, size));
    }

    @GetMapping("/{userId}")
    public TradingLimitSnapshotResponse get(@PathVariable UUID userId) {
        return TradingLimitSnapshotResponse.from(service.get(userId));
    }

    @PutMapping("/{userId}")
    public TradingLimitSnapshotResponse update(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateTradingLimitRequest request,
            HttpServletRequest servletRequest
    ) {
        TradingLimitSnapshot snapshot = service.update(
                userId,
                request.toCommand(),
                TradingLimitActorResolver.resolve(servletRequest)
        );
        auditService.record(AuditEventCommand.of(
                snapshot.policy() != null && snapshot.policy().active() ? "TRADING_LIMIT_UPDATED" : "TRADING_LIMIT_DISABLED",
                "BACK_OFFICE",
                        "UPDATE_TRADING_LIMIT",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "USER",
                userId,
                "Trading limit policy changed",
                auditService.metadata(java.util.Map.of(
                        "status", snapshot.status(),
                        "notionalCurrency", snapshot.policy() == null ? "USD" : snapshot.policy().notionalCurrency()
                ))
        ));
        return TradingLimitSnapshotResponse.from(snapshot);
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
