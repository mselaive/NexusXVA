"use client";

import React from "react";
import { AlertTriangle, CheckCircle2, FileSearch, Loader2, RefreshCw, ShieldAlert } from "lucide-react";
import { nexusApi } from "@/lib/api";
import { formatNumber } from "@/lib/format";
import type { AuditEvent, AuditOutcome } from "@/lib/types";
import { AppShell } from "./AppShell";

const howTo = [
  {
    title: "Audit trail",
    body: "Audit events are stored in PostgreSQL and are meant to answer who did what, from which group, against which resource and with what outcome.",
  },
  {
    title: "Technical logs",
    body: "Backend log files are for debugging. User activity should be investigated here, not by parsing raw log files.",
  },
  {
    title: "Sensitive data",
    body: "Metadata is sanitized. Passwords, cookies, tokens, CSRF values and full request bodies are not stored in audit events.",
  },
];

export function AuditLogsPage() {
  const [events, setEvents] = React.useState<AuditEvent[]>([]);
  const [selected, setSelected] = React.useState<AuditEvent | null>(null);
  const [username, setUsername] = React.useState("");
  const [module, setModule] = React.useState("");
  const [outcome, setOutcome] = React.useState<AuditOutcome | "">("");
  const [resourceType, setResourceType] = React.useState("");
  const [resourceId, setResourceId] = React.useState("");
  const [from, setFrom] = React.useState("");
  const [to, setTo] = React.useState("");
  const [totalElements, setTotalElements] = React.useState(0);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);

  async function loadEvents() {
    setLoading(true);
    setError(null);
    try {
      const page = await nexusApi.listAuditEvents({
        username,
        module,
        outcome,
        resourceType,
        resourceId,
        from: from ? new Date(from).toISOString() : undefined,
        to: to ? new Date(to).toISOString() : undefined,
        size: 50,
      });
      setEvents(page.items);
      setTotalElements(page.totalElements);
      setSelected((current) => page.items.find((event) => event.id === current?.id) ?? page.items[0] ?? null);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Audit logs unavailable");
    } finally {
      setLoading(false);
    }
  }

  React.useEffect(() => {
    void loadEvents();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const successCount = events.filter((event) => event.outcome === "SUCCESS").length;
  const deniedCount = events.filter((event) => event.outcome === "DENIED").length;
  const failureCount = events.filter((event) => event.outcome === "FAILURE").length;

  return (
    <AppShell title="Audit Logs" eyebrow="Administration" howTo={howTo}>
      {error ? <div className="alert">{error}</div> : null}

      <div className="panel section">
        <div className="valuation-filter-head">
          <div>
            <span className="page-eyebrow">Filters</span>
            <h2>User activity</h2>
          </div>
          <button className="btn secondary" type="button" onClick={loadEvents} disabled={loading}>
            {loading ? <Loader2 size={16} /> : <RefreshCw size={16} />}
            Refresh
          </button>
        </div>

        <div className="toolbar valuation-filters">
          <label className="field compact-field">
            <span>Username</span>
            <input className="input" value={username} onChange={(event) => setUsername(event.target.value)} placeholder="admin" />
          </label>
          <label className="field compact-field">
            <span>Module</span>
            <input className="input" value={module} onChange={(event) => setModule(event.target.value)} placeholder="FRONT_OFFICE" />
          </label>
          <label className="field compact-field">
            <span>Outcome</span>
            <select className="input" value={outcome} onChange={(event) => setOutcome(event.target.value as AuditOutcome | "")}>
              <option value="">All</option>
              <option value="SUCCESS">Success</option>
              <option value="FAILURE">Failure</option>
              <option value="DENIED">Denied</option>
            </select>
          </label>
          <label className="field compact-field">
            <span>Resource type</span>
            <input className="input" value={resourceType} onChange={(event) => setResourceType(event.target.value)} placeholder="PORTFOLIO" />
          </label>
          <label className="field compact-field">
            <span>Resource id</span>
            <input className="input" value={resourceId} onChange={(event) => setResourceId(event.target.value)} placeholder="uuid" />
          </label>
          <label className="field compact-field">
            <span>From</span>
            <input className="input" type="datetime-local" value={from} onChange={(event) => setFrom(event.target.value)} />
          </label>
          <label className="field compact-field">
            <span>To</span>
            <input className="input" type="datetime-local" value={to} onChange={(event) => setTo(event.target.value)} />
          </label>
        </div>
      </div>

      <div className="summary-strip section">
        <MetricCard icon={<FileSearch size={17} />} label="Loaded" value={`${formatNumber(events.length, 0)} / ${formatNumber(totalElements, 0)}`} />
        <MetricCard icon={<CheckCircle2 size={17} />} label="Success" value={formatNumber(successCount, 0)} />
        <MetricCard icon={<ShieldAlert size={17} />} label="Denied" value={formatNumber(deniedCount, 0)} />
        <MetricCard icon={<AlertTriangle size={17} />} label="Failure" value={formatNumber(failureCount, 0)} />
      </div>

      <div className="valuation-runs-layout audit-logs-layout">
        <div className="panel">
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Time</th>
                  <th>User</th>
                  <th>Group</th>
                  <th>Module</th>
                  <th>Event</th>
                  <th>Outcome</th>
                  <th>Resource</th>
                </tr>
              </thead>
              <tbody>
                {events.map((event) => (
                  <tr className={selected?.id === event.id ? "selected-row" : ""} key={event.id} onClick={() => setSelected(event)}>
                    <td>{formatDateTime(event.occurredAt)}</td>
                    <td>{event.displayName ?? event.username ?? "System"}</td>
                    <td>{event.activeGroup ?? "-"}</td>
                    <td>{event.module}</td>
                    <td>{event.eventType}</td>
                    <td>
                      <span className={`status-chip ${outcomeClass(event.outcome)}`}>{event.outcome}</span>
                    </td>
                    <td>{event.resourceType ? `${event.resourceType}${event.resourceId ? ` · ${shortId(event.resourceId)}` : ""}` : "-"}</td>
                  </tr>
                ))}
                {events.length === 0 ? (
                  <tr>
                    <td colSpan={7}>No audit events match the current filters.</td>
                  </tr>
                ) : null}
              </tbody>
            </table>
          </div>
        </div>

        <div className="panel valuation-detail audit-detail">
          {selected ? (
            <>
              <div className="valuation-detail-head">
                <div>
                  <span className="page-eyebrow">Event detail</span>
                  <h2>{selected.eventType}</h2>
                </div>
                <span className={`status-chip ${outcomeClass(selected.outcome)}`}>{selected.outcome}</span>
              </div>
              <div className="detail-grid">
                <Detail label="Occurred" value={formatDateTime(selected.occurredAt)} />
                <Detail label="Action" value={selected.action} />
                <Detail label="User" value={selected.displayName ?? selected.username ?? "System"} />
                <Detail label="Group" value={selected.activeGroup ?? "-"} />
                <Detail label="Endpoint" value={`${selected.httpMethod ?? "-"} ${selected.path ?? "-"}`} />
                <Detail label="Status" value={selected.statusCode == null ? "-" : String(selected.statusCode)} />
                <Detail label="Correlation" value={selected.correlationId ?? "-"} />
                <Detail label="Session" value={selected.sessionId ? shortId(selected.sessionId) : "-"} />
              </div>
              {selected.message ? <p className="muted-text">{selected.message}</p> : null}
              <pre className="json-box">{formatMetadata(selected.metadata)}</pre>
            </>
          ) : (
            <p className="muted-text">Select an event to inspect its metadata.</p>
          )}
        </div>
      </div>
    </AppShell>
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

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="detail-item">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function outcomeClass(outcome: AuditOutcome) {
  if (outcome === "SUCCESS") return "success-chip";
  if (outcome === "DENIED") return "warning-chip";
  return "danger-chip";
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("en-US", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function formatMetadata(value: unknown) {
  if (value == null) {
    return "{}";
  }
  return JSON.stringify(value, null, 2);
}

function shortId(value: string) {
  return value.length <= 12 ? value : `${value.slice(0, 8)}...${value.slice(-4)}`;
}
