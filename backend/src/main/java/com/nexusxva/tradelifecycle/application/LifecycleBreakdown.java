package com.nexusxva.tradelifecycle.application;

public record LifecycleBreakdown(
        String key,
        String label,
        int total,
        int pendingValidation,
        int approved,
        int rejected
) {
}
