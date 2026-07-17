package com.nexusxva.xva.api;

import com.nexusxva.xva.domain.DiscountCurve;
import com.nexusxva.xva.domain.CurveLifecycleStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DiscountCurveResponse(
        UUID id,
        String name,
        String currency,
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
        List<PointResponse> points
) {
    static DiscountCurveResponse from(DiscountCurve curve) {
        return new DiscountCurveResponse(
                curve.id(),
                curve.name(),
                curve.currency(),
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
                curve.points().stream().map(PointResponse::from).toList()
        );
    }

    public record PointResponse(LocalDate date, double discountFactor) {
        static PointResponse from(DiscountCurve.Point point) {
            return new PointResponse(point.date(), point.discountFactor());
        }
    }
}
