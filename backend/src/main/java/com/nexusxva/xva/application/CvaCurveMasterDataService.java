package com.nexusxva.xva.application;

import com.nexusxva.cva.domain.CreditCurvePoint;
import com.nexusxva.cva.domain.DiscountCurvePoint;
import com.nexusxva.shared.error.ConflictException;
import com.nexusxva.shared.error.ResourceNotFoundException;
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
        var marketCurve = marketDataCurveGateway.getDiscountCurve(currency, valuationDate);
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
                com.nexusxva.xva.domain.CurveSource.MARKET_DATA
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
