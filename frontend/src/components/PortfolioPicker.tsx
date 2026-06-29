"use client";

import React, { useEffect, useState } from "react";
import { Loader2, RefreshCw } from "lucide-react";
import { nexusApi, NexusApiError } from "@/lib/api";
import type { PortfolioSummary } from "@/lib/types";

type PortfolioPickerProps = {
  value: string;
  onChange: (portfolioId: string) => void;
  onError?: (message: string) => void;
  onRefresh?: () => void;
  reloadKey?: number;
  autoSelectFirst?: boolean;
};

export function PortfolioPicker({ value, onChange, onError, onRefresh, reloadKey = 0, autoSelectFirst = true }: PortfolioPickerProps) {
  const [portfolios, setPortfolios] = useState<PortfolioSummary[]>([]);
  const [loading, setLoading] = useState(false);

  async function loadPortfolios() {
    setLoading(true);
    try {
      const result = await nexusApi.listPortfolios();
      setPortfolios(result);
      if (autoSelectFirst && !value && result.length > 0) {
        onChange(result[0].id);
      }
      onRefresh?.();
    } catch (caught) {
      onError?.(caught instanceof NexusApiError || caught instanceof Error ? caught.message : "Could not load portfolios");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadPortfolios();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [reloadKey]);

  return (
    <div className="picker-row">
      <label className="field picker-field">
        <span>Portfolio</span>
        <select className="select" value={value} onChange={(event) => onChange(event.target.value)}>
          <option value="">Select portfolio</option>
          {portfolios.map((portfolio) => (
            <option key={portfolio.id} value={portfolio.id}>
              {portfolio.name} · {portfolio.baseCurrency} · {portfolio.positionCount} positions
            </option>
          ))}
        </select>
      </label>
      <button className="icon-btn picker-refresh" type="button" onClick={loadPortfolios} disabled={loading} title="Refresh portfolios" aria-label="Refresh portfolios">
        {loading ? <Loader2 size={15} /> : <RefreshCw size={15} />}
      </button>
    </div>
  );
}
