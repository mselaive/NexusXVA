package com.nexusxva.executescript.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusxva.executescript.application.RunExecuteScriptCommand;
import com.nexusxva.executescript.domain.ExecuteScriptMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RunExecuteScriptRequest(
        UUID templateId,
        ExecuteScriptMode mode,
        LocalDate businessDate,
        List<UUID> portfolioIds,
        JsonNode parameters
) {
    RunExecuteScriptCommand toCommand() {
        return new RunExecuteScriptCommand(templateId, mode, businessDate, portfolioIds, parameters);
    }
}
