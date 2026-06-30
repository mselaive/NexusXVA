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
import type { TradeBooking, TradeLifecycleRequest } from "@/lib/types";
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
  const [lifecyclePending, setLifecyclePending] = React.useState<TradeLifecycleRequest[]>([]);
  const [lifecycleHistory, setLifecycleHistory] = React.useState<TradeLifecycleRequest[]>([]);
  const [queueMode, setQueueMode] = React.useState<"trades" | "lifecycle">("trades");
  const [activeTab, setActiveTab] = React.useState<"pending" | "history">("pending");
  const [selected, setSelected] = React.useState<TradeBooking | null>(null);
  const [selectedLifecycle, setSelectedLifecycle] = React.useState<TradeLifecycleRequest | null>(null);
  const [query, setQuery] = React.useState("");
  const [error, setError] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState(true);
  const [actionId, setActionId] = React.useState<string | null>(null);
  const [rejecting, setRejecting] = React.useState<TradeBooking | null>(null);
  const [rejectingLifecycle, setRejectingLifecycle] = React.useState<TradeLifecycleRequest | null>(null);
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
      const [lifecyclePendingPage, lifecycleApprovedPage, lifecycleRejectedPage] = await Promise.all([
        nexusApi.listBackOfficeLifecycleRequests("PENDING_VALIDATION", 0, 100),
        nexusApi.listBackOfficeLifecycleRequests("APPROVED", 0, 100),
        nexusApi.listBackOfficeLifecycleRequests("REJECTED", 0, 100),
      ]);
      setPending(pendingPage.items);
      setConfirmed(confirmedPage.items);
      setRejected(rejectedPage.items);
      setLifecyclePending(lifecyclePendingPage.items);
      const nextLifecycleHistory = [...lifecycleApprovedPage.items, ...lifecycleRejectedPage.items].sort((left, right) =>
        (right.reviewedAt ?? right.submittedAt).localeCompare(left.reviewedAt ?? left.submittedAt));
      setLifecycleHistory(nextLifecycleHistory);
      const all = [...pendingPage.items, ...confirmedPage.items, ...rejectedPage.items];
      setSelected(all.find((booking) => booking.id === preferredId) ?? pendingPage.items[0] ?? all[0] ?? null);
      const allLifecycle = [...lifecyclePendingPage.items, ...nextLifecycleHistory];
      setSelectedLifecycle(allLifecycle.find((request) => request.id === preferredId) ?? lifecyclePendingPage.items[0] ?? allLifecycle[0] ?? null);
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

  async function approveLifecycle(request: TradeLifecycleRequest) {
    setActionId(request.id);
    setError(null);
    try {
      const reviewed = await nexusApi.approveLifecycleRequest(request.id);
      await loadBookings(reviewed.id);
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setActionId(null);
    }
  }

  async function rejectLifecycle() {
    if (!rejectingLifecycle || !rejectionReason.trim()) {
      return;
    }
    setActionId(rejectingLifecycle.id);
    setError(null);
    try {
      const reviewed = await nexusApi.rejectLifecycleRequest(rejectingLifecycle.id, rejectionReason.trim());
      setRejectingLifecycle(null);
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
        <Metric icon={<Clock3 size={18} />} label="Lifecycle pending" value={lifecyclePending.length} tone="pending" />
      </div>

      <div className="bo-toolbar">
        <div className="segmented-control" aria-label="Validation queue type">
          <button className={queueMode === "trades" ? "active" : ""} type="button" onClick={() => setQueueMode("trades")}>
            New trades
          </button>
          <button className={queueMode === "lifecycle" ? "active" : ""} type="button" onClick={() => setQueueMode("lifecycle")}>
            Lifecycle
          </button>
        </div>
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

      {queueMode === "lifecycle" ? (
        <LifecycleValidationView
          pending={lifecyclePending}
          history={lifecycleHistory}
          activeTab={activeTab}
          selected={selectedLifecycle}
          query={query}
          loading={loading}
          actionId={actionId}
          onSelect={setSelectedLifecycle}
          onApprove={approveLifecycle}
          onReject={setRejectingLifecycle}
        />
      ) : (
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
                  <strong>{bookingTitle(booking)}</strong>
                  <span>{booking.portfolioName}</span>
                  <small>{bookingSummary(booking)}</small>
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
                  <h2>{bookingTitle(selected)}</h2>
                  <p>{selected.portfolioName}</p>
                </div>
                <ShieldCheck size={24} />
              </div>

              <div className="trade-terms-grid">
                <Detail label="Booking type" value={bookingTypeLabel(selected)} />
                {selected.strategyType ? <Detail label="Strategy" value={selected.strategyName || selected.strategyType.replaceAll("_", " ")} /> : null}
                <Detail label="Strike" value={selected.strike == null ? "—" : formatNumber(selected.strike, 2)} />
                <Detail label="Quantity" value={formatNumber(selected.quantity, 2)} />
                <Detail label="Execution premium" value={selected.executionPrice == null ? "Unavailable" : formatNumber(selected.executionPrice, 4)} />
                <Detail label="Maturity" value={selected.maturityDate ?? "—"} />
                <Detail label="Instrument" value={selected.instrumentType === "CASH_EQUITY" ? "Cash equity" : "European option"} />
                <Detail label="Limit notional" value={selected.bookingNotional == null ? "Unavailable" : formatNumber(selected.bookingNotional, 2)} />
              </div>

              {selected.legs.length > 0 ? <BookingLegTable booking={selected} /> : null}

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
      )}

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
                  <h2 id="reject-title">Reject {bookingTitle(rejecting)}</h2>
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
      {rejectingLifecycle ? (
        <RejectLifecycleModal
          request={rejectingLifecycle}
          rejectionReason={rejectionReason}
          actionId={actionId}
          onReasonChange={setRejectionReason}
          onClose={() => { setRejectingLifecycle(null); setRejectionReason(""); }}
          onReject={rejectLifecycle}
        />
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

function BookingLegTable({ booking }: { booking: TradeBooking }) {
  return (
    <div className="table-wrap compact-table">
      <table>
        <thead>
          <tr>
            <th>Leg</th>
            <th>Type</th>
            <th>Strike</th>
            <th>Maturity</th>
            <th>Quantity</th>
            <th>Execution premium</th>
          </tr>
        </thead>
        <tbody>
          {booking.legs.map((leg) => (
            <tr key={leg.legIndex}>
              <td>{leg.legIndex + 1}</td>
              <td>{leg.optionType}</td>
              <td>{formatNumber(leg.strike, 2)}</td>
              <td>{leg.maturityDate}</td>
              <td>{formatNumber(leg.quantity, 2)}</td>
              <td>{leg.executionPrice == null ? "Unavailable" : formatNumber(leg.executionPrice, 4)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function LifecycleValidationView({
  pending,
  history,
  activeTab,
  selected,
  query,
  loading,
  actionId,
  onSelect,
  onApprove,
  onReject,
}: {
  pending: TradeLifecycleRequest[];
  history: TradeLifecycleRequest[];
  activeTab: "pending" | "history";
  selected: TradeLifecycleRequest | null;
  query: string;
  loading: boolean;
  actionId: string | null;
  onSelect: (request: TradeLifecycleRequest) => void;
  onApprove: (request: TradeLifecycleRequest) => void;
  onReject: (request: TradeLifecycleRequest) => void;
}) {
  const visibleRows = filterLifecycleRequests(activeTab === "pending" ? pending : history, query);
  return (
    <div className="bo-workspace">
      <section className="panel bo-queue">
        <div className="section-header">
          <div>
            <span className="page-eyebrow">{activeTab === "pending" ? "Oldest first" : "Most recently reviewed"}</span>
            <h2>{activeTab === "pending" ? "Lifecycle queue" : "Lifecycle history"}</h2>
          </div>
          <span className="badge">{visibleRows.length} entries</span>
        </div>
        {loading ? (
          <div className="empty"><Loader2 className="spin" size={18} /> Loading lifecycle requests</div>
        ) : visibleRows.length === 0 ? (
          <div className="empty">No lifecycle requests match this view.</div>
        ) : (
          <div className="booking-list">
            {visibleRows.map((request) => (
              <button
                className={`booking-row ${selected?.id === request.id ? "selected" : ""}`}
                key={request.id}
                type="button"
                onClick={() => onSelect(request)}
              >
                <span className={`booking-status ${request.status.toLowerCase()}`}>{request.status.replaceAll("_", " ")}</span>
                <strong>{request.requestType} {request.originalUnderlyingSymbol}</strong>
                <span>{request.portfolioName}</span>
                <small>{request.originalOptionType} {formatNumber(request.originalQuantity, 2)} @ {formatNumber(request.originalStrike, 2)}</small>
                <time>{new Date(request.submittedAt).toLocaleString()}</time>
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
                <span className={`booking-status ${selected.status.toLowerCase()}`}>{selected.status.replaceAll("_", " ")}</span>
                <h2>{selected.requestType} {selected.originalUnderlyingSymbol}</h2>
                <p>{selected.portfolioName}</p>
              </div>
              <ShieldCheck size={24} />
            </div>
            <div className="trade-terms-grid">
              <Detail label="Original" value={`${selected.originalOptionType} ${formatNumber(selected.originalQuantity, 2)} @ ${formatNumber(selected.originalStrike, 2)}`} />
              <Detail label="Original maturity" value={selected.originalMaturityDate} />
              <Detail label="Requested" value={selected.requestType === "AMEND" ? `${selected.requestedOptionType} ${selected.requestedUnderlyingSymbol} ${formatNumber(selected.requestedQuantity ?? 0, 2)} @ ${formatNumber(selected.requestedStrike ?? 0, 2)}` : "Full cancellation"} />
              <Detail label="Requested maturity" value={selected.requestedMaturityDate ?? "Cancel position"} />
            </div>
            <div className="audit-timeline">
              <AuditPoint title="Submitted by" name={selected.submittedBy.displayName || selected.submittedBy.username} date={selected.submittedAt} complete />
              <AuditPoint
                title={selected.status === "PENDING_VALIDATION" ? "Awaiting BO review" : `Reviewed by ${selected.reviewedBy?.displayName ?? selected.reviewedBy?.username ?? "BO"}`}
                name={selected.status === "REJECTED" ? selected.rejectionReason ?? "Rejected" : selected.status === "APPROVED" ? "Lifecycle approved" : "No action taken"}
                date={selected.reviewedAt}
                complete={selected.status !== "PENDING_VALIDATION"}
              />
            </div>
            {selected.status === "PENDING_VALIDATION" ? (
              <div className="bo-actions">
                <button className="btn secondary danger-action" type="button" onClick={() => onReject(selected)}>
                  <X size={16} /> Reject
                </button>
                <button className="btn" disabled={actionId === selected.id} type="button" onClick={() => onApprove(selected)}>
                  {actionId === selected.id ? <Loader2 className="spin" size={16} /> : <Check size={16} />}
                  Approve lifecycle
                </button>
              </div>
            ) : null}
          </>
        ) : (
          <div className="empty">Select a lifecycle request to inspect its terms and audit trail.</div>
        )}
      </section>
    </div>
  );
}

function filterLifecycleRequests(requests: TradeLifecycleRequest[], query: string) {
  const normalized = query.trim().toLowerCase();
  if (!normalized) {
    return requests;
  }
  return requests.filter((request) =>
    request.originalUnderlyingSymbol.toLowerCase().includes(normalized)
    || (request.requestedUnderlyingSymbol ?? "").toLowerCase().includes(normalized)
    || request.portfolioName.toLowerCase().includes(normalized)
    || request.submittedBy.displayName.toLowerCase().includes(normalized)
    || request.submittedBy.username.toLowerCase().includes(normalized));
}

function bookingTitle(booking: TradeBooking) {
  if (booking.bookingType === "OPTION_STRATEGY") {
    return `${booking.strategyName || booking.strategyType?.replaceAll("_", " ") || "Strategy"} ${booking.underlyingSymbol}`;
  }
  if (booking.bookingType === "CASH_EQUITY") {
    return `Cash equity ${booking.underlyingSymbol}`;
  }
  return `${booking.optionType} ${booking.underlyingSymbol}`;
}

function bookingSummary(booking: TradeBooking) {
  if (booking.bookingType === "OPTION_STRATEGY") {
    return `${booking.legs.length} legs · notional ${formatNumber(booking.bookingNotional ?? 0, 2)}`;
  }
  if (booking.bookingType === "CASH_EQUITY") {
    return `${formatNumber(booking.quantity, 2)} shares${booking.executionPrice == null ? "" : ` @ ${formatNumber(booking.executionPrice, 2)}`}`;
  }
  return `${formatNumber(booking.quantity, 2)} @ ${booking.strike == null ? "—" : formatNumber(booking.strike, 2)}`;
}

function bookingTypeLabel(booking: TradeBooking) {
  if (booking.bookingType === "OPTION_STRATEGY") {
    return "Option strategy";
  }
  if (booking.bookingType === "CASH_EQUITY") {
    return "Cash equity";
  }
  return "Single option";
}

function RejectLifecycleModal({
  request,
  rejectionReason,
  actionId,
  onReasonChange,
  onClose,
  onReject,
}: {
  request: TradeLifecycleRequest;
  rejectionReason: string;
  actionId: string | null;
  onReasonChange: (reason: string) => void;
  onClose: () => void;
  onReject: () => void;
}) {
  return (
    <div className="modal-backdrop" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) {
        onClose();
      }
    }}>
      <div className="modal-panel" role="dialog" aria-modal="true" aria-labelledby="reject-lifecycle-title">
        <div>
          <span className="page-eyebrow">Back Office decision</span>
          <h2 id="reject-lifecycle-title">Reject {request.requestType} {request.originalUnderlyingSymbol}</h2>
          <p>The reason will be visible to Front Office and retained in lifecycle history.</p>
        </div>
        <label className="field full">
          <span>Rejection reason</span>
          <textarea
            className="input rejection-input"
            maxLength={500}
            value={rejectionReason}
            onChange={(event) => onReasonChange(event.target.value)}
            placeholder="Describe what FO needs to correct"
          />
        </label>
        <div className="modal-actions">
          <button className="btn secondary" type="button" onClick={onClose}>Cancel</button>
          <button className="btn danger-button" disabled={!rejectionReason.trim() || actionId === request.id} type="button" onClick={onReject}>
            {actionId === request.id ? <Loader2 className="spin" size={16} /> : <XCircle size={16} />}
            Reject request
          </button>
        </div>
      </div>
    </div>
  );
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
