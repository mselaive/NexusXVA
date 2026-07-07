package com.nexusxva.xva.application;

public record UpdateCounterpartyCommand(
        String name,
        String externalId,
        String creditRating,
        boolean active
) {
    public UpdateCounterpartyCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("counterparty name is required");
        }
        name = name.trim();
        externalId = normalize(externalId);
        creditRating = normalize(creditRating);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
