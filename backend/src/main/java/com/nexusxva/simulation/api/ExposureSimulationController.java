package com.nexusxva.simulation.api;

import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.exposure.application.ExposureSimulationResult;
import com.nexusxva.exposure.application.ExposureSimulationService;
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
@RequestMapping("/api/simulations")
public class ExposureSimulationController {

    private final ExposureSimulationService exposureSimulationService;
    private final UserAccessService userAccessService;
    private final ValuationRunService valuationRunService;

    public ExposureSimulationController(
            ExposureSimulationService exposureSimulationService,
            UserAccessService userAccessService,
            ValuationRunService valuationRunService
    ) {
        this.exposureSimulationService = exposureSimulationService;
        this.userAccessService = userAccessService;
        this.valuationRunService = valuationRunService;
    }

    @PostMapping("/exposure")
    public ExposureSimulationResponse simulateExposure(
            @Valid @RequestBody ExposureSimulationRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requirePortfolioAccess(servletRequest, request.portfolioId());
        try {
            ExposureSimulationResult result = exposureSimulationService.simulate(request.toCommand());
            ExposureSimulationResponse response = ExposureSimulationResponse.from(result);
            valuationRunService.recordSuccess(
                    currentSession(servletRequest),
                    request.portfolioId(),
                    ValuationRunType.EXPOSURE,
                    result.model(),
                    result.valuationDate(),
                    request,
                    response,
                    exposureSummary(response)
            );
            return response;
        } catch (RuntimeException exception) {
            if (!(exception instanceof ResourceNotFoundException)) {
                valuationRunService.recordFailure(
                        currentSession(servletRequest),
                        request.portfolioId(),
                        ValuationRunType.EXPOSURE,
                        "GBM_BLACK_SCHOLES_EXPOSURE_V1",
                        request.valuationDate(),
                        request,
                        exception
                );
            }
            throw exception;
        }
    }

    private Map<String, Object> exposureSummary(ExposureSimulationResponse response) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("paths", response.paths());
        summary.put("timeSteps", response.timeSteps());
        summary.put("pfeConfidenceLevel", response.pfeConfidenceLevel());
        summary.put("baseCurrency", response.baseCurrency());
        summary.put("points", response.points().size());
        return summary;
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
