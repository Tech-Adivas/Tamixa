"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import type { LifeSkillChoiceAnalyticsResponse } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import { useActionResult } from "@/contexts/action-result-context";
import { ExternalLink, RefreshCw } from "lucide-react";

const DAY_OPTIONS = [7, 30, 90] as const;

export default function EduSimulatorAnalyticsPage() {
  const { showError } = useActionResult();
  const [days, setDays] = useState<number>(30);
  const [data, setData] = useState<LifeSkillChoiceAnalyticsResponse | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.admin.getEduLifeSkillChoiceAnalytics(days);
      setData(res);
    } catch (e) {
      showError(
        "Load failed",
        e instanceof Error ? e.message : "Failed to load Edu simulator analytics"
      );
      setData(null);
    } finally {
      setLoading(false);
    }
  }, [days, showError]);

  useEffect(() => {
    void load();
  }, [load]);

  return (
    <div className="space-y-6 p-4 md:p-6 max-w-6xl mx-auto">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Edu simulator analytics</h1>
          <p className="text-sm text-muted-foreground mt-1">
            Aggregated taps on interactive library episodes (<code className="text-xs">life_skill_choice_events</code>).
            No per-child breakdown; use story edit to inspect graph and copy.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Select value={String(days)} onValueChange={(v) => setDays(Number(v))}>
            <SelectTrigger className="w-[140px]">
              <SelectValue placeholder="Period" />
            </SelectTrigger>
            <SelectContent>
              {DAY_OPTIONS.map((d) => (
                <SelectItem key={d} value={String(d)}>
                  Last {d} days
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <Button type="button" variant="outline" size="icon" onClick={() => void load()} disabled={loading}>
            <RefreshCw className={`h-4 w-4 ${loading ? "animate-spin" : ""}`} />
          </Button>
        </div>
      </div>

      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base">Summary</CardTitle>
        </CardHeader>
        <CardContent>
          {loading && !data ? (
            <Skeleton className="h-8 w-48" />
          ) : (
            <p className="text-sm">
              <span className="font-medium">{data?.totalEvents?.toLocaleString() ?? "—"}</span> choice events in the
              selected window (up to 500 segment×choice buckets shown below).
            </p>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="pb-3">
          <CardTitle className="text-base">By story, segment, and choice</CardTitle>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          {loading && !data ? (
            <Skeleton className="h-64 w-full" />
          ) : !data?.rows?.length ? (
            <p className="text-sm text-muted-foreground">No events in this period.</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Count</TableHead>
                  <TableHead>Story</TableHead>
                  <TableHead>Segment</TableHead>
                  <TableHead>Choice id</TableHead>
                  <TableHead className="text-right">Edit</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.rows.map((row, i) => (
                  <TableRow key={`${row.libraryStoryId}-${row.segmentId}-${row.choiceId}-${i}`}>
                    <TableCell className="font-medium tabular-nums">{row.eventCount.toLocaleString()}</TableCell>
                    <TableCell>
                      <div className="flex flex-col gap-0.5">
                        <span className="text-muted-foreground text-xs">#{row.libraryStoryId}</span>
                        <span>{row.storyTitle?.trim() || "—"}</span>
                      </div>
                    </TableCell>
                    <TableCell>
                      <code className="text-xs bg-muted px-1.5 py-0.5 rounded">{row.segmentId}</code>
                    </TableCell>
                    <TableCell>
                      <code className="text-xs bg-muted px-1.5 py-0.5 rounded">{row.choiceId}</code>
                    </TableCell>
                    <TableCell className="text-right">
                      <Button variant="ghost" size="sm" asChild>
                        <Link href={`/dashboard/stories/${row.libraryStoryId}/edit`}>
                          Open
                          <ExternalLink className="ml-1 h-3.5 w-3.5 opacity-70" />
                        </Link>
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
