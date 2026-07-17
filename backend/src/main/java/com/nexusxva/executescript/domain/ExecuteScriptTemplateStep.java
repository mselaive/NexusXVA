package com.nexusxva.executescript.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public record ExecuteScriptTemplateStep(
        UUID id,
        UUID templateId,
        ExecuteScriptStepType stepType,
        int order,
        boolean critical,
        boolean enabled,
        JsonNode parameters
) {
}
