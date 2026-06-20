package com.nexusxva.admin.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record UpdateUserPortfolioAccessRequest(
        @NotBlank String accessMode,
        @NotNull List<UUID> portfolioIds
) {
}
