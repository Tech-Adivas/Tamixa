"use client";

import { AlertTriangle, Zap, TrendingDown } from "lucide-react";
import type { RevenueAlerts } from "@/types/api";

interface RevenueAlertsBannerProps {
  alerts: RevenueAlerts | null;
  loading?: boolean;
}

export function RevenueAlertsBanner({ alerts, loading }: RevenueAlertsBannerProps) {
  if (loading || !alerts) return null;

  const issues: { id: string; message: string; severity: "warning" | "error" }[] = [];

  if (
    alerts.paymentFailureRate != null &&
    alerts.paymentFailureThreshold != null &&
    alerts.paymentFailureRate > alerts.paymentFailureThreshold
  ) {
    issues.push({
      id: "payment",
      message: `Payment failure rate (${(alerts.paymentFailureRate * 100).toFixed(1)}%) exceeds threshold (${(alerts.paymentFailureThreshold * 100).toFixed(0)}%). Review billing provider.`,
      severity: "warning",
    });
  }

  if (alerts.aiTokenSpike) {
    issues.push({
      id: "ai",
      message: "AI token usage spike detected. Check for abuse or unexpected load.",
      severity: "warning",
    });
  }

  if (alerts.churnIncrease && alerts.churnRate != null) {
    issues.push({
      id: "churn",
      message: `Churn rate (${(alerts.churnRate * 100).toFixed(2)}%) has increased. Consider retention campaigns.`,
      severity: "warning",
    });
  }

  if (issues.length === 0) return null;

  return (
    <div
      className="mb-6 rounded-lg border border-amber-500/50 bg-amber-500/10 px-4 py-3 dark:border-amber-400/30 dark:bg-amber-500/5"
      role="alert"
    >
      <div className="flex items-start gap-3">
        <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-600 dark:text-amber-400" />
        <div className="flex-1 space-y-2">
          <p className="text-sm font-medium text-amber-800 dark:text-amber-200">
            Revenue alerts
          </p>
          <ul className="space-y-1 text-sm text-amber-700 dark:text-amber-300">
            {issues.map((i) => (
              <li key={i.id} className="flex items-center gap-2">
                {i.id === "payment" && (
                  <Zap className="h-3.5 w-3.5 shrink-0" />
                )}
                {i.id === "ai" && (
                  <Zap className="h-3.5 w-3.5 shrink-0" />
                )}
                {i.id === "churn" && (
                  <TrendingDown className="h-3.5 w-3.5 shrink-0" />
                )}
                {i.message}
              </li>
            ))}
          </ul>
        </div>
      </div>
    </div>
  );
}
