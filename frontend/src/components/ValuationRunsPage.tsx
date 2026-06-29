"use client";

import React from "react";
import { Activity, AlertTriangle, CheckCircle2, History, Loader2, RefreshCw } from "lucide-react";
import { nexusApi } from "@/lib/api";
import { formatNumber } from "@/lib/format";
import type { ValuationRun, ValuationRunStatus, ValuationRunType } from "@/lib/types";
import { AppShell } from "./AppShell";
import { PortfolioPicker } from "./PortfolioPicker";

const howTo = [
  {
    title: "What gets stored",
    body: "Pricing, Exposure and CVA store the request, response, summary, user and timestamp. The stored run is audit history, not a source for future calculations.",
  },
  {
    title: "Failures",
    body: "If a calculation reaches the backend and fails during valuation, NexusXVA stores a FAILED run with a clean error message.",
  },
  {
    title: "Portfolio access",
    body: "Users only see runs for portfolios they can access. Archived portfolios keep their historical runs.",
  },
];

export function ValuationRunsPage() {
  const [runs, setRuns] = React.useState<ValuationRun[]>([]);
  const [selected, setSelected] = React.useState<ValuationRun | null>(null);
  const [runType, setRunType] = React.useState<ValuationRunType | "">("");
  const [status, setStatus] = React.useState<ValuationRunStatus | "">("");
  const [portfolioId, setPortfolioId] = React.useState("");
  const [error, setError] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState(false);

  async function loadRuns() {
    setLoading(true);
    setError(null);
    try {
      const nextRuns = await nexusApi.listValuationRuns({
        runType,
        status,
        portfolioId: portfolioId || undefined,
        limit: 100,
      });
      setRuns(nextRuns);
      setSelected((current) => nextRuns.find((run) => run.id === current?.id) ?? nextRuns[0] ?? null);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Run history unavailable");
    } finally {
      setLoading(false);
    }
  }

  React.useEffect(() => {
    void loadRuns();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [runType, status, portfolioId]);

  const successes = runs.filter((run) => run.status === "SUCCESS").length;
  const failures = runs.filter((run) => run.status === "FAILED").length;

  return (
    <AppShell title="Run History" eyebrow="Persisted valuation audit" howTo={howTo}>
      {error ? <div className="alert">{error}</div> : null}
      <div className="panel section">
        <div className="valuation-filter-head">
          <div>
            <span className="page-eyebrow">Filters</span>
            <h2>Valuation runs</h2>
          </div>
          <button className="btn secondary" type="button" onClick={loadRuns} disabled={loading}>
            {loading ? <Loader2 size={16} /> : <RefreshCw size={16} />}
            Refresh
          </button>
        </div>
        <div className="toolbar valuation-filters">
          <div className="valuation-portfolio-filter">
            <PortfolioPicker value={portfolioId} onChange={setPortfolioId} onError={setError} autoSelectFirst={false} />
          </div>
          <label className="field compact-field">
            <span>Run type</span>
            <select className="input" value={runType} onChange={(event) => setRunType(event.target.value as ValuationRunType | "")}>
              <option value="">All</option>
              <option value="PRICING">Pricing</option>
              <option value="EXPOSURE">Exposure</option>
              <option value="CVA">CVA</option>
            </select>
          </label>
          <label className="field compact-field">
            <span>Status</span>
            <select className="input" value={status} onChange={(event) => setStatus(event.target.value as ValuationRunStatus | "")}>
              <option value="">All</option>
              <option value="SUCCESS">Success</option>
              <option value="FAILED">Failed</option>
            </select>
          </label>
        </div>
      </div>

      <div className="summary-strip section">
        <MetricCard icon={<History size={17} />} label="Loaded runs" value={formatNumber(runs.length, 0)} />
        <MetricCard icon={<CheckCircle2 size={17} />} label="Successful" value={formatNumber(successes, 0)} />
        <MetricCard icon={<AlertTriangle size={17} />} label="Failed" value={formatNumber(failures, 0)} />
        <MetricCard icon={<Activity size={17} />} label="Latest model" value={runs[0]?.model ?? "No runs"} />
      </div>

      <div className="valuation-runs-layout">
        <div className="panel">
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Created</th>
                  <th>Portfolio</th>
                  <th>Type</th>
                  <th>Status</th>
                  <th>Model</th>
                  <th>User</th>
                </tr>
              </thead>
              <tbody>
                {runs.map((run) => (
                  <tr
                    className={selected?.id === run.id ? "selected-row" : ""}
                    key={run.id}
                    onClick={() => setSelected(run)}
                  >
                    <td>{formatDateTime(run.createdAt)}</td>
                    <td>{run.portfolioName}</td>
                    <td>{run.runType}</td>
                    <td>
                      <span className={`status-chip ${run.status === "SUCCESS" ? "success-chip" : "danger-chip"}`}>
                        {run.status}
                      </span>
                    </td>
                    <td>{run.model}</td>
                    <td>{run.requestedByDisplayName ?? run.requestedByUsername ?? "System"}</td>
                  </tr>
                ))}
                {runs.length === 0 ? (
                  <tr>
                    <td colSpan={6}>No valuation runs match the current filters.</td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </div>

        <div className="panel valuation-detail">
          {selected ? (
            <>
              <div className="valuation-detail-head">
                <div>
                  <span className="page-eyebrow">{selected.runType}</span>
                  <h2>{selected.portfolioName}</h2>
                </div>
                <span className={`status-chip ${selected.status === "SUCCESS" ? "success-chip" : "danger-chip"}`}>
                  {selected.status}
                </span>
              </div>
              <div className="detail-grid">
                <Detail label="Model" value={selected.model} />
                <Detail label="Valuation date" value={selected.valuationDate ?? "N/A"} />
                <Detail label="Requested by" value={selected.requestedByDisplayName ?? selected.requestedByUsername ?? "System"} />
                <Detail label="Group" value={selected.activeGroupCode ?? "N/A"} />
              </div>
              {selected.errorMessage ? <div className="alert">{selected.errorMessage}</div> : null}
              <JsonBlock title="Summary" value={selected.summary} />
              <JsonBlock title="Input" value={selected.input} />
              <JsonBlock title="Result" value={selected.result} />
            </>
          ) : (
            <div className="empty-state">Run a pricing, exposure or CVA calculation to populate history.</div>
          )}
        </div>
      </div>
    </AppShell>
  );
}

function MetricCard({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div className="metric-card">
      <span>{icon}</span>
      <small>{label}</small>
      <strong>{value}</strong>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="detail-item">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function JsonBlock({ title, value }: { title: string; value: unknown }) {
  if (value == null) {
    return null;
  }
  return (
    <details className="json-block" open={title === "Summary"}>
      <summary>{title}</summary>
      <pre>{JSON.stringify(value, null, 2)}</pre>
    </details>
  );
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
