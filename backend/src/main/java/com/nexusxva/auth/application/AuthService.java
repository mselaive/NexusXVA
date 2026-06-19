package com.nexusxva.auth.application;

import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.domain.AuthenticatedUser;
import com.nexusxva.auth.infrastructure.JdbcAuthStore;
import com.nexusxva.shared.error.AccessDeniedException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JdbcAuthStore authStore;
    private final AuthProperties properties;
    private final AuthTokenHasher tokenHasher;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final SecureRandom secureRandom = new SecureRandom();
    private final Clock clock = Clock.systemUTC();

    public AuthService(JdbcAuthStore authStore, AuthProperties properties, AuthTokenHasher tokenHasher) {
        this.authStore = authStore;
        this.properties = properties;
        this.tokenHasher = tokenHasher;
    }

    public AuthResult login(String username, String password) {
        JdbcAuthStore.StoredUser user = authStore.findStoredUserByUsername(username)
                .filter(JdbcAuthStore.StoredUser::active)
                .filter(stored -> passwordEncoder.matches(password, stored.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);

        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(properties.getSessionHours(), ChronoUnit.HOURS);
        String sessionToken = randomToken();
        String csrfToken = randomToken();
        authStore.createSession(UUID.randomUUID(), user.id(), tokenHasher.hash(sessionToken), csrfToken, now, expiresAt);

        return new AuthResult(
                new AuthenticatedUser(user.id(), user.username(), user.displayName(), user.groups()),
                sessionToken,
                csrfToken,
                expiresAt
        );
    }

    public Optional<AuthSession> findSession(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return Optional.empty();
        }
        return authStore.findSessionByTokenHash(tokenHasher.hash(sessionToken));
    }

    public boolean csrfMatches(AuthSession session, String csrfToken) {
        if (csrfToken == null || csrfToken.isBlank()) {
            return false;
        }
        return session.csrfToken().equals(csrfToken);
    }

    public void logout(String sessionToken) {
        if (sessionToken != null && !sessionToken.isBlank()) {
            authStore.revokeSession(tokenHasher.hash(sessionToken));
        }
    }

    public AuthSession selectActiveGroup(AuthSession session, String group) {
        String normalizedGroup = group == null ? "" : group.trim().toUpperCase();
        if (!session.user().groups().contains(normalizedGroup)) {
            throw new AccessDeniedException("User is not assigned to the requested group");
        }
        authStore.updateActiveGroup(session.id(), normalizedGroup);
        return new AuthSession(
                session.id(),
                session.user(),
                normalizedGroup,
                session.csrfToken(),
                session.expiresAt()
        );
    }

    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
