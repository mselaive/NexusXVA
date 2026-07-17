package com.nexusxva.closechecklist.api;

import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.closechecklist.application.CloseChecklistService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/back-office/close-checklist/runs")
public class BackOfficeCloseChecklistController {

    private final CloseChecklistService service;

    public BackOfficeCloseChecklistController(CloseChecklistService service) {
        this.service = service;
    }

    @PostMapping
    public CloseChecklistRunResponse run(
            @RequestBody(required = false) RunCloseChecklistRequest request,
            HttpServletRequest servletRequest
    ) {
        return CloseChecklistRunResponse.from(service.runManual(
                request == null ? null : request.businessDate(),
                currentSession(servletRequest)
        ));
    }

    @GetMapping
    public List<CloseChecklistRunResponse> recent(@RequestParam(defaultValue = "20") int limit) {
        return service.recent(limit).stream()
                .map(CloseChecklistRunResponse::from)
                .toList();
    }

    @GetMapping("/{runId}")
    public CloseChecklistRunResponse get(@PathVariable UUID runId) {
        return CloseChecklistRunResponse.from(service.get(runId));
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
