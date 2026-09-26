"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import { api, getApiBaseUrl } from "@/lib/api";
import type { LibraryStorySummary, PagedResponse, PipelineStatusResponse, StoryWithIssues } from "@/types/api";
import { STORY_CATEGORIES } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
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
import { RefreshCw, Plus, CheckSquare, Square, Filter, Pencil, Trash2, Eye, Eraser, MoreHorizontal, CheckCircle, Circle, FileText, Clock, ClipboardCheck, Play } from "lucide-react";
import { useAuth } from "@/contexts/auth-context";
import { usePipelineActive } from "@/contexts/pipeline-active-context";
import { useActionResult } from "@/contexts/action-result-context";
import { isSuperAdmin, canManageStories } from "@/lib/admin-roles";
import {
  isLibraryStoryPipelineActivelyRunning,
  REGENERATE_THEN_TRANSLATIONS_HELP,
  isLibraryStoryPipelineMetaKey,
  adminStoryLanguageLabel,
  adminStoryEmotionModeLabel,
  ADMIN_STORY_LANGUAGE_SHORT,
} from "@/lib/library-story-workflow";
import { cn } from "@/lib/utils";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { PageHeader } from "@/components/layout/page-header";

const PAGE_SIZE = 20;
/** Languages for View story tabs. Translated content is stored in backend (story_translations) and returned by GET /stories/:id?language=... */
const VIEW_LANGUAGES: Array<{ code: string; label: string }> = [
  { code: "ta", label: "Tamil" },
  { code: "en", label: "English" },
  { code: "hi", label: "Hindi" },
  { code: "te", label: "Telugu" },
  { code: "kn", label: "Kannada" },
  { code: "ml", label: "Malayalam" },
];
/** Resolve cover URL for img src (relative path → full API URL). */
function resolveCoverSrc(coverImageUrl: string | null | undefined): string | null {
  if (!coverImageUrl?.trim()) return null;
  const u = coverImageUrl.trim();
  if (u.startsWith("http://") || u.startsWith("https://")) return u;
  const base = getApiBaseUrl();
  if (base) return u.startsWith("/") ? `${base}${u}` : `${base}/${u}`;
  return u.startsWith("/") ? u : null; // SSR: relative path, same-origin
}

/** Parse "STATUS" or "STATUS — error message" from pipeline value */
function parsePipelineStatus(value: string): { status: string; error?: string } {
  if (!value) return { status: value ?? "" };
  const idx = value.indexOf(" — ");
  if (idx === -1) return { status: value };
  return { status: value.slice(0, idx).trim(), error: value.slice(idx + 3).trim() };
}

/** Review queue under unified statuses (V99). Legacy PROCESSING/READY kept for rows not yet migrated. */
const REVIEW_QUEUE_STATUSES = new Set(["SUBMITTED", "TRANSLATING", "CONTENT_REVIEW", "PROCESSING", "READY"]);

function isReviewQueueStatus(status: string | null | undefined): boolean {
  return status != null && REVIEW_QUEUE_STATUSES.has(status);
}

export default function LibraryStoriesPage() {
  const router = useRouter();
  const { user } = useAuth();
  const { registerTriggered } = usePipelineActive();
  const searchParams = useSearchParams();
  const canManage = canManageStories(user ?? null);
  const canDelete = isSuperAdmin(user?.role ?? "");
  const editedIdFromUrl = searchParams?.get("edited");
  const editedId = editedIdFromUrl ? parseInt(editedIdFromUrl, 10) : null;
  const [data, setData] = useState<PagedResponse<LibraryStorySummary> | null>(
    null
  );
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [approvedFilter, setApprovedFilter] = useState<"all" | "approved" | "not_approved">("all");
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [bulkAction, setBulkAction] = useState<"submitForReview" | "category" | "delete" | null>(
    null
  );
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState<{
    single: number | null;
    bulk: number[] | null;
  }>({ single: null, bulk: null });
  const { showSuccess, showError } = useActionResult();
  const [bulkCategory, setBulkCategory] = useState("");
  const [publishing, setPublishing] = useState(false);
  const [pipelineStatusMap, setPipelineStatusMap] = useState<
    Record<number, PipelineStatusResponse>
  >({});
  const [viewStoryId, setViewStoryId] = useState<number | null>(null);
  const [viewStoryData, setViewStoryData] = useState<(LibraryStorySummary & { content: string }) | null>(null);
  const [viewStoryLoading, setViewStoryLoading] = useState(false);
  const [viewCoverRefreshKey, setViewCoverRefreshKey] = useState(0);
  const [viewStoryTabLang, setViewStoryTabLang] = useState("ta");
  const [viewStoryContentByLang, setViewStoryContentByLang] = useState<Record<string, { content: string; title?: string; moral?: string }>>({});
  const [viewStoryLangLoading, setViewStoryLangLoading] = useState(false);
  const [viewPreviewLoading, setViewPreviewLoading] = useState(false);
  const viewAudioRef = useRef<{ element: HTMLAudioElement; objectUrl: string } | null>(null);
  const [clearAllConfirmOpen, setClearAllConfirmOpen] = useState(false);
  const [clearAllLoading, setClearAllLoading] = useState(false);
  const [storiesWithIssues, setStoriesWithIssues] = useState<StoryWithIssues[]>([]);
  const [issuesLoading, setIssuesLoading] = useState(false);
  const [issuesError, setIssuesError] = useState<string | null>(null);

  const loadStoryIssues = useCallback(async () => {
    setIssuesLoading(true);
    setIssuesError(null);
    try {
      const issues = await api.admin.getStoriesWithIssues();
      setStoriesWithIssues(issues);
    } catch (e) {
      setIssuesError(e instanceof Error ? e.message : "Failed to load story issues");
      setStoriesWithIssues([]);
    } finally {
      setIssuesLoading(false);
    }
  }, []);

  const load = useCallback((silent = false) => {
    if (!silent) {
      setLoading(true);
      setError(null);
    }
    const narrationApprovedParam =
      approvedFilter === "approved"
        ? true
        : approvedFilter === "not_approved"
          ? false
          : undefined;
    api.admin
      .getLibraryStories(
        page,
        PAGE_SIZE,
        statusFilter === "ALL" ? undefined : statusFilter,
        narrationApprovedParam
      )
      .then(setData)
      .catch((e) => {
        if (!silent) {
          setError(e instanceof Error ? e.message : "Failed to load");
          setData(null);
        }
      })
      .finally(() => !silent && setLoading(false));
  }, [page, statusFilter, approvedFilter]);

  const listRows = data?.content ?? [];

  useEffect(() => {
    setPage(0);
  }, [statusFilter, approvedFilter]);

  useEffect(load, [load]);
  useEffect(() => {
    loadStoryIssues();
  }, [loadStoryIssues]);

  const handleRefreshAll = useCallback(() => {
    load();
    loadStoryIssues();
  }, [load, loadStoryIssues]);

  const loadPipelineStatuses = useCallback((ids: number[]) => {
    if (ids.length === 0) return;
    api.admin
      .getLibraryStoryPipelineStatusBatch(ids)
      .then((batch) => {
        if (!batch || Object.keys(batch).length === 0) return;
        setPipelineStatusMap((m) => ({ ...m, ...batch }));
      })
      .catch(() => {});
  }, []);

  const handleClearAllAudioAndCache = useCallback(async () => {
    setClearAllLoading(true);
    try {
      await api.admin.clearAllAudioAndCache();
      setClearAllConfirmOpen(false);
      showSuccess("Cache cleared", "All audio and cache cleared. Refresh to see updated status.");
      load();
    } catch (e) {
      showError("Clear failed", e instanceof Error ? e.message : "Failed to clear audio and cache");
    } finally {
      setClearAllLoading(false);
    }
  }, [load, showSuccess, showError]);

  // Load full story and pipeline status when View is opened. Tamil comes from this call; other languages loaded on tab select (from story_translations via GET /stories/:id?language=...).
  useEffect(() => {
    if (viewStoryId == null) {
      setViewStoryData(null);
      setViewStoryTabLang("ta");
      setViewStoryContentByLang({});
      if (viewAudioRef.current) {
        URL.revokeObjectURL(viewAudioRef.current.objectUrl);
        viewAudioRef.current.element.pause();
        viewAudioRef.current = null;
      }
      return;
    }
    setViewStoryLoading(true);
    loadPipelineStatuses([viewStoryId]);
    api.admin
      .getLibraryStory(viewStoryId)
      .then((story) => {
        const s = story as LibraryStorySummary & { content: string };
        setViewStoryData(s);
        setViewStoryContentByLang({
          ta: { content: s.content ?? "", title: s.title ?? undefined, moral: s.moral ?? undefined },
        });
        setViewCoverRefreshKey(Date.now());
      })
      .catch(() => setViewStoryData(null))
      .finally(() => setViewStoryLoading(false));
  }, [viewStoryId, loadPipelineStatuses]);

  const loadViewStoryLanguage = useCallback(
    (lang: string) => {
      if (viewStoryId == null || lang === "ta") return;
      if (viewStoryContentByLang[lang] !== undefined) return;
      setViewStoryLangLoading(true);
      api.admin
        .getLibraryStory(viewStoryId, lang)
        .then((story) => {
          const s = story as LibraryStorySummary & { content: string };
          setViewStoryContentByLang((prev) => ({
            ...prev,
            [lang]: { content: s.content ?? "", title: s.title ?? undefined, moral: s.moral ?? undefined },
          }));
        })
        .catch(() => {})
        .finally(() => setViewStoryLangLoading(false));
    },
    [viewStoryId, viewStoryContentByLang]
  );

  const handleViewTabSelect = useCallback(
    (lang: string) => {
      setViewStoryTabLang(lang);
      loadViewStoryLanguage(lang);
    },
    [loadViewStoryLanguage]
  );

  const handleViewPreview = useCallback(
    async (lang: string) => {
      if (viewStoryId == null) return;
      if (viewAudioRef.current) {
        URL.revokeObjectURL(viewAudioRef.current.objectUrl);
        viewAudioRef.current.element.pause();
        viewAudioRef.current = null;
      }
      setViewPreviewLoading(true);
      try {
        const blob = await api.admin.getLibraryStoryPreviewAudioBlob(viewStoryId, lang);
        const objectUrl = URL.createObjectURL(blob);
        const audio = new Audio(objectUrl);
        viewAudioRef.current = { element: audio, objectUrl };
        audio.addEventListener("ended", () => {
          if (viewAudioRef.current) {
            URL.revokeObjectURL(viewAudioRef.current.objectUrl);
            viewAudioRef.current = null;
          }
        });
        await audio.play();
      } catch (e) {
        showError("Preview failed", e instanceof Error ? e.message : "Audio not ready for this language.");
      } finally {
        setViewPreviewLoading(false);
      }
    },
    [viewStoryId, showError]
  );

  useEffect(() => {
    if (data?.content?.length) {
      const ids = data.content.map((r) => r.id);
      if (editedId && !ids.includes(editedId)) ids.push(editedId);
      loadPipelineStatuses(ids);
    } else if (editedId) {
      loadPipelineStatuses([editedId]);
    }
  }, [data?.content, loadPipelineStatuses, editedId]);

  // Poll pipeline status for PUBLISHED/PROCESSING/READY stories. Use silent list refresh to avoid
  // loading flicker—poll does not show "Loading…" or disrupt navigation.
  useEffect(() => {
    if (!data?.content?.length && !editedId) return;
    const pollableStatuses = ["PROCESSING", "PUBLISHED", "READY"];
    const fromContent = (data?.content ?? [])
      .filter((r) => pollableStatuses.includes(r.status))
      .map((r) => r.id);
    const idsToPoll = Array.from(new Set([...fromContent, ...(editedId ? [editedId] : [])]));
    if (idsToPoll.length === 0) return;
    let tick = 0;
    const interval = setInterval(() => {
      loadPipelineStatuses(idsToPoll);
      tick++;
      if (tick % 3 === 0) load(true); // Silent refresh list every ~9s (no loading spinner)
    }, 3000);
    return () => clearInterval(interval);
  }, [data?.content, loadPipelineStatuses, load, editedId]);

  const toggleSelect = (id: number) => {
    setSelectedIds((s) => {
      const next = new Set(s);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const hasPendingOrFailedPipeline = (status?: PipelineStatusResponse | Record<string, string | undefined> | null) => {
    if (!status) return false;
    return Object.values(status).some(
      (s) =>
        s === "PENDING" ||
        s?.includes("FAILED") ||
        s === "TRANSLATING" ||
        s === "REWRITING" ||
        s === "TTS_PROCESSING"
    );
  };

  /** True when pipeline is actively processing (TRANSLATING, REWRITING, TTS_PROCESSING). Disable edit/approve/delete during this. */
  const isPipelineInProgress = (status?: PipelineStatusResponse | Record<string, string | undefined> | null) =>
    isLibraryStoryPipelineActivelyRunning(status);

  const isPipelineFullyComplete = (status?: PipelineStatusResponse | Record<string, string | undefined> | null) => {
    const entries = Object.entries(status ?? {}).filter(([k]) => !isLibraryStoryPipelineMetaKey(k));
    return entries.length > 0 && entries.every(([, s]) => s === "COMPLETED");
  };

  const getCompletedLanguages = (status?: PipelineStatusResponse | Record<string, string | undefined> | null) => {
    return Object.entries(status ?? {})
      .filter(([k, s]) => !isLibraryStoryPipelineMetaKey(k) && s === "COMPLETED")
      .map(([lang]) => lang);
  };

  /** Total pipeline languages (excludes meta keys). */
  const getTotalPipelineLanguages = (status?: PipelineStatusResponse | Record<string, string | undefined> | null) =>
    Object.keys(status ?? {}).filter((k) => !isLibraryStoryPipelineMetaKey(k)).length;

  const toggleSelectAll = () => {
    if (listRows.length === 0) return;
    const filteredIds = new Set(listRows.map((r) => r.id));
    if (selectedIds.size === filteredIds.size && [...filteredIds].every((id) => selectedIds.has(id))) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(filteredIds));
    }
  };

  const handleBulkSubmitForReview = async () => {
    if (selectedIds.size === 0) {
      showError("Invalid selection", "Please select at least one story.");
      return;
    }
    const ids = Array.from(selectedIds).map((id) => Number(id));
    ids.forEach((id) => registerTriggered(id)); // Show pipeline banner for each story
    setPublishing(true);
    try {
      const res = await api.admin.bulkSubmitForReview(ids);
      showSuccess(
        "Submitted to review queue",
        `${res.updated} draft ${res.updated === 1 ? "story" : "stories"} moved to Story for review (status PUBLISHED). Run Regenerate & sync / translations from each story’s Edit page when needed, then Narration → Generate audio after approval.`
      );
      setBulkAction(null);
      setSelectedIds(new Set());
      load();
    } catch (e) {
      showError("Bulk submit failed", e instanceof Error ? e.message : "Bulk submit for review failed");
    } finally {
      setPublishing(false);
    }
  };

  const handleDeleteSingle = async (id: number) => {
    setDeletingId(id);
    try {
      await api.admin.deleteLibraryStory(id);
      showSuccess(
        "Story moved to trash",
        "The story is hidden for ~30 days (configurable). Super Admin: GET soft-deleted list or POST restore before the scheduled purge removes it permanently."
      );
      setDeleteConfirmOpen({ single: null, bulk: null });
      load();
    } catch (e) {
      showError("Delete failed", e instanceof Error ? e.message : "Delete failed");
    } finally {
      setDeletingId(null);
    }
  };

  const handleBulkDelete = async () => {
    const ids = deleteConfirmOpen.bulk ?? Array.from(selectedIds);
    if (ids.length === 0) return;
    setPublishing(true);
    try {
      const res = await api.admin.bulkDeleteLibraryStories(ids);
      showSuccess(
        "Stories moved to trash",
        `${res.deleted} story${res.deleted === 1 ? "" : "s"} soft-deleted. Super Admin can restore from the API until retention purge.`
      );
      setDeleteConfirmOpen({ single: null, bulk: null });
      setBulkAction(null);
      setSelectedIds(new Set());
      load();
    } catch (e) {
      showError("Bulk delete failed", e instanceof Error ? e.message : "Bulk delete failed");
    } finally {
      setPublishing(false);
    }
  };

  const handleBulkCategory = async () => {
    if (selectedIds.size === 0 || !bulkCategory.trim()) {
      showError("Invalid selection", "Please select one or more stories and choose a category.");
      return;
    }
    try {
      const res = await api.admin.bulkUpdateCategory(
        Array.from(selectedIds),
        bulkCategory
      );
      showSuccess("Category updated", `${res.updated} story${res.updated === 1 ? "" : "s"} updated to "${bulkCategory}".`);
      setBulkAction(null);
      setBulkCategory("");
      setSelectedIds(new Set());
      load();
    } catch (e) {
      showError("Bulk update failed", e instanceof Error ? e.message : "Bulk update failed");
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <PageHeader
          title="Story library"
          description="Content ops · Filter by status · Bulk actions"
          breadcrumbs
        />
        {canManage && (
        <Link href="/dashboard/stories/new">
          <Button>
            <Plus className="h-4 w-4 mr-2" /> New story
          </Button>
        </Link>
      )}
      </div>

      <div className="rounded-lg border border-border/80 bg-muted/20 px-4 py-2.5 text-sm text-muted-foreground">
        <span className="font-medium text-foreground">Pipeline:</span>{" "}
        Open a story → <strong>Edit</strong>. {REGENERATE_THEN_TRANSLATIONS_HELP} Then <strong>Submit for review</strong> →{" "}
        <strong>Approve</strong> or <strong>Reject</strong> in Story for review. After approval, open <strong>Narration</strong> and use{" "}
        <strong>Generate audio</strong>. Progress appears in the Pipeline column and the top banner when a job is running.
      </div>

      <Card className="border-border/80">
        <CardContent className="pt-4">
          <div className="flex items-center justify-between gap-3">
            <div>
              <p className="text-sm font-medium">Status consistency audit</p>
              <p className="text-xs text-muted-foreground">
                Stories with translation/pipeline anomalies detected by backend checks.
              </p>
            </div>
            <Button variant="outline" size="sm" onClick={loadStoryIssues} disabled={issuesLoading}>
              {issuesLoading ? "Checking..." : "Recheck"}
            </Button>
          </div>
          <div className="mt-3 flex flex-wrap items-center gap-2">
            <Badge variant={storiesWithIssues.length > 0 ? "destructive" : "default"}>
              {storiesWithIssues.length} story{storiesWithIssues.length === 1 ? "" : "ies"} with issues
            </Badge>
            {issuesError ? <span className="text-xs text-destructive">{issuesError}</span> : null}
          </div>
          {storiesWithIssues.length > 0 ? (
            <div className="mt-3 flex flex-wrap gap-2">
              {storiesWithIssues.slice(0, 8).map((item) => (
                <div key={item.storyId} className="inline-flex items-center gap-1 rounded-md border px-1.5 py-1">
                  <Badge variant="outline">
                    #{item.storyId} ({item.issues.length})
                  </Badge>
                  <Link href={`/dashboard/stories/approve?storyId=${item.storyId}`}>
                    <Badge variant="secondary" className="hover:bg-muted">
                      Review
                    </Badge>
                  </Link>
                  <Link href={`/dashboard/stories/to-speech?storyId=${item.storyId}`}>
                    <Badge variant="secondary" className="hover:bg-muted">
                      Narration
                    </Badge>
                  </Link>
                </div>
              ))}
              {storiesWithIssues.length > 8 ? (
                <span className="text-xs text-muted-foreground self-center">
                  +{storiesWithIssues.length - 8} more
                </span>
              ) : null}
            </div>
          ) : null}
        </CardContent>
      </Card>


      {selectedIds.size > 0 && (
        <Card className="border-primary/30">
          <CardContent className="pt-4">
            <div className="flex items-center gap-4 flex-wrap">
              <span className="text-sm">
                {selectedIds.size} selected
              </span>
              {canManage && (
                <>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setBulkAction("submitForReview")}
                  >
                    Bulk submit for review
                  </Button>
                  <Button
                    size="sm"
                    variant="outline"
                    onClick={() => setBulkAction("category")}
                  >
                    Bulk category
                  </Button>
                </>
              )}
              {canDelete && (
                <Button
                  size="sm"
                  variant="outline"
                  className="text-destructive hover:text-destructive"
                  onClick={() => setDeleteConfirmOpen({ single: null, bulk: Array.from(selectedIds) })}
                >
                  Bulk delete
                </Button>
              )}
              <Button
                size="sm"
                variant="ghost"
                onClick={() => {
                  setSelectedIds(new Set());
                  setBulkAction(null);
                }}
              >
                Clear
              </Button>
              {bulkAction === "submitForReview" && (
                <Button size="sm" onClick={handleBulkSubmitForReview} disabled={publishing}>
                  {publishing ? "Submitting…" : "Confirm submit for review"}
                </Button>
              )}
              {bulkAction === "category" && (
                <div className="flex items-center gap-2">
                  <Select
                    value={bulkCategory}
                    onValueChange={setBulkCategory}
                  >
                    <SelectTrigger className="w-[160px]">
                      <SelectValue placeholder="Category" />
                    </SelectTrigger>
                    <SelectContent>
                      {STORY_CATEGORIES.map((c) => (
                        <SelectItem key={c} value={c}>
                          {c}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <Button size="sm" onClick={handleBulkCategory}>
                    Update
                  </Button>
                </div>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      <Card className="border-border/80 shadow-sm overflow-hidden">
        <CardHeader className="flex flex-col gap-4 pb-2 sm:flex-row sm:items-start sm:justify-between sm:space-y-0">
          <div className="space-y-1.5 min-w-0 pr-2">
            <CardTitle className="text-lg font-semibold tracking-tight">Story library · HD</CardTitle>
            <p className="text-sm text-muted-foreground leading-snug max-w-xl">
              This table is the curated catalog (database table{" "}
              <span className="font-mono text-xs">library_stories</span>
              ). Parent-generated AI stories live in the{" "}
              <Link href="/dashboard/moderation" className="text-primary underline-offset-4 hover:underline">
                Moderation
              </Link>{" "}
              queue and are not counted here. Use{" "}
              <span className="font-medium text-foreground">All</span> for status and approval to page through every
              library row (20 per page).
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-full min-w-[120px] max-w-[140px] h-9 sm:w-[140px]">
                <Filter className="h-4 w-4 mr-1 shrink-0" />
                <SelectValue placeholder="All status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All status</SelectItem>
                <SelectItem value="DRAFT">Draft</SelectItem>
                <SelectItem value="SUBMITTED,TRANSLATING">Pipeline processing</SelectItem>
                <SelectItem value="TRANSLATION_FAILED">Translation failed</SelectItem>
                <SelectItem value="CONTENT_REVIEW">Ready for content review</SelectItem>
                <SelectItem value="CHANGES_REQUESTED">Changes requested</SelectItem>
                <SelectItem value="REJECTED">Rejected</SelectItem>
                <SelectItem value="APPROVED">Approved (needs audio)</SelectItem>
                <SelectItem value="AUDIO_GENERATING,AUDIO_FAILED,AUDIO_REVIEW">Audio in progress / review</SelectItem>
                <SelectItem value="PUBLISHED">Published (live on app)</SelectItem>
              </SelectContent>
            </Select>
            <Select value={approvedFilter} onValueChange={(v: "all" | "approved" | "not_approved") => setApprovedFilter(v)}>
              <SelectTrigger className="w-full min-w-[120px] max-w-[160px] h-9 sm:w-[160px]">
                <SelectValue placeholder="Approval" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All</SelectItem>
                <SelectItem value="approved">Approved (on app)</SelectItem>
                <SelectItem value="not_approved">Not approved</SelectItem>
              </SelectContent>
            </Select>
            <Button variant="outline" size="default" className="h-9 font-medium shrink-0" onClick={handleRefreshAll} disabled={loading || issuesLoading}>
              <RefreshCw
                className={`h-4 w-4 mr-1 shrink-0 ${loading || issuesLoading ? "animate-spin" : ""}`}
              />
              Refresh
            </Button>
            {canManage && (
              <Button
                variant="outline"
                size="default"
                className="h-9 font-medium text-destructive hover:text-destructive shrink-0"
                onClick={() => setClearAllConfirmOpen(true)}
                disabled={clearAllLoading}
                title="Delete all narration audio, legacy audio, S3 objects, and clear Redis cache. Use when audio is corrupted/stale."
              >
                <Eraser className={`h-4 w-4 mr-1 shrink-0 ${clearAllLoading ? "animate-spin" : ""}`} />
                <span className="hidden sm:inline">Clear audio & cache</span>
                <span className="sm:hidden">Clear</span>
              </Button>
            )}
          </div>
        </CardHeader>
        <CardContent>
          {error && (
            <p className="mb-4 text-sm text-destructive">{error}</p>
          )}
          {loading ? (
            <p className="text-muted-foreground">Loading…</p>
          ) : data ? (
            <>
              {(approvedFilter !== "all" || statusFilter !== "ALL") && (
                <div
                  className="mb-4 rounded-md border border-amber-500/40 bg-amber-500/10 px-3 py-2 text-sm text-amber-950 dark:text-amber-100"
                  role="status"
                >
                  <span className="font-medium">Filters are narrowing the list on the server.</span>{" "}
                  Set status to <span className="font-medium">All status</span> and approval to{" "}
                  <span className="font-medium">All</span> to load every curated story (paginated).{" "}
                  {approvedFilter === "not_approved" && (
                    <span>With &quot;Not approved&quot;, you see stories where narration approval is not set yet (including draft/review/rejected states).</span>
                  )}
                  {approvedFilter === "approved" && (
                    <span>With &quot;Approved&quot;, you only see titles already cleared for the app.</span>
                  )}
                </div>
              )}
              <div className="w-full overflow-x-auto">
                <table className="w-full table-fixed text-sm">
                  <colgroup>
                    <col style={{ width: "40px" }} />
                    <col style={{ width: "48px" }} />
                    <col style={{ width: "14%" }} />
                    <col style={{ width: "64px" }} />
                    <col style={{ width: "12%" }} />
                    <col style={{ width: "64px" }} />
                    <col style={{ width: "16%" }} />
                    <col style={{ width: "48px" }} />
                    <col style={{ width: "52px" }} />
                    <col style={{ width: "14%" }} />
                    <col style={{ width: "90px" }} />
                    <col style={{ width: "90px" }} />
                    <col style={{ width: "140px" }} />
                    <col style={{ width: "88px" }} />
                  </colgroup>
                  <thead>
                    <tr className="border-b border-border bg-[hsl(var(--table-header-bg))] text-[hsl(var(--table-header-foreground))] font-semibold">
                      <th className="w-10 px-4 py-3 text-center">
                        <button
                          type="button"
                          onClick={toggleSelectAll}
                          className="rounded p-1 hover:bg-muted"
                        >
                          {listRows.length > 0 &&
                          listRows.every((r) => selectedIds.has(r.id)) ? (
                            <CheckSquare className="h-4 w-4" />
                          ) : (
                            <Square className="h-4 w-4" />
                          )}
                        </button>
                      </th>
                      <th className="px-4 py-3 text-right">ID</th>
                      <th className="px-4 py-3 text-left">Title</th>
                      <th className="w-12 px-4 py-3 text-center">Approved</th>
                      <th className="px-4 py-3 text-left">Category</th>
                      <th className="px-4 py-3 text-center">Status</th>
                      <th className="px-4 py-3 text-left">
                        <span title="Updates every few seconds when a story is regenerating or running. Shows e.g. 'Generating Tamil audio…' and per-language progress.">
                          Pipeline
                        </span>
                      </th>
                      <th className="px-4 py-3 text-right">Age</th>
                      <th className="px-4 py-3 text-right">Words</th>
                      <th className="px-4 py-3 text-left">Audio</th>
                      <th className="px-4 py-3 text-left">Created</th>
                      <th className="px-4 py-3 text-left">Modified</th>
                      <th className="px-4 py-3 text-left">Owner</th>
                      <th className="w-14 px-4 py-3 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y">
                  {data.content.length === 0 ? (
                    <tr>
                      <td colSpan={14} className="py-12 text-center">
                        <p className="text-muted-foreground mb-2">
                          No stories yet.
                        </p>
                        <Link href="/dashboard/stories/new">
                          <Button size="sm">Create first story</Button>
                        </Link>
                      </td>
                    </tr>
                  ) : (
                    listRows.map((row) => {
                      const pipelineStatus = pipelineStatusMap[row.id];
                      const rowDisabled = isPipelineInProgress(pipelineStatus);
                      return (
                      <tr
                        key={row.id}
                        className="transition-colors hover:bg-muted/20"
                      >
                        <td className="px-4 py-3 text-center align-middle">
                          <button
                            type="button"
                            onClick={() => !rowDisabled && toggleSelect(row.id)}
                            disabled={rowDisabled}
                            className="p-1 rounded hover:bg-muted disabled:pointer-events-none disabled:opacity-70"
                          >
                            {selectedIds.has(row.id) ? (
                              <CheckSquare className="h-4 w-4" />
                            ) : (
                              <Square className="h-4 w-4" />
                            )}
                          </button>
                        </td>
                        <td className="px-4 py-3 font-mono font-medium text-right align-middle tabular-nums">{row.id}</td>
                        <td className="px-4 py-3 text-left align-middle">
                          <span className="truncate block max-w-[200px]" title={row.title ?? undefined}>
                          {row.title || "—"}
                          </span>
                        </td>
                        <td className="px-3 py-3 text-center align-middle overflow-hidden w-[64px] min-w-0">
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <span className="inline-flex cursor-default">
                                {row.narrationApprovedAt ? (
                                  <CheckCircle className="h-5 w-5 text-green-600 shrink-0" />
                                ) : (
                                  <Circle className="h-5 w-5 text-muted-foreground/60 shrink-0" />
                                )}
                              </span>
                            </TooltipTrigger>
                            <TooltipContent side="top">
                              {row.narrationApprovedAt
                                ? "Approved — Live on app"
                                : isReviewQueueStatus(row.status)
                                  ? "Ready for review — approve to allow generate/regenerate audio"
                                  : "Not in review queue yet"}
                            </TooltipContent>
                          </Tooltip>
                        </td>
                        <td className="px-3 py-3 text-left align-middle overflow-hidden min-w-0">
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <span className="block truncate cursor-default max-w-full">{row.theme || "—"}</span>
                            </TooltipTrigger>
                            <TooltipContent side="top">{row.theme || "—"}</TooltipContent>
                          </Tooltip>
                        </td>
                        <td className="px-3 py-3 text-center align-middle overflow-hidden w-[64px] min-w-0">
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <span className="inline-flex cursor-default">
                                {row.status === "PUBLISHED" && row.narrationApprovedAt ? (
                                  <CheckCircle className="h-5 w-5 text-green-600 shrink-0" />
                                ) : row.status === "PUBLISHED" && !row.narrationApprovedAt ? (
                                  <ClipboardCheck className="h-5 w-5 text-amber-600 shrink-0" />
                                ) : row.status === "PROCESSING" ? (
                                  <Clock className="h-5 w-5 text-amber-500 shrink-0 animate-pulse" />
                                ) : row.status === "READY" ? (
                                  <CheckCircle className="h-5 w-5 text-emerald-600 shrink-0" />
                                ) : row.status === "DRAFT" ? (
                                  <FileText className="h-5 w-5 text-muted-foreground shrink-0" />
                                ) : (
                                  <span className="text-muted-foreground text-xs">{row.status}</span>
                                )}
                              </span>
                            </TooltipTrigger>
                            <TooltipContent side="top">
                              {row.status === "PUBLISHED" && row.narrationApprovedAt
                                ? "Live on app — narration approved for delivery"
                                : row.status === "PUBLISHED" && !row.narrationApprovedAt
                                  ? "In review queue — approve to allow Narration / audio"
                                  : row.status === "PROCESSING"
                                    ? "Pipeline running (translate / script / prep)"
                                    : row.status === "READY"
                                      ? "Ready for human review in Story for review"
                                      : row.status === "DRAFT"
                                        ? "Draft — not submitted for review"
                                        : row.status}
                            </TooltipContent>
                          </Tooltip>
                        </td>
                        <td className="min-w-0 px-4 py-3 text-left align-middle">
                          {pipelineStatusMap[row.id] ? (
                            <PipelineStatusBadges
                              status={pipelineStatusMap[row.id]}
                            />
                          ) : (
                            <span className="text-muted-foreground text-xs">Loading…</span>
                          )}
                        </td>
                        <td className="px-4 py-3 text-right align-middle tabular-nums">{row.age}</td>
                        <td className="px-4 py-3 text-right align-middle tabular-nums">{row.wordCount}</td>
                        <td className="min-w-0 px-4 py-3 text-left align-middle">
                          <div className="flex min-w-0 flex-wrap items-center gap-2">
                            {!row.narrationApprovedAt && isReviewQueueStatus(row.status) ? (
                              <Link href={`/dashboard/stories/approve?storyId=${row.id}`}>
                                <span className="text-muted-foreground text-xs hover:underline" title="Approve first to generate or regenerate audio">Ready for review →</span>
                              </Link>
                            ) : (() => {
                              const completedLangs = getCompletedLanguages(pipelineStatusMap[row.id]);
                              const pipelineComplete = isPipelineFullyComplete(pipelineStatusMap[row.id]);
                              const hasAnyAudio = completedLangs.length > 0;
                              if (!hasAnyAudio) {
                                return hasPendingOrFailedPipeline(pipelineStatusMap[row.id]) ? (
                                  <Badge variant="outline">In progress</Badge>
                                ) : (
                                  <Badge variant="secondary">Pending</Badge>
                                );
                              }
                              const totalLangs = getTotalPipelineLanguages(pipelineStatusMap[row.id]);
                              return (
                                <Link href={`/dashboard/stories/to-speech?storyId=${row.id}`} className="inline-flex items-center gap-1.5">
                                  <Badge variant={pipelineComplete ? "default" : "outline"}>
                                    {pipelineComplete ? "Complete" : totalLangs > 0 ? `${completedLangs.length} of ${totalLangs}` : `${completedLangs.length}`}
                                  </Badge>
                                </Link>
                              );
                            })()}
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
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button
                                variant="ghost"
                                size="sm"
                                className="h-8 px-2 text-muted-foreground hover:text-foreground"
                                disabled={rowDisabled}
                                title={rowDisabled ? "Pipeline queued or running" : "Actions"}
                              >
                                <MoreHorizontal className="h-4 w-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end" className="min-w-[160px]">
                              <DropdownMenuItem
                                disabled={rowDisabled}
                                onClick={() => setViewStoryId(row.id)}
                              >
                                <Eye className="h-4 w-4 mr-2" />
                                View story
                              </DropdownMenuItem>
                              {canManage && (
                                <DropdownMenuItem
                                  onClick={() => router.push(`/dashboard/stories/${row.id}/edit`)}
                                >
                                  <Pencil className="h-4 w-4 mr-2" />
                                  Edit story
                                </DropdownMenuItem>
                              )}
                              {canDelete && (
                                <>
                                  <DropdownMenuSeparator />
                                  <DropdownMenuItem
                                    className="text-destructive focus:text-destructive focus:bg-destructive/10"
                                    onClick={() => setDeleteConfirmOpen({ single: row.id, bulk: null })}
                                  >
                                    <Trash2 className="h-4 w-4 mr-2" />
                                    Delete
                                  </DropdownMenuItem>
                                </>
                              )}
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </td>
                      </tr>
                      );
                    })
                  )}
                </tbody>
                </table>
              </div>
              <div className="mt-4 flex items-center justify-between">
                <p className="text-sm text-muted-foreground">
                  {data.totalElements} total · page {data.page + 1} of{" "}
                  {data.totalPages || 1}
                </p>
                <div className="flex gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={data.first}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                  >
                    Previous
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={data.last}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Next
                  </Button>
                </div>
              </div>
            </>
          ) : null}
        </CardContent>
      </Card>

      <Dialog
        open={deleteConfirmOpen.single !== null || (deleteConfirmOpen.bulk?.length ?? 0) > 0}
        onOpenChange={(open) => !open && setDeleteConfirmOpen({ single: null, bulk: null })}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {deleteConfirmOpen.bulk?.length
                ? `Delete ${deleteConfirmOpen.bulk.length} stories?`
                : "Delete story?"}
            </DialogTitle>
            <DialogDescription>
              {deleteConfirmOpen.bulk?.length
                ? "Stories will be moved to trash first (soft delete). You can restore them during retention; permanent purge happens later."
                : "The story will be moved to trash first (soft delete). You can restore it during retention; permanent purge happens later."}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setDeleteConfirmOpen({ single: null, bulk: null })}
            >
              Cancel
            </Button>
            <Button
              variant="destructive"
              disabled={publishing || deletingId !== null}
              onClick={() => {
                if (deleteConfirmOpen.single !== null) {
                  handleDeleteSingle(deleteConfirmOpen.single);
                } else if (deleteConfirmOpen.bulk?.length) {
                  handleBulkDelete();
                }
              }}
            >
              {deletingId !== null
                ? "Deleting…"
                : publishing
                  ? "Deleting…"
                  : "Delete"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog open={clearAllConfirmOpen} onOpenChange={setClearAllConfirmOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Clear all audio & cache?</DialogTitle>
            <DialogDescription>
              This will permanently delete all narration audio, legacy story audio, S3 objects, and clear the Redis TTS cache. 
              Use this when audio is corrupted or stale. Stories will need to be reprocessed for audio. This cannot be undone.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setClearAllConfirmOpen(false)} disabled={clearAllLoading}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              disabled={clearAllLoading}
              onClick={handleClearAllAudioAndCache}
            >
              {clearAllLoading ? "Clearing…" : "Clear all"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* View story: full details, cover, content, TTS preview */}
      <Dialog open={viewStoryId !== null} onOpenChange={(open) => !open && setViewStoryId(null)}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-hidden flex flex-col">
          <DialogHeader>
            <DialogTitle>View story</DialogTitle>
            <DialogDescription>
              Story details, cover image, full content, and narrated audio preview.
            </DialogDescription>
          </DialogHeader>
          {viewStoryLoading ? (
            <p className="py-8 text-center text-muted-foreground">Loading…</p>
          ) : viewStoryData ? (
            <div className="space-y-4 overflow-y-auto pr-2 -mr-2">
              {/* Animated cover (GIF) */}
              {viewStoryData.coverVideoUrl?.trim() && (
                <div>
                  <p className="text-xs font-medium text-muted-foreground mb-1.5">Animated cover (GIF)</p>
                  <div className="relative aspect-video max-w-full rounded-lg border bg-muted/30 overflow-hidden">
                    {viewStoryData.coverVideoUrl.includes(".gif") ? (
                      <Image
                        key={viewCoverRefreshKey}
                        src={`${resolveCoverSrc(viewStoryData.coverVideoUrl) ?? viewStoryData.coverVideoUrl}?t=${viewCoverRefreshKey}`}
                        alt="Animated cover"
                        fill
                        unoptimized
                        className="object-cover"
                      />
                    ) : (
                      <video
                        key={viewCoverRefreshKey}
                        src={`${resolveCoverSrc(viewStoryData.coverVideoUrl) ?? viewStoryData.coverVideoUrl}?t=${viewCoverRefreshKey}`}
                        className="w-full h-full object-cover"
                        controls
                        loop
                        muted
                        playsInline
                      />
                    )}
                  </div>
                </div>
              )}
              {/* Cover image */}
              {viewStoryData.coverImageUrl?.trim() && (
                <div>
                  <p className="text-xs font-medium text-muted-foreground mb-1.5">Cover image</p>
                  <div className="relative aspect-video max-w-full rounded-lg border bg-muted/30 overflow-hidden">
                    <Image
                      key={viewCoverRefreshKey}
                      src={`${resolveCoverSrc(viewStoryData.coverImageUrl) ?? viewStoryData.coverImageUrl}?t=${viewCoverRefreshKey}`}
                      alt="Story cover"
                      fill
                      unoptimized
                      className="object-cover"
                      onError={(e) => {
                        (e.currentTarget as HTMLImageElement).style.display = "none";
                      }}
                    />
                  </div>
                </div>
              )}
              {/* Details */}
              <div className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
                <div><span className="text-muted-foreground">Title</span><br />{viewStoryData.title || "—"}</div>
                <div><span className="text-muted-foreground">Theme</span><br />{viewStoryData.theme}</div>
                <div><span className="text-muted-foreground">Language</span><br />{viewStoryData.language}</div>
                <div><span className="text-muted-foreground">Age</span><br />{viewStoryData.age}</div>
                <div><span className="text-muted-foreground">Child name</span><br />{viewStoryData.childName}</div>
                <div><span className="text-muted-foreground">Word count</span><br />{viewStoryData.wordCount}</div>
                <div><span className="text-muted-foreground">Reading time</span><br />~{Number(viewStoryData.readingTimeMinutes ?? 0).toFixed(1)} min</div>
                <div><span className="text-muted-foreground">Status</span><br /><Badge variant="outline">{viewStoryData.status === "PUBLISHED" && !viewStoryData.narrationApprovedAt ? "In review queue" : viewStoryData.status === "PUBLISHED" && viewStoryData.narrationApprovedAt ? "Live on app" : viewStoryData.status}</Badge></div>
                <div><span className="text-muted-foreground">Owner</span><br />{viewStoryData.storyOwner || "system"}</div>
                <div><span className="text-muted-foreground">Modified</span><br />{viewStoryData.modifiedAt ? new Date(viewStoryData.modifiedAt).toLocaleString() : "—"}</div>
                {viewStoryData.emotionMode && (
                  <div>
                    <span className="text-muted-foreground">Tone</span>
                    <br />
                    {adminStoryEmotionModeLabel(viewStoryData.emotionMode)}
                  </div>
                )}
              </div>
              {/* Story content by language (stored in story_translations, returned by API per language) */}
              <div>
                <p className="text-xs font-medium text-muted-foreground mb-2">Story content by language</p>
                <div className="flex flex-wrap gap-1 mb-3">
                  {VIEW_LANGUAGES.map(({ code, label }) => (
                    <Button
                      key={code}
                      variant={viewStoryTabLang === code ? "default" : "outline"}
                      size="sm"
                      className="h-8"
                      onClick={() => handleViewTabSelect(code)}
                    >
                      {label}
                    </Button>
                  ))}
                </div>
                {viewStoryTabLang === "ta" && viewStoryData?.moral?.trim() && (
                  <div className="mb-2">
                    <p className="text-xs font-medium text-muted-foreground mb-1">Moral</p>
                    <p className="text-sm">{viewStoryData.moral}</p>
                  </div>
                )}
                {viewStoryTabLang !== "ta" && viewStoryContentByLang[viewStoryTabLang]?.moral?.trim() && (
                  <div className="mb-2">
                    <p className="text-xs font-medium text-muted-foreground mb-1">Moral</p>
                    <p className="text-sm">{viewStoryContentByLang[viewStoryTabLang].moral}</p>
                  </div>
                )}
                <div className="rounded-lg border bg-muted/20 p-4 max-h-[220px] overflow-y-auto text-sm whitespace-pre-wrap mb-3">
                  {viewStoryTabLang === "ta"
                    ? (viewStoryData?.content || "—")
                    : (viewStoryContentByLang[viewStoryTabLang]?.content?.trim()?.length
                        ? viewStoryContentByLang[viewStoryTabLang].content
                        : (viewStoryData?.content || "—"))}
                </div>
                {viewStoryTabLang !== "ta" && viewStoryLangLoading && viewStoryContentByLang[viewStoryTabLang] === undefined && (
                  <p className="text-xs text-muted-foreground mb-2">
                    Loading translation… Showing original story above until ready.
                  </p>
                )}
                {viewStoryTabLang !== "ta" && !viewStoryLangLoading && (!viewStoryContentByLang[viewStoryTabLang]?.content?.trim()?.length) && (
                  <p className="text-xs text-muted-foreground mb-2">
                    No translation for this language yet. Run <strong>Generate translations</strong> from <strong>Edit</strong> (open this story from Story library), then submit for review; after approval, use <strong>Narration</strong> → <strong>Generate audio</strong>. Showing original story above.
                  </p>
                )}
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={viewPreviewLoading}
                    onClick={() => handleViewPreview(viewStoryTabLang)}
                  >
                    {viewPreviewLoading ? (
                      <RefreshCw className="h-3.5 w-3.5 mr-1.5 animate-spin" />
                    ) : (
                      <Play className="h-3.5 w-3.5 mr-1.5" />
                    )}
                    Preview ({VIEW_LANGUAGES.find((l) => l.code === viewStoryTabLang)?.label ?? viewStoryTabLang})
                  </Button>
                  <Link href={`/dashboard/stories/to-speech?storyId=${viewStoryData?.id ?? viewStoryId ?? ""}`}>
                    <Button variant="ghost" size="sm" onClick={() => setViewStoryId(null)}>
                      Open Narration
                    </Button>
                  </Link>
                </div>
              </div>
            </div>
          ) : viewStoryId !== null && !viewStoryLoading ? (
            <p className="py-8 text-center text-muted-foreground">Failed to load story.</p>
          ) : null}
          <DialogFooter>
            <Button variant="outline" onClick={() => setViewStoryId(null)}>Close</Button>
            {viewStoryData && (
              <Link href={`/dashboard/stories/${viewStoryData.id}/edit`}>
                <Button onClick={() => setViewStoryId(null)}>Edit story</Button>
              </Link>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>

    </div>
  );
}

function PipelineStatusBadges({
  status,
}: {
  status?: PipelineStatusResponse | Record<string, string | undefined> | null;
}) {
  if (!status || Object.keys(status).length === 0) return <span>—</span>;
  const processingLang = status["processing"];
  const entries = Object.entries(status).filter(
    ([k]) => !isLibraryStoryPipelineMetaKey(k)
  );
  const total = entries.length;
  const completed = entries.filter(([, s]) => s === "COMPLETED").length;
  const failed = entries.filter(([, s]) => s?.includes("FAILED")).length;
  const isInProgressStatus = (s: string) =>
    s === "PENDING" ||
    s === "TRANSLATING" ||
    s === "REWRITING" ||
    s === "TTS_PROCESSING" ||
    s?.startsWith("TRANSLATING") ||
    s?.startsWith("REWRITING") ||
    s?.startsWith("TTS_PROCESSING");
  const inProgress = entries.some(([, s]) => isInProgressStatus(s ?? ""));

  const currentStep = entries.find(([, s]) => isInProgressStatus(s ?? ""));
  const baseStatus = (s: string) => (s?.split(" — ")[0] ?? s ?? "").replace("_", " ");
  const processingStage = processingLang ? status[processingLang]?.split(" — ")[0] : null;
  const progressLabel =
    completed === total
      ? "Complete"
      : failed > 0 && completed === 0
        ? "Failed"
        : inProgress
          ? processingLang
            ? processingStage === "TRANSLATING"
              ? `Translating ${ADMIN_STORY_LANGUAGE_SHORT[processingLang] || processingLang}…`
              : processingStage === "REWRITING"
                ? `Rewriting ${ADMIN_STORY_LANGUAGE_SHORT[processingLang] || processingLang}…`
                : processingStage === "TTS_PROCESSING"
                  ? `Generating ${ADMIN_STORY_LANGUAGE_SHORT[processingLang] || processingLang} audio…`
                  : `${ADMIN_STORY_LANGUAGE_SHORT[processingLang] || processingLang}: ${baseStatus(processingStage ?? "")}…`
            : currentStep
              ? `${ADMIN_STORY_LANGUAGE_SHORT[currentStep[0]] || currentStep[0]}: ${baseStatus(currentStep[1] ?? "")}…`
              : "Starting…"
          : null;

  return (
    <div className="flex min-w-0 max-w-full flex-col gap-1.5">
      {progressLabel && (
        <span className="text-[10px] text-muted-foreground">
          {progressLabel}
        </span>
      )}
      <div className="flex flex-wrap gap-1">
        {entries.map(([lang, stage]) => {
          const isFailed = stage?.includes("FAILED");
          const isDone = stage === "COMPLETED";
          const isPending = stage === "PENDING";
          const isActive =
            stage === "TRANSLATING" ||
            stage === "REWRITING" ||
            stage === "TTS_PROCESSING";
          const isOrange = isPending || isActive;
          const { status, error } = parsePipelineStatus(stage ?? "");
          const label = ADMIN_STORY_LANGUAGE_SHORT[lang] || lang;
          const badgeText = isDone ? label : isFailed && status ? `${label} — ${status}` : `${label} ${stage ?? ""}`.trim();
          return (
            <Badge
              key={lang}
              variant="outline"
              className={cn(
                "text-[10px] px-1.5 py-0 transition-all duration-300",
                isFailed &&
                  "border-red-500/60 bg-red-500/15 text-red-700 dark:text-red-400",
                isOrange &&
                  "border-orange-500/60 bg-orange-500/15 text-orange-700 dark:text-orange-400",
                isActive && "animate-pulse",
                isDone &&
                  "border-emerald-500/60 bg-emerald-500/15 text-emerald-700 dark:text-emerald-400"
              )}
              title={error ? `${status} — ${error}` : isDone ? `${adminStoryLanguageLabel(lang)} completed` : stage ?? undefined}
            >
              {badgeText}
            </Badge>
          );
        })}
      </div>
      {failed > 0 && (() => {
        const failedEntries = entries.filter(([, s]) => s?.includes("FAILED"));
        return (
          <div className="mt-1.5 rounded border border-red-500/30 bg-red-500/5 px-2 py-1.5 space-y-1">
            <span className="text-[10px] font-medium text-red-600 dark:text-red-400">
              Errors:
            </span>
            {failedEntries.map(([lang, stage]) => {
              const { status, error } = parsePipelineStatus(stage ?? "");
              return (
                <div key={lang} className="text-[10px]">
                  <span className="font-medium">{ADMIN_STORY_LANGUAGE_SHORT[lang] || lang}</span>
                  {status && <span className="text-muted-foreground"> — {status}</span>}
                  {error && (
                    <p className="text-destructive pl-1 text-[9px] break-words mt-0.5">
                      {error}
                    </p>
                  )}
                </div>
              );
            })}
          </div>
        );
      })()}
    </div>
  );
}
