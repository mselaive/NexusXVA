package com.nexusxva.tradebooking.application;

import com.nexusxva.marketdata.application.MarketDataValidationService;
import com.nexusxva.portfolio.application.PortfolioStore;
import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.portfolio.domain.Portfolio;
import com.nexusxva.shared.error.ConflictException;
import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.tradebooking.domain.BookingActor;
import com.nexusxva.tradebooking.domain.TradeBookingRequest;
import com.nexusxva.tradebooking.domain.TradeBookingStatus;
import com.nexusxva.tradinglimits.application.TradingLimitService;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeBookingService {

    private final TradeBookingStore tradeBookingStore;
    private final PortfolioStore portfolioStore;
    private final MarketDataValidationService marketDataValidationService;
    private final TradingLimitService tradingLimitService;

    public TradeBookingService(
            TradeBookingStore tradeBookingStore,
            PortfolioStore portfolioStore,
            MarketDataValidationService marketDataValidationService,
            TradingLimitService tradingLimitService
    ) {
        this.tradeBookingStore = tradeBookingStore;
        this.portfolioStore = portfolioStore;
        this.marketDataValidationService = marketDataValidationService;
        this.tradingLimitService = tradingLimitService;
    }

    @Transactional
    public TradeBookingRequest submitEuropeanOption(
            UUID portfolioId,
            CreateEuropeanOptionBookingCommand command,
            BookingActor submittedBy
    ) {
        Portfolio portfolio = portfolioStore.findPortfolio(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
        marketDataValidationService.validateUnderlyingSymbol(command.underlyingSymbol());
        tradingLimitService.validateBooking(submittedBy, portfolio.baseCurrency(), command);
        return tradeBookingStore.create(portfolioId, portfolio.name(), command, submittedBy);
    }

    @Transactional(readOnly = true)
    public List<TradeBookingRequest> mine(BookingActor actor) {
        if (actor.userId() == null) {
            return tradeBookingStore.findAllSubmitted();
        }
        return tradeBookingStore.findSubmittedBy(actor.userId());
    }

    @Transactional(readOnly = true)
    public Page<TradeBookingRequest> search(
            TradeBookingStatus status,
            UUID portfolioId,
            String symbol,
            int page,
            int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort sort = status == TradeBookingStatus.PENDING_VALIDATION
                ? Sort.by(Sort.Direction.ASC, "submittedAt")
                : Sort.by(Sort.Direction.DESC, "submittedAt");
        return tradeBookingStore.search(
                status,
                portfolioId,
                normalizeOptionalSymbol(symbol),
                PageRequest.of(Math.max(page, 0), safeSize, sort)
        );
    }

    @Transactional(readOnly = true)
    public TradeBookingRequest get(UUID bookingId) {
        return tradeBookingStore.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Trade booking not found"));
    }

    @Transactional
    public TradeBookingRequest approve(UUID bookingId, BookingActor reviewer) {
        TradeBookingRequest booking = tradeBookingStore.findByIdForUpdate(bookingId);
        ensurePending(booking);
        if (booking.portfolioId() == null || !portfolioStore.existsPortfolio(booking.portfolioId())) {
            throw new ConflictException("Portfolio no longer exists");
        }

        EuropeanOptionPosition position = portfolioStore.addEuropeanOptionPosition(
                booking.portfolioId(),
                new CreateEuropeanOptionBookingCommand(
                        booking.underlyingSymbol(),
                        booking.optionType(),
                        booking.strike(),
                        booking.maturityDate(),
                        booking.quantity()
                ).toPositionCommand()
        );
        return tradeBookingStore.confirm(bookingId, reviewer, position.id());
    }

    @Transactional
    public TradeBookingRequest reject(UUID bookingId, BookingActor reviewer, String reason) {
        TradeBookingRequest booking = tradeBookingStore.findByIdForUpdate(bookingId);
        ensurePending(booking);
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isBlank()) {
            throw new IllegalArgumentException("rejectionReason is required");
        }
        if (normalizedReason.length() > 500) {
            throw new IllegalArgumentException("rejectionReason must be at most 500 characters");
        }
        return tradeBookingStore.reject(bookingId, reviewer, normalizedReason);
    }

    private void ensurePending(TradeBookingRequest booking) {
        if (booking.status() != TradeBookingStatus.PENDING_VALIDATION) {
            throw new ConflictException("Trade booking has already been reviewed");
        }
    }

    private String normalizeOptionalSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        return symbol.trim().toUpperCase();
    }
}
