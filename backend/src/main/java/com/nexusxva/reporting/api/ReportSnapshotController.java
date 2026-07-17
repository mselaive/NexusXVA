package com.nexusxva.reporting.api;

import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.reporting.application.ReportSnapshotSearchCriteria;
import com.nexusxva.reporting.application.ReportSnapshotService;
import com.nexusxva.reporting.domain.ReportSnapshot;
import com.nexusxva.reporting.domain.ReportSnapshotType;
import com.nexusxva.shared.error.AccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report-snapshots")
public class ReportSnapshotController {

    private final ReportSnapshotService service;

    public ReportSnapshotController(ReportSnapshotService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReportSnapshotResponse> search(
            @RequestParam(required = false) ReportSnapshotType reportType,
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest request
    ) {
        AuthSession session = currentSession(request);
        UUID requestedByUserId = "FO".equals(activeGroup(session)) ? session.user().id() : null;
        String activeGroupCode = "BO".equals(activeGroup(session)) ? "BO" : null;
        if ("ADMIN".equals(activeGroup(session))) {
            activeGroupCode = null;
        }
        return service.search(new ReportSnapshotSearchCriteria(reportType, requestedByUserId, activeGroupCode, limit))
                .stream()
                .map(ReportSnapshotResponse::from)
                .toList();
    }

    @GetMapping("/{snapshotId}")
    public ReportSnapshotResponse get(@PathVariable UUID snapshotId, HttpServletRequest request) {
        ReportSnapshot snapshot = service.get(snapshotId);
        AuthSession session = currentSession(request);
        if ("FO".equals(activeGroup(session)) && !session.user().id().equals(snapshot.requestedByUserId())) {
            throw new AccessDeniedException("Report snapshot is not visible");
        }
        if ("BO".equals(activeGroup(session)) && !"BO".equals(snapshot.activeGroupCode())) {
            throw new AccessDeniedException("Report snapshot is not visible");
        }
        return ReportSnapshotResponse.from(snapshot);
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }

    private String activeGroup(AuthSession session) {
        return session == null ? null : session.activeGroup();
    }
}
