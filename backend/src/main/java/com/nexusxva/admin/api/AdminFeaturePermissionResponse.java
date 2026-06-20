package com.nexusxva.admin.api;

public record AdminFeaturePermissionResponse(
        String code,
        String groupCode,
        String name,
        String description,
        boolean effectiveEnabled,
        Boolean overrideEnabled
) {
}
