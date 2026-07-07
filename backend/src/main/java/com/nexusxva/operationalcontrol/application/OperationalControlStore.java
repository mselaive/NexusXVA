package com.nexusxva.operationalcontrol.application;

import com.nexusxva.operationalcontrol.domain.OperationalControlSettings;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OperationalControlStore {

    OperationalControlSettings settings();

    OperationalControlSettings save(OperationalControlSettings settings, UUID updatedByUserId);

    boolean tryStartScheduledEod(UUID runId, LocalDate businessDate, Instant startedAt);

    void completeScheduledEod(UUID runId, int captured, int skipped, int failed, String message);

    void failScheduledEod(UUID runId, String message);

    Optional<LocalDate> latestScheduledEodBusinessDate();
}
