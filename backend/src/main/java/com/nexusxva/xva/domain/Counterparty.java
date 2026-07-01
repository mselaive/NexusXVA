package com.nexusxva.xva.domain;

import java.time.Instant;
import java.util.UUID;

public record Counterparty(
        UUID id,
        String name,
        String externalId,
        String creditRating,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
