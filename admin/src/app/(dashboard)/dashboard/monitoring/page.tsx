"use client";

import { useState, useEffect, useCallback } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { mockApi } from "@/lib/mock-api";
import type { RuntimeConfigDto, SystemMonitoringMetrics } from "@/types/api";
import { api } from "@/lib/api";
import {
  Gauge,
  MessageSquare,
  Database,
  AlertTriangle,
  DollarSign,
} from "lucide-react";

export default function SystemMonitoringPage() {
  const [metrics, setMetrics] = useState<SystemMonitoringMetrics | null>(null);
  const [runtimeConfig, setRuntimeConfig] = useState<RuntimeConfigDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    if (mockApi.useMock()) {
      setMetrics(mockApi.getMockSystemMetrics());
      setLoading(false);
      return;
    }
    // Real API: health + AI metrics for system view
    Promise.all([
      api.admin.getHealth().catch(() => null),
      api.admin.getAiMetrics().catch(() => null),
      api.admin.getRuntimeConfig().catch(() => null),
    ])
      .then(([health, ai, runtime]) => {
        setRuntimeConfig(runtime);
        if (health || ai) {
          const cacheTotal = (ai?.cacheHits ?? 0) + (ai?.cacheMisses ?? 0);
          setMetrics({
            apiLatencyMs: (health as { latencyMs?: number })?.latencyMs ?? 0,
            kafkaLag: 0,
            redisHitRatio: cacheTotal > 0 ? (ai!.cacheHits / cacheTotal) : 0,
            errorRate: 0,
            aiCostUsd: 0,
          });
          setError(null);
        } else {
          setError("Unable to load monitoring data. Check backend connectivity.");
          setMetrics(null);
        }
      })
      .catch((e) => {
        setError(e instanceof Error ? e.message : "Failed to load monitoring data.");
        setMetrics(null);
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  if (error) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            System monitoring
          </h1>
          <p className="text-muted-foreground">
            API latency, Kafka, Redis, errors, and AI cost.
          </p>
        </div>
        <Card className="border-destructive/50">
          <CardContent className="pt-6">
            <p className="text-sm text-destructive">{error}</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  const cards = metrics
    ? [
        {
          label: "API latency",
          value: `${metrics.apiLatencyMs} ms`,
          icon: Gauge,
          description: "P95 response time",
        },
        {
          label: "Kafka lag",
          value: metrics.kafkaLag.toString(),
          icon: MessageSquare,
          description: "Consumer lag (messages)",
        },
        {
          label: "Redis hit ratio",
          value: `${(metrics.redisHitRatio * 100).toFixed(1)}%`,
          icon: Database,
          description: "Cache effectiveness",
        },
        {
          label: "Error rate",
          value: `${(metrics.errorRate * 100).toFixed(2)}%`,
          icon: AlertTriangle,
          description: "Last 24h",
        },
        {
          label: "AI cost",
          value: `$${metrics.aiCostUsd.toFixed(2)}`,
          icon: DollarSign,
          description: "Current period",
        },
      ]
    : [];

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            System monitoring
          </h1>
          <p className="text-muted-foreground">
            API latency, Kafka lag, Redis hit ratio, error rate, and AI cost.
          </p>
        </div>
        <Button
          variant="outline"
          size="sm"
          onClick={load}
          disabled={loading}
        >
          {loading ? "Refreshing…" : "Refresh"}
        </Button>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        {loading && !metrics
          ? Array.from({ length: 5 }).map((_, i) => (
              <Card key={i}>
                <CardHeader className="pb-2">
                  <Skeleton className="h-4 w-24" />
                </CardHeader>
                <CardContent>
                  <Skeleton className="h-8 w-16" />
                  <Skeleton className="mt-1 h-3 w-28" />
                </CardContent>
              </Card>
            ))
          : cards.map(({ label, value, icon: Icon, description }) => (
              <Card key={label}>
                <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                  <CardTitle className="text-sm font-medium text-muted-foreground">
                    {label}
                  </CardTitle>
                  <Icon className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-semibold">{value}</div>
                  <p className="text-xs text-muted-foreground">{description}</p>
                </CardContent>
              </Card>
            ))}
      </div>

      {runtimeConfig && (
        <Card
          className={
            runtimeConfig.flywayEnabled
              ? "border-amber-300 bg-amber-50/70 dark:border-amber-700 dark:bg-amber-950/20"
              : "border-emerald-300 bg-emerald-50/70 dark:border-emerald-700 dark:bg-emerald-950/20"
          }
        >
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">
              Migration mode ({runtimeConfig.migrationMode})
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-1 text-sm">
            <p>{runtimeConfig.operatorHint}</p>
            <p className="text-muted-foreground">
              Instance: {runtimeConfig.instanceId} | lockRetryCount:{" "}
              {runtimeConfig.flywayLockRetryCount}
            </p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
