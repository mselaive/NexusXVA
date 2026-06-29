"use client";

import React from "react";
import { CalendarCheck, CheckCircle2, Clock3, Loader2, RefreshCw, RotateCcw, ShieldCheck, XCircle } from "lucide-react";
import { nexusApi, NexusApiError } from "@/lib/api";
import { formatCurrency, formatNumber } from "@/lib/format";
import type { EodBatchResult, PortfolioEodSnapshot, PortfolioSummary } from "@/lib/types";
import { AppShell } from "./AppShell";

const howTo = [
  { title: "Who closes", body: "Only Back Office or the configured system scheduler can create an EOD close. Front Office consumes the result." },
  { title: "Audited close", body: "EOD runs are not deleted. BO can void or recapture a close with a reason, preserving the audit trail." },
  { title: "Quality gates", body: "The close fails when active positions are unpriceable or market data is stale, unless stale data is explicitly allowed by configuration." },
  { title: "Daily P&L", body: "Existing positions compare against prior EOD. Trades created after that close compare against their execution value." },
];

export function EodControlPage() {
  const [portfolios, setPortfolios] = React.useState<PortfolioSummary[]>([]);
  const [selectedId, setSelectedId] = React.useState("");
  const [businessDate, setBusinessDate] = React.useState(todayIsoDate());
  const [latest, setLatest] = React.useState<PortfolioEodSnapshot | null>(null);
  const [history, setHistory] = React.useState<PortfolioEodSnapshot[]>([]);
  const [batchResult, setBatchResult] = React.useState<EodBatchResult | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [capturing, setCapturing] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [success, setSuccess] = React.useState<string | null>(null);

  React.useEffect(() => {
    void loadPortfolios();
  }, []);

  React.useEffect(() => {
    if (selectedId) {
      void loadEod(selectedId);
    }
  }, [selectedId]);

  async function loadPortfolios() {
    setLoading(true);
    setError(null);
    try {
      const items = await nexusApi.listBackOfficeEodPortfolios();
      setPortfolios(items);
      setSelectedId((current) => current || items[0]?.id || "");
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  async function loadEod(portfolioId: string) {
    setLoading(true);
    setError(null);
    try {
      const snapshots = await nexusApi.getBackOfficePortfolioEodHistory(portfolioId, 20);
      setHistory(snapshots);
      setLatest(snapshots[0] ?? null);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  async function captureAll() {
    if (!window.confirm(`Run immutable EOD close for all ${portfolios.length} portfolios on ${businessDate}?`)) {
      return;
    }
    setCapturing(true);
    setError(null);
    setSuccess(null);
    try {
      const result = await nexusApi.runBackOfficeEodBatch(businessDate);
      setBatchResult(result);
      setSuccess(`EOD batch completed: ${result.captured} captured, ${result.skipped} skipped, ${result.failed} failed.`);
      if (selectedId) {
        await loadEod(selectedId);
      }
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setCapturing(false);
    }
  }

  async function voidRun(snapshot: PortfolioEodSnapshot) {
    const reason = window.prompt(`Why should EOD ${snapshot.businessDate} be voided?`);
    if (reason == null) {
      return;
    }
    if (!reason.trim()) {
      setError("Void reason is required.");
      return;
    }
    setCapturing(true);
    setError(null);
    setSuccess(null);
    try {
      await nexusApi.voidBackOfficeEodRun(snapshot.id, reason.trim());
      setSuccess(`EOD ${snapshot.businessDate} voided.`);
      await loadEod(snapshot.portfolioId);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setCapturing(false);
    }
  }

  async function recaptureRun(snapshot: PortfolioEodSnapshot) {
    const reason = window.prompt(`Why should EOD ${snapshot.businessDate} be recaptured?`);
    if (reason == null) {
      return;
    }
    if (!reason.trim()) {
      setError("Recapture reason is required.");
      return;
    }
    setCapturing(true);
    setError(null);
    setSuccess(null);
    try {
      await nexusApi.recaptureBackOfficeEodRun(snapshot.id, reason.trim());
      setSuccess(`EOD ${snapshot.businessDate} recaptured with a new active snapshot.`);
      await loadEod(snapshot.portfolioId);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setCapturing(false);
    }
  }

  const selected = portfolios.find((portfolio) => portfolio.id === selectedId) ?? null;

  return (
    <AppShell title="EOD Control" eyebrow="Back Office close process" howTo={howTo}>
      {error ? <div className="alert">{error}</div> : null}
      {success ? <div className="success">{success}</div> : null}

      <section className="panel section">
        <div className="section-header">
          <div>
            <h2>Run global EOD</h2>
            <p className="muted">Close every portfolio for the selected business date. Each book is processed independently and reported below.</p>
          </div>
          <ShieldCheck size={22} />
        </div>
        <div className="toolbar">
          <label className="field compact-field">
            <span>Business date</span>
            <input className="input" type="date" value={businessDate} onChange={(event) => setBusinessDate(event.target.value)} />
          </label>
          <button className="btn" type="button" onClick={captureAll} disabled={portfolios.length === 0 || capturing}>
            {capturing ? <Loader2 size={16} /> : <CalendarCheck size={16} />}
            Run EOD for all portfolios
          </button>
        </div>
      </section>

      <div className="summary-strip eod-summary-strip">
        <Metric label="Portfolios" value={formatNumber(batchResult?.totalPortfolios ?? portfolios.length, 0)} />
        <Metric label="Captured" value={formatNumber(batchResult?.captured ?? 0, 0)} />
        <Metric label="Skipped" value={formatNumber(batchResult?.skipped ?? 0, 0)} />
        <Metric label="Failed" value={formatNumber(batchResult?.failed ?? 0, 0)} />
      </div>

      {batchResult ? (
        <section className="panel section eod-batch-results">
          <div className="section-header">
            <div>
              <h2>Batch results</h2>
              <p className="muted">Review exceptions before considering the close process complete.</p>
            </div>
            <CheckCircle2 size={22} />
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Portfolio</th>
                  <th>Status</th>
                  <th>Message</th>
                </tr>
              </thead>
              <tbody>
                {batchResult.portfolios.map((result) => (
                  <tr key={result.portfolioId}>
                    <td>{result.portfolioName}</td>
                    <td><span className={`booking-status ${result.status.toLowerCase()}`}>{result.status}</span></td>
                    <td>{result.message}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}

      <section className="panel section eod-history-panel">
        <div className="section-header">
          <div>
            <h2>Portfolio close history</h2>
            <p className="muted">Select one portfolio to inspect its immutable closes after the global run.</p>
          </div>
          <Clock3 size={22} />
        </div>
        <div className="toolbar eod-history-toolbar">
          <label className="field">
            <span>Portfolio</span>
            <select className="select" value={selectedId} onChange={(event) => setSelectedId(event.target.value)}>
              {portfolios.map((portfolio) => (
                <option key={portfolio.id} value={portfolio.id}>{portfolio.name}</option>
              ))}
            </select>
          </label>
          <button className="btn secondary" type="button" onClick={() => loadEod(selectedId)} disabled={!selectedId || loading}>
            <RefreshCw size={16} />
            Refresh history
          </button>
          <div className="eod-selected-close">
            <span>{selected?.name ?? "No portfolio"}</span>
            <strong>{latest?.businessDate ?? "No close"}</strong>
          </div>
        </div>
        {loading ? (
          <div className="empty-state">Loading EOD history...</div>
        ) : history.length === 0 ? (
          <div className="empty-state">No EOD closes for this portfolio.</div>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Status</th>
                  <th>Market value</th>
                  <th>Trade value</th>
                  <th>Unrealized P&L</th>
                  <th>Missing economics</th>
                  <th>Positions</th>
                  <th>Source</th>
                  <th>Captured</th>
                  <th>Correction</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {history.map((snapshot) => (
                  <tr key={snapshot.id}>
                    <td><span className="status-inline"><CheckCircle2 size={14} /> {snapshot.businessDate}</span></td>
                    <td><span className={`booking-status ${snapshot.status.toLowerCase()}`}>{snapshot.status}</span></td>
                    <td>{formatCurrency(snapshot.totalMarketValue, snapshot.baseCurrency)}</td>
                    <td>{formatCurrency(snapshot.totalTradeValue, snapshot.baseCurrency)}</td>
                    <td>{formatCurrency(snapshot.totalUnrealizedPnl, snapshot.baseCurrency)}</td>
                    <td>{formatNumber(snapshot.positionsWithoutExecutionPrice, 0)}</td>
                    <td>{formatNumber(snapshot.positions.length, 0)}</td>
                    <td>{snapshot.source}</td>
                    <td>{new Date(snapshot.capturedAt).toLocaleString()}</td>
                    <td>
                      {snapshot.voidReason ? (
                        <span title={snapshot.voidReason}>{snapshot.voidedAt ? new Date(snapshot.voidedAt).toLocaleString() : "Corrected"}</span>
                      ) : snapshot.correctionOfRunId ? (
                        "Correction"
                      ) : (
                        "-"
                      )}
                    </td>
                    <td>
                      {snapshot.status === "ACTIVE" ? (
                        <div className="table-actions">
                          <button className="icon-btn" type="button" onClick={() => voidRun(snapshot)} disabled={capturing} title="Void EOD">
                            <XCircle size={15} />
                          </button>
                          <button className="icon-btn" type="button" onClick={() => recaptureRun(snapshot)} disabled={capturing} title="Recapture EOD">
                            <RotateCcw size={15} />
                          </button>
                        </div>
                      ) : (
                        <span className="muted">Locked</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </AppShell>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function todayIsoDate() {
  return new Date().toISOString().slice(0, 10);
}

function errorMessage(caught: unknown) {
  if (caught instanceof NexusApiError || caught instanceof Error) {
    return caught.message;
  }
  return "EOD operation failed";
}
