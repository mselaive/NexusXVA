package com.nexusxva.executescript.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusxva.executescript.domain.ExecuteScriptRun;
import com.nexusxva.executescript.domain.ExecuteScriptRunStatus;
import com.nexusxva.executescript.domain.ExecuteScriptStepStatus;
import com.nexusxva.executescript.domain.ExecuteScriptTemplate;
import com.nexusxva.executescript.domain.ExecuteScriptTemplateStep;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExecuteScriptStore {

    ExecuteScriptTemplate createTemplate(UUID templateId, SaveExecuteScriptTemplateCommand command, UUID updatedByUserId, Instant now);

    ExecuteScriptTemplate updateTemplate(UUID templateId, SaveExecuteScriptTemplateCommand command, UUID updatedByUserId, Instant now);

    Optional<ExecuteScriptTemplate> findTemplate(UUID templateId);

    List<ExecuteScriptTemplate> listTemplates(boolean includeInactive);

    void startRun(UUID runId, ExecuteScriptTemplate template, com.nexusxva.executescript.domain.ExecuteScriptMode mode, LocalDate businessDate, UUID requestedByUserId, JsonNode input, Instant startedAt);

    void createRunStep(UUID stepId, UUID runId, ExecuteScriptTemplateStep step);

    void markStepRunning(UUID stepId, Instant startedAt);

    void finishStep(UUID stepId, ExecuteScriptStepStatus status, String message, Object output);

    void completeRun(UUID runId, ExecuteScriptRunStatus status, String message);

    Optional<ExecuteScriptRun> findRun(UUID runId);

    List<ExecuteScriptRun> recentRuns(int limit);
}
