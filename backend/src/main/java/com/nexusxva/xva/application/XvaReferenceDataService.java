package com.nexusxva.xva.application;

import com.nexusxva.portfolio.application.PortfolioService;
import com.nexusxva.portfolio.domain.Portfolio;
import com.nexusxva.shared.error.ConflictException;
import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.xva.domain.Counterparty;
import com.nexusxva.xva.domain.NettingSet;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class XvaReferenceDataService {

    private final XvaStore store;
    private final PortfolioService portfolioService;

    public XvaReferenceDataService(XvaStore store, PortfolioService portfolioService) {
        this.store = store;
        this.portfolioService = portfolioService;
    }

    @Transactional
    public Counterparty createCounterparty(CreateCounterpartyCommand command) {
        return store.createCounterparty(command);
    }

    @Transactional(readOnly = true)
    public List<Counterparty> listCounterparties() {
        return store.listCounterparties();
    }

    @Transactional
    public NettingSet createNettingSet(CreateNettingSetCommand command) {
        store.findCounterparty(command.counterpartyId())
                .orElseThrow(() -> new ResourceNotFoundException("Counterparty not found"));
        return store.createNettingSet(command);
    }

    @Transactional(readOnly = true)
    public List<NettingSet> listNettingSets() {
        return store.listNettingSets();
    }

    @Transactional(readOnly = true)
    public NettingSet getNettingSet(UUID nettingSetId) {
        return store.findNettingSet(nettingSetId)
                .orElseThrow(() -> new ResourceNotFoundException("Netting set not found"));
    }

    @Transactional
    public NettingSet assignPortfolio(UUID nettingSetId, UUID portfolioId) {
        NettingSet nettingSet = getNettingSet(nettingSetId);
        Portfolio portfolio = portfolioService.getPortfolio(portfolioId);
        if (!portfolio.baseCurrency().equals(nettingSet.baseCurrency())) {
            throw new ConflictException("Portfolio currency must match netting set baseCurrency in V1");
        }
        return store.assignPortfolio(nettingSetId, portfolioId);
    }

    @Transactional
    public NettingSet removePortfolio(UUID nettingSetId, UUID portfolioId) {
        getNettingSet(nettingSetId);
        return store.removePortfolio(nettingSetId, portfolioId);
    }

    @Transactional
    public NettingSet updateCollateral(UUID nettingSetId, UpdateNettingSetCollateralCommand command) {
        getNettingSet(nettingSetId);
        return store.updateCollateral(nettingSetId, command);
    }
}
