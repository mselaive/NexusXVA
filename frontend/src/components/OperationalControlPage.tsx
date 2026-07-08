"use client";

import React from "react";
import { CalendarClock, CheckCircle2, Save, XCircle } from "lucide-react";
import { nexusApi } from "@/lib/api";
import type { OperationalControlSettings, UpdateOperationalControlRequest } from "@/lib/types";
import { useOperationalControlStatus } from "@/lib/operationalControl";
import { AppShell } from "./AppShell";

const days = [
  ["MONDAY", "Mon"],
  ["TUESDAY", "Tue"],
  ["WEDNESDAY", "Wed"],
  ["THURSDAY", "Thu"],
  ["FRIDAY", "Fri"],
  ["SATURDAY", "Sat"],
  ["SUNDAY", "Sun"],
] as const;

const howTo = [
  { title: "Trading window", body: "The window defines open/closed business time. ADMIN can block trade capture, risk runs, both, or neither." },
  { title: "EOD schedule", body: "The backend scheduler checks this database setting every minute, so changing the time does not require a restart." },
  { title: "BO remains open", body: "BO validation and EOD corrections stay available outside trading hours." },
];

export function OperationalControlPage() {
  const { status, refresh } = useOperationalControlStatus();
  const [settings, setSettings] = React.useState<OperationalControlSettings | null>(null);
  const [form, setForm] = React.useState<UpdateOperationalControlRequest | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState(false);
  const [message, setMessage] = React.useState<string | null>(null);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    void load();
  }, []);

  async function load() {
    try {
      setLoading(true);
      const next = await nexusApi.getOperationalControlSettings();
      setSettings(next);
      setForm(toForm(next));
      setError(null);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "Operational control unavailable");
    } finally {
      setLoading(false);
    }
  }

  async function save() {
    if (!form) {
      return;
    }
    try {
      setSaving(true);
      const updated = await nexusApi.updateOperationalControlSettings(form);
      setSettings(updated);
      setForm(toForm(updated));
      setMessage("Operational control updated.");
      setError(null);
      await refresh();
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "Could not save operational control");
    } finally {
      setSaving(false);
    }
  }

  function patch(patch: Partial<UpdateOperationalControlRequest>) {
    setForm((current) => current ? { ...current, ...patch } : current);
    setMessage(null);
  }

  function toggleDay(day: string) {
    if (!form) {
      return;
    }
    const hasDay = form.businessDays.includes(day);
    patch({
      businessDays: hasDay
        ? form.businessDays.filter((candidate) => candidate !== day)
        : [...form.businessDays, day],
    });
  }

  return (
    <AppShell title="Operational Control" eyebrow="Administration" howTo={howTo}>
      <section className="ops-control-hero">
        <div className={`ops-status-card ${status?.tradingOpen ? "open" : status?.operationalWindowEnforced ? "closed" : "advisory"}`}>
          {status?.tradingOpen ? <CheckCircle2 size={22} /> : <XCircle size={22} />}
          <span>{status?.tradingOpen ? "Trading Open" : status?.operationalWindowEnforced ? "Trading Closed" : "Window Advisory"}</span>
          <strong>{status?.reason?.replaceAll("_", " ") ?? "Loading"}</strong>
        </div>
        <div className="ops-status-grid">
          <MiniStatus label="Timezone" value={status?.timezone ?? settings?.timezone ?? "—"} />
          <MiniStatus label="Current business time" value={status ? new Date(status.currentBusinessTime).toLocaleString() : "—"} />
          <MiniStatus label="Next open" value={status ? new Date(status.nextOpenAt).toLocaleString() : "—"} />
          <MiniStatus label="Next EOD" value={status?.eodEnabled ? new Date(status.nextEodAt).toLocaleString() : "Disabled"} />
        </div>
      </section>

      {error ? <div className="panel-error">{error}</div> : null}
      {message ? <div className="success-banner">{message}</div> : null}

      <section className="panel section">
        <div className="section-heading ops-control-heading">
          <div>
            <span className="page-eyebrow">Global policy</span>
            <h2>Trading window and EOD schedule</h2>
          </div>
          <button className="btn" type="button" onClick={save} disabled={saving || loading || !form}>
            <Save size={16} />
            {saving ? "Saving..." : "Save control"}
          </button>
        </div>

        {!form ? <div className="empty">Loading operational control...</div> : (
          <div className="ops-control-layout">
            <div className="ops-form-card">
              <h3>Trading window</h3>
              <label className="ops-toggle-row">
                <span>
                  <strong>Block new trade bookings</strong>
                  <small>FO bookings and lifecycle requests return 409 outside the configured window.</small>
                </span>
                <input
                  type="checkbox"
                  checked={form.blockTradeBookingsOutsideWindow}
                  onChange={(event) => patch({ blockTradeBookingsOutsideWindow: event.target.checked })}
                />
              </label>
              <label className="ops-toggle-row">
                <span>
                  <strong>Block risk runs</strong>
                  <small>Pricing, Pre-Trade Analysis, Stress, Delta Hedge, Exposure and CVA return 409 outside the window.</small>
                </span>
                <input
                  type="checkbox"
                  checked={form.blockRiskRunsOutsideWindow}
                  onChange={(event) => patch({ blockRiskRunsOutsideWindow: event.target.checked })}
                />
              </label>
              <label>
                <span>Timezone</span>
                <input value={form.timezone} onChange={(event) => patch({ timezone: event.target.value })} />
              </label>
              <div className="ops-time-grid">
                <label>
                  <span>Open</span>
                  <input type="time" value={form.tradingOpenTime} onChange={(event) => patch({ tradingOpenTime: event.target.value })} />
                </label>
                <label>
                  <span>Close</span>
                  <input type="time" value={form.tradingCloseTime} onChange={(event) => patch({ tradingCloseTime: event.target.value })} />
                </label>
              </div>
              <div className="ops-days">
                {days.map(([day, label]) => (
                  <button
                    className={form.businessDays.includes(day) ? "active" : ""}
                    key={day}
                    type="button"
                    onClick={() => toggleDay(day)}
                  >
                    {label}
                  </button>
                ))}
              </div>
            </div>

            <div className="ops-form-card">
              <h3>EOD scheduler</h3>
              <label className="ops-toggle-row">
                <span>
                  <strong>Automatic EOD</strong>
                  <small>Runs once per business date after the configured time.</small>
                </span>
                <input
                  type="checkbox"
                  checked={form.eodEnabled}
                  onChange={(event) => patch({ eodEnabled: event.target.checked })}
                />
              </label>
              <label>
                <span>EOD run time</span>
                <input type="time" value={form.eodRunTime} onChange={(event) => patch({ eodRunTime: event.target.value })} />
              </label>
              <label className="ops-toggle-row">
                <span>
                  <strong>Allow stale market data</strong>
                  <small>Keep off unless BO explicitly accepts stale cached inputs for close.</small>
                </span>
                <input
                  type="checkbox"
                  checked={form.eodAllowStaleMarketData}
                  onChange={(event) => patch({ eodAllowStaleMarketData: event.target.checked })}
                />
              </label>
            </div>
          </div>
        )}
      </section>

      <section className="panel muted-panel">
        <CalendarClock size={18} />
        <span>
          Operational Control V1 is global. Holidays, per-market sessions and overnight trading windows are future slices.
        </span>
      </section>
    </AppShell>
  );
}

function MiniStatus({ label, value }: { label: string; value: string }) {
  return (
    <div className="mini-status">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function toForm(settings: OperationalControlSettings): UpdateOperationalControlRequest {
  return {
    timezone: settings.timezone,
    businessDays: settings.businessDays,
    tradingOpenTime: settings.tradingOpenTime,
    tradingCloseTime: settings.tradingCloseTime,
    blockTradeBookingsOutsideWindow: settings.blockTradeBookingsOutsideWindow,
    blockRiskRunsOutsideWindow: settings.blockRiskRunsOutsideWindow,
    eodEnabled: settings.eodEnabled,
    eodRunTime: settings.eodRunTime,
    eodAllowStaleMarketData: settings.eodAllowStaleMarketData,
  };
}
