package com.nexusxva.operationalcontrol.domain;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record CloseChecklistSettings(
        boolean enabled,
        List<UUID> portfolioIds,
        List<CloseChecklistStepDefinition> steps,
        CloseChecklistRiskDefaults riskDefaults
) {
    public CloseChecklistSettings {
        portfolioIds = portfolioIds == null ? List.of() : List.copyOf(portfolioIds);
        steps = steps == null ? defaultSteps() : List.copyOf(steps);
        riskDefaults = riskDefaults == null ? CloseChecklistRiskDefaults.defaults() : riskDefaults;
        if (enabled && steps.stream().filter(step -> step.enabled() && step.stepType() == CloseChecklistStepType.EOD).count() != 1) {
            throw new IllegalArgumentException("close checklist must contain exactly one enabled Official EOD block");
        }
        int eodOrder = steps.stream()
                .filter(step -> step.enabled() && step.stepType() == CloseChecklistStepType.EOD)
                .mapToInt(CloseChecklistStepDefinition::order)
                .findFirst()
                .orElse(Integer.MAX_VALUE);
        steps = steps.stream().map(step -> {
            CloseChecklistPhase phase = step.stepType() == CloseChecklistStepType.EOD
                    ? CloseChecklistPhase.EOD
                    : step.order() < eodOrder ? CloseChecklistPhase.PRE_EOD : CloseChecklistPhase.POST_EOD;
            return new CloseChecklistStepDefinition(phase, step.stepType(), step.templateId(), step.scriptMode(), step.enabled(), step.critical(), step.order());
        }).toList();
    }

    public List<CloseChecklistStepDefinition> enabledSteps() {
        return steps.stream()
                .filter(CloseChecklistStepDefinition::enabled)
                .sorted(Comparator.comparingInt(CloseChecklistStepDefinition::order))
                .toList();
    }

    public static CloseChecklistSettings defaults() {
        return new CloseChecklistSettings(false, List.of(), defaultSteps(), CloseChecklistRiskDefaults.defaults());
    }

    private static List<CloseChecklistStepDefinition> defaultSteps() {
        return List.of(
                new CloseChecklistStepDefinition(CloseChecklistPhase.PRE_EOD, CloseChecklistStepType.BO_OPERATIONS_REPORT, true, true, 10),
                new CloseChecklistStepDefinition(CloseChecklistPhase.PRE_EOD, CloseChecklistStepType.BO_LIFECYCLE_REPORT, true, false, 20),
                new CloseChecklistStepDefinition(CloseChecklistPhase.EOD, CloseChecklistStepType.EOD, true, true, 30),
                new CloseChecklistStepDefinition(CloseChecklistPhase.POST_EOD, CloseChecklistStepType.PORTFOLIO_PRICING, true, false, 40),
                new CloseChecklistStepDefinition(CloseChecklistPhase.POST_EOD, CloseChecklistStepType.EXPOSURE, false, false, 50),
                new CloseChecklistStepDefinition(CloseChecklistPhase.POST_EOD, CloseChecklistStepType.CVA, false, false, 60),
                new CloseChecklistStepDefinition(CloseChecklistPhase.POST_EOD, CloseChecklistStepType.FO_PNL_REPORT, true, false, 70)
        );
    }
}
