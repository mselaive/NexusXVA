package com.nexusxva.xva.api;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignPortfolioToNettingSetRequest(@NotNull UUID portfolioId) {
}
