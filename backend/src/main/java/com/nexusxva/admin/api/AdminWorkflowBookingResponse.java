package com.nexusxva.admin.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdminWorkflowBookingResponse(
        UUID id,
        UUID portfolioId,
        String portfolioName,
        String node,
        String status,
        String underlyingSymbol,
        String optionType,
        BigDecimal strike,
        LocalDate maturityDate,
        BigDecimal quantity,
        String submittedBy,
        Instant submittedAt,
        String reviewedBy,
        Instant reviewedAt,
        String rejectionReason,
        UUID confirmedPositionId
) {
}
