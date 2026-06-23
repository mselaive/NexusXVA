package com.nexusxva.eod.infrastructure;

import com.nexusxva.eod.application.PortfolioEodBatchService;
import java.time.LocalDate;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "nexusxva.eod", name = "enabled", havingValue = "true")
class PortfolioEodScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioEodScheduler.class);
    private final PortfolioEodBatchService batchService;
    private final ZoneId businessZone;

    PortfolioEodScheduler(
            PortfolioEodBatchService batchService,
            @Value("${nexusxva.eod.zone:America/New_York}") String businessZone
    ) {
        this.batchService = batchService;
        this.businessZone = ZoneId.of(businessZone);
    }

    @Scheduled(
            cron = "${nexusxva.eod.cron:0 15 17 * * MON-FRI}",
            zone = "${nexusxva.eod.zone:America/New_York}"
    )
    void captureAllPortfolios() {
        LocalDate businessDate = LocalDate.now(businessZone);
        var result = batchService.captureAll(businessDate, "SCHEDULED");
        LOGGER.info(
                "EOD batch completed businessDate={} total={} captured={} skipped={} failed={}",
                businessDate,
                result.totalPortfolios(),
                result.captured(),
                result.skipped(),
                result.failed()
        );
    }
}
