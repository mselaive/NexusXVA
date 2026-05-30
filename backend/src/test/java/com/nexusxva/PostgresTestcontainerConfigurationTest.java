package com.nexusxva;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

class PostgresTestcontainerConfigurationTest {

    @Test
    void postgresTestcontainerCanBeDeclaredForFutureIntegrationTests() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")) {
            assertThat(postgres.getDockerImageName()).contains("postgres");
        }
    }
}
