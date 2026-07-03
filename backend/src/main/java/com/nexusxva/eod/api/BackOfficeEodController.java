package com.nexusxva.eod.api;

import com.nexusxva.audit.application.AuditEventCommand;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.eod.application.PortfolioEodService;
import com.nexusxva.eod.application.PortfolioEodBatchService;
import com.nexusxva.portfolio.api.PortfolioSummaryResponse;
import com.nexusxva.portfolio.application.PortfolioStore;
import com.nexusxva.shared.error.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
    private final AuditService auditService;

    public BackOfficeEodController(
            PortfolioEodService service,
            PortfolioEodBatchService batchService,
            PortfolioStore portfolioStore,
            AuditService auditService
    ) {
        this.service = service;
        this.batchService = batchService;
        this.portfolioStore = portfolioStore;
        this.auditService = auditService;
    }

    @PostMapping("/run")
    public EodBatchResponse captureAll(
            @RequestBody(required = false) CapturePortfolioEodRequest request,
            HttpServletRequest servletRequest
    ) {
        LocalDate businessDate = request == null ? null : request.businessDate();
        EodBatchResponse response = EodBatchResponse.from(batchService.captureAll(businessDate, "MANUAL_BO_BATCH"));
        auditService.record(AuditEventCommand.of(
                "EOD_BATCH_STARTED",
                "BACK_OFFICE",
                "RUN_EOD_BATCH",
                AuditOutcome.SUCCESS,
                currentSession(servletRequest),
                servletRequest,
                200,
                "EOD_BATCH",
                null,
                "BO ran EOD batch",
                auditService.metadata(java.util.Map.of(
                        "businessDate", response.businessDate(),
                        "captured", response.captured(),
                        "skipped", response.skipped(),
                        "failed", response.failed()
                ))
        ));
        return response;
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

    @PostMapping("/runs/{runId}/void")
    public PortfolioEodSnapshotResponse voidRun(
            @PathVariable UUID runId,
            @Valid @RequestBody EodCorrectionRequest request,
            HttpServletRequest servletRequest
    ) {
        AuthSession session = currentSession(servletRequest);
        PortfolioEodSnapshotResponse response = PortfolioEodSnapshotResponse.from(service.voidRun(
                runId,
                session == null ? null : session.user().id(),
                request.reason()
        ));
        auditService.record(AuditEventCommand.of(
                "EOD_RUN_VOIDED",
                "BACK_OFFICE",
                "VOID_EOD_RUN",
                AuditOutcome.SUCCESS,
                session,
                servletRequest,
                200,
                "EOD_RUN",
                runId,
                "EOD run voided",
                auditService.metadata(java.util.Map.of("portfolioId", response.portfolioId(), "businessDate", response.businessDate()))
        ));
        return response;
    }

    @PostMapping("/runs/{runId}/recapture")
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioEodSnapshotResponse recapture(
            @PathVariable UUID runId,
            @Valid @RequestBody EodCorrectionRequest request,
            HttpServletRequest servletRequest
    ) {
        AuthSession session = currentSession(servletRequest);
        PortfolioEodSnapshotResponse response = PortfolioEodSnapshotResponse.from(service.recapture(
                runId,
                session == null ? null : session.user().id(),
                request.reason()
        ));
        auditService.record(AuditEventCommand.of(
                "EOD_RUN_RECAPTURED",
                "BACK_OFFICE",
                "RECAPTURE_EOD_RUN",
                AuditOutcome.SUCCESS,
                session,
                servletRequest,
                201,
                "EOD_RUN",
                runId,
                "EOD run recaptured",
                auditService.metadata(java.util.Map.of("newRunId", response.id(), "portfolioId", response.portfolioId(), "businessDate", response.businessDate()))
        ));
        return response;
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
