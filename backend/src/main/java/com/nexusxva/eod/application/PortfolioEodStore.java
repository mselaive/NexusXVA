package com.nexusxva.eod.application;

import com.nexusxva.eod.domain.PortfolioEodSnapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortfolioEodStore {

    PortfolioEodSnapshot create(PortfolioEodSnapshot snapshot);

    boolean exists(UUID portfolioId, LocalDate businessDate);

    Optional<PortfolioEodSnapshot> latest(UUID portfolioId);

    List<PortfolioEodSnapshot> history(UUID portfolioId, int limit);
}
