"use client";

import React from "react";
import { FlaskConical, Loader2, Send, Waves } from "lucide-react";
import { nexusApi, NexusApiError } from "@/lib/api";
import { formatCurrency, formatNumber, formatPercent, todayIsoDate } from "@/lib/format";
import type {
  AddEuropeanOptionPositionRequest,
  FrontOfficeStressTestResponse,
  OptionType,
  StressScenarioRequest,
  StressScenarioResult,
} from "@/lib/types";
import { AppShell } from "./AppShell";
import { PortfolioPicker } from "./PortfolioPicker";

type StressMode = "CONFIRMED" | "WITH_HYPOTHETICAL";

type StressScenarioForm = {
  name: string;
  spotShockPercent: string;
  volatilityShockBps: string;
  riskFreeRateShockBps: string;
  dividendYieldShockBps: string;
};

const howTo = [
  { title: "Purpose", body: "Stress Testing shocks market inputs and reprices the portfolio with Black-Scholes." },
  { title: "Modes", body: "Use confirmed portfolio only, or include one hypothetical trade without creating a booking." },
  { title: "Shock units", body: "Spot is a relative percent. Volatility, rates, and dividend yield are absolute basis-point shocks." },
  { title: "Scope", body: "V1 is pricing and Greeks only. Exposure and CVA stress are separate future slices." },
];

const defaultScenarios: StressScenarioForm[] = [
  scenarioForm("Base", "0", "0", "0", "0"),
  scenarioForm("Spot -5%, Vol +250bp", "-5", "250", "0", "0"),
  scenarioForm("Spot -10%, Vol +500bp", "-10", "500", "0", "0"),
  scenarioForm("Spot +5%, Vol -100bp", "5", "-100", "0", "0"),
  scenarioForm("Rates +100bp", "0", "0", "100", "0"),
  scenarioForm("Rates -100bp", "0", "0", "-100", "0"),
];

export function StressTestingPage() {
  const [selectedId, setSelectedId] = React.useState(initialPortfolioIdFromUrl);
  const [valuationDate, setValuationDate] = React.useState(todayIsoDate());
  const [mode, setMode] = React.useState<StressMode>("CONFIRMED");
  const [tradeForm, setTradeForm] = React.useState({
    underlyingSymbol: "AAPL",
    optionType: "CALL" as OptionType,
    strike: "190",
    maturityDate: "2027-06-01",
    quantity: "10",
  });
  const [scenarios, setScenarios] = React.useState(defaultScenarios);
  const [result, setResult] = React.useState<FrontOfficeStressTestResponse | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState(false);

  async function runStressTest() {
    if (!selectedId) {
      setError("Select a portfolio first.");
      return;
    }
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      setResult(await nexusApi.runFrontOfficeStressTest({
        portfolioId: selectedId,
        valuationDate,
        hypotheticalTrade: mode === "WITH_HYPOTHETICAL" ? toTradeRequest(tradeForm) : null,
        scenarios: scenarios.map(toScenarioRequest),
      }));
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  function updateScenario(index: number, patch: Partial<StressScenarioForm>) {
    setScenarios((current) => current.map((scenario, scenarioIndex) => (
      scenarioIndex === index ? { ...scenario, ...patch } : scenario
    )));
  }

  function addScenario() {
    setScenarios((current) => [...current, scenarioForm(`Custom ${current.length + 1}`, "0", "0", "0", "0")]);
  }

  function removeScenario(index: number) {
    setScenarios((current) => current.length <= 1 ? current : current.filter((_, scenarioIndex) => scenarioIndex !== index));
  }

  return (
    <AppShell title="Stress Testing" eyebrow="Front Office scenarios" howTo={howTo}>
      {error ? <div className="alert">{error}</div> : null}
      <div className="stress-layout">
        <section className="panel stress-controls">
          <div className="section-title">
            <div>
              <h2>Scenario setup</h2>
              <p>Shock market inputs uniformly across confirmed European option positions.</p>
            </div>
          </div>
          <PortfolioPicker value={selectedId} onChange={setSelectedId} onError={setError} />
          <div className="stress-mode-toggle" role="group" aria-label="Stress mode">
            <button className={mode === "CONFIRMED" ? "active" : ""} type="button" onClick={() => setMode("CONFIRMED")}>
              Confirmed portfolio
            </button>
            <button className={mode === "WITH_HYPOTHETICAL" ? "active" : ""} type="button" onClick={() => setMode("WITH_HYPOTHETICAL")}>
              Portfolio + hypothetical trade
            </button>
          </div>
          <label className="field">
            <span>Valuation date</span>
            <input className="input" type="date" value={valuationDate} onChange={(event) => setValuationDate(event.target.value)} />
          </label>

          {mode === "WITH_HYPOTHETICAL" ? (
            <HypotheticalTicket tradeForm={tradeForm} onChange={setTradeForm} />
          ) : (
            <div className="mini-note">
              Stress will use only BO-confirmed positions. Pending and rejected bookings stay out.
            </div>
          )}
        </section>

        <section className="panel stress-matrix">
          <div className="section-title">
            <div>
              <h2>Scenario matrix</h2>
              <p>Spot is entered as percent. Volatility, rates and dividend yield are basis points.</p>
            </div>
            <button className="btn secondary" type="button" onClick={addScenario}>
              Add Scenario
            </button>
          </div>
          <ScenarioMatrix scenarios={scenarios} onChange={updateScenario} onRemove={removeScenario} />
          <button className="btn stress-run" type="button" onClick={runStressTest} disabled={loading}>
            {loading ? <Loader2 size={16} /> : <Waves size={16} />}
            Run Stress Test
          </button>
        </section>
      </div>

      <section className="panel stress-results">
        <div className="section-title">
          <div>
            <h2>Stress results</h2>
            <p>Impacts are measured versus the confirmed base portfolio.</p>
          </div>
        </div>
        {result ? <StressResults result={result} trade={mode === "WITH_HYPOTHETICAL" ? toTradeRequest(tradeForm) : null} /> : <EmptyStress />}
      </section>
    </AppShell>
  );
}

function HypotheticalTicket({
  tradeForm,
  onChange,
}: {
  tradeForm: {
    underlyingSymbol: string;
    optionType: OptionType;
    strike: string;
    maturityDate: string;
    quantity: string;
  };
  onChange: (next: typeof tradeForm) => void;
}) {
  return (
    <div className="stress-ticket">
      <strong>Hypothetical trade</strong>
      <div className="form-grid">
        <label className="field">
          <span>Underlying</span>
          <input className="input ticker-input" value={tradeForm.underlyingSymbol} onChange={(event) => onChange({ ...tradeForm, underlyingSymbol: event.target.value })} />
        </label>
        <label className="field">
          <span>Option type</span>
          <select className="select" value={tradeForm.optionType} onChange={(event) => onChange({ ...tradeForm, optionType: event.target.value as OptionType })}>
            <option value="CALL">CALL</option>
            <option value="PUT">PUT</option>
          </select>
        </label>
        <label className="field">
          <span>Strike</span>
          <input className="input" type="number" min="0.01" step="0.01" value={tradeForm.strike} onChange={(event) => onChange({ ...tradeForm, strike: event.target.value })} />
        </label>
        <label className="field">
          <span>Maturity</span>
          <input className="input" type="date" value={tradeForm.maturityDate} onChange={(event) => onChange({ ...tradeForm, maturityDate: event.target.value })} />
        </label>
        <label className="field">
          <span>Quantity</span>
          <input className="input" type="number" step="1" value={tradeForm.quantity} onChange={(event) => onChange({ ...tradeForm, quantity: event.target.value })} />
        </label>
      </div>
    </div>
  );
}

function ScenarioMatrix({
  scenarios,
  onChange,
  onRemove,
}: {
  scenarios: StressScenarioForm[];
  onChange: (index: number, patch: Partial<StressScenarioForm>) => void;
  onRemove: (index: number) => void;
}) {
  return (
    <div className="table-wrap stress-table-wrap">
      <table className="stress-scenario-table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Spot %</th>
            <th>Vol bp</th>
            <th>Rate bp</th>
            <th>Div bp</th>
            <th />
          </tr>
        </thead>
        <tbody>
          {scenarios.map((scenario, index) => (
            <tr key={`${scenario.name}-${index}`}>
              <td>
                <input className="input" value={scenario.name} onChange={(event) => onChange(index, { name: event.target.value })} />
              </td>
              <td>
                <input className="input" type="number" step="0.5" value={scenario.spotShockPercent} onChange={(event) => onChange(index, { spotShockPercent: event.target.value })} />
              </td>
              <td>
                <input className="input" type="number" step="25" value={scenario.volatilityShockBps} onChange={(event) => onChange(index, { volatilityShockBps: event.target.value })} />
              </td>
              <td>
                <input className="input" type="number" step="25" value={scenario.riskFreeRateShockBps} onChange={(event) => onChange(index, { riskFreeRateShockBps: event.target.value })} />
              </td>
              <td>
                <input className="input" type="number" step="25" value={scenario.dividendYieldShockBps} onChange={(event) => onChange(index, { dividendYieldShockBps: event.target.value })} />
              </td>
              <td>
                <button className="text-button danger" type="button" onClick={() => onRemove(index)} disabled={scenarios.length <= 1}>
                  Remove
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function StressResults({ result, trade }: { result: FrontOfficeStressTestResponse; trade: AddEuropeanOptionPositionRequest | null }) {
  const worstPrice = result.scenarios.reduce((worst, scenario) => (
    scenario.impact.price < worst.impact.price ? scenario : worst
  ), result.scenarios[0]);
  const largestDelta = maxAbsScenario(result.scenarios, "delta");
  const largestVega = maxAbsScenario(result.scenarios, "vega");

  return (
    <div className="stress-result-stack">
      <div className="stress-kpis">
        <StressMetric label="Base portfolio" value={formatCurrency(result.basePortfolio.totalPrice)} />
        <StressMetric label="Worst price impact" value={formatCurrency(worstPrice?.impact.price ?? 0)} tone={(worstPrice?.impact.price ?? 0) < 0 ? "negative" : "positive"} />
        <StressMetric label="Largest delta move" value={formatNumber(largestDelta?.impact.delta ?? 0, 4)} />
        <StressMetric label="Largest vega move" value={formatNumber(largestVega?.impact.vega ?? 0, 4)} />
      </div>

      {result.hypotheticalTrade && trade ? (
        <div className="stress-hypo-note">
          <div>
            <strong>Hypothetical trade included</strong>
            <span>{trade.quantity} {trade.optionType} {trade.underlyingSymbol} @ {formatCurrency(trade.strike)} is included in stressed totals but was not booked.</span>
          </div>
          <a className="btn secondary" href={preTradeUrl(result.portfolioId, trade)}>
            <FlaskConical size={16} />
            Send to Pre-Trade Analysis
          </a>
        </div>
      ) : null}

      <StressImpactBars scenarios={result.scenarios} />

      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Scenario</th>
              <th>Total</th>
              <th>Price impact</th>
              <th>Delta</th>
              <th>Gamma</th>
              <th>Vega</th>
              <th>Theta</th>
              <th>Rho</th>
            </tr>
          </thead>
          <tbody>
            {result.scenarios.map((scenario) => (
              <tr key={scenario.scenario.name}>
                <td>
                  <strong>{scenario.scenario.name}</strong>
                  <small className="stress-shock-summary">
                    {formatPercent(scenario.scenario.spotShockPercent)} spot · {formatNumber(scenario.scenario.volatilityShockBps, 0)}bp vol · {formatNumber(scenario.scenario.riskFreeRateShockBps, 0)}bp rates
                  </small>
                </td>
                <td>{formatCurrency(scenario.totals.totalPrice)}</td>
                <td className={scenario.impact.price < 0 ? "negative" : "positive"}>{formatCurrency(scenario.impact.price)}</td>
                <td>{formatNumber(scenario.impact.delta, 4)}</td>
                <td>{formatNumber(scenario.impact.gamma, 4)}</td>
                <td>{formatNumber(scenario.impact.vega, 4)}</td>
                <td>{formatNumber(scenario.impact.theta, 4)}</td>
                <td>{formatNumber(scenario.impact.rho, 4)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {result.unpriceablePositions.length > 0 ? (
        <div className="mini-note">{result.unpriceablePositions.length} expired positions were excluded from stressed totals.</div>
      ) : null}
    </div>
  );
}

function StressMetric({ label, value, tone }: { label: string; value: string; tone?: "positive" | "negative" }) {
  return (
    <div className={`stress-metric ${tone ?? ""}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function StressImpactBars({ scenarios }: { scenarios: StressScenarioResult[] }) {
  const maxAbs = Math.max(...scenarios.map((scenario) => Math.abs(scenario.impact.price)), 1);
  return (
    <div className="stress-bars">
      {scenarios.map((scenario) => {
        const width = Math.max(4, Math.abs(scenario.impact.price) / maxAbs * 100);
        const positive = scenario.impact.price >= 0;
        return (
          <div className="stress-bar-row" key={scenario.scenario.name}>
            <span>{scenario.scenario.name}</span>
            <div className="stress-bar-track">
              <i className={positive ? "positive" : "negative"} style={{ width: `${width}%` }} />
            </div>
            <strong>{formatCurrency(scenario.impact.price)}</strong>
          </div>
        );
      })}
    </div>
  );
}

function EmptyStress() {
  return (
    <div className="empty-state">
      <Waves size={18} />
      <span>Run Stress Testing to see scenario impacts against the confirmed portfolio.</span>
    </div>
  );
}

function scenarioForm(
  name: string,
  spotShockPercent: string,
  volatilityShockBps: string,
  riskFreeRateShockBps: string,
  dividendYieldShockBps: string,
): StressScenarioForm {
  return { name, spotShockPercent, volatilityShockBps, riskFreeRateShockBps, dividendYieldShockBps };
}

function toScenarioRequest(scenario: StressScenarioForm): StressScenarioRequest {
  return {
    name: scenario.name.trim(),
    spotShockPercent: Number(scenario.spotShockPercent) / 100,
    volatilityShockBps: Number(scenario.volatilityShockBps),
    riskFreeRateShockBps: Number(scenario.riskFreeRateShockBps),
    dividendYieldShockBps: Number(scenario.dividendYieldShockBps),
  };
}

function toTradeRequest(trade: {
  underlyingSymbol: string;
  optionType: OptionType;
  strike: string;
  maturityDate: string;
  quantity: string;
}): AddEuropeanOptionPositionRequest {
  return {
    underlyingSymbol: trade.underlyingSymbol.trim().toUpperCase(),
    optionType: trade.optionType,
    strike: Number(trade.strike),
    maturityDate: trade.maturityDate,
    quantity: Number(trade.quantity),
  };
}

function maxAbsScenario(scenarios: StressScenarioResult[], key: "delta" | "vega") {
  return scenarios.reduce<StressScenarioResult | null>((largest, scenario) => {
    if (!largest || Math.abs(scenario.impact[key]) > Math.abs(largest.impact[key])) {
      return scenario;
    }
    return largest;
  }, null);
}

function preTradeUrl(portfolioId: string, trade: AddEuropeanOptionPositionRequest) {
  const params = new URLSearchParams({
    portfolioId,
    underlyingSymbol: trade.underlyingSymbol,
    optionType: trade.optionType,
    strike: String(trade.strike),
    maturityDate: trade.maturityDate,
    quantity: String(trade.quantity),
  });
  return `/pre-trade-analysis?${params.toString()}`;
}

function initialPortfolioIdFromUrl() {
  if (typeof window === "undefined") {
    return "";
  }
  return new URLSearchParams(window.location.search).get("portfolioId") ?? "";
}

function errorMessage(caught: unknown): string {
  if (caught instanceof NexusApiError) {
    return caught.message;
  }
  if (caught instanceof Error) {
    return caught.message;
  }
  return "Unexpected Stress Testing error";
}
