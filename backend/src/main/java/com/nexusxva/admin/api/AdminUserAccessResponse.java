package com.nexusxva.admin.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AdminUserAccessResponse(
        UUID id,
        String username,
        String displayName,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        Instant lastLoginAt,
        List<String> groups,
        List<AdminFeaturePermissionResponse> permissions,
        AdminPortfolioAccessResponse portfolioAccess
) {
}
