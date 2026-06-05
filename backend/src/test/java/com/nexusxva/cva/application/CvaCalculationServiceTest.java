package com.nexusxva.cva.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexusxva.exposure.application.ExposureSimulationCommand;
import com.nexusxva.exposure.application.ExposureSimulationResult;
import com.nexusxva.exposure.application.ExposureSimulationService;
import com.nexusxva.exposure.domain.ExposurePoint;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class CvaCalculationServiceTest {

    private static final UUID PORTFOLIO_ID = UUID.randomUUID();
    private static final LocalDate VALUATION_DATE = LocalDate.parse("2026-06-05");

    @Mock
    private ExposureSimulationService exposureSimulationService;

    @Test
    void calculatesCvaFromExposureSimulation() {
        when(exposureSimulationService.simulate(any())).thenReturn(new ExposureSimulationResult(
                PORTFOLIO_ID,
                VALUATION_DATE,
                "GBM_BLACK_SCHOLES_EXPOSURE_V1",
                100,
                2,
                0.95,
                List.of(
                        new ExposurePoint(VALUATION_DATE.plusDays(365), 100.0, 0.0, 120.0),
                        new ExposurePoint(VALUATION_DATE.plusDays(730), 80.0, 0.0, 100.0)
                )
        ));

        CvaCalculationService service = new CvaCalculationService(exposureSimulationService);
        CvaCalculationResult result = service.calculate(command());

        assertThat(result.model()).isEqualTo("SIMPLIFIED_CVA_V1");
        assertThat(result.exposureModel()).isEqualTo("GBM_BLACK_SCHOLES_EXPOSURE_V1");
        assertThat(result.cva()).isPositive();
        assertThat(result.points()).hasSize(2);

        ArgumentCaptor<ExposureSimulationCommand> captor = ArgumentCaptor.forClass(ExposureSimulationCommand.class);
        verify(exposureSimulationService).simulate(captor.capture());
        assertThat(captor.getValue().portfolioId()).isEqualTo(PORTFOLIO_ID);
        assertThat(captor.getValue().paths()).isEqualTo(100);
    }

    private CvaCalculationCommand command() {
        return new CvaCalculationCommand(
                PORTFOLIO_ID,
                VALUATION_DATE,
                730,
                2,
                100,
                12345L,
                0.95,
                0.60,
                0.02,
                0.05
        );
    }
}
