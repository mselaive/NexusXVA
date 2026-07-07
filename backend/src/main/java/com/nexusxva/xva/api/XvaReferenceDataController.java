package com.nexusxva.xva.api;

import com.nexusxva.audit.application.AuditEventCommand;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.shared.error.AccessDeniedException;
import com.nexusxva.xva.application.XvaReferenceDataService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/xva")
public class XvaReferenceDataController {

    private final XvaReferenceDataService service;
    private final AuditService auditService;

    public XvaReferenceDataController(XvaReferenceDataService service, AuditService auditService) {
        this.service = service;
        this.auditService = auditService;
    }

    @GetMapping("/counterparties")
    public List<CounterpartyResponse> listCounterparties(
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        return service.listCounterparties(includeInactive).stream().map(CounterpartyResponse::from).toList();
    }

    @PostMapping("/counterparties")
    public CounterpartyResponse createCounterparty(
            @Valid @RequestBody CreateCounterpartyRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        CounterpartyResponse response = CounterpartyResponse.from(service.createCounterparty(request.toCommand()));
        auditService.record(AuditEventCommand.of(
                "XVA_COUNTERPARTY_CREATED",
                "XVA",
                "CREATE_COUNTERPARTY",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "COUNTERPARTY",
                response.id(),
                "Counterparty created",
                auditService.metadata(java.util.Map.of("name", response.name(), "active", response.active()))
        ));
        return response;
    }

    @PatchMapping("/counterparties/{counterpartyId}")
    public CounterpartyResponse updateCounterparty(
            @PathVariable UUID counterpartyId,
            @Valid @RequestBody UpdateCounterpartyRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        CounterpartyResponse response = CounterpartyResponse.from(service.updateCounterparty(counterpartyId, request.toCommand()));
        auditService.record(AuditEventCommand.of(
                response.active() ? "XVA_COUNTERPARTY_UPDATED" : "XVA_COUNTERPARTY_DEACTIVATED",
                "XVA",
                response.active() ? "UPDATE_COUNTERPARTY" : "DEACTIVATE_COUNTERPARTY",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "COUNTERPARTY",
                response.id(),
                "Counterparty updated",
                auditService.metadata(java.util.Map.of("name", response.name(), "active", response.active()))
        ));
        return response;
    }

    @GetMapping("/netting-sets")
    public List<NettingSetResponse> listNettingSets(
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        return service.listNettingSets(includeInactive).stream().map(NettingSetResponse::from).toList();
    }

    @PostMapping("/netting-sets")
    public NettingSetResponse createNettingSet(
            @Valid @RequestBody CreateNettingSetRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        NettingSetResponse response = NettingSetResponse.from(service.createNettingSet(request.toCommand()));
        auditService.record(AuditEventCommand.of(
                "XVA_NETTING_SET_CREATED",
                "XVA",
                "CREATE_NETTING_SET",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "NETTING_SET",
                response.id(),
                "Netting set created",
                auditService.metadata(java.util.Map.of("name", response.name(), "counterpartyId", response.counterpartyId()))
        ));
        return response;
    }

    @PatchMapping("/netting-sets/{nettingSetId}")
    public NettingSetResponse updateNettingSet(
            @PathVariable UUID nettingSetId,
            @Valid @RequestBody UpdateNettingSetRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        NettingSetResponse response = NettingSetResponse.from(service.updateNettingSet(nettingSetId, request.toCommand()));
        auditService.record(AuditEventCommand.of(
                response.active() ? "XVA_NETTING_SET_UPDATED" : "XVA_NETTING_SET_DEACTIVATED",
                "XVA",
                response.active() ? "UPDATE_NETTING_SET" : "DEACTIVATE_NETTING_SET",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "NETTING_SET",
                response.id(),
                "Netting set updated",
                auditService.metadata(java.util.Map.of("name", response.name(), "active", response.active()))
        ));
        return response;
    }

    @PostMapping("/netting-sets/{nettingSetId}/portfolios")
    public NettingSetResponse assignPortfolio(
            @PathVariable UUID nettingSetId,
            @Valid @RequestBody AssignPortfolioToNettingSetRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        NettingSetResponse response = NettingSetResponse.from(service.assignPortfolio(nettingSetId, request.portfolioId()));
        auditService.record(AuditEventCommand.of(
                "XVA_NETTING_SET_PORTFOLIO_ASSIGNED",
                "XVA",
                "ASSIGN_PORTFOLIO_TO_NETTING_SET",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "NETTING_SET",
                nettingSetId,
                "Portfolio assigned to netting set",
                auditService.metadata(java.util.Map.of("portfolioId", request.portfolioId()))
        ));
        return response;
    }

    @DeleteMapping("/netting-sets/{nettingSetId}/portfolios/{portfolioId}")
    public NettingSetResponse removePortfolio(
            @PathVariable UUID nettingSetId,
            @PathVariable UUID portfolioId,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        NettingSetResponse response = NettingSetResponse.from(service.removePortfolio(nettingSetId, portfolioId));
        auditService.record(AuditEventCommand.of(
                "XVA_NETTING_SET_PORTFOLIO_REMOVED",
                "XVA",
                "REMOVE_PORTFOLIO_FROM_NETTING_SET",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "NETTING_SET",
                nettingSetId,
                "Portfolio removed from netting set",
                auditService.metadata(java.util.Map.of("portfolioId", portfolioId))
        ));
        return response;
    }

    @PatchMapping("/netting-sets/{nettingSetId}/collateral")
    public NettingSetResponse updateCollateral(
            @PathVariable UUID nettingSetId,
            @Valid @RequestBody UpdateNettingSetCollateralRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        NettingSetResponse response = NettingSetResponse.from(service.updateCollateral(nettingSetId, request.toCommand()));
        auditService.record(AuditEventCommand.of(
                "XVA_NETTING_SET_COLLATERAL_UPDATED",
                "XVA",
                "UPDATE_NETTING_SET_COLLATERAL",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "NETTING_SET",
                nettingSetId,
                "Netting set collateral updated",
                auditService.metadata(java.util.Map.of(
                        "collateralAmount", response.collateralAmount(),
                        "collateralCurrency", response.collateralCurrency()
                ))
        ));
        return response;
    }

    private void requireAdmin(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        if (value instanceof AuthSession session && !"ADMIN".equals(session.activeGroup())) {
            throw new AccessDeniedException("ADMIN group required");
        }
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
