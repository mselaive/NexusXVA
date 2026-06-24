"use client";

import React, { useEffect, useState } from "react";
import { CheckCircle2, Loader2, RefreshCw } from "lucide-react";
import { blembergApi, NexusApiError } from "@/lib/api";
import { logBlembergRefreshOutcome, summarizeBlembergRefresh } from "@/lib/blembergRefresh";
import { WATCHLIST_SYMBOLS } from "@/lib/marketUniverse";

const POST_REFRESH_SNAPSHOT_DELAY_MS = 1200;

export function MarketDataStatus() {
  const [status, setStatus] = useState<"checking" | "online" | "offline" | "refreshing">("checking");
  const [message, setMessage] = useState("Checking Blemberg");

  async function checkHealth() {
    setStatus("checking");
    setMessage("Checking Blemberg");
    try {
      const health = await blembergApi.health();
      if (health.status === "UP") {
        setStatus("online");
        setMessage("Ready for market data");
      } else {
        setStatus("offline");
        setMessage("Health is not UP");
      }
    } catch {
      setStatus("offline");
      setMessage("Use local provider or start Blemberg");
    }
  }

  async function refreshBlemberg() {
    setStatus("refreshing");
    setMessage("Checking missing symbols");
    try {
      const missingBeforeRefresh = await loadMissingSymbols();
      const prioritySymbols = missingBeforeRefresh.length > 0 ? missingBeforeRefresh : undefined;
      setMessage(prioritySymbols ? `Refreshing missing: ${shortSymbolList(prioritySymbols)}` : "Refreshing market data");
      console.info("[Blemberg] Header refresh requested", {
        mode: prioritySymbols ? "missing-first" : "global",
        prioritySymbols: prioritySymbols ?? [],
      });

      const refresh = await blembergApi.refreshMarketData(prioritySymbols);
      await delay(POST_REFRESH_SNAPSHOT_DELAY_MS);
      const [snapshots, coverage, health] = await Promise.all([
        blembergApi.snapshots(WATCHLIST_SYMBOLS).catch((caught) => {
          console.warn("[Blemberg] Post-refresh snapshot reload failed", caught);
          return undefined;
        }),
        prioritySymbols ? blembergApi.coverage(prioritySymbols).catch((caught) => {
          console.warn("[Blemberg] Post-refresh coverage reload failed", caught);
          return undefined;
        }) : Promise.resolve(undefined),
        blembergApi.health(),
      ]);

      const loggedPrioritySymbols = prioritySymbols ?? refresh.requestedSymbols ?? [];
      logBlembergRefreshOutcome("Header refresh", refresh, loggedPrioritySymbols, coverage);
      const summary = summarizeBlembergRefresh(refresh, loggedPrioritySymbols, coverage);
      const missingAfterRefresh = snapshots?.missingSymbols ?? [];
      setStatus(health.status === "UP" ? "online" : "offline");
      setMessage(health.status === "UP" ? `${summary.message} ${missingAfterRefresh.length} snapshots missing.` : "Refresh sent, health not UP");
      console.info("[Blemberg] Header refresh completed", {
        health: health.status,
        missingBeforeRefresh,
        missingAfterRefresh,
      });
      window.dispatchEvent(new CustomEvent("blemberg:refresh-completed", {
        detail: {
          prioritySymbols: loggedPrioritySymbols,
          missingBeforeRefresh,
          missingAfterRefresh,
        },
      }));
    } catch {
      setStatus("offline");
      setMessage("Refresh unavailable");
      console.error("[Blemberg] Header refresh failed");
    }
  }

  useEffect(() => {
    void checkHealth();
  }, []);

  const label = status === "online" ? "Blemberg online" : status === "offline" ? "Blemberg offline" : status === "refreshing" ? "Refreshing" : "Checking";

  return (
    <button
      className={`market-status ${status}`}
      type="button"
      onClick={refreshBlemberg}
      disabled={status === "refreshing"}
      title="Refresh Blemberg market data"
      aria-live="polite"
    >
      <span className="status-dot" />
      <span className="market-status-copy">
        <strong>{label}</strong>
        <span>{message}</span>
      </span>
      {status === "refreshing" || status === "checking" ? <Loader2 size={15} /> : status === "online" ? <CheckCircle2 size={15} /> : <RefreshCw size={15} />}
    </button>
  );
}

async function loadMissingSymbols() {
  try {
    const snapshots = await blembergApi.snapshots(WATCHLIST_SYMBOLS);
    return normalizeSymbols(snapshots.missingSymbols);
  } catch (caught) {
    if (caught instanceof NexusApiError && caught.status === 404) {
      return WATCHLIST_SYMBOLS;
    }
    throw caught;
  }
}

function normalizeSymbols(symbols: string[] | null | undefined) {
  return Array.from(new Set((symbols ?? []).map((symbol) => symbol.trim().toUpperCase()).filter(Boolean)));
}

function shortSymbolList(symbols: string[]) {
  const visible = symbols.slice(0, 4).join(", ");
  return symbols.length > 4 ? `${visible}, +${symbols.length - 4}` : visible;
}

function delay(milliseconds: number) {
  return new Promise((resolve) => window.setTimeout(resolve, milliseconds));
}
