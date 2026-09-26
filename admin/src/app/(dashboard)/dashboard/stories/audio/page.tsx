"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { api, authStorage } from "@/lib/api";
import type { LibraryStorySummary, PagedResponse, PipelineStatusResponse } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  DropdownMenuSeparator,
  DropdownMenuLabel,
} from "@/components/ui/dropdown-menu";
import { RefreshCw, Mic, ChevronDown, ChevronLeft, ChevronRight, Play, CheckCircle, Eye } from "lucide-react";
import { useActionResult } from "@/contexts/action-result-context";
import { usePipelineActive } from "@/contexts/pipeline-active-context";
import { PipelineStatusBadges } from "@/components/design-system/pipeline-status-badge";
import { AudioPreviewModal } from "@/components/audio-preview-modal";
import { AudioApprovalDialog } from "@/components/audio-approval-dialog";
import { cn } from "@/lib/utils";
import {
  isLibraryStoryPipelineActivelyRunning,
  ADMIN_STORY_LANGUAGE_SHORT,
  adminStoryLanguageLabel,
} from "@/lib/library-story-workflow";

const PAGE_SIZE = 20;
const POLL_INTERVAL_MS = 3000;
const POLL_DURATION_MS = 6 * 60 * 1000;

/** Supported languages for audio generation */
const SUPPORTED_LANGUAGES = ["ta", "en", "hi", "te", "kn", "ml"] as const;

function getLanguageStatus(status?: PipelineStatusResponse | null, language: string): string {
  if (!status || typeof status !== "object") return "PENDING";
  return status[language] ?? "PENDING";
}

function isLanguageCompleted(status?: PipelineStatusResponse | null, language: string): boolean {
  return getLanguageStatus(status, language) === "COMPLETED";
}

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  if (m >= 1 && s > 0) return `${m}m ${s}s`;
  if (m >= 1) return `${m}m`;
  return `${s}s`;
}

function mapErrorMessage(raw: unknown): string {
  const msg = (raw instanceof Error ? raw.message : String(raw ?? "")).trim();
  const lower = msg.toLowerCase();
  if (!msg) return "Operation failed";
  if (lower.includes("not approved yet") || lower.includes("not in ready status")) {
    return "Story must be in READY status before audio generation. Approve the story first in Content Review.";
  }
  if (lower.includes("not found")) {
    return "Story not found. Refresh the list and try again.";
  }
  if (lower.includes("unauthorized") || lower.includes("session expired") || lower.includes("401")) {
    return "Session expired. Please log in again.";
  }
  return msg;
}

export default function AudioGenerationPage() {
  const searchParams = useSearchParams();
  const storyIdParam = searchParams?.get("storyId")?.trim() ?? "";
  const focusedStoryId = /^\d+$/.test(storyIdParam) ? Number(storyIdParam) : null;

  const [mounted, setMounted] = useState(false);
  const [data, setData] = useState<PagedResponse<LibraryStorySummary> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [triggeringId, setTriggeringId] = useState<number | null>(null);
  const [pipelineStatusMap, setPipelineStatusMap] = useState<Record<number, PipelineStatusResponse>>({});
  const [pollUntil, setPollUntil] = useState<number | null>(null);
  
  // Audio preview modal state
  const [previewModal, setPreviewModal] = useState<{
    open: boolean;
    storyId: number;
    storyTitle: string;
    language: string;
    duration?: number;
    fileSize?: number;
    generatedAt?: string;
  } | null>(null);

  // Audio approval dialog state
  const [approvalDialog, setApprovalDialog] = useState<{
    open: boolean;
    storyId: number;
    storyTitle: string;
    language: string;
    duration?: number;
    fileSize?: number;
    generatedAt?: string;
  } | null>(null);
  const [isApproving, setIsApproving] = useState(false);

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

    // Fetch stories with status=PUBLISHED (approved for audio generation)
    api.admin
      .getLibraryStories(page, PAGE_SIZE, "PUBLISHED")
      .then((res) => setData(res))
      .catch((e) => {
        if (!silent) {
          const msg = e instanceof Error ? e.message : "Failed to load";
          setError(
            msg.includes("Unauthorized") || msg.includes("401")
              ? "Session expired or unauthorized. Please log in again."
              : msg
          );
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
        setPipelineStatusMap((m) => ({ ...m, ...next }));

        // Keep polling if any story is actively processing
        const hasInProgress = ids.some((id) => {
          const st = next[id];
          return isLibraryStoryPipelineActivelyRunning(st ?? null);
        });
        if (hasInProgress) {
          setPollUntil((prev) => {
            const deadline = Date.now() + POLL_DURATION_MS;
            return prev == null || prev < Date.now() ? deadline : prev;
          });
        }
      })
      .catch(() => {});
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

  // Open preview modal
  const handleOpenPreview = useCallback((
    storyId: number,
    storyTitle: string,
    language: string,
    statusObj?: PipelineStatusResponse
  ) => {
    setPreviewModal({
      open: true,
      storyId,
      storyTitle,
      language,
      duration: statusObj?.durationSeconds ? Number(statusObj.durationSeconds) : undefined,
      fileSize: statusObj?.fileSizeBytes ? Number(statusObj.fileSizeBytes) : undefined,
      generatedAt: statusObj?.generatedAtIst,
    });
  }, []);

  // Open approval dialog
  const handleOpenApproval = useCallback((
    storyId: number,
    storyTitle: string,
    language: string,
    statusObj?: PipelineStatusResponse
  ) => {
    setApprovalDialog({
      open: true,
      storyId,
      storyTitle,
      language,
      duration: statusObj?.durationSeconds ? Number(statusObj.durationSeconds) : undefined,
      fileSize: statusObj?.fileSizeBytes ? Number(statusObj.fileSizeBytes) : undefined,
      generatedAt: statusObj?.generatedAtIst,
    });
  }, []);

  const handleGenerateAudio = async (storyId: number, languages?: string[]) => {
    setTriggeringId(storyId);
    registerTriggered(storyId);
    try {
      if (languages && languages.length > 0) {
        await api.admin.regenerateLibraryStoryNarration(storyId, { languages });
        const scope = languages.map((l) => ADMIN_STORY_LANGUAGE_SHORT[l] ?? l).join(", ");
        showSuccess("Audio generation started", `Generating audio for ${scope}. Progress will update below.`);
      } else {
        await api.admin.triggerLibraryStoryPipeline(storyId);
        showSuccess("Audio generation started", "Generating audio for all languages. Progress will update below.");
      }
      setPollUntil(Date.now() + POLL_DURATION_MS);
      fetchPipelineStatus();
      refreshPipelineActive();
      setTimeout(() => fetchPipelineStatus(), 1500);
      setTimeout(() => fetchPipelineStatus(), 4000);
    } catch (e) {
      showError("Generation failed", mapErrorMessage(e));
    } finally {
      setTriggeringId(null);
    }
  };

  const handleApproveNarration = async (storyId: number, language: string) => {
    setIsApproving(true);
    try {
      await api.admin.approveLibraryStoryTranslationNarration(storyId, language);
      showSuccess("Narration approved", `${adminStoryLanguageLabel(language)} narration approved successfully.`);
      load(true);
    } catch (e) {
      showError("Approval failed", mapErrorMessage(e));
      throw e; // Re-throw to let dialog handle error
    } finally {
      setIsApproving(false);
    }
  };

  const rows = data?.content ?? [];
  const visibleRows = focusedStoryId == null ? rows : rows.filter((row) => row.id === focusedStoryId);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="page-header">Audio Generation</h1>
        <p className="page-subheader mt-1">
          Generate and manage TTS (text-to-speech) audio for approved stories across all supported languages.
          Stories must be in <strong>PUBLISHED</strong> status (approved for audio generation).
        </p>
      </div>

      <Card className="border-border shadow-sm overflow-hidden">
        <CardHeader className="card-header-responsive border-b bg-muted/30 px-4 py-3 sm:px-6 sm:py-4">
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-base font-semibold tracking-tight">
                Stories Ready for Audio Generation
              </CardTitle>
              {focusedStoryId != null && (
                <span className="text-xs text-muted-foreground mt-1">
                  Filtered to story #{focusedStoryId}{" "}
                  <Link className="underline-offset-2 hover:underline" href="/dashboard/stories/audio">
                    Clear
                  </Link>
                </span>
              )}
            </div>
            <Button variant="outline" size="sm" className="h-8" onClick={() => load()} disabled={loading}>
              <RefreshCw className={cn("h-4 w-4 mr-2", loading && "animate-spin")} />
              Refresh
            </Button>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          {error && (
            <div className="px-6 py-4 bg-destructive/10 border-b">
              <p className="text-sm text-destructive">{error}</p>
              {(error.includes("log in") || error.includes("Unauthorized")) && (
                <Link href="/login" className="inline-block mt-2">
                  <Button variant="outline" size="sm">
                    Go to login
                  </Button>
                </Link>
              )}
            </div>
          )}
          {loading && !data ? (
            <div className="px-6 py-12 text-center text-muted-foreground">Loading…</div>
          ) : visibleRows.length === 0 ? (
            <div className="px-6 py-12 text-center">
              <p className="text-muted-foreground mb-2">
                {focusedStoryId != null
                  ? `Story #${focusedStoryId} is not currently in the audio generation queue.`
                  : "No stories ready for audio generation."}
              </p>
              <p className="text-sm text-muted-foreground mb-4">
                Stories must be approved and in PUBLISHED status to appear here.
              </p>
              <Link href="/dashboard/stories/review">
                <Button variant="outline" size="sm">
                  Go to Content Review
                </Button>
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
                      <th className="w-48 px-4 py-3 text-left">Audio Status</th>
                      <th className="w-32 px-4 py-3 text-left">Preview</th>
                      <th className="w-40 px-4 py-3 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {visibleRows.map((row) => {
                      const rowId = Number(row.id);
                      const statusObj = pipelineStatusMap[rowId];
                      const isTriggering = triggeringId === row.id;
                      const isRunning = isLibraryStoryPipelineActivelyRunning(statusObj ?? null);

                      return (
                        <tr key={row.id} className="transition-colors hover:bg-muted/20">
                          <td className="px-4 py-3 font-mono text-right align-top tabular-nums">{row.id}</td>
                          <td className="px-4 py-3 text-left align-top">
                            <Link
                              href={`/dashboard/stories/${row.id}/edit`}
                              className="hover:underline font-medium"
                            >
                              {row.title || "Untitled"}
                            </Link>
                            <div className="text-xs text-muted-foreground mt-1">
                              {row.category && <span>{row.category}</span>}
                            </div>
                          </td>
                          <td className="px-4 py-3 text-left align-top">
                            <div className="space-y-2">
                              <PipelineStatusBadges status={statusObj} showProgress={true} compact={true} />
                              {statusObj?.durationSeconds && (
                                <div className="text-xs text-muted-foreground">
                                  Duration: {formatDuration(Number(statusObj.durationSeconds))}
                                </div>
                              )}
                              {statusObj?.generatedAtIst && (
                                <div className="text-xs text-muted-foreground">
                                  Generated: {statusObj.generatedAtIst}
                                </div>
                              )}
                            </div>
                          </td>
                          <td className="px-4 py-3 text-left align-top">
                            <DropdownMenu>
                              <DropdownMenuTrigger asChild>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  className="h-8 px-2 text-muted-foreground hover:text-foreground"
                                  disabled={!statusObj}
                                  title="Preview audio"
                                >
                                  <Eye className="h-4 w-4 mr-1" />
                                  Preview
                                </Button>
                              </DropdownMenuTrigger>
                              <DropdownMenuContent align="start" className="min-w-[140px]">
                                <DropdownMenuLabel className="text-xs">Select Language</DropdownMenuLabel>
                                <DropdownMenuSeparator />
                                {SUPPORTED_LANGUAGES.map((lang) => {
                                  const isCompleted = isLanguageCompleted(statusObj, lang);
                                  return (
                                    <DropdownMenuItem
                                      key={lang}
                                      onClick={() => handleOpenPreview(row.id, row.title || "Untitled", lang, statusObj)}
                                      disabled={!isCompleted}
                                    >
                                      <Play className="h-3.5 w-3.5 mr-2 shrink-0" />
                                      {adminStoryLanguageLabel(lang)}
                                      {isCompleted && (
                                        <CheckCircle className="h-3 w-3 ml-auto text-emerald-600" />
                                      )}
                                    </DropdownMenuItem>
                                  );
                                })}
                              </DropdownMenuContent>
                            </DropdownMenu>
                          </td>
                          <td className="px-4 py-3 text-right align-top">
                            <div className="flex items-center justify-end gap-2">
                              <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                  <Button
                                    size="sm"
                                    variant="default"
                                    className="h-8 min-w-[120px]"
                                    disabled={isTriggering || isRunning}
                                  >
                                    {isTriggering || isRunning ? (
                                      <>
                                        <RefreshCw className="h-3.5 w-3.5 animate-spin mr-1.5" aria-hidden />
                                        Processing…
                                      </>
                                    ) : (
                                      <>
                                        <Mic className="h-3.5 w-3.5 mr-1.5" />
                                        Generate
                                        <ChevronDown className="h-3.5 w-3.5 ml-1 opacity-70" />
                                      </>
                                    )}
                                  </Button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="end" className="min-w-[200px]">
                                  <DropdownMenuLabel className="text-xs">Generate Audio</DropdownMenuLabel>
                                  <DropdownMenuSeparator />
                                  <DropdownMenuItem onClick={() => handleGenerateAudio(row.id)}>
                                    <Mic className="h-3.5 w-3.5 mr-2" />
                                    All Languages
                                  </DropdownMenuItem>
                                  <DropdownMenuSeparator />
                                  <DropdownMenuLabel className="text-xs">Individual Languages</DropdownMenuLabel>
                                  {SUPPORTED_LANGUAGES.map((lang) => (
                                    <DropdownMenuItem
                                      key={lang}
                                      onClick={() => handleGenerateAudio(row.id, [lang])}
                                    >
                                      {adminStoryLanguageLabel(lang)}
                                    </DropdownMenuItem>
                                  ))}
                                </DropdownMenuContent>
                              </DropdownMenu>

                              <DropdownMenu>
                                <DropdownMenuTrigger asChild>
                                  <Button
                                    size="sm"
                                    variant="outline"
                                    className="h-8 px-3"
                                    title="Approve narration"
                                  >
                                    <CheckCircle className="h-3.5 w-3.5 mr-1.5" />
                                    Approve
                                    <ChevronDown className="h-3.5 w-3.5 ml-1 opacity-70" />
                                  </Button>
                                </DropdownMenuTrigger>
                                <DropdownMenuContent align="end" className="min-w-[180px]">
                                  <DropdownMenuLabel className="text-xs">Approve Narration</DropdownMenuLabel>
                                  <DropdownMenuSeparator />
                                  {SUPPORTED_LANGUAGES.map((lang) => {
                                    const isCompleted = isLanguageCompleted(statusObj, lang);
                                    return (
                                      <DropdownMenuItem
                                        key={`approve-${lang}`}
                                        onClick={() => handleOpenApproval(row.id, row.title || "Untitled", lang, statusObj)}
                                        disabled={!isCompleted}
                                      >
                                        <CheckCircle className="h-3.5 w-3.5 mr-2 text-emerald-600" />
                                        {adminStoryLanguageLabel(lang)}
                                      </DropdownMenuItem>
                                    );
                                  })}
                                </DropdownMenuContent>
                              </DropdownMenu>
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
                        ({data.totalElements} stor{data.totalElements === 1 ? "y" : "ies"} total)
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

      {/* Audio Preview Modal */}
      {previewModal && (
        <AudioPreviewModal
          open={previewModal.open}
          onOpenChange={(open) => {
            if (!open) setPreviewModal(null);
          }}
          storyId={previewModal.storyId}
          storyTitle={previewModal.storyTitle}
          language={previewModal.language}
          duration={previewModal.duration}
          fileSize={previewModal.fileSize}
          generatedAt={previewModal.generatedAt}
          onError={(error) => showError("Preview error", error)}
        />
      )}

      {/* Audio Approval Dialog */}
      {approvalDialog && (
        <AudioApprovalDialog
          open={approvalDialog.open}
          onOpenChange={(open) => {
            if (!open) setApprovalDialog(null);
          }}
          storyId={approvalDialog.storyId}
          storyTitle={approvalDialog.storyTitle}
          language={approvalDialog.language}
          languageLabel={adminStoryLanguageLabel(approvalDialog.language)}
          duration={approvalDialog.duration}
          fileSize={approvalDialog.fileSize}
          generatedAt={approvalDialog.generatedAt}
          onApprove={handleApproveNarration}
          isApproving={isApproving}
        />
      )}
    </div>
  );
}
