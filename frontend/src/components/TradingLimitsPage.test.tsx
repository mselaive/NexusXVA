import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { TradingLimitsPage } from "./TradingLimitsPage";

vi.mock("next/navigation", () => ({
  usePathname: () => "/trading-limits",
}));

describe("TradingLimitsPage", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders FO usage and saves an active policy", async () => {
    const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      if (url === "/nexus-api/auth/me" && !init?.method) {
        return json({
          enabled: true,
          authenticated: true,
          user: {
            id: "bo-user",
            username: "bo-user",
            displayName: "BO User",
            groups: ["BO"],
          },
          activeGroup: "BO",
          csrfToken: "csrf-token",
        });
      }
      if (url.startsWith("/nexus-api/back-office/trading-limits/users?") && !init?.method) {
        return json({ items: [limitFixture()], page: 0, size: 100, totalElements: 1, totalPages: 1 });
      }
      if (url === "/nexus-api/back-office/trading-limits/users/fo-user" && init?.method === "PUT") {
        return json({ ...limitFixture(), policy: { ...limitFixture().policy, maxTradesPerHour: 8, version: 2 } });
      }
      if (url === "/blemberg-api/actuator/health") {
        return json({ status: "UP" });
      }
      return json({ message: `Unhandled ${url}` }, 500);
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<TradingLimitsPage />);

    expect((await screen.findAllByText("Front Office User")).length).toBeGreaterThan(0);
    expect(screen.getByText("$2,500.00")).toBeInTheDocument();

    const hourlyInput = screen.getByLabelText("Max trades / hour");
    await userEvent.clear(hourlyInput);
    await userEvent.type(hourlyInput, "8");
    await userEvent.click(screen.getByRole("button", { name: /Save active policy/i }));

    await waitFor(() => expect(screen.getByText("Limits saved for Front Office User.")).toBeInTheDocument());
    const update = fetchMock.mock.calls.find(([url, init]) =>
      url === "/nexus-api/back-office/trading-limits/users/fo-user"
      && (init as RequestInit | undefined)?.method === "PUT");
    expect(update).toBeDefined();
    expect(JSON.parse(String((update?.[1] as RequestInit).body))).toMatchObject({
      maxTradesPerHour: 8,
      active: true,
      version: 1,
    });
  });
});

function limitFixture() {
  return {
    userId: "fo-user",
    username: "front-office",
    displayName: "Front Office User",
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
      updatedByUserId: "bo-user",
      updatedByUsername: "bo-user",
      updatedByDisplayName: "BO User",
      version: 1,
    },
    usage: {
      tradesThisHour: 1,
      tradesToday: 3,
      notionalThisHour: 2500,
      notionalToday: 7500,
      hourEndsAt: "2026-06-19T19:00:00Z",
      dayEndsAt: "2026-06-20T00:00:00Z",
    },
    remaining: {
      tradesThisHour: 4,
      tradesToday: 17,
      notionalThisHour: 7500,
      notionalToday: 42500,
    },
  };
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
