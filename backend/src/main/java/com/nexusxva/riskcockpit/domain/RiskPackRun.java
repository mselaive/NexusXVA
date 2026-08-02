package com.nexusxva.riskcockpit.domain;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
public record RiskPackRun(UUID id, UUID portfolioId, LocalDate valuationDate, RiskPackRunStatus status,
                          UUID requestedByUserId, String requestedByUsername, String requestedByGroup,
                          Instant portfolioUpdatedAt, Instant marketDataAsOf, JsonNode configuration,
                          Instant queuedAt, Instant startedAt, Instant completedAt, String errorMessage,
                          List<RiskPackComponent> components) {
    public boolean terminal() { return status == RiskPackRunStatus.SUCCESS || status == RiskPackRunStatus.PARTIAL || status == RiskPackRunStatus.FAILED; }
}
