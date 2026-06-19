package com.nexusxva.auth.api;

import com.nexusxva.auth.domain.AuthenticatedUser;
import java.util.List;
import java.util.UUID;

public record AuthUserResponse(
        UUID id,
        String username,
        String displayName,
        List<String> groups
) {

    static AuthUserResponse from(AuthenticatedUser user) {
        return new AuthUserResponse(user.id(), user.username(), user.displayName(), user.groups());
    }
}
