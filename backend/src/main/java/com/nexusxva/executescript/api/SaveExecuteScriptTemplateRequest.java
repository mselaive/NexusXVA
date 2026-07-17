package com.nexusxva.executescript.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusxva.executescript.application.SaveExecuteScriptTemplateCommand;
import com.nexusxva.executescript.domain.ExecuteScriptStepType;
import java.util.List;

public record SaveExecuteScriptTemplateRequest(
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
        SaveExecuteScriptTemplateCommand.Step toCommand() {
            return new SaveExecuteScriptTemplateCommand.Step(stepType, order, critical, enabled, parameters);
        }
    }

    SaveExecuteScriptTemplateCommand toCommand() {
        return new SaveExecuteScriptTemplateCommand(
                name,
                description,
                active,
                defaultParameters,
                steps == null ? List.of() : steps.stream().map(Step::toCommand).toList()
        );
    }
}
