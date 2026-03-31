"use client";

import { useEffect, useState, useCallback } from "react";
import { api } from "@/lib/api";
import type { AiMetricsDto, ApiUsageDto, StoryAiUsageDto } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { PageHeader } from "@/components/layout/page-header";
import { Cpu, Lightbulb, BarChart3 } from "lucide-react";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { formatUsdToInr, formatInr } from "@/lib/currency";

const AUTO_REFRESH_MS = 15_000;

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

function formatTokens(value: number | null | undefined): string {
  if (value == null) return "—";
  if (value >= 1_000_000) return `${(value / 1_000_000).toFixed(2)}M`;
  if (value >= 1_000) return `${(value / 1_000).toFixed(2)}K`;
  return String(value);
}

function formatTokensOrChars(value: number | null | undefined): string {
  if (value == null || value === 0) return "—";
  return formatTokens(value);
}

export default function AiMetricsPage() {
  const [data, setData] = useState<AiMetricsDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [lastUpdatedAt, setLastUpdatedAt] = useState<Date | null>(null);
  const [nowMs, setNowMs] = useState(() => Date.now());

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    api.admin
      .getAiMetrics()
      .then((result) => {
        setData(result);
        setLastUpdatedAt(new Date());
      })
      .catch((e) => {
        const msg = e?.message ?? String(e);
        setError(msg.includes("403") || msg.includes("Forbidden") ? "You don’t have permission to view AI metrics." : msg);
      })
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      if (!document.hidden) {
        load();
      }
    }, AUTO_REFRESH_MS);
    return () => window.clearInterval(timer);
  }, [load]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setNowMs(Date.now());
    }, 1000);
    return () => window.clearInterval(timer);
  }, []);

  const cacheTotal = data ? data.cacheHits + data.cacheMisses : 0;
  const cacheHitRate = cacheTotal > 0 ? ((data!.cacheHits / cacheTotal) * 100).toFixed(1) : null;

  const apiBreakdown = data?.apiBreakdown ?? [];
  const storyBreakdown = data?.storyBreakdown ?? [];
  const lastUpdatedLabel = formatUpdatedAgo(lastUpdatedAt, nowMs);

  return (
    <div className="space-y-6">
      <PageHeader
        title="AI usage metrics"
        description="Story generation, cache, voice processing, and per-API usage from the backend."
        breadcrumbs
      />
      <Card className="border-primary/30 bg-primary/5">
        <CardContent className="pt-6">
          <div className="flex gap-3">
            <Lightbulb className="h-5 w-5 shrink-0 text-primary" />
            <div className="text-sm">
              <p className="font-medium text-foreground">Recommendation</p>
              <p className="text-muted-foreground mt-0.5">
                Use <strong>Google Cloud TTS</strong> for default and cloned (Tamil) narration for best cost and quality.
                Add Vertex/Gemini-TTS only if you need prompt-driven style. Avatar video: HeyGen primary; Replicate/D-ID as fallbacks.
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="flex items-center gap-2">
            <Cpu className="h-5 w-5 text-muted-foreground" />
            Metrics
          </CardTitle>
          <div className="flex flex-col items-end gap-1">
            <Button variant="outline" size="sm" onClick={load} disabled={loading}>
              {loading ? "Refreshing…" : "Refresh"}
            </Button>
            <span className="text-xs text-muted-foreground">Updated {lastUpdatedLabel}</span>
          </div>
        </CardHeader>
        <CardContent>
          {error && (
            <p className="mb-4 text-sm text-destructive">{error}</p>
          )}
          {loading && !data && (
            <div className="grid gap-4 sm:grid-cols-2">
              {[1, 2, 3, 4, 5, 6].map((i) => (
                <div key={i} className="space-y-2">
                  <Skeleton className="h-4 w-32" />
                  <Skeleton className="h-8 w-20" />
                </div>
              ))}
            </div>
          )}
          {!error && data && !loading && (
            <dl className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <div>
                <dt className="text-sm text-muted-foreground">Story generations (total)</dt>
                <dd className="text-2xl font-semibold">{data.storyGenerationsTotal.toLocaleString()}</dd>
              </div>
              <div>
                <dt className="text-sm text-muted-foreground">Cache hits</dt>
                <dd className="text-2xl font-semibold">{data.cacheHits.toLocaleString()}</dd>
              </div>
              <div>
                <dt className="text-sm text-muted-foreground">Cache misses</dt>
                <dd className="text-2xl font-semibold">{data.cacheMisses.toLocaleString()}</dd>
              </div>
              {cacheHitRate != null && (
                <div>
                  <dt className="text-sm text-muted-foreground">Cache hit rate</dt>
                  <dd className="text-2xl font-semibold">{cacheHitRate}%</dd>
                </div>
              )}
              <div>
                <dt className="text-sm text-muted-foreground">Voice processing count</dt>
                <dd className="text-2xl font-semibold">{data.voiceProcessingCount.toLocaleString()}</dd>
              </div>
              <div>
                <dt className="text-sm text-muted-foreground">OpenAI tokens used</dt>
                <dd className="text-2xl font-semibold">{formatTokens(data.openaiTokensUsed)}</dd>
              </div>
            </dl>
          )}
        </CardContent>
      </Card>

      {apiBreakdown.length > 0 && (
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="flex items-center gap-2">
              <BarChart3 className="h-5 w-5 text-muted-foreground" />
              Per API
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>API</TableHead>
                  <TableHead className="text-right">Requests</TableHead>
                  <TableHead className="text-right">Tokens / characters</TableHead>
                  <TableHead className="text-right">Cost (₹)</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {apiBreakdown.map((row: ApiUsageDto) => (
                  <TableRow key={row.api}>
                    <TableCell className="font-medium">{row.displayName}</TableCell>
                    <TableCell className="text-right">{row.requests.toLocaleString()}</TableCell>
                    <TableCell className="text-right">{formatTokensOrChars(row.tokensOrCharacters)}</TableCell>
                    <TableCell className="text-right">{formatUsdToInr(row.costEstimateUsd)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}

      {storyBreakdown.length > 0 && (
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="flex items-center gap-2">
              <BarChart3 className="h-5 w-5 text-muted-foreground" />
              Per story (top by tokens)
            </CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Story ID</TableHead>
                  <TableHead>Title</TableHead>
                  <TableHead className="text-right">Tokens</TableHead>
                  <TableHead className="text-right">Avatar videos</TableHead>
                  <TableHead className="text-right">Cost (₹)</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {storyBreakdown.map((row: StoryAiUsageDto) => (
                  <TableRow key={row.storyId}>
                    <TableCell className="font-mono text-sm">{row.storyId}</TableCell>
                    <TableCell className="max-w-[200px] truncate" title={row.title ?? undefined}>
                      {row.title ?? "—"}
                    </TableCell>
                    <TableCell className="text-right">{formatTokensOrChars(row.totalTokens)}</TableCell>
                    <TableCell className="text-right">{row.avatarVideoCount.toLocaleString()}</TableCell>
                    <TableCell className="text-right">{formatInr(row.costInr)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
