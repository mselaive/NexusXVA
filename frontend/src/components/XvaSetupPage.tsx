"use client";

import React from "react";
import {
  Building2,
  CheckCircle2,
  CircleDollarSign,
  GitBranch,
  Landmark,
  Loader2,
  Plus,
  RefreshCw,
  Save,
  Search,
  Upload,
  Wallet,
  X,
} from "lucide-react";
import { nexusApi, NexusApiError } from "@/lib/api";
import { formatCurrency, formatNumber, todayIsoDate } from "@/lib/format";
import type { AdminPortfolioSummary, Counterparty, CreditCurve, CreditCurveType, DiscountCurve, NettingSet } from "@/lib/types";
import { AppShell } from "./AppShell";

const howTo = [
  { title: "Counterparties", body: "ADMIN owns counterparty reference data. Inactive counterparties are kept for history but blocked for new netting-set CVA." },
  { title: "Netting sets", body: "A netting set groups portfolios for profile-level CVA netting. V1 allows one netting set per portfolio." },
  { title: "Collateral", body: "Collateral is a static amount in the netting set base currency. It reduces positive exposure buckets in V1." },
  { title: "Portfolio assignment", body: "Only non-archived portfolios with matching base currency can be assigned. A portfolio can belong to one netting set." },
  { title: "CVA curves", body: "ADMIN can store credit and discount curves as master data. CVA can then reference curve IDs instead of sending inline points on every run." },
];

type CounterpartyForm = {
  name: string;
  externalId: string;
  creditRating: string;
  active: boolean;
};

type NettingSetForm = {
  name: string;
  baseCurrency: string;
  collateralAmount: string;
  active: boolean;
};

type CurveForm = {
  name: string;
  curveType: CreditCurveType;
  currency: string;
  valuationDate: string;
  recoveryRate: string;
  allowStale: boolean;
};

const emptyCounterpartyForm: CounterpartyForm = {
  name: "",
  externalId: "",
  creditRating: "",
  active: true,
};

const emptyNettingSetForm: NettingSetForm = {
  name: "",
  baseCurrency: "USD",
  collateralAmount: "0",
  active: true,
};

const emptyCurveForm: CurveForm = {
  name: "",
  curveType: "SURVIVAL_PROBABILITY",
  currency: "USD",
  valuationDate: todayIsoDate(),
  recoveryRate: "0.40",
  allowStale: false,
};

export function XvaSetupPage() {
  const [counterparties, setCounterparties] = React.useState<Counterparty[]>([]);
  const [nettingSets, setNettingSets] = React.useState<NettingSet[]>([]);
  const [portfolios, setPortfolios] = React.useState<AdminPortfolioSummary[]>([]);
  const [creditCurves, setCreditCurves] = React.useState<CreditCurve[]>([]);
  const [discountCurves, setDiscountCurves] = React.useState<DiscountCurve[]>([]);
  const [selectedCounterpartyId, setSelectedCounterpartyId] = React.useState("");
  const [selectedNettingSetId, setSelectedNettingSetId] = React.useState("");
  const [query, setQuery] = React.useState("");
  const [counterpartyForm, setCounterpartyForm] = React.useState<CounterpartyForm>(emptyCounterpartyForm);
  const [newCounterpartyForm, setNewCounterpartyForm] = React.useState<CounterpartyForm>(emptyCounterpartyForm);
  const [nettingSetForm, setNettingSetForm] = React.useState<NettingSetForm>(emptyNettingSetForm);
  const [newNettingSetForm, setNewNettingSetForm] = React.useState<NettingSetForm>(emptyNettingSetForm);
  const [creditCurveForm, setCreditCurveForm] = React.useState<CurveForm>(emptyCurveForm);
  const [discountCurveForm, setDiscountCurveForm] = React.useState<CurveForm>(emptyCurveForm);
  const [createCounterpartyOpen, setCreateCounterpartyOpen] = React.useState(false);
  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [success, setSuccess] = React.useState<string | null>(null);

  React.useEffect(() => {
    void load();
  }, []);

  React.useEffect(() => {
    const selected = counterparties.find((counterparty) => counterparty.id === selectedCounterpartyId) ?? null;
    setCounterpartyForm(selected ? formFromCounterparty(selected) : emptyCounterpartyForm);
  }, [counterparties, selectedCounterpartyId]);

  React.useEffect(() => {
    const selected = nettingSets.find((nettingSet) => nettingSet.id === selectedNettingSetId) ?? null;
    setNettingSetForm(selected ? formFromNettingSet(selected) : emptyNettingSetForm);
  }, [nettingSets, selectedNettingSetId]);

  async function load(preferredCounterpartyId = selectedCounterpartyId, preferredNettingSetId = selectedNettingSetId) {
    setLoading(true);
    setError(null);
    try {
      const [nextCounterparties, nextNettingSets, nextPortfolios] = await Promise.all([
        nexusApi.listCounterparties(true),
        nexusApi.listNettingSets(true),
        nexusApi.listAdminPortfolios(),
      ]);
      const [nextCreditCurves, nextDiscountCurves] = await Promise.all([
        nexusApi.listCreditCurves(undefined, true),
        nexusApi.listDiscountCurves(undefined, true),
      ]);
      setCounterparties(nextCounterparties);
      setNettingSets(nextNettingSets);
      setPortfolios(nextPortfolios);
      setCreditCurves(nextCreditCurves);
      setDiscountCurves(nextDiscountCurves);
      const nextCounterpartyId = preferredCounterpartyId && nextCounterparties.some((item) => item.id === preferredCounterpartyId)
        ? preferredCounterpartyId
        : nextCounterparties[0]?.id ?? "";
      const counterpartyNettingSets = nextNettingSets.filter((item) => item.counterpartyId === nextCounterpartyId);
      const nextNettingSetId = preferredNettingSetId && counterpartyNettingSets.some((item) => item.id === preferredNettingSetId)
        ? preferredNettingSetId
        : counterpartyNettingSets[0]?.id ?? "";
      setSelectedCounterpartyId(nextCounterpartyId);
      setSelectedNettingSetId(nextNettingSetId);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  const filteredCounterparties = counterparties.filter((counterparty) => {
    const needle = query.trim().toLowerCase();
    if (!needle) return true;
    return counterparty.name.toLowerCase().includes(needle)
      || (counterparty.externalId ?? "").toLowerCase().includes(needle)
      || (counterparty.creditRating ?? "").toLowerCase().includes(needle);
  });
  const selectedCounterparty = counterparties.find((counterparty) => counterparty.id === selectedCounterpartyId) ?? null;
  const selectedNettingSets = nettingSets.filter((nettingSet) => nettingSet.counterpartyId === selectedCounterpartyId);
  const selectedNettingSet = nettingSets.find((nettingSet) => nettingSet.id === selectedNettingSetId) ?? null;
  const assignedPortfolioIds = new Set(nettingSets.flatMap((nettingSet) => nettingSet.portfolios.map((portfolio) => portfolio.portfolioId)));
  const activeCounterparties = counterparties.filter((counterparty) => counterparty.active).length;
  const activeNettingSets = nettingSets.filter((nettingSet) => nettingSet.active && nettingSet.counterpartyActive).length;
  const assignedPortfolios = new Set(nettingSets.flatMap((nettingSet) => nettingSet.portfolios.map((portfolio) => portfolio.portfolioId))).size;
  const totalCollateral = nettingSets
    .filter((nettingSet) => nettingSet.active && nettingSet.counterpartyActive)
    .reduce((total, nettingSet) => total + nettingSet.collateralAmount, 0);
  const selectedCreditCurves = creditCurves.filter((curve) => curve.counterpartyId === selectedCounterpartyId);

  async function createCounterparty() {
    if (!newCounterpartyForm.name.trim()) return;
    await withSave("create-counterparty", async () => {
      const created = await nexusApi.createCounterparty(counterpartyRequest(newCounterpartyForm));
      setNewCounterpartyForm(emptyCounterpartyForm);
      setCreateCounterpartyOpen(false);
      setSuccess(`Counterparty "${created.name}" created.`);
      await load(created.id, "");
    });
  }

  async function saveCounterparty() {
    if (!selectedCounterparty) return;
    await withSave("counterparty", async () => {
      const updated = await nexusApi.updateCounterparty(selectedCounterparty.id, counterpartyRequest(counterpartyForm));
      setSuccess(`Counterparty "${updated.name}" updated.`);
      await load(updated.id, selectedNettingSetId);
    });
  }

  async function createNettingSet() {
    if (!selectedCounterparty || !newNettingSetForm.name.trim()) return;
    await withSave("create-netting-set", async () => {
      const created = await nexusApi.createNettingSet({
        counterpartyId: selectedCounterparty.id,
        name: newNettingSetForm.name.trim(),
        baseCurrency: newNettingSetForm.baseCurrency.trim().toUpperCase(),
        collateralCurrency: newNettingSetForm.baseCurrency.trim().toUpperCase(),
        collateralAmount: Number(newNettingSetForm.collateralAmount || "0"),
      });
      setNewNettingSetForm(emptyNettingSetForm);
      setSuccess(`Netting set "${created.name}" created.`);
      await load(selectedCounterparty.id, created.id);
    });
  }

  async function saveNettingSet() {
    if (!selectedNettingSet) return;
    await withSave("netting-set", async () => {
      const updated = await nexusApi.updateNettingSet(selectedNettingSet.id, {
        name: nettingSetForm.name.trim(),
        active: nettingSetForm.active,
      });
      const collateral = Number(nettingSetForm.collateralAmount || "0");
      const next = collateral !== updated.collateralAmount
        ? await nexusApi.updateNettingSetCollateral(updated.id, collateral)
        : updated;
      setSuccess(`Netting set "${next.name}" updated.`);
      await load(next.counterpartyId, next.id);
    });
  }

  async function assignPortfolio(portfolioId: string) {
    if (!selectedNettingSet) return;
    await withSave(`assign-${portfolioId}`, async () => {
      const updated = await nexusApi.assignNettingSetPortfolio(selectedNettingSet.id, portfolioId);
      setSuccess("Portfolio assigned.");
      await load(updated.counterpartyId, updated.id);
    });
  }

  async function removePortfolio(portfolioId: string) {
    if (!selectedNettingSet) return;
    await withSave(`remove-${portfolioId}`, async () => {
      const updated = await nexusApi.removeNettingSetPortfolio(selectedNettingSet.id, portfolioId);
      setSuccess("Portfolio removed.");
      await load(updated.counterpartyId, updated.id);
    });
  }

  async function createCreditCurve() {
    if (!selectedCounterparty || !creditCurveForm.name.trim()) return;
    await withSave("create-credit-curve", async () => {
      const created = await nexusApi.createCreditCurve({
        counterpartyId: selectedCounterparty.id,
        name: creditCurveForm.name.trim(),
        curveType: creditCurveForm.curveType,
        active: true,
        points: defaultCreditCurvePoints(creditCurveForm.curveType),
      });
      setCreditCurveForm(emptyCurveForm);
      setSuccess(`Credit curve "${created.name}" created.`);
      await load(selectedCounterparty.id, selectedNettingSetId);
    });
  }

  async function createDiscountCurve() {
    if (!discountCurveForm.name.trim()) return;
    await withSave("create-discount-curve", async () => {
      const created = await nexusApi.createDiscountCurve({
        name: discountCurveForm.name.trim(),
        currency: discountCurveForm.currency.trim().toUpperCase(),
        active: true,
        points: defaultDiscountCurvePoints(),
      });
      setDiscountCurveForm(emptyCurveForm);
      setSuccess(`Discount curve "${created.name}" created.`);
      await load(selectedCounterpartyId, selectedNettingSetId);
    });
  }

  async function importCreditCurve(file: File) {
    if (!selectedCounterparty || !creditCurveForm.name.trim()) {
      setError("Choose a counterparty and enter a curve name before importing.");
      return;
    }
    await withSave("import-credit-curve", async () => {
      const rows = parseCsv(file.name, await file.text());
      const points = rows.map((row) => ({
        date: requiredCell(row, "date"),
        survivalProbability: creditCurveForm.curveType === "SURVIVAL_PROBABILITY" ? numberCell(row, "survivalProbability", "value") : null,
        cumulativeDefaultProbability: creditCurveForm.curveType === "CUMULATIVE_DEFAULT_PROBABILITY" ? numberCell(row, "cumulativeDefaultProbability", "value") : null,
      }));
      const created = await nexusApi.importCreditCurve({
        counterpartyId: selectedCounterparty.id,
        name: creditCurveForm.name.trim(),
        curveType: creditCurveForm.curveType,
        active: false,
        points,
      });
      setSuccess(`Credit curve "${created.name}" imported as DRAFT.`);
      await load(selectedCounterparty.id, selectedNettingSetId);
    });
  }

  async function importCreditCurveFromMarketData() {
    if (!selectedCounterparty) return;
    const recoveryRate = Number(creditCurveForm.recoveryRate);
    if (!selectedCounterparty.creditRating?.trim()) {
      setError("Set a credit rating on the counterparty before importing a market curve.");
      return;
    }
    if (!Number.isFinite(recoveryRate) || recoveryRate < 0 || recoveryRate >= 1) {
      setError("Recovery rate must be between 0 and 1.");
      return;
    }
    await withSave("import-market-credit-curve", async () => {
      const created = await nexusApi.importMarketDataCreditCurve({
        counterpartyId: selectedCounterparty.id,
        valuationDate: creditCurveForm.valuationDate,
        recoveryRate,
        name: creditCurveForm.name.trim() || null,
        allowStale: creditCurveForm.allowStale,
      });
      setSuccess(`Market credit proxy "${created.name}" imported as DRAFT. Review it before approval.`);
      await load(selectedCounterparty.id, selectedNettingSetId);
    });
  }

  async function importDiscountCurve(file: File) {
    if (!discountCurveForm.name.trim()) {
      setError("Enter a discount curve name before importing.");
      return;
    }
    await withSave("import-discount-curve", async () => {
      const rows = parseCsv(file.name, await file.text());
      const points = rows.map((row) => ({ date: requiredCell(row, "date"), discountFactor: numberCell(row, "discountFactor", "value") }));
      const created = await nexusApi.importDiscountCurve({
        name: discountCurveForm.name.trim(),
        currency: discountCurveForm.currency.trim().toUpperCase(),
        active: false,
        points,
      });
      setSuccess(`Discount curve "${created.name}" imported as DRAFT.`);
      await load(selectedCounterpartyId, selectedNettingSetId);
    });
  }

  async function importDiscountCurveFromMarketData() {
    const currency = discountCurveForm.currency.trim().toUpperCase();
    if (!currency) {
      setError("Enter the discount curve currency before importing from market data.");
      return;
    }
    await withSave("import-market-discount-curve", async () => {
      const created = await nexusApi.importMarketDataDiscountCurve({
        currency,
        valuationDate: discountCurveForm.valuationDate,
        name: discountCurveForm.name.trim() || null,
        allowStale: discountCurveForm.allowStale,
      });
      setSuccess(`Market-data curve "${created.name}" imported as DRAFT. Review it before approval.`);
      await load(selectedCounterpartyId, selectedNettingSetId);
    });
  }

  async function approveCreditCurve(curveId: string) {
    await withSave(`approve-credit-${curveId}`, async () => {
      const approved = await nexusApi.approveCreditCurve(curveId);
      setSuccess(`Credit curve "${approved.name}" v${approved.version} approved.`);
      await load(selectedCounterpartyId, selectedNettingSetId);
    });
  }

  async function rejectCreditCurve(curveId: string) {
    const reason = window.prompt("Why reject this credit curve?");
    if (!reason?.trim()) return;
    await withSave(`reject-credit-${curveId}`, async () => {
      const rejected = await nexusApi.rejectCreditCurve(curveId, reason.trim());
      setSuccess(`Credit curve "${rejected.name}" v${rejected.version} rejected.`);
      await load(selectedCounterpartyId, selectedNettingSetId);
    });
  }

  async function approveDiscountCurve(curveId: string) {
    await withSave(`approve-discount-${curveId}`, async () => {
      const approved = await nexusApi.approveDiscountCurve(curveId);
      setSuccess(`Discount curve "${approved.name}" v${approved.version} approved.`);
      await load(selectedCounterpartyId, selectedNettingSetId);
    });
  }

  async function rejectDiscountCurve(curveId: string) {
    const reason = window.prompt("Why reject this discount curve?");
    if (!reason?.trim()) return;
    await withSave(`reject-discount-${curveId}`, async () => {
      const rejected = await nexusApi.rejectDiscountCurve(curveId, reason.trim());
      setSuccess(`Discount curve "${rejected.name}" v${rejected.version} rejected.`);
      await load(selectedCounterpartyId, selectedNettingSetId);
    });
  }

  async function withSave(label: string, operation: () => Promise<void>) {
    setSaving(label);
    setError(null);
    setSuccess(null);
    try {
      await operation();
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setSaving(null);
    }
  }

  return (
    <AppShell title="XVA Setup" eyebrow="Administration" howTo={howTo}>
      {error ? <div className="alert">{error}</div> : null}
      {success ? <div className="success">{success}</div> : null}

      <div className="summary-strip xva-summary">
        <MetricCard icon={<Building2 size={18} />} label="Active counterparties" value={formatNumber(activeCounterparties, 0)} />
        <MetricCard icon={<GitBranch size={18} />} label="Active netting sets" value={formatNumber(activeNettingSets, 0)} />
        <MetricCard icon={<Wallet size={18} />} label="Assigned portfolios" value={formatNumber(assignedPortfolios, 0)} />
        <MetricCard icon={<CircleDollarSign size={18} />} label="Static collateral" value={formatCurrency(totalCollateral)} />
      </div>

      <section className="panel xva-create-counterparty-panel">
        <div className="section-head">
          <div>
            <h2>Counterparty setup</h2>
            <p>Create a legal/reference counterparty, then assign netting sets and portfolios below.</p>
          </div>
          <div className="xva-top-actions">
            <button className="btn secondary" type="button" onClick={() => load()} disabled={loading}>
              {loading ? <Loader2 className="spin" size={16} /> : <RefreshCw size={16} />}
              Refresh
            </button>
            <button className="btn" type="button" onClick={() => setCreateCounterpartyOpen((current) => !current)}>
              <Plus size={16} />
              New counterparty
            </button>
          </div>
        </div>
        {createCounterpartyOpen ? (
          <div className="xva-create-inline">
            <TextInput label="Name" value={newCounterpartyForm.name} onChange={(name) => setNewCounterpartyForm({ ...newCounterpartyForm, name })} />
            <TextInput label="External id" value={newCounterpartyForm.externalId} onChange={(externalId) => setNewCounterpartyForm({ ...newCounterpartyForm, externalId })} />
            <TextInput label="Credit rating" value={newCounterpartyForm.creditRating} onChange={(creditRating) => setNewCounterpartyForm({ ...newCounterpartyForm, creditRating })} />
            <button className="btn xva-create-submit" type="button" onClick={createCounterparty} disabled={saving === "create-counterparty" || !newCounterpartyForm.name.trim()}>
              {saving === "create-counterparty" ? <Loader2 className="spin" size={16} /> : <Plus size={16} />}
              Create
            </button>
          </div>
        ) : null}
      </section>

      <div className="xva-setup-layout">
        <section className="panel xva-directory">
          <div className="section-head">
            <div>
              <h2>Counterparties</h2>
              <p>Reference data kept by ADMIN for netting-set CVA.</p>
            </div>
          </div>
          <label className="bo-search admin-search">
            <Search size={16} />
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search counterparty, external id or rating" />
          </label>
          <div className="xva-counterparty-list">
            {filteredCounterparties.map((counterparty) => (
              <button
                className={`xva-counterparty-row ${selectedCounterpartyId === counterparty.id ? "selected" : ""}`}
                key={counterparty.id}
                type="button"
                onClick={() => {
                  setSelectedCounterpartyId(counterparty.id);
                  setSelectedNettingSetId(nettingSets.find((item) => item.counterpartyId === counterparty.id)?.id ?? "");
                }}
              >
                <span className={`status-dot ${counterparty.active ? "active" : "inactive"}`} />
                <strong>{counterparty.name}</strong>
                <small>{counterparty.externalId || "No external id"} · {counterparty.creditRating || "No rating"}</small>
                <em>{nettingSets.filter((item) => item.counterpartyId === counterparty.id).length} netting sets</em>
              </button>
            ))}
            {loading ? <div className="empty"><Loader2 className="spin" size={18} /> Loading XVA setup</div> : null}
            {!loading && filteredCounterparties.length === 0 ? <div className="empty">No counterparties match this search.</div> : null}
          </div>
        </section>

        <section className="panel xva-detail">
          {selectedCounterparty ? (
            <>
              <div className="xva-detail-head">
                <div>
                  <span className={`status-chip ${selectedCounterparty.active ? "success-chip" : "danger-chip"}`}>
                    {selectedCounterparty.active ? "ACTIVE" : "INACTIVE"}
                  </span>
                  <h2>{selectedCounterparty.name}</h2>
                  <p>{selectedCounterparty.externalId || "No external id"} · {selectedCounterparty.creditRating || "No credit rating"}</p>
                </div>
                <Landmark size={24} />
              </div>

              <div className="xva-two-column">
                <section className="xva-card">
                  <div className="section-head compact">
                    <div>
                      <h3>Counterparty profile</h3>
                      <p>Inactive counterparties are blocked for new CVA runs.</p>
                    </div>
                  </div>
                  <div className="form-grid">
                    <TextInput label="Name" value={counterpartyForm.name} onChange={(name) => setCounterpartyForm({ ...counterpartyForm, name })} />
                    <TextInput label="External id" value={counterpartyForm.externalId} onChange={(externalId) => setCounterpartyForm({ ...counterpartyForm, externalId })} />
                    <TextInput label="Credit rating" value={counterpartyForm.creditRating} onChange={(creditRating) => setCounterpartyForm({ ...counterpartyForm, creditRating })} />
                    <ToggleField label="Active" checked={counterpartyForm.active} onChange={(active) => setCounterpartyForm({ ...counterpartyForm, active })} />
                  </div>
                  <button className="btn" type="button" onClick={saveCounterparty} disabled={saving === "counterparty" || !counterpartyForm.name.trim()}>
                    {saving === "counterparty" ? <Loader2 className="spin" size={16} /> : <Save size={16} />}
                    Save counterparty
                  </button>
                </section>

                <section className="xva-card">
                  <div className="section-head compact">
                    <div>
                      <h3>Create netting set</h3>
                      <p>Base and collateral currency must match in V1.</p>
                    </div>
                  </div>
                  <div className="form-grid">
                    <TextInput label="Name" value={newNettingSetForm.name} onChange={(name) => setNewNettingSetForm({ ...newNettingSetForm, name })} />
                    <TextInput label="Currency" value={newNettingSetForm.baseCurrency} onChange={(baseCurrency) => setNewNettingSetForm({ ...newNettingSetForm, baseCurrency })} />
                    <TextInput label="Collateral amount" value={newNettingSetForm.collateralAmount} type="number" onChange={(collateralAmount) => setNewNettingSetForm({ ...newNettingSetForm, collateralAmount })} />
                  </div>
                  <button className="btn secondary" type="button" onClick={createNettingSet} disabled={saving === "create-netting-set" || !selectedCounterparty.active || !newNettingSetForm.name.trim()}>
                    {saving === "create-netting-set" ? <Loader2 className="spin" size={16} /> : <Plus size={16} />}
                    Create netting set
                  </button>
                </section>
              </div>

              <div className="xva-netting-workspace">
                <section className="xva-card">
                  <div className="section-head compact">
                    <div>
                      <h3>Netting sets</h3>
                      <p>Select a set to edit collateral and portfolio assignment.</p>
                    </div>
                  </div>
                  <div className="xva-netting-list xva-netting-selector">
                    {selectedNettingSets.map((nettingSet) => (
                      <button
                        className={`xva-netting-row ${selectedNettingSetId === nettingSet.id ? "selected" : ""}`}
                        key={nettingSet.id}
                        type="button"
                        onClick={() => setSelectedNettingSetId(nettingSet.id)}
                      >
                        <span className={`status-dot ${nettingSet.active && nettingSet.counterpartyActive ? "active" : "inactive"}`} />
                        <strong>{nettingSet.name}</strong>
                        <small>{nettingSet.baseCurrency} · {formatCurrency(nettingSet.collateralAmount, nettingSet.collateralCurrency)} collateral</small>
                        <em>{nettingSet.portfolios.length} portfolios</em>
                      </button>
                    ))}
                    {selectedNettingSets.length === 0 ? <div className="empty">This counterparty has no netting sets yet.</div> : null}
                  </div>
                </section>

                <section className="xva-card xva-card-wide">
                  {selectedNettingSet ? (
                    <>
                      <div className="section-head compact">
                        <div>
                          <h3>{selectedNettingSet.name}</h3>
                          <p>{selectedNettingSet.baseCurrency} · {selectedNettingSet.counterpartyActive ? "Counterparty active" : "Counterparty inactive"}</p>
                        </div>
                        <span className={`status-chip ${selectedNettingSet.active && selectedNettingSet.counterpartyActive ? "success-chip" : "danger-chip"}`}>
                          {selectedNettingSet.active && selectedNettingSet.counterpartyActive ? "OPERABLE" : "BLOCKED"}
                        </span>
                      </div>
                      <div className="form-grid">
                        <TextInput label="Name" value={nettingSetForm.name} onChange={(name) => setNettingSetForm({ ...nettingSetForm, name })} />
                        <TextInput label="Collateral amount" value={nettingSetForm.collateralAmount} type="number" onChange={(collateralAmount) => setNettingSetForm({ ...nettingSetForm, collateralAmount })} />
                        <label className="field xva-readonly-field">
                          <span>Currency</span>
                          <strong>{selectedNettingSet.baseCurrency}</strong>
                        </label>
                        <ToggleField label="Active" checked={nettingSetForm.active} onChange={(active) => setNettingSetForm({ ...nettingSetForm, active })} />
                      </div>
                      <button className="btn" type="button" onClick={saveNettingSet} disabled={saving === "netting-set" || !nettingSetForm.name.trim()}>
                        {saving === "netting-set" ? <Loader2 className="spin" size={16} /> : <Save size={16} />}
                        Save netting set
                      </button>
                      <PortfolioAssignment
                        assignedPortfolioIds={assignedPortfolioIds}
                        nettingSet={selectedNettingSet}
                        portfolios={portfolios}
                        saving={saving}
                        onAssign={assignPortfolio}
                        onRemove={removePortfolio}
                      />
                    </>
                  ) : (
                    <div className="empty">Select or create a netting set to manage collateral and portfolios.</div>
                  )}
                </section>
              </div>

              <section className="xva-card xva-card-wide xva-curve-master-data">
                <div className="section-head compact">
                  <div>
                    <h3>Curve master data</h3>
                    <p>Create reusable curves for CVA curve mode. Demo curves start at 6, 12 and 18 months.</p>
                  </div>
                </div>
                <div className="xva-two-column">
                  <div>
                    <h4>Credit curves for {selectedCounterparty.name}</h4>
                    <div className="form-grid">
                      <TextInput label="Name" value={creditCurveForm.name} onChange={(name) => setCreditCurveForm({ ...creditCurveForm, name })} />
                      <label className="field">
                        <span>Curve type</span>
                        <select className="select" value={creditCurveForm.curveType} onChange={(event) => setCreditCurveForm({ ...creditCurveForm, curveType: event.target.value as CreditCurveType })}>
                          <option value="SURVIVAL_PROBABILITY">Survival probability</option>
                          <option value="CUMULATIVE_DEFAULT_PROBABILITY">Cumulative default probability</option>
                        </select>
                      </label>
                      <label className="field">
                        <span>Valuation date</span>
                        <input className="input" type="date" value={creditCurveForm.valuationDate} onChange={(event) => setCreditCurveForm({ ...creditCurveForm, valuationDate: event.target.value })} />
                      </label>
                      <TextInput label="Recovery rate" value={creditCurveForm.recoveryRate} type="number" onChange={(recoveryRate) => setCreditCurveForm({ ...creditCurveForm, recoveryRate })} />
                    </div>
                    <div className="xva-market-import-summary">
                      <span>Market rating</span>
                      <strong>{selectedCounterparty.creditRating || "Not configured"}</strong>
                      <small>Blemberg maps investment-grade ratings to a USD OAS proxy. Imported curves stay in DRAFT.</small>
                    </div>
                    <label className="inline-check curve-stale-toggle">
                      <input type="checkbox" checked={creditCurveForm.allowStale} onChange={(event) => setCreditCurveForm({ ...creditCurveForm, allowStale: event.target.checked })} />
                      Allow stale market credit curve import
                    </label>
                    <div className="toolbar">
                      <button className="btn secondary" type="button" onClick={createCreditCurve} disabled={saving === "create-credit-curve" || !selectedCounterparty.active || !creditCurveForm.name.trim()}>
                        {saving === "create-credit-curve" ? <Loader2 className="spin" size={16} /> : <Plus size={16} />}
                        Create credit curve
                      </button>
                      <label className="btn secondary curve-import-button">
                        <Upload size={16} /> Import CSV
                        <input type="file" accept=".csv,text/csv" onChange={(event) => { const file = event.target.files?.[0]; if (file) void importCreditCurve(file); event.target.value = ""; }} />
                      </label>
                      <button className="btn secondary" type="button" onClick={importCreditCurveFromMarketData} disabled={saving === "import-market-credit-curve" || !selectedCounterparty.active || !selectedCounterparty.creditRating}>
                        {saving === "import-market-credit-curve" ? <Loader2 className="spin" size={16} /> : <RefreshCw size={16} />}
                        Import market draft
                      </button>
                    </div>
                    <CurveList
                      curves={selectedCreditCurves.map((curve) => ({
                        id: curve.id,
                        label: `${curve.name} v${curve.version} · ${curve.curveType.replaceAll("_", " ")}`,
                        status: curve.status,
                        active: curve.active,
                        detail: curve.source === "MARKET_DATA"
                          ? `${curve.marketProxy ? "Market proxy" : "Market data"} · ${curve.sourceRatingBucket ?? curve.sourceCreditRating ?? "rating unavailable"} · OAS ${curve.sourceSpread == null ? "unknown" : `${(curve.sourceSpread * 10000).toFixed(1)} bp`} · recovery ${curve.sourceRecoveryRate == null ? "unknown" : `${(curve.sourceRecoveryRate * 100).toFixed(0)}%`} · hazard ${curve.sourceHazardRate == null ? "unknown" : curve.sourceHazardRate.toFixed(6)} · ${curve.sourceSeriesId ?? "series unavailable"} · observed ${curve.sourceObservationDate ?? "unknown"}${curve.sourceStale ? " · STALE" : ""}`
                          : curve.source,
                      }))}
                      emptyText="No credit curves for this counterparty."
                      saving={saving}
                      onApprove={approveCreditCurve}
                      onReject={rejectCreditCurve}
                    />
                  </div>
                  <div>
                    <h4>Discount curves</h4>
                    <div className="form-grid">
                      <TextInput label="Name" value={discountCurveForm.name} onChange={(name) => setDiscountCurveForm({ ...discountCurveForm, name })} />
                      <TextInput label="Currency" value={discountCurveForm.currency} onChange={(currency) => setDiscountCurveForm({ ...discountCurveForm, currency })} />
                      <label className="field">
                        <span>Valuation date</span>
                        <input className="input" type="date" value={discountCurveForm.valuationDate} onChange={(event) => setDiscountCurveForm({ ...discountCurveForm, valuationDate: event.target.value })} />
                      </label>
                    </div>
                    <label className="inline-check curve-stale-toggle">
                      <input type="checkbox" checked={discountCurveForm.allowStale} onChange={(event) => setDiscountCurveForm({ ...discountCurveForm, allowStale: event.target.checked })} />
                      Allow stale market curve import
                    </label>
                    <div className="toolbar">
                      <button className="btn secondary" type="button" onClick={createDiscountCurve} disabled={saving === "create-discount-curve" || !discountCurveForm.name.trim()}>
                        {saving === "create-discount-curve" ? <Loader2 className="spin" size={16} /> : <Plus size={16} />}
                        Create discount curve
                      </button>
                      <label className="btn secondary curve-import-button">
                        <Upload size={16} /> Import CSV
                        <input type="file" accept=".csv,text/csv" onChange={(event) => { const file = event.target.files?.[0]; if (file) void importDiscountCurve(file); event.target.value = ""; }} />
                      </label>
                      <button className="btn secondary" type="button" onClick={importDiscountCurveFromMarketData} disabled={saving === "import-market-discount-curve" || !discountCurveForm.currency.trim()}>
                        {saving === "import-market-discount-curve" ? <Loader2 className="spin" size={16} /> : <RefreshCw size={16} />}
                        Import from Blemberg
                      </button>
                    </div>
                    <CurveList
                      curves={discountCurves.map((curve) => ({
                        id: curve.id,
                        label: `${curve.currency} · ${curve.name} v${curve.version}`,
                        status: curve.status,
                        active: curve.active,
                        detail: curve.source === "MARKET_DATA"
                          ? `${curve.source}${curve.sourceStale ? " · STALE" : ""} · ${curve.constructionMethod ?? "method unavailable"} · as of ${curve.sourceAsOf ? new Date(curve.sourceAsOf).toLocaleString() : "unknown"}`
                          : curve.source,
                      }))}
                      emptyText="No discount curves configured."
                      saving={saving}
                      onApprove={approveDiscountCurve}
                      onReject={rejectDiscountCurve}
                    />
                  </div>
                </div>
              </section>
            </>
          ) : (
            <div className="empty">Create a counterparty to start configuring XVA reference data.</div>
          )}
        </section>
      </div>
    </AppShell>
  );
}

function CurveList({
  curves,
  emptyText,
  saving,
  onApprove,
  onReject,
}: {
  curves: Array<{ id: string; label: string; status: string; active: boolean; detail?: string }>;
  emptyText: string;
  saving: string | null;
  onApprove: (curveId: string) => void;
  onReject: (curveId: string) => void;
}) {
  if (curves.length === 0) {
    return <div className="empty compact-empty">{emptyText}</div>;
  }
  return (
    <div className="xva-curve-list">
      {curves.map((curve) => (
        <div className="xva-curve-pill" key={curve.id}>
          <div className="xva-curve-identity">
            <span>{curve.label} · {curve.status}{curve.active ? " · Active" : ""}</span>
            {curve.detail ? <small>{curve.detail}</small> : null}
          </div>
          {curve.status === "DRAFT" ? (
            <div className="row-actions">
              <button className="text-action" type="button" onClick={() => onApprove(curve.id)} disabled={saving === `approve-credit-${curve.id}` || saving === `approve-discount-${curve.id}`}>
                Approve
              </button>
              <button className="text-action danger" type="button" onClick={() => onReject(curve.id)} disabled={saving === `reject-credit-${curve.id}` || saving === `reject-discount-${curve.id}`}>
                Reject
              </button>
            </div>
          ) : null}
        </div>
      ))}
    </div>
  );
}

function PortfolioAssignment({
  portfolios,
  nettingSet,
  assignedPortfolioIds,
  saving,
  onAssign,
  onRemove,
}: {
  portfolios: AdminPortfolioSummary[];
  nettingSet: NettingSet;
  assignedPortfolioIds: Set<string>;
  saving: string | null;
  onAssign: (portfolioId: string) => void;
  onRemove: (portfolioId: string) => void;
}) {
  const assignedToCurrent = new Set(nettingSet.portfolios.map((portfolio) => portfolio.portfolioId));

  return (
    <div className="xva-assignment">
      <div className="section-head compact">
        <div>
          <h3>Portfolio assignment</h3>
          <p>Only matching currency portfolios can enter this netting set.</p>
        </div>
      </div>
      <div className="xva-portfolio-grid">
        {portfolios.map((portfolio) => {
          const assignedHere = assignedToCurrent.has(portfolio.id);
          const assignedElsewhere = assignedPortfolioIds.has(portfolio.id) && !assignedHere;
          const currencyMismatch = portfolio.baseCurrency !== nettingSet.baseCurrency;
          const disabled = assignedElsewhere || currencyMismatch || !nettingSet.active || !nettingSet.counterpartyActive;
          const reason = assignedElsewhere
            ? "Assigned elsewhere"
            : currencyMismatch
              ? `Needs ${nettingSet.baseCurrency}`
              : !nettingSet.active || !nettingSet.counterpartyActive
                ? "Netting set blocked"
                : "Available";
          return (
            <div className={`xva-portfolio-card ${assignedHere ? "assigned" : ""} ${disabled && !assignedHere ? "disabled" : ""}`} key={portfolio.id}>
              <div>
                <strong>{portfolio.name}</strong>
                <small>{portfolio.baseCurrency} · {formatNumber(portfolio.positionCount, 0)} positions · {reason}</small>
              </div>
              {assignedHere ? (
                <button className="btn secondary compact-action" type="button" onClick={() => onRemove(portfolio.id)} disabled={saving === `remove-${portfolio.id}`}>
                  {saving === `remove-${portfolio.id}` ? <Loader2 className="spin" size={14} /> : <X size={14} />}
                  Remove
                </button>
              ) : (
                <button className="btn compact-action" type="button" onClick={() => onAssign(portfolio.id)} disabled={disabled || saving === `assign-${portfolio.id}`}>
                  {saving === `assign-${portfolio.id}` ? <Loader2 className="spin" size={14} /> : <CheckCircle2 size={14} />}
                  Assign
                </button>
              )}
            </div>
          );
        })}
      </div>
    </div>
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

function TextInput({
  label,
  value,
  onChange,
  type = "text",
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  type?: "text" | "number";
}) {
  return (
    <label className="field">
      <span>{label}</span>
      <input className="input" type={type} value={value} onChange={(event) => onChange(event.target.value)} />
    </label>
  );
}

function ToggleField({
  label,
  checked,
  onChange,
}: {
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}) {
  return (
    <label className={`field xva-switch-field ${checked ? "active" : ""}`}>
      <span>{label}</span>
      <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
      <i aria-hidden="true" />
    </label>
  );
}

function formFromCounterparty(counterparty: Counterparty): CounterpartyForm {
  return {
    name: counterparty.name,
    externalId: counterparty.externalId ?? "",
    creditRating: counterparty.creditRating ?? "",
    active: counterparty.active,
  };
}

function formFromNettingSet(nettingSet: NettingSet): NettingSetForm {
  return {
    name: nettingSet.name,
    baseCurrency: nettingSet.baseCurrency,
    collateralAmount: String(nettingSet.collateralAmount),
    active: nettingSet.active,
  };
}

function counterpartyRequest(form: CounterpartyForm) {
  return {
    name: form.name.trim(),
    externalId: form.externalId.trim() || null,
    creditRating: form.creditRating.trim() || null,
    active: form.active,
  };
}

function defaultCreditCurvePoints(curveType: CreditCurveType) {
  const values = curveType === "SURVIVAL_PROBABILITY" ? [0.99, 0.97, 0.94] : [0.01, 0.03, 0.06];
  return values.map((value, index) => ({
    date: addMonths(todayIsoDate(), (index + 1) * 6),
    survivalProbability: curveType === "SURVIVAL_PROBABILITY" ? value : null,
    cumulativeDefaultProbability: curveType === "CUMULATIVE_DEFAULT_PROBABILITY" ? value : null,
  }));
}

function defaultDiscountCurvePoints() {
  return [0.98, 0.96, 0.93].map((discountFactor, index) => ({
    date: addMonths(todayIsoDate(), (index + 1) * 6),
    discountFactor,
  }));
}

function parseCsv(fileName: string, text: string): Array<Record<string, string>> {
  const lines = text.replace(/^\uFEFF/, "").split(/\r?\n/).filter((line) => line.trim().length > 0);
  if (lines.length < 2) throw new Error(`${fileName} must contain a header and at least one data row.`);
  const headers = parseCsvLine(lines[0]).map((header) => header.trim());
  const rows = lines.slice(1).map((line, index) => {
    const values = parseCsvLine(line);
    if (values.length !== headers.length) throw new Error(`${fileName} row ${index + 2} has ${values.length} columns; expected ${headers.length}.`);
    return Object.fromEntries(headers.map((header, column) => [header, values[column].trim()]));
  });
  return rows;
}

function parseCsvLine(line: string): string[] {
  const values: string[] = [];
  let value = "";
  let quoted = false;
  for (let index = 0; index < line.length; index += 1) {
    const character = line[index];
    if (character === '"') {
      if (quoted && line[index + 1] === '"') {
        value += '"';
        index += 1;
      } else {
        quoted = !quoted;
      }
    } else if (character === "," && !quoted) {
      values.push(value);
      value = "";
    } else {
      value += character;
    }
  }
  if (quoted) throw new Error("CSV contains an unclosed quoted value.");
  values.push(value);
  return values;
}

function requiredCell(row: Record<string, string>, name: string): string {
  const value = row[name]?.trim();
  if (!value) throw new Error(`CSV column ${name} is required.`);
  return value;
}

function numberCell(row: Record<string, string>, primary: string, fallback: string): number {
  const raw = row[primary]?.trim() || row[fallback]?.trim();
  const value = Number(raw);
  if (!raw || !Number.isFinite(value)) throw new Error(`CSV column ${primary} (or ${fallback}) must contain a finite number.`);
  return value;
}

function addMonths(date: string, months: number) {
  const next = new Date(`${date}T00:00:00Z`);
  next.setUTCMonth(next.getUTCMonth() + months);
  return next.toISOString().slice(0, 10);
}

function errorMessage(caught: unknown) {
  return caught instanceof NexusApiError || caught instanceof Error ? caught.message : "Unexpected XVA setup error";
}
