package com.nexusxva.auth.domain;

import java.time.Instant;
import java.util.UUID;

public record AuthSession(
        UUID id,
        AuthenticatedUser user,
        String activeGroup,
        String csrfToken,
        Instant expiresAt
) {
}
