package com.nexusxva.xva.api;

import com.nexusxva.xva.application.SaveCreditCurveCommand;
import com.nexusxva.xva.domain.CreditCurve;
import com.nexusxva.xva.domain.CreditCurveType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SaveCreditCurveRequest(
        @NotNull UUID counterpartyId,
        @NotBlank String name,
        @NotNull CreditCurveType curveType,
        boolean active,
        @NotEmpty List<@Valid PointRequest> points
) {
    SaveCreditCurveCommand toCommand() {
        return new SaveCreditCurveCommand(
                counterpartyId,
                name,
                curveType,
                active,
                points.stream().map(PointRequest::toDomain).toList()
        );
    }

    public record PointRequest(
            @NotNull LocalDate date,
            Double survivalProbability,
            Double cumulativeDefaultProbability
    ) {
        CreditCurve.Point toDomain() {
            return new CreditCurve.Point(date, survivalProbability, cumulativeDefaultProbability);
        }
    }
}
