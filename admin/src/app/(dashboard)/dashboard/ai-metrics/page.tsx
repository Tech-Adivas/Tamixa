"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { AiMetricsDto } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

export default function AiMetricsPage() {
  const [data, setData] = useState<AiMetricsDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = () => {
    setLoading(true);
    setError(null);
    api.admin
      .getAiMetrics()
      .then(setData)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">AI usage metrics</h1>
        <p className="text-muted-foreground">
          Story generation and OpenAI token usage (when tracked by backend).
        </p>
      </div>
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle>Metrics</CardTitle>
          <Button variant="outline" size="sm" onClick={load} disabled={loading}>
            {loading ? "Refreshing…" : "Refresh"}
          </Button>
        </CardHeader>
        <CardContent>
          {error && (
            <p className="mb-4 text-sm text-destructive">{error}</p>
          )}
          {!error && data && (
            <dl className="grid gap-4 sm:grid-cols-2">
              <div>
                <dt className="text-sm text-muted-foreground">Story generations (total)</dt>
                <dd className="text-2xl font-semibold">{data.storyGenerationsTotal}</dd>
              </div>
              <div>
                <dt className="text-sm text-muted-foreground">Cache hits</dt>
                <dd className="text-2xl font-semibold">{data.cacheHits}</dd>
              </div>
              <div>
                <dt className="text-sm text-muted-foreground">Cache misses</dt>
                <dd className="text-2xl font-semibold">{data.cacheMisses}</dd>
              </div>
              <div>
                <dt className="text-sm text-muted-foreground">Voice processing count</dt>
                <dd className="text-2xl font-semibold">{data.voiceProcessingCount}</dd>
              </div>
              <div>
                <dt className="text-sm text-muted-foreground">OpenAI tokens used</dt>
                <dd className="text-2xl font-semibold">
                  {data.openaiTokensUsed ?? "—"}
                </dd>
              </div>
            </dl>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
