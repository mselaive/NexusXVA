package com.nexusxva.xva.application;

import com.nexusxva.xva.domain.Counterparty;
import com.nexusxva.xva.domain.NettingSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface XvaStore {
    Counterparty createCounterparty(CreateCounterpartyCommand command);
    List<Counterparty> listCounterparties();
    Optional<Counterparty> findCounterparty(UUID counterpartyId);
    NettingSet createNettingSet(CreateNettingSetCommand command);
    List<NettingSet> listNettingSets();
    Optional<NettingSet> findNettingSet(UUID nettingSetId);
    NettingSet assignPortfolio(UUID nettingSetId, UUID portfolioId);
    NettingSet removePortfolio(UUID nettingSetId, UUID portfolioId);
    NettingSet updateCollateral(UUID nettingSetId, UpdateNettingSetCollateralCommand command);
}
