package com.nexusxva.auth.api;

import com.nexusxva.audit.application.AuditEventCommand;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.auth.application.AuthProperties;
import com.nexusxva.auth.application.AuthResult;
import com.nexusxva.auth.application.AuthService;
import com.nexusxva.auth.application.InvalidCredentialsException;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.shared.error.AccessDeniedException;
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
    private final AuditService auditService;

    public AuthController(AuthProperties properties, AuthService authService, AuditService auditService) {
        this.properties = properties;
        this.authService = authService;
        this.auditService = auditService;
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response
    ) {
        if (!properties.isEnabled()) {
            return AuthResponse.disabled();
        }
        AuthResult result;
        try {
            result = authService.login(request.username(), request.password());
        } catch (InvalidCredentialsException exception) {
            auditService.record(new AuditEventCommand(
                    "AUTH_LOGIN_FAILURE",
                    "AUTH",
                    "LOGIN",
                    AuditOutcome.FAILURE,
                    null,
                    servletRequest,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "USER",
                    null,
                    "Invalid login attempt",
                    auditService.metadata(java.util.Map.of("username", request.username()))
            ));
            throw exception;
        }
        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie(result.sessionToken(), properties.getSessionHours() * 60L * 60L).toString());
        AuthSession auditSession = new AuthSession(result.sessionId(), result.user(), null, result.csrfToken(), result.expiresAt());
        auditService.record(AuditEventCommand.of(
                "AUTH_LOGIN_SUCCESS",
                "AUTH",
                "LOGIN",
                AuditOutcome.SUCCESS,
                auditSession,
                servletRequest,
                HttpServletResponse.SC_OK,
                "USER",
                result.user().id(),
                "User logged in",
                auditService.metadata(java.util.Map.of("username", result.user().username()))
        ));
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
        AuthSession updated;
        try {
            updated = authService.selectActiveGroup(session, request.group());
        } catch (AccessDeniedException exception) {
            auditService.record(new AuditEventCommand(
                    "AUTH_ACTIVE_GROUP_FAILURE",
                    "AUTH",
                    "SELECT_ACTIVE_GROUP",
                    AuditOutcome.FAILURE,
                    session,
                    servletRequest,
                    HttpServletResponse.SC_FORBIDDEN,
                    "USER",
                    session.user().id().toString(),
                    "User attempted to select an unassigned group",
                    auditService.metadata(java.util.Map.of("requestedGroup", request.group()))
            ));
            throw exception;
        }
        auditService.record(AuditEventCommand.of(
                "AUTH_ACTIVE_GROUP_CHANGED",
                "AUTH",
                "SELECT_ACTIVE_GROUP",
                AuditOutcome.SUCCESS,
                updated,
                servletRequest,
                HttpServletResponse.SC_OK,
                "USER",
                updated.user().id(),
                "Active group changed",
                auditService.metadata(java.util.Map.of("activeGroup", updated.activeGroup()))
        ));
        return AuthResponse.authenticated(
                AuthUserResponse.from(updated.user()),
                updated.activeGroup(),
                updated.csrfToken()
        );
    }

    @PostMapping("/logout")
    public AuthResponse logout(HttpServletRequest request, HttpServletResponse response) {
        Optional<AuthSession> session = currentSession(request);
        sessionToken(request).ifPresent(authService::logout);
        session.ifPresent(value -> auditService.record(AuditEventCommand.of(
                "AUTH_LOGOUT",
                "AUTH",
                "LOGOUT",
                AuditOutcome.SUCCESS,
                value,
                request,
                HttpServletResponse.SC_OK,
                "USER",
                value.user().id(),
                "User logged out",
                null
        )));
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
