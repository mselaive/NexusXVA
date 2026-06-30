"use client";

import React from "react";
import { AlertTriangle, CheckCircle2, Clock3, GitPullRequest, Loader2, RefreshCw, Repeat2, XCircle } from "lucide-react";
import { nexusApi } from "@/lib/api";
import { formatNumber } from "@/lib/format";
import type { LifecycleAgingBucket, LifecycleBreakdown, TradeLifecycleReport } from "@/lib/types";
import { AppShell } from "./AppShell";

const howTo = [
  {
    title: "Purpose",
    body: "Lifecycle Reporting summarizes amendment and cancellation requests. It helps BO see queue pressure without changing the maker-checker workflow.",
  },
  {
    title: "Pending aging",
    body: "Pending aging is calculated from submittedAt until now. Older buckets should be reviewed first.",
  },
  {
    title: "Review time",
    body: "Average review time uses approved and rejected lifecycle requests. Pending items are excluded.",
  },
];

export function LifecycleReportingPage() {
  const [report, setReport] = React.useState<TradeLifecycleReport | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState(false);

  async function loadReport() {
    setLoading(true);
    setError(null);
    try {
      setReport(await nexusApi.getBackOfficeLifecycleReport());
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Lifecycle report unavailable");
    } finally {
      setLoading(false);
    }
  }

  React.useEffect(() => {
    void loadReport();
  }, []);

  return (
    <AppShell title="Lifecycle Reporting" eyebrow="Back Office reporting" howTo={howTo}>
      {error ? <div className="alert">{error}</div> : null}

      <div className="panel section">
        <div className="valuation-filter-head">
          <div>
            <span className="page-eyebrow">Lifecycle workload</span>
            <h2>Amendments and cancellations</h2>
          </div>
          <button className="btn secondary" type="button" onClick={loadReport} disabled={loading}>
            {loading ? <Loader2 size={16} className="spin" /> : <RefreshCw size={16} />}
            Refresh
          </button>
        </div>
        <p className="muted">
          This report is recalculated from lifecycle requests. It is operational reporting, not a separate accounting ledger.
        </p>
      </div>

      <div className="summary-strip section">
        <MetricCard icon={<GitPullRequest size={17} />} label="Total requests" value={formatNumber(report?.total ?? 0, 0)} />
        <MetricCard icon={<Clock3 size={17} />} label="Pending BO" value={formatNumber(report?.pendingValidation ?? 0, 0)} />
        <MetricCard icon={<CheckCircle2 size={17} />} label="Approved" value={formatNumber(report?.approved ?? 0, 0)} />
        <MetricCard icon={<XCircle size={17} />} label="Rejected" value={formatNumber(report?.rejected ?? 0, 0)} />
      </div>

      <div className="lifecycle-report-grid section">
        <section className="panel lifecycle-report-card">
          <div className="section-header">
            <div>
              <span className="page-eyebrow">Queue health</span>
              <h2>Pending aging</h2>
            </div>
            <AlertTriangle size={19} />
          </div>
          <AgingBars buckets={report?.pendingAgingBuckets ?? []} total={report?.pendingValidation ?? 0} />
          <div className="detail-grid lifecycle-detail-grid">
            <Detail label="Oldest pending" value={report?.oldestPendingSubmittedAt ? formatDateTime(report.oldestPendingSubmittedAt) : "No pending"} />
            <Detail label="Avg review time" value={report?.averageReviewMinutes == null ? "No reviewed requests" : `${formatNumber(report.averageReviewMinutes, 0)} min`} />
          </div>
        </section>

        <section className="panel lifecycle-report-card">
          <div className="section-header">
            <div>
              <span className="page-eyebrow">Request mix</span>
              <h2>Amend vs cancel</h2>
            </div>
            <Repeat2 size={19} />
          </div>
          <div className="lifecycle-mix">
            <MixCard label="Amendments" value={report?.amendments ?? 0} total={report?.total ?? 0} />
            <MixCard label="Cancellations" value={report?.cancellations ?? 0} total={report?.total ?? 0} />
          </div>
        </section>
      </div>

      <div className="lifecycle-report-grid section">
        <BreakdownTable title="By portfolio" rows={report?.byPortfolio ?? []} emptyText="No lifecycle requests by portfolio yet." />
        <BreakdownTable title="By symbol" rows={report?.bySymbol ?? []} emptyText="No lifecycle requests by symbol yet." />
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

function AgingBars({ buckets, total }: { buckets: LifecycleAgingBucket[]; total: number }) {
  if (buckets.length === 0) {
    return <div className="empty-state">No pending lifecycle requests.</div>;
  }
  return (
    <div className="aging-bars">
      {buckets.map((bucket) => {
        const width = total === 0 ? 0 : Math.max(6, Math.round((bucket.count / total) * 100));
        return (
          <div className="aging-row" key={bucket.label}>
            <span>{bucket.label}</span>
            <div className="aging-track">
              <div style={{ width: `${width}%` }} />
            </div>
            <strong>{formatNumber(bucket.count, 0)}</strong>
          </div>
        );
      })}
    </div>
  );
}

function MixCard({ label, value, total }: { label: string; value: number; total: number }) {
  const percentage = total === 0 ? 0 : (value / total) * 100;
  return (
    <div className="mix-card">
      <span>{label}</span>
      <strong>{formatNumber(value, 0)}</strong>
      <em>{formatNumber(percentage, 1)}%</em>
    </div>
  );
}

function BreakdownTable({ title, rows, emptyText }: { title: string; rows: LifecycleBreakdown[]; emptyText: string }) {
  return (
    <section className="panel">
      <div className="section-header">
        <div>
          <span className="page-eyebrow">Top 10</span>
          <h2>{title}</h2>
        </div>
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Total</th>
              <th>Pending</th>
              <th>Approved</th>
              <th>Rejected</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.key}>
                <td>{row.label}</td>
                <td>{formatNumber(row.total, 0)}</td>
                <td>{formatNumber(row.pendingValidation, 0)}</td>
                <td>{formatNumber(row.approved, 0)}</td>
                <td>{formatNumber(row.rejected, 0)}</td>
              </tr>
            ))}
            {rows.length === 0 ? (
              <tr>
                <td colSpan={5}>{emptyText}</td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </section>
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

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
