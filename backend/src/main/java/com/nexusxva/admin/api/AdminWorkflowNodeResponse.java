package com.nexusxva.admin.api;

import java.util.List;

public record AdminWorkflowNodeResponse(
        String id,
        String label,
        String description,
        long count,
        List<AdminWorkflowBookingResponse> bookings
) {
}
