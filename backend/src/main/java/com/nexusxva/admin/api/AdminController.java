package com.nexusxva.admin.api;

import com.nexusxva.audit.application.AuditEventCommand;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.admin.application.AdminAccessService;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.portfolio.application.PortfolioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private final PortfolioService portfolioService;
    private final AuditService auditService;

    public AdminController(AdminAccessService service, PortfolioService portfolioService, AuditService auditService) {
        this.service = service;
        this.portfolioService = portfolioService;
        this.auditService = auditService;
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

    @DeleteMapping("/portfolios/{portfolioId}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable UUID portfolioId, HttpServletRequest servletRequest) {
        AuthSession session = currentSession(servletRequest);
        portfolioService.archivePortfolio(
                portfolioId,
                session == null ? null : session.user().id(),
                "Archived by ADMIN"
        );
        auditService.record(AuditEventCommand.of(
                "ADMIN_PORTFOLIO_ARCHIVED",
                "ADMIN",
                "ARCHIVE_PORTFOLIO",
                AuditOutcome.SUCCESS,
                session,
                servletRequest,
                204,
                "PORTFOLIO",
                portfolioId,
                "Portfolio archived by ADMIN",
                null
        ));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/{userId}/groups")
    public AdminUserAccessResponse updateGroups(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserGroupsRequest request,
            HttpServletRequest servletRequest
    ) {
        AdminUserAccessResponse response = service.updateGroups(userId, request.groups(), currentSession(servletRequest));
        auditService.record(AuditEventCommand.of(
                "ADMIN_USER_GROUPS_CHANGED",
                "ADMIN",
                "UPDATE_USER_GROUPS",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "USER",
                userId,
                "User groups changed",
                auditService.metadata(java.util.Map.of("groups", request.groups()))
        ));
        return response;
    }

    @PutMapping("/users/{userId}/permissions")
    public AdminUserAccessResponse updatePermissions(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserPermissionsRequest request,
            HttpServletRequest servletRequest
    ) {
        AdminUserAccessResponse response = service.updatePermissions(userId, request.permissions(), currentSession(servletRequest));
        auditService.record(AuditEventCommand.of(
                "ADMIN_USER_PERMISSIONS_CHANGED",
                "ADMIN",
                "UPDATE_USER_PERMISSIONS",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "USER",
                userId,
                "User feature permissions changed",
                auditService.metadata(java.util.Map.of("permissions", request.permissions()))
        ));
        return response;
    }

    @PutMapping("/users/{userId}/portfolio-access")
    public AdminUserAccessResponse updatePortfolioAccess(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserPortfolioAccessRequest request,
            HttpServletRequest servletRequest
    ) {
        AdminUserAccessResponse response = service.updatePortfolioAccess(userId, request.accessMode(), request.portfolioIds(), currentSession(servletRequest));
        auditService.record(AuditEventCommand.of(
                "ADMIN_PORTFOLIO_ACCESS_CHANGED",
                "ADMIN",
                "UPDATE_PORTFOLIO_ACCESS",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "USER",
                userId,
                "User portfolio access changed",
                auditService.metadata(java.util.Map.of(
                        "accessMode", request.accessMode(),
                        "portfolioIds", request.portfolioIds()
                ))
        ));
        return response;
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
