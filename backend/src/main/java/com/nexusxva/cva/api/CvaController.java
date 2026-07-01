package com.nexusxva.cva.api;

import com.nexusxva.auth.application.FeaturePermissionCode;
import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.cva.application.CvaCalculationResult;
import com.nexusxva.cva.application.CvaCalculationService;
import com.nexusxva.cva.application.CvaNettingSetCalculationResult;
import com.nexusxva.cva.application.CvaNettingSetCalculationService;
import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.valuationruns.application.ValuationRunService;
import com.nexusxva.valuationruns.domain.ValuationRunType;
import com.nexusxva.xva.application.XvaReferenceDataService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk")
public class CvaController {

    private final CvaCalculationService cvaCalculationService;
    private final CvaNettingSetCalculationService cvaNettingSetCalculationService;
    private final UserAccessService userAccessService;
    private final ValuationRunService valuationRunService;
    private final XvaReferenceDataService xvaReferenceDataService;

    public CvaController(
            CvaCalculationService cvaCalculationService,
            CvaNettingSetCalculationService cvaNettingSetCalculationService,
            UserAccessService userAccessService,
            ValuationRunService valuationRunService,
            XvaReferenceDataService xvaReferenceDataService
    ) {
        this.cvaCalculationService = cvaCalculationService;
        this.cvaNettingSetCalculationService = cvaNettingSetCalculationService;
        this.userAccessService = userAccessService;
        this.valuationRunService = valuationRunService;
        this.xvaReferenceDataService = xvaReferenceDataService;
    }

    @PostMapping("/cva")
    public CvaCalculationResponse calculateCva(
            @Valid @RequestBody CvaCalculationRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requireFeature(servletRequest, FeaturePermissionCode.FO_RUN_CVA);
        userAccessService.requirePortfolioAccess(servletRequest, request.portfolioId());
        try {
            CvaCalculationResult result = cvaCalculationService.calculate(request.toCommand());
            CvaCalculationResponse response = CvaCalculationResponse.from(result);
            valuationRunService.recordSuccess(
                    currentSession(servletRequest),
                    request.portfolioId(),
                    ValuationRunType.CVA,
                    result.model(),
                    result.valuationDate(),
                    request,
                    response,
                    cvaSummary(response)
            );
            return response;
        } catch (RuntimeException exception) {
            if (!(exception instanceof ResourceNotFoundException)) {
                valuationRunService.recordFailure(
                        currentSession(servletRequest),
                        request.portfolioId(),
                        ValuationRunType.CVA,
                        "SIMPLIFIED_CVA_V1",
                        request.valuationDate(),
                        request,
                        exception
                );
            }
            throw exception;
        }
    }

    @PostMapping("/cva/netting-set")
    public CvaNettingSetCalculationResponse calculateNettingSetCva(
            @Valid @RequestBody CvaNettingSetCalculationRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requireFeature(servletRequest, FeaturePermissionCode.FO_RUN_CVA);
        xvaReferenceDataService.getNettingSet(request.nettingSetId())
                .portfolios()
                .forEach(portfolio -> userAccessService.requirePortfolioAccess(servletRequest, portfolio.portfolioId()));
        CvaNettingSetCalculationResult result = cvaNettingSetCalculationService.calculate(request.toCommand());
        return CvaNettingSetCalculationResponse.from(result);
    }

    private Map<String, Object> cvaSummary(CvaCalculationResponse response) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("cva", response.cva());
        summary.put("exposureModel", response.exposureModel());
        summary.put("creditMethod", response.creditMethod());
        summary.put("discountMethod", response.discountMethod());
        summary.put("points", response.points().size());
        return summary;
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
