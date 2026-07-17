"use client";

import React from "react";
import { ArrowDown, ArrowUp, CalendarClock, CheckCircle2, Plus, Save, Trash2, XCircle } from "lucide-react";
import { nexusApi } from "@/lib/api";
import type { AdminPortfolioSummary, CloseChecklistStepDefinition, CreditCurve, DiscountCurve, ExecuteScriptTemplate, OperationalControlSettings, UpdateOperationalControlRequest } from "@/lib/types";
import { useOperationalControlStatus } from "@/lib/operationalControl";
import { AppShell } from "./AppShell";

const days = [
  ["MONDAY", "Mon"],
  ["TUESDAY", "Tue"],
  ["WEDNESDAY", "Wed"],
  ["THURSDAY", "Thu"],
  ["FRIDAY", "Fri"],
  ["SATURDAY", "Sat"],
  ["SUNDAY", "Sun"],
] as const;

const howTo = [
  { title: "Trading window", body: "The window defines open/closed business time. ADMIN can block trade capture, risk runs, both, or neither." },
  { title: "EOD schedule", body: "The backend scheduler checks this database setting every minute, so changing the time does not require a restart." },
  { title: "BO remains open", body: "BO validation and EOD corrections stay available outside trading hours." },
];

export function OperationalControlPage() {
  const { status, refresh } = useOperationalControlStatus();
  const [settings, setSettings] = React.useState<OperationalControlSettings | null>(null);
  const [form, setForm] = React.useState<UpdateOperationalControlRequest | null>(null);
  const [portfolios, setPortfolios] = React.useState<AdminPortfolioSummary[]>([]);
  const [creditCurves, setCreditCurves] = React.useState<CreditCurve[]>([]);
  const [discountCurves, setDiscountCurves] = React.useState<DiscountCurve[]>([]);
  const [scriptTemplates, setScriptTemplates] = React.useState<ExecuteScriptTemplate[]>([]);
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
      const [next, nextPortfolios, nextCreditCurves, nextDiscountCurves, nextTemplates] = await Promise.all([
        nexusApi.getOperationalControlSettings(),
        nexusApi.listAdminPortfolios(),
        nexusApi.listCreditCurves(undefined, false),
        nexusApi.listDiscountCurves(undefined, false),
        nexusApi.listAdminExecuteScriptTemplates(),
      ]);
      setSettings(next);
      setForm(toForm(next));
      setPortfolios(nextPortfolios);
      setCreditCurves(nextCreditCurves);
      setDiscountCurves(nextDiscountCurves);
      setScriptTemplates(nextTemplates.filter((template) => template.active && !template.steps.some((step) => step.enabled && step.stepType === "EOD_CAPTURE")));
      setError(null);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "Operational control unavailable");
    } finally {
      setLoading(false);
    }
  }

  async function save() {
    if (!form) {
      return;
    }
    try {
      setSaving(true);
      const updated = await nexusApi.updateOperationalControlSettings(form);
      setSettings(updated);
      setForm(toForm(updated));
      setMessage("Operational control updated.");
      setError(null);
      await refresh();
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "Could not save operational control");
    } finally {
      setSaving(false);
    }
  }

  function patch(patch: Partial<UpdateOperationalControlRequest>) {
    setForm((current) => current ? { ...current, ...patch } : current);
    setMessage(null);
  }

  function toggleDay(day: string) {
    if (!form) {
      return;
    }
    const hasDay = form.businessDays.includes(day);
    patch({
      businessDays: hasDay
        ? form.businessDays.filter((candidate) => candidate !== day)
        : [...form.businessDays, day],
    });
  }

  function patchChecklist(patchValue: Partial<UpdateOperationalControlRequest["closeChecklist"]>) {
    if (!form) return;
    patch({ closeChecklist: { ...form.closeChecklist, ...patchValue } });
  }

  function togglePortfolio(portfolioId: string) {
    if (!form) return;
    const current = form.closeChecklist.portfolioIds;
    patchChecklist({
      portfolioIds: current.includes(portfolioId)
        ? current.filter((id) => id !== portfolioId)
        : [...current, portfolioId],
    });
  }

  function updateStep(index: number, patchValue: Partial<CloseChecklistStepDefinition>) {
    if (!form) return;
    patchChecklist({
      steps: form.closeChecklist.steps.map((step, stepIndex) => stepIndex === index ? { ...step, ...patchValue } : step),
    });
  }

  function addScriptBlock() {
    if (!form || scriptTemplates.length === 0) return;
    const eodIndex = form.closeChecklist.steps.findIndex((step) => step.stepType === "EOD");
    const insertAt = eodIndex >= 0 ? eodIndex : form.closeChecklist.steps.length;
    const next = [...form.closeChecklist.steps];
    next.splice(insertAt, 0, {
      phase: "PRE_EOD",
      stepType: "SCRIPT_TEMPLATE",
      templateId: scriptTemplates[0].id,
      scriptMode: "DRY_RUN",
      enabled: true,
      critical: false,
      order: 1,
    });
    patchChecklist({ steps: normalizeSequence(next) });
  }

  function moveStep(index: number, direction: -1 | 1) {
    if (!form) return;
    const target = index + direction;
    if (target < 0 || target >= form.closeChecklist.steps.length) return;
    const next = [...form.closeChecklist.steps];
    [next[index], next[target]] = [next[target], next[index]];
    patchChecklist({ steps: normalizeSequence(next) });
  }

  function removeStep(index: number) {
    if (!form || form.closeChecklist.steps[index].stepType === "EOD") return;
    patchChecklist({ steps: normalizeSequence(form.closeChecklist.steps.filter((_, stepIndex) => stepIndex !== index)) });
  }

  function patchRiskDefaults(patchValue: Partial<UpdateOperationalControlRequest["closeChecklist"]["riskDefaults"]>) {
    if (!form) return;
    patchChecklist({ riskDefaults: { ...form.closeChecklist.riskDefaults, ...patchValue } });
  }

  return (
    <AppShell title="Operational Control" eyebrow="Administration" howTo={howTo}>
      <section className="ops-control-hero">
        <div className={`ops-status-card ${status?.tradingOpen ? "open" : status?.operationalWindowEnforced ? "closed" : "advisory"}`}>
          {status?.tradingOpen ? <CheckCircle2 size={22} /> : <XCircle size={22} />}
          <span>{status?.tradingOpen ? "Trading Open" : status?.operationalWindowEnforced ? "Trading Closed" : "Window Advisory"}</span>
          <strong>{status?.reason?.replaceAll("_", " ") ?? "Loading"}</strong>
        </div>
        <div className="ops-status-grid">
          <MiniStatus label="Timezone" value={status?.timezone ?? settings?.timezone ?? "—"} />
          <MiniStatus label="Current business time" value={status ? new Date(status.currentBusinessTime).toLocaleString() : "—"} />
          <MiniStatus label="Next open" value={status ? new Date(status.nextOpenAt).toLocaleString() : "—"} />
          <MiniStatus label="Next EOD" value={status?.eodEnabled ? new Date(status.nextEodAt).toLocaleString() : "Disabled"} />
        </div>
      </section>

      {error ? <div className="panel-error">{error}</div> : null}
      {message ? <div className="success-banner">{message}</div> : null}

      <section className="panel section">
        <div className="section-heading ops-control-heading">
          <div>
            <span className="page-eyebrow">Global policy</span>
            <h2>Trading window and EOD schedule</h2>
          </div>
          <button className="btn" type="button" onClick={save} disabled={saving || loading || !form}>
            <Save size={16} />
            {saving ? "Saving..." : "Save control"}
          </button>
        </div>

        {!form ? <div className="empty">Loading operational control...</div> : (
          <div className="ops-control-layout">
            <div className="ops-form-card">
              <h3>Trading window</h3>
              <label className="ops-toggle-row">
                <span>
                  <strong>Block new trade bookings</strong>
                  <small>FO bookings and lifecycle requests return 409 outside the configured window.</small>
                </span>
                <input
                  type="checkbox"
                  checked={form.blockTradeBookingsOutsideWindow}
                  onChange={(event) => patch({ blockTradeBookingsOutsideWindow: event.target.checked })}
                />
              </label>
              <label className="ops-toggle-row">
                <span>
                  <strong>Block risk runs</strong>
                  <small>Pricing, Pre-Trade Analysis, Stress, Delta Hedge, Exposure and CVA return 409 outside the window.</small>
                </span>
                <input
                  type="checkbox"
                  checked={form.blockRiskRunsOutsideWindow}
                  onChange={(event) => patch({ blockRiskRunsOutsideWindow: event.target.checked })}
                />
              </label>
              <label>
                <span>Timezone</span>
                <input value={form.timezone} onChange={(event) => patch({ timezone: event.target.value })} />
              </label>
              <div className="ops-time-grid">
                <label>
                  <span>Open</span>
                  <input type="time" value={form.tradingOpenTime} onChange={(event) => patch({ tradingOpenTime: event.target.value })} />
                </label>
                <label>
                  <span>Close</span>
                  <input type="time" value={form.tradingCloseTime} onChange={(event) => patch({ tradingCloseTime: event.target.value })} />
                </label>
              </div>
              <div className="ops-days">
                {days.map(([day, label]) => (
                  <button
                    className={form.businessDays.includes(day) ? "active" : ""}
                    key={day}
                    type="button"
                    onClick={() => toggleDay(day)}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>

            <div className="ops-form-card">
              <h3>EOD scheduler</h3>
              <label className="ops-toggle-row">
                <span>
                  <strong>Automatic EOD</strong>
                  <small>Runs once per business date after the configured time.</small>
                </span>
                <input
                  type="checkbox"
                  checked={form.eodEnabled}
                  onChange={(event) => patch({ eodEnabled: event.target.checked })}
                />
              </label>
              <label>
                <span>EOD run time</span>
                <input type="time" value={form.eodRunTime} onChange={(event) => patch({ eodRunTime: event.target.value })} />
              </label>
              <label className="ops-toggle-row">
                <span>
                  <strong>Allow stale market data</strong>
                  <small>Keep off unless BO explicitly accepts stale cached inputs for close.</small>
                </span>
                <input
                  type="checkbox"
                  checked={form.eodAllowStaleMarketData}
                  onChange={(event) => patch({ eodAllowStaleMarketData: event.target.checked })}
                />
              </label>
            </div>
          </div>
        )}
      </section>

      {form ? (
        <section className="panel section">
          <div className="section-heading ops-control-heading">
            <div>
              <span className="page-eyebrow">Close checklist</span>
              <h2>Before EOD, EOD and after EOD runs</h2>
              <p className="muted">ADMIN configures the playbook. BO can run it manually from EOD Control, and the scheduler uses it when enabled.</p>
            </div>
          </div>

          <div className="ops-control-layout">
            <div className="ops-form-card">
              <h3>Checklist scope</h3>
              <label className="ops-toggle-row">
                <span>
                  <strong>Enable close checklist</strong>
                  <small>When enabled, scheduled close runs the configured checklist instead of plain EOD.</small>
                </span>
                <input
                  type="checkbox"
                  checked={form.closeChecklist.enabled}
                  onChange={(event) => patchChecklist({ enabled: event.target.checked })}
                />
              </label>
              <div className="checklist-portfolio-list">
                {portfolios.map((portfolio) => (
                  <label className="checklist-portfolio-option" key={portfolio.id}>
                    <input
                      type="checkbox"
                      checked={form.closeChecklist.portfolioIds.includes(portfolio.id)}
                      onChange={() => togglePortfolio(portfolio.id)}
                    />
                    <span>{portfolio.name}</span>
                    <small>{portfolio.baseCurrency} · {portfolio.positionCount} positions</small>
                  </label>
                ))}
              </div>
            </div>

            <div className="ops-form-card">
              <h3>Risk defaults</h3>
              <div className="ops-time-grid">
                <label>
                  <span>Horizon days</span>
                  <input type="number" min="1" value={form.closeChecklist.riskDefaults.horizonDays} onChange={(event) => patchRiskDefaults({ horizonDays: Number(event.target.value) })} />
                </label>
                <label>
                  <span>Time steps</span>
                  <input type="number" min="1" value={form.closeChecklist.riskDefaults.timeSteps} onChange={(event) => patchRiskDefaults({ timeSteps: Number(event.target.value) })} />
                </label>
                <label>
                  <span>Paths</span>
                  <input type="number" min="1" value={form.closeChecklist.riskDefaults.paths} onChange={(event) => patchRiskDefaults({ paths: Number(event.target.value) })} />
                </label>
                <label>
                  <span>Seed</span>
                  <input type="number" value={form.closeChecklist.riskDefaults.seed} onChange={(event) => patchRiskDefaults({ seed: Number(event.target.value) })} />
                </label>
                <label>
                  <span>PFE confidence</span>
                  <input type="number" min="0.01" max="0.99" step="0.01" value={form.closeChecklist.riskDefaults.pfeConfidenceLevel} onChange={(event) => patchRiskDefaults({ pfeConfidenceLevel: Number(event.target.value) })} />
                </label>
                <label>
                  <span>LGD</span>
                  <input type="number" min="0" max="1" step="0.01" value={form.closeChecklist.riskDefaults.lossGivenDefault} onChange={(event) => patchRiskDefaults({ lossGivenDefault: Number(event.target.value) })} />
                </label>
              </div>
              <label>
                <span>Credit curve</span>
                <select value={form.closeChecklist.riskDefaults.creditCurveId ?? ""} onChange={(event) => patchRiskDefaults({ creditCurveId: event.target.value || null })}>
                  <option value="">Select approved credit curve</option>
                  {creditCurves.map((curve) => (
                    <option key={curve.id} value={curve.id}>{curve.counterpartyName} · {curve.name} v{curve.version}</option>
                  ))}
                </select>
              </label>
              <label>
                <span>Discount curve</span>
                <select value={form.closeChecklist.riskDefaults.discountCurveId ?? ""} onChange={(event) => patchRiskDefaults({ discountCurveId: event.target.value || null })}>
                  <option value="">Select approved discount curve</option>
                  {discountCurves.map((curve) => (
                    <option key={curve.id} value={curve.id}>{curve.currency} · {curve.name} v{curve.version}</option>
                  ))}
                </select>
              </label>
            </div>
          </div>

          <div className="close-sequence-panel">
            <div className="section-header">
              <div>
                <h3>Close sequence</h3>
                <p className="muted">Scripts above Official EOD are pre-close checks. Scripts below it run after the official close.</p>
              </div>
              <button className="btn secondary" type="button" onClick={addScriptBlock} disabled={scriptTemplates.length === 0}>
                <Plus size={16} /> Add script
              </button>
            </div>
            {scriptTemplates.length === 0 ? <div className="alert">Create and activate an ExecuteScript template without EOD Capture before adding it here.</div> : null}
            <div className="close-sequence-list">
            {form.closeChecklist.steps.map((step, index) => (
              <React.Fragment key={`${step.stepType}-${step.templateId ?? "official"}-${index}`}>
                {index > 0 ? <div className="close-sequence-connector"><ArrowDown size={18} /></div> : null}
                <div className={`close-sequence-block ${step.stepType === "EOD" ? "official" : "script"}`}>
                  <div className="close-sequence-order">{index + 1}</div>
                  <div className="close-sequence-main">
                    <span className="page-eyebrow">{step.stepType === "EOD" ? "OFFICIAL CLOSE" : step.phase.replaceAll("_", " ")}</span>
                    <strong>{step.stepType === "EOD" ? "Official EOD" : step.stepType === "SCRIPT_TEMPLATE" ? templateName(scriptTemplates, step.templateId) : stepLabel(step.stepType)}</strong>
                    <small>{step.stepType === "EOD" ? "Creates the official close used by Daily P&L." : step.stepType === "SCRIPT_TEMPLATE" ? "Reusable ExecuteScript template" : "Legacy checklist action; replace it with a script template when ready."}</small>
                  </div>
                  {step.stepType === "SCRIPT_TEMPLATE" ? (
                    <label className="close-sequence-template">
                      <span>Script</span>
                      <select value={step.templateId ?? ""} onChange={(event) => updateStep(index, { templateId: event.target.value })}>
                        {scriptTemplates.map((template) => <option key={template.id} value={template.id}>{template.name}</option>)}
                      </select>
                    </label>
                  ) : null}
                  {step.stepType === "SCRIPT_TEMPLATE" ? (
                    <label className="close-sequence-mode">
                      <span>Mode</span>
                      <select value={step.scriptMode ?? "DRY_RUN"} onChange={(event) => updateStep(index, { scriptMode: event.target.value as "DRY_RUN" | "REAL_RUN" })}>
                        <option value="DRY_RUN">Dry run</option>
                        <option value="REAL_RUN">Real run</option>
                      </select>
                    </label>
                  ) : null}
                  <label className="inline-check"><input type="checkbox" checked={step.critical} onChange={(event) => updateStep(index, { critical: event.target.checked })} /> Critical</label>
                  <div className="close-sequence-actions">
                    <button className="icon-btn" type="button" title="Move up" disabled={index === 0} onClick={() => moveStep(index, -1)}><ArrowUp size={17} /></button>
                    <button className="icon-btn" type="button" title="Move down" disabled={index === form.closeChecklist.steps.length - 1} onClick={() => moveStep(index, 1)}><ArrowDown size={17} /></button>
                    {step.stepType !== "EOD" ? <button className="icon-btn danger" type="button" title="Remove script" onClick={() => removeStep(index)}><Trash2 size={17} /></button> : null}
                  </div>
                </div>
              </React.Fragment>
            ))}
            </div>
          </div>
        </section>
      ) : null}

      <section className="panel muted-panel">
        <CalendarClock size={18} />
        <span>
          Operational Control V1 is global. Holidays, per-market sessions and overnight trading windows are future slices.
        </span>
      </section>
    </AppShell>
  );
}

function MiniStatus({ label, value }: { label: string; value: string }) {
  return (
    <div className="mini-status">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function toForm(settings: OperationalControlSettings): UpdateOperationalControlRequest {
  return {
    timezone: settings.timezone,
    businessDays: settings.businessDays,
    tradingOpenTime: settings.tradingOpenTime,
    tradingCloseTime: settings.tradingCloseTime,
    blockTradeBookingsOutsideWindow: settings.blockTradeBookingsOutsideWindow,
    blockRiskRunsOutsideWindow: settings.blockRiskRunsOutsideWindow,
    eodEnabled: settings.eodEnabled,
    eodRunTime: settings.eodRunTime,
    eodAllowStaleMarketData: settings.eodAllowStaleMarketData,
    closeChecklist: settings.closeChecklist ?? defaultCloseChecklist(),
  };
}

function defaultCloseChecklist(): UpdateOperationalControlRequest["closeChecklist"] {
  return {
    enabled: false,
    portfolioIds: [],
    steps: [
      { phase: "PRE_EOD", stepType: "BO_OPERATIONS_REPORT", enabled: true, critical: true, order: 10 },
      { phase: "PRE_EOD", stepType: "BO_LIFECYCLE_REPORT", enabled: true, critical: false, order: 20 },
      { phase: "EOD", stepType: "EOD", enabled: true, critical: true, order: 30 },
      { phase: "POST_EOD", stepType: "PORTFOLIO_PRICING", enabled: true, critical: false, order: 40 },
      { phase: "POST_EOD", stepType: "EXPOSURE", enabled: false, critical: false, order: 50 },
      { phase: "POST_EOD", stepType: "CVA", enabled: false, critical: false, order: 60 },
      { phase: "POST_EOD", stepType: "FO_PNL_REPORT", enabled: true, critical: false, order: 70 },
    ],
    riskDefaults: {
      horizonDays: 365,
      timeSteps: 12,
      paths: 1000,
      seed: 12345,
      pfeConfidenceLevel: 0.95,
      lossGivenDefault: 0.6,
      creditCurveId: null,
      discountCurveId: null,
    },
  };
}

function stepLabel(stepType: string) {
  return stepType.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase());
}

function normalizeSequence(steps: CloseChecklistStepDefinition[]): CloseChecklistStepDefinition[] {
  const eodIndex = steps.findIndex((step) => step.stepType === "EOD");
  return steps.map((step, index) => ({
    ...step,
    enabled: true,
    order: (index + 1) * 10,
    phase: step.stepType === "EOD" ? "EOD" : index < eodIndex ? "PRE_EOD" : "POST_EOD",
  }));
}

function templateName(templates: ExecuteScriptTemplate[], templateId?: string | null) {
  return templates.find((template) => template.id === templateId)?.name ?? "Select script template";
}
