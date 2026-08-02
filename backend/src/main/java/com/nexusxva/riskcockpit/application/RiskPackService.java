package com.nexusxva.riskcockpit.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.operationalcontrol.application.OperationalControlService;
import com.nexusxva.portfolio.application.PortfolioService;
import com.nexusxva.portfolio.domain.Portfolio;
import com.nexusxva.riskcockpit.domain.RiskPackRun;
import com.nexusxva.shared.error.ConflictException;
import com.nexusxva.shared.error.ServiceUnavailableException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class RiskPackService implements ApplicationRunner {
    private final RiskPackStore store; private final PortfolioService portfolios; private final RiskPackWorker worker;
    private final ThreadPoolTaskExecutor executor; private final OperationalControlService operationalControl; private final ObjectMapper mapper;
    public RiskPackService(RiskPackStore store, PortfolioService portfolios, RiskPackWorker worker,
                           @Qualifier("riskPackExecutor") ThreadPoolTaskExecutor executor,
                           OperationalControlService operationalControl, ObjectMapper mapper) {
        this.store=store; this.portfolios=portfolios; this.worker=worker; this.executor=executor; this.operationalControl=operationalControl; this.mapper=mapper;
    }
    public RiskPackRun start(UUID portfolioId, LocalDate valuationDate, AuthSession session) {
        if (store.hasActiveRun(portfolioId)) throw new ConflictException("A Risk Pack is already running for this portfolio");
        Portfolio portfolio=portfolios.getPortfolio(portfolioId);
        LocalDate date=valuationDate==null?operationalControl.businessDate(Instant.now()):valuationDate;
        var defaults=operationalControl.settings().closeChecklist().riskDefaults();
        var config=mapper.valueToTree(Map.of("historicalObservations",260,"varConfidenceLevel",.99,
                "exposure",Map.of("horizonDays",defaults.horizonDays(),"timeSteps",defaults.timeSteps(),"paths",defaults.paths(),"seed",defaults.seed(),"pfeConfidenceLevel",defaults.pfeConfidenceLevel()),
                "cva",Map.of("lossGivenDefault",defaults.lossGivenDefault(),"creditCurveId",String.valueOf(defaults.creditCurveId()),"discountCurveId",String.valueOf(defaults.discountCurveId()))));
        UUID id=UUID.randomUUID(); Instant now=Instant.now();
        RiskPackRun run;
        try {
            run=store.create(id, portfolioId, date, session==null?null:session.user().id(), session==null?null:session.user().username(),
                    session==null?null:session.activeGroup(), portfolio.updatedAt(), config, now);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("A Risk Pack is already running for this portfolio");
        }
        try { executor.execute(() -> worker.execute(id)); }
        catch (RejectedExecutionException exception) {
            for (var type : com.nexusxva.riskcockpit.domain.RiskPackComponentType.values()) {
                store.completeComponent(id, type, com.nexusxva.riskcockpit.domain.RiskPackComponentStatus.SKIPPED,
                        null, "Risk Pack queue is full", Instant.now());
            }
            store.completeRun(id, com.nexusxva.riskcockpit.domain.RiskPackRunStatus.FAILED, null, "Risk Pack queue is full", Instant.now());
            throw new ServiceUnavailableException("Risk Pack queue is full");
        }
        return run;
    }
    public RiskPackRun get(UUID id){ return store.find(id).orElseThrow(() -> new com.nexusxva.shared.error.ResourceNotFoundException("Risk Pack run not found")); }
    public Optional<RiskPackRun> latest(UUID portfolioId){ return store.latest(portfolioId); }
    public List<RiskPackRun> recent(UUID portfolioId,int limit){ return store.recent(portfolioId,limit); }
    @Override public void run(ApplicationArguments args){ store.failAbandonedRuns(Instant.now()); }
}
