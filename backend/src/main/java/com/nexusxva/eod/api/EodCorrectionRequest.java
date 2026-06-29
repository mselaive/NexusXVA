package com.nexusxva.eod.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EodCorrectionRequest(
        @NotBlank(message = "reason is required")
        @Size(max = 500, message = "reason must be at most 500 characters")
        String reason
) {
}
