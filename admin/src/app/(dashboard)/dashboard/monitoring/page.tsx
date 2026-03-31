"use client";

import { useState, useEffect, useCallback } from "react";
import { useSearchParams } from "next/navigation";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { mockApi } from "@/lib/mock-api";
import type {
  DoraMetricsDto,
  RuntimeConfigDto,
  StoryLengthProfileDto,
  SystemMonitoringMetrics,
} from "@/types/api";
import { api } from "@/lib/api";
import {
  Gauge,
  MessageSquare,
  Database,
  AlertTriangle,
  DollarSign,
  Rocket,
  Wrench,
  Clock3,
} from "lucide-react";

const DORA_FILTERS_STORAGE_KEY = "admin.monitoring.doraFilters.v1";
const MONITORING_AUTO_REFRESH_MS = 20_000;

function formatUpdatedAgo(lastUpdatedAt: Date | null, nowMs: number): string {
  if (!lastUpdatedAt) return "Never";
  const diffMs = Math.max(0, nowMs - lastUpdatedAt.getTime());
  const totalSeconds = Math.floor(diffMs / 1000);
  if (totalSeconds < 5) return "just now";
  if (totalSeconds < 60) return `${totalSeconds}s ago`;
  const totalMinutes = Math.floor(totalSeconds / 60);
  if (totalMinutes < 60) return `${totalMinutes}m ago`;
  const totalHours = Math.floor(totalMinutes / 60);
  return `${totalHours}h ago`;
}

export default function SystemMonitoringPage() {
  const searchParams = useSearchParams();
  const [metrics, setMetrics] = useState<SystemMonitoringMetrics | null>(null);
  const [doraMetrics, setDoraMetrics] = useState<DoraMetricsDto | null>(null);
  const [runtimeConfig, setRuntimeConfig] = useState<RuntimeConfigDto | null>(null);
  const [storyLengthProfile, setStoryLengthProfile] = useState<StoryLengthProfileDto | null>(null);
  const [storyProfileDays, setStoryProfileDays] = useState<7 | 30>(7);
  const [currentAdminEmail, setCurrentAdminEmail] = useState<string | null>(null);
  const [doraDays, setDoraDays] = useState("30");
  const [doraService, setDoraService] = useState("backend");
  const [doraEnvironment, setDoraEnvironment] = useState("production");
  const [filtersHydrated, setFiltersHydrated] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [copiedDiagnostics, setCopiedDiagnostics] = useState(false);
  const [lastUpdatedAt, setLastUpdatedAt] = useState<Date | null>(null);
  const [nowMs, setNowMs] = useState(() => Date.now());

  useEffect(() => {
    const requestedProfileDays = searchParams.get("storyProfileDays");
    if (requestedProfileDays === "30") {
      setStoryProfileDays(30);
      return;
    }
    if (requestedProfileDays === "7") {
      setStoryProfileDays(7);
    }
  }, [searchParams]);

  useEffect(() => {
    try {
      const raw = window.localStorage.getItem(DORA_FILTERS_STORAGE_KEY);
      if (!raw) {
        setFiltersHydrated(true);
        return;
      }
      const parsed = JSON.parse(raw) as {
        days?: string;
        service?: string;
        environment?: string;
      };
      if (parsed.days) setDoraDays(parsed.days);
      if (parsed.service) setDoraService(parsed.service);
      if (parsed.environment) setDoraEnvironment(parsed.environment);
    } catch {
      // Ignore malformed localStorage payload and use defaults.
    } finally {
      setFiltersHydrated(true);
    }
  }, []);

  const load = useCallback(() => {
    const startedAt = typeof performance !== "undefined" ? performance.now() : Date.now();
    const parsedDays = Number.parseInt(doraDays, 10);
    const safeDays =
      Number.isFinite(parsedDays) && parsedDays > 0
        ? Math.min(parsedDays, 365)
        : 30;
    const serviceFilter = doraService.trim() || undefined;
    const environmentFilter = doraEnvironment.trim() || undefined;

    setLoading(true);
    setError(null);
    if (mockApi.useMock()) {
      setMetrics(mockApi.getMockSystemMetrics());
      setDoraMetrics(null);
      setStoryLengthProfile(null);
      setLastUpdatedAt(new Date());
      setLoading(false);
      return;
    }
    // Real API: health + AI metrics for system view
    Promise.all([
      api.admin.getHealth().catch(() => null),
      api.admin.getAiMetrics().catch(() => null),
      api.admin.getKafkaEvents(0, 1).catch(() => null),
      api.admin
        .getDoraMetrics(safeDays, serviceFilter, environmentFilter)
        .catch(() => null),
      api.admin.getRuntimeConfig().catch(() => null),
      api.admin.getStoryLengthProfile(storyProfileDays).catch(() => null),
      api.getMe().catch(() => null),
    ])
      .then(([health, ai, kafka, dora, runtime, storyProfile, me]) => {
        setRuntimeConfig(runtime);
        setDoraMetrics(dora);
        setStoryLengthProfile(storyProfile);
        setCurrentAdminEmail((me as { email?: string } | null)?.email ?? null);
        if (health || ai || dora || kafka) {
          const elapsedMsRaw =
            (typeof performance !== "undefined" ? performance.now() : Date.now()) - startedAt;
          const elapsedMs = Number.isFinite(elapsedMsRaw)
            ? Math.max(0, Math.round(elapsedMsRaw))
            : 0;
          const cacheTotal = (ai?.cacheHits ?? 0) + (ai?.cacheMisses ?? 0);
          const deployments = (dora?.successfulDeployments ?? 0) + (dora?.failedDeployments ?? 0);
          const errorRate = deployments > 0 ? (dora?.failedDeployments ?? 0) / deployments : 0;
          const aiCostUsd =
            ai?.apiBreakdown?.reduce((sum, row) => sum + (row.costEstimateUsd ?? 0), 0) ?? 0;
          setMetrics({
            apiLatencyMs: (health as { latencyMs?: number })?.latencyMs ?? elapsedMs,
            kafkaLag: kafka?.totalElements ?? 0,
            redisHitRatio: cacheTotal > 0 ? (ai!.cacheHits / cacheTotal) : 0,
            errorRate,
            aiCostUsd,
          });
          setLastUpdatedAt(new Date());
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
  }, [doraDays, doraEnvironment, doraService, storyProfileDays]);

  useEffect(() => {
    if (!filtersHydrated) return;
    try {
      window.localStorage.setItem(
        DORA_FILTERS_STORAGE_KEY,
        JSON.stringify({
          days: doraDays,
          service: doraService,
          environment: doraEnvironment,
        }),
      );
    } catch {
      // Ignore storage errors (private mode/quota).
    }
  }, [doraDays, doraEnvironment, doraService, filtersHydrated]);

  useEffect(() => {
    if (!filtersHydrated) return;
    load();
  }, [filtersHydrated, load]);

  useEffect(() => {
    if (!filtersHydrated) return;
    const timer = window.setInterval(() => {
      if (!document.hidden) {
        load();
      }
    }, MONITORING_AUTO_REFRESH_MS);
    return () => window.clearInterval(timer);
  }, [filtersHydrated, load]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setNowMs(Date.now());
    }, 1000);
    return () => window.clearInterval(timer);
  }, []);

  const adminRequestBase =
    typeof window !== "undefined" ? `${window.location.origin}/api` : "/api";
  const configuredPublicApiUrl =
    (typeof process !== "undefined" && process.env.NEXT_PUBLIC_API_URL?.trim()) ||
    null;
  const lastUpdatedLabel = formatUpdatedAgo(lastUpdatedAt, nowMs);

  const copyDiagnostics = useCallback(async () => {
    if (!runtimeConfig) return;
    const nowIso = new Date().toISOString();
    const lines = [
      `copiedAt=${nowIso}`,
      `adminEmail=${currentAdminEmail ?? "(unknown)"}`,
      `instanceId=${runtimeConfig.instanceId}`,
      `migrationMode=${runtimeConfig.migrationMode}`,
      `flywayEnabled=${runtimeConfig.flywayEnabled}`,
      `flywayLockRetryCount=${runtimeConfig.flywayLockRetryCount}`,
      `storyApprovalRetentionMode=${runtimeConfig.storyApprovalRetentionMode ?? "(unknown)"}`,
      `keepNarrationApprovalOnMetadataOnlyPublishedUpdate=${runtimeConfig.keepNarrationApprovalOnMetadataOnlyPublishedUpdate ?? "(unknown)"}`,
      `adminApiBase=${adminRequestBase}`,
      `configuredPublicApiUrl=${configuredPublicApiUrl ?? "(not set)"}`,
      `datasourceTarget=${runtimeConfig.datasourceTarget ?? "(not provided)"}`,
    ];
    const payload = lines.join("\n");
    try {
      if (typeof navigator !== "undefined" && navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(payload);
      } else {
        throw new Error("Clipboard API unavailable");
      }
      setCopiedDiagnostics(true);
      window.setTimeout(() => setCopiedDiagnostics(false), 1800);
    } catch {
      setCopiedDiagnostics(false);
    }
  }, [adminRequestBase, configuredPublicApiUrl, currentAdminEmail, runtimeConfig]);

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
        {
          label: "Deployments/day",
          value: (doraMetrics?.deploymentFrequencyPerDay ?? 0).toFixed(2),
          icon: Rocket,
          description: `DORA (last ${doraMetrics?.windowDays ?? 30} days)`,
        },
        {
          label: "Change failure rate",
          value: `${(doraMetrics?.changeFailureRatePercent ?? 0).toFixed(1)}%`,
          icon: Wrench,
          description: "Failed deployments",
        },
        {
          label: "MTTR",
          value: doraMetrics?.meanTimeToRestoreMinutes
            ? `${doraMetrics.meanTimeToRestoreMinutes} min`
            : "n/a",
          icon: Clock3,
          description: "Incident recovery time",
        },
        {
          label: `Avg story words (${storyLengthProfile?.windowDays ?? storyProfileDays}d)`,
          value: storyLengthProfile ? `${Math.round(storyLengthProfile.avgWordCount)}` : "n/a",
          icon: MessageSquare,
          description: `${storyLengthProfile?.totalStories ?? 0} stories sampled`,
        },
        {
          label: "Avg expected minutes",
          value: storyLengthProfile
            ? `${storyLengthProfile.avgExpectedMinutesByWords.toFixed(2)} min`
            : "n/a",
          icon: Clock3,
          description: `Word-based (${storyLengthProfile?.wpmAssumption ?? 120} WPM)`,
        },
        {
          label: "Avg reading minutes (model)",
          value: storyLengthProfile
            ? `${storyLengthProfile.avgReadingTimeMinutes.toFixed(2)} min`
            : "n/a",
          icon: Gauge,
          description: "Stored readingTimeMinutes",
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
        <div className="flex flex-col items-end gap-1">
          <Button
            variant="outline"
            size="sm"
            onClick={load}
            disabled={loading}
          >
            {loading ? "Refreshing…" : "Refresh"}
          </Button>
          <span className="text-xs text-muted-foreground">Updated {lastUpdatedLabel}</span>
        </div>
      </div>

      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-sm font-medium">
            Story length profile
          </CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div className="inline-flex items-center rounded-md border p-1">
            <Button
              size="sm"
              variant={storyProfileDays === 7 ? "default" : "ghost"}
              onClick={() => setStoryProfileDays(7)}
            >
              7d
            </Button>
            <Button
              size="sm"
              variant={storyProfileDays === 30 ? "default" : "ghost"}
              onClick={() => setStoryProfileDays(30)}
            >
              30d
            </Button>
          </div>
          <p className="text-xs text-muted-foreground">
            Avg words and duration signal for generated stories.
          </p>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-sm font-medium">
            DORA query filters
          </CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <div className="w-full sm:w-32">
            <label className="mb-1 block text-xs text-muted-foreground">
              Days
            </label>
            <Input
              type="number"
              min={1}
              max={365}
              value={doraDays}
              onChange={(e) => setDoraDays(e.target.value)}
            />
          </div>
          <div className="w-full sm:flex-1">
            <label className="mb-1 block text-xs text-muted-foreground">
              Service
            </label>
            <Input
              value={doraService}
              onChange={(e) => setDoraService(e.target.value)}
              placeholder="backend"
            />
          </div>
          <div className="w-full sm:flex-1">
            <label className="mb-1 block text-xs text-muted-foreground">
              Environment
            </label>
            <Input
              value={doraEnvironment}
              onChange={(e) => setDoraEnvironment(e.target.value)}
              placeholder="production"
            />
          </div>
          <Button onClick={load} disabled={loading} className="sm:self-end">
            Apply DORA filters
          </Button>
        </CardContent>
      </Card>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {loading && !metrics
          ? Array.from({ length: 8 }).map((_, i) => (
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
            <div className="flex items-center justify-between gap-3">
              <CardTitle className="text-sm font-medium">
                Migration mode ({runtimeConfig.migrationMode})
              </CardTitle>
              <Button
                variant="outline"
                size="sm"
                onClick={copyDiagnostics}
                className="h-7 px-2 text-xs"
              >
                {copiedDiagnostics ? "Copied" : "Copy diagnostics"}
              </Button>
            </div>
          </CardHeader>
          <CardContent className="space-y-1 text-sm">
            <p>{runtimeConfig.operatorHint}</p>
            <p className="text-muted-foreground">
              Instance: {runtimeConfig.instanceId} | lockRetryCount:{" "}
              {runtimeConfig.flywayLockRetryCount}
            </p>
            <p className="text-muted-foreground">
              Story approval retention mode:{" "}
              <span className="font-mono">
                {runtimeConfig.storyApprovalRetentionMode ?? "STRICT_REVIEW_CYCLE"}
              </span>
            </p>
            {currentAdminEmail && (
              <p className="text-muted-foreground">
                Admin: <span className="font-mono">{currentAdminEmail}</span>
              </p>
            )}
            <p className="text-muted-foreground break-all">
              Admin API base: <span className="font-mono">{adminRequestBase}</span> (Next.js proxy)
            </p>
            {configuredPublicApiUrl && (
              <p className="text-muted-foreground break-all">
                Configured public backend URL:{" "}
                <span className="font-mono">{configuredPublicApiUrl}</span>
              </p>
            )}
            {runtimeConfig.datasourceTarget && (
              <p className="text-muted-foreground break-all">
                Datasource target: <span className="font-mono">{runtimeConfig.datasourceTarget}</span>
              </p>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
