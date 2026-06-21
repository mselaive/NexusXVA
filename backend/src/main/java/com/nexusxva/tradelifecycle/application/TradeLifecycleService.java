package com.nexusxva.tradelifecycle.application;

import com.nexusxva.marketdata.application.MarketDataValidationService;
import com.nexusxva.notifications.application.NotificationService;
import com.nexusxva.portfolio.application.AddEuropeanOptionPositionCommand;
import com.nexusxva.portfolio.application.PortfolioStore;
import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.portfolio.domain.Portfolio;
import com.nexusxva.portfolio.domain.PositionLifecycleStatus;
import com.nexusxva.shared.error.ConflictException;
import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.tradebooking.domain.BookingActor;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequest;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequestStatus;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequestType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradeLifecycleService {

    private final TradeLifecycleStore lifecycleStore;
    private final PortfolioStore portfolioStore;
    private final MarketDataValidationService marketDataValidationService;
    private final NotificationService notificationService;

    public TradeLifecycleService(
            TradeLifecycleStore lifecycleStore,
            PortfolioStore portfolioStore,
            MarketDataValidationService marketDataValidationService,
            NotificationService notificationService
    ) {
        this.lifecycleStore = lifecycleStore;
        this.portfolioStore = portfolioStore;
        this.marketDataValidationService = marketDataValidationService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public EuropeanOptionPosition position(UUID positionId) {
        return portfolioStore.findEuropeanOptionPosition(positionId)
                .orElseThrow(() -> new ResourceNotFoundException("Position not found"));
    }

    @Transactional
    public TradeLifecycleRequest submitAmend(
            UUID positionId,
            AddEuropeanOptionPositionCommand requestedTerms,
            BookingActor submittedBy
    ) {
        EuropeanOptionPosition position = activePosition(positionId);
        Portfolio portfolio = portfolio(position.portfolioId());
        if (lifecycleStore.existsPendingForPosition(positionId)) {
            throw new ConflictException("Position already has a pending lifecycle request");
        }
        marketDataValidationService.validateUnderlyingSymbol(requestedTerms.underlyingSymbol());
        TradeLifecycleRequest request = lifecycleStore.createAmend(position, portfolio.name(), requestedTerms, submittedBy);
        notificationService.notifyLifecycleSubmitted(request);
        return request;
    }

    @Transactional
    public TradeLifecycleRequest submitCancel(UUID positionId, BookingActor submittedBy) {
        EuropeanOptionPosition position = activePosition(positionId);
        Portfolio portfolio = portfolio(position.portfolioId());
        if (lifecycleStore.existsPendingForPosition(positionId)) {
            throw new ConflictException("Position already has a pending lifecycle request");
        }
        TradeLifecycleRequest request = lifecycleStore.createCancel(position, portfolio.name(), submittedBy);
        notificationService.notifyLifecycleSubmitted(request);
        return request;
    }

    @Transactional(readOnly = true)
    public List<TradeLifecycleRequest> mine(BookingActor actor) {
        if (actor.userId() == null) {
            return lifecycleStore.findAllSubmitted();
        }
        return lifecycleStore.findSubmittedBy(actor.userId());
    }

    @Transactional(readOnly = true)
    public Page<TradeLifecycleRequest> search(
            TradeLifecycleRequestStatus status,
            UUID portfolioId,
            String symbol,
            int page,
            int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort sort = status == TradeLifecycleRequestStatus.PENDING_VALIDATION
                ? Sort.by(Sort.Direction.ASC, "submittedAt")
                : Sort.by(Sort.Direction.DESC, "submittedAt");
        return lifecycleStore.search(
                status,
                portfolioId,
                normalizeOptionalSymbol(symbol),
                PageRequest.of(Math.max(page, 0), safeSize, sort)
        );
    }

    @Transactional(readOnly = true)
    public TradeLifecycleRequest get(UUID requestId) {
        return lifecycleStore.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Lifecycle request not found"));
    }

    @Transactional
    public TradeLifecycleRequest approve(UUID requestId, BookingActor reviewer) {
        TradeLifecycleRequest request = lifecycleStore.findByIdForUpdate(requestId);
        ensurePending(request);
        EuropeanOptionPosition position = activePosition(request.positionId());

        if (request.requestType() == TradeLifecycleRequestType.CANCEL) {
            portfolioStore.markPositionCancelled(position.id());
            TradeLifecycleRequest reviewed = lifecycleStore.approve(requestId, reviewer, null);
            notificationService.notifyLifecycleReviewed(reviewed);
            return reviewed;
        }

        portfolioStore.markPositionAmended(position.id());
        EuropeanOptionPosition replacement = portfolioStore.addEuropeanOptionPosition(
                position.portfolioId(),
                new AddEuropeanOptionPositionCommand(
                        request.requestedUnderlyingSymbol(),
                        request.requestedOptionType(),
                        request.requestedStrike(),
                        request.requestedMaturityDate(),
                        request.requestedQuantity()
                )
        );
        TradeLifecycleRequest reviewed = lifecycleStore.approve(requestId, reviewer, replacement.id());
        notificationService.notifyLifecycleReviewed(reviewed);
        return reviewed;
    }

    @Transactional
    public TradeLifecycleRequest reject(UUID requestId, BookingActor reviewer, String reason) {
        TradeLifecycleRequest request = lifecycleStore.findByIdForUpdate(requestId);
        ensurePending(request);
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isBlank()) {
            throw new IllegalArgumentException("rejectionReason is required");
        }
        if (normalizedReason.length() > 500) {
            throw new IllegalArgumentException("rejectionReason must be at most 500 characters");
        }
        TradeLifecycleRequest reviewed = lifecycleStore.reject(requestId, reviewer, normalizedReason);
        notificationService.notifyLifecycleReviewed(reviewed);
        return reviewed;
    }

    private EuropeanOptionPosition activePosition(UUID positionId) {
        EuropeanOptionPosition position = position(positionId);
        if (position.lifecycleStatus() != PositionLifecycleStatus.ACTIVE) {
            throw new ConflictException("Position is not active");
        }
        return position;
    }

    private Portfolio portfolio(UUID portfolioId) {
        return portfolioStore.findPortfolio(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio not found"));
    }

    private void ensurePending(TradeLifecycleRequest request) {
        if (request.status() != TradeLifecycleRequestStatus.PENDING_VALIDATION) {
            throw new ConflictException("Lifecycle request has already been reviewed");
        }
    }

    private String normalizeOptionalSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return null;
        }
        return symbol.trim().toUpperCase();
    }
}
