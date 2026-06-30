package com.nexusxva.portfolio.application;

import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.portfolio.domain.CashEquityPosition;
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

    void archivePortfolio(UUID portfolioId, UUID archivedByUserId, String reason);

    EuropeanOptionPosition addEuropeanOptionPosition(
            UUID portfolioId,
            AddEuropeanOptionPositionCommand command
    );

    CashEquityPosition addCashEquityPosition(
            UUID portfolioId,
            AddCashEquityPositionCommand command
    );

    List<EuropeanOptionPosition> findEuropeanOptionPositions(UUID portfolioId);

    List<EuropeanOptionPosition> findActiveEuropeanOptionPositions(UUID portfolioId);

    List<CashEquityPosition> findCashEquityPositions(UUID portfolioId);

    List<CashEquityPosition> findActiveCashEquityPositions(UUID portfolioId);

    Optional<EuropeanOptionPosition> findEuropeanOptionPosition(UUID portfolioId, UUID positionId);

    Optional<EuropeanOptionPosition> findEuropeanOptionPosition(UUID positionId);

    void markPositionCancelled(UUID positionId);

    void markPositionAmended(UUID positionId);

}
