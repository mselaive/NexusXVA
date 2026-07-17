package com.nexusxva.xva.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DiscountCurve(
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
        List<Point> points
) {
    public DiscountCurve {
        points = points == null ? List.of() : List.copyOf(points);
    }

    public record Point(LocalDate date, double discountFactor) {
    }
}
