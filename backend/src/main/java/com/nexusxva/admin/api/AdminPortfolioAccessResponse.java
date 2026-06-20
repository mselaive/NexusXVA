package com.nexusxva.admin.api;

import java.util.List;

public record AdminPortfolioAccessResponse(
        String accessMode,
        List<AdminPortfolioSummaryResponse> portfolios
) {
}
