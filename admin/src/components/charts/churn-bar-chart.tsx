"use client";

import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from "recharts";
import type { ChurnTrendPoint } from "@/types/api";

interface ChurnBarChartProps {
  data: ChurnTrendPoint[];
  loading?: boolean;
}

export function ChurnBarChart({ data, loading }: ChurnBarChartProps) {
  if (loading) return null;
  return (
    <ResponsiveContainer width="100%" height={256}>
      <BarChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
        <XAxis
          dataKey="month"
          className="text-xs"
          tick={{ fill: "hsl(var(--muted-foreground))" }}
        />
        <YAxis
          className="text-xs"
          tick={{ fill: "hsl(var(--muted-foreground))" }}
          tickFormatter={(v) => `${(v * 100).toFixed(1)}%`}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: "hsl(var(--card))",
            border: "1px solid hsl(var(--border))",
            borderRadius: "var(--radius)",
          }}
          formatter={(v: number) => [
            `${(v * 100).toFixed(2)}%`,
            "Churn rate",
          ]}
        />
        <Bar
          dataKey="churnRate"
          fill="hsl(var(--destructive))"
          radius={[2, 2, 0, 0]}
          name="Churn rate"
        />
      </BarChart>
    </ResponsiveContainer>
  );
}
