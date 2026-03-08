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
import type { StoryGenerationByPlanPoint } from "@/types/api";

interface StoryGenerationBarChartProps {
  data: StoryGenerationByPlanPoint[];
  loading?: boolean;
}

export function StoryGenerationBarChart({
  data,
  loading,
}: StoryGenerationBarChartProps) {
  if (loading) return null;
  return (
    <ResponsiveContainer width="100%" height={256}>
      <BarChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
        <XAxis
          dataKey="plan"
          className="text-xs"
          tick={{ fill: "hsl(var(--muted-foreground))" }}
        />
        <YAxis
          className="text-xs"
          tick={{ fill: "hsl(var(--muted-foreground))" }}
        />
        <Tooltip
          contentStyle={{
            backgroundColor: "hsl(var(--card))",
            border: "1px solid hsl(var(--border))",
            borderRadius: "var(--radius)",
          }}
          formatter={(v: number) => [v.toLocaleString(), "Stories"]}
        />
        <Bar
          dataKey="count"
          fill="hsl(var(--primary))"
          radius={[2, 2, 0, 0]}
          name="Stories"
        />
      </BarChart>
    </ResponsiveContainer>
  );
}
