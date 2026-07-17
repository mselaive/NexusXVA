package com.nexusxva.executescript.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.backoffice.application.BackOfficeOperationsReportService;
import com.nexusxva.cva.application.CvaCalculationCommand;
import com.nexusxva.cva.application.CvaCalculationService;
import com.nexusxva.eod.application.PortfolioDailyPnlService;
import com.nexusxva.eod.application.PortfolioEodService;
import com.nexusxva.executescript.domain.ExecuteScriptMode;
import com.nexusxva.executescript.domain.ExecuteScriptRun;
import com.nexusxva.executescript.domain.ExecuteScriptRunStatus;
import com.nexusxva.executescript.domain.ExecuteScriptStepStatus;
import com.nexusxva.executescript.domain.ExecuteScriptStepType;
import com.nexusxva.executescript.domain.ExecuteScriptTemplate;
import com.nexusxva.executescript.domain.ExecuteScriptTemplateStep;
import com.nexusxva.exposure.application.ExposureSimulationCommand;
import com.nexusxva.exposure.application.ExposureSimulationService;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingService;
import com.nexusxva.reporting.application.ReportSnapshotService;
import com.nexusxva.reporting.domain.ReportSnapshotScopeType;
import com.nexusxva.reporting.domain.ReportSnapshotType;
import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.tradelifecycle.application.TradeLifecycleService;
import com.nexusxva.valuationruns.application.ValuationRunService;
import com.nexusxva.valuationruns.domain.ValuationRunType;
import com.nexusxva.xva.application.CvaCurveMasterDataService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecuteScriptService {

    private final ExecuteScriptStore store;
    private final ObjectMapper objectMapper;
    private final PortfolioBlackScholesPricingService pricingService;
    private final ExposureSimulationService exposureService;
    private final CvaCalculationService cvaService;
    private final CvaCurveMasterDataService curveMasterDataService;
    private final PortfolioEodService eodService;
    private final PortfolioDailyPnlService pnlService;
    private final BackOfficeOperationsReportService operationsReportService;
    private final TradeLifecycleService lifecycleService;
    private final ReportSnapshotService reportSnapshotService;
    private final ValuationRunService valuationRunService;

    public ExecuteScriptService(
            ExecuteScriptStore store,
            ObjectMapper objectMapper,
            PortfolioBlackScholesPricingService pricingService,
            ExposureSimulationService exposureService,
            CvaCalculationService cvaService,
            CvaCurveMasterDataService curveMasterDataService,
            PortfolioEodService eodService,
            PortfolioDailyPnlService pnlService,
            BackOfficeOperationsReportService operationsReportService,
            TradeLifecycleService lifecycleService,
            ReportSnapshotService reportSnapshotService,
            ValuationRunService valuationRunService
    ) {
        this.store = store;
        this.objectMapper = objectMapper;
        this.pricingService = pricingService;
        this.exposureService = exposureService;
        this.cvaService = cvaService;
        this.curveMasterDataService = curveMasterDataService;
        this.eodService = eodService;
        this.pnlService = pnlService;
        this.operationsReportService = operationsReportService;
        this.lifecycleService = lifecycleService;
        this.reportSnapshotService = reportSnapshotService;
        this.valuationRunService = valuationRunService;
    }

    @Transactional
    public ExecuteScriptTemplate createTemplate(SaveExecuteScriptTemplateCommand command, AuthSession session) {
        SaveExecuteScriptTemplateCommand normalized = normalize(command);
        return store.createTemplate(UUID.randomUUID(), normalized, userId(session), Instant.now());
    }

    @Transactional
    public ExecuteScriptTemplate updateTemplate(UUID templateId, SaveExecuteScriptTemplateCommand command, AuthSession session) {
        if (store.findTemplate(templateId).isEmpty()) {
            throw new ResourceNotFoundException("ExecuteScript template not found");
        }
        SaveExecuteScriptTemplateCommand normalized = normalize(command);
        return store.updateTemplate(templateId, normalized, userId(session), Instant.now());
    }

    @Transactional(readOnly = true)
    public List<ExecuteScriptTemplate> listTemplates(boolean includeInactive) {
        return store.listTemplates(includeInactive);
    }

    @Transactional(readOnly = true)
    public ExecuteScriptTemplate getTemplate(UUID templateId) {
        return store.findTemplate(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("ExecuteScript template not found"));
    }

    @Transactional
    public ExecuteScriptRun run(RunExecuteScriptCommand command, AuthSession session) {
        ExecuteScriptTemplate template = getTemplate(command.templateId());
        if (!template.active()) {
            throw new IllegalArgumentException("ExecuteScript template is inactive");
        }
        ExecuteScriptMode mode = command.mode() == null ? ExecuteScriptMode.DRY_RUN : command.mode();
        LocalDate businessDate = command.businessDate() == null ? LocalDate.now() : command.businessDate();
        List<UUID> portfolioIds = command.portfolioIds() == null ? List.of() : List.copyOf(command.portfolioIds());
        JsonNode parameters = mergedParameters(template.defaultParameters(), command.parameters());
        UUID runId = UUID.randomUUID();
        store.startRun(runId, template, mode, businessDate, userId(session), input(mode, businessDate, portfolioIds, parameters), Instant.now());
        execute(runId, template, mode, businessDate, portfolioIds, parameters, session);
        return getRun(runId);
    }

    @Transactional(readOnly = true)
    public ExecuteScriptRun getRun(UUID runId) {
        return store.findRun(runId)
                .orElseThrow(() -> new ResourceNotFoundException("ExecuteScript run not found"));
    }

    @Transactional(readOnly = true)
    public List<ExecuteScriptRun> recentRuns(int limit) {
        return store.recentRuns(limit);
    }

    private void execute(
            UUID runId,
            ExecuteScriptTemplate template,
            ExecuteScriptMode mode,
            LocalDate businessDate,
            List<UUID> portfolioIds,
            JsonNode parameters,
            AuthSession session
    ) {
        boolean hasFailure = false;
        boolean blocked = false;
        List<ExecuteScriptTemplateStep> steps = template.steps().stream()
                .filter(ExecuteScriptTemplateStep::enabled)
                .sorted(Comparator.comparingInt(ExecuteScriptTemplateStep::order))
                .toList();
        for (ExecuteScriptTemplateStep step : steps) {
            UUID stepId = UUID.randomUUID();
            store.createRunStep(stepId, runId, step);
            if (blocked) {
                store.finishStep(stepId, ExecuteScriptStepStatus.SKIPPED, "Skipped because a critical step failed", Map.of());
                continue;
            }
            store.markStepRunning(stepId, Instant.now());
            try {
                Object output = executeStep(step.stepType(), mode, businessDate, portfolioIds, parameters, session);
                store.finishStep(stepId, ExecuteScriptStepStatus.COMPLETED, "Step completed", output);
            } catch (RuntimeException exception) {
                hasFailure = true;
                String message = sanitizedMessage(exception);
                store.finishStep(stepId, ExecuteScriptStepStatus.FAILED, message, Map.of("error", message));
                if (step.critical()) {
                    blocked = true;
                }
            }
        }
        ExecuteScriptRunStatus status = blocked
                ? ExecuteScriptRunStatus.FAILED
                : hasFailure ? ExecuteScriptRunStatus.PARTIAL : ExecuteScriptRunStatus.COMPLETED;
        store.completeRun(runId, status, status == ExecuteScriptRunStatus.COMPLETED ? "ExecuteScript completed" : "ExecuteScript completed with issues");
    }

    private Object executeStep(
            ExecuteScriptStepType stepType,
            ExecuteScriptMode mode,
            LocalDate businessDate,
            List<UUID> portfolioIds,
            JsonNode parameters,
            AuthSession session
    ) {
        return switch (stepType) {
            case BO_OPERATIONS_REPORT -> runBoOperationsReport(mode, session, businessDate);
            case BO_LIFECYCLE_REPORT -> runBoLifecycleReport(mode, session, businessDate);
            case FO_PNL_REPORT -> runFoPnlReport(mode, session, businessDate, portfolioIds);
            case PORTFOLIO_PRICING -> runPricing(mode, session, businessDate, portfolioIds);
            case EXPOSURE -> runExposure(mode, session, businessDate, portfolioIds, parameters);
            case CVA -> runCva(mode, session, businessDate, portfolioIds, parameters);
            case EOD_VALIDATE -> validateEod(businessDate, portfolioIds);
            case EOD_CAPTURE -> runEodCapture(mode, businessDate, portfolioIds);
        };
    }

    private Object runBoOperationsReport(ExecuteScriptMode mode, AuthSession session, LocalDate businessDate) {
        var report = operationsReportService.report();
        Map<String, Object> summary = Map.of("portfolios", report.portfolios(), "failedPnlPortfolios", report.failedPnlPortfolios());
        if (mode == ExecuteScriptMode.REAL_RUN) {
            var snapshot = reportSnapshotService.record(session, ReportSnapshotType.BO_OPERATIONS, "BO Operations Reporting - ExecuteScript", businessDate, ReportSnapshotScopeType.BACK_OFFICE, null, "Back Office", Map.of("source", "EXECUTE_SCRIPT"), report, summary);
            return Map.of("reportSnapshotId", snapshot.id(), "summary", summary);
        }
        return Map.of("dryRun", true, "summary", summary);
    }

    private Object runBoLifecycleReport(ExecuteScriptMode mode, AuthSession session, LocalDate businessDate) {
        var report = lifecycleService.reportForBackOffice();
        Map<String, Object> summary = Map.of("total", report.total(), "pendingValidation", report.pendingValidation());
        if (mode == ExecuteScriptMode.REAL_RUN) {
            var snapshot = reportSnapshotService.record(session, ReportSnapshotType.BO_LIFECYCLE, "BO Lifecycle Reporting - ExecuteScript", businessDate, ReportSnapshotScopeType.BACK_OFFICE, null, "Back Office", Map.of("source", "EXECUTE_SCRIPT"), report, summary);
            return Map.of("reportSnapshotId", snapshot.id(), "summary", summary);
        }
        return Map.of("dryRun", true, "summary", summary);
    }

    private Object runFoPnlReport(ExecuteScriptMode mode, AuthSession session, LocalDate businessDate, List<UUID> portfolioIds) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (UUID portfolioId : portfolioIds) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("portfolioId", portfolioId);
            try {
                var pnl = pnlService.calculate(portfolioId, businessDate);
                row.put("status", "OK");
                row.put("dailyPnl", pnl.dailyPnl());
                row.put("sinceTradePnl", pnl.sinceTradePnl());
                row.put("currentMarketValue", pnl.currentMarketValue());
            } catch (RuntimeException exception) {
                row.put("status", "FAILED");
                row.put("error", sanitizedMessage(exception));
            }
            rows.add(row);
        }
        if (mode == ExecuteScriptMode.REAL_RUN) {
            var snapshot = reportSnapshotService.record(session, ReportSnapshotType.FO_PNL_SNAPSHOT, "FO P&L Snapshot - ExecuteScript", businessDate, ReportSnapshotScopeType.GLOBAL, null, "ExecuteScript", Map.of("portfolioIds", portfolioIds), Map.of("businessDate", businessDate, "portfolios", rows), Map.of("portfolioCount", rows.size()));
            return Map.of("reportSnapshotId", snapshot.id(), "portfolioCount", rows.size(), "portfolioResults", rows);
        }
        return Map.of("dryRun", true, "portfolioCount", rows.size(), "portfolioResults", rows);
    }

    private Object runPricing(ExecuteScriptMode mode, AuthSession session, LocalDate businessDate, List<UUID> portfolioIds) {
        List<Map<String, Object>> outputs = new ArrayList<>();
        for (UUID portfolioId : portfolioIds) {
            try {
                var result = pricingService.price(portfolioId, businessDate);
                if (mode == ExecuteScriptMode.REAL_RUN) {
                    var run = valuationRunService.recordSuccess(session, portfolioId, ValuationRunType.PRICING, result.model(), result.valuationDate(), Map.of("portfolioId", portfolioId, "valuationDate", businessDate, "source", "EXECUTE_SCRIPT"), result, Map.of("totalPrice", result.totalPrice(), "baseCurrency", result.baseCurrency()));
                    outputs.add(Map.of("portfolioId", portfolioId, "status", "SUCCESS", "valuationRunId", run.id()));
                } else {
                    outputs.add(Map.of("portfolioId", portfolioId, "status", "SUCCESS", "totalPrice", result.totalPrice(), "baseCurrency", result.baseCurrency()));
                }
            } catch (RuntimeException exception) {
                if (mode == ExecuteScriptMode.REAL_RUN) {
                    var run = valuationRunService.recordFailure(
                            session,
                            portfolioId,
                            ValuationRunType.PRICING,
                            "BLACK_SCHOLES_PORTFOLIO_PRICING_V1",
                            businessDate,
                            Map.of("portfolioId", portfolioId, "valuationDate", businessDate, "source", "EXECUTE_SCRIPT"),
                            exception
                    );
                    outputs.add(Map.of("portfolioId", portfolioId, "status", "FAILED", "valuationRunId", run.id(), "error", sanitizedMessage(exception)));
                } else {
                    outputs.add(Map.of("portfolioId", portfolioId, "status", "FAILED", "error", sanitizedMessage(exception)));
                }
            }
        }
        return Map.of("portfolioResults", outputs);
    }

    private Object runExposure(ExecuteScriptMode mode, AuthSession session, LocalDate businessDate, List<UUID> portfolioIds, JsonNode parameters) {
        List<Map<String, Object>> outputs = new ArrayList<>();
        for (UUID portfolioId : portfolioIds) {
            var command = exposureCommand(portfolioId, businessDate, parameters);
            try {
                var result = exposureService.simulate(command);
                if (mode == ExecuteScriptMode.REAL_RUN) {
                    var run = valuationRunService.recordSuccess(session, portfolioId, ValuationRunType.EXPOSURE, result.model(), result.valuationDate(), command, result, Map.of("points", result.points().size(), "paths", result.paths()));
                    outputs.add(Map.of("portfolioId", portfolioId, "status", "SUCCESS", "valuationRunId", run.id()));
                } else {
                    outputs.add(Map.of("portfolioId", portfolioId, "status", "SUCCESS", "points", result.points().size(), "paths", result.paths()));
                }
            } catch (RuntimeException exception) {
                if (mode == ExecuteScriptMode.REAL_RUN) {
                    var run = valuationRunService.recordFailure(session, portfolioId, ValuationRunType.EXPOSURE, "GBM_BLACK_SCHOLES_EXPOSURE_V1", businessDate, command, exception);
                    outputs.add(Map.of("portfolioId", portfolioId, "status", "FAILED", "valuationRunId", run.id(), "error", sanitizedMessage(exception)));
                } else {
                    outputs.add(Map.of("portfolioId", portfolioId, "status", "FAILED", "error", sanitizedMessage(exception)));
                }
            }
        }
        return Map.of("portfolioResults", outputs);
    }

    private Object runCva(ExecuteScriptMode mode, AuthSession session, LocalDate businessDate, List<UUID> portfolioIds, JsonNode parameters) {
        UUID creditCurveId = uuid(parameters, "cva", "creditCurveId");
        UUID discountCurveId = uuid(parameters, "cva", "discountCurveId");
        if (creditCurveId == null || discountCurveId == null) {
            throw new IllegalArgumentException("ExecuteScript CVA requires creditCurveId and discountCurveId");
        }
        var creditCurve = curveMasterDataService.activeCreditCurvePoints(creditCurveId);
        var discountCurve = curveMasterDataService.activeDiscountCurvePoints(discountCurveId);
        List<Map<String, Object>> outputs = new ArrayList<>();
        for (UUID portfolioId : portfolioIds) {
            var command = cvaCommand(portfolioId, businessDate, parameters, creditCurve, discountCurve);
            try {
                var result = cvaService.calculate(command);
                if (mode == ExecuteScriptMode.REAL_RUN) {
                    var run = valuationRunService.recordSuccess(session, portfolioId, ValuationRunType.CVA, result.model(), result.valuationDate(), command, result, Map.of("cva", result.cva(), "creditMethod", result.creditMethod(), "discountMethod", result.discountMethod()));
                    outputs.add(Map.of("portfolioId", portfolioId, "status", "SUCCESS", "valuationRunId", run.id()));
                } else {
                    outputs.add(Map.of("portfolioId", portfolioId, "status", "SUCCESS", "cva", result.cva(), "creditMethod", result.creditMethod(), "discountMethod", result.discountMethod()));
                }
            } catch (RuntimeException exception) {
                if (mode == ExecuteScriptMode.REAL_RUN) {
                    var run = valuationRunService.recordFailure(session, portfolioId, ValuationRunType.CVA, "SIMPLIFIED_CVA_V1", businessDate, command, exception);
                    outputs.add(Map.of("portfolioId", portfolioId, "status", "FAILED", "valuationRunId", run.id(), "error", sanitizedMessage(exception)));
                } else {
                    outputs.add(Map.of("portfolioId", portfolioId, "status", "FAILED", "error", sanitizedMessage(exception)));
                }
            }
        }
        return Map.of("portfolioResults", outputs);
    }

    private Object validateEod(LocalDate businessDate, List<UUID> portfolioIds) {
        List<Map<String, Object>> outputs = new ArrayList<>();
        for (UUID portfolioId : portfolioIds) {
            try {
                var pricing = pricingService.price(portfolioId, businessDate);
                outputs.add(Map.of("portfolioId", portfolioId, "status", "READY", "totalPrice", pricing.totalPrice(), "baseCurrency", pricing.baseCurrency()));
            } catch (RuntimeException exception) {
                outputs.add(Map.of("portfolioId", portfolioId, "status", "FAILED", "error", sanitizedMessage(exception)));
            }
        }
        return Map.of("dryRun", true, "portfolioResults", outputs);
    }

    private Object runEodCapture(ExecuteScriptMode mode, LocalDate businessDate, List<UUID> portfolioIds) {
        if (mode == ExecuteScriptMode.DRY_RUN) {
            return validateEod(businessDate, portfolioIds);
        }
        List<Map<String, Object>> outputs = new ArrayList<>();
        int captured = 0;
        for (UUID portfolioId : portfolioIds) {
            try {
                var snapshot = eodService.capture(portfolioId, businessDate, "EXECUTE_SCRIPT");
                captured++;
                outputs.add(Map.of("portfolioId", portfolioId, "status", "CAPTURED", "eodRunId", snapshot.id()));
            } catch (RuntimeException exception) {
                outputs.add(Map.of("portfolioId", portfolioId, "status", "FAILED", "error", sanitizedMessage(exception)));
            }
        }
        return Map.of("captured", captured, "portfolioResults", outputs);
    }

    private ExposureSimulationCommand exposureCommand(UUID portfolioId, LocalDate businessDate, JsonNode parameters) {
        JsonNode exposure = child(parameters, "exposure");
        return new ExposureSimulationCommand(
                portfolioId,
                date(parameters, "valuationDate", businessDate),
                intValue(exposure, "horizonDays", 365),
                intValue(exposure, "timeSteps", 12),
                intValue(exposure, "paths", 1000),
                longValue(exposure, "seed", 12345L),
                doubleValue(exposure, "pfeConfidenceLevel", 0.95)
        );
    }

    private CvaCalculationCommand cvaCommand(UUID portfolioId, LocalDate businessDate, JsonNode parameters, java.util.List<com.nexusxva.cva.domain.CreditCurvePoint> creditCurve, java.util.List<com.nexusxva.cva.domain.DiscountCurvePoint> discountCurve) {
        JsonNode exposure = child(parameters, "exposure");
        JsonNode cva = child(parameters, "cva");
        return new CvaCalculationCommand(
                portfolioId,
                date(parameters, "valuationDate", businessDate),
                intValue(exposure, "horizonDays", 365),
                intValue(exposure, "timeSteps", 12),
                intValue(exposure, "paths", 1000),
                longValue(exposure, "seed", 12345L),
                doubleValue(exposure, "pfeConfidenceLevel", 0.95),
                doubleValue(cva, "lossGivenDefault", 0.6),
                null,
                null,
                creditCurve,
                discountCurve
        );
    }

    private SaveExecuteScriptTemplateCommand normalize(SaveExecuteScriptTemplateCommand command) {
        String name = command.name() == null ? "" : command.name().trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (name.length() > 120) {
            throw new IllegalArgumentException("name must be at most 120 characters");
        }
        String description = command.description() == null ? null : command.description().trim();
        if (description != null && description.length() > 500) {
            throw new IllegalArgumentException("description must be at most 500 characters");
        }
        List<SaveExecuteScriptTemplateCommand.Step> steps = command.steps() == null ? List.of() : command.steps().stream()
                .map(step -> new SaveExecuteScriptTemplateCommand.Step(
                        step.stepType(),
                        step.order(),
                        step.critical(),
                        step.enabled(),
                        step.parameters() == null ? objectMapper.createObjectNode() : step.parameters()
                ))
                .sorted(Comparator.comparingInt(SaveExecuteScriptTemplateCommand.Step::order))
                .toList();
        if (steps.stream().noneMatch(SaveExecuteScriptTemplateCommand.Step::enabled)) {
            throw new IllegalArgumentException("at least one enabled step is required");
        }
        return new SaveExecuteScriptTemplateCommand(
                name,
                description,
                command.active(),
                command.defaultParameters() == null ? objectMapper.createObjectNode() : command.defaultParameters(),
                steps
        );
    }

    private JsonNode input(ExecuteScriptMode mode, LocalDate businessDate, List<UUID> portfolioIds, JsonNode parameters) {
        return objectMapper.valueToTree(Map.of(
                "mode", mode,
                "businessDate", businessDate,
                "portfolioIds", portfolioIds,
                "parameters", parameters
        ));
    }

    private JsonNode mergedParameters(JsonNode defaults, JsonNode override) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (defaults != null && defaults.isObject()) {
            defaults.fields().forEachRemaining(entry -> merged.put(entry.getKey(), entry.getValue()));
        }
        if (override != null && override.isObject()) {
            override.fields().forEachRemaining(entry -> merged.put(entry.getKey(), entry.getValue()));
        }
        return objectMapper.valueToTree(merged);
    }

    private JsonNode child(JsonNode node, String field) {
        JsonNode child = node == null ? null : node.get(field);
        return child == null || child.isNull() ? objectMapper.createObjectNode() : child;
    }

    private LocalDate date(JsonNode node, String field, LocalDate fallback) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? fallback : LocalDate.parse(value.asText());
    }

    private int intValue(JsonNode node, String field, int fallback) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? fallback : value.asInt();
    }

    private long longValue(JsonNode node, String field, long fallback) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? fallback : value.asLong();
    }

    private double doubleValue(JsonNode node, String field, double fallback) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? fallback : value.asDouble();
    }

    private UUID uuid(JsonNode node, String parent, String field) {
        JsonNode value = child(node, parent).get(field);
        return value == null || value.isNull() || value.asText().isBlank() ? null : UUID.fromString(value.asText());
    }

    private UUID userId(AuthSession session) {
        return session == null ? null : session.user().id();
    }

    private String sanitizedMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
