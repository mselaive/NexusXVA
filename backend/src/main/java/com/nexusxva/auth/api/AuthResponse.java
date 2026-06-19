package com.nexusxva.auth.api;

public record AuthResponse(
        boolean enabled,
        boolean authenticated,
        AuthUserResponse user,
        String activeGroup,
        String csrfToken
) {

    static AuthResponse disabled() {
        return new AuthResponse(false, true, null, null, null);
    }

    static AuthResponse anonymous() {
        return new AuthResponse(true, false, null, null, null);
    }

    static AuthResponse authenticated(AuthUserResponse user, String activeGroup, String csrfToken) {
        return new AuthResponse(true, true, user, activeGroup, csrfToken);
    }
}
