package com.nexusxva.eod.api;

import com.nexusxva.eod.application.PortfolioEodService;
import com.nexusxva.eod.application.PortfolioEodBatchService;
import com.nexusxva.portfolio.api.PortfolioSummaryResponse;
import com.nexusxva.portfolio.application.PortfolioStore;
import com.nexusxva.shared.error.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/back-office/eod")
public class BackOfficeEodController {

    private final PortfolioEodService service;
    private final PortfolioEodBatchService batchService;
    private final PortfolioStore portfolioStore;

    public BackOfficeEodController(
            PortfolioEodService service,
            PortfolioEodBatchService batchService,
            PortfolioStore portfolioStore
    ) {
        this.service = service;
        this.batchService = batchService;
        this.portfolioStore = portfolioStore;
    }

    @PostMapping("/run")
    public EodBatchResponse captureAll(@RequestBody(required = false) CapturePortfolioEodRequest request) {
        LocalDate businessDate = request == null ? null : request.businessDate();
        return EodBatchResponse.from(batchService.captureAll(businessDate, "MANUAL_BO_BATCH"));
    }

    @GetMapping("/portfolios")
    public List<PortfolioSummaryResponse> portfolios() {
        return portfolioStore.listPortfolioSummaries().stream()
                .map(PortfolioSummaryResponse::from)
                .toList();
    }

    @PostMapping("/portfolios/{portfolioId}")
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioEodSnapshotResponse capture(
            @PathVariable UUID portfolioId,
            @RequestBody(required = false) CapturePortfolioEodRequest request
    ) {
        LocalDate businessDate = request == null ? null : request.businessDate();
        return PortfolioEodSnapshotResponse.from(service.capture(portfolioId, businessDate, "MANUAL_BO"));
    }

    @GetMapping("/portfolios/{portfolioId}/latest")
    public PortfolioEodSnapshotResponse latest(@PathVariable UUID portfolioId) {
        return service.latest(portfolioId)
                .map(PortfolioEodSnapshotResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("EOD snapshot not found"));
    }

    @GetMapping("/portfolios/{portfolioId}")
    public List<PortfolioEodSnapshotResponse> history(
            @PathVariable UUID portfolioId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return service.history(portfolioId, limit).stream()
                .map(PortfolioEodSnapshotResponse::from)
                .toList();
    }
}
