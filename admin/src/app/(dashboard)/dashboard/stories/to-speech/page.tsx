"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { api, authStorage } from "@/lib/api";
import type { LibraryStorySummary, PagedResponse, PipelineStatusResponse } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { RefreshCw, Mic, ChevronDown, ChevronLeft, ChevronRight, Play, Pause, StopCircle } from "lucide-react";
import { useActionResult } from "@/contexts/action-result-context";
import { usePipelineActive } from "@/contexts/pipeline-active-context";
import { StoryProgressBar } from "@/components/design-system/story-progress-bar";
import { cn } from "@/lib/utils";

const PIPELINE_META_KEYS = ["processing", "progress", "overallStatus", "reviewedLanguages", "allLanguagesReviewed", "generatedAtIst", "durationSeconds", "audioCoverageWarnings", "failedLanguagesCount"];
const LANG_SHORT: Record<string, string> = { ta: "Ta", hi: "Hi", en: "En", te: "Te", kn: "Kn", ml: "Ml" };

function getCompletedLanguages(status?: PipelineStatusResponse | null): string[] {
  if (!status || typeof status !== "object") return [];
  return Object.entries(status)
    .filter(([k, s]) => !PIPELINE_META_KEYS.includes(k) && s === "COMPLETED")
    .map(([lang]) => lang);
}

/** Pipeline progress: completed count, total languages, estimated min left. */
function getPipelineProgress(status?: PipelineStatusResponse | null): { completed: number; total: number; estMinLeft: number } {
  if (!status || typeof status !== "object") return { completed: 0, total: 0, estMinLeft: 0 };
  const entries = Object.entries(status).filter(([k]) => !PIPELINE_META_KEYS.includes(k));
  const total = entries.length;
  const completed = entries.filter(([, s]) => s === "COMPLETED").length;
  const failed = entries.filter(([, s]) => s?.includes("FAILED")).length;
  const inProgress = entries.some(([, s]) =>
    ["PENDING", "TRANSLATING", "REWRITING", "TTS_PROCESSING"].includes(s ?? "") ||
    (s ?? "").startsWith("TRANSLATING") || (s ?? "").startsWith("REWRITING") || (s ?? "").startsWith("TTS_PROCESSING")
  );
  const pendingCount = total - completed - failed;
  const estMinLeft = inProgress && pendingCount > 0 ? pendingCount * 2 : 0; // ~2 min per language
  return { completed, total, estMinLeft };
}

const PAGE_SIZE = 20;
const POLL_INTERVAL_MS = 3000;
const POLL_DURATION_MS = 6 * 60 * 1000;
const SHOW_STARTING_FOR_MS = 60000;
/** Extra refetches after trigger so UI sees COMPLETED when pipeline completes (~1–2 min). */
const COMPLETION_REFETCH_DELAYS_MS = [70000, 110000];
/** When a status fetch fails (e.g. 401), retry after these delays to recover after token refresh. */
const STATUS_RETRY_DELAYS_MS = [2000, 4000, 6000];

/** Human-readable label for pipeline overallStatus. When all audio exists, show Completed. */
function progressLabel(status: string): string {
  switch (status) {
    case "TRANSLATING_LANGUAGES":
      return "Translating…";
    case "TTS_PROCESSING":
      return "Generating audio…";
    case "FINALIZING_STORY":
      return "Finalizing…";
    case "PENDING":
      return "Queued";
    case "FAILED":
      return "Failed";
    case "COMPLETED":
    case "READY_FOR_REVIEW":
      return "Completed";
    default:
      return status || "—";
  }
}

function isPipelineRunning(status: string): boolean {
  return ["TRANSLATING_LANGUAGES", "TTS_PROCESSING", "FINALIZING_STORY"].includes(status);
}

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  if (m >= 1 && s > 0) return `${m} min ${s} sec`;
  if (m >= 1) return `${m} min`;
  return `${s} sec`;
}

export default function StoryToSpeechTabPage() {
  const [mounted, setMounted] = useState(false);
  const [data, setData] = useState<PagedResponse<LibraryStorySummary> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [triggeringId, setTriggeringId] = useState<number | null>(null);
  const [pipelineStatusMap, setPipelineStatusMap] = useState<Record<number, PipelineStatusResponse>>({});
  const [pollUntil, setPollUntil] = useState<number | null>(null);
  const [recentlyTriggered, setRecentlyTriggered] = useState<{ storyId: number; at: number } | null>(null);
  const [previewLoadingId, setPreviewLoadingId] = useState<number | null>(null);
  const [playingAudio, setPlayingAudio] = useState<{ storyId: number; language: string } | null>(null);
  const [isPaused, setIsPaused] = useState(false);
  const audioRef = useRef<{ element: HTMLAudioElement; objectUrl: string } | null>(null);
  const { showSuccess, showError } = useActionResult();
  const { refresh: refreshPipelineActive, registerTriggered } = usePipelineActive();

  const load = useCallback((silent = false) => {
    if (typeof window === "undefined") return;
    if (!authStorage.getToken()) {
      if (!silent) setError("Please log in to view this page.");
      setLoading(false);
      return;
    }
    if (!silent) setLoading(true);
    setError(null);
    api.admin
      .getStoriesToSpeech(page, PAGE_SIZE)
      .then((res) => setData(res))
      .catch((e) => {
        if (!silent) {
          const msg = e instanceof Error ? e.message : "Failed to load";
          setError(msg.includes("Unauthorized") || msg.includes("401") ? "Session expired or unauthorized. Please log in again." : msg);
          setData(null);
        }
      })
      .finally(() => !silent && setLoading(false));
  }, [page]);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    if (mounted) load();
  }, [mounted, load]);

  const fetchPipelineStatus = useCallback(() => {
    if (!data?.content?.length) return;
    const ids = data.content.map((r) => r.id);
    api.admin
      .getLibraryStoryPipelineStatusBatch(ids)
      .then((batch) => {
        const next: Record<number, PipelineStatusResponse> = {};
        for (const [k, v] of Object.entries(batch || {})) {
          const id = typeof k === "string" ? parseInt(k, 10) : Number(k);
          if (!Number.isNaN(id) && v && typeof v === "object") next[id] = v as PipelineStatusResponse;
        }
        setPipelineStatusMap((m) => {
          const merged: Record<number, PipelineStatusResponse> = {};
          for (const [key, val] of Object.entries(m)) {
            const n = Number(key);
            if (!Number.isNaN(n)) merged[n] = val;
          }
          for (const [key, val] of Object.entries(next)) {
            const n = Number(key);
            if (!Number.isNaN(n)) merged[n] = val as PipelineStatusResponse;
          }
          return merged;
        });
        // When any visible story is in progress or queued, keep polling so progress bar updates
        const hasInProgress = ids.some((id) => {
          const s = (next[id] ?? batch?.[id])?.overallStatus;
          return isPipelineRunning(s ?? "") || (s ?? "") === "PENDING";
        });
        if (hasInProgress) {
          setPollUntil((prev) => {
            const deadline = Date.now() + POLL_DURATION_MS;
            return prev == null || prev < Date.now() ? deadline : prev;
          });
        }
      })
      .catch(() => {
        if (!data?.content?.length) return;
        STATUS_RETRY_DELAYS_MS.forEach((delayMs) => {
          setTimeout(() => fetchPipelineStatusRef.current(), delayMs);
        });
      });
  }, [data?.content]);

  const fetchPipelineStatusRef = useRef(fetchPipelineStatus);
  fetchPipelineStatusRef.current = fetchPipelineStatus;

  useEffect(() => {
    fetchPipelineStatus();
  }, [fetchPipelineStatus]);

  useEffect(() => {
    if (!data?.content?.length || pollUntil == null) return;
    const deadline = pollUntil;
    if (Date.now() >= deadline) {
      setPollUntil(null);
      return;
    }
    const t = setInterval(() => {
      if (Date.now() >= deadline) {
        setPollUntil(null);
        clearInterval(t);
        return;
      }
      fetchPipelineStatus();
      refreshPipelineActive();
    }, POLL_INTERVAL_MS);
    return () => clearInterval(t);
  }, [data?.content, pollUntil, fetchPipelineStatus, refreshPipelineActive]);

  useEffect(() => {
    if (recentlyTriggered == null) return;
    const t = setTimeout(() => setRecentlyTriggered(null), SHOW_STARTING_FOR_MS);
    return () => clearTimeout(t);
  }, [recentlyTriggered]);

  // After trigger, refetch at fixed delays so UI reliably sees COMPLETED when pipeline completes (~1–2 min).
  useEffect(() => {
    if (recentlyTriggered == null) return;
    const timers: ReturnType<typeof setTimeout>[] = [];
    for (const delayMs of COMPLETION_REFETCH_DELAYS_MS) {
      timers.push(
        setTimeout(() => {
          fetchPipelineStatusRef.current();
          refreshPipelineActive();
        }, delayMs)
      );
    }
    return () => timers.forEach((t) => clearTimeout(t));
  }, [recentlyTriggered, refreshPipelineActive]);

  const hasAudio = (id: number) => {
    const s = pipelineStatusMap[id]?.overallStatus;
    return s === "COMPLETED" || s === "READY_FOR_REVIEW";
  };

  const stopCurrentPlayback = useCallback(() => {
    const current = audioRef.current;
    if (current) {
      current.element.pause();
      current.element.currentTime = 0;
      URL.revokeObjectURL(current.objectUrl);
      audioRef.current = null;
    }
    setPlayingAudio(null);
    setIsPaused(false);
  }, []);

  useEffect(() => {
    return () => {
      if (audioRef.current) URL.revokeObjectURL(audioRef.current.objectUrl);
    };
  }, []);

  const handlePlayPreview = useCallback(
    async (id: number, language: string) => {
      stopCurrentPlayback();
      setPreviewLoadingId(id);
      try {
        const base = (typeof process !== "undefined" && process.env?.NEXT_PUBLIC_API_URL) || "";
        const url = `${base}/api/v1/admin/stories/${id}/preview-audio?language=${encodeURIComponent(language)}`;
        const token = typeof window !== "undefined" ? localStorage.getItem("admin_access_token") : null;
        const res = await fetch(url, {
          headers: token ? { Authorization: `Bearer ${token}` } : {},
        });
        if (!res.ok) {
          const err = await res.json().catch(() => ({}));
          const msg = (err as { message?: string }).message;
          throw new Error(msg ?? (res.status === 404 ? "Audio not ready for this language" : "Preview failed"));
        }
        const blob = await res.blob();
        const objectUrl = URL.createObjectURL(blob);
        const audio = new Audio(objectUrl);
        audio.addEventListener("ended", stopCurrentPlayback);
        audioRef.current = { element: audio, objectUrl };
        setPlayingAudio({ storyId: id, language });
        setIsPaused(false);
        await audio.play();
      } catch (e) {
        showError("Preview failed", e instanceof Error ? e.message : "Preview failed");
        setPlayingAudio(null);
        audioRef.current = null;
        // Refetch pipeline status for this story so "Done" / "Ready 100%" updates after backend cleared stale audio
        api.admin.getLibraryStoryPipelineStatusBatch([id]).then((batch) => {
          const next: Record<number, PipelineStatusResponse> = {};
          for (const [k, v] of Object.entries(batch || {})) {
            const sid = typeof k === "string" ? parseInt(k, 10) : k;
            if (!Number.isNaN(sid) && v && typeof v === "object") next[sid] = v as PipelineStatusResponse;
          }
          if (Object.keys(next).length > 0) setPipelineStatusMap((m) => ({ ...m, ...next }));
        }).catch(() => {});
      } finally {
        setPreviewLoadingId(null);
      }
    },
    [showError, stopCurrentPlayback]
  );

  const handlePausePreview = useCallback(() => {
    const current = audioRef.current;
    if (!current || !playingAudio) return;
    if (isPaused) {
      current.element.play();
      setIsPaused(false);
    } else {
      current.element.pause();
      setIsPaused(true);
    }
  }, [isPaused, playingAudio]);

  const handleStopPreview = useCallback(() => {
    stopCurrentPlayback();
  }, [stopCurrentPlayback]);

  const handleRetryFailed = async (storyId: number) => {
    setTriggeringId(storyId);
    registerTriggered(storyId);
    try {
      await api.admin.retryLibraryStory(storyId);
      showSuccess("Retry started", "Retrying failed languages. Progress will update below.");
      setRecentlyTriggered({ storyId, at: Date.now() });
      setPollUntil(Date.now() + POLL_DURATION_MS);
      fetchPipelineStatus();
      refreshPipelineActive();
      setTimeout(() => fetchPipelineStatus(), 1500);
      setTimeout(() => fetchPipelineStatus(), 4000);
    } catch (e) {
      showError("Retry failed", e instanceof Error ? e.message : "Failed to retry");
    } finally {
      setTriggeringId(null);
    }
  };

  const handleGenerateOrRegenerateAudio = async (storyId: number, languages?: string[]) => {
    setTriggeringId(storyId);
    registerTriggered(storyId); // Show banner immediately; backend poll will take over when active
    const isRegenerate = hasAudio(storyId);
    try {
      if (isRegenerate) {
        await api.admin.regenerateLibraryStoryNarration(
          storyId,
          languages?.length ? { languages } : undefined
        );
        const scope =
          languages?.length ? `flagged languages (${languages.map((l) => LANG_SHORT[l] ?? l).join(", ")})` : "all languages";
        showSuccess("Regenerate started", `${scope} — pipeline running. Progress will update below.`);
      } else {
        await api.admin.triggerLibraryStoryPipeline(storyId);
        showSuccess("Generate audio started", "Pipeline running. Progress will update below.");
      }
      setRecentlyTriggered({ storyId, at: Date.now() });
      setPollUntil(Date.now() + POLL_DURATION_MS);
      fetchPipelineStatus();
      refreshPipelineActive();
      const pollAgain = () => {
        fetchPipelineStatus();
        refreshPipelineActive();
      };
      setTimeout(pollAgain, 1500);
      setTimeout(pollAgain, 4000);
    } catch (e) {
      showError(isRegenerate ? "Regenerate failed" : "Generate failed", e instanceof Error ? e.message : "Failed to trigger pipeline");
    } finally {
      setTriggeringId(null);
    }
  };

  const rows = data?.content ?? [];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="page-header">Narration</h1>
        <p className="page-subheader mt-1">
          Only <strong>approved</strong> stories appear here. If a story has changes, it shows as <strong>Ready for review</strong> until approved; after approval you can use <strong>Generate audio</strong> or <strong>Regenerate audio</strong>. Enable <code className="text-xs bg-muted px-1 rounded">AUDIO_AFTER_APPROVAL=true</code> so Submit for review only saves content; approval happens in Story for review; then trigger audio here.
        </p>
      </div>

      <Card className="border-border shadow-sm overflow-hidden">
        <CardHeader className="card-header-responsive border-b bg-muted/30 px-4 py-3 sm:px-6 sm:py-4">
          <CardTitle className="text-base font-semibold tracking-tight">
            Approved stories — trigger TTS
          </CardTitle>
          <Button variant="outline" size="sm" className="h-8" onClick={() => load()} disabled={loading}>
            <RefreshCw className={cn("h-4 w-4 mr-2", loading && "animate-spin")} />
            Refresh
          </Button>
        </CardHeader>
        <CardContent className="p-0">
          {error && (
            <div className="px-6 py-4 bg-destructive/10 border-b">
              <p className="text-sm text-destructive">{error}</p>
              {(error.includes("log in") || error.includes("Unauthorized")) && (
                <Link href="/login" className="inline-block mt-2">
                  <Button variant="outline" size="sm">Go to login</Button>
                </Link>
              )}
            </div>
          )}
          {loading && !data ? (
            <div className="px-6 py-12 text-center text-muted-foreground">Loading…</div>
          ) : rows.length === 0 ? (
            <div className="px-6 py-12 text-center">
              <p className="text-muted-foreground mb-2">No approved stories.</p>
              <p className="text-sm text-muted-foreground mb-4">
                Approve stories in Story for review first; they will appear here for audio generation.
              </p>
              <Link href="/dashboard/stories/approve">
                <Button variant="outline" size="sm">Review</Button>
              </Link>
            </div>
          ) : (
            <>
              <div className="w-full overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border bg-[hsl(var(--table-header-bg))] text-[hsl(var(--table-header-foreground))] font-semibold">
                      <th className="w-16 px-4 py-3 text-right">ID</th>
                      <th className="min-w-[200px] px-4 py-3 text-left">Title</th>
                      <th className="w-40 px-4 py-3 text-left">Audio status</th>
                      <th className="w-20 px-4 py-3 text-right">Length</th>
                      <th className="w-28 px-4 py-3 text-left">Approved</th>
                      <th className="w-32 px-4 py-3 text-left">Preview</th>
                      <th className="w-40 px-4 py-3 text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {rows.map((row) => {
                      const rowId = Number(row.id);
                      const statusObj = pipelineStatusMap[rowId];
                      const status = statusObj?.overallStatus ?? "—";
                      const isTriggering = triggeringId === row.id;
                      const isRunning = isPipelineRunning(status);
                      const showStarting =
                        (recentlyTriggered?.storyId === row.id &&
                          Date.now() - recentlyTriggered.at < SHOW_STARTING_FOR_MS &&
                          status === "PENDING") ||
                        isTriggering;
                      const progressText = progressLabel(status);
                      const statusDisplay = showStarting ? "Starting…" : progressText;
                      const progressPct = statusObj?.progress;
                      const isMarkedComplete = status === "COMPLETED" || status === "READY_FOR_REVIEW";
                      // Do not show "100% ready" for marked-complete; show clear status only (reset if no audio).
                      const progressPercent =
                        !isMarkedComplete && progressPct != null && progressPct !== "" ? `${progressPct}%` : null;
                      const statusWithProgress =
                        progressPercent != null ? `${progressPercent} · ${statusDisplay}` : statusDisplay;
                      return (
                        <tr key={row.id} className="transition-colors hover:bg-muted/20">
                          <td className="px-4 py-3 font-mono text-right align-middle tabular-nums">{row.id}</td>
                          <td className="px-4 py-3 text-left align-middle">
                            <span className="truncate block max-w-[280px]" title={row.title ?? undefined}>
                              {row.title || "—"}
                            </span>
                          </td>
                          <td className="px-4 py-3 text-left align-middle text-muted-foreground text-xs">
                            {(isRunning || showStarting) ? (
                              <div className="flex flex-col gap-1.5 min-w-[140px] max-w-[240px]">
                                <div className="flex items-center justify-between gap-2">
                                  <span className="inline-flex items-center gap-1.5 text-xs font-medium">
                                    <RefreshCw className="h-3 w-3 animate-spin shrink-0" aria-hidden />
                                    {statusObj?.processing
                                      ? statusObj.processing === "starting"
                                        ? "Starting pipeline…"
                                        : `Generating ${LANG_SHORT[statusObj.processing] ?? statusObj.processing} audio…`
                                      : progressLabel(statusObj?.overallStatus ?? "")}
                                  </span>
                                  {(() => {
                                    const { completed, total, estMinLeft } = getPipelineProgress(statusObj);
                                    if (total === 0) return null;
                                    return (
                                      <span className="text-[10px] text-muted-foreground shrink-0">
                                        {completed}/{total} done
                                        {estMinLeft > 0 && ` · ~${estMinLeft} min left`}
                                      </span>
                                    );
                                  })()}
                                </div>
                                <div className="flex items-center gap-2">
                                  <StoryProgressBar
                                    value={Number(statusObj?.progress) || 0}
                                    max={100}
                                    variant="thin"
                                    showLabel={false}
                                    className="flex-1 min-w-0"
                                  />
                                  <span className="text-xs font-medium tabular-nums shrink-0 w-9 text-right">
                                    {Number(statusObj?.progress) || 0}%
                                  </span>
                                </div>
                              </div>
                            ) : (
                              <span className="block">
                                {statusWithProgress}
                                {statusObj?.generatedAtIst && isMarkedComplete && (
                                  <span className="block mt-0.5 text-muted-foreground/80" title="Audio generated at (IST)">
                                    {statusObj.generatedAtIst}
                                  </span>
                                )}
                                {statusObj?.audioCoverageWarnings && isMarkedComplete && (
                                  <span className="block mt-0.5 text-amber-600 dark:text-amber-500 text-xs font-medium" title="Audio may be truncated for these languages; consider Regenerate audio">
                                    ⚠ Possible truncation:{" "}
                                    {statusObj.audioCoverageWarnings
                                      .split(",")
                                      .map((l) => LANG_SHORT[l.trim().toLowerCase()] ?? l.trim())
                                      .join(", ")}
                                  </span>
                                )}
                                {statusObj?.failedLanguagesCount && Number(statusObj.failedLanguagesCount) > 0 && (
                                  <span className="block mt-0.5">
                                    <span className="text-destructive text-xs font-medium">
                                      ❌ {statusObj.failedLanguagesCount} language(s) failed.{" "}
                                    </span>
                                    <button
                                      type="button"
                                      onClick={() => handleRetryFailed(row.id)}
                                      disabled={!row.narrationApprovedAt || isTriggering || isRunning || showStarting}
                                      className="text-xs font-medium text-primary hover:underline"
                                    >
                                      Retry failed
                                    </button>
                                  </span>
                                )}
                              </span>
                            )}
                          </td>
                          <td className="px-4 py-3 text-right align-middle text-muted-foreground text-xs tabular-nums">
                            {statusObj?.durationSeconds
                              ? formatDuration(Number(statusObj.durationSeconds))
                              : "—"}
                          </td>
                          <td className="px-4 py-3 text-left align-middle text-muted-foreground text-sm whitespace-nowrap">
                            {row.narrationApprovedAt ? new Date(row.narrationApprovedAt).toLocaleDateString() : "—"}
                          </td>
                          <td className="px-4 py-3 text-left align-middle">
                            {playingAudio?.storyId === row.id ? (
                              <div className="flex items-center gap-1">
                                <span className="text-xs text-muted-foreground font-medium">
                                  {LANG_SHORT[playingAudio.language] ?? playingAudio.language}
                                </span>
                                <Button
                                  variant="outline"
                                  size="sm"
                                  className="h-7 w-7 p-0 shrink-0"
                                  onClick={handlePausePreview}
                                  title={isPaused ? "Resume" : "Pause"}
                                >
                                  {isPaused ? <Play className="h-3 w-3" /> : <Pause className="h-3 w-3" />}
                                </Button>
                                <Button
                                  variant="outline"
                                  size="sm"
                                  className="h-7 w-7 p-0 shrink-0 text-destructive hover:text-destructive"
                                  onClick={handleStopPreview}
                                  title="Stop"
                                >
                                  <StopCircle className="h-3 w-3" />
                                </Button>
                              </div>
                            ) : hasAudio(rowId) ? (
                              <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    className="h-8 w-8 p-0 text-muted-foreground hover:text-foreground"
                                    disabled={previewLoadingId === row.id}
                                    title="Preview audio — choose language"
                                  >
                                    {previewLoadingId === row.id ? (
                                      <RefreshCw className="h-4 w-4 animate-spin" />
                                    ) : (
                                      <Play className="h-4 w-4" />
                                    )}
                                  </Button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="start" className="min-w-[120px]">
                                  {(() => {
                                    const completedLangs = getCompletedLanguages(statusObj);
                                    const langs = completedLangs.length > 0 ? completedLangs : Object.keys(LANG_SHORT);
                                    return langs.map((lang) => (
                                      <DropdownMenuItem
                                        key={lang}
                                        onClick={() => handlePlayPreview(row.id, lang)}
                                      >
                                        <Play className="h-3.5 w-3.5 mr-2 shrink-0" />
                                        {LANG_SHORT[lang] ?? lang}
                                      </DropdownMenuItem>
                                    ));
                                  })()}
                                </DropdownMenuContent>
                              </DropdownMenu>
                            ) : (
                              <span className="text-muted-foreground text-xs">—</span>
                            )}
                          </td>
                          <td className="px-4 py-3 text-right align-middle">
                            <div className="flex items-center justify-end gap-2">
                              {hasAudio(rowId) && statusObj?.audioCoverageWarnings && isMarkedComplete ? (
                                <DropdownMenu>
                                  <DropdownMenuTrigger asChild>
                                    <Button
                                      size="sm"
                                      variant="default"
                                      className="h-8 min-w-[140px]"
                                      disabled={!row.narrationApprovedAt || isTriggering || isRunning || showStarting}
                                    >
                                      {isTriggering ? (
                                        <>
                                          <RefreshCw className="h-3.5 w-3.5 animate-spin mr-1.5" aria-hidden />
                                          Starting…
                                        </>
                                      ) : isRunning || showStarting ? (
                                        <>
                                          <RefreshCw className="h-3.5 w-3.5 animate-spin mr-1.5" aria-hidden />
                                          {statusDisplay}
                                        </>
                                      ) : (
                                        <>
                                          <Mic className="h-3.5 w-3.5 mr-1.5" />
                                          Regenerate
                                          <ChevronDown className="h-3.5 w-3.5 ml-1 opacity-70" />
                                        </>
                                      )}
                                    </Button>
                                  </DropdownMenuTrigger>
                                  <DropdownMenuContent align="end">
                                    <DropdownMenuItem
                                      onClick={() => {
                                        const flaggedLangs = statusObj.audioCoverageWarnings!
                                          .split(",")
                                          .map((l) => l.trim().toLowerCase())
                                          .filter(Boolean);
                                        handleGenerateOrRegenerateAudio(row.id, flaggedLangs);
                                      }}
                                    >
                                      Regenerate flagged (
                                      {statusObj.audioCoverageWarnings
                                        .split(",")
                                        .map((l) => LANG_SHORT[l.trim().toLowerCase()] ?? l.trim())
                                        .join(", ")}
                                      )
                                    </DropdownMenuItem>
                                    <DropdownMenuItem onClick={() => handleGenerateOrRegenerateAudio(row.id)}>
                                      Regenerate all languages
                                    </DropdownMenuItem>
                                  </DropdownMenuContent>
                                </DropdownMenu>
                              ) : (
                                <Button
                                  size="sm"
                                  variant="default"
                                  className="h-8 min-w-[140px]"
                                  disabled={!row.narrationApprovedAt || isTriggering || isRunning || showStarting}
                                  onClick={() => handleGenerateOrRegenerateAudio(row.id)}
                                  title={!row.narrationApprovedAt ? "Approve the story first in Story for review" : hasAudio(rowId) ? "Clear old audio and run TTS pipeline for all languages" : "Run TTS pipeline for all languages"}
                                >
                                  {isTriggering ? (
                                    <>
                                      <RefreshCw className="h-3.5 w-3.5 animate-spin mr-1.5" aria-hidden />
                                      Starting…
                                    </>
                                  ) : isRunning || showStarting ? (
                                    <>
                                      <RefreshCw className="h-3.5 w-3.5 animate-spin mr-1.5" aria-hidden />
                                      {statusDisplay}
                                    </>
                                  ) : (
                                    <>
                                      <Mic className="h-3.5 w-3.5 mr-1.5" />
                                      {hasAudio(rowId) ? "Regenerate audio" : "Generate audio"}
                                    </>
                                  )}
                                </Button>
                              )}
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
              {data && (data.totalPages > 1 || data.page > 0) && (
                <div className="px-6 py-3 border-t bg-muted/20 flex items-center justify-between text-sm">
                  <span className="text-muted-foreground">
                    Page {data.page + 1} of {Math.max(1, data.totalPages)}
                    {data.totalElements > 0 && (
                      <span className="ml-2">
                        ({data.totalElements} story{data.totalElements === 1 ? "" : "s"} total)
                      </span>
                    )}
                  </span>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      className="h-8"
                      disabled={data.first || loading}
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                    >
                      <ChevronLeft className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      className="h-8"
                      disabled={data.last || loading}
                      onClick={() => setPage((p) => p + 1)}
                    >
                      <ChevronRight className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
