package com.nexusxva.portfolio.application;

import com.nexusxva.marketdata.application.MarketDataValidationService;
import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.portfolio.domain.Portfolio;
import com.nexusxva.portfolio.domain.PortfolioSummary;
import com.nexusxva.shared.error.ResourceNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PortfolioService {

    private final PortfolioStore portfolioStore;
    private final MarketDataValidationService marketDataValidationService;

    public PortfolioService(
            PortfolioStore portfolioStore,
            MarketDataValidationService marketDataValidationService
    ) {
        this.portfolioStore = portfolioStore;
        this.marketDataValidationService = marketDataValidationService;
    }

    @Transactional
    public Portfolio createPortfolio(CreatePortfolioCommand command) {
        return portfolioStore.createPortfolio(command);
    }

    @Transactional(readOnly = true)
    public List<PortfolioSummary> listPortfolios() {
        return portfolioStore.listPortfolioSummaries();
    }

    @Transactional(readOnly = true)
    public Portfolio getPortfolio(UUID portfolioId) {
        return portfolioStore.findPortfolio(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
    }

    @Transactional
    public Portfolio updatePortfolio(UUID portfolioId, UpdatePortfolioCommand command) {
        ensurePortfolioExists(portfolioId);
        return portfolioStore.updatePortfolio(portfolioId, command);
    }

    @Transactional
    public void deletePortfolio(UUID portfolioId) {
        ensurePortfolioExists(portfolioId);
        portfolioStore.deletePortfolio(portfolioId);
    }

    @Transactional
    public EuropeanOptionPosition addEuropeanOptionPosition(
            UUID portfolioId,
            AddEuropeanOptionPositionCommand command
    ) {
        ensurePortfolioExists(portfolioId);
        marketDataValidationService.validateUnderlyingSymbol(command.underlyingSymbol());
        return portfolioStore.addEuropeanOptionPosition(portfolioId, command);
    }

    @Transactional(readOnly = true)
    public List<EuropeanOptionPosition> listEuropeanOptionPositions(UUID portfolioId) {
        ensurePortfolioExists(portfolioId);
        return portfolioStore.findEuropeanOptionPositions(portfolioId);
    }

    @Transactional(readOnly = true)
    public EuropeanOptionPosition getEuropeanOptionPosition(UUID portfolioId, UUID positionId) {
        ensurePortfolioExists(portfolioId);
        return portfolioStore.findEuropeanOptionPosition(portfolioId, positionId)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found"));
    }

    @Transactional
    public EuropeanOptionPosition updateEuropeanOptionPosition(
            UUID portfolioId,
            UUID positionId,
            UpdateEuropeanOptionPositionCommand command
    ) {
        ensurePortfolioExists(portfolioId);
        validateUnderlyingSymbolChange(portfolioId, positionId, command);
        return portfolioStore.updateEuropeanOptionPosition(portfolioId, positionId, command);
    }

    @Transactional
    public void deleteEuropeanOptionPosition(UUID portfolioId, UUID positionId) {
        ensurePortfolioExists(portfolioId);
        portfolioStore.deleteEuropeanOptionPosition(portfolioId, positionId);
    }

    private void ensurePortfolioExists(UUID portfolioId) {
        if (!portfolioStore.existsPortfolio(portfolioId)) {
            throw new ResourceNotFoundException("Portfolio not found");
        }
    }

    private void validateUnderlyingSymbolChange(
            UUID portfolioId,
            UUID positionId,
            UpdateEuropeanOptionPositionCommand command
    ) {
        if (command.underlyingSymbol() == null) {
            return;
        }

        EuropeanOptionPosition currentPosition = portfolioStore.findEuropeanOptionPosition(portfolioId, positionId)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found"));

        if (!command.underlyingSymbol().equals(currentPosition.underlyingSymbol())) {
            marketDataValidationService.validateUnderlyingSymbol(command.underlyingSymbol());
        }
    }
}
