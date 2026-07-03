package com.nexusxva.backoffice.api;

import com.nexusxva.backoffice.application.BackOfficeOperationsReportService;
import com.nexusxva.backoffice.application.BackOfficeOperationsReportService.BackOfficeOperationsReport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/back-office/reports")
public class BackOfficeReportsController {

    private final BackOfficeOperationsReportService service;

    public BackOfficeReportsController(BackOfficeOperationsReportService service) {
        this.service = service;
    }

    @GetMapping("/operations")
    public BackOfficeOperationsReport operations() {
        return service.report();
    }
}
