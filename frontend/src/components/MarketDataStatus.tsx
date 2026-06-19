"use client";

import React, { useEffect, useState } from "react";
import { CheckCircle2, Loader2, RefreshCw } from "lucide-react";
import { blembergApi } from "@/lib/api";

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
    setMessage("Refreshing market data");
    try {
      await blembergApi.refreshMarketData();
      const health = await blembergApi.health();
      setStatus(health.status === "UP" ? "online" : "offline");
      setMessage(health.status === "UP" ? "Refresh requested" : "Refresh sent, health not UP");
      console.info("[Blemberg] Global refresh requested", { health: health.status });
      window.dispatchEvent(new CustomEvent("blemberg:refresh-completed"));
    } catch {
      setStatus("offline");
      setMessage("Refresh unavailable");
      console.error("[Blemberg] Global refresh failed");
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
