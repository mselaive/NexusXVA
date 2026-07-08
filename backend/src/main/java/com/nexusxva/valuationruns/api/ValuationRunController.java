package com.nexusxva.valuationruns.api;

import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.portfolio.application.PortfolioService;
import com.nexusxva.portfolio.api.PortfolioSummaryResponse;
import com.nexusxva.portfolio.domain.PortfolioSummary;
import com.nexusxva.valuationruns.application.ValuationRunSearchCriteria;
import com.nexusxva.valuationruns.application.ValuationRunService;
import com.nexusxva.valuationruns.domain.ValuationRun;
import com.nexusxva.valuationruns.domain.ValuationRunScopeType;
import com.nexusxva.valuationruns.domain.ValuationRunStatus;
import com.nexusxva.valuationruns.domain.ValuationRunType;
import com.nexusxva.xva.application.XvaReferenceDataService;

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
    private final XvaReferenceDataService xvaReferenceDataService;

    public ValuationRunController(
            ValuationRunService valuationRunService,
            PortfolioService portfolioService,
            UserAccessService userAccessService,
            XvaReferenceDataService xvaReferenceDataService
    ) {
        this.valuationRunService = valuationRunService;
        this.portfolioService = portfolioService;
        this.userAccessService = userAccessService;
        this.xvaReferenceDataService = xvaReferenceDataService;
    }

    @GetMapping
    public List<ValuationRunResponse> search(
            @RequestParam(required = false) ValuationRunType runType,
            @RequestParam(required = false) ValuationRunStatus status,
            @RequestParam(required = false) ValuationRunScopeType scopeType,
            @RequestParam(required = false) UUID scopeId,
            @RequestParam(required = false) UUID portfolioId,
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest request
    ) {
        if (portfolioId != null) {
            userAccessService.requirePortfolioAccess(request, portfolioId);
        }
        List<UUID> visiblePortfolioIds = portfolioId == null ? visiblePortfolioIds(request) : null;
        List<UUID> visibleNettingSetIds = portfolioId == null ? visibleNettingSetIds(request, visiblePortfolioIds) : null;
        return valuationRunService.search(new ValuationRunSearchCriteria(
                        runType,
                        status,
                        scopeType,
                        scopeId,
                        portfolioId,
                        visiblePortfolioIds,
                        visibleNettingSetIds,
                        limit
                ))
                .stream()
                .map(ValuationRunResponse::from)
                .toList();
    }

    @GetMapping("/portfolios")
    public List<PortfolioSummaryResponse> portfolios(HttpServletRequest request) {
        return userAccessService.filterVisiblePortfolios(request, portfolioService.listPortfolios())
                .stream()
                .map(PortfolioSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{runId}")
    public ValuationRunResponse get(@PathVariable UUID runId, HttpServletRequest request) {
        ValuationRun run = valuationRunService.get(runId);
        requireRunAccess(request, run);
        return ValuationRunResponse.from(run);
    }

    private void requireRunAccess(HttpServletRequest request, ValuationRun run) {
        if (run.scopeType() == ValuationRunScopeType.PORTFOLIO) {
            userAccessService.requirePortfolioAccess(request, run.portfolioId());
            return;
        }
        xvaReferenceDataService.getNettingSet(run.scopeId())
                .portfolios()
                .forEach(portfolio -> userAccessService.requirePortfolioAccess(request, portfolio.portfolioId()));
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

    private List<UUID> visibleNettingSetIds(HttpServletRequest request, List<UUID> visiblePortfolioIds) {
        if (currentSession(request) == null || visiblePortfolioIds == null) {
            return null;
        }
        return xvaReferenceDataService.listNettingSets(true)
                .stream()
                .filter(nettingSet -> nettingSet.portfolios()
                        .stream()
                        .allMatch(portfolio -> visiblePortfolioIds.contains(portfolio.portfolioId())))
                .map(nettingSet -> nettingSet.id())
                .toList();
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
