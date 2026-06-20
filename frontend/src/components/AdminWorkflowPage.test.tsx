import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AdminWorkflowPage } from "./AdminWorkflowPage";

vi.mock("next/navigation", () => ({
  usePathname: () => "/workflows",
}));

describe("AdminWorkflowPage", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders workflow nodes and shows bookings for the selected state", async () => {
    const fetchMock = vi.fn(async (url: string) => {
      if (url === "/nexus-api/auth/me") {
        return json({
          enabled: true,
          authenticated: true,
          user: { id: "admin", username: "admin", displayName: "Admin User", groups: ["ADMIN"] },
          activeGroup: "ADMIN",
          csrfToken: "csrf-token",
        });
      }
      if (url === "/nexus-api/admin/portfolios") {
        return json([{ id: "portfolio-1", name: "Equity Book", baseCurrency: "USD", positionCount: 2 }]);
      }
      if (url === "/nexus-api/admin/workflow-map") {
        return json(workflowMap());
      }
      if (url === "/blemberg-api/actuator/health") {
        return json({ status: "UP" });
      }
      return json({ message: `Unhandled ${url}` }, 500);
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<AdminWorkflowPage />);

    expect(await screen.findByText("Trade booking lifecycle")).toBeInTheDocument();
    expect((await screen.findAllByText("Waiting BO")).length).toBeGreaterThan(0);
    expect(screen.getByText((_content, element) => element?.tagName === "TD" && element.textContent?.includes("AAPL"))).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: /Accepted/i }));
    expect(await screen.findByText("No bookings in this node.")).toBeInTheDocument();
  });
});

function workflowMap() {
  return {
    portfolioId: null,
    nodes: [
      { id: "SUBMITTED", label: "Booked", description: "All submitted", count: 1, bookings: [] },
      {
        id: "PENDING_VALIDATION",
        label: "Waiting BO",
        description: "Needs review",
        count: 1,
        bookings: [
          {
            id: "booking-1",
            portfolioId: "portfolio-1",
            portfolioName: "Equity Book",
            node: "PENDING_VALIDATION",
            status: "PENDING_VALIDATION",
            underlyingSymbol: "AAPL",
            optionType: "CALL",
            strike: 190,
            maturityDate: "2027-06-01",
            quantity: 10,
            submittedBy: "Raul User",
            submittedAt: "2026-06-01T00:00:00Z",
            reviewedBy: null,
            reviewedAt: null,
            rejectionReason: null,
            confirmedPositionId: null,
          },
        ],
      },
      { id: "CONFIRMED", label: "Accepted", description: "Approved", count: 0, bookings: [] },
      { id: "REJECTED", label: "Rejected", description: "Rejected", count: 0, bookings: [] },
    ],
    links: [],
  };
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
