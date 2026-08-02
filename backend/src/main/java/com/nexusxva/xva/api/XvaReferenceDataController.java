package com.nexusxva.xva.api;

import com.nexusxva.audit.application.AuditEventCommand;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.shared.error.AccessDeniedException;
import com.nexusxva.xva.application.CvaCurveMasterDataService;
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
    private final CvaCurveMasterDataService curveService;
    private final AuditService auditService;

    public XvaReferenceDataController(
            XvaReferenceDataService service,
            CvaCurveMasterDataService curveService,
            AuditService auditService
    ) {
        this.service = service;
        this.curveService = curveService;
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

    @GetMapping("/credit-curves")
    public List<CreditCurveResponse> listCreditCurves(
            @RequestParam(required = false) UUID counterpartyId,
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        return curveService.listCreditCurves(counterpartyId, includeInactive)
                .stream()
                .map(CreditCurveResponse::from)
                .toList();
    }

    @PostMapping("/credit-curves")
    public CreditCurveResponse createCreditCurve(
            @Valid @RequestBody SaveCreditCurveRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        CreditCurveResponse response = CreditCurveResponse.from(curveService.createCreditCurve(request.toCommand()));
        auditService.record(AuditEventCommand.of(
                "XVA_CREDIT_CURVE_CREATED",
                "XVA",
                "CREATE_CREDIT_CURVE",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "CREDIT_CURVE",
                response.id(),
                "Credit curve created",
                auditService.metadata(java.util.Map.of(
                        "counterpartyId", response.counterpartyId(),
                        "name", response.name(),
                        "points", response.points().size()
                ))
        ));
        return response;
    }

    @PostMapping("/credit-curves/imports")
    public CreditCurveResponse importCreditCurve(
            @Valid @RequestBody SaveCreditCurveRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        CreditCurveResponse response = CreditCurveResponse.from(curveService.importCreditCurve(request.toCommand()));
        auditService.record(AuditEventCommand.of(
                "XVA_CREDIT_CURVE_IMPORTED", "XVA", "IMPORT_CREDIT_CURVE", AuditOutcome.SUCCESS,
                currentSession(servletRequest), servletRequest, 200, "CREDIT_CURVE", response.id(),
                "Credit curve imported as draft",
                auditService.metadata(java.util.Map.of("name", response.name(), "points", response.points().size(), "source", "IMPORT"))
        ));
        return response;
    }

    @PostMapping("/credit-curves/imports/market-data")
    public CreditCurveResponse importMarketCreditCurve(
            @Valid @RequestBody ImportMarketCreditCurveRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        CreditCurveResponse response = CreditCurveResponse.from(curveService.importMarketCreditCurve(
                request.counterpartyId(), request.valuationDate(), request.recoveryRate(), request.name(), request.allowStale()));
        auditService.record(AuditEventCommand.of(
                "XVA_CREDIT_CURVE_MARKET_DATA_IMPORTED", "XVA", "IMPORT_MARKET_DATA_CREDIT_CURVE", AuditOutcome.SUCCESS,
                currentSession(servletRequest), servletRequest, 200, "CREDIT_CURVE", response.id(),
                "Market-data credit curve imported as draft",
                auditService.metadata(java.util.Map.of(
                        "name", response.name(), "counterpartyId", response.counterpartyId(),
                        "rating", response.sourceCreditRating(), "ratingBucket", response.sourceRatingBucket(),
                        "sourceSeriesId", response.sourceSeriesId(), "observationDate", response.sourceObservationDate(),
                        "stale", response.sourceStale(), "points", response.points().size()))
        ));
        return response;
    }

    @PatchMapping("/credit-curves/{curveId}")
    public CreditCurveResponse updateCreditCurve(
            @PathVariable UUID curveId,
            @Valid @RequestBody SaveCreditCurveRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        CreditCurveResponse response = CreditCurveResponse.from(curveService.updateCreditCurve(curveId, request.toCommand()));
        auditService.record(AuditEventCommand.of(
                "XVA_CREDIT_CURVE_UPDATED",
                "XVA",
                "UPDATE_CREDIT_CURVE",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "CREDIT_CURVE",
                response.id(),
                "Credit curve updated",
                auditService.metadata(java.util.Map.of(
                        "counterpartyId", response.counterpartyId(),
                        "name", response.name(),
                        "active", response.active(),
                        "points", response.points().size()
                ))
        ));
        return response;
    }

    @PostMapping("/credit-curves/{curveId}/approve")
    public CreditCurveResponse approveCreditCurve(@PathVariable UUID curveId, HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        CreditCurveResponse response = CreditCurveResponse.from(curveService.approveCreditCurve(curveId, userId(servletRequest)));
        auditService.record(AuditEventCommand.of(
                "XVA_CREDIT_CURVE_APPROVED",
                "XVA",
                "APPROVE_CREDIT_CURVE",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "CREDIT_CURVE",
                response.id(),
                "Credit curve approved",
                auditService.metadata(java.util.Map.of("name", response.name(), "version", response.version()))
        ));
        return response;
    }

    @PostMapping("/credit-curves/{curveId}/reject")
    public CreditCurveResponse rejectCreditCurve(
            @PathVariable UUID curveId,
            @Valid @RequestBody RejectCurveRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        CreditCurveResponse response = CreditCurveResponse.from(curveService.rejectCreditCurve(curveId, request.reason()));
        auditService.record(AuditEventCommand.of(
                "XVA_CREDIT_CURVE_REJECTED",
                "XVA",
                "REJECT_CREDIT_CURVE",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "CREDIT_CURVE",
                response.id(),
                "Credit curve rejected",
                auditService.metadata(java.util.Map.of("name", response.name(), "version", response.version()))
        ));
        return response;
    }

    @GetMapping("/discount-curves")
    public List<DiscountCurveResponse> listDiscountCurves(
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        return curveService.listDiscountCurves(currency, includeInactive)
                .stream()
                .map(DiscountCurveResponse::from)
                .toList();
    }

    @PostMapping("/discount-curves")
    public DiscountCurveResponse createDiscountCurve(
            @Valid @RequestBody SaveDiscountCurveRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        DiscountCurveResponse response = DiscountCurveResponse.from(curveService.createDiscountCurve(request.toCommand()));
        auditService.record(AuditEventCommand.of(
                "XVA_DISCOUNT_CURVE_CREATED",
                "XVA",
                "CREATE_DISCOUNT_CURVE",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "DISCOUNT_CURVE",
                response.id(),
                "Discount curve created",
                auditService.metadata(java.util.Map.of(
                        "currency", response.currency(),
                        "name", response.name(),
                        "points", response.points().size()
                ))
        ));
        return response;
    }

    @PostMapping("/discount-curves/imports")
    public DiscountCurveResponse importDiscountCurve(
            @Valid @RequestBody SaveDiscountCurveRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        DiscountCurveResponse response = DiscountCurveResponse.from(curveService.importDiscountCurve(request.toCommand()));
        auditService.record(AuditEventCommand.of(
                "XVA_DISCOUNT_CURVE_IMPORTED", "XVA", "IMPORT_DISCOUNT_CURVE", AuditOutcome.SUCCESS,
                currentSession(servletRequest), servletRequest, 200, "DISCOUNT_CURVE", response.id(),
                "Discount curve imported as draft",
                auditService.metadata(java.util.Map.of("name", response.name(), "points", response.points().size(), "source", "IMPORT"))
        ));
        return response;
    }

    @PostMapping("/discount-curves/imports/market-data")
    public DiscountCurveResponse importMarketDiscountCurve(
            @Valid @RequestBody ImportMarketDiscountCurveRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        DiscountCurveResponse response = DiscountCurveResponse.from(curveService.importMarketDiscountCurve(
                request.currency(), request.valuationDate(), request.name(), request.allowStale()));
        auditService.record(AuditEventCommand.of(
                "XVA_DISCOUNT_CURVE_MARKET_DATA_IMPORTED", "XVA", "IMPORT_MARKET_DATA_DISCOUNT_CURVE", AuditOutcome.SUCCESS,
                currentSession(servletRequest), servletRequest, 200, "DISCOUNT_CURVE", response.id(),
                "Market-data discount curve imported as draft",
                auditService.metadata(java.util.Map.of(
                        "name", response.name(), "currency", response.currency(), "points", response.points().size(),
                        "source", "MARKET_DATA", "valuationDate", request.valuationDate(), "allowStale", request.allowStale()))
        ));
        return response;
    }

    @PatchMapping("/discount-curves/{curveId}")
    public DiscountCurveResponse updateDiscountCurve(
            @PathVariable UUID curveId,
            @Valid @RequestBody SaveDiscountCurveRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        DiscountCurveResponse response = DiscountCurveResponse.from(curveService.updateDiscountCurve(curveId, request.toCommand()));
        auditService.record(AuditEventCommand.of(
                "XVA_DISCOUNT_CURVE_UPDATED",
                "XVA",
                "UPDATE_DISCOUNT_CURVE",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "DISCOUNT_CURVE",
                response.id(),
                "Discount curve updated",
                auditService.metadata(java.util.Map.of(
                        "currency", response.currency(),
                        "name", response.name(),
                        "active", response.active(),
                        "points", response.points().size()
                ))
        ));
        return response;
    }

    @PostMapping("/discount-curves/{curveId}/approve")
    public DiscountCurveResponse approveDiscountCurve(@PathVariable UUID curveId, HttpServletRequest servletRequest) {
        requireAdmin(servletRequest);
        DiscountCurveResponse response = DiscountCurveResponse.from(curveService.approveDiscountCurve(curveId, userId(servletRequest)));
        auditService.record(AuditEventCommand.of(
                "XVA_DISCOUNT_CURVE_APPROVED",
                "XVA",
                "APPROVE_DISCOUNT_CURVE",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "DISCOUNT_CURVE",
                response.id(),
                "Discount curve approved",
                auditService.metadata(java.util.Map.of("name", response.name(), "version", response.version()))
        ));
        return response;
    }

    @PostMapping("/discount-curves/{curveId}/reject")
    public DiscountCurveResponse rejectDiscountCurve(
            @PathVariable UUID curveId,
            @Valid @RequestBody RejectCurveRequest request,
            HttpServletRequest servletRequest
    ) {
        requireAdmin(servletRequest);
        DiscountCurveResponse response = DiscountCurveResponse.from(curveService.rejectDiscountCurve(curveId, request.reason()));
        auditService.record(AuditEventCommand.of(
                "XVA_DISCOUNT_CURVE_REJECTED",
                "XVA",
                "REJECT_DISCOUNT_CURVE",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "DISCOUNT_CURVE",
                response.id(),
                "Discount curve rejected",
                auditService.metadata(java.util.Map.of("name", response.name(), "version", response.version()))
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

    private UUID userId(HttpServletRequest request) {
        AuthSession session = currentSession(request);
        return session == null ? null : session.user().id();
    }
}
