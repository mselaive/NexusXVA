package com.nexusxva.executescript.api;

import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.executescript.application.ExecuteScriptService;
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
public class BackOfficeExecuteScriptController {

    private final ExecuteScriptService service;

    public BackOfficeExecuteScriptController(ExecuteScriptService service) {
        this.service = service;
    }

    @GetMapping("/api/back-office/execute-scripts/templates")
    public List<ExecuteScriptTemplateResponse> templates() {
        return service.listTemplates(false).stream().map(ExecuteScriptTemplateResponse::from).toList();
    }

    @PostMapping("/api/back-office/execute-scripts/runs")
    public ExecuteScriptRunResponse run(@RequestBody RunExecuteScriptRequest request, HttpServletRequest servletRequest) {
        return ExecuteScriptRunResponse.from(service.run(request.toCommand(), currentSession(servletRequest)));
    }

    @GetMapping("/api/back-office/execute-scripts/runs")
    public List<ExecuteScriptRunResponse> runs(@RequestParam(defaultValue = "20") int limit) {
        return service.recentRuns(limit).stream().map(ExecuteScriptRunResponse::from).toList();
    }

    @GetMapping("/api/back-office/execute-scripts/runs/{runId}")
    public ExecuteScriptRunResponse run(@PathVariable UUID runId) {
        return ExecuteScriptRunResponse.from(service.getRun(runId));
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
