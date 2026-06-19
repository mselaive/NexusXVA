import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { Dashboard } from "./Dashboard";
import { UPadPage } from "./WorkflowPages";

describe("Dashboard", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("loads overview metrics and workflow navigation", async () => {
    mockFetch();

    render(<Dashboard />);

    expect(await screen.findByText("One workstation for the full NexusXVA flow.")).toBeInTheDocument();
    expect(screen.getAllByText("u-Pad").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Pricing").length).toBeGreaterThan(0);
    expect(screen.getByText("Local/Blemberg")).toBeInTheDocument();
  });

  it("books a trade from u-Pad", async () => {
    mockFetch();

    render(<UPadPage />);

    await waitFor(() => expect(screen.getAllByText("AAPL").length).toBeGreaterThan(0));
    await userEvent.click(screen.getByRole("button", { name: /Send to BO/i }));

    await waitFor(() => expect(screen.getByText("CALL AAPL sent for BO validation.")).toBeInTheDocument());
  });
});

function mockFetch() {
  const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
    if (url === "/nexus-api/auth/me" && !init?.method) {
      return json({
        enabled: true,
        authenticated: true,
        user: {
          id: "user-1",
          username: "fo-user",
          displayName: "FO User",
          groups: ["FO"],
        },
        activeGroup: "FO",
        csrfToken: "csrf-token",
      });
    }

    if (url === "/nexus-api/portfolios" && !init?.method) {
      return json([
        {
          id: "portfolio-1",
          name: "Demo Book",
          description: "Demo",
          baseCurrency: "USD",
          createdAt: "2026-06-01T00:00:00Z",
          updatedAt: "2026-06-01T00:00:00Z",
          positionCount: 1,
        },
      ]);
    }

    if (url === "/nexus-api/portfolios/portfolio-1" && !init?.method) {
      return json({
        id: "portfolio-1",
        name: "Demo Book",
        description: "Demo",
        baseCurrency: "USD",
        createdAt: "2026-06-01T00:00:00Z",
        updatedAt: "2026-06-01T00:00:00Z",
        positions: [
          {
            id: "position-1",
            portfolioId: "portfolio-1",
            underlyingSymbol: "AAPL",
            optionType: "CALL",
            strike: 190,
            maturityDate: "2027-06-01",
            quantity: 10,
            createdAt: "2026-06-01T00:00:00Z",
            updatedAt: "2026-06-01T00:00:00Z",
          },
        ],
      });
    }

    if (url === "/nexus-api/trade-bookings/mine" && !init?.method) {
      return json([]);
    }

    if (url === "/nexus-api/trading-limits/me" && !init?.method) {
      return json(tradingLimitFixture());
    }

    if (url.startsWith("/blemberg-api/api/market-data/snapshots") && !init?.method) {
      return json({ snapshots: [], missingSymbols: ["AAPL"] });
    }

    if (url === "/nexus-api/portfolios/portfolio-1/trade-bookings/european-options" && init?.method === "POST") {
      return json({
        id: "booking-1",
        portfolioId: "portfolio-1",
        portfolioName: "Demo Book",
        instrumentType: "EUROPEAN_OPTION",
        underlyingSymbol: "AAPL",
        optionType: "CALL",
        strike: 190,
        maturityDate: "2027-06-01",
        quantity: 10,
        status: "PENDING_VALIDATION",
        submittedBy: { userId: "user-1", username: "fo-user", displayName: "FO User" },
        submittedAt: "2026-06-01T00:00:00Z",
        reviewedBy: null,
        reviewedAt: null,
        rejectionReason: null,
        confirmedPositionId: null,
      });
    }

    if (url === "/nexus-api/portfolios/portfolio-1/pricing/black-scholes" && init?.method === "POST") {
      return json({
        portfolioId: "portfolio-1",
        valuationDate: "2026-06-05",
        model: "BLACK_SCHOLES",
        baseCurrency: "USD",
        totalPrice: 123.45,
        totalGreeks: { delta: 1.2, gamma: 0.03, vega: 4.5, theta: -0.7, rho: 2.1 },
        positions: [
          {
            positionId: "position-1",
            status: "PRICED",
            underlyingSymbol: "AAPL",
            quantity: 10,
            unitPrice: 12.345,
            positionPrice: 123.45,
            unitGreeks: { delta: 0.12, gamma: 0.003, vega: 0.45, theta: -0.07, rho: 0.21 },
            positionGreeks: { delta: 1.2, gamma: 0.03, vega: 4.5, theta: -0.7, rho: 2.1 },
            marketData: {
              spot: 190,
              volatility: 0.22,
              riskFreeRate: 0.04,
              dividendYield: 0,
              currency: "USD",
              asOf: "2026-06-05T12:00:00Z",
              source: "LOCAL",
              stale: false,
            },
          },
        ],
        unpriceablePositions: [],
      });
    }

    return json({ message: `Unhandled ${url}` }, 500);
  });

  vi.stubGlobal("fetch", fetchMock);
}

function tradingLimitFixture() {
  return {
    userId: "user-1",
    username: "fo-user",
    displayName: "FO User",
    status: "ACTIVE",
    policy: {
      maxTradesPerHour: 5,
      maxTradesPerDay: 20,
      maxNotionalPerHour: 10000,
      maxNotionalPerDay: 50000,
      notionalCurrency: "USD",
      active: true,
      createdAt: "2026-06-19T00:00:00Z",
      updatedAt: "2026-06-19T00:00:00Z",
      updatedByUserId: "user-1",
      updatedByUsername: "bo-user",
      updatedByDisplayName: "BO User",
      version: 0,
    },
    usage: {
      tradesThisHour: 1,
      tradesToday: 3,
      notionalThisHour: 1900,
      notionalToday: 5700,
      hourEndsAt: "2026-06-19T19:00:00Z",
      dayEndsAt: "2026-06-20T00:00:00Z",
    },
    remaining: {
      tradesThisHour: 4,
      tradesToday: 17,
      notionalThisHour: 8100,
      notionalToday: 44300,
    },
  };
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
