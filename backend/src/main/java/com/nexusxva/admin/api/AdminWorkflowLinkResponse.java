package com.nexusxva.admin.api;

public record AdminWorkflowLinkResponse(
        String from,
        String to,
        long count
) {
}
