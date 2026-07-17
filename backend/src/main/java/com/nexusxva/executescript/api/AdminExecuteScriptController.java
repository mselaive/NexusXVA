package com.nexusxva.executescript.api;

import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.executescript.application.ExecuteScriptService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/execute-scripts/templates")
public class AdminExecuteScriptController {

    private final ExecuteScriptService service;

    public AdminExecuteScriptController(ExecuteScriptService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExecuteScriptTemplateResponse> templates(@RequestParam(defaultValue = "true") boolean includeInactive) {
        return service.listTemplates(includeInactive).stream().map(ExecuteScriptTemplateResponse::from).toList();
    }

    @PostMapping
    public ExecuteScriptTemplateResponse create(@RequestBody SaveExecuteScriptTemplateRequest request, HttpServletRequest servletRequest) {
        return ExecuteScriptTemplateResponse.from(service.createTemplate(request.toCommand(), currentSession(servletRequest)));
    }

    @PatchMapping("/{templateId}")
    public ExecuteScriptTemplateResponse update(@PathVariable UUID templateId, @RequestBody SaveExecuteScriptTemplateRequest request, HttpServletRequest servletRequest) {
        return ExecuteScriptTemplateResponse.from(service.updateTemplate(templateId, request.toCommand(), currentSession(servletRequest)));
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
