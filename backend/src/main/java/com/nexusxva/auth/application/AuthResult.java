package com.nexusxva.auth.application;

import com.nexusxva.auth.domain.AuthenticatedUser;
import java.time.Instant;

public record AuthResult(
        AuthenticatedUser user,
        String sessionToken,
        String csrfToken,
        Instant expiresAt
) {
}
