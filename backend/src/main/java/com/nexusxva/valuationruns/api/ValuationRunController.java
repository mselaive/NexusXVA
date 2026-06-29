package com.nexusxva.valuationruns.api;

import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.portfolio.application.PortfolioService;
import com.nexusxva.portfolio.domain.PortfolioSummary;
import com.nexusxva.valuationruns.application.ValuationRunSearchCriteria;
import com.nexusxva.valuationruns.application.ValuationRunService;
import com.nexusxva.valuationruns.domain.ValuationRun;
import com.nexusxva.valuationruns.domain.ValuationRunStatus;
import com.nexusxva.valuationruns.domain.ValuationRunType;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/valuation-runs")
public class ValuationRunController {

    private final ValuationRunService valuationRunService;
    private final PortfolioService portfolioService;
    private final UserAccessService userAccessService;

    public ValuationRunController(
            ValuationRunService valuationRunService,
            PortfolioService portfolioService,
            UserAccessService userAccessService
    ) {
        this.valuationRunService = valuationRunService;
        this.portfolioService = portfolioService;
        this.userAccessService = userAccessService;
    }

    @GetMapping
    public List<ValuationRunResponse> search(
            @RequestParam(required = false) ValuationRunType runType,
            @RequestParam(required = false) ValuationRunStatus status,
            @RequestParam(required = false) UUID portfolioId,
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest request
    ) {
        if (portfolioId != null) {
            userAccessService.requirePortfolioAccess(request, portfolioId);
        }
        List<UUID> visiblePortfolioIds = portfolioId == null ? visiblePortfolioIds(request) : null;
        return valuationRunService.search(new ValuationRunSearchCriteria(
                        runType,
                        status,
                        portfolioId,
                        visiblePortfolioIds,
                        limit
                ))
                .stream()
                .map(ValuationRunResponse::from)
                .toList();
    }

    @GetMapping("/{runId}")
    public ValuationRunResponse get(@PathVariable UUID runId, HttpServletRequest request) {
        ValuationRun run = valuationRunService.get(runId);
        userAccessService.requirePortfolioAccess(request, run.portfolioId());
        return ValuationRunResponse.from(run);
    }

    private List<UUID> visiblePortfolioIds(HttpServletRequest request) {
        if (currentSession(request) == null) {
            return null;
        }
        return userAccessService.filterVisiblePortfolios(request, portfolioService.listPortfolios())
                .stream()
                .map(PortfolioSummary::id)
                .toList();
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
