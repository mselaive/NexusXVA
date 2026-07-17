package com.nexusxva.executescript.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusxva.executescript.domain.ExecuteScriptStepType;
import com.nexusxva.executescript.domain.ExecuteScriptTemplate;
import com.nexusxva.executescript.domain.ExecuteScriptTemplateStep;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExecuteScriptTemplateResponse(
        UUID id,
        String name,
        String description,
        boolean active,
        JsonNode defaultParameters,
        Instant createdAt,
        Instant updatedAt,
        UUID updatedByUserId,
        List<Step> steps
) {
    public record Step(
            UUID id,
            ExecuteScriptStepType stepType,
            int order,
            boolean critical,
            boolean enabled,
            JsonNode parameters
    ) {
        static Step from(ExecuteScriptTemplateStep step) {
            return new Step(step.id(), step.stepType(), step.order(), step.critical(), step.enabled(), step.parameters());
        }
    }

    static ExecuteScriptTemplateResponse from(ExecuteScriptTemplate template) {
        return new ExecuteScriptTemplateResponse(
                template.id(),
                template.name(),
                template.description(),
                template.active(),
                template.defaultParameters(),
                template.createdAt(),
                template.updatedAt(),
                template.updatedByUserId(),
                template.steps().stream().map(Step::from).toList()
        );
    }
}
