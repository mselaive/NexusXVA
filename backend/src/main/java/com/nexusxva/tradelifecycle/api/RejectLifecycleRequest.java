package com.nexusxva.tradelifecycle.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectLifecycleRequest(
        @NotBlank @Size(max = 500) String rejectionReason
) {
}
