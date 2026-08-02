package com.nexusxva.riskcockpit.domain;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;
public record RiskPackComponent(UUID id, RiskPackComponentType type, RiskPackComponentStatus status,
                                Instant startedAt, Instant completedAt, Long durationMs,
                                JsonNode output, String errorMessage) {}
