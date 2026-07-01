package com.nexusxva.xva.api;

import com.nexusxva.xva.domain.Counterparty;
import java.time.Instant;
import java.util.UUID;

public record CounterpartyResponse(
        UUID id,
        String name,
        String externalId,
        String creditRating,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    static CounterpartyResponse from(Counterparty counterparty) {
        return new CounterpartyResponse(
                counterparty.id(),
                counterparty.name(),
                counterparty.externalId(),
                counterparty.creditRating(),
                counterparty.active(),
                counterparty.createdAt(),
                counterparty.updatedAt()
        );
    }
}
