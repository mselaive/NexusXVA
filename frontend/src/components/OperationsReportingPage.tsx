"use client";

import React from "react";
import { AlertTriangle, CalendarX2, CheckCircle2, Clock3, Loader2, RefreshCw, RotateCcw } from "lucide-react";
import { nexusApi } from "@/lib/api";
import { formatNumber } from "@/lib/format";
import type { BackOfficeOperationsReport } from "@/lib/types";
import { AppShell } from "./AppShell";

const howTo = [
  {
    title: "Purpose",
    body: "Operations Reporting gives BO one place to monitor pending validations, lifecycle queue pressure and EOD close coverage.",
  },
  {
    title: "Missing close",
    body: "A portfolio is missing today's close when it has no active EOD snapshot for the current UTC business date.",
  },
  {
    title: "Corrections",
    body: "Corrected runs count VOIDED and SUPERSEDED EOD records. They stay in history for audit instead of being deleted.",
  },
];

export function OperationsReportingPage() {
  const [report, setReport] = React.useState<BackOfficeOperationsReport | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState(false);

  async function loadReport() {
    setLoading(true);
    setError(null);
    try {
      setReport(await nexusApi.getBackOfficeOperationsReport());
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Operations report unavailable");
    } finally {
      setLoading(false);
    }
  }

  React.useEffect(() => {
    void loadReport();
  }, []);

  return (
    <AppShell title="Operations Reporting" eyebrow="Back Office reporting" howTo={howTo}>
      {error ? <div className="alert">{error}</div> : null}

      <div className="panel section">
        <div className="valuation-filter-head">
          <div>
            <span className="page-eyebrow">Daily controls</span>
            <h2>Bookings, lifecycle and EOD coverage</h2>
          </div>
          <button className="btn secondary" type="button" onClick={loadReport} disabled={loading}>
            {loading ? <Loader2 size={16} className="spin" /> : <RefreshCw size={16} />}
            Refresh
          </button>
        </div>
        <p className="muted">
          This report is derived from existing operational data. It does not create accounting entries or new EOD snapshots.
        </p>
      </div>

      <div className="summary-strip section">
        <MetricCard icon={<Clock3 size={17} />} label="Pending bookings" value={formatNumber(report?.pendingTradeBookings ?? 0, 0)} />
        <MetricCard icon={<AlertTriangle size={17} />} label="Pending lifecycle" value={formatNumber(report?.pendingLifecycleRequests ?? 0, 0)} />
        <MetricCard icon={<CalendarX2 size={17} />} label="Missing close" value={formatNumber(report?.portfoliosWithoutTodayClose ?? 0, 0)} />
        <MetricCard icon={<RotateCcw size={17} />} label="Corrected EOD runs" value={formatNumber(report?.correctedEodRuns ?? 0, 0)} />
      </div>

      <section className="panel section">
        <div className="section-header">
          <div>
            <span className="page-eyebrow">{report?.businessDate ?? "UTC business date"}</span>
            <h2>EOD portfolio status</h2>
          </div>
          <CheckCircle2 size={19} />
        </div>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Portfolio</th>
                <th>Currency</th>
                <th>Positions</th>
                <th>Latest EOD</th>
                <th>Status</th>
                <th>Missing today</th>
                <th>Corrections</th>
              </tr>
            </thead>
            <tbody>
              {(report?.eodPortfolios ?? []).map((portfolio) => (
                <tr key={portfolio.portfolioId}>
                  <td>{portfolio.portfolioName}</td>
                  <td>{portfolio.baseCurrency}</td>
                  <td>{formatNumber(portfolio.positionCount, 0)}</td>
                  <td>{portfolio.latestEodDate ?? "No close"}</td>
                  <td>
                    <span className={`booking-status ${portfolio.latestEodStatus === "ACTIVE" ? "confirmed" : portfolio.latestEodStatus === "MISSING" ? "pending_validation" : "rejected"}`}>
                      {portfolio.latestEodStatus}
                    </span>
                  </td>
                  <td>{portfolio.missingTodayClose ? "Yes" : "No"}</td>
                  <td>{formatNumber(portfolio.correctedRuns, 0)}</td>
                </tr>
              ))}
              {!report || report.eodPortfolios.length === 0 ? (
                <tr>
                  <td colSpan={7}>No portfolios available for BO reporting.</td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </section>
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
