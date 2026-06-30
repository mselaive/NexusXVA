package com.nexusxva.tradelifecycle.api;

import com.nexusxva.tradelifecycle.application.TradeLifecycleReport;
import com.nexusxva.tradelifecycle.application.TradeLifecycleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/back-office/lifecycle-report")
public class BackOfficeLifecycleReportController {

    private final TradeLifecycleService service;

    public BackOfficeLifecycleReportController(TradeLifecycleService service) {
        this.service = service;
    }

    @GetMapping
    public TradeLifecycleReport report() {
        return service.reportForBackOffice();
    }
}
