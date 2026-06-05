package com.nexusxva.cva.application;

import com.nexusxva.cva.domain.CvaInput;
import com.nexusxva.cva.domain.CvaResult;
import com.nexusxva.cva.domain.SimplifiedCvaCalculator;
import com.nexusxva.exposure.application.ExposureSimulationCommand;
import com.nexusxva.exposure.application.ExposureSimulationResult;
import com.nexusxva.exposure.application.ExposureSimulationService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CvaCalculationService {

    private final ExposureSimulationService exposureSimulationService;
    private final SimplifiedCvaCalculator cvaCalculator;

    @Autowired
    public CvaCalculationService(ExposureSimulationService exposureSimulationService) {
        this(exposureSimulationService, new SimplifiedCvaCalculator());
    }

    CvaCalculationService(
            ExposureSimulationService exposureSimulationService,
            SimplifiedCvaCalculator cvaCalculator
    ) {
        this.exposureSimulationService = exposureSimulationService;
        this.cvaCalculator = cvaCalculator;
    }

    public CvaCalculationResult calculate(CvaCalculationCommand command) {
        ExposureSimulationResult exposure = exposureSimulationService.simulate(new ExposureSimulationCommand(
                command.portfolioId(),
                command.valuationDate(),
                command.horizonDays(),
                command.timeSteps(),
                command.paths(),
                command.seed(),
                command.pfeConfidenceLevel()
        ));
        CvaResult cva = cvaCalculator.calculate(new CvaInput(
                command.valuationDate(),
                exposure.points(),
                command.lossGivenDefault(),
                command.counterpartyHazardRate(),
                command.discountRate()
        ));

        return CvaCalculationResult.from(command, exposure, cva.cva(), cva.points());
    }
}
