package com.nexusxva.simulation.api;

import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.exposure.application.ExposureSimulationService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulations")
public class ExposureSimulationController {

    private final ExposureSimulationService exposureSimulationService;
    private final UserAccessService userAccessService;

    public ExposureSimulationController(
            ExposureSimulationService exposureSimulationService,
            UserAccessService userAccessService
    ) {
        this.exposureSimulationService = exposureSimulationService;
        this.userAccessService = userAccessService;
    }

    @PostMapping("/exposure")
    public ExposureSimulationResponse simulateExposure(
            @Valid @RequestBody ExposureSimulationRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requirePortfolioAccess(servletRequest, request.portfolioId());
        return ExposureSimulationResponse.from(exposureSimulationService.simulate(request.toCommand()));
    }
}
