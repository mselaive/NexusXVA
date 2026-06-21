"use client";

import React, { useEffect, useState } from "react";
import {
  Activity,
  Building2,
  CircleDollarSign,
  Cpu,
  FlaskConical,
  Gem,
  Landmark,
  Loader2,
  Plus,
  RefreshCw,
  Send,
  Shield,
  SquarePen,
  Wallet,
  Waves,
} from "lucide-react";
import { blembergApi, nexusApi, NexusApiError } from "@/lib/api";
import { formatCurrency, formatNumber, formatPercent, todayIsoDate } from "@/lib/format";
import type {
  AddEuropeanOptionPositionRequest,
  CvaCalculationResponse,
  ExposureSimulationResponse,
  OptionType,
  Portfolio,
  PortfolioPricingResponse,
  PortfolioSummary,
  BlembergMarketSnapshot,
  BlembergRefreshResponse,
  EuropeanOptionPosition,
  TradeBooking,
  TradeLifecycleRequest,
  TradingLimitSnapshot,
} from "@/lib/types";
import { AppShell } from "./AppShell";
import { ExposureChart } from "./ExposureChart";
import { InfoButton } from "./InfoButton";
import { PortfolioPicker } from "./PortfolioPicker";

type RunForm = {
  valuationDate: string;
  horizonDays: string;
  timeSteps: string;
  paths: string;
  seed: string;
  pfeConfidenceLevel: string;
  lossGivenDefault: string;
  counterpartyHazardRate: string;
  discountRate: string;
};

type TradeTicketForm = {
  underlyingSymbol: string;
  optionType: OptionType;
  strike: string;
  maturityDate: string;
  quantity: string;
};

const defaultRunForm: RunForm = {
  valuationDate: todayIsoDate(),
  horizonDays: "365",
  timeSteps: "12",
  paths: "1000",
  seed: "12345",
  pfeConfidenceLevel: "0.95",
  lossGivenDefault: "0.6",
  counterpartyHazardRate: "0.02",
  discountRate: "0.04",
};

const notebooks = [
  {
    id: "faang",
    name: "FAANG+",
    icon: Cpu,
    description: "Large-cap technology underlyings.",
    symbols: ["AAPL", "MSFT", "NVDA", "AMZN", "GOOGL", "META", "TSLA", "AVGO", "ORCL", "AMD"],
  },
  {
    id: "banks",
    name: "Banks",
    icon: Landmark,
    description: "US bank and broker-dealer names.",
    symbols: ["JPM", "BAC", "GS", "MS", "C", "WFC"],
  },
  {
    id: "metals",
    name: "Metals",
    icon: Gem,
    description: "ETF proxies for metals exposure.",
    symbols: ["GLD", "SLV", "CPER"],
  },
  {
    id: "funds",
    name: "Funds",
    icon: Building2,
    description: "ETF/index proxies used by NexusXVA V1.",
    symbols: ["SPY", "QQQ", "DIA", "IWM", "VTI", "TLT"],
  },
];

const allNotebookSymbols = notebooks.flatMap((notebook) => notebook.symbols);

const howTo = {
  overview: [
    { title: "Flow", body: "Use u-Pad to book trades, Pricing to value the portfolio, Exposure to simulate future values, and CVA to convert exposure into a credit adjustment." },
    { title: "Market data", body: "The frontend never talks to market providers directly. It reads NexusXVA and Blemberg cache through backend/proxy endpoints." },
  ],
  portfolios: [
    { title: "Create portfolio", body: "Create the book here first. A portfolio stores metadata and positions, not spot, volatility, rates, or dividends." },
    { title: "Inspect positions", body: "Select a portfolio to see its persisted European option positions. Valuation happens later in Pricing or Exposure." },
  ],
  upad: [
    { title: "Submit trade", body: "Choose an existing portfolio, enter the option terms, and send the booking to Back Office validation." },
    { title: "Pending bookings", body: "A submitted trade is not a portfolio position yet. It becomes confirmed only after BO approval." },
    { title: "Market watch", body: "The watch reads Blemberg snapshots. Missing symbols usually mean Blemberg has not refreshed them yet or the provider rate limit skipped them." },
  ],
  pricing: [
    { title: "1. Select portfolio", body: "Choose the book you want to value. Pricing reads the persisted positions and does not modify the portfolio." },
    { title: "2. Valuation date", body: "This is the date of the valuation. It is used to calculate time to maturity for each option." },
    { title: "3. Run pricing", body: "The backend asks marketdata for spot, volatility, risk-free rate, and dividend yield, then values each live option with Black-Scholes." },
    { title: "Read the result", body: "Total price and Greeks are portfolio totals. Position rows show unit price, position price, and the market data used." },
    { title: "Unpriceable rows", body: "Expired positions are excluded from totals and reported separately instead of failing the whole run." },
  ],
  exposure: [
    { title: "1. Select portfolio", body: "Pick a USD portfolio with live European option positions. Exposure V1 reprices those positions across simulated future dates." },
    { title: "2. Valuation date", body: "Starting date of the simulation. Positions already matured by this date do not contribute to exposure." },
    { title: "3. Horizon days", body: "How far forward the simulation runs. Use 365 for one year, 730 for two years, etc." },
    { title: "4. Time steps", body: "Number of future buckets in the exposure curve. More steps give a smoother profile but cost more compute." },
    { title: "5. Paths and seed", body: "Paths controls how many random scenarios are simulated. Seed makes the random run reproducible." },
    { title: "6. PFE confidence", body: "The percentile used for Potential Future Exposure. 0.95 means the 95th percentile at each bucket." },
    { title: "Read the chart", body: "EE is average positive exposure, ENE is average negative exposure, and PFE is the tail exposure percentile." },
  ],
  cva: [
    { title: "1. Start from exposure", body: "CVA V1 first runs the same exposure simulation, then applies default probabilities, LGD, and discounting." },
    { title: "2. LGD", body: "Loss Given Default. 0.6 means 60% of positive exposure is lost if the counterparty defaults." },
    { title: "3. Hazard rate", body: "Flat annual default intensity. Higher hazard rate increases default probability and usually increases CVA." },
    { title: "4. Discount rate", body: "Flat annual discount rate used to discount future exposure contributions back to valuation date." },
    { title: "Run CVA", body: "The output shows total CVA and bucket contributions with exposure, discount factor, survival probability, default increment, and contribution." },
    { title: "Curve mode later", body: "The backend supports credit and discount curves, but Dashboard V1 currently exposes the simpler flat mode." },
  ],
};

const runFieldHelp: Record<string, string> = {
  "Valuation date": "Today of the analysis. Exposure and CVA start from this date.",
  "Horizon days": "How far into the future the simulation runs. Example: 365 means one year.",
  "Time steps": "Number of future buckets. More steps produce a finer exposure curve but cost more compute.",
  Paths: "Number of Monte Carlo scenarios. More paths reduce noise but take longer.",
  Seed: "Random seed for reproducibility. Same inputs plus same seed should produce the same paths.",
  "PFE confidence": "Percentile used for Potential Future Exposure. 0.95 means the 95th percentile.",
  LGD: "Loss Given Default. 0.6 means 60% loss if the counterparty defaults.",
  "Hazard rate": "Flat annual default intensity used when no credit curve is provided.",
  "Discount rate": "Flat annual discount rate used when no discount curve is provided.",
};

const runPresets = [
  {
    name: "Fast check",
    description: "6M, monthly buckets, 500 paths",
    values: { horizonDays: "180", timeSteps: "6", paths: "500", pfeConfidenceLevel: "0.95" },
  },
  {
    name: "Market standard",
    description: "1Y, monthly buckets, 2,000 paths",
    values: { horizonDays: "365", timeSteps: "12", paths: "2000", pfeConfidenceLevel: "0.95" },
  },
  {
    name: "Tail review",
    description: "2Y, monthly buckets, 5,000 paths, 99% PFE",
    values: { horizonDays: "730", timeSteps: "24", paths: "5000", pfeConfidenceLevel: "0.99" },
  },
];

export function OverviewPage() {
  const [portfolios, setPortfolios] = useState<PortfolioSummary[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    nexusApi.listPortfolios().then(setPortfolios).catch((caught) => setError(errorMessage(caught)));
  }, []);

  const totalPositions = portfolios.reduce((sum, portfolio) => sum + portfolio.positionCount, 0);

  return (
    <AppShell title="Overview" eyebrow="Risk command center" howTo={howTo.overview}>
      <Alert message={error} />
      <div className="hero-band">
        <div>
          <span className="page-eyebrow">Portfolio to Pricing to Exposure to CVA</span>
          <h2>One workstation for the full NexusXVA flow.</h2>
        </div>
        <a className="btn" href="/upad">
          <SquarePen size={16} />
          Open u-Pad
        </a>
      </div>

      <div className="summary-strip">
        <Metric label="Portfolios" value={formatNumber(portfolios.length, 0)} />
        <Metric label="Positions" value={formatNumber(totalPositions, 0)} />
        <Metric label="Market data" value="Local/Blemberg" />
        <Metric label="CVA mode" value="Flat UI" />
      </div>

      <div className="workflow-lanes">
        <WorkflowLink href="/upad" icon={<SquarePen size={20} />} title="u-Pad" text="Book European option trades into existing portfolios." />
        <WorkflowLink href="/pre-trade-analysis" icon={<FlaskConical size={20} />} title="Pre-Trade Analysis" text="Test pre-trade price and Greeks before sending to BO." />
        <WorkflowLink href="/stress-testing" icon={<Waves size={20} />} title="Stress Testing" text="Shock spot, vol and rates across FO scenarios." />
        <WorkflowLink href="/pricing" icon={<CircleDollarSign size={20} />} title="Pricing" text="Run portfolio-level Black-Scholes valuation." />
        <WorkflowLink href="/exposure" icon={<Activity size={20} />} title="Exposure" text="Simulate GBM paths and inspect EE, ENE and PFE." />
        <WorkflowLink href="/cva" icon={<Shield size={20} />} title="CVA" text="Calculate simplified CVA from expected exposure." />
      </div>
    </AppShell>
  );
}

export function PortfoliosPage() {
  const [selectedId, setSelectedId] = useState(initialPortfolioIdFromUrl);
  const [portfolio, setPortfolio] = useState<Portfolio | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [lifecycleAction, setLifecycleAction] = useState<"amend" | "cancel" | null>(null);
  const [lifecyclePosition, setLifecyclePosition] = useState<EuropeanOptionPosition | null>(null);
  const [lifecycleRequests, setLifecycleRequests] = useState<TradeLifecycleRequest[]>([]);
  const [amendForm, setAmendForm] = useState<TradeTicketForm>(initialTradeFormFromUrl);
  const [portfolioPickerReloadKey, setPortfolioPickerReloadKey] = useState(0);
  const [portfolioForm, setPortfolioForm] = useState({
    name: "USD Equity Options Book",
    description: "Trade capture book",
    baseCurrency: "USD",
  });

  async function load() {
    setError(null);
    try {
      const result = await nexusApi.listPortfolios();
      const nextId = selectedId || result[0]?.id || "";
      setSelectedId(nextId);
      if (nextId) {
        const [nextPortfolio, requests] = await Promise.all([
          nexusApi.getPortfolio(nextId),
          nexusApi.listMyLifecycleRequests(),
        ]);
        setPortfolio(nextPortfolio);
        setLifecycleRequests(requests.filter((request) => request.portfolioId === nextId));
      }
    } catch (caught) {
      setError(errorMessage(caught));
    }
  }

  async function refreshSelectedPortfolio() {
    if (!selectedId) {
      return;
    }
    try {
      const [nextPortfolio, requests] = await Promise.all([
        nexusApi.getPortfolio(selectedId),
        nexusApi.listMyLifecycleRequests(),
      ]);
      setPortfolio(nextPortfolio);
      setLifecycleRequests(requests.filter((request) => request.portfolioId === selectedId));
    } catch (caught) {
      setError(errorMessage(caught));
    }
  }

  async function createPortfolio() {
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      const created = await nexusApi.createPortfolio({
        name: portfolioForm.name.trim(),
        description: portfolioForm.description.trim() || null,
        baseCurrency: portfolioForm.baseCurrency.trim().toUpperCase() || "USD",
      });
      setSelectedId(created.id);
      setPortfolio(created);
      setLifecycleRequests([]);
      setPortfolioPickerReloadKey((current) => current + 1);
      setSuccess("Portfolio created.");
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  function openAmend(position: EuropeanOptionPosition) {
    setLifecyclePosition(position);
    setLifecycleAction("amend");
    setAmendForm({
      underlyingSymbol: position.underlyingSymbol,
      optionType: position.optionType,
      strike: String(position.strike),
      maturityDate: position.maturityDate,
      quantity: String(position.quantity),
    });
  }

  function openCancel(position: EuropeanOptionPosition) {
    setLifecyclePosition(position);
    setLifecycleAction("cancel");
  }

  function closeLifecycleModal() {
    setLifecycleAction(null);
    setLifecyclePosition(null);
  }

  async function submitLifecycleRequest() {
    if (!lifecyclePosition || !lifecycleAction) {
      return;
    }
    setLoading(true);
    setError(null);
    setSuccess(null);
    try {
      if (lifecycleAction === "cancel") {
        await nexusApi.requestPositionCancel(lifecyclePosition.id);
        setSuccess(`${lifecyclePosition.underlyingSymbol} cancellation request sent to BO. It will stay pending until Back Office reviews it.`);
      } else {
        await nexusApi.requestPositionAmend(lifecyclePosition.id, {
          underlyingSymbol: amendForm.underlyingSymbol.trim().toUpperCase(),
          optionType: amendForm.optionType,
          strike: Number(amendForm.strike),
          maturityDate: amendForm.maturityDate,
          quantity: Number(amendForm.quantity),
        });
        setSuccess(`${lifecyclePosition.underlyingSymbol} amendment request sent to BO. The original position remains active until approval.`);
      }
      closeLifecycleModal();
      await refreshSelectedPortfolio();
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!selectedId) {
      return;
    }
    Promise.all([nexusApi.getPortfolio(selectedId), nexusApi.listMyLifecycleRequests()])
      .then(([nextPortfolio, requests]) => {
        setPortfolio(nextPortfolio);
        setLifecycleRequests(requests.filter((request) => request.portfolioId === selectedId));
      })
      .catch((caught) => setError(errorMessage(caught)));
  }, [selectedId]);

  return (
    <AppShell title="Portfolios" eyebrow="Books and positions" howTo={howTo.portfolios}>
      <Alert message={error} />
      {success ? <div className="success">{success}</div> : null}
      <div className="portfolio-grid">
        <section className="panel">
          <SectionTitle title="Create portfolio" info="Portfolios are books of trade terms. Market data stays outside the portfolio and comes from Blemberg or the local provider during valuation." />
          <div className="form-grid">
            <Field label="Name" className="full">
              <input className="input" value={portfolioForm.name} onChange={(event) => setPortfolioForm({ ...portfolioForm, name: event.target.value })} />
            </Field>
            <Field label="Description" className="full">
              <textarea className="textarea" value={portfolioForm.description} onChange={(event) => setPortfolioForm({ ...portfolioForm, description: event.target.value })} />
            </Field>
            <Field label="Currency">
              <input className="input" maxLength={3} value={portfolioForm.baseCurrency} onChange={(event) => setPortfolioForm({ ...portfolioForm, baseCurrency: event.target.value })} />
            </Field>
            <div className="field">
              <span>&nbsp;</span>
              <button className="btn" type="button" onClick={createPortfolio} disabled={loading}>
                {loading ? <Loader2 size={16} /> : <Plus size={16} />}
                Create Portfolio
              </button>
            </div>
          </div>
        </section>

        <section className="panel">
          <SectionTitle title="Select portfolio" info="Choose a persisted book to inspect its positions." />
          <PortfolioPicker value={selectedId} onChange={setSelectedId} onError={setError} onRefresh={refreshSelectedPortfolio} reloadKey={portfolioPickerReloadKey} />
        </section>
      </div>
      {portfolio ? (
        <>
          <div className="summary-strip portfolio-summary">
            <Metric label="Name" value={portfolio.name} />
            <Metric label="Base currency" value={portfolio.baseCurrency} />
            <Metric label="Positions" value={formatNumber(portfolio.positions.length, 0)} />
            <Metric label="Updated" value={new Date(portfolio.updatedAt).toLocaleDateString()} />
          </div>
          <div className="panel section">
            <SectionTitle title="Position inventory" info="Portfolio stores trade terms only. Spot, rates, volatility and dividends come from marketdata during valuation." />
            <PositionTable portfolio={portfolio} onAmend={openAmend} onCancel={openCancel} />
          </div>
          <div className="panel section">
            <SectionTitle title="Lifecycle requests" info="Amendments and cancellations are maker-checker requests. AMENDED and CANCELLED positions are historical; only ACTIVE positions can be changed again." />
            <LifecycleRequestTable requests={lifecycleRequests} />
          </div>
        </>
      ) : (
        <EmptyState text="No portfolio selected." />
      )}
      {lifecycleAction && lifecyclePosition ? (
        <LifecycleRequestModal
          action={lifecycleAction}
          position={lifecyclePosition}
          amendForm={amendForm}
          onAmendFormChange={setAmendForm}
          loading={loading}
          onClose={closeLifecycleModal}
          onSubmit={submitLifecycleRequest}
        />
      ) : null}
    </AppShell>
  );
}

export function UPadPage() {
  const [selectedId, setSelectedId] = useState(initialPortfolioIdFromUrl);
  const openedFromPreTrade = initialUPadSourceFromUrl() === "pre-trade-analysis";
  const [portfolio, setPortfolio] = useState<Portfolio | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(
    openedFromPreTrade ? "Ticket loaded from Pre-Trade Analysis. Review it before sending to BO." : null,
  );
  const [loading, setLoading] = useState<string | null>(null);
  const [activeNotebookId, setActiveNotebookId] = useState(() => notebookIdForSymbol(initialTradeFormFromUrl().underlyingSymbol));
  const [marketSnapshots, setMarketSnapshots] = useState<BlembergMarketSnapshot[]>([]);
  const [tradeBookings, setTradeBookings] = useState<TradeBooking[]>([]);
  const [lifecycleRequests, setLifecycleRequests] = useState<TradeLifecycleRequest[]>([]);
  const [myLimits, setMyLimits] = useState<TradingLimitSnapshot | null>(null);

  const [tradeForm, setTradeForm] = useState(initialTradeFormFromUrl);

  const marketSnapshotBySymbol = new Map(marketSnapshots.map((snapshot) => [snapshot.symbol.toUpperCase(), snapshot]));

  function pickTradeSymbol(symbol: string, snapshot?: BlembergMarketSnapshot) {
    const lastPrice = snapshot?.lastPrice ?? marketSnapshotBySymbol.get(symbol.toUpperCase())?.lastPrice;
    setTradeForm((current) => ({
      ...current,
      underlyingSymbol: symbol,
      strike: lastPrice != null && Number.isFinite(lastPrice) ? formatTicketNumber(lastPrice) : current.strike,
    }));
  }

  useEffect(() => {
    if (!selectedId) {
      setPortfolio(null);
      setTradeBookings([]);
      setLifecycleRequests([]);
      return;
    }
    loadSelectedPortfolio(selectedId);
  }, [selectedId]);

  useEffect(() => {
    void loadMyLimits();
  }, []);

  async function loadMyLimits() {
    try {
      setMyLimits(await nexusApi.getMyTradingLimits());
    } catch (caught) {
      if (caught instanceof NexusApiError && caught.status !== 401) {
        console.warn("[u-Pad] Could not load trading limits", caught.message);
      }
    }
  }

  async function loadSelectedPortfolio(portfolioId: string) {
    try {
      const [selectedPortfolio, bookings] = await Promise.all([
        nexusApi.getPortfolio(portfolioId),
        nexusApi.listMyTradeBookings(),
      ]);
      setPortfolio(selectedPortfolio);
      setTradeBookings(bookings.filter((booking) => booking.portfolioId === portfolioId));
      setLifecycleRequests((await nexusApi.listMyLifecycleRequests()).filter((request) => request.portfolioId === portfolioId));
    } catch (caught) {
      setError(errorMessage(caught));
    }
  }

  async function bookTrade() {
    if (!selectedId) {
      setError("Select a portfolio first. Create portfolios from the Portfolios page.");
      return;
    }
    const request: AddEuropeanOptionPositionRequest = {
      underlyingSymbol: tradeForm.underlyingSymbol.trim().toUpperCase(),
      optionType: tradeForm.optionType,
      strike: Number(tradeForm.strike),
      maturityDate: tradeForm.maturityDate,
      quantity: Number(tradeForm.quantity),
    };

    setLoading("trade");
    setError(null);
    setSuccess(null);
    try {
      await nexusApi.submitEuropeanOptionBooking(selectedId, request);
      await Promise.all([loadSelectedPortfolio(selectedId), loadMyLimits()]);
      setSuccess(`${request.optionType} ${request.underlyingSymbol} sent for BO validation.`);
    } catch (caught) {
      setError(tradingLimitError(caught));
    } finally {
      setLoading(null);
    }
  }

  return (
    <AppShell title="u-Pad" eyebrow="Trade capture" howTo={howTo.upad}>
      <Alert message={error} />
      {success ? <div className="success">{success}</div> : null}
      <div className="dealpad-layout">
        <section className="ticket-panel trade-ticket">
          <SectionTitle title="Trade Ticket" info="Submitting validates the symbol and creates a pending booking. It does not enter the confirmed portfolio until Back Office approves it." />
          <PortfolioPicker value={selectedId} onChange={setSelectedId} onError={setError} />
          {myLimits ? <MyTradingLimits snapshot={myLimits} /> : null}
          <div className="notebook-grid" aria-label="u-Pad notebooks">
            {notebooks.map((notebook) => {
              const Icon = notebook.icon;
              return (
                <button
                  className={`notebook-card compact ${activeNotebookId === notebook.id ? "active" : ""}`}
                  key={notebook.id}
                  type="button"
                  onClick={() => {
                    setActiveNotebookId(notebook.id);
                    pickTradeSymbol(notebook.symbols[0], marketSnapshotBySymbol.get(notebook.symbols[0]));
                  }}
                >
                  <Icon size={17} />
                  <strong>{notebook.name}</strong>
                  <span>{notebook.symbols.length} symbols</span>
                </button>
              );
            })}
          </div>
          <div className="symbol-strip compact" aria-label="Selected notebook symbols">
            <div>
              <strong>{notebooks.find((notebook) => notebook.id === activeNotebookId)?.name ?? notebooks[0].name}</strong>
              <span>Choose a symbol. If Blemberg has Last, strike is filled with that value.</span>
            </div>
            <div className="symbol-buttons">
              {(notebooks.find((notebook) => notebook.id === activeNotebookId) ?? notebooks[0]).symbols.map((symbol) => (
                <button className={`symbol-chip ${tradeForm.underlyingSymbol.toUpperCase() === symbol ? "active" : ""}`} key={symbol} type="button" onClick={() => pickTradeSymbol(symbol, marketSnapshotBySymbol.get(symbol))}>
                  {symbol}
                  {marketSnapshotBySymbol.get(symbol)?.lastPrice != null ? <small>{formatMarketNumber(marketSnapshotBySymbol.get(symbol)!.lastPrice!)}</small> : null}
                </button>
              ))}
            </div>
          </div>
          <div className="form-grid">
            <Field label="Underlying">
              <input className="input ticker-input" value={tradeForm.underlyingSymbol} onChange={(event) => setTradeForm({ ...tradeForm, underlyingSymbol: event.target.value })} />
            </Field>
            <Field label="Option type">
              <select className="select" value={tradeForm.optionType} onChange={(event) => setTradeForm({ ...tradeForm, optionType: event.target.value as OptionType })}>
                <option value="CALL">CALL</option>
                <option value="PUT">PUT</option>
              </select>
            </Field>
            <Field label="Strike">
              <input className="input" type="number" min="0.01" step="0.01" value={tradeForm.strike} onChange={(event) => setTradeForm({ ...tradeForm, strike: event.target.value })} />
            </Field>
            <Field label="Maturity">
              <input className="input" type="date" value={tradeForm.maturityDate} onChange={(event) => setTradeForm({ ...tradeForm, maturityDate: event.target.value })} />
            </Field>
            <Field label="Quantity">
              <input className="input" type="number" step="1" value={tradeForm.quantity} onChange={(event) => setTradeForm({ ...tradeForm, quantity: event.target.value })} />
            </Field>
            <div className="field">
              <span>&nbsp;</span>
              <button className="btn trade-submit" type="button" onClick={bookTrade} disabled={loading === "trade"}>
                {loading === "trade" ? <Loader2 size={16} /> : <Send size={16} />}
                Send to BO
              </button>
            </div>
          </div>
        </section>

        <NotebookMarketWatch
          activeNotebookId={activeNotebookId}
          selectedSymbol={tradeForm.underlyingSymbol}
          onNotebookChange={(notebookId) => setActiveNotebookId(notebookId)}
          onPick={pickTradeSymbol}
          onSnapshotsLoaded={setMarketSnapshots}
        />

        <section className="panel dealpad-blotter">
          <SectionTitle title="Pending bookings" info="These entries are waiting for BO or were rejected. They are excluded from portfolio pricing, exposure and CVA." />
          {portfolio ? <TradeBookingTable bookings={tradeBookings.filter((booking) => booking.status !== "CONFIRMED")} /> : <EmptyState text="Select a portfolio to see pending bookings." />}
        </section>

        <section className="panel dealpad-blotter">
          <SectionTitle title="Confirmed positions" info="Only BO-approved positions live in the portfolio and participate in pricing, exposure and CVA." />
          {portfolio ? <PositionTable portfolio={portfolio} /> : <EmptyState text="Select a portfolio to see confirmed positions." />}
        </section>

        <section className="panel dealpad-blotter">
          <SectionTitle title="Lifecycle requests" info="Amendments and cancellations wait for BO approval before changing confirmed positions." />
          {portfolio ? <LifecycleRequestTable requests={lifecycleRequests} /> : <EmptyState text="Select a portfolio to see lifecycle requests." />}
        </section>
      </div>
    </AppShell>
  );
}

function TradeBookingTable({ bookings }: { bookings: TradeBooking[] }) {
  if (bookings.length === 0) {
    return <EmptyState text="No pending or rejected bookings for this portfolio." />;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Status</th>
            <th>Symbol</th>
            <th>Type</th>
            <th>Strike</th>
            <th>Maturity</th>
            <th>Quantity</th>
            <th>Submitted</th>
            <th>Reason</th>
          </tr>
        </thead>
        <tbody>
          {bookings.map((booking) => (
            <tr key={booking.id}>
              <td><span className={`booking-status ${booking.status.toLowerCase()}`}>{booking.status.replaceAll("_", " ")}</span></td>
              <td>{booking.underlyingSymbol}</td>
              <td>{booking.optionType}</td>
              <td>{formatNumber(booking.strike, 2)}</td>
              <td>{booking.maturityDate}</td>
              <td>{formatNumber(booking.quantity, 2)}</td>
              <td>{new Date(booking.submittedAt).toLocaleString()}</td>
              <td>{booking.rejectionReason ?? "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function MyTradingLimits({ snapshot }: { snapshot: TradingLimitSnapshot }) {
  const policy = snapshot.policy?.active ? snapshot.policy : null;
  return (
    <div className={`upad-limit-strip ${snapshot.status.toLowerCase()}`}>
      <div>
        <strong>My trading limits</strong>
        <span>{snapshot.status === "ACTIVE" ? "Preventive BO controls are active" : "No active blocking policy"}</span>
      </div>
      <LimitCapacity
        label="Trades / hour"
        used={snapshot.usage.tradesThisHour}
        maximum={policy?.maxTradesPerHour}
      />
      <LimitCapacity
        label="Trades / day"
        used={snapshot.usage.tradesToday}
        maximum={policy?.maxTradesPerDay}
      />
      <LimitCapacity
        label="Notional / day"
        used={snapshot.usage.notionalToday}
        maximum={policy?.maxNotionalPerDay}
        currency
      />
      <small>UTC resets: hour {new Date(snapshot.usage.hourEndsAt).toLocaleTimeString()} · day {new Date(snapshot.usage.dayEndsAt).toLocaleString()}</small>
    </div>
  );
}

function LimitCapacity({
  label,
  used,
  maximum,
  currency = false,
}: {
  label: string;
  used: number;
  maximum: number | null | undefined;
  currency?: boolean;
}) {
  const display = (value: number) => currency ? formatCurrency(value, "USD") : formatNumber(value, 0);
  return (
    <div className="upad-limit-value">
      <span>{label}</span>
      <strong>{display(used)} <small>/ {maximum == null ? "Unlimited" : display(maximum)}</small></strong>
    </div>
  );
}

function NotebookMarketWatch({
  activeNotebookId,
  selectedSymbol,
  onNotebookChange,
  onPick,
  onSnapshotsLoaded,
}: {
  activeNotebookId: string;
  selectedSymbol: string;
  onNotebookChange: (notebookId: string) => void;
  onPick: (symbol: string, snapshot?: BlembergMarketSnapshot) => void;
  onSnapshotsLoaded: (snapshots: BlembergMarketSnapshot[]) => void;
}) {
  const [snapshots, setSnapshots] = useState<BlembergMarketSnapshot[]>([]);
  const [missingSymbols, setMissingSymbols] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [statusMessage, setStatusMessage] = useState("Reading Blemberg cache");
  const [refreshSummary, setRefreshSummary] = useState<string | null>(null);
  const activeNotebook = notebooks.find((notebook) => notebook.id === activeNotebookId) ?? notebooks[0];

  async function loadSnapshots(symbols = allNotebookSymbols) {
    setLoading(true);
    setError(null);
    try {
      console.info("[u-Pad] Loading Blemberg snapshots", { symbols });
      const result = await blembergApi.snapshots(symbols);
      setSnapshots(result.snapshots ?? []);
      onSnapshotsLoaded(result.snapshots ?? []);
      setMissingSymbols(result.missingSymbols ?? []);
      setStatusMessage(`Loaded ${result.snapshots?.length ?? 0} snapshots, ${result.missingSymbols?.length ?? 0} missing`);
      console.info("[u-Pad] Snapshot load completed", {
        snapshots: result.snapshots?.length ?? 0,
        missingSymbols: result.missingSymbols ?? [],
      });
    } catch (caught) {
      setSnapshots([]);
      onSnapshotsLoaded([]);
      setMissingSymbols(allNotebookSymbols);
      setError(errorMessage(caught));
      setStatusMessage("Snapshot load failed");
      console.error("[u-Pad] Snapshot load failed", caught);
    } finally {
      setLoading(false);
    }
  }

  async function refreshMissingFirst() {
    const currentSnapshotSymbols = new Set(snapshots.map((snapshot) => snapshot.symbol.toUpperCase()));
    const prioritySymbols = allNotebookSymbols.filter((symbol) => !currentSnapshotSymbols.has(symbol));
    const requestedPriority = prioritySymbols.length > 0 ? prioritySymbols : missingSymbols;

    setRefreshing(true);
    setError(null);
    setStatusMessage(requestedPriority.length > 0 ? `Refreshing missing first: ${requestedPriority.slice(0, 6).join(", ")}` : "Refreshing Blemberg market data");
    try {
      console.info("[u-Pad] Requesting Blemberg refresh", {
        prioritySymbols: requestedPriority,
        note: "Blemberg may ignore prioritySymbols if V1 endpoint does not support targeted refresh yet.",
      });
      const refresh = await blembergApi.refreshMarketData(requestedPriority);
      console.info("[u-Pad] Blemberg refresh response", refresh);
      setRefreshSummary(refreshMessage(refresh, requestedPriority));
      await loadSnapshots([...requestedPriority, ...allNotebookSymbols.filter((symbol) => !requestedPriority.includes(symbol))]);
    } catch (caught) {
      setError(errorMessage(caught));
      setStatusMessage("Blemberg refresh failed");
      setRefreshSummary(null);
      console.error("[u-Pad] Blemberg refresh failed", caught);
    } finally {
      setRefreshing(false);
    }
  }

  useEffect(() => {
    void loadSnapshots();
  }, []);

  useEffect(() => {
    function handleGlobalRefresh() {
      void loadSnapshots();
    }

    window.addEventListener("blemberg:refresh-completed", handleGlobalRefresh);
    return () => window.removeEventListener("blemberg:refresh-completed", handleGlobalRefresh);
  }, []);

  const snapshotBySymbol = new Map(snapshots.map((snapshot) => [snapshot.symbol.toUpperCase(), snapshot]));
  const latestAsOf = snapshots.reduce<string | null>((latest, snapshot) => {
    if (!snapshot.asOf) {
      return latest;
    }
    return !latest || snapshot.asOf > latest ? snapshot.asOf : latest;
  }, null);

  return (
    <section className="ticket-panel market-watch">
      <div className="market-watch-header">
        <SectionTitle title="Market watch" info="Latest cached Blemberg snapshots for the symbols NexusXVA can book in u-Pad." />
        <button className="icon-btn" type="button" onClick={refreshMissingFirst} disabled={loading || refreshing} title="Refresh missing snapshots first" aria-label="Refresh missing snapshots first">
          {loading || refreshing ? <Loader2 size={15} /> : <RefreshCw size={15} />}
        </button>
      </div>

      <div className="watch-summary">
        <span>{snapshots.length} live snapshots</span>
        <span>{missingSymbols.length} missing</span>
        <span>{latestAsOf ? `Last as of ${formatShortDateTime(latestAsOf)}` : "No refresh data"}</span>
        <span>{statusMessage}</span>
      </div>

      <div className="watch-notebooks" aria-label="Market watch notebooks">
        {notebooks.map((notebook) => (
          <button className={`watch-notebook ${activeNotebook.id === notebook.id ? "active" : ""}`} key={notebook.id} type="button" onClick={() => onNotebookChange(notebook.id)}>
            <span>{notebook.name}</span>
            <strong>{notebook.symbols.filter((symbol) => snapshotBySymbol.has(symbol)).length}/{notebook.symbols.length}</strong>
          </button>
        ))}
      </div>

      {error ? <div className="mini-alert">{error}</div> : null}
      {refreshSummary ? <div className="mini-note">{refreshSummary}</div> : null}

      <div className="watch-table-wrap">
        <table className="watch-table">
          <thead>
            <tr>
              <th>Symbol</th>
              <th>Last</th>
              <th>Prev</th>
              <th>Chg</th>
              <th>Volume</th>
              <th>As of</th>
            </tr>
          </thead>
          <tbody>
            {[...activeNotebook.symbols].sort((left, right) => Number(snapshotBySymbol.has(left)) - Number(snapshotBySymbol.has(right))).map((symbol) => {
              const snapshot = snapshotBySymbol.get(symbol);
              const selected = selectedSymbol.toUpperCase() === symbol;
              return (
                <tr className={selected ? "selected" : ""} key={symbol} onClick={() => onPick(symbol, snapshot)}>
                  <td>
                    <button
                      className="symbol-link"
                      type="button"
                      onClick={(event) => {
                        event.stopPropagation();
                        onPick(symbol, snapshot);
                      }}
                    >
                      {symbol}
                    </button>
                  </td>
                  <td>{snapshot?.lastPrice != null ? formatMarketNumber(snapshot.lastPrice) : "Missing"}</td>
                  <td>{snapshot?.previousClose != null ? formatMarketNumber(snapshot.previousClose) : "-"}</td>
                  <td className={changeClass(snapshot)}>{formatChange(snapshot)}</td>
                  <td>{snapshot?.volume != null ? formatNumber(snapshot.volume, 0) : "-"}</td>
                  <td>{snapshot?.asOf ? formatShortDateTime(snapshot.asOf) : "-"}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function formatMarketNumber(value: number) {
  return value.toLocaleString("en-US", {
    maximumFractionDigits: value >= 100 ? 2 : 4,
    minimumFractionDigits: 2,
  });
}

function refreshMessage(refresh: BlembergRefreshResponse, prioritySymbols: string[]) {
  const skipped = refresh.jobSummaries?.reduce((sum, job) => sum + (job.skippedRateLimit ?? 0), 0) ?? 0;
  const requested = refresh.symbolsRequested ?? prioritySymbols.length;
  const succeeded = refresh.symbolsSucceeded ?? 0;
  const priorityIgnored = prioritySymbols.length > 0 && requested > prioritySymbols.length;
  const priorityNote = priorityIgnored ? " Blemberg refreshed the full watchlist, so prioritySymbols are not honored yet." : "";
  const rateNote = skipped > 0 ? ` ${skipped} items were skipped by provider rate limit.` : "";
  return `Refresh ${refresh.status ?? "completed"}: ${succeeded}/${requested} symbols succeeded.${rateNote}${priorityNote}`;
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

function formatChange(snapshot: BlembergMarketSnapshot | undefined) {
  if (!snapshot?.lastPrice || !snapshot.previousClose) {
    return "-";
  }
  const change = snapshot.lastPrice - snapshot.previousClose;
  const percent = change / snapshot.previousClose;
  return `${change >= 0 ? "+" : ""}${formatMarketNumber(change)} (${formatPercent(percent)})`;
}

function changeClass(snapshot: BlembergMarketSnapshot | undefined) {
  if (!snapshot?.lastPrice || !snapshot.previousClose) {
    return "";
  }
  return snapshot.lastPrice >= snapshot.previousClose ? "positive" : "negative";
}

export function PricingPage() {
  const [selectedId, setSelectedId] = useState(initialPortfolioIdFromUrl);
  const [valuationDate, setValuationDate] = useState(todayIsoDate());
  const [pricing, setPricing] = useState<PortfolioPricingResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function runPricing(portfolioId = selectedId) {
    if (!portfolioId) {
      setError("Select a portfolio first.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      setPricing(await nexusApi.runPortfolioPricing(portfolioId, valuationDate));
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (!selectedId) {
      return;
    }
    void runPricing(selectedId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedId, valuationDate]);

  return (
    <AppShell title="Pricing" eyebrow="Black-Scholes valuation" howTo={howTo.pricing}>
      <Alert message={error} />
      <div className="panel section pricing-selector">
        <SectionTitle title="Open portfolio" info="Choose a portfolio and NexusXVA will price it automatically using the selected valuation date." />
        <div className="toolbar">
          <PortfolioPicker value={selectedId} onChange={setSelectedId} onError={setError} />
          <label className="field compact-field">
            <span>Valuation date</span>
            <span className="field-help">
              <InfoButton title="Valuation date" body="Date used to compute each option's time to maturity for Black-Scholes pricing." />
            </span>
            <input className="input" type="date" value={valuationDate} onChange={(event) => setValuationDate(event.target.value)} />
          </label>
          <button className="btn" type="button" onClick={() => runPricing()} disabled={loading || !selectedId}>
            {loading ? <Loader2 size={16} /> : <CircleDollarSign size={16} />}
            Reprice
          </button>
        </div>
      </div>
      <div className="panel">
        <SectionTitle title="Portfolio valuation" info="The backend requests market-data pricing inputs and reuses the Black-Scholes calculator for each live position." />
        {pricing ? <PricingResult pricing={pricing} /> : <EmptyState text="Select a portfolio to run pricing automatically." />}
      </div>
    </AppShell>
  );
}

export function ExposurePage() {
  const [selectedId, setSelectedId] = useState(initialPortfolioIdFromUrl);
  const [form, setForm] = useState(defaultRunForm);
  const [exposure, setExposure] = useState<ExposureSimulationResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function runExposure() {
    if (!selectedId) {
      setError("Select a portfolio first.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      setExposure(await nexusApi.runExposure(toExposureRequest(selectedId, form)));
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AppShell title="Exposure" eyebrow="Monte Carlo analytics" howTo={howTo.exposure}>
      <Alert message={error} />
      <RunSetup selectedId={selectedId} setSelectedId={setSelectedId} form={form} setForm={setForm} onError={setError} />
      <div className="panel">
        <SectionTitle title="Exposure profile" info="Exposure V1 simulates GBM spot paths, reprices live positions and aggregates EE, ENE and PFE by bucket." />
        <button className="btn section-action" type="button" onClick={runExposure} disabled={loading}>
          {loading ? <Loader2 size={16} /> : <Activity size={16} />}
          Run Exposure
        </button>
        {exposure ? <ExposureResult exposure={exposure} /> : <EmptyState text="Run exposure to chart EE, ENE and PFE." />}
      </div>
    </AppShell>
  );
}

export function CvaPage() {
  const [selectedId, setSelectedId] = useState(initialPortfolioIdFromUrl);
  const [form, setForm] = useState(defaultRunForm);
  const [cva, setCva] = useState<CvaCalculationResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function runCva() {
    if (!selectedId) {
      setError("Select a portfolio first.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      setCva(await nexusApi.runCva(toCvaRequest(selectedId, form)));
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  return (
    <AppShell title="CVA" eyebrow="Credit valuation adjustment" howTo={howTo.cva}>
      <Alert message={error} />
      <RunSetup selectedId={selectedId} setSelectedId={setSelectedId} form={form} setForm={setForm} onError={setError} includeCva />
      <div className="panel">
        <SectionTitle title="CVA contribution" info="Dashboard V1 uses flat LGD, hazard rate and discount rate. Curve-mode is supported by the backend and can be added to the UI later." />
        <button className="btn warning section-action" type="button" onClick={runCva} disabled={loading}>
          {loading ? <Loader2 size={16} /> : <Shield size={16} />}
          Run CVA
        </button>
        {cva ? <CvaResult cva={cva} /> : <EmptyState text="Run CVA to see adjustment and bucket-level contributions." />}
      </div>
    </AppShell>
  );
}

function RunSetup({
  selectedId,
  setSelectedId,
  form,
  setForm,
  onError,
  includeCva = false,
}: {
  selectedId: string;
  setSelectedId: (value: string) => void;
  form: RunForm;
  setForm: (value: RunForm) => void;
  onError: (value: string) => void;
  includeCva?: boolean;
}) {
  const [advancedOpen, setAdvancedOpen] = useState(false);

  function applyPreset(values: Partial<RunForm>) {
    setForm({
      ...form,
      ...values,
      seed: randomSeed(),
    });
  }

  return (
    <div className="panel section">
      <SectionTitle title="Run setup" info="These values are sent to the backend simulation/CVA endpoint. The frontend does not calculate paths or valuation adjustments." />
      <PortfolioPicker value={selectedId} onChange={setSelectedId} onError={onError} />
      <div className="preset-grid" aria-label="Simulation presets">
        {runPresets.map((preset) => (
          <button className="preset-card" key={preset.name} type="button" onClick={() => applyPreset(preset.values)}>
            <strong>{preset.name}</strong>
            <span>{preset.description}</span>
          </button>
        ))}
      </div>
      <div className="form-grid">
        <RunField label="Valuation date" value={form.valuationDate} type="date" onChange={(valuationDate) => setForm({ ...form, valuationDate })} />
        <RunField label="Horizon days" value={form.horizonDays} type="number" onChange={(horizonDays) => setForm({ ...form, horizonDays })} />
        <RunField label="Time steps" value={form.timeSteps} type="number" onChange={(timeSteps) => setForm({ ...form, timeSteps })} />
        <RunField label="Paths" value={form.paths} type="number" onChange={(paths) => setForm({ ...form, paths })} />
        <RunField label="PFE confidence" value={form.pfeConfidenceLevel} type="number" step="0.01" onChange={(pfeConfidenceLevel) => setForm({ ...form, pfeConfidenceLevel })} />
        {includeCva ? (
          <>
            <RunField label="LGD" value={form.lossGivenDefault} type="number" step="0.01" onChange={(lossGivenDefault) => setForm({ ...form, lossGivenDefault })} />
            <RunField label="Hazard rate" value={form.counterpartyHazardRate} type="number" step="0.001" onChange={(counterpartyHazardRate) => setForm({ ...form, counterpartyHazardRate })} />
            <RunField label="Discount rate" value={form.discountRate} type="number" step="0.001" onChange={(discountRate) => setForm({ ...form, discountRate })} />
          </>
        ) : null}
      </div>
      <button className="advanced-toggle" type="button" onClick={() => setAdvancedOpen((current) => !current)}>
        {advancedOpen ? "Hide advanced simulation controls" : "Show advanced simulation controls"}
      </button>
      {advancedOpen ? (
        <div className="advanced-panel">
          <RunField label="Seed" value={form.seed} type="number" onChange={(seed) => setForm({ ...form, seed })} />
          <button className="btn secondary" type="button" onClick={() => setForm({ ...form, seed: randomSeed() })}>
            Random seed
          </button>
        </div>
      ) : null}
    </div>
  );
}

function toExposureRequest(portfolioId: string, form: RunForm) {
  return {
    portfolioId,
    valuationDate: form.valuationDate,
    horizonDays: Number(form.horizonDays),
    timeSteps: Number(form.timeSteps),
    paths: Number(form.paths),
    seed: Number(form.seed),
    pfeConfidenceLevel: Number(form.pfeConfidenceLevel),
  };
}

function randomSeed() {
  return String(Math.floor(Math.random() * 900000) + 100000);
}

function toCvaRequest(portfolioId: string, form: RunForm) {
  return {
    ...toExposureRequest(portfolioId, form),
    lossGivenDefault: Number(form.lossGivenDefault),
    counterpartyHazardRate: Number(form.counterpartyHazardRate),
    discountRate: Number(form.discountRate),
  };
}

function SectionTitle({ title, info }: { title: string; info: string }) {
  return (
    <div className="section-header">
      <h2>{title}</h2>
      <InfoButton title={title} body={info} />
    </div>
  );
}

function Field({ label, className, children }: { label: string; className?: string; children: React.ReactNode }) {
  return (
    <label className={`field ${className ?? ""}`}>
      <span>{label}</span>
      {children}
    </label>
  );
}

function RunField({
  label,
  value,
  onChange,
  type,
  step,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type: "date" | "number";
  step?: string;
}) {
  return (
    <Field label={label} className="third">
      <span className="field-help">
        <InfoButton title={label} body={runFieldHelp[label] ?? "This value is sent to the backend run request."} />
      </span>
      <input className="input" type={type} step={step} value={value} onChange={(event) => onChange(event.target.value)} />
    </Field>
  );
}

function PositionTable({
  portfolio,
  onAmend,
  onCancel,
}: {
  portfolio: Portfolio;
  onAmend?: (position: EuropeanOptionPosition) => void;
  onCancel?: (position: EuropeanOptionPosition) => void;
}) {
  if (portfolio.positions.length === 0) {
    return <EmptyState text="No positions in this portfolio." />;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Symbol</th>
            <th>Type</th>
            <th>Strike</th>
            <th>Maturity</th>
            <th>Quantity</th>
            <th>Status</th>
            <th>Updated</th>
            {onAmend || onCancel ? <th>Actions</th> : null}
          </tr>
        </thead>
        <tbody>
          {portfolio.positions.map((position) => (
            <tr className={(position.lifecycleStatus ?? "ACTIVE") !== "ACTIVE" ? "inactive-position-row" : ""} key={position.id}>
              <td>{position.underlyingSymbol}</td>
              <td>{position.optionType}</td>
              <td>{formatNumber(position.strike, 2)}</td>
              <td>{position.maturityDate}</td>
              <td>{formatNumber(position.quantity, 2)}</td>
              <td><span className={`lifecycle-status ${(position.lifecycleStatus ?? "ACTIVE").toLowerCase()}`}>{position.lifecycleStatus ?? "ACTIVE"}</span></td>
              <td>{new Date(position.updatedAt).toLocaleString()}</td>
              {onAmend || onCancel ? (
                <td>
                  {(position.lifecycleStatus ?? "ACTIVE") === "ACTIVE" ? (
                    <div className="row-actions">
                      {onAmend ? <button className="text-action" type="button" onClick={() => onAmend(position)}>Request Amend</button> : null}
                      {onCancel ? <button className="text-action danger" type="button" onClick={() => onCancel(position)}>Request Cancel</button> : null}
                    </div>
                  ) : (
                    <span className="muted-cell">
                      {(position.lifecycleStatus ?? "ACTIVE") === "AMENDED" ? "Use replacement ACTIVE position" : "Closed history"}
                    </span>
                  )}
                </td>
              ) : null}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function LifecycleRequestModal({
  action,
  position,
  amendForm,
  onAmendFormChange,
  loading,
  onClose,
  onSubmit,
}: {
  action: "amend" | "cancel";
  position: EuropeanOptionPosition;
  amendForm: TradeTicketForm;
  onAmendFormChange: (next: TradeTicketForm) => void;
  loading: boolean;
  onClose: () => void;
  onSubmit: () => void;
}) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) {
        onClose();
      }
    }}>
      <div className="modal-panel" role="dialog" aria-modal="true">
        <div>
          <span className="page-eyebrow">Front Office lifecycle</span>
          <h2>{action === "cancel" ? "Request cancellation" : "Request amendment"}</h2>
          <p>{position.optionType} {position.underlyingSymbol} · {formatNumber(position.quantity, 2)} @ {formatNumber(position.strike, 2)}</p>
        </div>
        {action === "amend" ? (
          <div className="form-grid">
            <Field label="Underlying">
              <input className="input ticker-input" value={amendForm.underlyingSymbol} onChange={(event) => onAmendFormChange({ ...amendForm, underlyingSymbol: event.target.value })} />
            </Field>
            <Field label="Option type">
              <select className="select" value={amendForm.optionType} onChange={(event) => onAmendFormChange({ ...amendForm, optionType: event.target.value as OptionType })}>
                <option value="CALL">CALL</option>
                <option value="PUT">PUT</option>
              </select>
            </Field>
            <Field label="Strike">
              <input className="input" type="number" min="0.01" step="0.01" value={amendForm.strike} onChange={(event) => onAmendFormChange({ ...amendForm, strike: event.target.value })} />
            </Field>
            <Field label="Maturity">
              <input className="input" type="date" value={amendForm.maturityDate} onChange={(event) => onAmendFormChange({ ...amendForm, maturityDate: event.target.value })} />
            </Field>
            <Field label="Quantity">
              <input className="input" type="number" step="1" value={amendForm.quantity} onChange={(event) => onAmendFormChange({ ...amendForm, quantity: event.target.value })} />
            </Field>
          </div>
        ) : (
          <div className="mini-note">
            This request closes the full position only after Back Office approval. Partial cancellation is not part of V1.
          </div>
        )}
        <div className="modal-actions">
          <button className="btn secondary" type="button" onClick={onClose}>Close</button>
          <button className={`btn ${action === "cancel" ? "danger-button" : ""}`} type="button" onClick={onSubmit} disabled={loading}>
            {loading ? <Loader2 size={16} /> : <Send size={16} />}
            Send to BO
          </button>
        </div>
      </div>
    </div>
  );
}

function LifecycleRequestTable({ requests }: { requests: TradeLifecycleRequest[] }) {
  if (requests.length === 0) {
    return <EmptyState text="No lifecycle requests for this portfolio." />;
  }
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Status</th>
            <th>Type</th>
            <th>Original</th>
            <th>Requested</th>
            <th>Submitted</th>
            <th>Reason</th>
          </tr>
        </thead>
        <tbody>
          {requests.map((request) => (
            <tr key={request.id}>
              <td><span className={`booking-status ${request.status.toLowerCase()}`}>{request.status.replaceAll("_", " ")}</span></td>
              <td>{request.requestType}</td>
              <td>{request.originalOptionType} {request.originalUnderlyingSymbol} · {formatNumber(request.originalQuantity, 2)} @ {formatNumber(request.originalStrike, 2)}</td>
              <td>{request.requestType === "AMEND" ? `${request.requestedOptionType} ${request.requestedUnderlyingSymbol} · ${formatNumber(request.requestedQuantity ?? 0, 2)} @ ${formatNumber(request.requestedStrike ?? 0, 2)}` : "Full cancellation"}</td>
              <td>{new Date(request.submittedAt).toLocaleString()}</td>
              <td>{request.rejectionReason ?? "—"}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function PricingResult({ pricing }: { pricing: PortfolioPricingResponse }) {
  return (
    <div className="section">
      <div className="summary-strip">
        <Metric label="Total price" value={formatCurrency(pricing.totalPrice, pricing.baseCurrency)} />
        <Metric label="Delta" value={formatNumber(pricing.totalGreeks.delta)} />
        <Metric label="Gamma" value={formatNumber(pricing.totalGreeks.gamma)} />
        <Metric label="Vega" value={formatNumber(pricing.totalGreeks.vega)} />
      </div>
      <div className="table-wrap table-spacing">
        <table>
          <thead>
            <tr>
              <th>Symbol</th>
              <th>Status</th>
              <th>Qty</th>
              <th>Unit price</th>
              <th>Position price</th>
              <th>Spot</th>
              <th>Vol</th>
              <th>Rate</th>
              <th>Dividend</th>
              <th>Source</th>
            </tr>
          </thead>
          <tbody>
            {pricing.positions.map((position) => (
              <tr key={position.positionId}>
                <td>{position.underlyingSymbol}</td>
                <td>{position.status}</td>
                <td>{formatNumber(position.quantity, 2)}</td>
                <td>{formatCurrency(position.unitPrice, pricing.baseCurrency)}</td>
                <td>{formatCurrency(position.positionPrice, pricing.baseCurrency)}</td>
                <td>{formatNumber(position.marketData.spot, 2)}</td>
                <td>{formatPercent(position.marketData.volatility)}</td>
                <td>{formatPercent(position.marketData.riskFreeRate)}</td>
                <td>{formatPercent(position.marketData.dividendYield)}</td>
                <td>{position.marketData.source}{position.marketData.stale ? " · stale" : ""}</td>
              </tr>
            ))}
            {pricing.unpriceablePositions.map((position) => (
              <tr key={position.positionId}>
                <td>{position.positionId.slice(0, 8)}</td>
                <td>{position.status}</td>
                <td colSpan={8}>{position.reason}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function ExposureResult({ exposure }: { exposure: ExposureSimulationResponse }) {
  return (
    <div className="table-spacing">
      <div className="summary-strip">
        <Metric label="Model" value={exposure.model} />
        <Metric label="Paths" value={formatNumber(exposure.paths, 0)} />
        <Metric label="Time steps" value={formatNumber(exposure.timeSteps, 0)} />
        <Metric label="PFE confidence" value={formatPercent(exposure.pfeConfidenceLevel)} />
      </div>
      <div className="table-spacing">
        <ExposureChart points={exposure.points} />
      </div>
      <ExposureTable points={exposure.points} />
    </div>
  );
}

function ExposureTable({ points }: { points: ExposureSimulationResponse["points"] }) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Date</th>
            <th>Expected Exposure</th>
            <th>Expected Negative Exposure</th>
            <th>PFE</th>
          </tr>
        </thead>
        <tbody>
          {points.map((point) => (
            <tr key={point.date}>
              <td>{point.date}</td>
              <td>{formatCurrency(point.expectedExposure)}</td>
              <td>{formatCurrency(point.expectedNegativeExposure)}</td>
              <td>{formatCurrency(point.pfe)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function CvaResult({ cva }: { cva: CvaCalculationResponse }) {
  return (
    <div className="table-spacing">
      <div className="summary-strip">
        <Metric label="CVA" value={formatCurrency(cva.cva)} />
        <Metric label="Credit method" value={cva.creditMethod} />
        <Metric label="Discount method" value={cva.discountMethod} />
        <Metric label="LGD" value={formatPercent(cva.lossGivenDefault)} />
      </div>
      <div className="table-wrap table-spacing">
        <table>
          <thead>
            <tr>
              <th>Date</th>
              <th>EE</th>
              <th>Discount factor</th>
              <th>Survival</th>
              <th>Default increment</th>
              <th>Discounted EE</th>
              <th>Contribution</th>
            </tr>
          </thead>
          <tbody>
            {cva.points.map((point) => (
              <tr key={point.date}>
                <td>{point.date}</td>
                <td>{formatCurrency(point.expectedExposure)}</td>
                <td>{formatNumber(point.discountFactor)}</td>
                <td>{formatPercent(point.survivalProbability)}</td>
                <td>{formatPercent(point.defaultProbabilityIncrement)}</td>
                <td>{formatCurrency(point.discountedExpectedExposure)}</td>
                <td>{formatCurrency(point.cvaContribution)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
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

function WorkflowLink({ href, icon, title, text }: { href: string; icon: React.ReactNode; title: string; text: string }) {
  return (
    <a className="workflow-link" href={href}>
      <span>{icon}</span>
      <strong>{title}</strong>
      <small>{text}</small>
    </a>
  );
}

function Alert({ message }: { message: string | null }) {
  return message ? <div className="alert">{message}</div> : null;
}

function EmptyState({ text }: { text: string }) {
  return <div className="empty">{text}</div>;
}

function initialPortfolioIdFromUrl() {
  if (typeof window === "undefined") {
    return "";
  }
  return new URLSearchParams(window.location.search).get("portfolioId") ?? "";
}

function initialUPadSourceFromUrl() {
  if (typeof window === "undefined") {
    return "";
  }
  return new URLSearchParams(window.location.search).get("source") ?? "";
}

function initialTradeFormFromUrl(): TradeTicketForm {
  const fallback: TradeTicketForm = {
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
  const optionType = params.get("optionType") === "PUT" ? "PUT" : "CALL";
  return {
    underlyingSymbol: (params.get("underlyingSymbol") ?? params.get("symbol") ?? fallback.underlyingSymbol).toUpperCase(),
    optionType,
    strike: params.get("strike") ?? fallback.strike,
    maturityDate: params.get("maturityDate") ?? fallback.maturityDate,
    quantity: params.get("quantity") ?? fallback.quantity,
  };
}

function notebookIdForSymbol(symbol: string) {
  return notebooks.find((notebook) => notebook.symbols.includes(symbol.toUpperCase()))?.id ?? notebooks[0].id;
}

function errorMessage(caught: unknown): string {
  if (caught instanceof NexusApiError) {
    return caught.message;
  }
  if (caught instanceof Error) {
    return caught.message;
  }
  return "Unexpected dashboard error";
}

function tradingLimitError(caught: unknown): string {
  if (!(caught instanceof NexusApiError) || caught.status !== 409 || typeof caught.body !== "object" || caught.body == null) {
    return errorMessage(caught);
  }
  const metadata = caught.body.metadata;
  if (!metadata?.limitType) {
    return caught.message;
  }
  const limit = String(metadata.limitType).replaceAll("_", " ").toLowerCase();
  const maximum = metadata.maximum == null ? "unknown" : String(metadata.maximum);
  const current = metadata.currentUsage == null ? "unknown" : String(metadata.currentUsage);
  const requested = metadata.requested == null ? "unknown" : String(metadata.requested);
  const reset = metadata.periodEndsAt ? new Date(String(metadata.periodEndsAt)).toLocaleString() : "the next UTC period";
  return `Booking blocked by ${limit}: maximum ${maximum}, current ${current}, requested ${requested}. Capacity resets at ${reset}.`;
}
