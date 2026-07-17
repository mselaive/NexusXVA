package com.nexusxva.xva.api;

import com.nexusxva.xva.application.SaveDiscountCurveCommand;
import com.nexusxva.xva.domain.DiscountCurve;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record SaveDiscountCurveRequest(
        @NotBlank String name,
        @NotBlank String currency,
        boolean active,
        @NotEmpty List<@Valid PointRequest> points
) {
    SaveDiscountCurveCommand toCommand() {
        return new SaveDiscountCurveCommand(
                name,
                currency,
                active,
                points.stream().map(PointRequest::toDomain).toList()
        );
    }

    public record PointRequest(
            @NotNull LocalDate date,
            @DecimalMin("0.0") @DecimalMax("1.0") double discountFactor
    ) {
        DiscountCurve.Point toDomain() {
            return new DiscountCurve.Point(date, discountFactor);
        }
    }
}
