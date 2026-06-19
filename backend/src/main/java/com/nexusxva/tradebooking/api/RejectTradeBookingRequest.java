package com.nexusxva.tradebooking.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectTradeBookingRequest(
        @NotBlank @Size(max = 500) String rejectionReason
) {
}

