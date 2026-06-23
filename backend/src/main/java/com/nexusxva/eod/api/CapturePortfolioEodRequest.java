package com.nexusxva.eod.api;

import java.time.LocalDate;

public record CapturePortfolioEodRequest(
        LocalDate businessDate
) {
}
