import type { BlembergCoverageItem, BlembergCoverageResponse, BlembergRefreshResponse } from "./types";

export type BlembergRefreshSummary = {
  message: string;
  shouldWarn: boolean;
  prioritySymbols: string[];
  attemptedSymbols: string[];
  succeededSymbols: string[];
  skippedRateLimitSymbols: string[];
  missingSnapshotSymbols: string[];
  pricingNotReadySymbols: string[];
  coverageBySymbol: Record<string, BlembergCoverageItem>;
};

export function summarizeBlembergRefresh(
  refresh: BlembergRefreshResponse,
  prioritySymbols: string[],
  coverage?: BlembergCoverageResponse,
): BlembergRefreshSummary {
  const priority = normalizeSymbols(prioritySymbols);
  const attemptedSymbols = normalizeSymbols(refresh.attemptedSymbols);
  const succeededSymbols = normalizeSymbols(refresh.succeededSymbols);
  const skippedRateLimitSymbols = normalizeSymbols(refresh.skippedRateLimitSymbols);
  const missingSnapshotSymbols = normalizeSymbols(refresh.missingSnapshotSymbols);
  const pricingNotReadySymbols = normalizeSymbols(refresh.pricingNotReadySymbols);
  const skippedRateLimitCount = skippedRateLimitSymbols.length
    || refresh.jobSummaries?.reduce((sum, job) => sum + (job.skippedRateLimit ?? 0), 0)
    || 0;
  const coverageBySymbol = coverageMap(coverage);
  const prioritySet = new Set(priority);

  const prioritySkipped = skippedRateLimitSymbols.filter((symbol) => prioritySet.has(symbol));
  const priorityMissingSnapshots = missingSnapshotSymbols.filter((symbol) => prioritySet.has(symbol));
  const priorityPricingNotReady = pricingNotReadySymbols.filter((symbol) => prioritySet.has(symbol));
  const coverageNotReady = Object.values(coverageBySymbol)
    .filter((item) => prioritySet.has(item.symbol.toUpperCase()) && isCoverageNotReady(item))
    .map((item) => item.symbol.toUpperCase());
  const unresolvedPriority = uniqueSymbols([
    ...prioritySkipped,
    ...priorityMissingSnapshots,
    ...priorityPricingNotReady,
    ...coverageNotReady,
  ]);

  const attempted = attemptedSymbols.length || refresh.symbolsRequested || priority.length;
  const succeeded = succeededSymbols.length || refresh.symbolsSucceeded || 0;
  const rateLimitText = skippedRateLimitCount > 0
    ? ` ${skippedRateLimitCount} skipped by rate limit.`
    : "";
  const unresolvedText = unresolvedPriority.length > 0
    ? ` Priority still incomplete: ${shortList(unresolvedPriority)}.`
    : "";
  const message = `Refresh ${refresh.status ?? "completed"}: ${succeeded}/${attempted} attempted symbols succeeded.${rateLimitText}${unresolvedText}`;

  return {
    message,
    shouldWarn: skippedRateLimitCount > 0 || unresolvedPriority.length > 0,
    prioritySymbols: priority,
    attemptedSymbols,
    succeededSymbols,
    skippedRateLimitSymbols,
    missingSnapshotSymbols,
    pricingNotReadySymbols,
    coverageBySymbol,
  };
}

export function logBlembergRefreshOutcome(
  context: string,
  refresh: BlembergRefreshResponse,
  prioritySymbols: string[],
  coverage?: BlembergCoverageResponse,
) {
  const summary = summarizeBlembergRefresh(refresh, prioritySymbols, coverage);
  const payload = {
    prioritySymbols: summary.prioritySymbols,
    attemptedSymbols: summary.attemptedSymbols,
    succeededSymbols: summary.succeededSymbols,
    skippedRateLimitSymbols: summary.skippedRateLimitSymbols,
    missingSnapshotSymbols: summary.missingSnapshotSymbols,
    pricingNotReadySymbols: summary.pricingNotReadySymbols,
    coverageBySymbol: summary.coverageBySymbol,
  };

  if (summary.shouldWarn) {
    console.warn(`[Blemberg] ${context}: ${summary.message}`, payload);
  } else {
    console.info(`[Blemberg] ${context}: ${summary.message}`, payload);
  }
}

function normalizeSymbols(symbols: string[] | null | undefined) {
  return uniqueSymbols((symbols ?? []).map((symbol) => symbol.trim().toUpperCase()).filter(Boolean));
}

function uniqueSymbols(symbols: string[]) {
  return Array.from(new Set(symbols));
}

function coverageMap(response?: BlembergCoverageResponse) {
  const items = response?.symbols ?? response?.coverage ?? response?.items ?? response?.results ?? [];
  return items.reduce<Record<string, BlembergCoverageItem>>((map, item) => {
    if (item.symbol) {
      map[item.symbol.toUpperCase()] = { ...item, symbol: item.symbol.toUpperCase() };
    }
    return map;
  }, {});
}

function isCoverageNotReady(item: BlembergCoverageItem) {
  if (typeof item.pricingReady === "boolean") {
    return !item.pricingReady;
  }
  if (typeof item.readyForPricing === "boolean") {
    return !item.readyForPricing;
  }
  return item.status?.toUpperCase() === "NOT_READY";
}

function shortList(symbols: string[]) {
  const visible = symbols.slice(0, 8).join(", ");
  return symbols.length > 8 ? `${visible}, +${symbols.length - 8} more` : visible;
}
