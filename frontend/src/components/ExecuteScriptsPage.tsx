"use client";

import React from "react";
import { Loader2, Play, RefreshCw, SquareTerminal } from "lucide-react";
import { nexusApi } from "@/lib/api";
import type { ExecuteScriptMode, ExecuteScriptRun, ExecuteScriptTemplate, PortfolioSummary } from "@/lib/types";
import { AppShell } from "./AppShell";

const howTo = [
  { title: "Dry-run", body: "Dry-run keeps results inside ExecuteScript history and does not create EOD closes or official valuation/report snapshots." },
  { title: "Real-run", body: "Real-run can create report snapshots, valuation runs and EOD captures depending on the template steps." },
  { title: "Critical steps", body: "If a critical step fails, remaining steps are skipped so BO can fix the issue first." },
];

export function ExecuteScriptsPage() {
  const [templates, setTemplates] = React.useState<ExecuteScriptTemplate[]>([]);
  const [portfolios, setPortfolios] = React.useState<PortfolioSummary[]>([]);
  const [runs, setRuns] = React.useState<ExecuteScriptRun[]>([]);
  const [selectedRun, setSelectedRun] = React.useState<ExecuteScriptRun | null>(null);
  const [templateId, setTemplateId] = React.useState("");
  const [mode, setMode] = React.useState<ExecuteScriptMode>("DRY_RUN");
  const [businessDate, setBusinessDate] = React.useState(todayIsoDate());
  const [selectedPortfolioIds, setSelectedPortfolioIds] = React.useState<string[]>([]);
  const [parametersText, setParametersText] = React.useState(defaultParametersText());
  const [loading, setLoading] = React.useState(true);
  const [running, setRunning] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [message, setMessage] = React.useState<string | null>(null);

  React.useEffect(() => {
    void load();
  }, []);

  async function load() {
    try {
      setLoading(true);
      const [nextTemplates, nextPortfolios, nextRuns] = await Promise.all([
        nexusApi.listBackOfficeExecuteScriptTemplates(),
        nexusApi.listBackOfficeEodPortfolios(),
        nexusApi.listExecuteScriptRuns(20),
      ]);
      setTemplates(nextTemplates);
      setPortfolios(nextPortfolios);
      setRuns(nextRuns);
      setTemplateId((current) => current || nextTemplates[0]?.id || "");
      setSelectedPortfolioIds((current) => current.length > 0 ? current : nextPortfolios.slice(0, 3).map((portfolio) => portfolio.id));
      setSelectedRun(nextRuns[0] ?? null);
      setError(null);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "ExecuteScript unavailable");
    } finally {
      setLoading(false);
    }
  }

  function togglePortfolio(portfolioId: string) {
    setSelectedPortfolioIds((current) => current.includes(portfolioId)
      ? current.filter((id) => id !== portfolioId)
      : [...current, portfolioId]);
  }

  async function runScript() {
    if (!templateId) {
      setError("Select a template.");
      return;
    }
    if (selectedPortfolioIds.length === 0) {
      setError("Select at least one portfolio.");
      return;
    }
    let parameters: unknown;
    try {
      parameters = JSON.parse(parametersText || "{}");
    } catch {
      setError("Parameters must be valid JSON.");
      return;
    }
    const warning = mode === "REAL_RUN"
      ? "Run ExecuteScript in REAL_RUN mode? This may create report snapshots, valuation runs or EOD closes."
      : "Run ExecuteScript in DRY_RUN mode?";
    if (!window.confirm(warning)) {
      return;
    }
    try {
      setRunning(true);
      const run = await nexusApi.runExecuteScript({
        templateId,
        mode,
        businessDate,
        portfolioIds: selectedPortfolioIds,
        parameters,
      });
      const nextRuns = await nexusApi.listExecuteScriptRuns(20);
      setRuns(nextRuns);
      setSelectedRun(run);
      setMessage(`ExecuteScript ${run.status}: ${run.message ?? "completed"}.`);
      setError(null);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Could not run ExecuteScript");
    } finally {
      setRunning(false);
    }
  }

  const selectedTemplate = templates.find((template) => template.id === templateId) ?? null;

  return (
    <AppShell title="Execute Scripts" eyebrow="Back Office diagnostics" howTo={howTo}>
      {error ? <div className="alert">{error}</div> : null}
      {message ? <div className="success">{message}</div> : null}

      <section className="panel section">
        <div className="section-header">
          <div>
            <h2>Run operational script</h2>
            <p className="muted">Use dry-run to test reporting/risk/EOD readiness before running the formal close.</p>
          </div>
          <SquareTerminal size={22} />
        </div>
        <div className="form-grid">
          <label className="field">
            <span>Template</span>
            <select className="input" value={templateId} onChange={(event) => setTemplateId(event.target.value)}>
              {templates.map((template) => <option key={template.id} value={template.id}>{template.name}</option>)}
            </select>
          </label>
          <label className="field">
            <span>Mode</span>
            <select className="input" value={mode} onChange={(event) => setMode(event.target.value as ExecuteScriptMode)}>
              <option value="DRY_RUN">DRY RUN</option>
              <option value="REAL_RUN">REAL RUN</option>
            </select>
          </label>
          <label className="field">
            <span>Business date</span>
            <input className="input" type="date" value={businessDate} onChange={(event) => setBusinessDate(event.target.value)} />
          </label>
        </div>

        {selectedTemplate ? (
          <div className="script-template-preview">
            <strong>{selectedTemplate.name}</strong>
            <span>{selectedTemplate.description || "No description"}</span>
            <small>{selectedTemplate.steps.filter((step) => step.enabled).length} enabled steps</small>
          </div>
        ) : null}

        <div className="execute-script-layout">
          <div>
            <h3>Portfolios</h3>
            <div className="checklist-portfolio-list">
              {portfolios.map((portfolio) => (
                <label className="checklist-portfolio-option" key={portfolio.id}>
                  <input type="checkbox" checked={selectedPortfolioIds.includes(portfolio.id)} onChange={() => togglePortfolio(portfolio.id)} />
                  <span>
                    <strong>{portfolio.name}</strong>
                    <small>{portfolio.baseCurrency} · {portfolio.positionCount} positions</small>
                  </span>
                </label>
              ))}
            </div>
          </div>
          <label className="field">
            <span>Run parameters JSON</span>
            <textarea className="input json-input tall-json-input" value={parametersText} onChange={(event) => setParametersText(event.target.value)} />
          </label>
        </div>

        <div className="form-actions">
          <button className="btn" type="button" onClick={runScript} disabled={running || loading || templates.length === 0}>
            {running ? <Loader2 size={16} /> : <Play size={16} />}
            {running ? "Running..." : "Run script"}
          </button>
          <button className="btn secondary" type="button" onClick={load} disabled={running}>
            <RefreshCw size={16} />
            Refresh
          </button>
        </div>
      </section>

      <section className="panel section">
        <div className="section-header">
          <div>
            <h2>Run history</h2>
            <p className="muted">Every dry-run and real-run keeps step outputs for review.</p>
          </div>
        </div>
        <div className="checklist-run-layout">
          <div className="checklist-run-list">
            {runs.length === 0 ? <div className="empty-state">No script runs yet.</div> : runs.map((run) => (
              <button className={`checklist-run-item ${selectedRun?.id === run.id ? "active" : ""}`} key={run.id} type="button" onClick={() => setSelectedRun(run)}>
                <span>{run.templateName}</span>
                <strong>{run.mode} · {run.status}</strong>
                <small>{run.businessDate} · {new Date(run.startedAt).toLocaleString()}</small>
              </button>
            ))}
          </div>
          <div className="checklist-run-detail">
            {!selectedRun ? <div className="empty-state">Select a script run.</div> : (
              <>
                <div className="detail-grid">
                  <div className="detail-item"><span>Template</span><strong>{selectedRun.templateName}</strong></div>
                  <div className="detail-item"><span>Mode</span><strong>{selectedRun.mode}</strong></div>
                  <div className="detail-item"><span>Status</span><strong>{selectedRun.status}</strong></div>
                  <div className="detail-item"><span>Business date</span><strong>{selectedRun.businessDate}</strong></div>
                </div>
                <div className="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>Step</th>
                        <th>Status</th>
                        <th>Critical</th>
                        <th>Message</th>
                      </tr>
                    </thead>
                    <tbody>
                      {selectedRun.steps.map((step) => (
                        <tr key={step.id}>
                          <td>{step.stepType.replaceAll("_", " ")}</td>
                          <td><span className={`booking-status ${step.status.toLowerCase()}`}>{step.status}</span></td>
                          <td>{step.critical ? "Yes" : "No"}</td>
                          <td>{step.message ?? "-"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <label className="field">
                  <span>Selected run output</span>
                  <textarea className="input json-input tall-json-input" readOnly value={JSON.stringify(selectedRun, null, 2)} />
                </label>
              </>
            )}
          </div>
        </div>
      </section>
    </AppShell>
  );
}

function todayIsoDate() {
  return new Date().toISOString().slice(0, 10);
}

function defaultParametersText() {
  return JSON.stringify({
    valuationDate: todayIsoDate(),
    exposure: { horizonDays: 365, timeSteps: 12, paths: 1000, seed: 12345, pfeConfidenceLevel: 0.95 },
    cva: { lossGivenDefault: 0.6, creditCurveId: null, discountCurveId: null },
  }, null, 2);
}
