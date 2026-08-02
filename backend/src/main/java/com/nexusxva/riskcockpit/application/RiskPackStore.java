package com.nexusxva.riskcockpit.application;
import com.fasterxml.jackson.databind.JsonNode;
import com.nexusxva.riskcockpit.domain.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface RiskPackStore {
    RiskPackRun create(UUID runId, UUID portfolioId, LocalDate valuationDate, UUID userId, String username,
                       String group, Instant portfolioUpdatedAt, JsonNode configuration, Instant now);
    Optional<RiskPackRun> find(UUID runId);
    Optional<RiskPackRun> latest(UUID portfolioId);
    List<RiskPackRun> recent(UUID portfolioId, int limit);
    boolean hasActiveRun(UUID portfolioId);
    void markRunRunning(UUID runId, Instant now);
    void completeRun(UUID runId, RiskPackRunStatus status, Instant marketDataAsOf, String error, Instant now);
    void markComponentRunning(UUID runId, RiskPackComponentType type, Instant now);
    void completeComponent(UUID runId, RiskPackComponentType type, RiskPackComponentStatus status, JsonNode output, String error, Instant now);
    int failAbandonedRuns(Instant now);
}
