package com.nexusxva.eod.application;

import com.nexusxva.eod.domain.EodRunStatus;
import com.nexusxva.eod.domain.PortfolioEodSnapshot;
import com.nexusxva.eod.domain.PositionEodSnapshot;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingResult;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingService;
import com.nexusxva.shared.error.ConflictException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

@Service
public class PortfolioEodService {

    private final PortfolioEodStore store;
    private final PortfolioBlackScholesPricingService pricingService;
    private final boolean allowStaleMarketData;

    public PortfolioEodService(
            PortfolioEodStore store,
            PortfolioBlackScholesPricingService pricingService,
            @Value("${nexusxva.eod.allow-stale:false}") boolean allowStaleMarketData
    ) {
        this.store = store;
        this.pricingService = pricingService;
        this.allowStaleMarketData = allowStaleMarketData;
    }

    @Transactional
    public PortfolioEodSnapshot capture(UUID portfolioId, LocalDate businessDate, String source) {
        return captureInternal(portfolioId, businessDate, source, null);
    }

    @Transactional
    public PortfolioEodSnapshot voidRun(UUID runId, UUID voidedByUserId, String reason) {
        PortfolioEodSnapshot run = activeRun(runId);
        String normalizedReason = normalizeReason(reason);
        store.voidRun(runId, voidedByUserId, normalizedReason);
        return store.find(runId).orElse(run);
    }

    @Transactional
    public PortfolioEodSnapshot recapture(UUID runId, UUID voidedByUserId, String reason) {
        PortfolioEodSnapshot run = activeRun(runId);
        String normalizedReason = normalizeReason(reason);
        store.supersedeRun(runId, voidedByUserId, normalizedReason);
        return captureInternal(run.portfolioId(), run.businessDate(), "MANUAL_BO_CORRECTION", run.id());
    }

    private PortfolioEodSnapshot captureInternal(
            UUID portfolioId,
            LocalDate businessDate,
            String source,
            UUID correctionOfRunId
    ) {
        LocalDate resolvedDate = businessDate == null ? LocalDate.now(ZoneOffset.UTC) : businessDate;
        if (resolvedDate.isAfter(LocalDate.now(ZoneOffset.UTC))) {
            throw new IllegalArgumentException("EOD businessDate must not be in the future");
        }
        if (store.exists(portfolioId, resolvedDate)) {
            throw new ConflictException("EOD snapshot already exists for portfolio and businessDate");
        }

        PortfolioBlackScholesPricingResult pricing = pricingService.price(portfolioId, resolvedDate);
        if (!pricing.unpriceablePositions().isEmpty()) {
            throw new IllegalArgumentException("EOD snapshot requires all active positions to be priceable");
        }
        if (!allowStaleMarketData && pricing.positions().stream().anyMatch(position -> position.marketData().stale())) {
            throw new IllegalArgumentException("EOD snapshot cannot use stale market data");
        }
        List<PositionEodSnapshot> positions = pricing.positions().stream()
                .map(position -> new PositionEodSnapshot(
                        position.positionId(),
                        position.underlyingSymbol(),
                        position.quantity(),
                        position.unitPrice(),
                        position.positionPrice(),
                        position.executionPrice(),
                        position.tradeValue(),
                        position.unrealizedPnl(),
                        position.marketData().asOf(),
                        position.marketData().source(),
                        position.marketData().stale()
                ))
                .toList();

        return store.create(new PortfolioEodSnapshot(
                UUID.randomUUID(),
                portfolioId,
                pricing.valuationDate(),
                pricing.baseCurrency(),
                pricing.totalPrice(),
                pricing.totalTradeValue(),
                pricing.totalUnrealizedPnl(),
                pricing.positionsWithoutExecutionPrice(),
                Instant.now(),
                source,
                EodRunStatus.ACTIVE,
                null,
                null,
                null,
                correctionOfRunId,
                positions
        ));
    }

    @Transactional(readOnly = true)
    public Optional<PortfolioEodSnapshot> latest(UUID portfolioId) {
        return store.latest(portfolioId);
    }

    @Transactional(readOnly = true)
    public List<PortfolioEodSnapshot> history(UUID portfolioId, int limit) {
        return store.history(portfolioId, Math.min(Math.max(limit, 1), 60));
    }

    private PortfolioEodSnapshot activeRun(UUID runId) {
        PortfolioEodSnapshot run = store.find(runId)
                .orElseThrow(() -> new com.nexusxva.shared.error.ResourceNotFoundException("EOD snapshot not found"));
        if (run.status() != EodRunStatus.ACTIVE) {
            throw new ConflictException("Only ACTIVE EOD snapshots can be corrected");
        }
        return run;
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Correction reason is required");
        }
        String normalized = reason.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("Correction reason must be at most 500 characters");
        }
        return normalized;
    }
}
