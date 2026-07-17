package com.nexusxva.xva.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectCurveRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
