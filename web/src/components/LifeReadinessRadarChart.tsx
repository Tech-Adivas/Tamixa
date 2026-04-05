import { useId, useMemo } from "react";
import type { LifeReadinessAxisId, LifeReadinessSnapshot } from "../lib/lifeReadinessModel";
import { LIFE_READINESS_AXES, readinessStatusLabel, valueForAxis } from "../lib/lifeReadinessModel";

export interface LifeReadinessRadarChartProps {
  snapshot: LifeReadinessSnapshot;
  size?: number;
  className?: string;
}

/**
 * Lightweight SVG spider / radar chart (web). For React Native, mirror geometry with
 * react-native-svg or victory-native.
 */
export default function LifeReadinessRadarChart({
  snapshot,
  size = 280,
  className,
}: LifeReadinessRadarChartProps) {
  const uid = useId();
  const cx = size / 2;
  const cy = size / 2;
  const maxR = size * 0.36;
  const n = LIFE_READINESS_AXES.length;

  const rings = [0.25, 0.5, 0.75, 1];

  const { polygonPoints, labelPositions } = useMemo(() => {
    const pts: string[] = [];
    const labels: { id: LifeReadinessAxisId; x: number; y: number; lx: number; ly: number; t: string }[] = [];
    for (let i = 0; i < n; i++) {
      const angle = -Math.PI / 2 + (i * 2 * Math.PI) / n;
      const v = valueForAxis(snapshot, LIFE_READINESS_AXES[i].id) / 100;
      const r = maxR * Math.min(1, Math.max(0, v));
      const x = cx + r * Math.cos(angle);
      const y = cy + r * Math.sin(angle);
      pts.push(`${x.toFixed(1)},${y.toFixed(1)}`);
      const lr = maxR + 22;
      labels.push({
        id: LIFE_READINESS_AXES[i].id,
        x,
        y,
        lx: cx + lr * Math.cos(angle),
        ly: cy + lr * Math.sin(angle),
        t: LIFE_READINESS_AXES[i].label,
      });
    }
    return { polygonPoints: pts.join(" "), labelPositions: labels };
  }, [snapshot, cx, cy, maxR, n]);

  const summary = LIFE_READINESS_AXES.map(
    (a) => `${a.label}: ${readinessStatusLabel(valueForAxis(snapshot, a.id))}`
  ).join("; ");

  return (
    <figure className={`life-readiness-radar ${className ?? ""}`} role="img" aria-label={`Life readiness: ${summary}`}>
      <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`} className="life-readiness-radar__svg">
        <defs>
          <linearGradient id={`${uid}-fill`} x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="rgba(18, 140, 126, 0.45)" />
            <stop offset="100%" stopColor="rgba(52, 183, 241, 0.35)" />
          </linearGradient>
        </defs>
        {rings.map((t) => (
          <polygon
            key={t}
            points={Array.from({ length: n }, (_, i) => {
              const angle = -Math.PI / 2 + (i * 2 * Math.PI) / n;
              const r = maxR * t;
              const x = cx + r * Math.cos(angle);
              const y = cy + r * Math.sin(angle);
              return `${x.toFixed(1)},${y.toFixed(1)}`;
            }).join(" ")}
            fill="none"
            stroke="rgba(255,255,255,0.14)"
            strokeWidth={1}
          />
        ))}
        {LIFE_READINESS_AXES.map((_, i) => {
          const angle = -Math.PI / 2 + (i * 2 * Math.PI) / n;
          const x2 = cx + maxR * Math.cos(angle);
          const y2 = cy + maxR * Math.sin(angle);
          return (
            <line
              key={i}
              x1={cx}
              y1={cy}
              x2={x2}
              y2={y2}
              stroke="rgba(255,255,255,0.18)"
              strokeWidth={1}
            />
          );
        })}
        <polygon
          points={polygonPoints}
          fill={`url(#${uid}-fill)`}
          stroke="rgba(128, 203, 196, 0.95)"
          strokeWidth={2}
          strokeLinejoin="round"
        />
        {labelPositions.map((lp) => (
          <text
            key={lp.id}
            x={lp.lx}
            y={lp.ly}
            textAnchor="middle"
            dominantBaseline="middle"
            className="life-readiness-radar__label"
          >
            {lp.t}
          </text>
        ))}
      </svg>
      <figcaption className="visually-hidden">{summary}</figcaption>
    </figure>
  );
}
