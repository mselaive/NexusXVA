"use client";

import React from "react";
import {
  Check,
  CheckCircle2,
  Clock3,
  Loader2,
  Search,
  ShieldCheck,
  X,
  XCircle,
} from "lucide-react";
import { nexusApi, NexusApiError } from "@/lib/api";
import { formatNumber } from "@/lib/format";
import type { TradeBooking } from "@/lib/types";
import { AppShell } from "./AppShell";

const howTo = [
  { title: "Pending", body: "Review the option terms submitted by Front Office. Pending entries are not yet part of the portfolio." },
  { title: "Approve", body: "Approval creates one immutable confirmed position. Pricing, exposure and CVA can then use it." },
  { title: "Reject", body: "Rejection requires a reason. Front Office can see that reason and submit a corrected booking." },
  { title: "History", body: "Confirmed and rejected requests remain available with maker, reviewer and timestamps." },
];

export function TradeValidationPage() {
  const [pending, setPending] = React.useState<TradeBooking[]>([]);
  const [confirmed, setConfirmed] = React.useState<TradeBooking[]>([]);
  const [rejected, setRejected] = React.useState<TradeBooking[]>([]);
  const [activeTab, setActiveTab] = React.useState<"pending" | "history">("pending");
  const [selected, setSelected] = React.useState<TradeBooking | null>(null);
  const [query, setQuery] = React.useState("");
  const [error, setError] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [actionId, setActionId] = React.useState<string | null>(null);
  const [rejecting, setRejecting] = React.useState<TradeBooking | null>(null);
  const [rejectionReason, setRejectionReason] = React.useState("");

  React.useEffect(() => {
    loadBookings();
  }, []);

  async function loadBookings(preferredId?: string) {
    setLoading(true);
    setError(null);
    try {
      const [pendingPage, confirmedPage, rejectedPage] = await Promise.all([
        nexusApi.listBackOfficeTradeBookings("PENDING_VALIDATION", 0, 100),
        nexusApi.listBackOfficeTradeBookings("CONFIRMED", 0, 100),
        nexusApi.listBackOfficeTradeBookings("REJECTED", 0, 100),
      ]);
      setPending(pendingPage.items);
      setConfirmed(confirmedPage.items);
      setRejected(rejectedPage.items);
      const all = [...pendingPage.items, ...confirmedPage.items, ...rejectedPage.items];
      setSelected(all.find((booking) => booking.id === preferredId) ?? pendingPage.items[0] ?? all[0] ?? null);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  async function approve(booking: TradeBooking) {
    setActionId(booking.id);
    setError(null);
    try {
      const reviewed = await nexusApi.approveTradeBooking(booking.id);
      await loadBookings(reviewed.id);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setActionId(null);
    }
  }

  async function reject() {
    if (!rejecting || !rejectionReason.trim()) {
      return;
    }
    setActionId(rejecting.id);
    setError(null);
    try {
      const reviewed = await nexusApi.rejectTradeBooking(rejecting.id, rejectionReason.trim());
      setRejecting(null);
      setRejectionReason("");
      await loadBookings(reviewed.id);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setActionId(null);
    }
  }

  const history = [...confirmed, ...rejected].sort((left, right) =>
    (right.reviewedAt ?? right.submittedAt).localeCompare(left.reviewedAt ?? left.submittedAt));
  const visibleRows = filterBookings(activeTab === "pending" ? pending : history, query);

  return (
    <AppShell title="Trade Validation" eyebrow="Back Office control" howTo={howTo}>
      {error ? <div className="alert">{error}</div> : null}

      <div className="summary-strip bo-summary">
        <Metric icon={<Clock3 size={18} />} label="Pending" value={pending.length} tone="pending" />
        <Metric icon={<CheckCircle2 size={18} />} label="Confirmed" value={confirmed.length} tone="confirmed" />
        <Metric icon={<XCircle size={18} />} label="Rejected" value={rejected.length} tone="rejected" />
      </div>

      <div className="bo-toolbar">
        <div className="segmented-control" aria-label="Trade validation view">
          <button className={activeTab === "pending" ? "active" : ""} type="button" onClick={() => setActiveTab("pending")}>
            Pending
          </button>
          <button className={activeTab === "history" ? "active" : ""} type="button" onClick={() => setActiveTab("history")}>
            History
          </button>
        </div>
        <label className="bo-search">
          <Search size={16} />
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search symbol, portfolio or maker" />
        </label>
      </div>

      <div className="bo-workspace">
        <section className="panel bo-queue">
          <div className="section-header">
            <div>
              <span className="page-eyebrow">{activeTab === "pending" ? "Oldest first" : "Most recently reviewed"}</span>
              <h2>{activeTab === "pending" ? "Validation queue" : "Review history"}</h2>
            </div>
            <span className="badge">{visibleRows.length} entries</span>
          </div>

          {loading ? (
            <div className="empty"><Loader2 className="spin" size={18} /> Loading bookings</div>
          ) : visibleRows.length === 0 ? (
            <div className="empty">No trade bookings match this view.</div>
          ) : (
            <div className="booking-list">
              {visibleRows.map((booking) => (
                <button
                  className={`booking-row ${selected?.id === booking.id ? "selected" : ""}`}
                  key={booking.id}
                  type="button"
                  onClick={() => setSelected(booking)}
                >
                  <span className={`booking-status ${booking.status.toLowerCase()}`}>{statusLabel(booking.status)}</span>
                  <strong>{booking.optionType} {booking.underlyingSymbol}</strong>
                  <span>{booking.portfolioName}</span>
                  <small>{formatNumber(booking.quantity, 2)} @ {formatNumber(booking.strike, 2)}</small>
                  <time>{new Date(booking.submittedAt).toLocaleString()}</time>
                </button>
              ))}
            </div>
          )}
        </section>

        <section className="panel bo-detail">
          {selected ? (
            <>
              <div className="bo-detail-head">
                <div>
                  <span className={`booking-status ${selected.status.toLowerCase()}`}>{statusLabel(selected.status)}</span>
                  <h2>{selected.optionType} {selected.underlyingSymbol}</h2>
                  <p>{selected.portfolioName}</p>
                </div>
                <ShieldCheck size={24} />
              </div>

              <div className="trade-terms-grid">
                <Detail label="Strike" value={formatNumber(selected.strike, 2)} />
                <Detail label="Quantity" value={formatNumber(selected.quantity, 2)} />
                <Detail label="Maturity" value={selected.maturityDate} />
                <Detail label="Instrument" value="European option" />
              </div>

              <div className="audit-timeline">
                <AuditPoint
                  title="Submitted by"
                  name={selected.submittedBy.displayName || selected.submittedBy.username}
                  date={selected.submittedAt}
                  complete
                />
                <AuditPoint
                  title={selected.status === "PENDING_VALIDATION" ? "Awaiting BO review" : `Reviewed by ${selected.reviewedBy?.displayName ?? selected.reviewedBy?.username ?? "BO"}`}
                  name={selected.status === "REJECTED" ? selected.rejectionReason ?? "Rejected" : selected.status === "CONFIRMED" ? "Position confirmed" : "No action taken"}
                  date={selected.reviewedAt}
                  complete={selected.status !== "PENDING_VALIDATION"}
                />
              </div>

              {selected.status === "PENDING_VALIDATION" ? (
                <div className="bo-actions">
                  <button className="btn secondary danger-action" type="button" onClick={() => setRejecting(selected)}>
                    <X size={16} /> Reject
                  </button>
                  <button className="btn" disabled={actionId === selected.id} type="button" onClick={() => approve(selected)}>
                    {actionId === selected.id ? <Loader2 className="spin" size={16} /> : <Check size={16} />}
                    Approve position
                  </button>
                </div>
              ) : null}
            </>
          ) : (
            <div className="empty">Select a booking to inspect its terms and audit trail.</div>
          )}
        </section>
      </div>

      {rejecting ? (
        <div className="modal-backdrop" role="presentation" onMouseDown={(event) => {
          if (event.target === event.currentTarget) {
            setRejecting(null);
            setRejectionReason("");
          }
        }}>
          <div className="modal-panel" role="dialog" aria-modal="true" aria-labelledby="reject-title">
            <div>
              <span className="page-eyebrow">Back Office decision</span>
              <h2 id="reject-title">Reject {rejecting.optionType} {rejecting.underlyingSymbol}</h2>
              <p>The reason will be visible to Front Office and retained in history.</p>
            </div>
            <label className="field full">
              <span>Rejection reason</span>
              <textarea
                className="input rejection-input"
                maxLength={500}
                value={rejectionReason}
                onChange={(event) => setRejectionReason(event.target.value)}
                placeholder="Describe what FO needs to correct"
              />
            </label>
            <div className="modal-actions">
              <button className="btn secondary" type="button" onClick={() => { setRejecting(null); setRejectionReason(""); }}>
                Cancel
              </button>
              <button className="btn danger-button" disabled={!rejectionReason.trim() || actionId === rejecting.id} type="button" onClick={reject}>
                {actionId === rejecting.id ? <Loader2 className="spin" size={16} /> : <XCircle size={16} />}
                Reject booking
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </AppShell>
  );
}

function filterBookings(bookings: TradeBooking[], query: string) {
  const normalized = query.trim().toLowerCase();
  if (!normalized) {
    return bookings;
  }
  return bookings.filter((booking) =>
    booking.underlyingSymbol.toLowerCase().includes(normalized)
    || booking.portfolioName.toLowerCase().includes(normalized)
    || booking.submittedBy.displayName.toLowerCase().includes(normalized)
    || booking.submittedBy.username.toLowerCase().includes(normalized));
}

function statusLabel(status: TradeBooking["status"]) {
  return status.replaceAll("_", " ");
}

function Metric({ icon, label, value, tone }: { icon: React.ReactNode; label: string; value: number; tone: string }) {
  return (
    <div className={`bo-metric ${tone}`}>
      <span>{icon}</span>
      <div>
        <small>{label}</small>
        <strong>{value}</strong>
      </div>
    </div>
  );
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function AuditPoint({ title, name, date, complete }: { title: string; name: string; date: string | null; complete: boolean }) {
  return (
    <div className={complete ? "complete" : ""}>
      <span />
      <div>
        <strong>{title}</strong>
        <p>{name}</p>
        <small>{date ? new Date(date).toLocaleString() : "Pending"}</small>
      </div>
    </div>
  );
}

function errorMessage(caught: unknown) {
  if (caught instanceof NexusApiError || caught instanceof Error) {
    return caught.message;
  }
  return "Unexpected trade validation error";
}
