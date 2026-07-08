"use client";

import React from "react";
import { nexusApi } from "./api";
import type { OperationalControlStatus } from "./types";

export function useOperationalControlStatus(enabled = true) {
  const [status, setStatus] = React.useState<OperationalControlStatus | null>(null);
  const [error, setError] = React.useState<string | null>(null);

  const load = React.useCallback(async () => {
    if (!enabled) {
      return;
    }
    try {
      setStatus(await nexusApi.getOperationalControlStatus());
      setError(null);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : "Operational status unavailable");
    }
  }, [enabled]);

  React.useEffect(() => {
    void load();
    const interval = window.setInterval(load, 60000);
    return () => window.clearInterval(interval);
  }, [load]);

  return { status, error, refresh: load };
}

export function operationalClosedMessage(status: OperationalControlStatus | null) {
  if (!status || status.tradingOpen || !status.operationalWindowEnforced) {
    return null;
  }
  return closedMessage(status, "Trading is closed");
}

export function operationalTradeClosedMessage(status: OperationalControlStatus | null) {
  if (!status || status.tradingOpen || !status.tradeBookingsWindowEnforced) {
    return null;
  }
  return closedMessage(status, "Trade booking is closed");
}

export function operationalRiskClosedMessage(status: OperationalControlStatus | null) {
  if (!status || status.tradingOpen || !status.riskRunsWindowEnforced) {
    return null;
  }
  return closedMessage(status, "Risk runs are closed");
}

function closedMessage(status: OperationalControlStatus, prefix: string) {
  const nextOpen = new Date(status.nextOpenAt).toLocaleString();
  return `${prefix} (${status.reason.replaceAll("_", " ").toLowerCase()}). Next open: ${nextOpen}.`;
}
