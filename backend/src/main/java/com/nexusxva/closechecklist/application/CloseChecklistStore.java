package com.nexusxva.closechecklist.application;

import com.nexusxva.closechecklist.domain.CloseChecklistRun;
import com.nexusxva.closechecklist.domain.CloseChecklistRunStatus;
import com.nexusxva.closechecklist.domain.CloseChecklistStepStatus;
import com.nexusxva.operationalcontrol.domain.CloseChecklistSettings;
import com.nexusxva.operationalcontrol.domain.CloseChecklistStepDefinition;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CloseChecklistStore {
    void startRun(UUID runId, LocalDate businessDate, String source, UUID requestedByUserId, CloseChecklistSettings settings, Instant startedAt);
    boolean tryStartScheduledRun(UUID runId, LocalDate businessDate, CloseChecklistSettings settings, Instant startedAt);
    void createStep(UUID stepId, UUID runId, CloseChecklistStepDefinition definition);
    void markStepRunning(UUID stepId, Instant startedAt);
    void completeStep(UUID stepId, String message, Object output);
    void failStep(UUID stepId, String message, Object output);
    void skipStep(UUID stepId, String message, Object output);
    void completeRun(UUID runId, CloseChecklistRunStatus status, String message);
    Optional<CloseChecklistRun> find(UUID runId);
    List<CloseChecklistRun> recent(int limit);
    Optional<LocalDate> latestScheduledBusinessDate();
}
