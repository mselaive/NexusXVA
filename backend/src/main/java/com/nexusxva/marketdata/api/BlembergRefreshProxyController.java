package com.nexusxva.marketdata.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nexusxva.shared.error.ServiceUnavailableException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@RestController
@RequestMapping("/api/market-data/blemberg")
public class BlembergRefreshProxyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlembergRefreshProxyController.class);
    private static final String ENDPOINT = "POST /api/admin/market-data/refresh";
    private static final int MAX_LOG_BODY_LENGTH = 500;

    private final RestClient blembergRestClient;

    public BlembergRefreshProxyController(@Qualifier("blembergRefreshRestClient") RestClient blembergRestClient) {
        this.blembergRestClient = blembergRestClient;
    }

    @PostMapping("/refresh")
    public BlembergRefreshResponse refresh(@Valid @RequestBody(required = false) BlembergRefreshRequest request) {
        BlembergRefreshRequest normalized = request == null ? new BlembergRefreshRequest(List.of()) : request.normalized();
        LOGGER.info("Blemberg refresh requested endpoint={} prioritySymbols={}", ENDPOINT, normalized.prioritySymbols());
        try {
            BlembergRefreshResponse response = blembergRestClient.post()
                    .uri("/api/admin/market-data/refresh")
                    .body(normalized)
                    .retrieve()
                    .body(BlembergRefreshResponse.class);
            BlembergRefreshResponse safeResponse = response == null ? BlembergRefreshResponse.empty() : response;
            LOGGER.info(
                    "Blemberg refresh completed endpoint={} status={} requestedSymbols={} attemptedSymbols={} succeededSymbols={} skippedRateLimitSymbols={} missingSnapshotSymbols={} pricingNotReadySymbols={}",
                    ENDPOINT,
                    safeResponse.status(),
                    safeResponse.requestedSymbols(),
                    safeResponse.attemptedSymbols(),
                    safeResponse.succeededSymbols(),
                    safeResponse.skippedRateLimitSymbols(),
                    safeResponse.missingSnapshotSymbols(),
                    safeResponse.pricingNotReadySymbols()
            );
            if (!safeResponse.skippedRateLimitSymbols().isEmpty()
                    || !safeResponse.missingSnapshotSymbols().isEmpty()
                    || !safeResponse.pricingNotReadySymbols().isEmpty()) {
                LOGGER.warn(
                        "Blemberg refresh incomplete endpoint={} skippedRateLimitSymbols={} missingSnapshotSymbols={} pricingNotReadySymbols={}",
                        ENDPOINT,
                        safeResponse.skippedRateLimitSymbols(),
                        safeResponse.missingSnapshotSymbols(),
                        safeResponse.pricingNotReadySymbols()
                );
            }
            return safeResponse;
        } catch (HttpStatusCodeException exception) {
            LOGGER.warn(
                    "Blemberg refresh returned error endpoint={} status={} body={}",
                    ENDPOINT,
                    exception.getStatusCode().value(),
                    sanitizedBody(exception.getResponseBodyAsString())
            );
            throw new ServiceUnavailableException("Market data service unavailable");
        } catch (ResourceAccessException exception) {
            LOGGER.warn(
                    "Blemberg refresh failed endpoint={} reason={} message={}",
                    ENDPOINT,
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
            throw new ServiceUnavailableException("Market data service unavailable");
        } catch (RestClientException exception) {
            LOGGER.warn(
                    "Blemberg refresh failed endpoint={} reason={} message={}",
                    ENDPOINT,
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
            throw new ServiceUnavailableException("Market data service unavailable");
        }
    }

    private String sanitizedBody(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String oneLine = body.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= MAX_LOG_BODY_LENGTH ? oneLine : oneLine.substring(0, MAX_LOG_BODY_LENGTH) + "...";
    }

    public record BlembergRefreshRequest(
            @Size(max = 100) List<String> prioritySymbols
    ) {
        BlembergRefreshRequest normalized() {
            if (prioritySymbols == null) {
                return new BlembergRefreshRequest(List.of());
            }
            return new BlembergRefreshRequest(prioritySymbols.stream()
                    .filter(symbol -> symbol != null && !symbol.isBlank())
                    .map(symbol -> symbol.trim().toUpperCase())
                    .distinct()
                    .limit(100)
                    .toList());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BlembergRefreshResponse(
            String runId,
            String status,
            Integer symbolsRequested,
            Integer symbolsSucceeded,
            Integer symbolsFailed,
            List<String> requestedSymbols,
            List<String> attemptedSymbols,
            List<String> succeededSymbols,
            List<String> skippedRateLimitSymbols,
            List<String> missingSnapshotSymbols,
            List<String> pricingNotReadySymbols,
            List<BlembergRefreshJobSummary> jobSummaries,
            List<BlembergRefreshError> errors
    ) {
        static BlembergRefreshResponse empty() {
            return new BlembergRefreshResponse(
                    null,
                    "UNKNOWN",
                    null,
                    null,
                    null,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        public BlembergRefreshResponse {
            requestedSymbols = requestedSymbols == null ? List.of() : requestedSymbols;
            attemptedSymbols = attemptedSymbols == null ? List.of() : attemptedSymbols;
            succeededSymbols = succeededSymbols == null ? List.of() : succeededSymbols;
            skippedRateLimitSymbols = skippedRateLimitSymbols == null ? List.of() : skippedRateLimitSymbols;
            missingSnapshotSymbols = missingSnapshotSymbols == null ? List.of() : missingSnapshotSymbols;
            pricingNotReadySymbols = pricingNotReadySymbols == null ? List.of() : pricingNotReadySymbols;
            jobSummaries = jobSummaries == null ? List.of() : jobSummaries;
            errors = errors == null ? List.of() : errors;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BlembergRefreshJobSummary(
            String jobName,
            Integer requested,
            Integer succeeded,
            Integer failed,
            Integer skippedRateLimit
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BlembergRefreshError(
            String jobName,
            String provider,
            String symbol,
            String status,
            String errorCode,
            String message
    ) {
    }
}
