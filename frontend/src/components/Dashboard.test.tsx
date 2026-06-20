import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { Dashboard } from "./Dashboard";
import { FrontOfficeDeskPage } from "./FrontOfficeDeskPage";
import { FrontOfficeWhatIfPage } from "./FrontOfficeWhatIfPage";
import { StressTestingPage } from "./StressTestingPage";
import { UPadPage } from "./WorkflowPages";

describe("Dashboard", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("loads overview metrics and workflow navigation", async () => {
    mockFetch();

    render(<Dashboard />);

    expect(await screen.findByText("One workstation for the full NexusXVA flow.")).toBeInTheDocument();
    expect(screen.getAllByText("Pre-Trade Analysis").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Stress Testing").length).toBeGreaterThan(0);
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

  it("renders FO Desk counts, portfolio shortcuts and booking actions", async () => {
    mockFetch();

    render(<FrontOfficeDeskPage />);

    expect(await screen.findByText("Welcome, FO User")).toBeInTheDocument();
    expect(screen.getByText("Pending BO")).toBeInTheDocument();
    expect(screen.getAllByText("Demo Book").length).toBeGreaterThan(0);
    expect(screen.getByRole("link", { name: /Book in u-Pad/i })).toHaveAttribute("href", "/upad?portfolioId=portfolio-1");
    await userEvent.click(screen.getByRole("button", { name: "Rejected" }));
    expect(screen.getByText("Needs strike review")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Correct in u-Pad/i })).toHaveAttribute("href", "/upad?portfolioId=portfolio-1");
  });

  it("runs pre-trade analysis and can send the ticket to u-Pad", async () => {
    mockFetch();

    render(<FrontOfficeWhatIfPage />);

    await screen.findByText("Hypothetical trade");
    await userEvent.click(screen.getByRole("button", { name: /Run Analysis/i }));

    expect(await screen.findByText("Pre-trade impact")).toBeInTheDocument();
    expect(screen.getByText("Incremental impact")).toBeInTheDocument();
    expect(screen.getAllByText("With trade").length).toBeGreaterThan(0);
    expect(screen.getByRole("link", { name: /Send to u-Pad/i })).toHaveAttribute(
      "href",
      "/upad?source=pre-trade-analysis&portfolioId=portfolio-1&underlyingSymbol=AAPL&optionType=CALL&strike=190&maturityDate=2027-06-01&quantity=10",
    );
  });

  it("runs FO stress testing and displays scenario impacts", async () => {
    mockFetch();

    render(<StressTestingPage />);

    await screen.findByText("Scenario matrix");
    await userEvent.click(screen.getByRole("button", { name: /Run Stress Test/i }));

    expect(await screen.findByText("Worst price impact")).toBeInTheDocument();
    expect(screen.getAllByText("Spot -10%, Vol +500bp").length).toBeGreaterThan(0);
    expect(screen.getByText("Largest delta move")).toBeInTheDocument();
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

    if (url === "/nexus-api/front-office/desk" && !init?.method) {
      return json({
        user: { id: "user-1", username: "fo-user", displayName: "FO User" },
        bookingCounts: {
          pendingValidation: 1,
          confirmed: 1,
          rejected: 1,
          total: 3,
        },
        portfolios: [
          {
            id: "portfolio-1",
            name: "Demo Book",
            description: "Demo",
            baseCurrency: "USD",
            createdAt: "2026-06-01T00:00:00Z",
            updatedAt: "2026-06-01T00:00:00Z",
            positionCount: 1,
          },
        ],
        bookings: [
          {
            id: "booking-1",
            portfolioId: "portfolio-1",
            portfolioName: "Demo Book",
            portfolioVisible: true,
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
          },
          {
            id: "booking-2",
            portfolioId: "portfolio-1",
            portfolioName: "Demo Book",
            portfolioVisible: true,
            instrumentType: "EUROPEAN_OPTION",
            underlyingSymbol: "MSFT",
            optionType: "PUT",
            strike: 300,
            maturityDate: "2027-06-01",
            quantity: 4,
            status: "REJECTED",
            submittedBy: { userId: "user-1", username: "fo-user", displayName: "FO User" },
            submittedAt: "2026-06-01T00:00:00Z",
            reviewedBy: { userId: "bo-1", username: "bo-user", displayName: "BO User" },
            reviewedAt: "2026-06-01T01:00:00Z",
            rejectionReason: "Needs strike review",
            confirmedPositionId: null,
          },
        ],
      });
    }

    if (url === "/nexus-api/front-office/what-if/european-option" && init?.method === "POST") {
      return json({
        portfolioId: "portfolio-1",
        valuationDate: "2026-06-20",
        model: "BLACK_SCHOLES_PRE_TRADE_WHAT_IF_V1",
        basePortfolio: {
          totalPrice: 100,
          totalGreeks: { delta: 1, gamma: 0.1, vega: 2, theta: -0.3, rho: 0.5 },
        },
        hypotheticalTrade: {
          positionId: "hypo-1",
          status: "PRICED",
          underlyingSymbol: "AAPL",
          quantity: 10,
          unitPrice: 12,
          positionPrice: 120,
          unitGreeks: { delta: 0.2, gamma: 0.01, vega: 0.3, theta: -0.05, rho: 0.08 },
          positionGreeks: { delta: 2, gamma: 0.1, vega: 3, theta: -0.5, rho: 0.8 },
          marketData: {
            spot: 190,
            volatility: 0.22,
            riskFreeRate: 0.045,
            dividendYield: 0.005,
            currency: "USD",
            asOf: "2026-06-20T12:00:00Z",
            source: "LOCAL",
            stale: false,
          },
        },
        withTradePortfolio: {
          totalPrice: 220,
          totalGreeks: { delta: 3, gamma: 0.2, vega: 5, theta: -0.8, rho: 1.3 },
        },
        impact: { price: 120, delta: 2, gamma: 0.1, vega: 3, theta: -0.5, rho: 0.8 },
      });
    }

    if (url === "/nexus-api/front-office/stress-tests/european-options" && init?.method === "POST") {
      return json({
        portfolioId: "portfolio-1",
        valuationDate: "2026-06-20",
        model: "BLACK_SCHOLES_STRESS_TEST_V1",
        baseCurrency: "USD",
        basePortfolio: {
          totalPrice: 100,
          totalGreeks: { delta: 1, gamma: 0.1, vega: 2, theta: -0.3, rho: 0.5 },
        },
        hypotheticalTrade: null,
        scenarios: [
          {
            scenario: {
              name: "Base",
              spotShockPercent: 0,
              volatilityShockBps: 0,
              riskFreeRateShockBps: 0,
              dividendYieldShockBps: 0,
            },
            totals: {
              totalPrice: 100,
              totalGreeks: { delta: 1, gamma: 0.1, vega: 2, theta: -0.3, rho: 0.5 },
            },
            impact: { price: 0, delta: 0, gamma: 0, vega: 0, theta: 0, rho: 0 },
            positions: [],
          },
          {
            scenario: {
              name: "Spot -10%, Vol +500bp",
              spotShockPercent: -0.1,
              volatilityShockBps: 500,
              riskFreeRateShockBps: 0,
              dividendYieldShockBps: 0,
            },
            totals: {
              totalPrice: 82,
              totalGreeks: { delta: 0.7, gamma: 0.12, vega: 3, theta: -0.5, rho: 0.3 },
            },
            impact: { price: -18, delta: -0.3, gamma: 0.02, vega: 1, theta: -0.2, rho: -0.2 },
            positions: [],
          },
        ],
        unpriceablePositions: [],
      });
    }

    if (url === "/nexus-api/trading-limits/me" && !init?.method) {
      return json(tradingLimitFixture());
    }

    if (url.startsWith("/blemberg-api/api/market-data/snapshots") && !init?.method) {
      return json({
        snapshots: [
          {
            symbol: "AAPL",
            lastPrice: 190,
            previousClose: 188,
            volume: 1000000,
            currency: "USD",
            asOf: "2026-06-20T12:00:00Z",
            source: "BLEMBERG",
          },
        ],
        missingSymbols: ["MSFT"],
      });
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
