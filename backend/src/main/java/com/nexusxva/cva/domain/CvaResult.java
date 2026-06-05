package com.nexusxva.cva.domain;

import java.util.List;

public record CvaResult(
        double cva,
        List<CvaPoint> points
) {

    public CvaResult {
        if (!Double.isFinite(cva) || cva < 0.0) {
            throw new IllegalArgumentException("cva must be finite and greater than or equal to zero");
        }
        if (points == null) {
            throw new IllegalArgumentException("cva points are required");
        }
        points = List.copyOf(points);
    }
}
