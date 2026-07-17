package com.nexusxva.xva.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreditCurve(
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
        List<Point> points
) {
    public CreditCurve {
        points = points == null ? List.of() : List.copyOf(points);
    }

    public record Point(
            LocalDate date,
            Double survivalProbability,
            Double cumulativeDefaultProbability
    ) {
    }
}
