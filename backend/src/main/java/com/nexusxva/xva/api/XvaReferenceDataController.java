package com.nexusxva.xva.api;

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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/xva")
public class XvaReferenceDataController {

    private final XvaReferenceDataService service;

    public XvaReferenceDataController(XvaReferenceDataService service) {
        this.service = service;
    }

    @GetMapping("/counterparties")
    public List<CounterpartyResponse> listCounterparties() {
        return service.listCounterparties().stream().map(CounterpartyResponse::from).toList();
    }

    @PostMapping("/counterparties")
    public CounterpartyResponse createCounterparty(
            @Valid @RequestBody CreateCounterpartyRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        return CounterpartyResponse.from(service.createCounterparty(request.toCommand()));
    }

    @GetMapping("/netting-sets")
    public List<NettingSetResponse> listNettingSets() {
        return service.listNettingSets().stream().map(NettingSetResponse::from).toList();
    }

    @PostMapping("/netting-sets")
    public NettingSetResponse createNettingSet(
            @Valid @RequestBody CreateNettingSetRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        return NettingSetResponse.from(service.createNettingSet(request.toCommand()));
    }

    @PostMapping("/netting-sets/{nettingSetId}/portfolios")
    public NettingSetResponse assignPortfolio(
            @PathVariable UUID nettingSetId,
            @Valid @RequestBody AssignPortfolioToNettingSetRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        return NettingSetResponse.from(service.assignPortfolio(nettingSetId, request.portfolioId()));
    }

    @DeleteMapping("/netting-sets/{nettingSetId}/portfolios/{portfolioId}")
    public NettingSetResponse removePortfolio(
            @PathVariable UUID nettingSetId,
            @PathVariable UUID portfolioId,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        return NettingSetResponse.from(service.removePortfolio(nettingSetId, portfolioId));
    }

    @PatchMapping("/netting-sets/{nettingSetId}/collateral")
    public NettingSetResponse updateCollateral(
            @PathVariable UUID nettingSetId,
            @Valid @RequestBody UpdateNettingSetCollateralRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        return NettingSetResponse.from(service.updateCollateral(nettingSetId, request.toCommand()));
    }

    private void requireAdmin(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        if (value instanceof AuthSession session && !"ADMIN".equals(session.activeGroup())) {
            throw new AccessDeniedException("ADMIN group required");
        }
    }
}
