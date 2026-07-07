package com.nexusxva.operationalcontrol.api;

import com.nexusxva.audit.application.AuditEventCommand;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.operationalcontrol.application.OperationalControlService;
import com.nexusxva.operationalcontrol.domain.OperationalControlSettings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OperationalControlController {

    private final OperationalControlService service;
    private final AuditService auditService;

    public OperationalControlController(OperationalControlService service, AuditService auditService) {
        this.service = service;
        this.auditService = auditService;
    }

    @GetMapping("/api/admin/operational-control")
    public OperationalControlResponse getSettings() {
        return OperationalControlResponse.from(service.settings());
    }

    @PutMapping("/api/admin/operational-control")
    public OperationalControlResponse updateSettings(
            @Valid @RequestBody UpdateOperationalControlRequest request,
            HttpServletRequest servletRequest
    ) {
        AuthSession session = currentSession(servletRequest);
        OperationalControlSettings updated = service.update(
                request.toSettings(),
                session == null ? null : session.user().id()
        );
        auditService.record(new AuditEventCommand(
                "OPERATIONAL_CONTROL_UPDATED",
                "ADMIN",
                "UPDATE_OPERATIONAL_CONTROL",
                AuditOutcome.SUCCESS,
                session,
                servletRequest,
                200,
                "OPERATIONAL_CONTROL",
                "1",
                "Operational control settings updated",
                auditService.metadata(java.util.Map.of(
                        "timezone", updated.timezone().getId(),
                        "businessDays", updated.businessDays().stream().map(Enum::name).toList(),
                        "tradingOpenTime", updated.tradingOpenTime(),
                        "tradingCloseTime", updated.tradingCloseTime(),
                        "eodEnabled", updated.eodEnabled(),
                        "eodRunTime", updated.eodRunTime()
                ))
        ));
        return OperationalControlResponse.from(updated);
    }

    @GetMapping("/api/operational-control/status")
    public OperationalControlStatusResponse status() {
        return OperationalControlStatusResponse.from(service.status());
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
