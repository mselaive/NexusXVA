package com.nexusxva.auth.api;

import com.nexusxva.auth.application.AuthProperties;
import com.nexusxva.auth.application.AuthResult;
import com.nexusxva.auth.application.AuthService;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthProperties properties;
    private final AuthService authService;

    public AuthController(AuthProperties properties, AuthService authService) {
        this.properties = properties;
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        if (!properties.isEnabled()) {
            return AuthResponse.disabled();
        }
        AuthResult result = authService.login(request.username(), request.password());
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(result.sessionToken(), properties.getSessionHours() * 60L * 60L).toString());
        return AuthResponse.authenticated(AuthUserResponse.from(result.user()), null, result.csrfToken());
    }

    @GetMapping("/me")
    public AuthResponse me(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return AuthResponse.disabled();
        }
        return currentSession(request)
                .map(session -> AuthResponse.authenticated(
                        AuthUserResponse.from(session.user()),
                        session.activeGroup(),
                        session.csrfToken()
                ))
                .orElseGet(AuthResponse::anonymous);
    }

    @PostMapping("/active-group")
    public AuthResponse selectActiveGroup(
            @Valid @RequestBody ActiveGroupRequest request,
            HttpServletRequest servletRequest
    ) {
        AuthSession session = currentSession(servletRequest)
                .orElseThrow(() -> new IllegalStateException("Authentication required"));
        AuthSession updated = authService.selectActiveGroup(session, request.group());
        return AuthResponse.authenticated(
                AuthUserResponse.from(updated.user()),
                updated.activeGroup(),
                updated.csrfToken()
        );
    }

    @PostMapping("/logout")
    public AuthResponse logout(HttpServletRequest request, HttpServletResponse response) {
        sessionToken(request).ifPresent(authService::logout);
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie("", 0).toString());
        return properties.isEnabled() ? AuthResponse.anonymous() : AuthResponse.disabled();
    }

    private Optional<AuthSession> currentSession(HttpServletRequest request) {
        Object session = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        if (session instanceof AuthSession authSession) {
            return Optional.of(authSession);
        }
        return Optional.empty();
    }

    private Optional<String> sessionToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> properties.getCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private ResponseCookie sessionCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(properties.getCookieName(), token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
