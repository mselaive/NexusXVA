package com.nexusxva.portfolio.application;

import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.portfolio.domain.Portfolio;
import com.nexusxva.portfolio.domain.PortfolioSummary;
import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.shared.error.ConflictException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PortfolioService {

    private final PortfolioStore portfolioStore;
    private final PendingTradeBookingChecker pendingTradeBookingChecker;

    public PortfolioService(
            PortfolioStore portfolioStore,
            PendingTradeBookingChecker pendingTradeBookingChecker
    ) {
        this.portfolioStore = portfolioStore;
        this.pendingTradeBookingChecker = pendingTradeBookingChecker;
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
        if (pendingTradeBookingChecker.hasPendingBookings(portfolioId)) {
            throw new ConflictException("Portfolio has pending trade bookings");
        }
        portfolioStore.deletePortfolio(portfolioId);
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

    private void ensurePortfolioExists(UUID portfolioId) {
        if (!portfolioStore.existsPortfolio(portfolioId)) {
            throw new ResourceNotFoundException("Portfolio not found");
        }
    }

}
