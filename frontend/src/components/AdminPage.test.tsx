import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AdminPage } from "./AdminPage";

vi.mock("next/navigation", () => ({
  usePathname: () => "/admin",
}));

describe("AdminPage", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("renders users, protects the current ADMIN group, and updates a permission", async () => {
    const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      if (url === "/nexus-api/auth/me" && !init?.method) {
        return json({
          enabled: true,
          authenticated: true,
          user: { id: "user-1", username: "raul", displayName: "Raul User", groups: ["ADMIN", "FO"] },
          activeGroup: "ADMIN",
          csrfToken: "csrf-token",
        });
      }
      if (url.startsWith("/nexus-api/admin/users?") && !init?.method) {
        return json({ items: [adminUser()], page: 0, size: 100, totalElements: 1, totalPages: 1 });
      }
      if (url === "/nexus-api/admin/portfolios" && !init?.method) {
        return json([{ id: "portfolio-1", name: "Equity Book", baseCurrency: "USD", positionCount: 2 }]);
      }
      if (url === "/nexus-api/admin/users/user-1/permissions" && init?.method === "PUT") {
        return json({
          ...adminUser(),
          permissions: [{ ...adminUser().permissions[0], effectiveEnabled: false, overrideEnabled: false }],
        });
      }
      if (url === "/blemberg-api/actuator/health") {
        return json({ status: "UP" });
      }
      return json({ message: `Unhandled ${url}` }, 500);
    });
    vi.stubGlobal("fetch", fetchMock);

    render(<AdminPage />);

    expect((await screen.findAllByText("Raul User")).length).toBeGreaterThan(0);
    expect(screen.getByRole("checkbox", { name: /ADMIN/i })).toBeDisabled();
    await userEvent.click(screen.getByRole("checkbox", { name: /Book trades/i }));

    await waitFor(() => expect(screen.getByText("Access updated.")).toBeInTheDocument());
    expect(fetchMock).toHaveBeenCalledWith(
      "/nexus-api/admin/users/user-1/permissions",
      expect.objectContaining({ method: "PUT" }),
    );
  });
});

function adminUser() {
  return {
    id: "user-1",
    username: "raul",
    displayName: "Raul User",
    active: true,
    createdAt: "2026-06-01T00:00:00Z",
    updatedAt: "2026-06-01T00:00:00Z",
    lastLoginAt: null,
    groups: ["ADMIN", "FO"],
    permissions: [
      {
        code: "FO_BOOK_TRADES",
        groupCode: "FO",
        name: "Book trades",
        description: "Submit trade bookings from u-Pad.",
        effectiveEnabled: true,
        overrideEnabled: null,
      },
    ],
    portfolioAccess: {
      accessMode: "ALL",
      portfolios: [{ id: "portfolio-1", name: "Equity Book", baseCurrency: "USD", positionCount: 2 }],
    },
  };
}

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}
