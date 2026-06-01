package com.nexusxva.portfolio.application;

import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.portfolio.domain.Portfolio;
import com.nexusxva.portfolio.domain.PortfolioSummary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioStore {

    Portfolio createPortfolio(CreatePortfolioCommand command);

    List<PortfolioSummary> listPortfolioSummaries();

    Optional<Portfolio> findPortfolio(UUID portfolioId);

    boolean existsPortfolio(UUID portfolioId);

    Portfolio updatePortfolio(UUID portfolioId, UpdatePortfolioCommand command);

    void deletePortfolio(UUID portfolioId);

    EuropeanOptionPosition addEuropeanOptionPosition(
            UUID portfolioId,
            AddEuropeanOptionPositionCommand command
    );

    List<EuropeanOptionPosition> findEuropeanOptionPositions(UUID portfolioId);

    Optional<EuropeanOptionPosition> findEuropeanOptionPosition(UUID portfolioId, UUID positionId);

    EuropeanOptionPosition updateEuropeanOptionPosition(
            UUID portfolioId,
            UUID positionId,
            UpdateEuropeanOptionPositionCommand command
    );

    void deleteEuropeanOptionPosition(UUID portfolioId, UUID positionId);
}
