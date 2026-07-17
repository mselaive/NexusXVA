"use client";

import React from "react";
import { Plus, Save } from "lucide-react";
import { nexusApi } from "@/lib/api";
import type { ExecuteScriptStepType, ExecuteScriptTemplate, ExecuteScriptTemplateStep, SaveExecuteScriptTemplateRequest } from "@/lib/types";
import { AppShell } from "./AppShell";

const stepTypes: ExecuteScriptStepType[] = [
  "FO_PNL_REPORT",
  "BO_OPERATIONS_REPORT",
  "BO_LIFECYCLE_REPORT",
  "PORTFOLIO_PRICING",
  "EXPOSURE",
  "CVA",
  "EOD_VALIDATE",
  "EOD_CAPTURE",
];

const stepCatalog: Record<ExecuteScriptStepType, { label: string; description: string; parameters: string }> = {
  FO_PNL_REPORT: {
    label: "FO P&L report",
    description: "Builds the Front Office P&L view for the selected portfolios.",
    parameters: "Uses the business date and selected portfolios.",
  },
  BO_OPERATIONS_REPORT: {
    label: "BO operations report",
    description: "Checks pending bookings, lifecycle queues, EOD coverage and P&L failures.",
    parameters: "Uses the business date. No extra values required.",
  },
  BO_LIFECYCLE_REPORT: {
    label: "BO lifecycle report",
    description: "Summarizes amend and cancel requests, their status and review aging.",
    parameters: "Uses the business date. No extra values required.",
  },
  PORTFOLIO_PRICING: {
    label: "Portfolio pricing",
    description: "Prices BO-confirmed positions with current market data.",
    parameters: "Uses valuationDate and the selected portfolios.",
  },
  EXPOSURE: {
    label: "Exposure simulation",
    description: "Runs Monte Carlo exposure for every selected portfolio.",
    parameters: "Uses horizon days, time steps, paths, seed and PFE confidence.",
  },
  CVA: {
    label: "CVA calculation",
    description: "Calculates portfolio CVA from exposure and persisted credit curves.",
    parameters: "Uses LGD, creditCurveId and discountCurveId.",
  },
  EOD_VALIDATE: {
    label: "Validate EOD inputs",
    description: "Checks whether a close could run, without creating an official EOD.",
    parameters: "Uses the business date, portfolios and stale market-data policy.",
  },
  EOD_CAPTURE: {
    label: "Capture official EOD",
    description: "Creates official portfolio closes. It only has an effect in REAL_RUN.",
    parameters: "Uses the business date and selected portfolios.",
  },
};

const howTo = [
  { title: "Purpose", body: "ExecuteScript templates are reusable operational playbooks. They are separate from the official Close Checklist." },
  { title: "Dry-run", body: "BO can run templates in dry-run mode to validate reports, market data, pricing, exposure and CVA before touching EOD." },
  { title: "Real-run", body: "Real-run can create report snapshots, valuation runs and EOD captures when the template includes those steps." },
];

export function ExecuteScriptsSetupPage() {
  const [templates, setTemplates] = React.useState<ExecuteScriptTemplate[]>([]);
  const [selectedId, setSelectedId] = React.useState("");
  const [form, setForm] = React.useState<FormState>(defaultForm());
  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState(false);
  const [message, setMessage] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    void load();
  }, []);

  async function load() {
    try {
      setLoading(true);
      const next = await nexusApi.listAdminExecuteScriptTemplates();
      setTemplates(next);
      if (next.length > 0) {
        selectTemplate(next[0]);
      }
      setError(null);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "ExecuteScript setup unavailable");
    } finally {
      setLoading(false);
    }
  }

  function selectTemplate(template: ExecuteScriptTemplate) {
    setSelectedId(template.id);
    setForm({
      name: template.name,
      description: template.description ?? "",
      active: template.active,
      defaultParametersText: JSON.stringify(template.defaultParameters ?? {}, null, 2),
      steps: template.steps.map((step) => ({ ...step, parameters: step.parameters ?? {} })),
    });
    setMessage(null);
    setError(null);
  }

  function newTemplate() {
    setSelectedId("");
    setForm(defaultForm());
    setMessage(null);
    setError(null);
  }

  function updateStep(index: number, patch: Partial<ExecuteScriptTemplateStep>) {
    setForm((current) => ({
      ...current,
      steps: current.steps.map((step, stepIndex) => stepIndex === index ? { ...step, ...patch } : step),
    }));
  }

  function addStep() {
    setForm((current) => ({
      ...current,
      steps: [...current.steps, { stepType: "PORTFOLIO_PRICING", order: current.steps.length + 1, critical: false, enabled: true, parameters: {} }],
    }));
  }

  async function save() {
    let defaultParameters: unknown;
    try {
      defaultParameters = JSON.parse(form.defaultParametersText || "{}");
    } catch {
      setError("Default parameters must be valid JSON.");
      return;
    }
    const body: SaveExecuteScriptTemplateRequest = {
      name: form.name,
      description: form.description || null,
      active: form.active,
      defaultParameters,
      steps: form.steps.map((step, index) => ({ ...step, order: index + 1, parameters: step.parameters ?? {} })),
    };
    try {
      setSaving(true);
      const saved = selectedId
        ? await nexusApi.updateExecuteScriptTemplate(selectedId, body)
        : await nexusApi.createExecuteScriptTemplate(body);
      const next = await nexusApi.listAdminExecuteScriptTemplates();
      setTemplates(next);
      selectTemplate(saved);
      setMessage("ExecuteScript template saved.");
      setError(null);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Could not save ExecuteScript template");
    } finally {
      setSaving(false);
    }
  }

  return (
    <AppShell title="Execute Scripts Setup" eyebrow="Administration" howTo={howTo}>
      {error ? <div className="alert">{error}</div> : null}
      {message ? <div className="success">{message}</div> : null}

      <section className="panel section">
        <div className="section-header">
          <div>
            <h2>Operational playbook templates</h2>
            <p className="muted">ADMIN defines reusable scripts. BO chooses dry-run or real-run when executing.</p>
          </div>
          <div className="toolbar compact-toolbar">
            <button className="btn secondary" type="button" onClick={newTemplate}>
              <Plus size={16} />
              New template
            </button>
            <button className="btn" type="button" onClick={save} disabled={saving}>
              <Save size={16} />
              {saving ? "Saving..." : "Save template"}
            </button>
          </div>
        </div>
        <div className="execute-setup-workspace">
          <div className="execute-template-strip">
            {loading ? <div className="empty-state">Loading templates...</div> : null}
            {!loading && templates.length === 0 ? <div className="empty-state">No templates yet.</div> : null}
            {templates.map((template) => (
              <button className={`script-list-item ${selectedId === template.id ? "active" : ""}`} key={template.id} type="button" onClick={() => selectTemplate(template)}>
                <span>{template.name}</span>
                <strong>{template.active ? "ACTIVE" : "INACTIVE"}</strong>
                <small>{template.steps.length} steps · updated {new Date(template.updatedAt).toLocaleDateString()}</small>
              </button>
            ))}
          </div>

          <div className="execute-template-editor">
            <div className="execute-template-basics">
              <label className="field">
                <span>Name</span>
                <input className="input" value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} />
              </label>
              <label className="field">
                <span>Description</span>
                <input className="input" value={form.description} onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))} />
              </label>
              <label className={`execute-active-control ${form.active ? "active" : ""}`}>
                <span>
                  <strong>Active</strong>
                  <small>Only active templates are visible to BO.</small>
                </span>
                <input type="checkbox" checked={form.active} onChange={(event) => setForm((current) => ({ ...current, active: event.target.checked }))} />
              </label>
            </div>

            <details className="execute-advanced-parameters">
              <summary>Advanced run defaults</summary>
              <p className="muted">These values are defaults for Exposure and CVA. BO can override them when executing a script.</p>
              <label className="field full">
                <span>Parameters JSON</span>
                <textarea className="input json-input" value={form.defaultParametersText} onChange={(event) => setForm((current) => ({ ...current, defaultParametersText: event.target.value }))} />
              </label>
            </details>

            <div className="execute-steps-panel">
              <div className="execute-steps-header">
                <div>
                  <h3>Execution steps</h3>
                  <p className="muted">Steps run from top to bottom. A critical failure stops every later step.</p>
                </div>
                <button className="btn secondary" type="button" onClick={addStep}>
                  <Plus size={16} />
                  Add step
                </button>
              </div>

              <div className="execute-step-list">
                {form.steps.length === 0 ? <div className="empty-state">Add the first action this script should execute.</div> : null}
                {form.steps.map((step, index) => {
                  const info = stepCatalog[step.stepType];
                  return (
                    <article className={`execute-step-card ${step.enabled ? "" : "disabled"}`} key={`${step.stepType}-${index}`}>
                      <div className="execute-step-summary">
                        <span className="step-order">{index + 1}</span>
                        <div>
                          <strong>{info.label}</strong>
                          <p>{info.description}</p>
                          <small>{info.parameters}</small>
                        </div>
                      </div>
                      <div className="execute-step-controls">
                        <label className="field execute-step-selector">
                          <span>Action</span>
                          <select className="input" value={step.stepType} onChange={(event) => updateStep(index, { stepType: event.target.value as ExecuteScriptStepType })}>
                            {stepTypes.map((type) => <option key={type} value={type}>{stepCatalog[type].label}</option>)}
                          </select>
                        </label>
                        <label className="execute-step-toggle">
                          <input type="checkbox" checked={step.enabled} onChange={(event) => updateStep(index, { enabled: event.target.checked })} />
                          <span><strong>Enabled</strong><small>Include this action</small></span>
                        </label>
                        <label className="execute-step-toggle">
                          <input type="checkbox" checked={step.critical} onChange={(event) => updateStep(index, { critical: event.target.checked })} />
                          <span><strong>Critical</strong><small>Stop if it fails</small></span>
                        </label>
                        <button className="btn ghost execute-remove-step" type="button" onClick={() => setForm((current) => ({ ...current, steps: current.steps.filter((_, stepIndex) => stepIndex !== index) }))}>Remove</button>
                      </div>
                    </article>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      </section>
    </AppShell>
  );
}

type FormState = {
  name: string;
  description: string;
  active: boolean;
  defaultParametersText: string;
  steps: ExecuteScriptTemplateStep[];
};

function defaultForm(): FormState {
  return {
    name: "Pre-close diagnostics",
    description: "Dry-run reports, pricing, exposure and CVA before EOD.",
    active: true,
    defaultParametersText: JSON.stringify({
      valuationDate: new Date().toISOString().slice(0, 10),
      exposure: { horizonDays: 365, timeSteps: 12, paths: 1000, seed: 12345, pfeConfidenceLevel: 0.95 },
      cva: { lossGivenDefault: 0.6, creditCurveId: null, discountCurveId: null },
    }, null, 2),
    steps: [
      { stepType: "BO_OPERATIONS_REPORT", order: 1, critical: false, enabled: true, parameters: {} },
      { stepType: "BO_LIFECYCLE_REPORT", order: 2, critical: false, enabled: true, parameters: {} },
      { stepType: "EOD_VALIDATE", order: 3, critical: true, enabled: true, parameters: {} },
      { stepType: "PORTFOLIO_PRICING", order: 4, critical: false, enabled: true, parameters: {} },
      { stepType: "EXPOSURE", order: 5, critical: false, enabled: true, parameters: {} },
      { stepType: "CVA", order: 6, critical: false, enabled: false, parameters: {} },
    ],
  };
}
