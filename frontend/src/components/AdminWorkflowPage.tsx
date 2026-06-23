"use client";

import React from "react";
import {
  ArrowRight,
  CheckCircle2,
  ClipboardList,
  Clock3,
  GitBranch,
  Loader2,
  XCircle,
} from "lucide-react";
import { nexusApi, NexusApiError } from "@/lib/api";
import { formatNumber } from "@/lib/format";
import type {
  AdminPortfolioSummary,
  AdminWorkflowBooking,
  AdminWorkflowMap,
  AdminWorkflowNode,
} from "@/lib/types";
import { AppShell } from "./AppShell";

const howTo = [
  { title: "Booked", body: "Every FO submission enters the workflow as an immutable booking request." },
  { title: "Waiting BO", body: "Pending requests are visible to Back Office for maker-checker validation." },
  { title: "Accepted", body: "Approved requests become confirmed positions and can enter pricing, exposure and CVA." },
  { title: "Rejected", body: "Rejected requests remain as history, but never become confirmed positions." },
];

const nodeIcons = {
  SUBMITTED: ClipboardList,
  PENDING_VALIDATION: Clock3,
  CONFIRMED: CheckCircle2,
  REJECTED: XCircle,
  LIFECYCLE_REQUESTED: ClipboardList,
  LIFECYCLE_WAITING_BO: Clock3,
  LIFECYCLE_APPROVED: CheckCircle2,
  LIFECYCLE_REJECTED: XCircle,
} as const;

type WorkflowMode = "BOOKINGS" | "LIFECYCLE";

const workflowModes = {
  BOOKINGS: {
    title: "New trade bookings",
    description: "Follow FO submissions from capture through BO validation and confirmed position creation.",
    nodeIds: ["SUBMITTED", "PENDING_VALIDATION", "CONFIRMED", "REJECTED"],
    defaultNode: "PENDING_VALIDATION",
    kpis: [
      { id: "SUBMITTED", label: "Submitted", tone: "submitted" },
      { id: "PENDING_VALIDATION", label: "Waiting BO", tone: "pending_validation" },
      { id: "CONFIRMED", label: "Accepted", tone: "confirmed" },
      { id: "REJECTED", label: "Rejected", tone: "rejected" },
    ],
  },
  LIFECYCLE: {
    title: "Position lifecycle",
    description: "Inspect amendment and cancellation requests without mixing them with new trade bookings.",
    nodeIds: ["LIFECYCLE_REQUESTED", "LIFECYCLE_WAITING_BO", "LIFECYCLE_APPROVED", "LIFECYCLE_REJECTED"],
    defaultNode: "LIFECYCLE_WAITING_BO",
    kpis: [
      { id: "LIFECYCLE_REQUESTED", label: "Requested", tone: "submitted" },
      { id: "LIFECYCLE_WAITING_BO", label: "Waiting BO", tone: "pending_validation" },
      { id: "LIFECYCLE_APPROVED", label: "Approved", tone: "confirmed" },
      { id: "LIFECYCLE_REJECTED", label: "Rejected", tone: "rejected" },
    ],
  },
} as const;

export function AdminWorkflowPage() {
  const [portfolios, setPortfolios] = React.useState<AdminPortfolioSummary[]>([]);
  const [workflow, setWorkflow] = React.useState<AdminWorkflowMap | null>(null);
  const [workflowMode, setWorkflowMode] = React.useState<WorkflowMode>("BOOKINGS");
  const [selectedNode, setSelectedNode] = React.useState("PENDING_VALIDATION");
  const [portfolioId, setPortfolioId] = React.useState("");
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    void loadPortfolios();
  }, []);

  React.useEffect(() => {
    void loadWorkflow();
  }, [portfolioId]);

  async function loadPortfolios() {
    try {
      setPortfolios(await nexusApi.listAdminPortfolios());
    } catch (caught) {
      setError(errorMessage(caught));
    }
  }

  async function loadWorkflow() {
    setLoading(true);
    setError(null);
    try {
      const next = await nexusApi.getAdminWorkflowMap(portfolioId || undefined);
      setWorkflow(next);
      if (!next.nodes.some((node) => node.id === selectedNode)) {
        setSelectedNode(next.nodes[0]?.id ?? "PENDING_VALIDATION");
      }
    } catch (caught) {
      setError(errorMessage(caught));
    } finally {
      setLoading(false);
    }
  }

  const mode = workflowModes[workflowMode];
  const visibleNodes = workflow?.nodes.filter((node) => mode.nodeIds.includes(node.id as never)) ?? [];
  const visibleNode = visibleNodes.find((node) => node.id === selectedNode) ?? visibleNodes[0] ?? null;

  function selectWorkflowMode(nextMode: WorkflowMode) {
    setWorkflowMode(nextMode);
    setSelectedNode(workflowModes[nextMode].defaultNode);
  }

  return (
    <AppShell title="Workflows" eyebrow="Administration" howTo={howTo}>
      {error ? <div className="alert">{error}</div> : null}

      <section className="workflow-hero panel">
        <div>
          <span className="badge">Maker-checker flow</span>
          <h2>{mode.title}</h2>
          <p>{mode.description}</p>
        </div>
        <div className="workflow-filter">
          <label>
            Portfolio
            <select className="select" value={portfolioId} onChange={(event) => setPortfolioId(event.target.value)}>
              <option value="">All portfolios</option>
              {portfolios.map((portfolio) => (
                <option key={portfolio.id} value={portfolio.id}>{portfolio.name}</option>
              ))}
            </select>
          </label>
        </div>
      </section>

      <div className="workflow-mode-tabs" role="tablist" aria-label="Workflow process">
        <button
          className={workflowMode === "BOOKINGS" ? "active" : ""}
          type="button"
          role="tab"
          aria-selected={workflowMode === "BOOKINGS"}
          onClick={() => selectWorkflowMode("BOOKINGS")}
        >
          New trade bookings
        </button>
        <button
          className={workflowMode === "LIFECYCLE" ? "active" : ""}
          type="button"
          role="tab"
          aria-selected={workflowMode === "LIFECYCLE"}
          onClick={() => selectWorkflowMode("LIFECYCLE")}
        >
          Position lifecycle
        </button>
      </div>

      <div className="workflow-kpis">
        {mode.kpis.map((kpi) => (
          <WorkflowKpi
            key={kpi.id}
            label={kpi.label}
            value={workflow?.nodes.find((node) => node.id === kpi.id)?.count ?? 0}
            tone={kpi.tone}
          />
        ))}
      </div>

      <div className="admin-workflow-layout visual">
        <section className="panel workflow-map-panel visual">
          <div className="admin-card-head">
            <div>
              <h2>Workflow map</h2>
              <p>Click any node to inspect the real booking entries behind the count.</p>
            </div>
            <GitBranch size={20} />
          </div>
          {loading ? (
            <div className="empty"><Loader2 className="spin" size={18} /> Loading workflow</div>
          ) : visibleNodes.length > 0 ? (
            <VisualWorkflow nodes={visibleNodes} selectedNode={selectedNode} onSelect={setSelectedNode} />
          ) : (
            <div className="empty">No workflow data available.</div>
          )}
        </section>
        <section className="panel workflow-detail-panel">
          {visibleNode ? <WorkflowBookingList node={visibleNode} /> : <div className="empty">Select a node.</div>}
        </section>
      </div>
    </AppShell>
  );
}

function WorkflowKpi({ label, value, tone }: { label: string; value: number; tone: string }) {
  return (
    <div className={`workflow-kpi ${tone}`}>
      <span>{label}</span>
      <strong>{formatNumber(value, 0)}</strong>
    </div>
  );
}

function VisualWorkflow({
  nodes,
  selectedNode,
  onSelect,
}: {
  nodes: AdminWorkflowNode[];
  selectedNode: string;
  onSelect: (node: string) => void;
}) {
  return (
    <div className="workflow-visual-map">
      {nodes.map((node, index) => (
        <React.Fragment key={node.id}>
          <WorkflowNodeCard node={node} selected={selectedNode === node.id} onSelect={() => onSelect(node.id)} />
          {index < nodes.length - 1 ? (
            <div className="workflow-connector" aria-hidden="true">
              <ArrowRight size={18} />
            </div>
          ) : null}
        </React.Fragment>
      ))}
    </div>
  );
}

function WorkflowNodeCard({
  node,
  selected,
  onSelect,
}: {
  node: AdminWorkflowNode;
  selected: boolean;
  onSelect: () => void;
}) {
  const Icon = nodeIcons[node.id as keyof typeof nodeIcons] ?? GitBranch;
  return (
    <button className={`workflow-card ${selected ? "active" : ""} ${node.id.toLowerCase()}`} type="button" onClick={onSelect}>
      <span className="workflow-card-icon"><Icon size={20} /></span>
      <span className="workflow-card-label">{node.label}</span>
      <strong>{formatNumber(node.count, 0)}</strong>
      <small>{node.description}</small>
    </button>
  );
}

function WorkflowBookingList({ node }: { node: AdminWorkflowNode }) {
  return (
    <>
      <div className="admin-detail-head compact">
        <div>
          <span className="badge">{node.id.replaceAll("_", " ")}</span>
          <h2>{node.label}</h2>
          <p>{formatNumber(node.count, 0)} entries</p>
        </div>
        <CheckCircle2 size={22} />
      </div>
      {node.bookings.length === 0 ? (
        <div className="empty">No bookings in this node.</div>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Portfolio</th>
                <th>Trade</th>
                <th>Status</th>
                <th>Maker</th>
                <th>Reviewer</th>
              </tr>
            </thead>
            <tbody>
              {node.bookings.map((booking) => <WorkflowBookingRow booking={booking} key={booking.id} />)}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

function WorkflowBookingRow({ booking }: { booking: AdminWorkflowBooking }) {
  return (
    <tr>
      <td>{booking.portfolioName}</td>
      <td>
        {booking.optionType} {booking.underlyingSymbol}
        <br />
        <small>{formatNumber(booking.quantity, 2)} @ {formatNumber(booking.strike, 2)} · {booking.maturityDate}</small>
      </td>
      <td><span className={`booking-status ${booking.status.toLowerCase()}`}>{booking.status.replaceAll("_", " ")}</span></td>
      <td>{booking.submittedBy ?? "System"}<br /><small>{new Date(booking.submittedAt).toLocaleString()}</small></td>
      <td>{booking.reviewedBy ?? "—"}<br /><small>{booking.reviewedAt ? new Date(booking.reviewedAt).toLocaleString() : booking.rejectionReason ?? "Pending"}</small></td>
    </tr>
  );
}

function errorMessage(caught: unknown) {
  return caught instanceof NexusApiError || caught instanceof Error ? caught.message : "Unexpected workflow error";
}
