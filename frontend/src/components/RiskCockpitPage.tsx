"use client";

import React from "react";
import { Activity, AlertTriangle, BarChart3, CircleDollarSign, Clock3, Play, RefreshCw, ShieldCheck, Waves } from "lucide-react";
import { AppShell } from "./AppShell";
import { RiskBarChart, RiskLineChart } from "./RiskCockpitCharts";
import { authApi, nexusApi } from "@/lib/api";
import type { PortfolioSummary, RiskPackComponent, RiskPackRun } from "@/lib/types";

type Tab = "overview" | "market" | "pnl" | "exposure" | "diagnostics";
const terminal = new Set(["SUCCESS", "PARTIAL", "FAILED"]);
const money = (value: number | null | undefined, currency="USD") => value == null ? "Unavailable" : new Intl.NumberFormat("en-US",{style:"currency",currency,maximumFractionDigits:0}).format(value);
const number = (value: number | null | undefined) => value == null ? "Unavailable" : new Intl.NumberFormat("en-US",{maximumFractionDigits:2}).format(value);
const component = (run: RiskPackRun | null, type: string) => run?.components.find((item)=>item.type===type);

export function RiskCockpitPage() {
  const [portfolios,setPortfolios]=React.useState<PortfolioSummary[]>([]);
  const [portfolioId,setPortfolioId]=React.useState("");
  const [run,setRun]=React.useState<RiskPackRun|null>(null);
  const [tab,setTab]=React.useState<Tab>("overview");
  const [valuationDate,setValuationDate]=React.useState(new Date().toISOString().slice(0,10));
  const [canRun,setCanRun]=React.useState(false);
  const [loading,setLoading]=React.useState(true);
  const [error,setError]=React.useState<string|null>(null);

  React.useEffect(()=>{ Promise.all([nexusApi.listRiskCockpitPortfolios(),authApi.me()]).then(([items,auth])=>{
    setPortfolios(items); setPortfolioId((current)=>current||items[0]?.id||""); setCanRun(auth.activeGroup==="FO");
  }).catch((e)=>setError(e instanceof Error?e.message:"Risk Cockpit unavailable")).finally(()=>setLoading(false)); },[]);

  const loadLatest=React.useCallback(async()=>{ if(!portfolioId)return; try { const latest=await nexusApi.getLatestRiskPack(portfolioId); setRun(latest??null); setError(null); } catch(e){setError(e instanceof Error?e.message:"Unable to load Risk Pack");}},[portfolioId]);
  React.useEffect(()=>{void loadLatest();},[loadLatest]);
  React.useEffect(()=>{ if(!run||terminal.has(run.status))return; const id=window.setInterval(async()=>{try{setRun(await nexusApi.getRiskPack(run.id));}catch{}},2000); return()=>window.clearInterval(id);},[run]);

  async function start(){ if(!portfolioId)return; setError(null); try{const queued=await nexusApi.startRiskPack(portfolioId,valuationDate); setRun(await nexusApi.getRiskPack(queued.runId)); setTab("overview");}catch(e){setError(e instanceof Error?e.message:"Unable to queue Risk Pack");}}
  const portfolio=portfolios.find((item)=>item.id===portfolioId);
  const pricing=component(run,"PRICING")?.output as any;
  const valuation=pricing?.valuation; const pnl=pricing?.pnl;
  const varData=component(run,"VAR")?.output as any;
  const stress=component(run,"STRESS")?.output as any;
  const exposure=component(run,"EXPOSURE")?.output as any;
  const cva=component(run,"CVA")?.output as any;
  const outdated=Boolean(run&&portfolio&&new Date(portfolio.updatedAt)>new Date(run.portfolioUpdatedAt));
  const peakEe=exposure?.points?.reduce((max:number,p:any)=>Math.max(max,p.expectedExposure),0);
  const peakPfe=exposure?.points?.reduce((max:number,p:any)=>Math.max(max,p.pfe),0);

  return <AppShell eyebrow="Portfolio risk" title="Risk Cockpit" howTo={[
    {title:"Risk Pack",body:"Runs Pricing, standard Stress, Historical VaR, Exposure and CVA under one valuation date."},
    {title:"Partial result",body:"Successful components remain visible when another component lacks market data or approved curves."},
    {title:"Historical VaR V1",body:"Replays aligned equity spot returns. Volatility, rates, dividends and FX are held at current values."},
  ]}>
    <section className="risk-cockpit-toolbar">
      <label>Portfolio<select value={portfolioId} onChange={(e)=>setPortfolioId(e.target.value)}>{portfolios.map((item)=><option value={item.id} key={item.id}>{item.name}</option>)}</select></label>
      <label>Valuation date<input type="date" value={valuationDate} onChange={(e)=>setValuationDate(e.target.value)} /></label>
      <div className="risk-run-meta"><span className={`status-pill status-${(run?.status??"NONE").toLowerCase()}`}>{run?.status??"NO RUN"}</span><small>{run?.completedAt?new Date(run.completedAt).toLocaleString():run?.queuedAt?`Queued ${new Date(run.queuedAt).toLocaleString()}`:"Select a portfolio"}</small></div>
      <div className="risk-toolbar-actions"><button className="icon-button" title="Refresh results" onClick={()=>void loadLatest()}><RefreshCw size={17}/></button>{canRun&&<button className="primary-button" disabled={!portfolioId||Boolean(run&&!terminal.has(run.status))} onClick={()=>void start()}><Play size={17}/>Run Risk Pack</button>}</div>
    </section>
    {error&&<div className="error-banner">{error}</div>}
    {outdated&&<div className="warning-banner"><AlertTriangle size={17}/>Portfolio changed after this run. Results are outdated.</div>}
    {loading?<div className="loading-state">Loading cockpit…</div>:!run?<div className="empty-state">No Risk Pack has been run for this portfolio yet.</div>:<>
      <nav className="risk-tabs" aria-label="Risk Cockpit views">
        {([['overview','Overview'],['market','Market Risk'],['pnl','P&L & Sensitivities'],['exposure','Exposure & CVA'],['diagnostics','Run Diagnostics']] as [Tab,string][]).map(([id,label])=><button className={tab===id?"active":""} onClick={()=>setTab(id)} key={id}>{label}</button>)}
      </nav>
      {tab==="overview"&&<Overview run={run} valuation={valuation} pnl={pnl} varData={varData} peakEe={peakEe} peakPfe={peakPfe} cva={cva} />}
      {tab==="market"&&<MarketRisk varData={varData} stress={stress} />}
      {tab==="pnl"&&<PnlSensitivity valuation={valuation} pnl={pnl} />}
      {tab==="exposure"&&<ExposureCva exposure={exposure} cva={cva} />}
      {tab==="diagnostics"&&<Diagnostics run={run} />}
    </>}
  </AppShell>;
}

function Overview({run,valuation,pnl,varData,peakEe,peakPfe,cva}:{run:RiskPackRun;valuation:any;pnl:any;varData:any;peakEe:number;peakPfe:number;cva:any}){
  const currency=valuation?.baseCurrency??"USD";
  return <div className="risk-view"><div className="risk-kpi-grid">
    <Kpi icon={CircleDollarSign} label="Market value" value={money(valuation?.totalPrice,currency)} tone="neutral"/>
    <Kpi icon={Activity} label="Daily P&L" value={money(pnl?.dailyPnl,currency)} tone={(pnl?.dailyPnl??0)<0?"danger":"positive"}/>
    <Kpi icon={BarChart3} label="99% Historical VaR" value={money(varData?.valueAtRisk,currency)} tone="danger"/>
    <Kpi icon={Waves} label="Expected Shortfall" value={money(varData?.expectedShortfall,currency)} tone="warning"/>
    <Kpi icon={Activity} label="Peak EE / PFE" value={`${money(peakEe,currency)} / ${money(peakPfe,currency)}`} tone="warning"/>
    <Kpi icon={ShieldCheck} label="CVA charge" value={money(cva?.cva,currency)} tone="danger"/>
  </div><section className="risk-band"><div><h2>Risk Pack status</h2><p>One valuation date, five independently observable components.</p></div><div className="component-strip">{run.components.map((item)=><span className={`component-state state-${item.status.toLowerCase()}`} key={item.type}><i/>{item.type}<small>{item.status}</small></span>)}</div></section>
  <div className="risk-two-column"><section><h2>Attention items</h2><ul className="attention-list">{run.components.filter((item)=>item.status==='FAILED'||item.status==='SKIPPED').map((item)=><li key={item.type}><AlertTriangle size={16}/><div><strong>{item.type}</strong><span>{item.errorMessage}</span></div></li>)}{run.components.every((item)=>item.status==='SUCCESS')&&<li className="clear"><ShieldCheck size={16}/>All components completed.</li>}</ul></section><section><h2>Run context</h2><dl className="risk-definition-list"><div><dt>Valuation date</dt><dd>{run.valuationDate}</dd></div><div><dt>Market data as-of</dt><dd>{run.marketDataAsOf?new Date(run.marketDataAsOf).toLocaleString():"Unavailable"}</dd></div><div><dt>Requested by</dt><dd>{run.requestedByUsername??"System"}</dd></div><div><dt>Run ID</dt><dd className="mono">{run.id}</dd></div></dl></section></div></div>;
}
function MarketRisk({varData,stress}:{varData:any;stress:any}){return <div className="risk-view"><div className="risk-three-column"><section><h2>Historical loss distribution</h2><RiskBarChart valueLabel="Scenario count" points={(varData?.pnlDistribution??[]).map((b:any)=>({label:`${number(b.fromPnl)} to ${number(b.toPnl)}`,value:b.count}))}/></section><section><h2>Worst historical scenarios</h2><table className="compact-table"><thead><tr><th>Date</th><th>P&L</th><th>Loss</th></tr></thead><tbody>{(varData?.worstScenarios??[]).map((s:any)=><tr key={s.date}><td>{s.date}</td><td>{money(s.pnl)}</td><td className="negative-value">{money(s.loss)}</td></tr>)}</tbody></table></section><section><h2>VaR scenario by symbol</h2><RiskBarChart valueLabel="P&L contribution" points={(varData?.varScenarioContributions??[]).map((c:any)=>({label:c.symbol,value:c.pnl}))}/></section></div><section className="risk-wide-section"><h2>Standard stress impacts</h2><RiskBarChart valueLabel="Price impact" points={(stress?.scenarios??[]).map((s:any)=>({label:s.scenario.name,value:s.impact.price}))}/></section><div className="model-limitations"><strong>Not modelled in Historical VaR V1</strong>{(varData?.unmodeledRiskFactors??["VOLATILITY","RATES","DIVIDEND_YIELD","FX"]).map((item:string)=><span key={item}>{item}</span>)}</div></div>}
function PnlSensitivity({valuation,pnl}:{valuation:any;pnl:any}){const g=valuation?.totalGreeks??{};return <div className="risk-view"><div className="risk-kpi-grid sensitivities">{["delta","gamma","vega","theta","rho"].map((key)=><Kpi key={key} icon={Activity} label={key.toUpperCase()} value={number(g[key])} tone="neutral"/>)}</div><section className="risk-wide-section"><h2>Position drill-down</h2><table className="compact-table"><thead><tr><th>Product</th><th>Symbol</th><th>Market value</th><th>Daily P&L</th><th>Since trade P&L</th></tr></thead><tbody>{(pnl?.positions??[]).map((p:any)=><tr key={p.positionId}><td>{p.instrumentType}</td><td>{p.underlyingSymbol}</td><td>{money(p.currentMarketValue,pnl.baseCurrency)}</td><td>{money(p.dailyPnl,pnl.baseCurrency)}</td><td>{money(p.sinceTradePnl,pnl.baseCurrency)}</td></tr>)}</tbody></table></section></div>}
function ExposureCva({exposure,cva}:{exposure:any;cva:any}){return <div className="risk-view"><div className="risk-two-column"><section><h2>EE and PFE profile</h2><RiskLineChart primaryLabel="Expected exposure" secondaryLabel="PFE" points={(exposure?.points??[]).map((p:any)=>({label:p.date,value:p.expectedExposure,secondary:p.pfe}))}/></section><section><h2>CVA contribution</h2><RiskBarChart valueLabel="CVA contribution" points={(cva?.points??[]).map((p:any)=>({label:p.date,value:p.cvaContribution}))}/></section></div><section className="risk-wide-section"><h2>Exposure buckets</h2><table className="compact-table"><thead><tr><th>Date</th><th>EE</th><th>ENE</th><th>PFE</th><th>CVA contribution</th></tr></thead><tbody>{(exposure?.points??[]).map((p:any,i:number)=><tr key={p.date}><td>{p.date}</td><td>{money(p.expectedExposure)}</td><td>{money(p.expectedNegativeExposure)}</td><td>{money(p.pfe)}</td><td>{money(cva?.points?.[i]?.cvaContribution)}</td></tr>)}</tbody></table></section></div>}
function Diagnostics({run}:{run:RiskPackRun}){return <div className="risk-view"><section className="risk-wide-section"><h2>Component diagnostics</h2><table className="compact-table"><thead><tr><th>Component</th><th>Status</th><th>Duration</th><th>Started</th><th>Diagnostic</th></tr></thead><tbody>{run.components.map((c)=><tr key={c.type}><td><strong>{c.type}</strong></td><td><span className={`status-pill status-${c.status.toLowerCase()}`}>{c.status}</span></td><td>{c.durationMs==null?"—":`${c.durationMs} ms`}</td><td>{c.startedAt?new Date(c.startedAt).toLocaleTimeString():"—"}</td><td>{c.errorMessage??"Completed without reported errors"}</td></tr>)}</tbody></table></section><section className="risk-wide-section"><h2>Configuration used</h2><pre className="risk-json">{JSON.stringify(run.configuration,null,2)}</pre></section></div>}
function Kpi({icon:Icon,label,value,tone}:{icon:React.ComponentType<{size?:number}>;label:string;value:string;tone:string}){return <article className={`risk-kpi tone-${tone}`}><span><Icon size={18}/></span><div><small>{label}</small><strong className={value==="Unavailable"?"is-unavailable":undefined}>{value}</strong></div></article>}
