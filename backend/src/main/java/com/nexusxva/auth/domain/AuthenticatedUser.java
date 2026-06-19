package com.nexusxva.auth.domain;

import java.util.List;
import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String username,
        String displayName,
        List<String> groups
) {
}
