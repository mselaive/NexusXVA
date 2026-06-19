package com.nexusxva.tradebooking.infrastructure;

import com.nexusxva.portfolio.application.PendingTradeBookingChecker;
import com.nexusxva.tradebooking.domain.TradeBookingStatus;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class JpaPendingTradeBookingChecker implements PendingTradeBookingChecker {

    private final TradeBookingJpaRepository repository;

    JpaPendingTradeBookingChecker(TradeBookingJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean hasPendingBookings(UUID portfolioId) {
        return repository.existsByPortfolioIdAndStatus(portfolioId, TradeBookingStatus.PENDING_VALIDATION);
    }
}

