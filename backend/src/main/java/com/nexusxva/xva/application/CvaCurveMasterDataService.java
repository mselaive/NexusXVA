package com.nexusxva.xva.application;

import com.nexusxva.cva.domain.CreditCurvePoint;
import com.nexusxva.cva.domain.DiscountCurvePoint;
import com.nexusxva.shared.error.ConflictException;
import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.shared.error.ServiceUnavailableException;
import com.nexusxva.xva.domain.Counterparty;
import com.nexusxva.xva.domain.CreditCurve;
import com.nexusxva.xva.domain.CurveLifecycleStatus;
import com.nexusxva.xva.domain.DiscountCurve;
import com.nexusxva.marketdata.application.MarketDataCurveGateway;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CvaCurveMasterDataService {

    private final CvaCurveStore store;
    private final XvaStore xvaStore;
    private final MarketDataCurveGateway marketDataCurveGateway;

    public CvaCurveMasterDataService(CvaCurveStore store, XvaStore xvaStore, MarketDataCurveGateway marketDataCurveGateway) {
        this.store = store;
        this.xvaStore = xvaStore;
        this.marketDataCurveGateway = marketDataCurveGateway;
    }

    @Transactional
    public CreditCurve createCreditCurve(SaveCreditCurveCommand command) {
        Counterparty counterparty = xvaStore.findCounterparty(command.counterpartyId())
                .orElseThrow(() -> new ResourceNotFoundException("Counterparty not found"));
        if (!counterparty.active()) {
            throw new ConflictException("Counterparty must be active");
        }
        return store.createCreditCurve(command);
    }

    @Transactional
    public CreditCurve importCreditCurve(SaveCreditCurveCommand command) {
        return createCreditCurve(new SaveCreditCurveCommand(
                command.counterpartyId(), command.name(), command.curveType(), false, command.points(),
                com.nexusxva.xva.domain.CurveSource.IMPORT));
    }

    @Transactional
    public CreditCurve importMarketCreditCurve(UUID counterpartyId, java.time.LocalDate valuationDate,
                                                Double recoveryRate, String requestedName, boolean allowStale) {
        if (counterpartyId == null) throw new IllegalArgumentException("counterpartyId is required");
        if (valuationDate == null) throw new IllegalArgumentException("valuationDate is required");
        double normalizedRecoveryRate = recoveryRate == null ? 0.40 : recoveryRate;
        if (!Double.isFinite(normalizedRecoveryRate) || normalizedRecoveryRate < 0.0 || normalizedRecoveryRate >= 1.0) {
            throw new IllegalArgumentException("recoveryRate must be between 0 and 1");
        }
        Counterparty counterparty = xvaStore.findCounterparty(counterpartyId)
                .orElseThrow(() -> new ResourceNotFoundException("Counterparty not found"));
        if (!counterparty.active()) throw new ConflictException("Counterparty must be active");
        String rating = normalizeSupportedRating(counterparty.creditRating());
        var marketCurve = marketDataCurveGateway.getCreditCurve(rating, "USD", valuationDate, normalizedRecoveryRate);
        if (!valuationDate.equals(marketCurve.valuationDate()) || !"USD".equals(marketCurve.currency())
                || !ratingBucket(rating).equals(marketCurve.ratingBucket())) {
            throw new ServiceUnavailableException("Market data service returned an inconsistent credit curve");
        }
        if (marketCurve.stale() && !allowStale) {
            throw new ConflictException("Market credit curve is stale; enable allowStale to import it as draft");
        }
        String name = requestedName == null || requestedName.isBlank() ? marketCurve.name() : requestedName.trim();
        return createCreditCurve(new SaveCreditCurveCommand(
                counterpartyId,
                name,
                com.nexusxva.xva.domain.CreditCurveType.CUMULATIVE_DEFAULT_PROBABILITY,
                false,
                marketCurve.points().stream()
                        .map(point -> new CreditCurve.Point(point.date(), null, point.cumulativeDefaultProbability()))
                        .toList(),
                com.nexusxva.xva.domain.CurveSource.MARKET_DATA,
                marketCurve.asOf(),
                marketCurve.source(),
                marketCurve.sourceSeriesId(),
                marketCurve.method(),
                marketCurve.stale(),
                marketCurve.currency(),
                marketCurve.creditRating(),
                marketCurve.ratingBucket(),
                marketCurve.recoveryRate(),
                marketCurve.spread(),
                marketCurve.spreadUnit(),
                marketCurve.hazardRate(),
                marketCurve.observationDate(),
                marketCurve.marketProxy()
        ));
    }

    private String normalizeSupportedRating(String rating) {
        if (rating == null || rating.isBlank()) throw new IllegalArgumentException("Counterparty creditRating is required");
        String normalized = rating.trim().toUpperCase();
        if (!normalized.matches("^(AAA|AA[+-]?|A[+-]?|BBB[+-]?)$")) {
            throw new IllegalArgumentException("Counterparty creditRating is not supported by market data");
        }
        return normalized;
    }

    private String ratingBucket(String rating) {
        if (rating.startsWith("BBB")) return "BBB";
        if (rating.startsWith("AAA")) return "AAA";
        if (rating.startsWith("AA")) return "AA";
        return "A";
    }

    @Transactional
    public CreditCurve updateCreditCurve(UUID curveId, SaveCreditCurveCommand command) {
        CreditCurve curve = getCreditCurve(curveId);
        if (curve.status() != CurveLifecycleStatus.DRAFT) {
            throw new ConflictException("Only draft credit curves can be updated; create a new version instead");
        }
        return store.updateCreditCurve(curveId, command);
    }

    @Transactional(readOnly = true)
    public List<CreditCurve> listCreditCurves(UUID counterpartyId, boolean includeInactive) {
        return store.listCreditCurves(counterpartyId, includeInactive);
    }

    @Transactional(readOnly = true)
    public CreditCurve getCreditCurve(UUID curveId) {
        return store.findCreditCurve(curveId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit curve not found"));
    }

    @Transactional(readOnly = true)
    public List<CreditCurvePoint> activeCreditCurvePoints(UUID curveId) {
        CreditCurve curve = getCreditCurve(curveId);
        if (!curve.active() || curve.status() != CurveLifecycleStatus.APPROVED) {
            throw new ResourceNotFoundException("Credit curve not found");
        }
        return curve.points().stream()
                .map(point -> new CreditCurvePoint(point.date(), point.survivalProbability(), point.cumulativeDefaultProbability()))
                .toList();
    }

    @Transactional
    public DiscountCurve createDiscountCurve(SaveDiscountCurveCommand command) {
        return store.createDiscountCurve(command);
    }

    @Transactional
    public DiscountCurve importDiscountCurve(SaveDiscountCurveCommand command) {
        return createDiscountCurve(new SaveDiscountCurveCommand(
                command.name(), command.currency(), false, command.points(),
                com.nexusxva.xva.domain.CurveSource.IMPORT));
    }

    @Transactional
    public DiscountCurve importMarketDiscountCurve(String currency, java.time.LocalDate valuationDate, String requestedName, boolean allowStale) {
        if (currency == null || !currency.trim().toUpperCase().matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("currency must be a 3-letter code");
        }
        if (valuationDate == null) {
            throw new IllegalArgumentException("valuationDate is required");
        }
        String normalizedCurrency = currency.trim().toUpperCase();
        var marketCurve = marketDataCurveGateway.getDiscountCurve(normalizedCurrency, valuationDate);
        if (!normalizedCurrency.equals(marketCurve.currency()) || !valuationDate.equals(marketCurve.valuationDate())) {
            throw new IllegalArgumentException("Market discount curve does not match requested currency and valuationDate");
        }
        if (marketCurve.stale() && !allowStale) {
            throw new ConflictException("Market discount curve is stale; enable allowStale to import it as draft");
        }
        String name = requestedName == null || requestedName.isBlank() ? marketCurve.name() : requestedName.trim();
        return createDiscountCurve(new SaveDiscountCurveCommand(
                name,
                marketCurve.currency(),
                false,
                marketCurve.points().stream()
                        .map(point -> new DiscountCurve.Point(point.date(), point.discountFactor()))
                        .toList(),
                com.nexusxva.xva.domain.CurveSource.MARKET_DATA,
                marketCurve.asOf(),
                marketCurve.source(),
                marketCurve.method(),
                marketCurve.stale()
        ));
    }

    @Transactional
    public DiscountCurve updateDiscountCurve(UUID curveId, SaveDiscountCurveCommand command) {
        DiscountCurve curve = getDiscountCurve(curveId);
        if (curve.status() != CurveLifecycleStatus.DRAFT) {
            throw new ConflictException("Only draft discount curves can be updated; create a new version instead");
        }
        return store.updateDiscountCurve(curveId, command);
    }

    @Transactional(readOnly = true)
    public List<DiscountCurve> listDiscountCurves(String currency, boolean includeInactive) {
        return store.listDiscountCurves(currency, includeInactive);
    }

    @Transactional(readOnly = true)
    public DiscountCurve getDiscountCurve(UUID curveId) {
        return store.findDiscountCurve(curveId)
                .orElseThrow(() -> new ResourceNotFoundException("Discount curve not found"));
    }

    @Transactional(readOnly = true)
    public List<DiscountCurvePoint> activeDiscountCurvePoints(UUID curveId) {
        DiscountCurve curve = getDiscountCurve(curveId);
        if (!curve.active() || curve.status() != CurveLifecycleStatus.APPROVED) {
            throw new ResourceNotFoundException("Discount curve not found");
        }
        return curve.points().stream()
                .map(point -> new DiscountCurvePoint(point.date(), point.discountFactor()))
                .toList();
    }

    @Transactional
    public CreditCurve approveCreditCurve(UUID curveId, UUID approvedByUserId) {
        return store.approveCreditCurve(curveId, approvedByUserId);
    }

    @Transactional
    public CreditCurve rejectCreditCurve(UUID curveId, String reason) {
        String normalized = reason == null ? "" : reason.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("rejectionReason is required");
        }
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("rejectionReason must be at most 500 characters");
        }
        return store.rejectCreditCurve(curveId, normalized);
    }

    @Transactional
    public DiscountCurve approveDiscountCurve(UUID curveId, UUID approvedByUserId) {
        return store.approveDiscountCurve(curveId, approvedByUserId);
    }

    @Transactional
    public DiscountCurve rejectDiscountCurve(UUID curveId, String reason) {
        String normalized = reason == null ? "" : reason.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("rejectionReason is required");
        }
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("rejectionReason must be at most 500 characters");
        }
        return store.rejectDiscountCurve(curveId, normalized);
    }
}
