import { afterEach, describe, expect, it, vi } from "vitest";
import { nexusApi, NexusApiError } from "./api";

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
