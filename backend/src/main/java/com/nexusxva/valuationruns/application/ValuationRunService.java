package com.nexusxva.valuationruns.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.valuationruns.domain.ValuationRun;
import com.nexusxva.valuationruns.domain.ValuationRunStatus;
import com.nexusxva.valuationruns.domain.ValuationRunType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ValuationRunService {

    private static final int ERROR_LIMIT = 1000;

    private final ValuationRunStore store;
    private final ObjectMapper objectMapper;

    public ValuationRunService(ValuationRunStore store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ValuationRun recordSuccess(
            AuthSession session,
            UUID portfolioId,
            ValuationRunType runType,
            String model,
            LocalDate valuationDate,
            Object input,
            Object result,
            Object summary
    ) {
        return store.save(new ValuationRun(
                UUID.randomUUID(),
                portfolioId,
                null,
                runType,
                model,
                valuationDate,
                ValuationRunStatus.SUCCESS,
                userId(session),
                username(session),
                displayName(session),
                activeGroup(session),
                toJson(input),
                toJson(result),
                toJson(summary),
                null,
                Instant.now()
        ));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ValuationRun recordFailure(
            AuthSession session,
            UUID portfolioId,
            ValuationRunType runType,
            String model,
            LocalDate valuationDate,
            Object input,
            RuntimeException exception
    ) {
        return store.save(new ValuationRun(
                UUID.randomUUID(),
                portfolioId,
                null,
                runType,
                model,
                valuationDate,
                ValuationRunStatus.FAILED,
                userId(session),
                username(session),
                displayName(session),
                activeGroup(session),
                toJson(input),
                null,
                null,
                sanitize(exception),
                Instant.now()
        ));
    }

    @Transactional(readOnly = true)
    public ValuationRun get(UUID runId) {
        return store.find(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Valuation run not found"));
    }

    @Transactional(readOnly = true)
    public List<ValuationRun> search(ValuationRunSearchCriteria criteria) {
        return store.search(criteria);
    }

    private JsonNode toJson(Object value) {
        return objectMapper.valueToTree(value);
    }

    private UUID userId(AuthSession session) {
        return session == null ? null : session.user().id();
    }

    private String username(AuthSession session) {
        return session == null ? null : session.user().username();
    }

    private String displayName(AuthSession session) {
        return session == null ? null : session.user().displayName();
    }

    private String activeGroup(AuthSession session) {
        return session == null ? null : session.activeGroup();
    }

    private String sanitize(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }
        return message.length() > ERROR_LIMIT ? message.substring(0, ERROR_LIMIT) : message;
    }
}
