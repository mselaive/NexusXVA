package com.nexusxva.eod.application;

import com.nexusxva.portfolio.application.PortfolioStore;
import com.nexusxva.operationalcontrol.application.OperationalControlStore;
import com.nexusxva.shared.error.ConflictException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PortfolioEodBatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioEodBatchService.class);

    private final PortfolioStore portfolioStore;
    private final PortfolioEodService eodService;
    private final OperationalControlStore operationalControlStore;

    public PortfolioEodBatchService(
            PortfolioStore portfolioStore,
            PortfolioEodService eodService,
            OperationalControlStore operationalControlStore
    ) {
        this.portfolioStore = portfolioStore;
        this.eodService = eodService;
        this.operationalControlStore = operationalControlStore;
    }

    public EodBatchResult captureAll(LocalDate businessDate, String source) {
        LocalDate resolvedDate = businessDate == null
                ? LocalDate.now(operationalControlStore.settings().timezone())
                : businessDate;
        ArrayList<EodBatchPortfolioResult> results = new ArrayList<>();
        int captured = 0;
        int skipped = 0;
        int failed = 0;

        LOGGER.info("EOD batch started businessDate={} source={}", resolvedDate, source);
        for (var portfolio : portfolioStore.listPortfolioSummaries()) {
            try {
                eodService.capture(portfolio.id(), resolvedDate, source);
                captured++;
                LOGGER.info("EOD portfolio captured portfolioId={} portfolioName={} businessDate={} source={}",
                        portfolio.id(),
                        portfolio.name(),
                        resolvedDate,
                        source);
                results.add(new EodBatchPortfolioResult(
                        portfolio.id(),
                        portfolio.name(),
                        "CAPTURED",
                        "EOD snapshot captured"
                ));
            } catch (ConflictException exception) {
                skipped++;
                LOGGER.info("EOD portfolio skipped portfolioId={} portfolioName={} businessDate={} source={} reason={}",
                        portfolio.id(),
                        portfolio.name(),
                        resolvedDate,
                        source,
                        exception.getMessage());
                results.add(new EodBatchPortfolioResult(
                        portfolio.id(),
                        portfolio.name(),
                        "SKIPPED",
                        exception.getMessage()
                ));
            } catch (RuntimeException exception) {
                failed++;
                LOGGER.warn("EOD portfolio failed portfolioId={} portfolioName={} businessDate={} source={} reason={}",
                        portfolio.id(),
                        portfolio.name(),
                        resolvedDate,
                        source,
                        sanitizedMessage(exception));
                results.add(new EodBatchPortfolioResult(
                        portfolio.id(),
                        portfolio.name(),
                        "FAILED",
                        sanitizedMessage(exception)
                ));
            }
        }

        LOGGER.info(
                "EOD batch completed businessDate={} source={} total={} captured={} skipped={} failed={}",
                resolvedDate,
                source,
                results.size(),
                captured,
                skipped,
                failed
        );
        return new EodBatchResult(
                resolvedDate,
                results.size(),
                captured,
                skipped,
                failed,
                Instant.now(),
                results
        );
    }

    private String sanitizedMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "EOD capture failed" : message;
    }
}
