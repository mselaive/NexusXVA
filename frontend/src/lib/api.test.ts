import { afterEach, describe, expect, it, vi } from "vitest";
import { blembergApi, nexusApi, NexusApiError } from "./api";
import { summarizeBlembergRefresh } from "./blembergRefresh";

describe("nexusApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("loads portfolios from the configured API base", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify([{ id: "portfolio-1", name: "Book", baseCurrency: "USD", positionCount: 0 }]), {
        status: 200,
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const result = await nexusApi.listPortfolios();

    expect(fetchMock).toHaveBeenCalledWith(
      "/nexus-api/portfolios",
      expect.objectContaining({
        headers: expect.objectContaining({ "Content-Type": "application/json" }),
      }),
    );
    expect(result).toHaveLength(1);
  });

  it("maps backend ApiError payloads to NexusApiError", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ status: 400, message: "Unknown underlyingSymbol" }), {
          status: 400,
        }),
      ),
    );

    await expect(nexusApi.getPortfolio("missing")).rejects.toMatchObject<NexusApiError>({
      name: "NexusApiError",
      message: "Unknown underlyingSymbol",
      status: 400,
    });
  });
});

describe("blembergApi", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("sends priority symbols when refreshing market data", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ status: "COMPLETED", requestedSymbols: ["AAPL"] }), {
        status: 200,
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await blembergApi.refreshMarketData(["AAPL"]);

    expect(fetchMock).toHaveBeenCalledWith(
      "/blemberg-api/api/admin/market-data/refresh",
      expect.objectContaining({
        method: "POST",
        body: JSON.stringify({ prioritySymbols: ["AAPL"] }),
      }),
    );
  });

  it("loads Blemberg coverage for requested symbols", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ symbols: [{ symbol: "AAPL", pricingReady: true }] }), {
        status: 200,
      }),
    );
    vi.stubGlobal("fetch", fetchMock);

    const result = await blembergApi.coverage(["AAPL", "MSFT"]);

    expect(fetchMock).toHaveBeenCalledWith(
      "/blemberg-api/api/market-data/coverage?symbols=AAPL%2CMSFT",
      expect.any(Object),
    );
    expect(result.symbols?.[0].symbol).toBe("AAPL");
  });
});

describe("Blemberg refresh summaries", () => {
  it("warns when a priority symbol is skipped by rate limit", () => {
    const summary = summarizeBlembergRefresh(
      { status: "COMPLETED", skippedRateLimitSymbols: ["AAPL"], attemptedSymbols: ["AAPL"] },
      ["AAPL"],
    );

    expect(summary.shouldWarn).toBe(true);
    expect(summary.message).toContain("skipped by rate limit");
  });

  it("warns when a priority symbol is still missing or not ready", () => {
    const summary = summarizeBlembergRefresh(
      { status: "COMPLETED", missingSnapshotSymbols: ["MSFT"], pricingNotReadySymbols: ["AAPL"] },
      ["AAPL", "MSFT"],
    );

    expect(summary.shouldWarn).toBe(true);
    expect(summary.message).toContain("Priority still incomplete");
  });

  it("uses info semantics when all priority symbols are refreshed", () => {
    const summary = summarizeBlembergRefresh(
      { status: "COMPLETED", attemptedSymbols: ["AAPL"], succeededSymbols: ["AAPL"] },
      ["AAPL"],
      { symbols: [{ symbol: "AAPL", pricingReady: true }] },
    );

    expect(summary.shouldWarn).toBe(false);
    expect(summary.message).toContain("1/1");
  });
});
