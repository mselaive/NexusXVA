"use client";

type Point = { label: string; value: number; secondary?: number };

export function RiskLineChart({ points, primaryLabel, secondaryLabel }: { points: Point[]; primaryLabel: string; secondaryLabel?: string }) {
  if (!points.length) return <div className="risk-empty-chart">No successful component data.</div>;
  const width = 720, height = 220, pad = 30;
  const values = points.flatMap((point) => [point.value, point.secondary ?? point.value]);
  const max = Math.max(1, ...values), min = Math.min(0, ...values);
  const x = (index: number) => pad + index * (width - pad * 2) / Math.max(1, points.length - 1);
  const y = (value: number) => height - pad - (value - min) * (height - pad * 2) / Math.max(1, max - min);
  const path = (key: "value" | "secondary") => points.map((point, index) => `${index ? "L" : "M"}${x(index)},${y(key === "value" ? point.value : point.secondary ?? 0)}`).join(" ");
  return <div className="risk-chart-wrap">
    <div className="risk-chart-legend"><span><i className="primary" />{primaryLabel}</span>{secondaryLabel && <span><i className="secondary" />{secondaryLabel}</span>}</div>
    <svg viewBox={`0 0 ${width} ${height}`} role="img" aria-label={`${primaryLabel} chart`}>
      <line x1={pad} x2={width-pad} y1={y(0)} y2={y(0)} className="risk-chart-axis" />
      <path d={path("value")} className="risk-chart-line primary" />
      {secondaryLabel && <path d={path("secondary")} className="risk-chart-line secondary" />}
      {points.map((point,index)=><circle key={point.label} cx={x(index)} cy={y(point.value)} r="3" className="risk-chart-dot"><title>{point.label}: {point.value.toFixed(2)}</title></circle>)}
    </svg>
  </div>;
}

export function RiskBarChart({ points, valueLabel }: { points: Point[]; valueLabel: string }) {
  if (!points.length) return <div className="risk-empty-chart">No successful component data.</div>;
  const max = Math.max(1, ...points.map((point)=>Math.abs(point.value)));
  return <div className="risk-bars" role="img" aria-label={`${valueLabel} chart`}>
    {points.map((point)=><div className="risk-bar-row" key={point.label} title={`${point.label}: ${point.value.toFixed(2)}`}>
      <span>{point.label}</span><div><i className={point.value < 0 ? "negative" : "positive"} style={{width:`${Math.max(2,Math.abs(point.value)/max*100)}%`}} /></div><strong>{point.value.toFixed(0)}</strong>
    </div>)}
  </div>;
}
