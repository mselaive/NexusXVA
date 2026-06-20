package com.nexusxva.admin.api;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record UpdateUserPermissionsRequest(
        @NotNull Map<String, Boolean> permissions
) {
}
