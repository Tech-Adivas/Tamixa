"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import type { StoryWithIssues } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { RefreshCw, Pencil, PlayCircle, AlertTriangle } from "lucide-react";
import { useAuth } from "@/contexts/auth-context";
import { useActionResult } from "@/contexts/action-result-context";
import { usePipelineActive } from "@/contexts/pipeline-active-context";
import { canManageStories } from "@/lib/admin-roles";
import { adminStoryLanguageLabel } from "@/lib/library-story-workflow";

export default function StoriesWithIssuesPage() {
  const { user } = useAuth();
  const canManage = canManageStories(user ?? null);
  const [stories, setStories] = useState<StoryWithIssues[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [triggeringId, setTriggeringId] = useState<number | null>(null);
  const { showSuccess, showError } = useActionResult();
  const { registerTriggered } = usePipelineActive();

  const load = (silent = false) => {
    if (!silent) setLoading(true);
    setError(null);
    api.admin
      .getStoriesWithIssues()
      .then((data) => setStories(data))
      .catch((e) => setError(e instanceof Error ? e.message : "Failed to load"))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    const id = setInterval(() => {
      if (document.visibilityState === "visible") load(true);
    }, 10000);
    const onVisibility = () => {
      if (document.visibilityState === "visible") load(true);
    };
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      clearInterval(id);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, []);

  const handleTriggerPipeline = async (id: number) => {
    if (!canManage) return;
    setTriggeringId(id);
    registerTriggered(id); // Show pipeline banner immediately
    try {
      await api.admin.triggerLibraryStoryPipeline(id);
      showSuccess("Pipeline started", "Pipeline is running. Check Story for review in a few minutes.");
      load();
    } catch (e) {
      showError("Trigger failed", e instanceof Error ? e.message : "Could not trigger pipeline");
    } finally {
      setTriggeringId(null);
    }
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-semibold tracking-tight">Pipeline triage</h1>
        <p className="text-muted-foreground">Loading…</p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Pipeline triage</h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            Stories with pipeline failures (translation, rewrite, or TTS). Fix and retry or trigger pipeline.
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => load()} disabled={loading}>
          <RefreshCw className="h-4 w-4 mr-1" /> Refresh
        </Button>
      </div>

      {error && (
        <p className="text-destructive text-sm">{error}</p>
      )}

      {stories.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <AlertTriangle className="h-12 w-12 mx-auto text-muted-foreground/50 mb-3" />
            <p className="text-muted-foreground font-medium">Nothing in triage</p>
            <p className="text-sm text-muted-foreground mt-1">
              All pipeline translations are healthy. Check{" "}
              <Link href="/dashboard/story-for-review" className="text-primary underline">
                Story for review
              </Link>{" "}
              or{" "}
              <Link href="/dashboard/stories" className="text-primary underline">
                Story library
              </Link>
              .
            </p>
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">{stories.length} story{stories.length === 1 ? "" : "s"} in triage</CardTitle>
            <p className="text-sm text-muted-foreground">
              Click Edit to fix content, or Run pipeline to retry translation/TTS.
            </p>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Story</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Issues</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {stories.map((s) => (
                  <TableRow key={s.storyId}>
                    <TableCell>
                      <div className="font-medium">{s.title}</div>
                      <div className="text-xs text-muted-foreground">
                        #{s.storyId} · {s.theme || "—"}
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="secondary">{s.status}</Badge>
                    </TableCell>
                    <TableCell>
                      <div className="space-y-1 max-w-md">
                        {s.issues.map((i) => (
                          <div
                            key={`${s.storyId}-${i.language}`}
                            className="text-sm"
                          >
                            <span className="font-medium text-destructive">
                              {adminStoryLanguageLabel(i.language)}
                            </span>
                            {" "}({i.status}):{" "}
                            <span className="text-muted-foreground" title={i.error}>
                              {i.error.length > 80 ? `${i.error.slice(0, 80)}…` : i.error}
                            </span>
                          </div>
                        ))}
                      </div>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Link href={`/dashboard/stories/${s.storyId}/edit`}>
                          <Button variant="outline" size="sm">
                            <Pencil className="h-3 w-3 mr-1" /> Edit
                          </Button>
                        </Link>
                        {canManage && (
                          <Button
                            variant="default"
                            size="sm"
                            onClick={() => handleTriggerPipeline(s.storyId)}
                            disabled={triggeringId === s.storyId}
                          >
                            {triggeringId === s.storyId ? (
                              <RefreshCw className="h-3 w-3 mr-1 animate-spin" />
                            ) : (
                              <PlayCircle className="h-3 w-3 mr-1" />
                            )}
                            Run pipeline
                          </Button>
                        )}
                      </div>
                    </TableCell>
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
