package com.nexusxva.frontoffice.api;

import com.nexusxva.frontoffice.application.FrontOfficeStressTestService.StressScenario;
import com.nexusxva.portfolio.application.AddEuropeanOptionPositionCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FrontOfficeStressTestRequest(
        @NotNull UUID portfolioId,
        @NotNull LocalDate valuationDate,
        @Valid FrontOfficeWhatIfRequest.HypotheticalEuropeanOptionTradeRequest hypotheticalTrade,
        @NotEmpty @Size(max = 20) List<@Valid StressScenarioRequest> scenarios
) {

    AddEuropeanOptionPositionCommand hypotheticalTradeCommand() {
        return hypotheticalTrade == null ? null : hypotheticalTrade.toCommand();
    }

    List<StressScenario> scenarioCommands() {
        return scenarios.stream()
                .map(StressScenarioRequest::toCommand)
                .toList();
    }

    public record StressScenarioRequest(
            @NotBlank @Size(max = 80) String name,
            double spotShockPercent,
            double volatilityShockBps,
            double riskFreeRateShockBps,
            double dividendYieldShockBps
    ) {

        StressScenario toCommand() {
            return new StressScenario(
                    name.trim(),
                    spotShockPercent,
                    volatilityShockBps,
                    riskFreeRateShockBps,
                    dividendYieldShockBps
            );
        }
    }
}
