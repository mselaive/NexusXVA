package com.nexusxva.simulation.api;

import com.nexusxva.exposure.application.ExposureSimulationService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulations")
public class ExposureSimulationController {

    private final ExposureSimulationService exposureSimulationService;

    public ExposureSimulationController(ExposureSimulationService exposureSimulationService) {
        this.exposureSimulationService = exposureSimulationService;
    }

    @PostMapping("/exposure")
    public ExposureSimulationResponse simulateExposure(@Valid @RequestBody ExposureSimulationRequest request) {
        return ExposureSimulationResponse.from(exposureSimulationService.simulate(request.toCommand()));
    }
}
