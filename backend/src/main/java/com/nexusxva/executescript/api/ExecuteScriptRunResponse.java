package com.nexusxva.executescript.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusxva.executescript.domain.ExecuteScriptMode;
import com.nexusxva.executescript.domain.ExecuteScriptRun;
import com.nexusxva.executescript.domain.ExecuteScriptRunStatus;
import com.nexusxva.executescript.domain.ExecuteScriptRunStep;
import com.nexusxva.executescript.domain.ExecuteScriptStepStatus;
import com.nexusxva.executescript.domain.ExecuteScriptStepType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExecuteScriptRunResponse(
        UUID id,
        UUID templateId,
        String templateName,
        ExecuteScriptMode mode,
        LocalDate businessDate,
        ExecuteScriptRunStatus status,
        Instant startedAt,
        Instant completedAt,
        UUID requestedByUserId,
        String message,
        JsonNode input,
        List<Step> steps
) {
    public record Step(
            UUID id,
            ExecuteScriptStepType stepType,
            int order,
            boolean critical,
            ExecuteScriptStepStatus status,
            Instant startedAt,
            Instant completedAt,
            String message,
            JsonNode output
    ) {
        static Step from(ExecuteScriptRunStep step) {
            return new Step(
                    step.id(),
                    step.stepType(),
                    step.order(),
                    step.critical(),
                    step.status(),
                    step.startedAt(),
                    step.completedAt(),
                    step.message(),
                    step.output()
            );
        }
    }

    static ExecuteScriptRunResponse from(ExecuteScriptRun run) {
        return new ExecuteScriptRunResponse(
                run.id(),
                run.templateId(),
                run.templateName(),
                run.mode(),
                run.businessDate(),
                run.status(),
                run.startedAt(),
                run.completedAt(),
                run.requestedByUserId(),
                run.message(),
                run.input(),
                run.steps().stream().map(Step::from).toList()
        );
    }
}
