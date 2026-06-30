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
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public TradeLifecycleReport reportForBackOffice() {
        return buildReport(lifecycleStore.findAllSubmitted());
    }

    @Transactional(readOnly = true)
    public TradeLifecycleReport reportForFrontOffice(BookingActor actor) {
        return buildReport(mine(actor));
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
                        request.requestedQuantity(),
                        position.executionPrice()
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

    private TradeLifecycleReport buildReport(List<TradeLifecycleRequest> requests) {
        Instant now = Instant.now();
        int pending = 0;
        int approved = 0;
        int rejected = 0;
        int amendments = 0;
        int cancellations = 0;
        long reviewedMinutes = 0;
        int reviewedCount = 0;
        Instant oldestPending = null;
        int pendingUnderTwoHours = 0;
        int pendingTwoToEightHours = 0;
        int pendingEightToTwentyFourHours = 0;
        int pendingOverTwentyFourHours = 0;
        Map<String, BreakdownAccumulator> byPortfolio = new HashMap<>();
        Map<String, BreakdownAccumulator> bySymbol = new HashMap<>();

        for (TradeLifecycleRequest request : requests) {
            if (request.requestType() == TradeLifecycleRequestType.AMEND) {
                amendments++;
            } else {
                cancellations++;
            }

            if (request.status() == TradeLifecycleRequestStatus.PENDING_VALIDATION) {
                pending++;
                oldestPending = oldestPending == null || request.submittedAt().isBefore(oldestPending)
                        ? request.submittedAt()
                        : oldestPending;
                long ageHours = Duration.between(request.submittedAt(), now).toHours();
                if (ageHours < 2) {
                    pendingUnderTwoHours++;
                } else if (ageHours < 8) {
                    pendingTwoToEightHours++;
                } else if (ageHours < 24) {
                    pendingEightToTwentyFourHours++;
                } else {
                    pendingOverTwentyFourHours++;
                }
            } else if (request.status() == TradeLifecycleRequestStatus.APPROVED) {
                approved++;
            } else if (request.status() == TradeLifecycleRequestStatus.REJECTED) {
                rejected++;
            }

            if (request.reviewedAt() != null) {
                reviewedMinutes += Math.max(0, Duration.between(request.submittedAt(), request.reviewedAt()).toMinutes());
                reviewedCount++;
            }

            accumulate(byPortfolio, request.portfolioId() == null ? "UNKNOWN" : request.portfolioId().toString(), request.portfolioName(), request);
            accumulate(bySymbol, request.originalUnderlyingSymbol(), request.originalUnderlyingSymbol(), request);
        }

        return new TradeLifecycleReport(
                requests.size(),
                pending,
                approved,
                rejected,
                amendments,
                cancellations,
                reviewedCount == 0 ? null : Math.round((double) reviewedMinutes / reviewedCount),
                oldestPending,
                List.of(
                        new LifecycleAgingBucket("0-2h", pendingUnderTwoHours),
                        new LifecycleAgingBucket("2-8h", pendingTwoToEightHours),
                        new LifecycleAgingBucket("8-24h", pendingEightToTwentyFourHours),
                        new LifecycleAgingBucket(">24h", pendingOverTwentyFourHours)
                ),
                topBreakdowns(byPortfolio),
                topBreakdowns(bySymbol)
        );
    }

    private void accumulate(
            Map<String, BreakdownAccumulator> accumulator,
            String key,
            String label,
            TradeLifecycleRequest request
    ) {
        accumulator.computeIfAbsent(key, ignored -> new BreakdownAccumulator(key, label)).add(request.status());
    }

    private List<LifecycleBreakdown> topBreakdowns(Map<String, BreakdownAccumulator> accumulator) {
        return accumulator.values()
                .stream()
                .sorted(Comparator
                        .comparingInt(BreakdownAccumulator::pendingValidation).reversed()
                        .thenComparing(Comparator.comparingInt(BreakdownAccumulator::total).reversed())
                        .thenComparing(BreakdownAccumulator::label))
                .limit(10)
                .map(BreakdownAccumulator::toReport)
                .toList();
    }

    private static class BreakdownAccumulator {
        private final String key;
        private final String label;
        private int pendingValidation;
        private int approved;
        private int rejected;

        private BreakdownAccumulator(String key, String label) {
            this.key = key;
            this.label = label == null || label.isBlank() ? key : label;
        }

        private void add(TradeLifecycleRequestStatus status) {
            if (status == TradeLifecycleRequestStatus.PENDING_VALIDATION) {
                pendingValidation++;
            } else if (status == TradeLifecycleRequestStatus.APPROVED) {
                approved++;
            } else if (status == TradeLifecycleRequestStatus.REJECTED) {
                rejected++;
            }
        }

        private int total() {
            return pendingValidation + approved + rejected;
        }

        private int pendingValidation() {
            return pendingValidation;
        }

        private String label() {
            return label;
        }

        private LifecycleBreakdown toReport() {
            return new LifecycleBreakdown(key, label, total(), pendingValidation, approved, rejected);
        }
    }
}
