package com.nexusxva.admin.api;

import java.util.List;

public record AdminUserPageResponse(
        List<AdminUserAccessResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
