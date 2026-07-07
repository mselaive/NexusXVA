package com.nexusxva.eod.application;

import com.nexusxva.eod.domain.EodRunStatus;
import com.nexusxva.eod.domain.PortfolioEodSnapshot;
import com.nexusxva.eod.domain.PositionEodSnapshot;
import com.nexusxva.operationalcontrol.application.OperationalControlStore;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingResult;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingService;
import com.nexusxva.shared.error.ConflictException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioEodService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioEodService.class);

    private final PortfolioEodStore store;
    private final PortfolioBlackScholesPricingService pricingService;
    private final OperationalControlStore operationalControlStore;

    public PortfolioEodService(
            PortfolioEodStore store,
            PortfolioBlackScholesPricingService pricingService,
            OperationalControlStore operationalControlStore
    ) {
        this.store = store;
        this.pricingService = pricingService;
        this.operationalControlStore = operationalControlStore;
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
        LocalDate resolvedDate = businessDate == null
                ? LocalDate.now(operationalControlStore.settings().timezone())
                : businessDate;
        LOGGER.info(
                "EOD capture started portfolioId={} businessDate={} source={} correctionOfRunId={}",
                portfolioId,
                resolvedDate,
                source,
                correctionOfRunId
        );
        if (resolvedDate.isAfter(LocalDate.now(operationalControlStore.settings().timezone()))) {
            throw new IllegalArgumentException("EOD businessDate must not be in the future");
        }
        if (store.exists(portfolioId, resolvedDate)) {
            LOGGER.info("EOD capture skipped portfolioId={} businessDate={} source={} reason=ACTIVE_RUN_EXISTS",
                    portfolioId,
                    resolvedDate,
                    source);
            throw new ConflictException("EOD snapshot already exists for portfolio and businessDate");
        }

        PortfolioBlackScholesPricingResult pricing = pricingService.price(portfolioId, resolvedDate);
        if (!pricing.unpriceablePositions().isEmpty()) {
            throw new IllegalArgumentException("EOD snapshot requires all active positions to be priceable");
        }
        boolean hasStaleMarketData = pricing.positions().stream().anyMatch(position -> position.marketData().stale())
                || pricing.cashEquityPositions().stream().anyMatch(position -> position.marketData().stale());
        boolean allowStaleMarketData = operationalControlStore.settings().eodAllowStaleMarketData();
        if (!allowStaleMarketData && hasStaleMarketData) {
            LOGGER.warn("EOD capture rejected portfolioId={} businessDate={} source={} reason=STALE_MARKET_DATA",
                    portfolioId,
                    resolvedDate,
                    source);
            throw new IllegalArgumentException("EOD snapshot cannot use stale market data");
        }
        List<PositionEodSnapshot> optionPositions = pricing.positions().stream()
                .map(position -> new PositionEodSnapshot(
                        position.positionId(),
                        "EUROPEAN_OPTION",
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
        List<PositionEodSnapshot> cashEquityPositions = pricing.cashEquityPositions().stream()
                .map(position -> new PositionEodSnapshot(
                        position.positionId(),
                        "CASH_EQUITY",
                        position.underlyingSymbol(),
                        position.quantity(),
                        position.spot(),
                        position.marketValue(),
                        position.executionPrice(),
                        position.tradeValue(),
                        position.unrealizedPnl(),
                        position.marketData().asOf(),
                        position.marketData().source(),
                        position.marketData().stale()
                ))
                .toList();
        List<PositionEodSnapshot> positions = Stream.concat(optionPositions.stream(), cashEquityPositions.stream()).toList();

        PortfolioEodSnapshot snapshot = store.create(new PortfolioEodSnapshot(
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
        LOGGER.info(
                "EOD capture completed portfolioId={} runId={} businessDate={} source={} optionPositions={} cashEquityPositions={} totalPositions={} totalMarketValue={} totalTradeValue={} totalUnrealizedPnl={} positionsWithoutExecutionPrice={} staleMarketData={}",
                portfolioId,
                snapshot.id(),
                snapshot.businessDate(),
                source,
                optionPositions.size(),
                cashEquityPositions.size(),
                positions.size(),
                snapshot.totalMarketValue(),
                snapshot.totalTradeValue(),
                snapshot.totalUnrealizedPnl(),
                snapshot.positionsWithoutExecutionPrice(),
                hasStaleMarketData
        );
        return snapshot;
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
