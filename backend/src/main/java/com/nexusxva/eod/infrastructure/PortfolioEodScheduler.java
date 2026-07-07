package com.nexusxva.eod.infrastructure;

import com.nexusxva.audit.application.AuditEventCommand;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.eod.application.PortfolioEodBatchService;
import com.nexusxva.operationalcontrol.application.OperationalControlStore;
import com.nexusxva.operationalcontrol.domain.OperationalControlSettings;
import java.time.Instant;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "nexusxva.eod.scheduler.enabled", havingValue = "true", matchIfMissing = true)
class PortfolioEodScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioEodScheduler.class);
    private final PortfolioEodBatchService batchService;
    private final OperationalControlStore operationalControlStore;
    private final AuditService auditService;

    PortfolioEodScheduler(
            PortfolioEodBatchService batchService,
            OperationalControlStore operationalControlStore,
            AuditService auditService
    ) {
        this.batchService = batchService;
        this.operationalControlStore = operationalControlStore;
        this.auditService = auditService;
    }

    @Scheduled(fixedDelayString = "${nexusxva.eod.scheduler-tick:60000}")
    void captureAllPortfoliosIfDue() {
        OperationalControlSettings settings = operationalControlStore.settings();
        if (!settings.eodEnabled()) {
            return;
        }
        ZonedDateTime now = Instant.now().atZone(settings.timezone());
        LocalDate businessDate = now.toLocalDate();
        LocalTime localTime = now.toLocalTime();
        if (!settings.businessDays().contains(now.getDayOfWeek()) || localTime.isBefore(settings.eodRunTime())) {
            return;
        }

        UUID runId = UUID.randomUUID();
        Instant startedAt = Instant.now();
        if (!operationalControlStore.tryStartScheduledEod(runId, businessDate, startedAt)) {
            return;
        }

        LOGGER.info(
                "Scheduled EOD triggered runId={} businessDate={} timezone={} eodRunTime={}",
                runId,
                businessDate,
                settings.timezone(),
                settings.eodRunTime()
        );
        auditService.record(new AuditEventCommand(
                "SCHEDULED_EOD_TRIGGERED",
                "EOD",
                "RUN_SCHEDULED_EOD",
                AuditOutcome.SUCCESS,
                null,
                null,
                200,
                "EOD_SCHEDULER_RUN",
                runId.toString(),
                "Scheduled EOD triggered",
                auditService.metadata(Map.of("businessDate", businessDate.toString(), "timezone", settings.timezone().getId()))
        ));

        try {
            var result = batchService.captureAll(businessDate, "SCHEDULED");
            operationalControlStore.completeScheduledEod(
                    runId,
                    result.captured(),
                    result.skipped(),
                    result.failed(),
                    "Scheduled EOD completed"
            );
            LOGGER.info(
                    "Scheduled EOD completed runId={} businessDate={} total={} captured={} skipped={} failed={}",
                    runId,
                    businessDate,
                    result.totalPortfolios(),
                    result.captured(),
                    result.skipped(),
                    result.failed()
            );
        } catch (RuntimeException exception) {
            operationalControlStore.failScheduledEod(runId, sanitizedMessage(exception));
            LOGGER.warn(
                    "Scheduled EOD failed runId={} businessDate={} reason={}",
                    runId,
                    businessDate,
                    sanitizedMessage(exception)
            );
        }
    }

    private String sanitizedMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Scheduled EOD failed" : message;
    }
}
