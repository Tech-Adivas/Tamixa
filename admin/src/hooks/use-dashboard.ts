"use client";

import { useState, useEffect, useCallback } from "react";
import type {
  DashboardKpis,
  RevenueChartPoint,
  StoryUsageChartPoint,
} from "@/types/api";
import { api } from "@/lib/api";
import { mockApi } from "@/lib/mock-api";

/** Last N months for revenue chart */
function getLastMonthStrings(count: number): string[] {
  const now = new Date();
  return Array.from({ length: count }, (_, i) => {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
  }).reverse();
}

export function useDashboard() {
  const [kpis, setKpis] = useState<DashboardKpis | null>(null);
  const [revenue, setRevenue] = useState<RevenueChartPoint[]>([]);
  const [storyUsage, setStoryUsage] = useState<StoryUsageChartPoint[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    if (mockApi.useMock()) {
      setKpis(mockApi.getMockDashboardKpis());
      setRevenue(mockApi.getMockRevenueChart());
      setStoryUsage(mockApi.getMockStoryUsageChart());
      setLoading(false);
      return;
    }
    const months = getLastMonthStrings(6);
    const safeFetch = async <T>(fn: () => Promise<T>, fallback: T): Promise<T> => {
      try {
        return await fn();
      } catch {
        return fallback;
      }
    };
    Promise.all([
      safeFetch(() => api.admin.getSubscriptionMetrics(), { activeSubscriptions: 0, mrr: 0, trialCount: 0, planDistribution: {} }),
      safeFetch(() => api.admin.getAiMetrics(), { storyGenerationsTotal: 0, cacheHits: 0, cacheMisses: 0, voiceProcessingCount: 0, openaiTokensUsed: null }),
      safeFetch(() => api.admin.getStories(0, 1, "FLAGGED"), { totalElements: 0 }),
      safeFetch(() => api.admin.getStoryUsagePerDay(7), []),
      safeFetch(
        () => Promise.all(months.map((m) => api.admin.getRevenueMetrics(m))),
        months.map((m) => ({ month: m, revenue: 0 }))
      ),
    ])
      .then(
        ([sub, ai, flaggedRes, usageData, revenueByMonth]) => {
          const flaggedCount = typeof flaggedRes === "object" && "totalElements" in flaggedRes ? flaggedRes.totalElements : 0;
          const revPoints: RevenueChartPoint[] = revenueByMonth.map((r) => ({
            month: new Date(r.month + "-01").toLocaleString("en-US", {
              month: "short",
              year: "2-digit",
            }),
            revenue: typeof r.revenue === "number" ? r.revenue : 0,
          }));
          const currentMonthRevenue = revPoints.length > 0 ? revPoints[revPoints.length - 1].revenue : 0;
          const usagePoints: StoryUsageChartPoint[] = Array.isArray(usageData)
            ? usageData.map((u) => ({
                date: new Date(u.date).toLocaleDateString("en-US", { weekday: "short" }),
                count: typeof u.count === "number" ? u.count : Number(u.count) || 0,
              }))
            : mockApi.getMockStoryUsageChart();
          setKpis({
            activeSubscriptions: sub.activeSubscriptions,
            monthlyRevenue: currentMonthRevenue,
            storyGenerationsToday: Number(ai.storyGenerationsTotal) || 0,
            aiTokenUsage: ai.openaiTokensUsed ?? 0,
            moderationFlags: flaggedCount,
          });
          setRevenue(revPoints);
          setStoryUsage(usagePoints);
        },
        (e) => setError(e instanceof Error ? e.message : "Failed to load")
      )
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return { kpis, revenue, storyUsage, loading, error, refetch: load };
}
