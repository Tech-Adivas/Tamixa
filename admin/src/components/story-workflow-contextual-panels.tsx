"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { api } from "@/lib/api";
import type { LibraryStorySummary, PipelineStatusResponse } from "@/types/api";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { StoryProgressBar } from "@/components/design-system/story-progress-bar";
import { useAuth } from "@/contexts/auth-context";
import { useActionResult } from "@/contexts/action-result-context";
import { usePipelineActive } from "@/contexts/pipeline-active-context";
import { canModerateStories } from "@/lib/admin-roles";
import {
  isLibraryStoryPipelineBusy,
  isLibraryStoryInReviewQueue,
  POST_APPROVAL_CONTENT_CHANGE_HELP,
  REGENERATE_THEN_TRANSLATIONS_HELP,
  REVIEW_QUEUE_EXPECTATION_HELP,
} from "@/lib/library-story-workflow";
import { parseJsonStoryContent, cn, resolveLibraryStoryEditorBody, parsePipelineLanguageSet } from "@/lib/utils";
import {
  RefreshCw,
  Eye,
  MessageSquare,
  CheckCircle,
  XCircle,
  Mic,
  ChevronDown,
  Play,
  Pause,
  StopCircle,
  ExternalLink,
} from "lucide-react";

const LANG_LABELS: Record<string, string> = {
  ta: "Tamil",
  hi: "Hindi",
  en: "English",
  te: "Telugu",
  kn: "Kannada",
  ml: "Malayalam",
};

const LANG_SHORT: Record<string, string> = {
  ta: "Ta",
  hi: "Hi",
  en: "En",
  te: "Te",
  kn: "Kn",
  ml: "Ml",
};

const REVIEW_META_KEYS = [
  "processing",
  "progress",
  "overallStatus",
  "reviewedLanguages",
  "reviewStaleLanguages",
  "allLanguagesReviewed",
];

function reviewLangEntries(status?: PipelineStatusResponse | Record<string, string | undefined> | null) {
  if (!status) return [];
  return Object.entries(status).filter(([k]) => !REVIEW_META_KEYS.includes(k));
}

function isPipelineFullyComplete(status?: PipelineStatusResponse | Record<string, string | undefined> | null): boolean {
  if (!status) return false;
  if (status.overallStatus === "COMPLETED" || status.overallStatus === "READY_FOR_REVIEW") return true;
  const entries = reviewLangEntries(status);
  return entries.length > 0 && entries.every(([, s]) => typeof s === "string" && s.trim() === "COMPLETED");
}

function getAllPipelineLanguages(
  status?: PipelineStatusResponse | Record<string, string | undefined> | null
): Array<{ lang: string; stage: string }> {
  return reviewLangEntries(status).map(([lang, stage]) => ({
    lang,
    stage: typeof stage === "string" ? stage : "PENDING",
  }));
}

function isPipelineInProgress(status?: PipelineStatusResponse | Record<string, string | undefined> | null): boolean {
  return isLibraryStoryPipelineBusy(status);
}

const NARRATION_META_KEYS = [
  "processing",
  "progress",
  "overallStatus",
  "reviewedLanguages",
  "reviewStaleLanguages",
  "allLanguagesReviewed",
  "generatedAtIst",
  "durationSeconds",
  "audioCoverageWarnings",
  "failedLanguagesCount",
];

function getCompletedLanguages(status?: PipelineStatusResponse | null): string[] {
  if (!status || typeof status !== "object") return [];
  return Object.entries(status)
    .filter(([k, s]) => !NARRATION_META_KEYS.includes(k) && s === "COMPLETED")
    .map(([lang]) => lang);
}

function getPipelineProgress(status?: PipelineStatusResponse | null): { completed: number; total: number; estMinLeft: number } {
  if (!status || typeof status !== "object") return { completed: 0, total: 0, estMinLeft: 0 };
  const entries = Object.entries(status).filter(([k]) => !NARRATION_META_KEYS.includes(k));
  const total = entries.length;
  const completed = entries.filter(([, s]) => s === "COMPLETED").length;
  const failed = entries.filter(([, s]) => s?.includes("FAILED")).length;
  const inProgress = entries.some(([, s]) =>
    ["PENDING", "TRANSLATING", "REWRITING", "TTS_PROCESSING"].includes(s ?? "") ||
    (s ?? "").startsWith("TRANSLATING") ||
    (s ?? "").startsWith("REWRITING") ||
    (s ?? "").startsWith("TTS_PROCESSING")
  );
  const pendingCount = total - completed - failed;
  const estMinLeft = inProgress && pendingCount > 0 ? pendingCount * 2 : 0;
  return { completed, total, estMinLeft };
}

function narrationProgressLabel(status: string): string {
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

function isNarrationPipelineRunning(status: string): boolean {
  return ["TRANSLATING_LANGUAGES", "TTS_PROCESSING", "FINALIZING_STORY"].includes(status);
}

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  if (m >= 1 && s > 0) return `${m} min ${s} sec`;
  if (m >= 1) return `${m} min`;
  return `${s} sec`;
}

const POLL_MS = 3000;
const POLL_DURATION_MS = 6 * 60 * 1000;
const SHOW_STARTING_FOR_MS = 60000;
const COMPLETION_REFETCH_DELAYS_MS = [70000, 110000];

function pipelineStatusFromBatch(
  batch: Record<number, PipelineStatusResponse> | null | undefined,
  storyId: number
): PipelineStatusResponse | undefined {
  if (!batch) return undefined;
  const asRecord = batch as Record<string | number, PipelineStatusResponse>;
  return asRecord[storyId] ?? asRecord[String(storyId)];
}

export function StoryReviewStepPanel({
  storyId,
  onGoToSubmitStep,
  onStoryRefresh,
}: {
  storyId: number;
  onGoToSubmitStep?: () => void;
  onStoryRefresh?: () => void;
}) {
  const { user } = useAuth();
  const { showSuccess, showError } = useActionResult();
  const { refresh: refreshPipelineActive } = usePipelineActive();
  const canModerate = canModerateStories(user ?? null);

  const [story, setStory] = useState<LibraryStorySummary | null>(null);
  const [storyLoading, setStoryLoading] = useState(true);
  const [pipelineStatus, setPipelineStatus] = useState<PipelineStatusResponse | undefined>(undefined);
  const [rejectMarked, setRejectMarked] = useState(false);

  const [approving, setApproving] = useState(false);
  const [syncing, setSyncing] = useState(false);
  const [rejecting, setRejecting] = useState(false);
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [rejectNotes, setRejectNotes] = useState("");

  const [translationModalOpen, setTranslationModalOpen] = useState(false);
  const [translationLang, setTranslationLang] = useState<string | null>(null);
  const [translationForm, setTranslationForm] = useState<{ title: string; content: string; moral: string } | null>(null);
  const [translationComments, setTranslationComments] = useState("");
  const [translationLoading, setTranslationLoading] = useState(false);

  const loadStory = useCallback(async () => {
    setStoryLoading(true);
    try {
      const s = await api.admin.getLibraryStory(storyId);
      setStory(s);
      setRejectMarked(!!s.rejectMarkedAt);
    } catch (e) {
      showError("Load failed", e instanceof Error ? e.message : "Could not load story");
      setStory(null);
    } finally {
      setStoryLoading(false);
    }
  }, [storyId, showError]);

  const mergePipelineStatus = useCallback((status: PipelineStatusResponse) => {
    setPipelineStatus(status);
  }, []);

  const fetchPipeline = useCallback(() => {
    api.admin
      .getLibraryStoryPipelineStatusBatch([storyId])
      .then((batch) => {
        const s = pipelineStatusFromBatch(batch, storyId);
        if (s && typeof s === "object") mergePipelineStatus(s);
      })
      .catch(() => {});
  }, [storyId, mergePipelineStatus]);

  useEffect(() => {
    void loadStory();
  }, [loadStory]);

  useEffect(() => {
    fetchPipeline();
  }, [fetchPipeline]);

  useEffect(() => {
    if (!isPipelineInProgress(pipelineStatus)) return;
    const id = setInterval(fetchPipeline, POLL_MS);
    return () => clearInterval(id);
  }, [pipelineStatus, fetchPipeline]);

  const refreshPipelineFromDb = useCallback(async () => {
    setSyncing(true);
    try {
      const status = await api.admin.getLibraryStoryPipelineStatus(storyId);
      if (status) {
        mergePipelineStatus(status);
        if (status.allLanguagesReviewed === "true") {
          showSuccess("Synced", "All languages reviewed. Approve is now enabled.");
        }
      }
    } catch {
      showError("Sync failed", "Could not refresh from database.");
    } finally {
      setSyncing(false);
    }
  }, [storyId, mergePipelineStatus, showSuccess, showError]);

  const markLanguageReviewed = useCallback(
    async (lang: string) => {
      const normalized = lang.trim().toLowerCase();
      if (!normalized) return;
      try {
        await api.admin.markReviewedLanguages(storyId, [normalized]);
        const status = await api.admin.getLibraryStoryPipelineStatus(storyId);
        if (status) mergePipelineStatus(status);
      } catch (e) {
        showError("Failed to save review", e instanceof Error ? e.message : "Could not persist");
      }
    },
    [storyId, mergePipelineStatus, showError]
  );

  const openTranslationModal = (language: string) => {
    setTranslationLang(language);
    setTranslationForm(null);
    setTranslationComments("");
    setTranslationModalOpen(true);
    setTranslationLoading(true);
    const langCode = language.trim().toLowerCase();
    api.admin
      .getLibraryStory(storyId, langCode)
      .then((s) => {
        const contentToEdit = resolveLibraryStoryEditorBody(s);
        const parsed = parseJsonStoryContent(contentToEdit);
        const resolved = parsed ?? { content: contentToEdit };
        setTranslationForm({
          title: (s.title ?? resolved.title ?? "")?.trim() || "",
          content: resolved.content,
          moral: (s.moral ?? resolved.moral ?? "")?.trim() || "",
        });
      })
      .catch((e) => {
        const msg = e instanceof Error ? e.message : "Load failed";
        showError(
          "Translation not loaded",
          msg.includes("404") || msg.includes("not found")
            ? `${LANG_LABELS[language] ?? language} content is not ready yet. Run the pipeline so this language is translated.`
            : msg
        );
        setTranslationModalOpen(false);
      })
      .finally(() => setTranslationLoading(false));
  };

  const handleApprove = async () => {
    setApproving(true);
    try {
      await api.admin.approveLibraryStoryNarration(storyId);
      showSuccess(
        "Approved for delivery",
        "Open Narration (Story to Speech) and use Generate audio when you are ready to produce MP3s."
      );
      setPipelineStatus(undefined);
      await loadStory();
      onStoryRefresh?.();
      refreshPipelineActive();
      setTimeout(refreshPipelineActive, 1500);
    } catch (e) {
      showError("Approve failed", e instanceof Error ? e.message : "Failed to approve");
    } finally {
      setApproving(false);
    }
  };

  const handleReject = async () => {
    setRejecting(true);
    try {
      await api.admin.rejectLibraryStory(storyId, rejectNotes || undefined);
      showSuccess("Story rejected", "The story has been rejected. You can edit it and submit again from the library.");
      setRejectModalOpen(false);
      setRejectNotes("");
      setRejectMarked(false);
      setPipelineStatus(undefined);
      await loadStory();
      onStoryRefresh?.();
    } catch (e) {
      showError("Reject failed", e instanceof Error ? e.message : "Failed to reject");
    } finally {
      setRejecting(false);
    }
  };

  const allLangs = getAllPipelineLanguages(pipelineStatus);
  const pipelineComplete = isPipelineFullyComplete(pipelineStatus);
  const pipelineInProgress = isPipelineInProgress(pipelineStatus);
  const apiReviewedStr = pipelineStatus?.reviewedLanguages;
  const dbReviewedLangs = new Set<string>();
  if (typeof apiReviewedStr === "string" && apiReviewedStr.trim()) {
    apiReviewedStr
      .split(",")
      .map((s) => s.trim().toLowerCase())
      .filter(Boolean)
      .forEach((l) => dbReviewedLangs.add(l));
  }
  const requiredLangs = allLangs.map(({ lang }) => lang.trim().toLowerCase());
  const staleReviewLangs = parsePipelineLanguageSet(pipelineStatus?.reviewStaleLanguages);
  const apiAllReviewed = pipelineStatus?.allLanguagesReviewed;
  const allLanguagesReviewed =
    apiAllReviewed === "true" ||
    (apiAllReviewed == null &&
      (requiredLangs.length === 0 ||
        (requiredLangs.every((l) => dbReviewedLangs.has(l)) &&
          requiredLangs.every((l) => !staleReviewLangs.has(l)))));
  /** Green "reviewed" badges come only from API `reviewedLanguages` — no duplicate client Set (avoids stale greens after submit). */
  const reviewedLangsForBadge = dbReviewedLangs;

  const approveDisabled = approving || !allLanguagesReviewed;
  const rejectDisabled = rejecting || pipelineInProgress || !rejectMarked;

  if (storyLoading && !story) {
    return (
      <div className="rounded-lg border border-dashed border-border bg-muted/20 px-4 py-8 text-center text-sm text-muted-foreground">
        Loading review…
      </div>
    );
  }

  if (!story) {
    return (
      <div className="rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-4 text-sm text-destructive">
        Could not load this story.
      </div>
    );
  }

  const inReviewQueue = isLibraryStoryInReviewQueue(story.status);
  const alreadyApproved = !!story.narrationApprovedAt;

  if (!inReviewQueue) {
    return (
      <div className="space-y-3 rounded-lg border border-border bg-muted/10 px-4 py-4 text-sm">
        <p className="text-muted-foreground">
          This story is not in the review queue yet (draft, rejected, or changes requested). Use{" "}
          <strong>Save &amp; submit</strong> from the previous step when you are ready.
        </p>
        <p className="text-xs text-muted-foreground rounded-md border border-border/60 bg-background/50 px-3 py-2">
          {REGENERATE_THEN_TRANSLATIONS_HELP}
        </p>
        {onGoToSubmitStep && (
          <Button type="button" variant="secondary" size="sm" onClick={onGoToSubmitStep}>
            Go to Save &amp; submit
          </Button>
        )}
        <p className="text-xs text-muted-foreground">
          For the full list of stories in review, open{" "}
          <Link href="/dashboard/stories/approve" className="text-primary underline-offset-4 hover:underline inline-flex items-center gap-0.5">
            Story for review <ExternalLink className="h-3 w-3" />
          </Link>
          .
        </p>
      </div>
    );
  }

  if (alreadyApproved) {
    return (
      <div className="space-y-3 rounded-lg border border-green-500/30 bg-green-500/5 px-4 py-4 text-sm">
        <p className="font-medium text-green-800 dark:text-green-200">This story is already approved for delivery.</p>
        <p className="text-muted-foreground">
          Manage narration audio in the <strong>Narration</strong> step, or use the Narration tab to work across many stories.
        </p>
        <p className="text-xs text-muted-foreground">
          {pipelineComplete ? "Pipeline scripts looked complete at last check." : "You can still open the full Review tab if you need bulk workflows."}
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm font-medium text-foreground">{story.title || `Story #${storyId}`}</p>
          <p className="text-xs text-muted-foreground font-mono">ID {storyId}</p>
        </div>
        <Button type="button" variant="outline" size="sm" className="h-8" onClick={() => void loadStory()} disabled={storyLoading}>
          <RefreshCw className={cn("h-3.5 w-3.5 mr-1.5", storyLoading && "animate-spin")} />
          Refresh story
        </Button>
      </div>

      <p className="text-xs text-muted-foreground rounded-md border border-border/60 bg-muted/20 px-3 py-2">
        {REVIEW_QUEUE_EXPECTATION_HELP} Major text changes belong in{" "}
        <Link href={`/dashboard/stories/${storyId}/edit`} className="text-primary underline-offset-4 hover:underline">
          Edit
        </Link>
        : {REGENERATE_THEN_TRANSLATIONS_HELP}
      </p>

      {!pipelineComplete && (
        <p className="text-xs text-amber-800 dark:text-amber-200/90 rounded-md border border-amber-200 bg-amber-50 dark:border-amber-900 dark:bg-amber-950/40 px-3 py-2">
          Pipeline still filling in languages. You can open each language when ready; approve is gated on marking every language reviewed once content exists.
        </p>
      )}

      <div>
        <p className="text-xs font-medium text-muted-foreground mb-2">Languages — view &amp; mark reviewed</p>
        {staleReviewLangs.size > 0 ? (
          <p className="text-xs text-red-800 dark:text-red-200/90 rounded-md border border-red-200 bg-red-50 dark:border-red-900 dark:bg-red-950/40 px-3 py-2 mb-2">
            Red badges: story text or narration script changed after you marked that language reviewed. Open the language and tap{" "}
            <strong>Have reviewed</strong> again before approving.
          </p>
        ) : null}
        <div className="flex flex-wrap gap-1.5">
          {pipelineStatus === undefined ? (
            <span className="text-muted-foreground text-xs">Loading pipeline…</span>
          ) : allLangs.length === 0 ? (
            <span className="text-muted-foreground text-xs">No pipeline languages yet. Run <strong>Generate translations</strong> from the Content step.</span>
          ) : (
            allLangs.map(({ lang, stage }) => {
              const isCompleted = stage.startsWith("COMPLETED");
              const langKey = lang.trim().toLowerCase();
              const isReviewed = reviewedLangsForBadge.has(langKey);
              const isReviewStale = isReviewed && staleReviewLangs.has(langKey);
              return (
                <div
                  key={lang}
                  className={cn(
                    "group inline-flex items-center gap-0.5 rounded-md border px-2 py-1.5 text-sm transition-colors",
                    isReviewStale
                      ? "border-red-500/60 bg-red-500/15 text-red-900 dark:text-red-200"
                      : isReviewed
                        ? "border-green-500/60 bg-green-500/15 text-green-800 dark:text-green-200"
                        : isCompleted
                          ? "border-border bg-muted/40"
                          : "border-border bg-muted/20 text-muted-foreground"
                  )}
                  title={`${LANG_LABELS[lang] ?? lang}${
                    isReviewStale
                      ? " — content changed since review; open and tap Have reviewed"
                      : isReviewed
                        ? " (reviewed)"
                        : ` — ${stage}`
                  }`}
                >
                  <span className="font-medium text-foreground w-6 text-center">{LANG_SHORT[lang] ?? lang}</span>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-6 w-6 p-0 min-w-0"
                    onClick={() => openTranslationModal(lang)}
                    title={`View ${LANG_LABELS[lang] ?? lang}`}
                  >
                    <Eye className="h-3 w-3" />
                  </Button>
                </div>
              );
            })
          )}
        </div>
      </div>

      {canModerate ? (
        <div className="flex flex-wrap items-center gap-2 pt-1">
          {approveDisabled && (
            <Button type="button" variant="outline" size="sm" className="h-9" disabled={syncing} onClick={() => void refreshPipelineFromDb()}>
              <RefreshCw className={cn("h-3.5 w-3.5 mr-1.5", syncing && "animate-spin")} />
              {syncing ? "Syncing…" : "Sync reviewed state"}
            </Button>
          )}
          <Button type="button" size="sm" className="h-9" disabled={approveDisabled} onClick={() => void handleApprove()}>
            {approving ? (
              <>
                <RefreshCw className="h-3.5 w-3.5 animate-spin mr-1.5" />
                Approving…
              </>
            ) : (
              <>
                <CheckCircle className="h-3.5 w-3.5 mr-1.5" />
                Approve for delivery
              </>
            )}
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            className="h-9 text-destructive hover:text-destructive hover:bg-destructive/10"
            disabled={rejectDisabled}
            onClick={() => setRejectModalOpen(true)}
            title={
              rejectDisabled
                ? pipelineInProgress
                  ? "Pipeline queued or running"
                  : !rejectMarked
                    ? "Tick Reject in a language view first"
                    : ""
                : "Reject — send back to library"
            }
          >
            {rejecting ? (
              <>
                <RefreshCw className="h-3.5 w-3.5 animate-spin mr-1.5" />
                Rejecting…
              </>
            ) : (
              <>
                <XCircle className="h-3.5 w-3.5 mr-1.5" />
                Reject
              </>
            )}
          </Button>
        </div>
      ) : (
        <p className="text-xs text-muted-foreground">You do not have permission to approve or reject stories.</p>
      )}

      <p className="text-xs text-muted-foreground">
        Open the full queue:{" "}
        <Link href="/dashboard/stories/approve" className="text-primary underline-offset-4 hover:underline inline-flex items-center gap-0.5">
          Story for review <ExternalLink className="h-3 w-3" />
        </Link>
      </p>

      <Dialog open={rejectModalOpen} onOpenChange={setRejectModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Reject story</DialogTitle>
            <DialogDescription>
              Rejecting removes the story from the review queue. The creator can edit and submit again from Story library.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3 py-2">
            <Label htmlFor="inline-reject-notes">Notes (optional)</Label>
            <Input
              id="inline-reject-notes"
              placeholder="e.g. Content needs revision…"
              value={rejectNotes}
              onChange={(e) => setRejectNotes(e.target.value)}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRejectModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="destructive" disabled={rejecting} onClick={() => void handleReject()}>
              {rejecting ? "Rejecting…" : "Reject"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={translationModalOpen} onOpenChange={setTranslationModalOpen}>
        <DialogContent className="max-w-xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>View {translationLang ? LANG_LABELS[translationLang] ?? translationLang : ""}</DialogTitle>
            <DialogDescription>
              Review content for this language. Use &ldquo;Have reviewed&rdquo; when done. Once all languages are reviewed, Approve is enabled.
            </DialogDescription>
          </DialogHeader>
          {translationLoading ? (
            <p className="py-6 text-center text-muted-foreground">Loading…</p>
          ) : translationForm ? (
            <div className="space-y-4 py-2">
              {translationLang &&
                !String(pipelineStatus?.[translationLang] ?? "").startsWith("COMPLETED") && (
                  <div className="rounded-md border border-amber-200 bg-amber-50 dark:border-amber-800 dark:bg-amber-950/40 px-3 py-2 text-sm text-amber-800 dark:text-amber-200">
                    Pipeline not complete for this language yet. Content may be placeholder.
                  </div>
                )}
              <div>
                <Label className="text-muted-foreground">Title</Label>
                <div className="mt-1 rounded-md border border-input bg-muted/30 px-3 py-2 text-sm">{translationForm.title || "—"}</div>
              </div>
              <div>
                <Label className="text-muted-foreground">Story content</Label>
                <div className="mt-1 rounded-md border border-input bg-muted/30 px-3 py-2 text-sm whitespace-pre-wrap max-h-[300px] overflow-y-auto">
                  {translationForm.content || "—"}
                </div>
                <p className="text-xs text-muted-foreground mt-1">
                  {translationForm.content.split(/\s+/).filter((w) => w.trim()).length} words
                </p>
              </div>
              <div>
                <Label className="text-muted-foreground">Moral</Label>
                <div className="mt-1 rounded-md border border-input bg-muted/30 px-3 py-2 text-sm">{translationForm.moral || "—"}</div>
              </div>
              <div>
                <Label htmlFor="inline-translation-comments">
                  <MessageSquare className="h-3.5 w-3.5 inline mr-1" />
                  Comments (optional)
                </Label>
                <textarea
                  id="inline-translation-comments"
                  value={translationComments}
                  onChange={(e) => setTranslationComments(e.target.value)}
                  placeholder="Notes for this language…"
                  className="mt-1 flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                />
              </div>
              <div className="flex items-center gap-2 pt-2 border-t">
                <input
                  type="checkbox"
                  id="inline-translation-reject"
                  checked={rejectMarked}
                  onChange={async (e) => {
                    const checked = e.target.checked;
                    try {
                      await api.admin.setRejectMarked(storyId, checked);
                      setRejectMarked(checked);
                    } catch (err) {
                      showError("Failed to save", err instanceof Error ? err.message : "Could not persist");
                    }
                  }}
                  className="h-4 w-4 rounded border-input"
                />
                <Label htmlFor="inline-translation-reject" className="text-sm font-medium cursor-pointer text-destructive">
                  Reject this story
                </Label>
                <span className="text-xs text-muted-foreground">(enables Reject)</span>
              </div>
            </div>
          ) : null}
          <DialogFooter>
            <Button variant="outline" onClick={() => setTranslationModalOpen(false)}>
              Close
            </Button>
            {translationForm && translationLang && (
              <Button
                onClick={async () => {
                  await markLanguageReviewed(translationLang);
                  setTranslationModalOpen(false);
                }}
              >
                {rejectMarked ? "Submit" : "Have reviewed"}
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

export function StoryNarrationStepPanel({
  storyId,
  onGoToReviewStep,
  onStoryRefresh,
}: {
  storyId: number;
  onGoToReviewStep?: () => void;
  onStoryRefresh?: () => void;
}) {
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const reviewStepHref = useMemo(() => {
    const q = new URLSearchParams(searchParams.toString());
    q.set("step", "3");
    return `${pathname}?${q.toString()}`;
  }, [pathname, searchParams]);

  const { showSuccess, showError } = useActionResult();
  const { refresh: refreshPipelineActive, registerTriggered } = usePipelineActive();

  const [story, setStory] = useState<LibraryStorySummary | null>(null);
  const [storyLoading, setStoryLoading] = useState(true);
  const [pipelineStatus, setPipelineStatus] = useState<PipelineStatusResponse | undefined>(undefined);
  const [triggering, setTriggering] = useState(false);
  const [pollUntil, setPollUntil] = useState<number | null>(null);
  const [recentlyTriggered, setRecentlyTriggered] = useState<{ at: number } | null>(null);
  const [previewLoading, setPreviewLoading] = useState(false);
  const [playingAudio, setPlayingAudio] = useState<{ language: string } | null>(null);
  const [isPaused, setIsPaused] = useState(false);
  const audioRef = useRef<{ element: HTMLAudioElement; objectUrl: string } | null>(null);

  const loadStory = useCallback(async () => {
    setStoryLoading(true);
    try {
      const s = await api.admin.getLibraryStory(storyId);
      setStory(s);
    } catch (e) {
      showError("Load failed", e instanceof Error ? e.message : "Could not load story");
      setStory(null);
    } finally {
      setStoryLoading(false);
    }
  }, [storyId, showError]);

  const fetchPipelineStatus = useCallback(() => {
    api.admin
      .getLibraryStoryPipelineStatusBatch([storyId])
      .then((batch) => {
        const s = pipelineStatusFromBatch(batch, storyId);
        if (s && typeof s === "object") setPipelineStatus(s);
      })
      .catch(() => {});
  }, [storyId]);

  const fetchPipelineStatusRef = useRef(fetchPipelineStatus);
  fetchPipelineStatusRef.current = fetchPipelineStatus;

  useEffect(() => {
    void loadStory();
  }, [loadStory]);

  useEffect(() => {
    fetchPipelineStatus();
  }, [fetchPipelineStatus]);

  useEffect(() => {
    if (pollUntil == null) return;
    if (Date.now() >= pollUntil) {
      setPollUntil(null);
      return;
    }
    const t = setInterval(() => {
      if (Date.now() >= pollUntil) {
        setPollUntil(null);
        clearInterval(t);
        return;
      }
      fetchPipelineStatus();
      refreshPipelineActive();
    }, POLL_MS);
    return () => clearInterval(t);
  }, [pollUntil, fetchPipelineStatus, refreshPipelineActive]);

  useEffect(() => {
    const status = pipelineStatus?.overallStatus ?? "";
    // Keep polling even when the pipeline is only queued (PENDING/starting).
    const busy = isLibraryStoryPipelineBusy(pipelineStatus ?? null) || isNarrationPipelineRunning(status);
    if (busy && pollUntil == null) {
      setPollUntil(Date.now() + POLL_DURATION_MS);
    }
  }, [pipelineStatus, pollUntil]);

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
    return () => timers.forEach((x) => clearTimeout(x));
  }, [recentlyTriggered, refreshPipelineActive]);

  useEffect(() => {
    return () => {
      if (audioRef.current) URL.revokeObjectURL(audioRef.current.objectUrl);
    };
  }, []);

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

  const hasAudio = pipelineStatus?.overallStatus === "COMPLETED" || pipelineStatus?.overallStatus === "READY_FOR_REVIEW";

  const handlePlayPreview = useCallback(
    async (language: string) => {
      stopCurrentPlayback();
      setPreviewLoading(true);
      try {
        const base = (typeof process !== "undefined" && process.env?.NEXT_PUBLIC_API_URL) || "";
        const url = `${base}/api/v1/admin/stories/${storyId}/preview-audio?language=${encodeURIComponent(language)}`;
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
        setPlayingAudio({ language });
        setIsPaused(false);
        await audio.play();
      } catch (e) {
        showError("Preview failed", e instanceof Error ? e.message : "Preview failed");
        setPlayingAudio(null);
        audioRef.current = null;
        api.admin.getLibraryStoryPipelineStatusBatch([storyId]).then((batch) => {
          const s = pipelineStatusFromBatch(batch, storyId);
          if (s && typeof s === "object") setPipelineStatus(s);
        }).catch(() => {});
      } finally {
        setPreviewLoading(false);
      }
    },
    [storyId, showError, stopCurrentPlayback]
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

  const handleRetryFailed = async () => {
    setTriggering(true);
    registerTriggered(storyId);
    try {
      await api.admin.retryLibraryStory(storyId);
      showSuccess("Retry started", "Retrying failed languages.");
      setRecentlyTriggered({ at: Date.now() });
      setPollUntil(Date.now() + POLL_DURATION_MS);
      fetchPipelineStatus();
      refreshPipelineActive();
      setTimeout(fetchPipelineStatus, 1500);
      setTimeout(fetchPipelineStatus, 4000);
    } catch (e) {
      showError("Retry failed", e instanceof Error ? e.message : "Failed to retry");
    } finally {
      setTriggering(false);
    }
  };

  const handleGenerateOrRegenerateAudio = async (languages?: string[]) => {
    setTriggering(true);
    registerTriggered(storyId);
    const isRegenerate = hasAudio;
    try {
      if (isRegenerate) {
        await api.admin.regenerateLibraryStoryNarration(
          storyId,
          languages?.length ? { languages } : undefined
        );
        const scope =
          languages?.length ? `languages (${languages.map((l) => LANG_SHORT[l] ?? l).join(", ")})` : "all languages";
        showSuccess("Regenerate started", `${scope} — pipeline queued or running.`);
      } else {
        await api.admin.triggerLibraryStoryPipeline(storyId);
        showSuccess("Generate audio started", "Pipeline queued or running.");
      }
      setRecentlyTriggered({ at: Date.now() });
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
      setTriggering(false);
    }
  };

  if (storyLoading && !story) {
    return (
      <div className="rounded-lg border border-dashed border-border bg-muted/20 px-4 py-8 text-center text-sm text-muted-foreground">
        Loading narration…
      </div>
    );
  }

  if (!story) {
    return (
      <div className="rounded-lg border border-destructive/30 bg-destructive/5 px-4 py-4 text-sm text-destructive">
        Could not load this story.
      </div>
    );
  }

  if (!story.narrationApprovedAt) {
    return (
      <div className="space-y-3 rounded-lg border border-border bg-muted/10 px-4 py-4 text-sm">
        <p className="text-muted-foreground">
          Narration audio is tied to <strong>approved</strong> stories. Approve this story in the Review step first.
        </p>
        {onGoToReviewStep ? (
          <Button type="button" variant="secondary" size="sm" onClick={onGoToReviewStep}>
            Go to Review
          </Button>
        ) : (
          <Button type="button" variant="secondary" size="sm" asChild>
            <Link href={reviewStepHref}>Go to Review</Link>
          </Button>
        )}
        <p className="text-xs text-muted-foreground">
          Full queue:{" "}
          <Link href="/dashboard/stories/to-speech" className="text-primary underline-offset-4 hover:underline inline-flex items-center gap-0.5">
            Narration <ExternalLink className="h-3 w-3" />
          </Link>
        </p>
      </div>
    );
  }

  const statusObj = pipelineStatus;
  const status = statusObj?.overallStatus ?? "—";
  const isRunning = isLibraryStoryPipelineBusy(statusObj ?? null) || isNarrationPipelineRunning(status);
  const showStarting =
    (recentlyTriggered != null && Date.now() - recentlyTriggered.at < SHOW_STARTING_FOR_MS && status === "PENDING") || triggering;
  const progressText = narrationProgressLabel(status);
  const statusDisplay = showStarting ? "Starting…" : progressText;
  const progressPct = statusObj?.progress;
  const isMarkedComplete = status === "COMPLETED" || status === "READY_FOR_REVIEW";
  const progressPercent = !isMarkedComplete && progressPct != null && progressPct !== "" ? `${progressPct}%` : null;
  const statusWithProgress = progressPercent != null ? `${progressPercent} · ${statusDisplay}` : statusDisplay;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm font-medium text-foreground">{story.title || `Story #${storyId}`}</p>
          <p className="text-xs text-muted-foreground">
            Approved {new Date(story.narrationApprovedAt).toLocaleString()}
          </p>
        </div>
        <Button type="button" variant="outline" size="sm" className="h-8" onClick={() => { void loadStory(); fetchPipelineStatus(); onStoryRefresh?.(); }}>
          <RefreshCw className="h-3.5 w-3.5 mr-1.5" />
          Refresh
        </Button>
      </div>

      <p className="text-xs text-muted-foreground rounded-md border border-border/60 bg-muted/15 px-3 py-2">
        {POST_APPROVAL_CONTENT_CHANGE_HELP}
      </p>

      <div className="rounded-lg border border-border bg-muted/20 px-3 py-3 text-sm">
        <p className="text-xs font-medium text-muted-foreground mb-2">Audio status</p>
        {isRunning || showStarting ? (
          <div className="flex flex-col gap-1.5 min-w-0 max-w-md">
            <div className="flex items-center justify-between gap-2">
              <span className="inline-flex items-center gap-1.5 text-xs font-medium">
                <RefreshCw className="h-3 w-3 animate-spin shrink-0" aria-hidden />
                {statusObj?.processing
                  ? statusObj.processing === "starting"
                    ? "Starting pipeline…"
                    : `Generating ${LANG_SHORT[statusObj.processing] ?? statusObj.processing} audio…`
                  : narrationProgressLabel(statusObj?.overallStatus ?? "")}
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
              <span className="text-xs font-medium tabular-nums shrink-0 w-9 text-right">{Number(statusObj?.progress) || 0}%</span>
            </div>
          </div>
        ) : (
          <div className="space-y-1 text-muted-foreground text-xs">
            <span>{statusWithProgress}</span>
            {statusObj?.generatedAtIst && isMarkedComplete && (
              <span className="block text-muted-foreground/80" title="Audio generated at (IST)">
                {statusObj.generatedAtIst}
              </span>
            )}
            {statusObj?.audioCoverageWarnings && isMarkedComplete && (
              <span className="block text-amber-600 dark:text-amber-500 font-medium" title="Possible truncation">
                Possible truncation:{" "}
                {statusObj.audioCoverageWarnings
                  .split(",")
                  .map((l) => LANG_SHORT[l.trim().toLowerCase()] ?? l.trim())
                  .join(", ")}
              </span>
            )}
            {statusObj?.failedLanguagesCount && Number(statusObj.failedLanguagesCount) > 0 && (
              <span className="block">
                <span className="text-destructive font-medium">{statusObj.failedLanguagesCount} language(s) failed. </span>
                <button
                  type="button"
                  onClick={() => void handleRetryFailed()}
                  disabled={triggering || isRunning || showStarting}
                  className="text-primary hover:underline font-medium"
                >
                  Retry failed
                </button>
              </span>
            )}
          </div>
        )}
        {statusObj?.durationSeconds ? (
          <p className="text-xs text-muted-foreground mt-2 tabular-nums">
            Length: {formatDuration(Number(statusObj.durationSeconds))}
          </p>
        ) : null}
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <span className="text-xs font-medium text-muted-foreground">Preview</span>
        {playingAudio ? (
          <div className="flex items-center gap-1">
            <span className="text-xs font-medium">{LANG_SHORT[playingAudio.language] ?? playingAudio.language}</span>
            <Button variant="outline" size="sm" className="h-7 w-7 p-0 shrink-0" onClick={handlePausePreview} title={isPaused ? "Resume" : "Pause"}>
              {isPaused ? <Play className="h-3 w-3" /> : <Pause className="h-3 w-3" />}
            </Button>
            <Button variant="outline" size="sm" className="h-7 w-7 p-0 shrink-0 text-destructive" onClick={stopCurrentPlayback} title="Stop">
              <StopCircle className="h-3 w-3" />
            </Button>
          </div>
        ) : hasAudio ? (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" size="sm" className="h-8" disabled={previewLoading}>
                {previewLoading ? <RefreshCw className="h-3.5 w-3.5 animate-spin" /> : <Play className="h-3.5 w-3.5 mr-1.5" />}
                Play…
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="start" className="min-w-[120px]">
              {(() => {
                const completedLangs = getCompletedLanguages(statusObj);
                const langs = completedLangs.length > 0 ? completedLangs : Object.keys(LANG_SHORT);
                return langs.map((lang) => (
                  <DropdownMenuItem key={lang} onClick={() => void handlePlayPreview(lang)}>
                    <Play className="h-3.5 w-3.5 mr-2 shrink-0" />
                    {LANG_SHORT[lang] ?? lang}
                  </DropdownMenuItem>
                ));
              })()}
            </DropdownMenuContent>
          </DropdownMenu>
        ) : (
          <span className="text-xs text-muted-foreground">—</span>
        )}
      </div>

      <div className="flex flex-wrap gap-2">
        {hasAudio && statusObj?.audioCoverageWarnings && isMarkedComplete ? (
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                size="sm"
                variant="default"
                className="h-9 min-w-[140px]"
                disabled={triggering || isRunning || showStarting}
              >
                {triggering ? (
                  <>
                    <RefreshCw className="h-3.5 w-3.5 animate-spin mr-1.5" />
                    Starting…
                  </>
                ) : isRunning || showStarting ? (
                  <>
                    <RefreshCw className="h-3.5 w-3.5 animate-spin mr-1.5" />
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
            <DropdownMenuContent align="start">
              <DropdownMenuItem
                onClick={() => {
                  const flaggedLangs = statusObj.audioCoverageWarnings!
                    .split(",")
                    .map((l) => l.trim().toLowerCase())
                    .filter(Boolean);
                  void handleGenerateOrRegenerateAudio(flaggedLangs);
                }}
              >
                Regenerate flagged (
                {statusObj.audioCoverageWarnings
                  .split(",")
                  .map((l) => LANG_SHORT[l.trim().toLowerCase()] ?? l.trim())
                  .join(", ")}
                )
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => void handleGenerateOrRegenerateAudio()}>Regenerate all languages</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        ) : (
          <Button
            size="sm"
            variant="default"
            className="h-9 min-w-[140px]"
            disabled={triggering || isRunning || showStarting}
            onClick={() => void handleGenerateOrRegenerateAudio()}
          >
            {triggering ? (
              <>
                <RefreshCw className="h-3.5 w-3.5 animate-spin mr-1.5" />
                Starting…
              </>
            ) : isRunning || showStarting ? (
              <>
                <RefreshCw className="h-3.5 w-3.5 animate-spin mr-1.5" />
                {statusDisplay}
              </>
            ) : (
              <>
                <Mic className="h-3.5 w-3.5 mr-1.5" />
                {hasAudio ? "Regenerate audio" : "Generate audio"}
              </>
            )}
          </Button>
        )}
      </div>

      <p className="text-xs text-muted-foreground">
        Full narration queue:{" "}
        <Link href="/dashboard/stories/to-speech" className="text-primary underline-offset-4 hover:underline inline-flex items-center gap-0.5">
          Narration tab <ExternalLink className="h-3 w-3" />
        </Link>
      </p>
    </div>
  );
}
