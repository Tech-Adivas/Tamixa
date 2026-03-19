"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import type { LibraryStorySummary, CreateLibraryStoryRequest, PagedResponse, PipelineStatusResponse } from "@/types/api";
import { STORY_CATEGORIES, AGE_GROUPS, MIN_WORD_COUNT } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { RefreshCw, Eye, MessageSquare, CheckCircle, XCircle, ChevronLeft, ChevronRight } from "lucide-react";
import { useAuth } from "@/contexts/auth-context";
import { usePipelineActive } from "@/contexts/pipeline-active-context";
import { useActionResult } from "@/contexts/action-result-context";
import { canModerateStories } from "@/lib/admin-roles";
import { cn, parseJsonStoryContent } from "@/lib/utils";

const PAGE_SIZE = 10;
const LANG_LABELS: Record<string, string> = {
  ta: "Tamil",
  hi: "Hindi",
  en: "English",
  te: "Telugu",
  kn: "Kannada",
  ml: "Malayalam",
};

const PIPELINE_META_KEYS = ["processing", "progress", "overallStatus", "reviewedLanguages", "allLanguagesReviewed"];

function langEntries(status?: PipelineStatusResponse | Record<string, string | undefined> | null) {
  if (!status) return [];
  return Object.entries(status).filter(([k]) => !PIPELINE_META_KEYS.includes(k));
}

function isPipelineFullyComplete(status?: PipelineStatusResponse | Record<string, string | undefined> | null): boolean {
  if (!status) return false;
  if (status.overallStatus === "COMPLETED" || status.overallStatus === "READY_FOR_REVIEW") return true;
  const entries = langEntries(status);
  return entries.length > 0 && entries.every(([, s]) => typeof s === "string" && s.trim() === "COMPLETED");
}

/** All languages in pipeline status (for display when some are not yet COMPLETED). */
function getAllPipelineLanguages(status?: PipelineStatusResponse | Record<string, string | undefined> | null): Array<{ lang: string; stage: string }> {
  return langEntries(status).map(([lang, stage]) => ({
    lang,
    stage: typeof stage === "string" ? stage : "PENDING",
  }));
}

/** True when pipeline is actively processing (TRANSLATING, REWRITING, TTS_PROCESSING). Disable approve/edit during this. */
function isPipelineInProgress(status?: PipelineStatusResponse | Record<string, string | undefined> | null): boolean {
  if (status?.overallStatus === "COMPLETED" || status?.overallStatus === "READY_FOR_REVIEW") return false;
  const entries = langEntries(status);
  return entries.some(
    ([, s]) =>
      s === "TRANSLATING" ||
      s === "REWRITING" ||
      s === "TTS_PROCESSING" ||
      (typeof s === "string" && (s.startsWith("TRANSLATING") || s.startsWith("REWRITING") || s.startsWith("TTS_PROCESSING")))
  );
}

const LANG_SHORT: Record<string, string> = {
  ta: "Ta",
  hi: "Hi",
  en: "En",
  te: "Te",
  kn: "Kn",
  ml: "Ml",
};

export default function ApproveTabPage() {
  const { user } = useAuth();
  const { refresh: refreshPipelineActive, registerTriggered } = usePipelineActive();
  const canModerate = canModerateStories(user ?? null);
  // Only disable Approve/Edit for the specific story whose pipeline is in progress—not all stories
  const [data, setData] = useState<PagedResponse<LibraryStorySummary> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [pipelineStatusMap, setPipelineStatusMap] = useState<
    Record<number, PipelineStatusResponse>
  >({});
  const [approvingId, setApprovingId] = useState<number | null>(null);
  const [syncingId, setSyncingId] = useState<number | null>(null);
  const [rejectingId, setRejectingId] = useState<number | null>(null);
  const [rejectModalOpen, setRejectModalOpen] = useState(false);
  const [rejectStoryId, setRejectStoryId] = useState<number | null>(null);
  const [rejectNotes, setRejectNotes] = useState("");
  const { showSuccess, showError } = useActionResult();

  const [editStoryModalOpen, setEditStoryModalOpen] = useState(false);
  const [editStoryId, setEditStoryId] = useState<number | null>(null);
  const [editForm, setEditForm] = useState<CreateLibraryStoryRequest | null>(null);
  const [editStoryLoading, setEditStoryLoading] = useState(false);
  const [editStorySubmitting, setEditStorySubmitting] = useState(false);
  const [editCoverVideoUrl, setEditCoverVideoUrl] = useState<string | null>(null);

  const [translationModalOpen, setTranslationModalOpen] = useState(false);
  const [translationStoryId, setTranslationStoryId] = useState<number | null>(null);
  const [translationLang, setTranslationLang] = useState<string | null>(null);
  const [translationForm, setTranslationForm] = useState<{
    title: string;
    content: string;
    moral: string;
  } | null>(null);
  const [translationComments, setTranslationComments] = useState("");
  const [translationLoading, setTranslationLoading] = useState(false);
  /** Per story: languages the user has viewed (closed the view modal). Approve enabled only when all completed langs reviewed. */
  const [reviewedByStory, setReviewedByStory] = useState<Record<number, Set<string>>>({});
  /** Per story: user has ticked "Reject" checkbox in the view modal. Enables the common Reject button. */
  const [rejectMarkedByStory, setRejectMarkedByStory] = useState<Record<number, boolean>>({});

  const loadPipelineStatuses = useCallback((ids: number[]) => {
    if (ids.length === 0) return;
    api.admin
      .getLibraryStoryPipelineStatusBatch(ids)
      .then((batch) => {
        if (!batch || Object.keys(batch).length === 0) return;
        setPipelineStatusMap((m) => ({ ...m, ...batch }));
        setReviewedByStory((prev) => {
          const next = { ...prev };
          for (const [idStr, status] of Object.entries(batch)) {
            const id = Number(idStr);
            const reviewedStr = status?.reviewedLanguages;
            const dbLangs =
              typeof reviewedStr === "string" && reviewedStr.trim()
                ? reviewedStr.split(",").map((s) => s.trim().toLowerCase()).filter(Boolean)
                : [];
            next[id] = new Set(dbLangs);
          }
          return next;
        });
      })
      .catch(() => {});
  }, []);

  const refreshPipelineStatusForStory = useCallback(
    async (storyId: number) => {
      setSyncingId(storyId);
      try {
        const status = await api.admin.getLibraryStoryPipelineStatus(storyId);
        if (!status) return;
        setPipelineStatusMap((m) => ({ ...m, [storyId]: status }));
        const reviewedStr = status.reviewedLanguages;
        const dbLangs =
          typeof reviewedStr === "string" && reviewedStr.trim()
            ? reviewedStr.split(",").map((s) => s.trim().toLowerCase()).filter(Boolean)
            : [];
        setReviewedByStory((prev) => ({
          ...prev,
          [storyId]: new Set(dbLangs),
        }));
        if (status.allLanguagesReviewed === "true") {
          showSuccess("Synced", "All languages reviewed. Approve is now enabled.");
        }
      } catch {
        showError("Sync failed", "Could not refresh from database. Check your connection.");
      } finally {
        setSyncingId(null);
      }
    },
    [showSuccess, showError]
  );

  const markLanguageReviewed = useCallback(
    async (storyId: number, lang: string) => {
      const normalizedLang = lang.trim().toLowerCase();
      if (!normalizedLang) return;
      try {
        await api.admin.markReviewedLanguages(storyId, [normalizedLang]);
        setReviewedByStory((prev) => {
          const next = { ...prev };
          const set = new Set(next[storyId] ?? []);
          set.add(normalizedLang);
          next[storyId] = set;
          return next;
        });
        await refreshPipelineStatusForStory(storyId);
      } catch (e) {
        showError("Failed to save review", e instanceof Error ? e.message : "Could not persist reviewed state");
      }
    },
    [showError, refreshPipelineStatusForStory]
  );

  const handleTranslationModalClose = useCallback((open: boolean) => {
    setTranslationModalOpen(open);
  }, []);

  const handleMarkLanguageReviewed = useCallback(async () => {
    if (translationStoryId != null && translationLang) {
      await markLanguageReviewed(translationStoryId, translationLang);
    }
    setTranslationModalOpen(false);
  }, [translationStoryId, translationLang, markLanguageReviewed]);

  const load = useCallback((silent = false) => {
    if (!silent) {
      setLoading(true);
      setError(null);
    }
    api.admin
      .getLibraryStories(page, PAGE_SIZE, "PUBLISHED")
      .then((res) => {
        setData(res);
        const marked: Record<number, boolean> = {};
        (res?.content ?? []).forEach((r) => {
          if (r.rejectMarkedAt) marked[r.id] = true;
        });
        setRejectMarkedByStory(marked);
      })
      .catch((e) => {
        if (!silent) {
          setError(e instanceof Error ? e.message : "Failed to load");
          setData(null);
        }
      })
      .finally(() => !silent && setLoading(false));
  }, [page]);

  useEffect(load, [load]);

  useEffect(() => {
    if (!data?.content?.length) return;
    loadPipelineStatuses(data.content.map((r) => r.id));
  }, [data?.content, loadPipelineStatuses]);

  // Show all stories pending review (not yet approved), including those still in pipeline
  const pendingStories = (data?.content ?? []).filter((row) => !row.narrationApprovedAt);

  const handleApprove = async (storyId: number) => {
    setApprovingId(storyId);
    registerTriggered(storyId); // Show pipeline banner immediately
    try {
      await api.admin.approveLibraryStoryNarration(storyId);
      showSuccess("Approved for delivery", "Story is content-approved. Go to Narration (Story to Speech) and click Generate audio to create audio. Story will appear on the app when audio is ready.");
      load(true);
      setPipelineStatusMap((m) => {
        const next = { ...m };
        delete next[storyId];
        return next;
      });
      refreshPipelineActive();
      setTimeout(refreshPipelineActive, 1500);
    } catch (e) {
      showError("Approve failed", e instanceof Error ? e.message : "Failed to approve");
    } finally {
      setApprovingId(null);
    }
  };

  const openRejectModal = (storyId: number) => {
    setRejectStoryId(storyId);
    setRejectNotes("");
    setRejectModalOpen(true);
  };

  const handleReject = async () => {
    if (rejectStoryId == null) return;
    setRejectingId(rejectStoryId);
    try {
      await api.admin.rejectLibraryStory(rejectStoryId, rejectNotes || undefined);
      showSuccess("Story rejected", "The story has been rejected and removed from the review queue.");
      setRejectModalOpen(false);
      setRejectStoryId(null);
      setRejectNotes("");
      setRejectMarkedByStory((prev) => {
        const next = { ...prev };
        delete next[rejectStoryId];
        return next;
      });
      load(true);
      setPipelineStatusMap((m) => {
        const next = { ...m };
        delete next[rejectStoryId];
        return next;
      });
    } catch (e) {
      showError("Reject failed", e instanceof Error ? e.message : "Failed to reject");
    } finally {
      setRejectingId(null);
    }
  };

  // eslint-disable-next-line @typescript-eslint/no-unused-vars -- Edit full story modal; wire to UI when needed
  const openEditStoryModal = (storyId: number) => {
    setEditStoryId(storyId);
    setEditForm(null);
    setEditCoverVideoUrl(null);
    setEditStoryModalOpen(true);
    setEditStoryLoading(true);
    api.admin
      .getLibraryStory(storyId)
      .then((story) => {
        const contentRaw = story.content ?? "";
        const parsed = parseJsonStoryContent(contentRaw);
        const resolved = parsed ?? { content: contentRaw };
        setEditForm({
          title: resolved.title ?? story.title ?? "",
          content: resolved.content,
          theme: resolved.theme ?? story.theme,
          language: story.language ?? "ta",
          age: story.age,
          childName: story.childName ?? "Child",
          moral: resolved.moral ?? story.moral ?? "",
          status: story.status ?? "DRAFT",
          coverImageUrl: story.coverImageUrl ?? "",
          emotionMode: story.emotionMode ?? "CALM",
        });
        setEditCoverVideoUrl(story.coverVideoUrl ?? null);
      })
      .catch((e) => {
        showError("Failed to load story", e instanceof Error ? e.message : "Load failed");
        setEditStoryModalOpen(false);
      })
      .finally(() => setEditStoryLoading(false));
  };

  const handleEditStorySubmit = async () => {
    if (!editForm || !editStoryId) return;
    const wordCount = editForm.content?.split(/\s+/).filter((w) => w.trim()).length ?? 0;
    if (wordCount < MIN_WORD_COUNT) {
      showError("Validation failed", `Minimum ${MIN_WORD_COUNT} words required.`);
      return;
    }
    setEditStorySubmitting(true);
    registerTriggered(editStoryId); // Show pipeline banner immediately
    try {
      await api.admin.updateLibraryStory(editStoryId, {
        ...editForm,
        title: editForm.title?.trim() || null,
        moral: editForm.moral?.trim() || null,
        status: "PUBLISHED",
        coverImageUrl: editForm.coverImageUrl ?? null,
        coverVideoUrl: editCoverVideoUrl ?? null,
      });
      await api.admin.approveLibraryStoryNarration(editStoryId);
      showSuccess("Approved for delivery", "Pipeline is running (translate + audio). The story will display on the app when ready.");
      setEditStoryModalOpen(false);
      setEditStoryId(null);
      setEditForm(null);
      load(true);
      setPipelineStatusMap((m) => {
        const next = { ...m };
        delete next[editStoryId];
        return next;
      });
      refreshPipelineActive();
      setTimeout(refreshPipelineActive, 1500);
    } catch (e) {
      showError("Submit failed", e instanceof Error ? e.message : "Submit failed");
    } finally {
      setEditStorySubmitting(false);
    }
  };

  const openTranslationModal = (storyId: number, language: string) => {
    setTranslationStoryId(storyId);
    setTranslationLang(language);
    setTranslationForm(null);
    setTranslationComments("");
    setTranslationModalOpen(true);
    setTranslationLoading(true);
    const langCode = language.trim().toLowerCase();
    api.admin
      .getLibraryStory(storyId, langCode)
      .then((story) => {
        const contentToEdit = (story.narratedContent?.trim() || story.content) ?? "";
        const parsed = parseJsonStoryContent(contentToEdit);
        const resolved = parsed ?? { content: contentToEdit };
        // Prefer API-returned title/moral (from DB) so they stay in the correct language
        setTranslationForm({
          title: (story.title ?? resolved.title ?? "")?.trim() || "",
          content: resolved.content,
          moral: (story.moral ?? resolved.moral ?? "")?.trim() || "",
        });
      })
      .catch((e) => {
        const msg = e instanceof Error ? e.message : "Load failed";
        showError(
          "Translation not loaded",
          msg.includes("404") || msg.includes("not found")
            ? `${LANG_LABELS[language] ?? language} content is not ready yet. Run the pipeline so this language is translated. If you still see Tamil for other languages, set TRANSLATION_PROVIDER=openai on the backend and re-run the pipeline.`
            : msg
        );
        setTranslationModalOpen(false);
      })
      .finally(() => setTranslationLoading(false));
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="page-header">Review</h1>
        <p className="page-subheader mt-1">
          Stories appear here after you <strong>Submit for review</strong> in Library. The pipeline runs on submit (translate + TTS for all languages). Refresh to see content as it populates. <strong>Approve</strong> to publish or <strong>Reject</strong> to send back. If the pipeline gets stuck, use Run pipeline in Library.
        </p>
      </div>

      <Card className="border-border shadow-sm overflow-hidden">
        <CardHeader className="card-header-responsive border-b bg-muted/30 px-4 py-3 sm:px-6 sm:py-4">
          <CardTitle className="text-base font-semibold tracking-tight">
            Stories ready for verification
          </CardTitle>
          <Button
              variant="outline"
              size="sm"
              className="h-8"
              onClick={() => load()}
              disabled={loading}
            >
              <RefreshCw className={cn("h-4 w-4 mr-2", loading && "animate-spin")} />
              Refresh
            </Button>
        </CardHeader>
        <CardContent className="p-0">
          {error && (
            <div className="px-6 py-4 bg-destructive/10 border-b">
              <p className="text-sm text-destructive">{error}</p>
            </div>
          )}
          {loading && !data ? (
            <div className="px-6 py-12 text-center text-muted-foreground">
              Loading…
            </div>
          ) : pendingStories.length === 0 ? (
            <div className="px-6 py-12 text-center">
              <p className="text-muted-foreground mb-2">No stories pending review.</p>
              <p className="text-sm text-muted-foreground mb-4">
                Submit stories for review from Story library; they will appear here. Approve them for delivery to publish on the app.
              </p>
              <Link href="/dashboard/stories">
                <Button variant="outline" size="sm">Go to Library</Button>
              </Link>
            </div>
          ) : (
            <>
              <div className="w-full overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b border-border bg-[hsl(var(--table-header-bg))] text-[hsl(var(--table-header-foreground))] font-semibold">
                      <th className="w-16 px-4 py-3 text-right">ID</th>
                      <th className="min-w-[180px] px-4 py-3 text-left">Title</th>
                      <th className="min-w-[140px] px-4 py-3 text-left">Languages (view)</th>
                      <th className="w-28 px-4 py-3 text-left">Created</th>
                      <th className="w-28 px-4 py-3 text-left">Modified</th>
                      <th className="w-32 px-4 py-3 text-left">Owner</th>
                      <th className="w-44 px-4 py-3 text-right">Approval</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                    {pendingStories.map((row) => {
                      const pipelineStatus = pipelineStatusMap[row.id];
                      const allLangs = getAllPipelineLanguages(pipelineStatus);
                      const pipelineComplete = isPipelineFullyComplete(pipelineStatus);
                      const pipelineInProgress = isPipelineInProgress(pipelineStatus);
                      const apiReviewedStr = pipelineStatus?.reviewedLanguages;
                      const dbReviewedLangs = new Set<string>();
                      if (typeof apiReviewedStr === "string" && apiReviewedStr.trim()) {
                        apiReviewedStr.split(",").map((s) => s.trim().toLowerCase()).filter(Boolean).forEach((l) => dbReviewedLangs.add(l));
                      }
                      const requiredLangs = allLangs.map(({ lang }) => lang.trim().toLowerCase());
                      // Use backend allLanguagesReviewed flag; fallback to client check if backend hasn't deployed
                      const apiAllReviewed = pipelineStatus?.allLanguagesReviewed;
                      const allLanguagesReviewed =
                        apiAllReviewed === "true" ||
                        (apiAllReviewed == null &&
                          requiredLangs.length > 0 &&
                          requiredLangs.every((l) => dbReviewedLangs.has(l)));
                      const reviewedLangsForBadge = new Set(dbReviewedLangs);
                      (reviewedByStory[row.id] ?? []).forEach((l) => reviewedLangsForBadge.add(l.trim().toLowerCase()));
                      // Enable Approve when all languages reviewed (from DB). Don't block on pipeline progress.
                      const approveDisabled =
                        approvingId === row.id ||
                        requiredLangs.length === 0 ||
                        !allLanguagesReviewed;
                      const rejectDisabled =
                        rejectingId === row.id ||
                        pipelineInProgress ||
                        !rejectMarkedByStory[row.id];
                      return (
                        <tr
                          key={row.id}
                          className="transition-colors hover:bg-muted/20"
                          title={!pipelineComplete ? "Approve to run pipeline (translate + TTS) and publish" : "Approve for delivery"}
                        >
                          <td className="px-4 py-3 font-mono font-medium text-right align-middle tabular-nums">{row.id}</td>
                          <td className="px-4 py-3 text-left align-middle">
                            <span className="truncate block max-w-[220px]" title={row.title ?? undefined}>
                              {row.title || "—"}
                            </span>
                          </td>
                          <td className="px-4 py-3 text-left align-middle">
                            <div className="flex flex-wrap items-center gap-1">
                              {pipelineStatus === undefined ? (
                                <span className="text-muted-foreground text-xs">Loading…</span>
                              ) : (
                                <>
                                  {allLangs.map(({ lang, stage }) => {
                                  const isCompleted = stage.startsWith("COMPLETED");
                                  const isReviewed = reviewedLangsForBadge.has(lang.trim().toLowerCase());
                                  return (
                                    <div
                                      key={lang}
                                      className={cn(
                                        "group inline-flex items-center gap-0.5 rounded-md border px-2 py-1.5 text-sm transition-colors",
                                        isReviewed
                                          ? "border-green-500/60 bg-green-500/15 text-green-800 dark:text-green-200"
                                          : isCompleted
                                            ? "border-border bg-muted/40"
                                            : "border-border bg-muted/20 text-muted-foreground"
                                      )}
                                      title={`${LANG_LABELS[lang] ?? lang}${isReviewed ? " (reviewed)" : ` — view & mark reviewed (${stage})`}`}
                                    >
                                      <span className="font-medium text-foreground w-6 text-center">
                                        {LANG_SHORT[lang] ?? lang}
                                      </span>
                                      <Button
                                        type="button"
                                        variant="ghost"
                                        size="sm"
                                        className="h-6 w-6 p-0 min-w-0"
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          openTranslationModal(row.id, lang);
                                        }}
                                        title={`View ${LANG_LABELS[lang] ?? lang}`}
                                      >
                                        <Eye className="h-3 w-3" />
                                      </Button>
                                    </div>
                                  );
                                })}
                                </>
                              )}
                            </div>
                          </td>
                          <td className="px-4 py-3 text-left align-middle text-muted-foreground text-sm whitespace-nowrap">
                            {new Date(row.createdAt).toLocaleDateString()}
                          </td>
                          <td className="px-4 py-3 text-left align-middle text-muted-foreground text-sm whitespace-nowrap">
                            {row.modifiedAt ? new Date(row.modifiedAt).toLocaleDateString() : "—"}
                          </td>
                          <td className="px-4 py-3 text-left align-middle text-muted-foreground text-sm">
                            {row.storyOwner || "system"}
                          </td>
                          <td className="px-4 py-3 text-right align-middle">
                            <span className="inline-block flex flex-wrap items-center gap-2 justify-end">
                              {canModerate && (
                                <>
                                  {approveDisabled && (
                                    <Button
                                      size="sm"
                                      variant="outline"
                                      className="h-8"
                                      disabled={syncingId === row.id}
                                      onClick={() => refreshPipelineStatusForStory(row.id)}
                                      title="Refresh reviewed state from database"
                                    >
                                      <RefreshCw
                                        className={cn("h-3.5 w-3.5 mr-1.5", syncingId === row.id && "animate-spin")}
                                      />
                                      {syncingId === row.id ? "Syncing…" : "Sync"}
                                    </Button>
                                  )}
                                  <Button
                                    size="sm"
                                    variant="default"
                                    className="h-8"
                                    disabled={approveDisabled}
                                    onClick={() => handleApprove(row.id)}
                                    title={
                                      approveDisabled
                                        ? pipelineInProgress
                                          ? "Pipeline in progress"
                                          : requiredLangs.length === 0
                                            ? "Waiting for pipeline languages to load"
                                            : !allLanguagesReviewed
                                              ? `API allReviewed=${apiAllReviewed ?? "missing"}; DB reviewed: ${Array.from(dbReviewedLangs).join(",") || "none"}; missing: ${requiredLangs.filter((l) => !dbReviewedLangs.has(l)).join(",") || "all"}. Click Sync to refresh.`
                                              : "Approving…"
                                        : "Approve for delivery"
                                    }
                                  >
                                    {approvingId === row.id ? (
                                      <>
                                        <RefreshCw className="h-3.5 w-3.5 animate-spin mr-1.5" />
                                        Approving…
                                      </>
                                    ) : (
                                      <>
                                        <CheckCircle className="h-3.5 w-3.5 mr-1.5" />
                                        Approve
                                      </>
                                    )}
                                  </Button>
                                  <Button
                                    size="sm"
                                    variant="outline"
                                    className="h-8 text-destructive hover:text-destructive hover:bg-destructive/10"
                                    disabled={rejectDisabled}
                                    onClick={() => openRejectModal(row.id)}
                                    title={
                                      rejectDisabled
                                        ? pipelineInProgress
                                          ? "Pipeline in progress"
                                          : !rejectMarkedByStory[row.id]
                                            ? "Tick the Reject checkbox in a language view to enable"
                                            : "Rejecting…"
                                        : "Reject — removes story from review queue"
                                    }
                                  >
                                    {rejectingId === row.id ? (
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
                                </>
                              )}
                            </span>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
              {/* Pagination */}
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
                      <ChevronLeft className="h-4 w-4 mr-1" />
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      className="h-8"
                      disabled={data.last || loading}
                      onClick={() => setPage((p) => p + 1)}
                    >
                      Next
                      <ChevronRight className="h-4 w-4 ml-1" />
                    </Button>
                  </div>
                </div>
              )}
              {pendingStories.length > 0 && (
                <div className="px-6 py-2 border-t text-sm text-muted-foreground">
                  {pendingStories.length} story{pendingStories.length === 1 ? "" : "s"} on this page ready for verification
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>

      {/* Reject story confirmation */}
      <Dialog open={rejectModalOpen} onOpenChange={setRejectModalOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Reject story</DialogTitle>
            <DialogDescription>
              Rejecting removes the story from the review queue. The creator can edit and submit again from Story library.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-3 py-2">
            <Label htmlFor="reject-notes">Notes (optional)</Label>
            <Input
              id="reject-notes"
              placeholder="e.g. Content needs revision, tone too formal…"
              value={rejectNotes}
              onChange={(e) => setRejectNotes(e.target.value)}
              className="mt-1"
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRejectModalOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              disabled={rejectingId != null}
              onClick={handleReject}
            >
              {rejectingId != null ? "Rejecting…" : "Reject"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit story (full) modal */}
      <Dialog open={editStoryModalOpen} onOpenChange={setEditStoryModalOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Edit story</DialogTitle>
            <DialogDescription>
              Update content and details, then submit for delivery to publish on the app.
            </DialogDescription>
          </DialogHeader>
          {editStoryLoading ? (
            <p className="py-6 text-center text-muted-foreground">Loading story…</p>
          ) : editForm ? (
            <div className="space-y-4 py-2">
              <div>
                <Label htmlFor="edit-title">Title</Label>
                <Input
                  id="edit-title"
                  value={editForm.title ?? ""}
                  onChange={(e) =>
                    setEditForm((f) => (f ? { ...f, title: e.target.value } : f))
                  }
                  className="mt-1"
                />
              </div>
              <div>
                <Label htmlFor="edit-theme">Category</Label>
                <Select
                  value={editForm.theme}
                  onValueChange={(v) =>
                    setEditForm((f) => (f ? { ...f, theme: v } : f))
                  }
                >
                  <SelectTrigger className="mt-1">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {STORY_CATEGORIES.map((c) => (
                      <SelectItem key={c} value={c}>{c}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label htmlFor="edit-content">Story content *</Label>
                <textarea
                  id="edit-content"
                  value={editForm.content}
                  onChange={(e) =>
                    setEditForm((f) => (f ? { ...f, content: e.target.value } : f))
                  }
                  className="mt-1 flex min-h-[160px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                />
                <p className="text-xs text-muted-foreground mt-1">
                  {editForm.content?.split(/\s+/).filter((w) => w.trim()).length ?? 0} / {MIN_WORD_COUNT} words
                </p>
              </div>
              <div>
                <Label htmlFor="edit-moral">Moral (optional)</Label>
                <Input
                  id="edit-moral"
                  value={editForm.moral ?? ""}
                  onChange={(e) =>
                    setEditForm((f) => (f ? { ...f, moral: e.target.value } : f))
                  }
                  className="mt-1"
                />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label>Age group</Label>
                  <Select
                    value={String(editForm.age)}
                    onValueChange={(v) =>
                      setEditForm((f) => (f ? { ...f, age: parseInt(v, 10) } : f))
                    }
                  >
                    <SelectTrigger className="mt-1">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {AGE_GROUPS.map((a) => (
                        <SelectItem key={a.value} value={String(a.value)}>{a.label}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <Label htmlFor="edit-childName">Child name placeholder</Label>
                  <Input
                    id="edit-childName"
                    value={editForm.childName ?? "Child"}
                    onChange={(e) =>
                      setEditForm((f) => (f ? { ...f, childName: e.target.value } : f))
                    }
                    className="mt-1"
                  />
                </div>
              </div>
              <div>
                <Label htmlFor="edit-cover">Cover image URL (optional)</Label>
                <Input
                  id="edit-cover"
                  value={editForm.coverImageUrl ?? ""}
                  onChange={(e) =>
                    setEditForm((f) =>
                      f ? { ...f, coverImageUrl: e.target.value || null } : f
                    )
                  }
                  className="mt-1"
                  placeholder="Leave blank to keep existing"
                />
              </div>
            </div>
          ) : null}
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditStoryModalOpen(false)} disabled={editStorySubmitting}>
              Cancel
            </Button>
            {editForm && (
              <Button onClick={handleEditStorySubmit} disabled={editStorySubmitting || editStoryLoading}>
                {editStorySubmitting ? "Submitting…" : "Submit for delivery"}
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* View & approve translation (per language) modal */}
      <Dialog open={translationModalOpen} onOpenChange={handleTranslationModalClose}>
        <DialogContent className="max-w-xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              View {translationLang ? LANG_LABELS[translationLang] ?? translationLang : ""}
            </DialogTitle>
            <DialogDescription>
              Review content for this language. Click &ldquo;Have reviewed&rdquo; when done. Once all languages are reviewed, the Approve button in the table will be enabled.
            </DialogDescription>
          </DialogHeader>
          {translationLoading ? (
            <p className="py-6 text-center text-muted-foreground">Loading…</p>
          ) : translationForm ? (
            <div className="space-y-4 py-2">
              {translationStoryId != null &&
                translationLang &&
                !String(pipelineStatusMap[translationStoryId]?.[translationLang] ?? "").startsWith("COMPLETED") && (
                  <div className="rounded-md border border-amber-200 bg-amber-50 dark:border-amber-800 dark:bg-amber-950/40 px-3 py-2 text-sm text-amber-800 dark:text-amber-200">
                    Pipeline not complete for this language yet. Content may be placeholder.
                  </div>
                )}
              <div>
                <Label className="text-muted-foreground">Title</Label>
                <div className="mt-1 rounded-md border border-input bg-muted/30 px-3 py-2 text-sm">
                  {translationForm.title || "—"}
                </div>
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
                <div className="mt-1 rounded-md border border-input bg-muted/30 px-3 py-2 text-sm">
                  {translationForm.moral || "—"}
                </div>
              </div>
              <div>
                <Label htmlFor="translation-comments">
                  <MessageSquare className="h-3.5 w-3.5 inline mr-1" />
                  Comments (optional)
                </Label>
                <textarea
                  id="translation-comments"
                  value={translationComments}
                  onChange={(e) => setTranslationComments(e.target.value)}
                  placeholder="Notes for this language…"
                  className="mt-1 flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                />
              </div>
              {translationStoryId != null && (
                <div className="flex items-center gap-2 pt-2 border-t">
                  <input
                    type="checkbox"
                    id="translation-reject-checkbox"
                    checked={!!rejectMarkedByStory[translationStoryId]}
                    onChange={async (e) => {
                      const checked = e.target.checked;
                      try {
                        await api.admin.setRejectMarked(translationStoryId, checked);
                        setRejectMarkedByStory((prev) => ({
                          ...prev,
                          [translationStoryId]: checked,
                        }));
                      } catch (err) {
                        showError("Failed to save", err instanceof Error ? err.message : "Could not persist");
                      }
                    }}
                    className="h-4 w-4 rounded border-input"
                  />
                  <Label
                    htmlFor="translation-reject-checkbox"
                    className="text-sm font-medium cursor-pointer text-destructive"
                  >
                    Reject this story
                  </Label>
                  <span className="text-xs text-muted-foreground">
                    (Enables the Reject button in the table)
                  </span>
                </div>
              )}
            </div>
          ) : null}
          <DialogFooter>
            <Button variant="outline" onClick={() => handleTranslationModalClose(false)}>
              Close
            </Button>
            {translationForm && translationStoryId != null && translationLang && (
              <Button onClick={handleMarkLanguageReviewed}>
                {rejectMarkedByStory[translationStoryId] ? "Submit" : "Have reviewed"}
              </Button>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
