package com.nexusxva.riskcockpit.api;

import com.nexusxva.audit.application.AuditEventCommand;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.operationalcontrol.application.OperationalControlService;
import com.nexusxva.portfolio.api.PortfolioSummaryResponse;
import com.nexusxva.portfolio.application.PortfolioService;
import com.nexusxva.riskcockpit.application.RiskPackService;
import com.nexusxva.riskcockpit.domain.RiskPackRun;
import com.nexusxva.shared.error.AccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/risk-cockpit")
public class RiskCockpitController {
    private final RiskPackService riskPacks; private final PortfolioService portfolios; private final UserAccessService access;
    private final OperationalControlService operationalControl; private final AuditService audit;
    public RiskCockpitController(RiskPackService riskPacks, PortfolioService portfolios, UserAccessService access,
                                 OperationalControlService operationalControl, AuditService audit) {
        this.riskPacks=riskPacks; this.portfolios=portfolios; this.access=access; this.operationalControl=operationalControl; this.audit=audit;
    }
    @GetMapping("/portfolios")
    public List<PortfolioSummaryResponse> portfolios(HttpServletRequest request) {
        var values=portfolios.listPortfolios();
        if (isFo(request)) values=access.filterVisiblePortfolios(request,values);
        return values.stream().map(PortfolioSummaryResponse::from).toList();
    }
    @GetMapping("/portfolios/{portfolioId}/latest")
    public ResponseEntity<RiskPackRun> latest(@PathVariable UUID portfolioId,HttpServletRequest request) {
        requireReadAccess(portfolioId,request);
        return riskPacks.latest(portfolioId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }
    @GetMapping("/portfolios/{portfolioId}/runs")
    public List<RiskPackRun> runs(@PathVariable UUID portfolioId,@RequestParam(defaultValue="20") int limit,HttpServletRequest request) {
        requireReadAccess(portfolioId,request); return riskPacks.recent(portfolioId,limit);
    }
    @PostMapping("/portfolios/{portfolioId}/runs")
    public ResponseEntity<Map<String,Object>> run(@PathVariable UUID portfolioId,@RequestBody(required=false) StartRiskPackRequest body,HttpServletRequest request) {
        AuthSession session=session(request);
        if (session!=null && !"FO".equals(session.activeGroup())) throw new AccessDeniedException("Only Front Office can run a Risk Pack");
        access.requirePortfolioAccess(request,portfolioId);
        operationalControl.ensureRiskRunOpen("RUN_RISK_PACK",session,request);
        RiskPackRun run=riskPacks.start(portfolioId,body==null?null:body.valuationDate(),session);
        audit.record(AuditEventCommand.of("RISK_PACK_QUEUED","MARKET_RISK","RUN_RISK_PACK", AuditOutcome.SUCCESS,
                session,request,202,"PORTFOLIO",portfolioId,"Risk Pack queued",
                audit.metadata(Map.of("runId",run.id(),"valuationDate",run.valuationDate()))));
        return ResponseEntity.accepted().body(Map.of("runId",run.id(),"status",run.status()));
    }
    @GetMapping("/runs/{runId}")
    public RiskPackRun get(@PathVariable UUID runId,HttpServletRequest request) {
        RiskPackRun run=riskPacks.get(runId); requireReadAccess(run.portfolioId(),request); return run;
    }
    private void requireReadAccess(UUID portfolioId,HttpServletRequest request){ if(isFo(request)) access.requirePortfolioAccess(request,portfolioId); }
    private boolean isFo(HttpServletRequest request){ AuthSession value=session(request); return value!=null&&"FO".equals(value.activeGroup()); }
    private AuthSession session(HttpServletRequest request){ Object value=request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE); return value instanceof AuthSession auth?auth:null; }
}
