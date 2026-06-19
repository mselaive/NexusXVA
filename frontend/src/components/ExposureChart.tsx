"use client";

import React from "react";
import type { ExposurePoint } from "@/lib/types";
import { formatCurrency } from "@/lib/format";

type ExposureChartProps = {
  points: ExposurePoint[];
};

const width = 900;
const height = 260;
const padding = { top: 18, right: 22, bottom: 34, left: 58 };

export function ExposureChart({ points }: ExposureChartProps) {
  if (points.length === 0) {
    return <div className="empty">No exposure points to chart.</div>;
  }

  const maxY = Math.max(
    1,
    ...points.flatMap((point) => [point.expectedExposure, point.expectedNegativeExposure, point.pfe]),
  );

  const xFor = (index: number) => {
    if (points.length === 1) {
      return padding.left;
    }
    const plotWidth = width - padding.left - padding.right;
    return padding.left + (index / (points.length - 1)) * plotWidth;
  };

  const yFor = (value: number) => {
    const plotHeight = height - padding.top - padding.bottom;
    return padding.top + plotHeight - (value / maxY) * plotHeight;
  };

  const lineFor = (selector: (point: ExposurePoint) => number) =>
    points.map((point, index) => `${xFor(index)},${yFor(selector(point))}`).join(" ");

  return (
    <div className="chart" aria-label="Exposure chart">
      <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-labelledby="exposure-chart-title">
        <title id="exposure-chart-title">Exposure profile</title>
        <line x1={padding.left} y1={padding.top} x2={padding.left} y2={height - padding.bottom} stroke="#b6c3ca" />
        <line
          x1={padding.left}
          y1={height - padding.bottom}
          x2={width - padding.right}
          y2={height - padding.bottom}
          stroke="#b6c3ca"
        />
        {[0, 0.5, 1].map((ratio) => {
          const y = yFor(maxY * ratio);
          return (
            <g key={ratio}>
              <line x1={padding.left} y1={y} x2={width - padding.right} y2={y} stroke="#edf1f3" />
              <text x={8} y={y + 4} fill="#61727a" fontSize="12">
                {formatCurrency(maxY * ratio)}
              </text>
            </g>
          );
        })}
        <polyline points={lineFor((point) => point.expectedExposure)} fill="none" stroke="var(--chart-ee)" strokeWidth="3" />
        <polyline
          points={lineFor((point) => point.expectedNegativeExposure)}
          fill="none"
          stroke="var(--chart-ene)"
          strokeWidth="3"
        />
        <polyline points={lineFor((point) => point.pfe)} fill="none" stroke="var(--chart-pfe)" strokeWidth="3" />
        <text x={padding.left} y={height - 8} fill="#61727a" fontSize="12">
          {points[0]?.date}
        </text>
        <text x={width - padding.right - 92} y={height - 8} fill="#61727a" fontSize="12">
          {points[points.length - 1]?.date}
        </text>
      </svg>
      <div className="legend">
        <span>
          <i className="swatch" style={{ background: "var(--chart-ee)" }} />
          Expected Exposure
        </span>
        <span>
          <i className="swatch" style={{ background: "var(--chart-ene)" }} />
          Expected Negative Exposure
        </span>
        <span>
          <i className="swatch" style={{ background: "var(--chart-pfe)" }} />
          PFE
        </span>
      </div>
    </div>
  );
}
