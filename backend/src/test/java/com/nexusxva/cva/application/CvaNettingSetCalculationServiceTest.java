package com.nexusxva.cva.application;

import static org.assertj.core.api.Assertions.assertThat;
import com.nexusxva.exposure.application.ExposureSimulationCommand;
import com.nexusxva.exposure.application.ExposureSimulationResult;
import com.nexusxva.exposure.application.ExposureSimulationService;
import com.nexusxva.exposure.domain.ExposurePoint;
import com.nexusxva.xva.application.CreateCounterpartyCommand;
import com.nexusxva.xva.application.CreateNettingSetCommand;
import com.nexusxva.xva.application.UpdateNettingSetCollateralCommand;
import com.nexusxva.xva.application.XvaReferenceDataService;
import com.nexusxva.xva.application.XvaStore;
import com.nexusxva.xva.domain.Counterparty;
import com.nexusxva.xva.domain.NettingSet;
import com.nexusxva.xva.domain.NettingSetPortfolio;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CvaNettingSetCalculationServiceTest {

    @Test
    void aggregatesPortfolioExposureProfilesAndSubtractsStaticCollateral() {
        UUID nettingSetId = UUID.randomUUID();
        UUID counterpartyId = UUID.randomUUID();
        UUID firstPortfolioId = UUID.randomUUID();
        UUID secondPortfolioId = UUID.randomUUID();
        LocalDate valuationDate = LocalDate.of(2026, 6, 30);
        LocalDate exposureDate = valuationDate.plusMonths(1);

        NettingSet nettingSet = new NettingSet(
                nettingSetId,
                counterpartyId,
                "Global Bank",
                "Equity Options CSA",
                "USD",
                BigDecimal.valueOf(25),
                "USD",
                true,
                Instant.parse("2026-06-30T10:00:00Z"),
                Instant.parse("2026-06-30T10:00:00Z"),
                List.of(
                        new NettingSetPortfolio(firstPortfolioId, "Tech Book", "USD", Instant.parse("2026-06-30T10:00:00Z")),
                        new NettingSetPortfolio(secondPortfolioId, "Bank Book", "USD", Instant.parse("2026-06-30T10:00:00Z"))
                )
        );
        CvaNettingSetCalculationService service = new CvaNettingSetCalculationService(
                new StubXvaReferenceDataService(nettingSet),
                new StubExposureSimulationService(valuationDate, exposureDate, firstPortfolioId, secondPortfolioId)
        );

        CvaNettingSetCalculationResult result = service.calculate(new CvaNettingSetCalculationCommand(
                nettingSetId,
                valuationDate,
                30,
                1,
                100,
                12345L,
                0.95,
                0.6,
                0.02,
                0.04,
                List.of(),
                List.of()
        ));

        assertThat(result.portfolioCount()).isEqualTo(2);
        assertThat(result.points()).hasSize(1);
        assertThat(result.points().getFirst().expectedExposure()).isEqualTo(125.0);
        assertThat(result.cva()).isPositive();
    }

    private ExposureSimulationCommand portfolioCommand(UUID portfolioId, LocalDate valuationDate) {
        return new ExposureSimulationCommand(portfolioId, valuationDate, 30, 1, 100, 12345L, 0.95);
    }

    private ExposureSimulationResult exposureResult(
            UUID portfolioId,
            LocalDate valuationDate,
            LocalDate exposureDate,
            double expectedExposure,
            double pfe
    ) {
        return new ExposureSimulationResult(
                portfolioId,
                valuationDate,
                "GBM_BLACK_SCHOLES_EXPOSURE_V1",
                "USD",
                100,
                1,
                0.95,
                List.of(new ExposurePoint(exposureDate, expectedExposure, 0.0, pfe))
        );
    }

    private static class StubXvaReferenceDataService extends XvaReferenceDataService {
        private final NettingSet nettingSet;

        StubXvaReferenceDataService(NettingSet nettingSet) {
            super(new EmptyXvaStore(), null);
            this.nettingSet = nettingSet;
        }

        @Override
        public NettingSet getNettingSet(UUID nettingSetId) {
            return nettingSet;
        }
    }

    private class StubExposureSimulationService extends ExposureSimulationService {
        private final LocalDate valuationDate;
        private final LocalDate exposureDate;
        private final UUID firstPortfolioId;
        private final UUID secondPortfolioId;

        StubExposureSimulationService(
                LocalDate valuationDate,
                LocalDate exposureDate,
                UUID firstPortfolioId,
                UUID secondPortfolioId
        ) {
            super(null, null, null, null);
            this.valuationDate = valuationDate;
            this.exposureDate = exposureDate;
            this.firstPortfolioId = firstPortfolioId;
            this.secondPortfolioId = secondPortfolioId;
        }

        @Override
        public ExposureSimulationResult simulate(ExposureSimulationCommand command) {
            if (command.portfolioId().equals(firstPortfolioId)) {
                return exposureResult(firstPortfolioId, valuationDate, exposureDate, 100.0, 180.0);
            }
            if (command.portfolioId().equals(secondPortfolioId)) {
                return exposureResult(secondPortfolioId, valuationDate, exposureDate, 50.0, 70.0);
            }
            throw new IllegalArgumentException("Unexpected portfolio");
        }
    }

    private static class EmptyXvaStore implements XvaStore {
        @Override
        public Counterparty createCounterparty(CreateCounterpartyCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Counterparty> listCounterparties() {
            return List.of();
        }

        @Override
        public Optional<Counterparty> findCounterparty(UUID counterpartyId) {
            return Optional.empty();
        }

        @Override
        public NettingSet createNettingSet(CreateNettingSetCommand command) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<NettingSet> listNettingSets() {
            return List.of();
        }

        @Override
        public Optional<NettingSet> findNettingSet(UUID nettingSetId) {
            return Optional.empty();
        }

        @Override
        public NettingSet assignPortfolio(UUID nettingSetId, UUID portfolioId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public NettingSet removePortfolio(UUID nettingSetId, UUID portfolioId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public NettingSet updateCollateral(UUID nettingSetId, UpdateNettingSetCollateralCommand command) {
            throw new UnsupportedOperationException();
        }
    }
}
