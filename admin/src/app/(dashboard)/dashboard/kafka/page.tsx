"use client";

import { useEffect, useState } from "react";
import { api } from "@/lib/api";
import type { PagedResponse } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";

const PAGE_SIZE = 20;

export default function KafkaPage() {
  const [data, setData] = useState<PagedResponse<Record<string, unknown>> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    setError(null);
    api.admin
      .getKafkaEvents(page, PAGE_SIZE)
      .then(setData)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [page]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Kafka event monitoring</h1>
        <p className="text-muted-foreground">
          Event stream is observed via backend logs and metrics. In production, connect to your log aggregator or Kafka UI.
        </p>
      </div>
      <Card>
        <CardHeader>
          <CardTitle>Recent events</CardTitle>
        </CardHeader>
        <CardContent>
          {error && (
            <p className="mb-4 text-sm text-destructive">{error}</p>
          )}
          {loading ? (
            <div className="space-y-3">
              <Skeleton className="h-32 w-full" />
              <Skeleton className="h-4 w-48" />
              <Skeleton className="h-4 w-64" />
            </div>
          ) : data && data.content.length === 0 ? (
            <p className="text-muted-foreground">
              No in-app event store. Use backend logs or Prometheus for story-created and audio processing events.
            </p>
          ) : data ? (
            <p className="text-muted-foreground">
              {data.totalElements} events (stub). Integrate with Kafka consumer or log stream for live data.
            </p>
          ) : null}
            <div className="mt-4 flex gap-2">
              <Button
                variant="outline"
                size="sm"
                disabled={data?.first ?? true}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Previous
              </Button>
              <Button
                variant="outline"
                size="sm"
                disabled={data?.last ?? true}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </Button>
            </div>
        </CardContent>
      </Card>
    </div>
  );
}
