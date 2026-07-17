package com.nexusxva.executescript.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExecuteScriptTemplate(
        UUID id,
        String name,
        String description,
        boolean active,
        JsonNode defaultParameters,
        Instant createdAt,
        Instant updatedAt,
        UUID updatedByUserId,
        List<ExecuteScriptTemplateStep> steps
) {
}
