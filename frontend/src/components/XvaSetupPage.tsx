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
  ShieldOff,
  Wallet,
  X,
} from "lucide-react";
import { nexusApi, NexusApiError } from "@/lib/api";
import { formatCurrency, formatNumber } from "@/lib/format";
import type { AdminPortfolioSummary, Counterparty, NettingSet } from "@/lib/types";
import { AppShell } from "./AppShell";

const howTo = [
  { title: "Counterparties", body: "ADMIN owns counterparty reference data. Inactive counterparties are kept for history but blocked for new netting-set CVA." },
  { title: "Netting sets", body: "A netting set groups portfolios for profile-level CVA netting. V1 allows one netting set per portfolio." },
  { title: "Collateral", body: "Collateral is a static amount in the netting set base currency. It reduces positive exposure buckets in V1." },
  { title: "Portfolio assignment", body: "Only non-archived portfolios with matching base currency can be assigned. A portfolio can belong to one netting set." },
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

export function XvaSetupPage() {
  const [counterparties, setCounterparties] = React.useState<Counterparty[]>([]);
  const [nettingSets, setNettingSets] = React.useState<NettingSet[]>([]);
  const [portfolios, setPortfolios] = React.useState<AdminPortfolioSummary[]>([]);
  const [selectedCounterpartyId, setSelectedCounterpartyId] = React.useState("");
  const [selectedNettingSetId, setSelectedNettingSetId] = React.useState("");
  const [query, setQuery] = React.useState("");
  const [counterpartyForm, setCounterpartyForm] = React.useState<CounterpartyForm>(emptyCounterpartyForm);
  const [newCounterpartyForm, setNewCounterpartyForm] = React.useState<CounterpartyForm>(emptyCounterpartyForm);
  const [nettingSetForm, setNettingSetForm] = React.useState<NettingSetForm>(emptyNettingSetForm);
  const [newNettingSetForm, setNewNettingSetForm] = React.useState<NettingSetForm>(emptyNettingSetForm);
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
      setCounterparties(nextCounterparties);
      setNettingSets(nextNettingSets);
      setPortfolios(nextPortfolios);
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

  async function createCounterparty() {
    if (!newCounterpartyForm.name.trim()) return;
    await withSave("create-counterparty", async () => {
      const created = await nexusApi.createCounterparty(counterpartyRequest(newCounterpartyForm));
      setNewCounterpartyForm(emptyCounterpartyForm);
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

      <div className="xva-setup-layout">
        <section className="panel xva-directory">
          <div className="section-head">
            <div>
              <h2>Counterparties</h2>
              <p>Reference data kept by ADMIN for netting-set CVA.</p>
            </div>
            <button className="icon-button" type="button" onClick={() => load()} disabled={loading} title="Refresh XVA setup">
              {loading ? <Loader2 className="spin" size={16} /> : <RefreshCw size={16} />}
            </button>
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
          <div className="xva-create-card">
            <h3>Create counterparty</h3>
            <TextInput label="Name" value={newCounterpartyForm.name} onChange={(name) => setNewCounterpartyForm({ ...newCounterpartyForm, name })} />
            <TextInput label="External id" value={newCounterpartyForm.externalId} onChange={(externalId) => setNewCounterpartyForm({ ...newCounterpartyForm, externalId })} />
            <TextInput label="Credit rating" value={newCounterpartyForm.creditRating} onChange={(creditRating) => setNewCounterpartyForm({ ...newCounterpartyForm, creditRating })} />
            <button className="btn" type="button" onClick={createCounterparty} disabled={saving === "create-counterparty" || !newCounterpartyForm.name.trim()}>
              {saving === "create-counterparty" ? <Loader2 className="spin" size={16} /> : <Plus size={16} />}
              Create counterparty
            </button>
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
                    <label className="admin-check xva-toggle">
                      <input type="checkbox" checked={counterpartyForm.active} onChange={(event) => setCounterpartyForm({ ...counterpartyForm, active: event.target.checked })} />
                      <span>Active</span>
                    </label>
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
                  <div className="xva-netting-list">
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
                        <label className="admin-check xva-toggle">
                          <input type="checkbox" checked={nettingSetForm.active} onChange={(event) => setNettingSetForm({ ...nettingSetForm, active: event.target.checked })} />
                          <span>Active</span>
                        </label>
                        <div className="xva-readonly-field">
                          <span>Currency</span>
                          <strong>{selectedNettingSet.baseCurrency}</strong>
                        </div>
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
            </>
          ) : (
            <div className="empty">Create a counterparty to start configuring XVA reference data.</div>
          )}
        </section>
      </div>
    </AppShell>
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

function errorMessage(caught: unknown) {
  return caught instanceof NexusApiError || caught instanceof Error ? caught.message : "Unexpected XVA setup error";
}
