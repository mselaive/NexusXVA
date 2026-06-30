"use client";

import React from "react";
import {
  AlertCircle,
  CheckCircle2,
  CircleDollarSign,
  Clock3,
  FlaskConical,
  LineChart,
  RefreshCw,
  Send,
  Shield,
  SquarePen,
  Wallet,
  Waves,
  XCircle,
} from "lucide-react";
import { nexusApi } from "@/lib/api";
import { formatNumber } from "@/lib/format";
import type {
  FrontOfficeDeskBooking,
  FrontOfficeDeskResponse,
  PortfolioSummary,
  TradeBookingStatus,
  TradeLifecycleReport,
} from "@/lib/types";
import { AppShell } from "./AppShell";

const howTo = [
  { title: "What this is", body: "FO Desk is the Front Office cockpit. It shows your bookings across portfolios and the books you can work on." },
  { title: "Pending", body: "Pending bookings are waiting for BO validation. They do not affect pricing, exposure or CVA yet." },
  { title: "Rejected", body: "Rejected bookings keep the BO reason so FO can correct the trade in u-Pad." },
  { title: "Confirmed", body: "Confirmed bookings created portfolio positions and can be used by Pricing, Exposure and CVA." },
];

const tabs: Array<{ id: "ALL" | TradeBookingStatus; label: string }> = [
  { id: "PENDING_VALIDATION", label: "Pending" },
  { id: "REJECTED", label: "Rejected" },
  { id: "CONFIRMED", label: "Confirmed" },
  { id: "ALL", label: "All" },
];

export function FrontOfficeDeskPage() {
  const [desk, setDesk] = React.useState<FrontOfficeDeskResponse | null>(null);
  const [lifecycleReport, setLifecycleReport] = React.useState<TradeLifecycleReport | null>(null);
  const [activeTab, setActiveTab] = React.useState<"ALL" | TradeBookingStatus>("PENDING_VALIDATION");
  const [error, setError] = React.useState<string | null>(null);
  const [loading, setLoading] = React.useState(true);

  React.useEffect(() => {
    void loadDesk();
  }, []);

  async function loadDesk() {
    setLoading(true);
    setError(null);
    try {
      const [nextDesk, nextLifecycleReport] = await Promise.all([
        nexusApi.getFrontOfficeDesk(),
        nexusApi.getMyLifecycleReport(),
      ]);
      setDesk(nextDesk);
      setLifecycleReport(nextLifecycleReport);
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Could not load FO Desk");
    } finally {
      setLoading(false);
    }
  }

  const bookings = desk?.bookings ?? [];
  const filteredBookings = activeTab === "ALL"
    ? bookings
    : bookings.filter((booking) => booking.status === activeTab);

  return (
    <AppShell title="FO Desk" eyebrow="Front Office cockpit" howTo={howTo}>
      {error ? <div className="alert">{error}</div> : null}
      <section className="fo-hero">
        <div>
          <span>My workstation</span>
          <h2>{desk ? `Welcome, ${desk.user.displayName}` : "Loading Front Office desk"}</h2>
          <p>Track submitted bookings, jump into trade capture, and run risk workflows only after BO has confirmed positions.</p>
        </div>
        <button className="btn secondary" type="button" onClick={loadDesk} disabled={loading}>
          <RefreshCw size={16} />
          Refresh
        </button>
      </section>

      <div className="fo-kpis">
        <DeskMetric icon={<Clock3 size={18} />} label="Pending BO" value={desk?.bookingCounts.pendingValidation ?? 0} tone="pending" />
        <DeskMetric icon={<XCircle size={18} />} label="Rejected" value={desk?.bookingCounts.rejected ?? 0} tone="rejected" />
        <DeskMetric icon={<CheckCircle2 size={18} />} label="Confirmed" value={desk?.bookingCounts.confirmed ?? 0} tone="confirmed" />
        <DeskMetric icon={<Wallet size={18} />} label="Visible portfolios" value={desk?.portfolios.length ?? 0} tone="portfolio" />
      </div>

      <section className="panel section fo-lifecycle-summary">
        <SectionHeader
          title="My lifecycle activity"
          text="Amendment and cancellation requests submitted by you. Pending lifecycle requests do not change confirmed positions until BO approves them."
        />
        <div className="fo-lifecycle-grid">
          <DeskMetric icon={<Clock3 size={18} />} label="Lifecycle pending" value={lifecycleReport?.pendingValidation ?? 0} tone="pending" />
          <DeskMetric icon={<CheckCircle2 size={18} />} label="Lifecycle approved" value={lifecycleReport?.approved ?? 0} tone="confirmed" />
          <DeskMetric icon={<XCircle size={18} />} label="Lifecycle rejected" value={lifecycleReport?.rejected ?? 0} tone="rejected" />
          <div className="fo-lifecycle-note">
            <span>Oldest pending</span>
            <strong>{lifecycleReport?.oldestPendingSubmittedAt ? new Date(lifecycleReport.oldestPendingSubmittedAt).toLocaleString() : "No pending lifecycle"}</strong>
            <small>{formatNumber(lifecycleReport?.amendments ?? 0, 0)} amendments · {formatNumber(lifecycleReport?.cancellations ?? 0, 0)} cancellations</small>
          </div>
        </div>
      </section>

      <div className="fo-desk-layout">
        <section className="panel fo-portfolio-panel">
          <SectionHeader
            title="Portfolio shortcuts"
            text="Use visible portfolios as launch points for trade capture and risk workflows."
          />
          {loading && !desk ? <Empty text="Loading portfolios..." /> : null}
          {desk && desk.portfolios.length === 0 ? <Empty text="No visible portfolios for this FO user." /> : null}
          <div className="fo-portfolio-grid">
            {(desk?.portfolios ?? []).map((portfolio) => (
              <PortfolioShortcut key={portfolio.id} portfolio={portfolio} />
            ))}
          </div>
        </section>

        <section className="panel fo-blotter-panel">
          <SectionHeader
            title="My booking blotter"
            text="Your immutable booking requests across visible and historical portfolios."
          />
          <div className="fo-tabs" role="tablist" aria-label="Booking status filters">
            {tabs.map((tab) => (
              <button
                className={activeTab === tab.id ? "active" : ""}
                key={tab.id}
                type="button"
                onClick={() => setActiveTab(tab.id)}
              >
                {tab.label}
              </button>
            ))}
          </div>
          <BookingBlotter bookings={filteredBookings} />
        </section>
      </div>
    </AppShell>
  );
}

function DeskMetric({
  icon,
  label,
  value,
  tone,
}: {
  icon: React.ReactNode;
  label: string;
  value: number;
  tone: string;
}) {
  return (
    <div className={`fo-metric ${tone}`}>
      <span>{icon}</span>
      <small>{label}</small>
      <strong>{formatNumber(value, 0)}</strong>
    </div>
  );
}

function PortfolioShortcut({ portfolio }: { portfolio: PortfolioSummary }) {
  return (
    <article className="fo-portfolio-card">
      <div>
        <span className="badge">{portfolio.baseCurrency}</span>
        <h3>{portfolio.name}</h3>
        <p>{portfolio.description || "Portfolio ready for FO workflows."}</p>
      </div>
      <div className="fo-portfolio-meta">
        <span>{portfolio.positionCount} confirmed positions</span>
        <span>Updated {new Date(portfolio.updatedAt).toLocaleDateString()}</span>
      </div>
      <div className="fo-action-grid">
        <a className="btn secondary" href={`/upad?portfolioId=${portfolio.id}`}>
          <SquarePen size={15} />
          Book in u-Pad
        </a>
        <a className="icon-link" href={`/pre-trade-analysis?portfolioId=${portfolio.id}`} title="Run Pre-Trade Analysis">
          <FlaskConical size={16} />
        </a>
        <a className="icon-link" href={`/stress-testing?portfolioId=${portfolio.id}`} title="Run Stress Testing">
          <Waves size={16} />
        </a>
        <a className="icon-link" href={`/pricing?portfolioId=${portfolio.id}`} title="Run Pricing">
          <CircleDollarSign size={16} />
        </a>
        <a className="icon-link" href={`/exposure?portfolioId=${portfolio.id}`} title="Run Exposure">
          <LineChart size={16} />
        </a>
        <a className="icon-link" href={`/cva?portfolioId=${portfolio.id}`} title="Run CVA">
          <Shield size={16} />
        </a>
      </div>
    </article>
  );
}

function BookingBlotter({ bookings }: { bookings: FrontOfficeDeskBooking[] }) {
  if (bookings.length === 0) {
    return <Empty text="No bookings in this status." />;
  }

  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Status</th>
            <th>Portfolio</th>
            <th>Symbol</th>
            <th>Type</th>
            <th>Strike</th>
            <th>Maturity</th>
            <th>Qty</th>
            <th>Execution premium</th>
            <th>Submitted</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          {bookings.map((booking) => (
            <tr key={booking.id}>
              <td><StatusPill status={booking.status} /></td>
              <td>
                <div className="fo-booking-portfolio">
                  <strong>{booking.portfolioName}</strong>
                  {!booking.portfolioVisible ? <small>Historical only</small> : null}
                </div>
              </td>
              <td>{booking.underlyingSymbol}</td>
              <td>{booking.bookingType === "CASH_EQUITY" ? "Cash equity" : booking.bookingType === "OPTION_STRATEGY" ? booking.strategyName ?? booking.strategyType?.replaceAll("_", " ") : booking.optionType}</td>
              <td>{booking.strike == null ? "—" : formatNumber(booking.strike, 2)}</td>
              <td>{booking.maturityDate ?? "—"}</td>
              <td>{formatNumber(booking.quantity, 2)}</td>
              <td>{booking.executionPrice == null ? "Unavailable" : formatNumber(booking.executionPrice, 4)}</td>
              <td>{new Date(booking.submittedAt).toLocaleString()}</td>
              <td>
                <BookingAction booking={booking} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function BookingAction({ booking }: { booking: FrontOfficeDeskBooking }) {
  if (booking.status === "REJECTED") {
    return (
      <div className="fo-booking-action rejected">
        <span>{booking.rejectionReason ?? "Rejected by BO"}</span>
        {booking.portfolioVisible && booking.portfolioId ? (
          <a href={`/upad?portfolioId=${booking.portfolioId}`}>Correct in u-Pad</a>
        ) : null}
      </div>
    );
  }

  if (booking.status === "PENDING_VALIDATION") {
    return (
      <div className="fo-booking-action pending">
        <AlertCircle size={14} />
        <span>Excluded from risk runs</span>
      </div>
    );
  }

  return (
    <div className="fo-booking-action confirmed">
      <CheckCircle2 size={14} />
      {booking.portfolioVisible && booking.portfolioId ? (
        <a href={`/portfolios?portfolioId=${booking.portfolioId}`}>
          Confirmed position
        </a>
      ) : (
        <span>Confirmed</span>
      )}
    </div>
  );
}

function StatusPill({ status }: { status: TradeBookingStatus }) {
  return <span className={`booking-status ${status.toLowerCase()}`}>{status.replaceAll("_", " ")}</span>;
}

function SectionHeader({ title, text }: { title: string; text: string }) {
  return (
    <div className="section-title">
      <div>
        <h2>{title}</h2>
        <p>{text}</p>
      </div>
    </div>
  );
}

function Empty({ text }: { text: string }) {
  return (
    <div className="empty-state">
      <Send size={18} />
      <span>{text}</span>
    </div>
  );
}
