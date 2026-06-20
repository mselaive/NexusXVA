package com.nexusxva.admin.api;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record UpdateUserGroupsRequest(
        @NotNull List<String> groups
) {
}
