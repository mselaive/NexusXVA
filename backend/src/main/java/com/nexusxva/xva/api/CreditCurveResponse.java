package com.nexusxva.xva.api;

import com.nexusxva.xva.domain.CreditCurve;
import com.nexusxva.xva.domain.CreditCurveType;
import com.nexusxva.xva.domain.CurveLifecycleStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreditCurveResponse(
        UUID id,
        UUID counterpartyId,
        String counterpartyName,
        String name,
        CreditCurveType curveType,
        int version,
        CurveLifecycleStatus status,
        String source,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        Instant submittedAt,
        Instant approvedAt,
        UUID approvedByUserId,
        String rejectionReason,
        Instant sourceAsOf,
        String sourceReference,
        String sourceSeriesId,
        String constructionMethod,
        boolean sourceStale,
        String sourceCurrency,
        String sourceCreditRating,
        String sourceRatingBucket,
        Double sourceRecoveryRate,
        Double sourceSpread,
        String sourceSpreadUnit,
        Double sourceHazardRate,
        LocalDate sourceObservationDate,
        boolean marketProxy,
        List<PointResponse> points
) {
    static CreditCurveResponse from(CreditCurve curve) {
        return new CreditCurveResponse(
                curve.id(),
                curve.counterpartyId(),
                curve.counterpartyName(),
                curve.name(),
                curve.curveType(),
                curve.version(),
                curve.status(),
                curve.source(),
                curve.active(),
                curve.createdAt(),
                curve.updatedAt(),
                curve.submittedAt(),
                curve.approvedAt(),
                curve.approvedByUserId(),
                curve.rejectionReason(),
                curve.sourceAsOf(),
                curve.sourceReference(),
                curve.sourceSeriesId(),
                curve.constructionMethod(),
                curve.sourceStale(),
                curve.sourceCurrency(),
                curve.sourceCreditRating(),
                curve.sourceRatingBucket(),
                curve.sourceRecoveryRate(),
                curve.sourceSpread(),
                curve.sourceSpreadUnit(),
                curve.sourceHazardRate(),
                curve.sourceObservationDate(),
                curve.marketProxy(),
                curve.points().stream().map(PointResponse::from).toList()
        );
    }

    public record PointResponse(LocalDate date, Double survivalProbability, Double cumulativeDefaultProbability) {
        static PointResponse from(CreditCurve.Point point) {
            return new PointResponse(point.date(), point.survivalProbability(), point.cumulativeDefaultProbability());
        }
    }
}
