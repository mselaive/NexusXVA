package com.nexusxva.xva.application;

import com.nexusxva.xva.domain.Counterparty;
import com.nexusxva.xva.domain.NettingSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface XvaStore {
    Counterparty createCounterparty(CreateCounterpartyCommand command);
    Counterparty updateCounterparty(UUID counterpartyId, UpdateCounterpartyCommand command);
    List<Counterparty> listCounterparties(boolean includeInactive);
    Optional<Counterparty> findCounterparty(UUID counterpartyId);
    NettingSet createNettingSet(CreateNettingSetCommand command);
    NettingSet updateNettingSet(UUID nettingSetId, UpdateNettingSetCommand command);
    List<NettingSet> listNettingSets(boolean includeInactive);
    Optional<NettingSet> findNettingSet(UUID nettingSetId);
    NettingSet assignPortfolio(UUID nettingSetId, UUID portfolioId);
    NettingSet removePortfolio(UUID nettingSetId, UUID portfolioId);
    NettingSet updateCollateral(UUID nettingSetId, UpdateNettingSetCollateralCommand command);
}
