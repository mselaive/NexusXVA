package com.nexusxva.admin.api;

import com.nexusxva.admin.application.AdminAccessService;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
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
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminAccessService service;

    public AdminController(AdminAccessService service) {
        this.service = service;
    }

    @GetMapping("/users")
    public AdminUserPageResponse users(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return service.users(query, page, size);
    }

    @GetMapping("/users/{userId}")
    public AdminUserAccessResponse user(@PathVariable UUID userId) {
        return service.user(userId);
    }

    @GetMapping("/portfolios")
    public java.util.List<AdminPortfolioSummaryResponse> portfolios() {
        return service.portfolios();
    }

    @PutMapping("/users/{userId}/groups")
    public AdminUserAccessResponse updateGroups(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserGroupsRequest request,
            HttpServletRequest servletRequest
    ) {
        return service.updateGroups(userId, request.groups(), currentSession(servletRequest));
    }

    @PutMapping("/users/{userId}/permissions")
    public AdminUserAccessResponse updatePermissions(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserPermissionsRequest request,
            HttpServletRequest servletRequest
    ) {
        return service.updatePermissions(userId, request.permissions(), currentSession(servletRequest));
    }

    @PutMapping("/users/{userId}/portfolio-access")
    public AdminUserAccessResponse updatePortfolioAccess(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserPortfolioAccessRequest request,
            HttpServletRequest servletRequest
    ) {
        return service.updatePortfolioAccess(userId, request.accessMode(), request.portfolioIds(), currentSession(servletRequest));
    }

    @GetMapping("/workflow-map")
    public AdminWorkflowMapResponse workflowMap(@RequestParam(required = false) UUID portfolioId) {
        return service.workflowMap(portfolioId);
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
