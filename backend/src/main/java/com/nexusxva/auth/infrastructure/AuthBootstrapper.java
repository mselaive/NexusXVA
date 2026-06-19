package com.nexusxva.auth.infrastructure;

import com.nexusxva.auth.application.AuthProperties;
import com.nexusxva.auth.application.AuthService;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
class AuthBootstrapper implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthBootstrapper.class);

    private final AuthProperties properties;
    private final AuthService authService;
    private final JdbcAuthStore authStore;

    AuthBootstrapper(AuthProperties properties, AuthService authService, JdbcAuthStore authStore) {
        this.properties = properties;
        this.authService = authService;
        this.authStore = authStore;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        AuthProperties.BootstrapAdmin admin = properties.getBootstrapAdmin();
        if (admin.getPassword() == null || admin.getPassword().isBlank()) {
            LOGGER.warn("NexusXVA auth is enabled but bootstrap admin password is empty; no admin user was created");
            return;
        }
        if (authStore.userExists(admin.getUsername())) {
            return;
        }

        authStore.createBootstrapAdmin(
                UUID.randomUUID(),
                admin.getUsername().trim(),
                admin.getDisplayName().trim(),
                authService.encodePassword(admin.getPassword()),
                Instant.now()
        );
        LOGGER.warn("Created bootstrap admin user '{}'. Change the bootstrap password before sharing this environment.", admin.getUsername());
    }
}
