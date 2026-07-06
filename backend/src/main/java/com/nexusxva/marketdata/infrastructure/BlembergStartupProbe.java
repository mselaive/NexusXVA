package com.nexusxva.marketdata.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(prefix = "nexusxva.market-data.blemberg", name = "startup-probe-enabled", havingValue = "true", matchIfMissing = true)
class BlembergStartupProbe implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlembergStartupProbe.class);
    private static final String ENDPOINT = "GET /actuator/health";

    private final RestClient blembergRestClient;
    private final BlembergProperties properties;

    BlembergStartupProbe(@Qualifier("blembergRestClient") RestClient blembergRestClient, BlembergProperties properties) {
        this.blembergRestClient = blembergRestClient;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        LOGGER.info("Blemberg startup probe started endpoint={} baseUrl={} timeout={}", ENDPOINT, properties.getBaseUrl(), properties.getTimeout());
        try {
            BlembergHealthResponse response = blembergRestClient.get()
                    .uri("/actuator/health")
                    .retrieve()
                    .body(BlembergHealthResponse.class);
            String status = response == null || response.status() == null ? "UNKNOWN" : response.status();
            LOGGER.info("Blemberg startup probe completed endpoint={} baseUrl={} status={}", ENDPOINT, properties.getBaseUrl(), status);
        } catch (HttpStatusCodeException exception) {
            LOGGER.warn(
                    "Blemberg startup probe returned error endpoint={} baseUrl={} status={}",
                    ENDPOINT,
                    properties.getBaseUrl(),
                    exception.getStatusCode().value()
            );
        } catch (ResourceAccessException exception) {
            LOGGER.warn(
                    "Blemberg startup probe failed endpoint={} baseUrl={} reason={}",
                    ENDPOINT,
                    properties.getBaseUrl(),
                    exception.getClass().getSimpleName()
            );
        } catch (RestClientException exception) {
            LOGGER.warn(
                    "Blemberg startup probe failed endpoint={} baseUrl={} reason={}",
                    ENDPOINT,
                    properties.getBaseUrl(),
                    exception.getClass().getSimpleName()
            );
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record BlembergHealthResponse(String status) {
    }
}
