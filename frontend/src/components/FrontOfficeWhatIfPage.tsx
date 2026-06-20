"use client";

import React from "react";
import { Activity, CircleDollarSign, FlaskConical, Loader2, RefreshCw, Send } from "lucide-react";
import { blembergApi, nexusApi, NexusApiError } from "@/lib/api";
import { formatCurrency, formatNumber, formatPercent, todayIsoDate } from "@/lib/format";
import type {
  AddEuropeanOptionPositionRequest,
  BlembergMarketSnapshot,
  FrontOfficeWhatIfResponse,
  OptionType,
  PortfolioPositionPricing,
  WhatIfPortfolioTotals,
} from "@/lib/types";
import { AppShell } from "./AppShell";
import { PortfolioPicker } from "./PortfolioPicker";

const howTo = [
  { title: "Purpose", body: "Pre-Trade Analysis prices a hypothetical European option before it is sent to BO." },
  { title: "No persistence", body: "The run does not create a booking, a confirmed position, or a stored valuation result." },
  { title: "Impact", body: "Impact is the incremental price and Greeks that would be added to the confirmed portfolio." },
  { title: "Market context", body: "Use notebooks and the latest Blemberg snapshot to compare strike versus current market before running the analysis." },
  { title: "Send to u-Pad", body: "If the result is acceptable, send the ticket to u-Pad. u-Pad is still the official booking screen." },
];

const notebooks = [
  {
    id: "faang",
    name: "FAANG+",
    symbols: ["AAPL", "MSFT", "NVDA", "AMZN", "GOOGL", "META", "TSLA", "AVGO", "ORCL", "AMD"],
  },
  {
    id: "banks",
    name: "Banks",
    symbols: ["JPM", "BAC", "GS", "MS", "C", "WFC"],
  },
  {
    id: "metals",
    name: "Metals",
    symbols: ["GLD", "SLV", "CPER"],
  },
  {
    id: "funds",
    name: "Funds",
    symbols: ["SPY", "QQQ", "DIA", "IWM", "VTI", "TLT"],
  },
];

const allNotebookSymbols = notebooks.flatMap((notebook) => notebook.symbols);

export function FrontOfficeWhatIfPage() {
  const [selectedId, setSelectedId] = React.useState(initialPortfolioIdFromUrl);
  const [valuationDate, setValuationDate] = React.useState(todayIsoDate());
  const [tradeForm, setTradeForm] = React.useState(initialTradeFormFromUrl);
  const [result, setResult] = React.useState<FrontOfficeWhatIfResponse | null>(null);
  const [analyzedTrade, setAnalyzedTrade] = React.useState<AddEuropeanOptionPositionRequest | null>(null);
  const [activeNotebookId, setActiveNotebookId] = React.useState(() => notebookIdForSymbol("AAPL"));
  const [marketSnapshots, setMarketSnapshots] = React.useState<BlembergMarketSnapshot[]>([]);
  const [missingSymbols, setMissingSymbols] = React.useState<string[]>([]);
  const [marketLoading, setMarketLoading] = React.useState(false);
  const [marketError, setMarketError] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState(false);
  const marketSnapshotBySymbol = new Map(marketSnapshots.map((snapshot) => [snapshot.symbol.toUpperCase(), snapshot]));
  const selectedSnapshot = marketSnapshotBySymbol.get(tradeForm.underlyingSymbol.toUpperCase());

  React.useEffect(() => {
    void loadMarketSnapshots();
  }, []);

  async function loadMarketSnapshots(symbols = allNotebookSymbols) {
    setMarketLoading(true);
    setMarketError(null);
    try {
      const response = await blembergApi.snapshots(symbols);
      setMarketSnapshots(response.snapshots ?? []);
      setMissingSymbols(response.missingSymbols ?? []);
    } catch (caught) {
      setMarketSnapshots([]);
      setMissingSymbols(allNotebookSymbols);
      setMarketError(errorMessage(caught));
    } finally {
      setMarketLoading(false);
    }
  }

  function pickSymbol(symbol: string, snapshot?: BlembergMarketSnapshot) {
    const lastPrice = snapshot?.lastPrice ?? marketSnapshotBySymbol.get(symbol.toUpperCase())?.lastPrice;
    setTradeForm((current) => ({
      ...current,
      underlyingSymbol: symbol,
      strike: lastPrice != null && Number.isFinite(lastPrice) ? formatTicketNumber(lastPrice) : current.strike,
    }));
  }

  async function runWhatIf() {
    if (!selectedId) {
      setError("Select a portfolio first.");
      return;
    }
    const trade: AddEuropeanOptionPositionRequest = {
      underlyingSymbol: tradeForm.underlyingSymbol.trim().toUpperCase(),
      optionType: tradeForm.optionType,
      strike: Number(tradeForm.strike),
      maturityDate: tradeForm.maturityDate,
      quantity: Number(tradeForm.quantity),
    };
    setLoading(true);
    setError(null);
    setResult(null);
    setAnalyzedTrade(null);
    try {
      setResult(await nexusApi.runFrontOfficeWhatIf({
        portfolioId: selectedId,
        valuationDate,
        trade,
      }));
      setAnalyzedTrade(trade);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AppShell title="Pre-Trade Analysis" eyebrow="Front Office analytics" howTo={howTo}>
      {error ? <div className="alert">{error}</div> : null}
      <div className="whatif-layout">
        <section className="panel whatif-ticket">
          <div className="section-title">
            <div>
              <h2>Hypothetical trade</h2>
              <p>Test one European option against confirmed portfolio positions before submitting it from u-Pad.</p>
            </div>
          </div>
          <PortfolioPicker value={selectedId} onChange={setSelectedId} onError={setError} />
          <MarketContext
            activeNotebookId={activeNotebookId}
            selectedSymbol={tradeForm.underlyingSymbol}
            optionType={tradeForm.optionType}
            strike={Number(tradeForm.strike)}
            snapshots={marketSnapshots}
            missingSymbols={missingSymbols}
            loading={marketLoading}
            error={marketError}
            onNotebookChange={setActiveNotebookId}
            onRefresh={() => loadMarketSnapshots()}
            onPick={pickSymbol}
          />
          <div className="form-grid">
            <label className="field full">
              <span>Valuation date</span>
              <input className="input" type="date" value={valuationDate} onChange={(event) => setValuationDate(event.target.value)} />
            </label>
            <label className="field">
              <span>Underlying</span>
              <input className="input ticker-input" value={tradeForm.underlyingSymbol} onChange={(event) => setTradeForm({ ...tradeForm, underlyingSymbol: event.target.value })} />
            </label>
            <label className="field">
              <span>Option type</span>
              <select className="select" value={tradeForm.optionType} onChange={(event) => setTradeForm({ ...tradeForm, optionType: event.target.value as OptionType })}>
                <option value="CALL">CALL</option>
                <option value="PUT">PUT</option>
              </select>
            </label>
            <label className="field">
              <span>Strike</span>
              <input className="input" type="number" min="0.01" step="0.01" value={tradeForm.strike} onChange={(event) => setTradeForm({ ...tradeForm, strike: event.target.value })} />
            </label>
            <label className="field">
              <span>Maturity</span>
              <input className="input" type="date" value={tradeForm.maturityDate} onChange={(event) => setTradeForm({ ...tradeForm, maturityDate: event.target.value })} />
            </label>
            <label className="field">
              <span>Quantity</span>
              <input className="input" type="number" step="1" value={tradeForm.quantity} onChange={(event) => setTradeForm({ ...tradeForm, quantity: event.target.value })} />
            </label>
            <div className="field">
              <span>&nbsp;</span>
              <button className="btn" type="button" onClick={runWhatIf} disabled={loading}>
                {loading ? <Loader2 size={16} /> : <FlaskConical size={16} />}
                Run Analysis
              </button>
            </div>
          </div>
          <div className="mini-note">
            Pre-Trade Analysis is stateless. Use u-Pad only when you are ready to send the trade to BO validation.
          </div>
        </section>

        <section className="panel whatif-result">
          <div className="section-title">
            <div>
              <h2>Pre-trade impact</h2>
              <p>Impact uses the same market-data pricing inputs and Black-Scholes engine as portfolio pricing.</p>
            </div>
          </div>
          {result && analyzedTrade ? (
            <WhatIfResult result={result} bookingUrl={upadUrl(result.portfolioId, analyzedTrade)} trade={analyzedTrade} snapshot={selectedSnapshot} />
          ) : (
            <EmptyWhatIf />
          )}
        </section>
      </div>
    </AppShell>
  );
}

function WhatIfResult({
  result,
  bookingUrl,
  trade,
  snapshot,
}: {
  result: FrontOfficeWhatIfResponse;
  bookingUrl: string;
  trade: AddEuropeanOptionPositionRequest;
  snapshot?: BlembergMarketSnapshot;
}) {
  return (
    <div className="whatif-result-stack">
      <StrikeContextCard
        optionType={trade.optionType}
        strike={trade.strike}
        marketSpot={snapshot?.lastPrice ?? result.hypotheticalTrade.marketData.spot}
        source={snapshot?.source ?? result.hypotheticalTrade.marketData.source}
        asOf={snapshot?.asOf ?? result.hypotheticalTrade.marketData.asOf}
      />
      <div className="whatif-summary-grid">
        <TotalsCard title="Base portfolio" icon={<CircleDollarSign size={18} />} totals={result.basePortfolio} />
        <HypotheticalTradeCard trade={result.hypotheticalTrade} />
        <TotalsCard title="With trade" icon={<Activity size={18} />} totals={result.withTradePortfolio} />
        <ImpactCard result={result} />
      </div>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Measure</th>
              <th>Base</th>
              <th>Impact</th>
              <th>With trade</th>
            </tr>
          </thead>
          <tbody>
            <WhatIfRow label="Price" base={result.basePortfolio.totalPrice} impact={result.impact.price} withTrade={result.withTradePortfolio.totalPrice} currency />
            <WhatIfRow label="Delta" base={result.basePortfolio.totalGreeks.delta} impact={result.impact.delta} withTrade={result.withTradePortfolio.totalGreeks.delta} />
            <WhatIfRow label="Gamma" base={result.basePortfolio.totalGreeks.gamma} impact={result.impact.gamma} withTrade={result.withTradePortfolio.totalGreeks.gamma} />
            <WhatIfRow label="Vega" base={result.basePortfolio.totalGreeks.vega} impact={result.impact.vega} withTrade={result.withTradePortfolio.totalGreeks.vega} />
            <WhatIfRow label="Theta" base={result.basePortfolio.totalGreeks.theta} impact={result.impact.theta} withTrade={result.withTradePortfolio.totalGreeks.theta} />
            <WhatIfRow label="Rho" base={result.basePortfolio.totalGreeks.rho} impact={result.impact.rho} withTrade={result.withTradePortfolio.totalGreeks.rho} />
          </tbody>
        </table>
      </div>
      <div className="whatif-actions">
        <div>
          <strong>Ready to book?</strong>
          <span>Send the analyzed ticket to u-Pad, review it, then submit it to BO validation.</span>
        </div>
        <a className="btn" href={bookingUrl}>
          <Send size={16} />
          Send to u-Pad
        </a>
      </div>
    </div>
  );
}

function MarketContext({
  activeNotebookId,
  selectedSymbol,
  optionType,
  strike,
  snapshots,
  missingSymbols,
  loading,
  error,
  onNotebookChange,
  onRefresh,
  onPick,
}: {
  activeNotebookId: string;
  selectedSymbol: string;
  optionType: OptionType;
  strike: number;
  snapshots: BlembergMarketSnapshot[];
  missingSymbols: string[];
  loading: boolean;
  error: string | null;
  onNotebookChange: (notebookId: string) => void;
  onRefresh: () => void;
  onPick: (symbol: string, snapshot?: BlembergMarketSnapshot) => void;
}) {
  const activeNotebook = notebooks.find((notebook) => notebook.id === activeNotebookId) ?? notebooks[0];
  const snapshotBySymbol = new Map(snapshots.map((snapshot) => [snapshot.symbol.toUpperCase(), snapshot]));
  const selectedSnapshot = snapshotBySymbol.get(selectedSymbol.toUpperCase());

  return (
    <div className="pretrade-market">
      <div className="pretrade-market-head">
        <div>
          <strong>Market context</strong>
          <span>{snapshots.length} live snapshots · {missingSymbols.length} missing</span>
        </div>
        <button className="icon-btn" type="button" onClick={onRefresh} disabled={loading} title="Refresh market snapshots" aria-label="Refresh market snapshots">
          {loading ? <Loader2 size={15} /> : <RefreshCw size={15} />}
        </button>
      </div>

      <div className="pretrade-notebooks" aria-label="Pre-trade market notebooks">
        {notebooks.map((notebook) => (
          <button
            className={activeNotebook.id === notebook.id ? "active" : ""}
            key={notebook.id}
            type="button"
            onClick={() => onNotebookChange(notebook.id)}
          >
            {notebook.name}
            <small>{notebook.symbols.filter((symbol) => snapshotBySymbol.has(symbol)).length}/{notebook.symbols.length}</small>
          </button>
        ))}
      </div>

      <div className="pretrade-symbols" aria-label="Pre-trade symbols">
        {activeNotebook.symbols.map((symbol) => {
          const snapshot = snapshotBySymbol.get(symbol);
          const selected = selectedSymbol.toUpperCase() === symbol;
          return (
            <button className={selected ? "active" : ""} key={symbol} type="button" onClick={() => onPick(symbol, snapshot)}>
              <span>{symbol}</span>
              <small>{snapshot?.lastPrice != null ? formatMarketNumber(snapshot.lastPrice) : "Missing"}</small>
            </button>
          );
        })}
      </div>

      {error ? <div className="mini-alert">{error}</div> : null}
      <StrikeContextCard
        optionType={optionType}
        strike={strike}
        marketSpot={selectedSnapshot?.lastPrice ?? null}
        source={selectedSnapshot?.source ?? "Blemberg cache"}
        asOf={selectedSnapshot?.asOf ?? null}
        compact
      />
    </div>
  );
}

function StrikeContextCard({
  optionType,
  strike,
  marketSpot,
  source,
  asOf,
  compact = false,
}: {
  optionType: OptionType;
  strike: number;
  marketSpot: number | null | undefined;
  source: string | null | undefined;
  asOf: string | null | undefined;
  compact?: boolean;
}) {
  const validSpot = marketSpot != null && Number.isFinite(marketSpot) && marketSpot > 0;
  const validStrike = Number.isFinite(strike) && strike > 0;
  const spread = validSpot && validStrike ? marketSpot - strike : null;
  const moneyness = validSpot && validStrike ? strike / marketSpot : null;
  const intrinsic = validSpot && validStrike
    ? optionType === "CALL"
      ? Math.max(marketSpot - strike, 0)
      : Math.max(strike - marketSpot, 0)
    : null;
  const status = validSpot && validStrike ? optionStatus(optionType, marketSpot, strike) : "Waiting for market";

  return (
    <div className={`strike-context ${compact ? "compact" : ""}`}>
      <div>
        <small>Strike vs market</small>
        <strong>{status}</strong>
      </div>
      <MarketMeasure label="Market last" value={validSpot ? formatCurrency(marketSpot) : "Missing"} />
      <MarketMeasure label="Strike" value={validStrike ? formatCurrency(strike) : "Invalid"} />
      <MarketMeasure label="Last - strike" value={spread == null ? "-" : formatCurrency(spread)} tone={spread == null ? undefined : spread >= 0 ? "positive" : "negative"} />
      <MarketMeasure label="Moneyness K/S" value={moneyness == null ? "-" : formatPercent(moneyness)} />
      {!compact ? <MarketMeasure label="Intrinsic/unit" value={intrinsic == null ? "-" : formatCurrency(intrinsic)} /> : null}
      <span className="strike-context-source">
        {asOf ? `${source ?? "Market data"} · ${formatShortDateTime(asOf)}` : source ?? "No Blemberg snapshot yet"}
      </span>
    </div>
  );
}

function MarketMeasure({ label, value, tone }: { label: string; value: string; tone?: "positive" | "negative" }) {
  return (
    <div className={`market-measure ${tone ?? ""}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function TotalsCard({ title, icon, totals }: { title: string; icon: React.ReactNode; totals: WhatIfPortfolioTotals }) {
  return (
    <div className="whatif-card">
      <span>{icon}</span>
      <small>{title}</small>
      <strong>{formatCurrency(totals.totalPrice)}</strong>
      <em>Delta {formatNumber(totals.totalGreeks.delta, 4)}</em>
    </div>
  );
}

function HypotheticalTradeCard({ trade }: { trade: PortfolioPositionPricing }) {
  return (
    <div className="whatif-card trade">
      <span><Send size={18} /></span>
      <small>Hypothetical trade</small>
      <strong>{formatCurrency(trade.positionPrice)}</strong>
      <em>{trade.quantity} {trade.underlyingSymbol} · spot {formatNumber(trade.marketData.spot, 2)}</em>
    </div>
  );
}

function ImpactCard({ result }: { result: FrontOfficeWhatIfResponse }) {
  const positive = result.impact.price >= 0;
  return (
    <div className={`whatif-card impact ${positive ? "positive" : "negative"}`}>
      <span><FlaskConical size={18} /></span>
      <small>Incremental impact</small>
      <strong>{formatCurrency(result.impact.price)}</strong>
      <em>Vega {formatNumber(result.impact.vega, 4)}</em>
    </div>
  );
}

function WhatIfRow({
  label,
  base,
  impact,
  withTrade,
  currency = false,
}: {
  label: string;
  base: number;
  impact: number;
  withTrade: number;
  currency?: boolean;
}) {
  const format = (value: number) => currency ? formatCurrency(value) : formatNumber(value, 4);
  return (
    <tr>
      <td>{label}</td>
      <td>{format(base)}</td>
      <td>{format(impact)}</td>
      <td>{format(withTrade)}</td>
    </tr>
  );
}

function EmptyWhatIf() {
  return (
    <div className="empty-state">
      <FlaskConical size={18} />
      <span>Run Pre-Trade Analysis to compare the confirmed portfolio against a hypothetical trade.</span>
    </div>
  );
}

function errorMessage(caught: unknown): string {
  if (caught instanceof NexusApiError) {
    return caught.message;
  }
  if (caught instanceof Error) {
    return caught.message;
  }
  return "Unexpected Pre-Trade Analysis error";
}

function initialPortfolioIdFromUrl() {
  if (typeof window === "undefined") {
    return "";
  }
  return new URLSearchParams(window.location.search).get("portfolioId") ?? "";
}

function initialTradeFormFromUrl() {
  const fallback = {
    underlyingSymbol: "AAPL",
    optionType: "CALL" as OptionType,
    strike: "190",
    maturityDate: "2027-06-01",
    quantity: "10",
  };
  if (typeof window === "undefined") {
    return fallback;
  }
  const params = new URLSearchParams(window.location.search);
  return {
    underlyingSymbol: (params.get("underlyingSymbol") ?? params.get("symbol") ?? fallback.underlyingSymbol).toUpperCase(),
    optionType: params.get("optionType") === "PUT" ? "PUT" as OptionType : "CALL" as OptionType,
    strike: params.get("strike") ?? fallback.strike,
    maturityDate: params.get("maturityDate") ?? fallback.maturityDate,
    quantity: params.get("quantity") ?? fallback.quantity,
  };
}

function notebookIdForSymbol(symbol: string) {
  return notebooks.find((notebook) => notebook.symbols.includes(symbol.toUpperCase()))?.id ?? notebooks[0].id;
}

function optionStatus(optionType: OptionType, spot: number, strike: number) {
  const intrinsic = optionType === "CALL" ? spot - strike : strike - spot;
  if (Math.abs(intrinsic / spot) <= 0.01) {
    return "Near the money";
  }
  return intrinsic > 0 ? "In the money" : "Out of the money";
}

function formatMarketNumber(value: number) {
  return value.toLocaleString("en-US", {
    maximumFractionDigits: value >= 100 ? 2 : 4,
    minimumFractionDigits: 2,
  });
}

function formatTicketNumber(value: number) {
  return value.toFixed(value >= 100 ? 2 : 4).replace(/\.?0+$/, "");
}

function formatShortDateTime(value: string) {
  return new Date(value).toLocaleString([], {
    month: "short",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function upadUrl(portfolioId: string, trade: AddEuropeanOptionPositionRequest) {
  const params = new URLSearchParams({
    source: "pre-trade-analysis",
    portfolioId,
    underlyingSymbol: trade.underlyingSymbol,
    optionType: trade.optionType,
    strike: String(trade.strike),
    maturityDate: trade.maturityDate,
    quantity: String(trade.quantity),
  });
  return `/upad?${params.toString()}`;
}
