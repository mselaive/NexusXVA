package com.nexusxva.executescript.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusxva.executescript.domain.ExecuteScriptMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RunExecuteScriptCommand(
        UUID templateId,
        ExecuteScriptMode mode,
        LocalDate businessDate,
        List<UUID> portfolioIds,
        JsonNode parameters
) {
}
