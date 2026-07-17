package com.nexusxva.executescript.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusxva.executescript.domain.ExecuteScriptStepType;
import java.util.List;

public record SaveExecuteScriptTemplateCommand(
        String name,
        String description,
        boolean active,
        JsonNode defaultParameters,
        List<Step> steps
) {
    public record Step(
            ExecuteScriptStepType stepType,
            int order,
            boolean critical,
            boolean enabled,
            JsonNode parameters
    ) {
    }
}
