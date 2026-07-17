"use client";

import React from "react";
import { Archive, BarChart3, History, Loader2, RefreshCw } from "lucide-react";
import { nexusApi } from "@/lib/api";
import { formatNumber } from "@/lib/format";
import type { ReportSnapshot, ReportSnapshotType } from "@/lib/types";
import { AppShell } from "./AppShell";

const howTo = [
  {
    title: "What gets stored",
    body: "FO/BO report snapshots store the rendered report JSON, summary, user, group and timestamp. They are audit/reporting history, not a source for future calculations.",
  },
  {
    title: "FO visibility",
    body: "Front Office sees only its own report snapshots. Back Office sees BO reports. Admin can inspect every snapshot.",
  },
  {
    title: "EOD vs report history",
    body: "EOD remains the official close/P&L reference. Report History preserves what the workstation showed at a point in time.",
  },
];

export function ReportSnapshotsPage() {
  const [snapshots, setSnapshots] = React.useState<ReportSnapshot[]>([]);
  const [selected, setSelected] = React.useState<ReportSnapshot | null>(null);
  const [reportType, setReportType] = React.useState<ReportSnapshotType | "">("");
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  async function load() {
    setLoading(true);
    setError(null);
    try {
      const next = await nexusApi.listReportSnapshots({ reportType, limit: 100 });
      setSnapshots(next);
      setSelected((current) => next.find((snapshot) => snapshot.id === current?.id) ?? next[0] ?? null);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Report history unavailable");
    } finally {
      setLoading(false);
    }
  }

  React.useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reportType]);

  return (
    <AppShell title="Report History" eyebrow="Persisted reporting snapshots" howTo={howTo}>
      {error ? <div className="alert">{error}</div> : null}
      <section className="panel section">
        <div className="valuation-filter-head">
          <div>
            <span className="page-eyebrow">Filters</span>
            <h2>Saved report views</h2>
          </div>
          <button className="btn secondary" type="button" onClick={load} disabled={loading}>
            {loading ? <Loader2 size={16} /> : <RefreshCw size={16} />}
            Refresh
          </button>
        </div>
        <div className="toolbar valuation-filters">
          <label className="field compact-field">
            <span>Report type</span>
            <select className="input" value={reportType} onChange={(event) => setReportType(event.target.value as ReportSnapshotType | "")}>
              <option value="">All</option>
              <option value="FO_PNL_SNAPSHOT">FO P&L Snapshot</option>
              <option value="BO_OPERATIONS">BO Operations</option>
              <option value="BO_LIFECYCLE">BO Lifecycle</option>
            </select>
          </label>
        </div>
      </section>

      <div className="summary-strip section">
        <MetricCard icon={<Archive size={17} />} label="Loaded snapshots" value={formatNumber(snapshots.length, 0)} />
        <MetricCard icon={<BarChart3 size={17} />} label="FO P&L" value={formatNumber(snapshots.filter((item) => item.reportType === "FO_PNL_SNAPSHOT").length, 0)} />
        <MetricCard icon={<History size={17} />} label="BO reports" value={formatNumber(snapshots.filter((item) => item.activeGroupCode === "BO").length, 0)} />
      </div>

      <div className="valuation-runs-layout">
        <div className="panel">
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Created</th>
                  <th>Report</th>
                  <th>Scope</th>
                  <th>User</th>
                  <th>Business date</th>
                </tr>
              </thead>
              <tbody>
                {snapshots.map((snapshot) => (
                  <tr
                    className={selected?.id === snapshot.id ? "selected-row" : ""}
                    key={snapshot.id}
                    onClick={() => setSelected(snapshot)}
                  >
                    <td>{formatDateTime(snapshot.createdAt)}</td>
                    <td>
                      <strong>{snapshot.title}</strong>
                      <small className="table-subtext">{snapshot.reportType.replaceAll("_", " ")}</small>
                    </td>
                    <td>{snapshot.scopeName ?? snapshot.scopeType}</td>
                    <td>{snapshot.requestedByDisplayName ?? snapshot.requestedByUsername ?? "System"}</td>
                    <td>{snapshot.businessDate ?? "N/A"}</td>
                  </tr>
                ))}
                {snapshots.length === 0 ? (
                  <tr>
                    <td colSpan={5}>No report snapshots match the current filters.</td>
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
                  <span className="page-eyebrow">{selected.reportType.replaceAll("_", " ")}</span>
                  <h2>{selected.title}</h2>
                </div>
                <span className="status-chip success-chip">{selected.activeGroupCode ?? "SYSTEM"}</span>
              </div>
              <div className="detail-grid">
                <Detail label="Created" value={formatDateTime(selected.createdAt)} />
                <Detail label="Business date" value={selected.businessDate ?? "N/A"} />
                <Detail label="Scope" value={selected.scopeName ?? selected.scopeType} />
                <Detail label="Requested by" value={selected.requestedByDisplayName ?? selected.requestedByUsername ?? "System"} />
              </div>
              <JsonBlock title="Summary" value={selected.summary} />
              <JsonBlock title="Filters" value={selected.filters} />
              <JsonBlock title="Result" value={selected.result} />
            </>
          ) : (
            <div className="empty-state">Open FO P&L, BO Operations or BO Lifecycle Reporting to create snapshots.</div>
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
