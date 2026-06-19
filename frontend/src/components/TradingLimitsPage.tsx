"use client";

import React from "react";
import {
  Activity,
  Ban,
  Check,
  CircleDollarSign,
  Gauge,
  Loader2,
  Search,
  ShieldAlert,
  UserRound,
  UsersRound,
} from "lucide-react";
import { nexusApi, NexusApiError } from "@/lib/api";
import { formatCurrency, formatNumber } from "@/lib/format";
import type {
  TradingLimitSnapshot,
  UpdateTradingLimitRequest,
} from "@/lib/types";
import { AppShell } from "./AppShell";

const howTo = [
  { title: "Who is controlled", body: "Trading Limits applies to active users assigned to Front Office. Each user has one policy across all portfolios." },
  { title: "Trade limits", body: "Hourly and daily trade counts include every submitted booking, even when Back Office rejects it later." },
  { title: "Notional limits", body: "V1 notional is abs(quantity) × strike in USD. It is a preventive approximation, not premium, cash spent or P&L." },
  { title: "UTC reset", body: "Hourly and daily usage resets at calendar boundaries in UTC. The exact reset time is shown beside each meter." },
];

type LimitForm = {
  maxTradesPerHour: string;
  maxTradesPerDay: string;
  maxNotionalPerHour: string;
  maxNotionalPerDay: string;
  active: boolean;
};

const emptyForm: LimitForm = {
  maxTradesPerHour: "",
  maxTradesPerDay: "",
  maxNotionalPerHour: "",
  maxNotionalPerDay: "",
  active: true,
};

export function TradingLimitsPage() {
  const [users, setUsers] = React.useState<TradingLimitSnapshot[]>([]);
  const [selected, setSelected] = React.useState<TradingLimitSnapshot | null>(null);
  const [query, setQuery] = React.useState("");
  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [success, setSuccess] = React.useState<string | null>(null);
  const [form, setForm] = React.useState<LimitForm>(emptyForm);

  React.useEffect(() => {
    const timer = window.setTimeout(() => {
      void loadUsers(query);
    }, 180);
    return () => window.clearTimeout(timer);
  }, [query]);

  async function loadUsers(search: string, preferredUserId?: string) {
    setLoading(true);
    setError(null);
    try {
      const page = await nexusApi.listTradingLimitUsers(search, 0, 100);
      setUsers(page.items);
      const next = page.items.find((user) => user.userId === (preferredUserId ?? selected?.userId))
        ?? page.items[0]
        ?? null;
      selectUser(next);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  function selectUser(user: TradingLimitSnapshot | null) {
    setSelected(user);
    setSuccess(null);
    setForm(user ? formFromSnapshot(user) : emptyForm);
  }

  async function savePolicy(active = form.active) {
    if (!selected?.userId) {
      return;
    }
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const updated = await nexusApi.updateTradingLimitUser(
        selected.userId,
        requestFromForm(form, selected, active),
      );
      setSelected(updated);
      setForm(formFromSnapshot(updated));
      setUsers((current) => current.map((user) => user.userId === updated.userId ? updated : user));
      setSuccess(active ? `Limits saved for ${updated.displayName}.` : `Limits disabled for ${updated.displayName}.`);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setSaving(false);
    }
  }

  const limited = users.filter((user) => user.status === "ACTIVE").length;
  const unlimited = users.filter((user) => user.status !== "ACTIVE").length;
  const nearLimit = users.filter(isNearLimit).length;

  return (
    <AppShell title="Trading Limits" eyebrow="Back Office control" howTo={howTo}>
      {error ? <div className="alert">{error}</div> : null}
      {success ? <div className="success">{success}</div> : null}

      <div className="summary-strip limits-summary">
        <SummaryMetric icon={<UsersRound size={18} />} label="FO users" value={users.length} />
        <SummaryMetric icon={<Gauge size={18} />} label="Limited" value={limited} />
        <SummaryMetric icon={<Activity size={18} />} label="Near limit" value={nearLimit} tone={nearLimit > 0 ? "warning" : undefined} />
        <SummaryMetric icon={<Ban size={18} />} label="Unlimited / disabled" value={unlimited} />
      </div>

      <div className="limits-workspace">
        <section className="panel limits-directory">
          <div className="section-head">
            <div>
              <h2>Front Office users</h2>
              <p>Preventive controls across every portfolio.</p>
            </div>
          </div>
          <label className="bo-search limits-search">
            <Search size={16} />
            <input
              aria-label="Search Front Office users"
              placeholder="Search name or username"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </label>

          {loading ? (
            <div className="empty"><Loader2 className="spin" size={18} /> Loading users</div>
          ) : users.length === 0 ? (
            <div className="empty">No active Front Office users match this search.</div>
          ) : (
            <div className="limit-user-list">
              {users.map((user) => (
                <button
                  className={`limit-user-row ${selected?.userId === user.userId ? "selected" : ""}`}
                  key={user.userId ?? user.username}
                  type="button"
                  onClick={() => selectUser(user)}
                >
                  <span className="limit-user-avatar"><UserRound size={16} /></span>
                  <span>
                    <strong>{user.displayName}</strong>
                    <small>@{user.username}</small>
                  </span>
                  <span className={`limit-status ${user.status.toLowerCase()}`}>{user.status}</span>
                  <small>{usageHeadline(user)}</small>
                </button>
              ))}
            </div>
          )}
        </section>

        <section className="panel limits-detail">
          {selected ? (
            <>
              <div className="limits-detail-head">
                <div>
                  <span className={`limit-status ${selected.status.toLowerCase()}`}>{selected.status}</span>
                  <h2>{selected.displayName}</h2>
                  <p>@{selected.username} · USD preventive policy</p>
                </div>
                <ShieldAlert size={24} />
              </div>

              <div className="usage-grid">
                <UsageMeter
                  label="Trades this hour"
                  current={selected.usage.tradesThisHour}
                  maximum={selected.policy?.active ? selected.policy.maxTradesPerHour : null}
                  resetAt={selected.usage.hourEndsAt}
                />
                <UsageMeter
                  label="Trades today"
                  current={selected.usage.tradesToday}
                  maximum={selected.policy?.active ? selected.policy.maxTradesPerDay : null}
                  resetAt={selected.usage.dayEndsAt}
                />
                <UsageMeter
                  label="Notional this hour"
                  current={selected.usage.notionalThisHour}
                  maximum={selected.policy?.active ? selected.policy.maxNotionalPerHour : null}
                  resetAt={selected.usage.hourEndsAt}
                  currency
                />
                <UsageMeter
                  label="Notional today"
                  current={selected.usage.notionalToday}
                  maximum={selected.policy?.active ? selected.policy.maxNotionalPerDay : null}
                  resetAt={selected.usage.dayEndsAt}
                  currency
                />
              </div>

              <div className="limits-editor">
                <div className="section-head compact">
                  <div>
                    <h3>Policy settings</h3>
                    <p>Leave a field empty when that measure should be unlimited.</p>
                  </div>
                </div>
                <div className="form-grid">
                  <LimitInput label="Max trades / hour" value={form.maxTradesPerHour} onChange={(value) => setForm({ ...form, maxTradesPerHour: value })} integer />
                  <LimitInput label="Max trades / day" value={form.maxTradesPerDay} onChange={(value) => setForm({ ...form, maxTradesPerDay: value })} integer />
                  <LimitInput label="Max notional / hour" value={form.maxNotionalPerHour} onChange={(value) => setForm({ ...form, maxNotionalPerHour: value })} prefix="USD" />
                  <LimitInput label="Max notional / day" value={form.maxNotionalPerDay} onChange={(value) => setForm({ ...form, maxNotionalPerDay: value })} prefix="USD" />
                </div>

                <div className="notional-explainer">
                  <CircleDollarSign size={18} />
                  <span><strong>Notional V1 = abs(quantity) × strike.</strong> This is not premium paid, cash movement or P&amp;L.</span>
                </div>

                <div className="limits-actions">
                  <button className="btn primary" type="button" onClick={() => savePolicy(true)} disabled={saving}>
                    {saving ? <Loader2 className="spin" size={16} /> : <Check size={16} />}
                    Save active policy
                  </button>
                  {selected.policy ? (
                    <button className="btn secondary danger-outline" type="button" onClick={() => savePolicy(false)} disabled={saving || selected.status === "DISABLED"}>
                      <Ban size={16} />
                      Disable policy
                    </button>
                  ) : null}
                </div>
                {selected.policy ? (
                  <p className="audit-note">
                    Last updated {new Date(selected.policy.updatedAt).toLocaleString()} by {selected.policy.updatedByDisplayName}.
                  </p>
                ) : null}
              </div>
            </>
          ) : (
            <div className="empty">Select a Front Office user to inspect and configure limits.</div>
          )}
        </section>
      </div>
    </AppShell>
  );
}

function SummaryMetric({ icon, label, value, tone }: { icon: React.ReactNode; label: string; value: number; tone?: string }) {
  return (
    <div className={`summary-metric ${tone ?? ""}`}>
      <span>{icon}</span>
      <div><strong>{value}</strong><small>{label}</small></div>
    </div>
  );
}

function UsageMeter({
  label,
  current,
  maximum,
  resetAt,
  currency = false,
}: {
  label: string;
  current: number;
  maximum: number | null | undefined;
  resetAt: string;
  currency?: boolean;
}) {
  const percent = maximum == null || maximum <= 0 ? 0 : Math.min((current / maximum) * 100, 100);
  const displayCurrent = currency ? formatCurrency(current, "USD") : formatNumber(current, 0);
  const displayMaximum = maximum == null ? "Unlimited" : currency ? formatCurrency(maximum, "USD") : formatNumber(maximum, 0);
  return (
    <div className="usage-meter">
      <div>
        <span>{label}</span>
        <strong>{displayCurrent} <small>/ {displayMaximum}</small></strong>
      </div>
      <div className="usage-track" aria-label={`${label} ${Math.round(percent)} percent used`}>
        <span className={percent >= 80 ? "near" : ""} style={{ width: `${percent}%` }} />
      </div>
      <small>Resets {new Date(resetAt).toLocaleString()} · UTC boundary</small>
    </div>
  );
}

function LimitInput({
  label,
  value,
  onChange,
  integer = false,
  prefix,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  integer?: boolean;
  prefix?: string;
}) {
  return (
    <label className="field limit-field">
      <span>{label}</span>
      <div className="limit-input-wrap">
        {prefix ? <small>{prefix}</small> : null}
        <input
          className="input"
          type="number"
          min="1"
          step={integer ? "1" : "0.01"}
          placeholder="Unlimited"
          value={value}
          onChange={(event) => onChange(event.target.value)}
        />
      </div>
    </label>
  );
}

function formFromSnapshot(snapshot: TradingLimitSnapshot): LimitForm {
  return {
    maxTradesPerHour: valueOrBlank(snapshot.policy?.maxTradesPerHour),
    maxTradesPerDay: valueOrBlank(snapshot.policy?.maxTradesPerDay),
    maxNotionalPerHour: valueOrBlank(snapshot.policy?.maxNotionalPerHour),
    maxNotionalPerDay: valueOrBlank(snapshot.policy?.maxNotionalPerDay),
    active: snapshot.policy?.active ?? true,
  };
}

function requestFromForm(
  form: LimitForm,
  snapshot: TradingLimitSnapshot,
  active: boolean,
): UpdateTradingLimitRequest {
  return {
    maxTradesPerHour: optionalNumber(form.maxTradesPerHour),
    maxTradesPerDay: optionalNumber(form.maxTradesPerDay),
    maxNotionalPerHour: optionalNumber(form.maxNotionalPerHour),
    maxNotionalPerDay: optionalNumber(form.maxNotionalPerDay),
    active,
    version: snapshot.policy?.version ?? null,
  };
}

function optionalNumber(value: string) {
  return value.trim() === "" ? null : Number(value);
}

function valueOrBlank(value: number | null | undefined) {
  return value == null ? "" : String(value);
}

function usageHeadline(user: TradingLimitSnapshot) {
  if (user.status !== "ACTIVE") {
    return "No active blocking policy";
  }
  return `${user.usage.tradesToday} trades · ${formatCurrency(user.usage.notionalToday, "USD")} today`;
}

function isNearLimit(user: TradingLimitSnapshot) {
  if (user.status !== "ACTIVE" || !user.policy) {
    return false;
  }
  const ratios = [
    ratio(user.usage.tradesThisHour, user.policy.maxTradesPerHour),
    ratio(user.usage.tradesToday, user.policy.maxTradesPerDay),
    ratio(user.usage.notionalThisHour, user.policy.maxNotionalPerHour),
    ratio(user.usage.notionalToday, user.policy.maxNotionalPerDay),
  ];
  return ratios.some((value) => value >= 0.8);
}

function ratio(current: number, maximum: number | null) {
  return maximum == null || maximum <= 0 ? 0 : current / maximum;
}

function errorMessage(caught: unknown) {
  return caught instanceof NexusApiError || caught instanceof Error ? caught.message : "Unexpected request failure";
}
