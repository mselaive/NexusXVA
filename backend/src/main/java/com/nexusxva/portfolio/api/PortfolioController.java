package com.nexusxva.portfolio.api;

import com.nexusxva.audit.application.AuditEventCommand;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.auth.application.FeaturePermissionCode;
import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.portfolio.application.PortfolioService;
import com.nexusxva.portfolio.domain.Portfolio;

import jakarta.validation.Valid;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final UserAccessService userAccessService;
    private final AuditService auditService;

    public PortfolioController(
            PortfolioService portfolioService,
            UserAccessService userAccessService,
            AuditService auditService
    ) {
        this.portfolioService = portfolioService;
        this.userAccessService = userAccessService;
        this.auditService = auditService;
    }

    @PostMapping
    public ResponseEntity<PortfolioResponse> createPortfolio(
            @Valid @RequestBody CreatePortfolioRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requireFeature(servletRequest, FeaturePermissionCode.FO_CREATE_PORTFOLIOS);
        Portfolio portfolio = portfolioService.createPortfolio(request.toCommand());
        userAccessService.grantCreatedPortfolioIfNeeded(servletRequest, portfolio.id());
        auditService.record(AuditEventCommand.of(
                "PORTFOLIO_CREATED",
                "PORTFOLIO",
                "CREATE_PORTFOLIO",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                201,
                "PORTFOLIO",
                portfolio.id(),
                "Portfolio created",
                auditService.metadata(java.util.Map.of("name", portfolio.name(), "baseCurrency", portfolio.baseCurrency()))
        ));

        return ResponseEntity
                .created(URI.create("/api/portfolios/" + portfolio.id()))
                .body(PortfolioResponse.from(portfolio));
    }

    @GetMapping
    public List<PortfolioSummaryResponse> listPortfolios(HttpServletRequest servletRequest) {
        return userAccessService.filterVisiblePortfolios(servletRequest, portfolioService.listPortfolios())
                .stream()
                .map(PortfolioSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{portfolioId}")
    public PortfolioResponse getPortfolio(@PathVariable UUID portfolioId, HttpServletRequest servletRequest) {
        userAccessService.requirePortfolioAccess(servletRequest, portfolioId);
        return PortfolioResponse.from(portfolioService.getPortfolio(portfolioId));
    }

    @PatchMapping("/{portfolioId}")
    public PortfolioResponse updatePortfolio(
            @PathVariable UUID portfolioId,
            @Valid @RequestBody UpdatePortfolioRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requirePortfolioAccess(servletRequest, portfolioId);
        Portfolio portfolio = portfolioService.updatePortfolio(portfolioId, request.toCommand());
        auditService.record(AuditEventCommand.of(
                "PORTFOLIO_UPDATED",
                "PORTFOLIO",
                "UPDATE_PORTFOLIO",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "PORTFOLIO",
                portfolio.id(),
                "Portfolio updated",
                auditService.metadata(java.util.Map.of("name", portfolio.name(), "baseCurrency", portfolio.baseCurrency()))
        ));
        return PortfolioResponse.from(portfolio);
    }

    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<Void> deletePortfolio(@PathVariable UUID portfolioId, HttpServletRequest servletRequest) {
        userAccessService.requirePortfolioAccess(servletRequest, portfolioId);
        AuthSession session = currentSession(servletRequest);
        portfolioService.archivePortfolio(
                portfolioId,
                session == null ? null : session.user().id(),
                "Archived through portfolio endpoint"
        );
        auditService.record(AuditEventCommand.of(
                "PORTFOLIO_ARCHIVED",
                "PORTFOLIO",
                "ARCHIVE_PORTFOLIO",
                AuditOutcome.SUCCESS,
                session,
                servletRequest,
                204,
                "PORTFOLIO",
                portfolioId,
                "Portfolio archived",
                auditService.metadata(java.util.Map.of("source", "portfolio endpoint"))
        ));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{portfolioId}/instruments/{positionId}")
    public EuropeanOptionPositionResponse getInstrument(
            @PathVariable UUID portfolioId,
            @PathVariable UUID positionId,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requirePortfolioAccess(servletRequest, portfolioId);
        return EuropeanOptionPositionResponse.from(
                portfolioService.getEuropeanOptionPosition(portfolioId, positionId)
        );
    }

    @GetMapping("/{portfolioId}/instruments")
    public List<EuropeanOptionPositionResponse> listInstruments(
            @PathVariable UUID portfolioId,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requirePortfolioAccess(servletRequest, portfolioId);
        return portfolioService.listEuropeanOptionPositions(portfolioId)
                .stream()
                .map(EuropeanOptionPositionResponse::from)
                .toList();
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }

}
