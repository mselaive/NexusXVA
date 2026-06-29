package com.nexusxva.portfolio.domain;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record Portfolio(
        UUID id,
        String name,
        String description,
        String baseCurrency,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt,
        UUID archivedByUserId,
        String archiveReason,
        List<EuropeanOptionPosition> positions
) {

    private static final String DEFAULT_BASE_CURRENCY = "USD";

    public Portfolio(
            UUID id,
            String name,
            String description,
            String baseCurrency,
            Instant createdAt,
            Instant updatedAt,
            List<EuropeanOptionPosition> positions
    ) {
        this(id, name, description, baseCurrency, createdAt, updatedAt, null, null, null, positions);
    }

    public Portfolio {
        if (id == null) {
            throw new IllegalArgumentException("portfolio id is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("portfolio name is required");
        }
        if (description != null && description.length() > 500) {
            throw new IllegalArgumentException("portfolio description must be at most 500 characters");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("portfolio createdAt is required");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("portfolio updatedAt is required");
        }
        if (positions == null) {
            throw new IllegalArgumentException("portfolio positions are required");
        }

        name = name.trim();
        description = normalizeDescription(description);
        archiveReason = normalizeDescription(archiveReason);
        baseCurrency = normalizeBaseCurrency(baseCurrency);
        positions = List.copyOf(positions);
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    private static String normalizeBaseCurrency(String baseCurrency) {
        String normalized = baseCurrency;
        if (normalized == null || normalized.isBlank()) {
            normalized = DEFAULT_BASE_CURRENCY;
        }
        normalized = normalized.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("baseCurrency must be a 3-letter currency code");
        }
        return normalized;
    }
}
