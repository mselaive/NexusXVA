package com.nexusxva.auth.application;

import com.nexusxva.auth.domain.AuthenticatedUser;
import java.time.Instant;
import java.util.UUID;

public record AuthResult(
        AuthenticatedUser user,
        UUID sessionId,
        String sessionToken,
        String csrfToken,
        Instant expiresAt
) {
}
