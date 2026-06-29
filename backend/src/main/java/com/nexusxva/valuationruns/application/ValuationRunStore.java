package com.nexusxva.valuationruns.application;

import com.nexusxva.valuationruns.domain.ValuationRun;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ValuationRunStore {

    ValuationRun save(ValuationRun run);

    Optional<ValuationRun> find(UUID runId);

    List<ValuationRun> search(ValuationRunSearchCriteria criteria);
}
