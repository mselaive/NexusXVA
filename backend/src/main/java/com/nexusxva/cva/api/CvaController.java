package com.nexusxva.cva.api;

import com.nexusxva.auth.application.FeaturePermissionCode;
import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.cva.application.CvaCalculationResult;
import com.nexusxva.cva.application.CvaCalculationService;
import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.valuationruns.application.ValuationRunService;
import com.nexusxva.valuationruns.domain.ValuationRunType;

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
    private final UserAccessService userAccessService;
    private final ValuationRunService valuationRunService;

    public CvaController(
            CvaCalculationService cvaCalculationService,
            UserAccessService userAccessService,
            ValuationRunService valuationRunService
    ) {
        this.cvaCalculationService = cvaCalculationService;
        this.userAccessService = userAccessService;
        this.valuationRunService = valuationRunService;
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
