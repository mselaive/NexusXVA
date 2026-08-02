"use client";

import React from "react";
import { formatCurrency, formatPercent } from "@/lib/format";
import type { CvaPoint } from "@/lib/types";

const width = 760;
const height = 240;
const padding = { top: 20, right: 20, bottom: 38, left: 64 };

export function CvaRiskCharts({ points, currency }: { points: CvaPoint[]; currency: string }) {
  if (points.length === 0) {
    return <div className="empty">No CVA buckets to chart.</div>;
  }

  return (
    <div className="cva-chart-grid">
      <CvaLineChart
        title="Exposure after collateral"
        description="Gross expected exposure compared with the residual exposure used by CVA."
        points={points}
        currency={currency}
      />
      <CvaContributionChart points={points} currency={currency} />
      <CvaSurvivalChart points={points} />
    </div>
  );
}

function CvaLineChart({
  title,
  description,
  points,
  currency,
}: {
  title: string;
  description: string;
  points: CvaPoint[];
  currency: string;
}) {
  const maxY = Math.max(1, ...points.flatMap((point) => [point.grossExpectedExposure, point.expectedExposure]));
  const x = xScale(points.length);
  const y = yScale(maxY);
  const polyline = (selector: (point: CvaPoint) => number) => points
    .map((point, index) => `${x(index)},${y(selector(point))}`)
    .join(" ");

  return (
    <section className="cva-chart-panel">
      <header><h3>{title}</h3><p>{description}</p></header>
      <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Gross and residual expected exposure chart">
        <ChartAxes maxY={maxY} y={y} formatter={(value) => formatCurrency(value, currency)} />
        <polyline points={polyline((point) => point.grossExpectedExposure)} fill="none" stroke="var(--cva-gross)" strokeWidth="3" />
        <polyline points={polyline((point) => point.expectedExposure)} fill="none" stroke="var(--cva-residual)" strokeWidth="3" />
        {points.map((point, index) => (
          <g key={point.date}>
            <circle cx={x(index)} cy={y(point.grossExpectedExposure)} r="3.5" fill="var(--cva-gross)">
              <title>{point.date}: gross {formatCurrency(point.grossExpectedExposure, currency)}</title>
            </circle>
            <circle cx={x(index)} cy={y(point.expectedExposure)} r="3.5" fill="var(--cva-residual)">
              <title>{point.date}: residual {formatCurrency(point.expectedExposure, currency)}</title>
            </circle>
          </g>
        ))}
        <DateLabels points={points} />
      </svg>
      <div className="legend">
        <span><i className="swatch" style={{ background: "var(--cva-gross)" }} />Gross EE</span>
        <span><i className="swatch" style={{ background: "var(--cva-residual)" }} />Residual EE</span>
      </div>
    </section>
  );
}

function CvaContributionChart({ points, currency }: { points: CvaPoint[]; currency: string }) {
  const maxY = Math.max(0.01, ...points.map((point) => point.cvaContribution));
  const x = xScale(points.length);
  const y = yScale(maxY);
  const plotWidth = width - padding.left - padding.right;
  const barWidth = Math.max(5, Math.min(34, plotWidth / points.length - 6));

  return (
    <section className="cva-chart-panel">
      <header><h3>CVA contribution by bucket</h3><p>Expected credit loss assigned to each future interval.</p></header>
      <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label="CVA contribution by bucket chart">
        <ChartAxes maxY={maxY} y={y} formatter={(value) => formatCurrency(value, currency)} />
        {points.map((point, index) => {
          const top = y(point.cvaContribution);
          return (
            <rect
              key={point.date}
              x={x(index) - barWidth / 2}
              y={top}
              width={barWidth}
              height={height - padding.bottom - top}
              rx="2"
              fill="var(--cva-cost)"
            >
              <title>{point.date}: {formatCurrency(point.cvaContribution, currency)}</title>
            </rect>
          );
        })}
        <DateLabels points={points} />
      </svg>
    </section>
  );
}

function CvaSurvivalChart({ points }: { points: CvaPoint[] }) {
  const minSurvival = Math.min(...points.map((point) => point.survivalProbability));
  const range = Math.max(0.005, 1 - minSurvival);
  const x = xScale(points.length);
  const y = (value: number) => padding.top + ((1 - value) / range) * (height - padding.top - padding.bottom);
  const line = points.map((point, index) => `${x(index)},${y(point.survivalProbability)}`).join(" ");

  return (
    <section className="cva-chart-panel cva-chart-wide">
      <header><h3>Counterparty survival</h3><p>Probability that the counterparty remains solvent through each bucket.</p></header>
      <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label="Counterparty survival probability chart">
        {[minSurvival, (1 + minSurvival) / 2, 1].map((value) => (
          <g key={value}>
            <line x1={padding.left} y1={y(value)} x2={width - padding.right} y2={y(value)} stroke="#e3e9ec" />
            <text x="6" y={y(value) + 4} fill="#61727a" fontSize="12">{formatPercent(value)}</text>
          </g>
        ))}
        <polyline points={line} fill="none" stroke="var(--cva-survival)" strokeWidth="3" />
        {points.map((point, index) => (
          <circle key={point.date} cx={x(index)} cy={y(point.survivalProbability)} r="3.5" fill="var(--cva-survival)">
            <title>{point.date}: {formatPercent(point.survivalProbability)}</title>
          </circle>
        ))}
        <DateLabels points={points} />
      </svg>
    </section>
  );
}

function ChartAxes({ maxY, y, formatter }: { maxY: number; y: (value: number) => number; formatter: (value: number) => string }) {
  return (
    <>
      {[0, 0.5, 1].map((ratio) => (
        <g key={ratio}>
          <line x1={padding.left} y1={y(maxY * ratio)} x2={width - padding.right} y2={y(maxY * ratio)} stroke="#e3e9ec" />
          <text x="6" y={y(maxY * ratio) + 4} fill="#61727a" fontSize="12">{formatter(maxY * ratio)}</text>
        </g>
      ))}
    </>
  );
}

function DateLabels({ points }: { points: CvaPoint[] }) {
  return (
    <>
      <text x={padding.left} y={height - 10} fill="#61727a" fontSize="12">{points[0]?.date}</text>
      <text x={width - padding.right - 92} y={height - 10} fill="#61727a" fontSize="12">{points.at(-1)?.date}</text>
    </>
  );
}

function xScale(length: number) {
  return (index: number) => {
    if (length <= 1) return padding.left;
    return padding.left + (index / (length - 1)) * (width - padding.left - padding.right);
  };
}

function yScale(maxY: number) {
  return (value: number) => padding.top + (height - padding.top - padding.bottom) * (1 - value / maxY);
}
