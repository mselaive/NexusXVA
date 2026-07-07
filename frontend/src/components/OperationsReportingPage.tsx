"use client";

import React from "react";
import { AlertTriangle, CalendarX2, CheckCircle2, Clock3, Loader2, RefreshCw, RotateCcw } from "lucide-react";
import { nexusApi } from "@/lib/api";
import { formatCurrency, formatNumber } from "@/lib/format";
import type { BackOfficeEodPortfolioStatus, BackOfficeOperationsReport } from "@/lib/types";
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

type ReportFilter = "ALL" | "MISSING" | "CORRECTED" | "PNL_FAILED" | "OK";

export function OperationsReportingPage() {
  const [report, setReport] = React.useState<BackOfficeOperationsReport | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState(false);
  const [filter, setFilter] = React.useState<ReportFilter>("ALL");

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

  const rows = report?.eodPortfolios ?? [];
  const attentionRows = rows
    .filter((row) => row.missingTodayClose || row.latestCloseCorrected || row.pnlStatus === "FAILED" || row.noCloseEver)
    .slice(0, 8);
  const filteredRows = rows.filter((row) => matchesFilter(row, filter));

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
        <MetricCard icon={<AlertTriangle size={17} />} label="Failed P&L" value={formatNumber(report?.failedPnlPortfolios ?? 0, 0)} />
      </div>

      <section className="panel section operations-attention">
        <div className="section-header">
          <div>
            <span className="page-eyebrow">Attention required</span>
            <h2>Oldest queues and portfolio exceptions</h2>
          </div>
          <a className="btn secondary" href="/eod-control">Open EOD Control</a>
        </div>
        <div className="operations-queue-strip">
          <QueueStat label="Oldest trade booking" value={formatAge(report?.oldestPendingTradeBookingSubmittedAt)} />
          <QueueStat label="Oldest lifecycle request" value={formatAge(report?.oldestPendingLifecycleSubmittedAt)} />
          <QueueStat label="No close ever" value={formatNumber(report?.portfoliosWithNoCloseEver ?? 0, 0)} />
          <QueueStat label="Latest close corrected" value={formatNumber(report?.portfoliosWithCorrectedLatestClose ?? 0, 0)} />
        </div>
        {attentionRows.length === 0 ? (
          <div className="empty-state">No immediate BO exceptions. The close queue looks clean.</div>
        ) : (
          <div className="operations-attention-list">
            {attentionRows.map((portfolio) => (
              <div className="operations-attention-item" key={portfolio.portfolioId}>
                <div>
                  <strong>{portfolio.portfolioName}</strong>
                  <small>{portfolio.baseCurrency} · {formatNumber(portfolio.positionCount, 0)} positions</small>
                </div>
                <div className="operations-attention-tags">
                  {portfolio.noCloseEver ? <StatusPill tone="pending" text="No close ever" /> : null}
                  {portfolio.missingTodayClose ? <StatusPill tone="pending" text="Missing today" /> : null}
                  {portfolio.latestCloseCorrected ? <StatusPill tone="rejected" text={portfolio.latestEodStatus} /> : null}
                  {portfolio.pnlStatus === "FAILED" ? <StatusPill tone="rejected" text="P&L failed" /> : null}
                </div>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="panel section">
        <div className="section-header">
          <div>
            <span className="page-eyebrow">{report?.businessDate ?? "UTC business date"}</span>
            <h2>EOD portfolio status</h2>
          </div>
          <CheckCircle2 size={19} />
        </div>
        <div className="fo-tabs operations-filters" role="tablist" aria-label="Operations report filters">
          {[
            ["ALL", "All"],
            ["MISSING", "Missing close"],
            ["CORRECTED", "Corrected"],
            ["PNL_FAILED", "P&L failed"],
            ["OK", "OK"],
          ].map(([id, label]) => (
            <button
              className={filter === id ? "active" : ""}
              key={id}
              type="button"
              onClick={() => setFilter(id as ReportFilter)}
            >
              {label}
            </button>
          ))}
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
                <th>Market value</th>
                <th>Daily P&L</th>
                <th>Since trade P&L</th>
                <th>Options daily</th>
                <th>Cash daily</th>
                <th>Missing economics</th>
                <th>P&L status</th>
              </tr>
            </thead>
            <tbody>
              {filteredRows.map((portfolio) => (
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
                  <td>{formatMoney(portfolio.currentMarketValue, portfolio.baseCurrency)}</td>
                  <td>{formatMoney(portfolio.dailyPnl, portfolio.baseCurrency)}</td>
                  <td>{formatMoney(portfolio.sinceTradePnl, portfolio.baseCurrency)}</td>
                  <td>{formatMoney(portfolio.optionDailyPnl, portfolio.baseCurrency)}</td>
                  <td>{formatMoney(portfolio.cashEquityDailyPnl, portfolio.baseCurrency)}</td>
                  <td>{formatNumber((portfolio.positionsWithoutReference ?? 0) + (portfolio.positionsWithoutExecutionPrice ?? 0), 0)}</td>
                  <td>
                    <span className={`booking-status ${portfolio.pnlStatus === "OK" ? "confirmed" : "rejected"}`} title={portfolio.pnlErrorMessage ?? undefined}>
                      {portfolio.pnlStatus}
                    </span>
                  </td>
                </tr>
              ))}
              {!report || filteredRows.length === 0 ? (
                <tr>
                  <td colSpan={14}>No portfolios match this operations filter.</td>
                </tr>
              ) : null}
            </tbody>
          </table>
        </div>
      </section>
    </AppShell>
  );
}

function matchesFilter(row: BackOfficeEodPortfolioStatus, filter: ReportFilter) {
  if (filter === "MISSING") {
    return row.missingTodayClose || row.noCloseEver;
  }
  if (filter === "CORRECTED") {
    return row.latestCloseCorrected || row.correctedRuns > 0;
  }
  if (filter === "PNL_FAILED") {
    return row.pnlStatus === "FAILED";
  }
  if (filter === "OK") {
    return !row.missingTodayClose && !row.latestCloseCorrected && row.pnlStatus === "OK";
  }
  return true;
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

function QueueStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="detail-item">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function StatusPill({ tone, text }: { tone: "pending" | "rejected"; text: string }) {
  return <span className={`booking-status ${tone === "pending" ? "pending_validation" : "rejected"}`}>{text}</span>;
}

function formatMoney(value: number | null, currency: string) {
  return value == null ? "Unavailable" : formatCurrency(value, currency);
}

function formatAge(value: string | null | undefined) {
  if (!value) {
    return "None pending";
  }
  const minutes = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 60000));
  if (minutes < 60) {
    return `${minutes} min`;
  }
  const hours = Math.floor(minutes / 60);
  if (hours < 48) {
    return `${hours}h ${minutes % 60}m`;
  }
  return `${Math.floor(hours / 24)}d ${hours % 24}h`;
}
