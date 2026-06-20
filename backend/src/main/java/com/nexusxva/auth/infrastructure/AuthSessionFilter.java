package com.nexusxva.auth.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.auth.application.AuthProperties;
import com.nexusxva.auth.application.AuthService;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.shared.error.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnProperty(prefix = "nexusxva.auth", name = "enabled", havingValue = "true")
public class AuthSessionFilter extends OncePerRequestFilter {

    public static final String SESSION_ATTRIBUTE = "nexusxva.auth.session";
    static final String CSRF_HEADER = "X-CSRF-Token";

    private final AuthProperties properties;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    AuthSessionFilter(AuthProperties properties, AuthService authService, ObjectMapper objectMapper) {
        this.properties = properties;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled() || !request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<AuthSession> session = sessionToken(request).flatMap(authService::findSession);
        session.ifPresent(value -> request.setAttribute(SESSION_ATTRIBUTE, value));

        if (isPublic(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (session.isEmpty()) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Authentication required", request.getRequestURI());
            return;
        }

        if (requiresCsrf(request) && !authService.csrfMatches(session.get(), request.getHeader(CSRF_HEADER))) {
            writeError(response, HttpStatus.FORBIDDEN, "Invalid CSRF token", request.getRequestURI());
            return;
        }

        if (!isSessionManagement(request) && !isAuthorized(session.get(), request)) {
            writeError(response, HttpStatus.FORBIDDEN, authorizationMessage(session.get()), request.getRequestURI());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublic(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/me")
                || path.equals("/api/health");
    }

    private boolean requiresCsrf(HttpServletRequest request) {
        return !request.getMethod().equals("GET")
                && !request.getMethod().equals("HEAD")
                && !request.getMethod().equals("OPTIONS");
    }

    private boolean isSessionManagement(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/auth/active-group")
                || path.equals("/api/auth/logout");
    }

    private boolean isAuthorized(AuthSession session, HttpServletRequest request) {
        String activeGroup = session.activeGroup();
        if (activeGroup == null || activeGroup.isBlank()) {
            return false;
        }
        if (!session.user().groups().contains(activeGroup)) {
            return false;
        }

        String path = request.getRequestURI();
        return switch (activeGroup) {
            case "FO" -> path.startsWith("/api/portfolios")
                    || path.startsWith("/api/front-office")
                    || path.startsWith("/api/trade-bookings/mine")
                    || path.equals("/api/trading-limits/me")
                    || path.startsWith("/api/pricing")
                    || path.startsWith("/api/simulations")
                    || path.startsWith("/api/risk");
            case "BO" -> path.startsWith("/api/back-office/trade-bookings")
                    || path.startsWith("/api/back-office/trading-limits");
            case "ADMIN" -> path.startsWith("/api/admin");
            default -> false;
        };
    }

    private String authorizationMessage(AuthSession session) {
        if (session.activeGroup() == null || session.activeGroup().isBlank()) {
            return "Active group required";
        }
        return "Active group is not authorized for this resource";
    }

    private Optional<String> sessionToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> properties.getCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message, String path) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path,
                List.of(),
                java.util.Map.of()
        ));
    }
}
