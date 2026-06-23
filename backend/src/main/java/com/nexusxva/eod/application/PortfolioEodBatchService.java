package com.nexusxva.eod.application;

import com.nexusxva.portfolio.application.PortfolioStore;
import com.nexusxva.shared.error.ConflictException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PortfolioEodBatchService {

    private final PortfolioStore portfolioStore;
    private final PortfolioEodService eodService;
    private final ZoneId businessZone;

    public PortfolioEodBatchService(
            PortfolioStore portfolioStore,
            PortfolioEodService eodService,
            @Value("${nexusxva.eod.zone:America/New_York}") String businessZone
    ) {
        this.portfolioStore = portfolioStore;
        this.eodService = eodService;
        this.businessZone = ZoneId.of(businessZone);
    }

    public EodBatchResult captureAll(LocalDate businessDate, String source) {
        LocalDate resolvedDate = businessDate == null ? LocalDate.now(businessZone) : businessDate;
        ArrayList<EodBatchPortfolioResult> results = new ArrayList<>();
        int captured = 0;
        int skipped = 0;
        int failed = 0;

        for (var portfolio : portfolioStore.listPortfolioSummaries()) {
            try {
                eodService.capture(portfolio.id(), resolvedDate, source);
                captured++;
                results.add(new EodBatchPortfolioResult(
                        portfolio.id(),
                        portfolio.name(),
                        "CAPTURED",
                        "EOD snapshot captured"
                ));
            } catch (ConflictException exception) {
                skipped++;
                results.add(new EodBatchPortfolioResult(
                        portfolio.id(),
                        portfolio.name(),
                        "SKIPPED",
                        exception.getMessage()
                ));
            } catch (RuntimeException exception) {
                failed++;
                results.add(new EodBatchPortfolioResult(
                        portfolio.id(),
                        portfolio.name(),
                        "FAILED",
                        sanitizedMessage(exception)
                ));
            }
        }

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
