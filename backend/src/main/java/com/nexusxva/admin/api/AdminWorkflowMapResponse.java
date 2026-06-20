package com.nexusxva.admin.api;

import java.util.List;
import java.util.UUID;

public record AdminWorkflowMapResponse(
        UUID portfolioId,
        List<AdminWorkflowNodeResponse> nodes,
        List<AdminWorkflowLinkResponse> links
) {
}
