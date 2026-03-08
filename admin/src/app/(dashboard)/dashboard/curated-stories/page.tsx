"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { api } from "@/lib/api";
import type { CuratedStorySummary, PagedResponse } from "@/types/api";
import { STORY_CATEGORIES } from "@/types/api";
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
import { RefreshCw, Plus, CheckSquare, Square, Filter, Pencil, Trash2, Play, Volume2, Pause, StopCircle, Repeat, Eye } from "lucide-react";
import { useActionResult } from "@/contexts/action-result-context";
import { cn } from "@/lib/utils";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

const PAGE_SIZE = 20;
const LANG_LABELS: Record<string, string> = {
  ta: "Tamil",
  hi: "Hindi",
  en: "English",
  te: "Telugu",
  kn: "Kannada",
  ml: "Malayalam",
};

/** Resolve cover URL for img src (relative path → full API URL). */
function resolveCoverSrc(coverImageUrl: string | null | undefined): string | null {
  if (!coverImageUrl?.trim()) return null;
  const u = coverImageUrl.trim();
  if (u.startsWith("http://") || u.startsWith("https://")) return u;
  const base =
    (typeof process !== "undefined" && process.env?.NEXT_PUBLIC_API_URL) ||
    (typeof window !== "undefined" ? "http://localhost:8080" : "");
  return u.startsWith("/") ? `${base}${u}` : `${base}/${u}`;
}

/** Parse "STATUS" or "STATUS — error message" from pipeline value */
function parsePipelineStatus(value: string): { status: string; error?: string } {
  if (!value) return { status: value ?? "" };
  const idx = value.indexOf(" — ");
  if (idx === -1) return { status: value };
  return { status: value.slice(0, idx).trim(), error: value.slice(idx + 3).trim() };
}

export default function CuratedStoriesPage() {
  const searchParams = useSearchParams();
  const editedIdFromUrl = searchParams?.get("edited");
  const editedId = editedIdFromUrl ? parseInt(editedIdFromUrl, 10) : null;
  const [data, setData] = useState<PagedResponse<CuratedStorySummary> | null>(
    null
  );
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>("ALL");
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [bulkAction, setBulkAction] = useState<"publish" | "category" | "delete" | null>(
    null
  );
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState<{
    single: number | null;
    bulk: number[] | null;
  }>({ single: null, bulk: null });
  const [republishingId, setRepublishingId] = useState<number | null>(null);
  const [republishDialog, setRepublishDialog] = useState<{
    storyId: number;
    languages: string[];
    pipelineStatus?: Record<string, string>;
  } | null>(null);
  const [republishAll, setRepublishAll] = useState(true);
  const [republishSelectedLangs, setRepublishSelectedLangs] = useState<Set<string>>(new Set());
  const [triggeringId, setTriggeringId] = useState<number | null>(null);
  const { showSuccess, showError } = useActionResult();
  const [bulkCategory, setBulkCategory] = useState("");
  const [publishing, setPublishing] = useState(false);
  const [pipelineStatusMap, setPipelineStatusMap] = useState<
    Record<number, Record<string, string>>
  >({});
  const [previewLoadingId, setPreviewLoadingId] = useState<number | null>(null);
  const [playingAudio, setPlayingAudio] = useState<{
    storyId: number;
    language: string;
  } | null>(null);
  const [isPaused, setIsPaused] = useState(false);
  const [viewStoryId, setViewStoryId] = useState<number | null>(null);
  const [viewStoryData, setViewStoryData] = useState<(CuratedStorySummary & { content: string }) | null>(null);
  const [viewStoryLoading, setViewStoryLoading] = useState(false);
  const [viewCoverRefreshKey, setViewCoverRefreshKey] = useState(0);
  const audioRef = useRef<{
    element: HTMLAudioElement;
    objectUrl: string;
  } | null>(null);

  const load = useCallback((silent = false) => {
    if (!silent) {
      setLoading(true);
      setError(null);
    }
    api.admin
      .getCuratedStories(page, PAGE_SIZE, statusFilter === "ALL" ? undefined : statusFilter)
      .then(setData)
      .catch((e) => {
        if (!silent) {
          setError(e instanceof Error ? e.message : "Failed to load");
          setData(null);
        }
      })
      .finally(() => !silent && setLoading(false));
  }, [page, statusFilter]);

  useEffect(load, [load]);

  const loadPipelineStatuses = useCallback((ids: number[]) => {
    ids.forEach((id) => {
      api.admin
        .getCuratedStoryPipelineStatus(id)
        .then((status) => {
          if (!status) return;
          setPipelineStatusMap((m) => ({ ...m, [id]: status }));
        })
        .catch(() => {});
    });
  }, []);

  // Load full story and pipeline status when View is opened
  useEffect(() => {
    if (viewStoryId == null) {
      setViewStoryData(null);
      return;
    }
    setViewStoryLoading(true);
    loadPipelineStatuses([viewStoryId]);
    api.admin
      .getCuratedStory(viewStoryId)
      .then((story) => {
        setViewStoryData(story as CuratedStorySummary & { content: string });
        setViewCoverRefreshKey(Date.now());
      })
      .catch(() => setViewStoryData(null))
      .finally(() => setViewStoryLoading(false));
  }, [viewStoryId, loadPipelineStatuses]);

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
    const idsToPoll = [...new Set([...fromContent, ...(editedId ? [editedId] : [])])];
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

  const openRepublishDialog = (storyId: number, pipelineStatus?: Record<string, string>) => {
    const langs = pipelineStatus
      ? Object.keys(pipelineStatus).filter((k) => k !== "processing")
      : Object.keys(LANG_LABELS);
    const failedLangs = pipelineStatus
      ? Object.entries(pipelineStatus)
          .filter(([k]) => k !== "processing")
          .filter(([, v]) => v?.includes("FAILED"))
          .map(([k]) => k)
      : [];
    // When there are failed languages, default to selecting only those (retry-style). Otherwise default to all.
    const hasFailures = failedLangs.length > 0;
    setRepublishDialog({ storyId, languages: langs, pipelineStatus });
    setRepublishAll(!hasFailures);
    setRepublishSelectedLangs(new Set(hasFailures ? failedLangs : []));
  };

  const handleRepublish = async () => {
    if (!republishDialog) return;
    const { storyId } = republishDialog;
    setRepublishingId(storyId);
    try {
      const languages = republishAll ? [] : Array.from(republishSelectedLangs);
      await api.admin.republishCuratedStory(storyId, { languages });
      const scope =
        languages.length === 0
          ? "all languages"
          : languages.map((l) => LANG_LABELS[l] ?? l).join(", ");
      setRepublishDialog(null);
      showSuccess("Update & republish triggered", `Audio cleared and reprocessing started for ${scope}. The pipeline will process shortly.`);
      loadPipelineStatuses([storyId]);
      load();
    } catch (e) {
      showError("Update & republish failed", e instanceof Error ? e.message : "Republish failed");
    } finally {
      setRepublishingId(null);
    }
  };

  const handleTriggerPipeline = async (id: number) => {
    setTriggeringId(id);
    try {
      await api.admin.triggerCuratedStoryPipeline(id);
      showSuccess("Pipeline triggered", "Running in background (~5 min). Refresh to see progress.");
      loadPipelineStatuses([id]);
      load();
    } catch (e) {
      showError("Trigger failed", e instanceof Error ? e.message : "Trigger failed");
    } finally {
      setTriggeringId(null);
    }
  };

  const hasPendingOrFailedPipeline = (status?: Record<string, string>) => {
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

  const isPipelineFullyComplete = (status?: Record<string, string>) => {
    if (!status || Object.keys(status).length === 0) return false;
    return Object.values(status).every((s) => s === "COMPLETED");
  };

  const getCompletedLanguages = (status?: Record<string, string>) => {
    if (!status) return [];
    return Object.entries(status)
      .filter(([, s]) => s === "COMPLETED")
      .map(([lang]) => lang);
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
      if (audioRef.current) {
        URL.revokeObjectURL(audioRef.current.objectUrl);
      }
    };
  }, []);

  const handlePlayPreview = async (id: number, language: string) => {
    stopCurrentPlayback();
    setPreviewLoadingId(id);
    try {
      const base = (typeof process !== "undefined" && process.env?.NEXT_PUBLIC_API_URL) || "";
      const url = `${base}/api/v1/admin/curated-stories/${id}/preview-audio?language=${encodeURIComponent(language)}`;
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
    } finally {
      setPreviewLoadingId(null);
    }
  };

  const handlePausePreview = () => {
    const current = audioRef.current;
    if (!current || !playingAudio) return;
    if (isPaused) {
      current.element.play();
      setIsPaused(false);
    } else {
      current.element.pause();
      setIsPaused(true);
    }
  };

  const handleStopPreview = () => {
    stopCurrentPlayback();
  };

  const toggleSelectAll = () => {
    if (!data?.content?.length) return;
    if (selectedIds.size === data.content.length) {
      setSelectedIds(new Set());
    } else {
      setSelectedIds(new Set(data.content.map((r) => r.id)));
    }
  };

  const handleBulkPublish = async () => {
    if (selectedIds.size === 0) {
      showError("Invalid selection", "Please select at least one story.");
      return;
    }
    setPublishing(true);
    try {
      const ids = Array.from(selectedIds).map((id) => Number(id));
      const res = await api.admin.bulkPublish(ids);
      showSuccess("Stories published", `${res.updated} story${res.updated === 1 ? "" : "s"} published. The pipeline will process each one.`);
      setBulkAction(null);
      setSelectedIds(new Set());
      load();
    } catch (e) {
      showError("Bulk publish failed", e instanceof Error ? e.message : "Bulk publish failed");
    } finally {
      setPublishing(false);
    }
  };

  const handleDeleteSingle = async (id: number) => {
    setDeletingId(id);
    try {
      await api.admin.deleteCuratedStory(id);
      showSuccess("Story deleted", "The story and all associated data have been permanently removed.");
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
      const res = await api.admin.bulkDeleteCuratedStories(ids);
      showSuccess("Stories deleted", `${res.deleted} story${res.deleted === 1 ? "" : "s"} permanently removed.`);
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
      <div className="flex items-center justify-between flex-wrap gap-4">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            Curated Tamil stories
          </h1>
          <p className="text-muted-foreground text-sm">
            Content ops · Filter by status · Bulk actions
          </p>
        </div>
        <Link href="/dashboard/curated-stories/new">
          <Button>
            <Plus className="h-4 w-4 mr-2" /> New story
          </Button>
        </Link>
      </div>

      {selectedIds.size > 0 && (
        <Card className="border-primary/30">
          <CardContent className="pt-4">
            <div className="flex items-center gap-4 flex-wrap">
              <span className="text-sm">
                {selectedIds.size} selected
              </span>
              <Button
                size="sm"
                variant="outline"
                onClick={() => setBulkAction("publish")}
              >
                Bulk publish
              </Button>
              <Button
                size="sm"
                variant="outline"
                onClick={() => setBulkAction("category")}
              >
                Bulk category
              </Button>
              <Button
                size="sm"
                variant="outline"
                className="text-destructive hover:text-destructive"
                onClick={() => setDeleteConfirmOpen({ single: null, bulk: Array.from(selectedIds) })}
              >
                Bulk delete
              </Button>
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
              {bulkAction === "publish" && (
                <Button size="sm" onClick={handleBulkPublish} disabled={publishing}>
                  {publishing ? "Publishing…" : "Confirm publish"}
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

      <Card className="border-border/80 shadow-sm">
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle className="text-lg font-semibold tracking-tight">Curated stories · HD</CardTitle>
          <div className="flex items-center gap-2">
            <Select value={statusFilter} onValueChange={setStatusFilter}>
              <SelectTrigger className="w-[140px] h-9">
                <Filter className="h-4 w-4 mr-1" />
                <SelectValue placeholder="All status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All status</SelectItem>
                <SelectItem value="DRAFT">Draft</SelectItem>
                <SelectItem value="PUBLISHED">Published</SelectItem>
              </SelectContent>
            </Select>
            <Button variant="outline" size="default" className="h-9 font-medium" onClick={load} disabled={loading}>
              <RefreshCw
                className={`h-4 w-4 mr-1 ${loading ? "animate-spin" : ""}`}
              />
              Refresh
            </Button>
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
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-10">
                      <button
                        type="button"
                        onClick={toggleSelectAll}
                        className="p-1 rounded hover:bg-muted"
                      >
                        {selectedIds.size === data.content.length &&
                        data.content.length > 0 ? (
                          <CheckSquare className="h-4 w-4" />
                        ) : (
                          <Square className="h-4 w-4" />
                        )}
                      </button>
                    </TableHead>
                    <TableHead>ID</TableHead>
                    <TableHead>Title</TableHead>
                    <TableHead>Category</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>Pipeline</TableHead>
                    <TableHead>Age</TableHead>
                    <TableHead>Words</TableHead>
                    <TableHead>Audio</TableHead>
                    <TableHead>Created</TableHead>
                    <TableHead className="w-[80px]">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.content.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={10} className="py-12 text-center">
                        <p className="text-muted-foreground mb-2">
                          No stories yet.
                        </p>
                        <Link href="/dashboard/curated-stories/new">
                          <Button size="sm">Create first story</Button>
                        </Link>
                      </TableCell>
                    </TableRow>
                  ) : (
                    data.content.map((row) => (
                      <TableRow key={row.id}>
                        <TableCell>
                          <button
                            type="button"
                            onClick={() => toggleSelect(row.id)}
                            className="p-1 rounded hover:bg-muted"
                          >
                            {selectedIds.has(row.id) ? (
                              <CheckSquare className="h-4 w-4" />
                            ) : (
                              <Square className="h-4 w-4" />
                            )}
                          </button>
                        </TableCell>
                        <TableCell>{row.id}</TableCell>
                        <TableCell className="max-w-[120px] truncate">
                          {row.title || "—"}
                        </TableCell>
                        <TableCell>{row.theme}</TableCell>
                        <TableCell>
                          <Badge
                            variant={
                              row.status === "PUBLISHED" || row.status === "READY"
                                ? "default"
                                : row.status === "PROCESSING"
                                  ? "outline"
                                  : "secondary"
                            }
                          >
                            {row.status === "PROCESSING" ? "Processing…" : row.status}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <PipelineStatusBadges
                            status={pipelineStatusMap[row.id]}
                          />
                        </TableCell>
                        <TableCell>{row.age}</TableCell>
                        <TableCell>{row.wordCount}</TableCell>
                        <TableCell>
                          <div className="flex items-center gap-2">
                            {isPipelineFullyComplete(pipelineStatusMap[row.id]) ? (
                              <>
                                <Badge variant="default">Done</Badge>
                                {playingAudio?.storyId === row.id ? (
                                  <div className="flex items-center gap-2" title="Narrated audio controls">
                                    <span className="text-xs text-muted-foreground font-medium">
                                      {LANG_LABELS[playingAudio.language] ?? playingAudio.language}
                                    </span>
                                    <Button
                                      variant="outline"
                                      size="sm"
                                      className="h-8 w-8 p-0 shrink-0"
                                      onClick={handlePausePreview}
                                      title={isPaused ? "Resume" : "Pause"}
                                    >
                                      {isPaused ? (
                                        <Play className="h-4 w-4" />
                                      ) : (
                                        <Pause className="h-4 w-4" />
                                      )}
                                    </Button>
                                    <Button
                                      variant="outline"
                                      size="sm"
                                      className="h-8 w-8 p-0 shrink-0 text-destructive hover:text-destructive"
                                      onClick={handleStopPreview}
                                      title="Stop"
                                    >
                                      <StopCircle className="h-4 w-4" />
                                    </Button>
                                  </div>
                                ) : (() => {
                                  const completedLangs = getCompletedLanguages(pipelineStatusMap[row.id]);
                                  if (completedLangs.length === 0) return null;
                                  return (
                                    <DropdownMenu>
                                      <DropdownMenuTrigger asChild>
                                        <Button
                                          variant="ghost"
                                          size="sm"
                                          className="h-7 px-2"
                                          disabled={previewLoadingId === row.id}
                                          title="Preview narrated audio (conversational)"
                                        >
                                          {previewLoadingId === row.id ? (
                                            <RefreshCw className="h-3.5 w-3.5 animate-spin" />
                                          ) : (
                                            <Volume2 className="h-3.5 w-3.5" />
                                          )}
                                        </Button>
                                      </DropdownMenuTrigger>
                                      <DropdownMenuContent align="start">
                                        {completedLangs.map((lang) => (
                                          <DropdownMenuItem
                                            key={lang}
                                            onClick={() => handlePlayPreview(row.id, lang)}
                                          >
                                            <Play className="h-3.5 w-3.5 mr-2" />
                                            {LANG_LABELS[lang] ?? lang}
                                          </DropdownMenuItem>
                                        ))}
                                      </DropdownMenuContent>
                                    </DropdownMenu>
                                  );
                                })()}
                              </>
                            ) : hasPendingOrFailedPipeline(pipelineStatusMap[row.id]) ? (
                              <Badge variant="outline">In progress</Badge>
                            ) : (
                              <Badge variant="secondary">Pending</Badge>
                            )}
                          </div>
                        </TableCell>
                        <TableCell className="text-muted-foreground text-sm">
                          {new Date(row.createdAt).toLocaleDateString()}
                        </TableCell>
                        <TableCell className="flex gap-1">
                          {(row.status === "PUBLISHED" || row.status === "PROCESSING") &&
                            (() => {
                              const st = pipelineStatusMap[row.id];
                              if (!st) return true;
                              const langEntries = Object.entries(st).filter(([k]) => k !== "processing");
                              return langEntries.length > 0 && langEntries.every(([, v]) => v === "PENDING");
                            })() && (
                              <Button
                                variant="ghost"
                                size="sm"
                                className="h-8 w-8 p-0"
                                disabled={triggeringId === row.id}
                                onClick={() => handleTriggerPipeline(row.id)}
                                title="Trigger pipeline (stuck at PENDING)"
                              >
                                <Play
                                  className={`h-4 w-4 ${triggeringId === row.id ? "animate-pulse" : ""}`}
                                />
                              </Button>
                            )}
                          {(row.status === "PUBLISHED" ||
                            row.status === "READY" ||
                            row.status === "PROCESSING") && (
                              <Button
                                variant="ghost"
                                size="sm"
                                className="h-8 w-8 p-0"
                                disabled={republishingId === row.id}
                                onClick={() =>
                                  openRepublishDialog(row.id, pipelineStatusMap[row.id])
                                }
                                title="Update & republish (clear & reprocess audio)"
                              >
                                <Repeat
                                  className={`h-4 w-4 ${republishingId === row.id ? "animate-spin" : ""}`}
                                />
                              </Button>
                            )}
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 w-8 p-0"
                            onClick={() => setViewStoryId(row.id)}
                            title="View story (details, cover, content, TTS preview)"
                          >
                            <Eye className="h-4 w-4" />
                          </Button>
                          {playingAudio?.storyId === row.id ? (
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-8 w-8 p-0"
                              disabled
                              title="Edit (disabled while playing)"
                            >
                              <Pencil className="h-4 w-4 opacity-50" />
                            </Button>
                          ) : (
                            <Link href={`/dashboard/curated-stories/${row.id}/edit`}>
                              <Button variant="ghost" size="sm" className="h-8 w-8 p-0" title="Edit story">
                                <Pencil className="h-4 w-4" />
                              </Button>
                            </Link>
                          )}
                          <Button
                            variant="ghost"
                            size="sm"
                            className="h-8 w-8 p-0 text-destructive hover:text-destructive"
                            disabled={playingAudio?.storyId === row.id}
                            onClick={() => setDeleteConfirmOpen({ single: row.id, bulk: null })}
                            title="Delete story"
                          >
                            <Trash2 className="h-4 w-4" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
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
                ? "This action cannot be undone. The selected stories and all associated data (favorites, analytics, audio) will be permanently removed."
                : "This action cannot be undone. The story and all associated data (favorites, analytics, audio) will be permanently removed."}
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

      <Dialog
        open={republishDialog !== null}
        onOpenChange={(open) => !open && setRepublishDialog(null)}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Update & republish</DialogTitle>
            <DialogDescription>
              Clear existing audio for the selected languages and reprocess them. Use this when
              a language has failed or you need fresh audio for specific languages.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-2">
            {republishDialog?.pipelineStatus && (() => {
              const failedEntries = Object.entries(republishDialog.pipelineStatus)
                .filter(([k]) => k !== "processing")
                .filter(([, v]) => v?.includes("FAILED"));
              if (failedEntries.length === 0) return null;
              return (
                <div className="rounded-md border border-amber-500/50 bg-amber-500/5 p-3 space-y-2">
                  <p className="text-xs font-medium text-amber-700 dark:text-amber-400">
                    Failed languages (pre-selected for reprocessing):
                  </p>
                  <ul className="text-[11px] space-y-1 text-muted-foreground">
                    {failedEntries.map(([lang, val]) => {
                      const { status, error } = parsePipelineStatus(val ?? "");
                      return (
                        <li key={lang} className="flex flex-col gap-0.5">
                          <span>
                            <strong className="text-foreground">{LANG_LABELS[lang] ?? lang}</strong>:{" "}
                            {status}
                          </span>
                          {error && (
                            <span className="text-destructive pl-2 text-[10px]">{error}</span>
                          )}
                        </li>
                      );
                    })}
                  </ul>
                </div>
              );
            })()}
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={republishAll}
                onChange={(e) => {
                  setRepublishAll(e.target.checked);
                  if (e.target.checked) setRepublishSelectedLangs(new Set());
                }}
                className="rounded"
              />
              <span>All languages</span>
            </label>
            {!republishAll && republishDialog && (
              <div className="flex flex-col gap-2">
                <p className="text-xs text-muted-foreground">
                  Choose which languages to reprocess. Failed languages and their error details
                  are shown above.
                </p>
                <div className="flex flex-wrap gap-2">
                  {republishDialog.languages.map((lang) => {
                    const raw = republishDialog.pipelineStatus?.[lang];
                    const { status, error } = parsePipelineStatus(raw ?? "");
                    const isFailed = status?.includes("FAILED");
                    return (
                      <div
                        key={lang}
                        className={cn(
                          "flex flex-col gap-0.5 rounded-md border px-2 py-1.5",
                          isFailed && "border-red-500/50 bg-red-500/5"
                        )}
                      >
                        <label className="flex items-center gap-1.5 cursor-pointer text-sm">
                          <input
                            type="checkbox"
                            checked={republishSelectedLangs.has(lang)}
                            onChange={(e) => {
                              setRepublishSelectedLangs((prev) => {
                                const next = new Set(prev);
                                if (e.target.checked) next.add(lang);
                                else next.delete(lang);
                                return next;
                              });
                            }}
                            className="rounded"
                          />
                          <span className="font-medium">{LANG_LABELS[lang] ?? lang}</span>
                          {status && (
                            <Badge
                              variant="outline"
                              className={cn(
                                "text-[10px]",
                                isFailed && "border-red-500/60 text-red-600 dark:text-red-400"
                              )}
                            >
                              {status}
                            </Badge>
                          )}
                        </label>
                        {error && (
                          <p className="text-[11px] text-destructive pl-6 pr-1 break-words">
                            {error}
                          </p>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRepublishDialog(null)}>
              Cancel
            </Button>
            <Button
              disabled={
                republishingId !== null ||
                (!republishAll && republishSelectedLangs.size === 0)
              }
              onClick={handleRepublish}
            >
              {republishingId !== null ? "Republishing…" : "Update & republish"}
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
              {/* Animated cover (Sora) */}
              {viewStoryData.coverVideoUrl?.trim() && (
                <div>
                  <p className="text-xs font-medium text-muted-foreground mb-1.5">Animated cover (GIF)</p>
                  <div className="aspect-video max-w-full rounded-lg border bg-muted/30 overflow-hidden">
                    {viewStoryData.coverVideoUrl.includes(".gif") ? (
                      <img
                        key={viewCoverRefreshKey}
                        src={`${resolveCoverSrc(viewStoryData.coverVideoUrl) ?? viewStoryData.coverVideoUrl}?t=${viewCoverRefreshKey}`}
                        alt="Animated cover"
                        className="w-full h-full object-cover"
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
                  <div className="aspect-video max-w-full rounded-lg border bg-muted/30 overflow-hidden">
                    <img
                      key={viewCoverRefreshKey}
                      src={`${resolveCoverSrc(viewStoryData.coverImageUrl) ?? viewStoryData.coverImageUrl}?t=${viewCoverRefreshKey}`}
                      alt="Story cover"
                      className="w-full h-full object-cover"
                      onError={(e) => {
                        (e.target as HTMLImageElement).style.display = "none";
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
                <div><span className="text-muted-foreground">Reading time</span><br />~{viewStoryData.readingTimeMinutes} min</div>
                <div><span className="text-muted-foreground">Status</span><br /><Badge variant="outline">{viewStoryData.status}</Badge></div>
                {viewStoryData.emotionMode && (
                  <div><span className="text-muted-foreground">Tone</span><br />{viewStoryData.emotionMode}</div>
                )}
              </div>
              {viewStoryData.moral?.trim() && (
                <div>
                  <p className="text-xs font-medium text-muted-foreground mb-1">Moral</p>
                  <p className="text-sm">{viewStoryData.moral}</p>
                </div>
              )}
              {/* Full content */}
              <div>
                <p className="text-xs font-medium text-muted-foreground mb-1.5">Story content</p>
                <div className="rounded-lg border bg-muted/20 p-4 max-h-[220px] overflow-y-auto text-sm whitespace-pre-wrap">
                  {viewStoryData.content || "—"}
                </div>
              </div>
              {/* TTS preview */}
              <div>
                <p className="text-xs font-medium text-muted-foreground mb-2">Preview voice (TTS)</p>
                {viewStoryId != null && pipelineStatusMap[viewStoryId] && (() => {
                  const completedLangs = getCompletedLanguages(pipelineStatusMap[viewStoryId]);
                  if (completedLangs.length === 0) {
                    return (
                      <p className="text-sm text-muted-foreground">
                        No narrated audio ready yet. Run the pipeline or wait for processing.
                      </p>
                    );
                  }
                  return (
                    <div className="flex flex-wrap gap-2">
                      {completedLangs.map((lang) => (
                        <Button
                          key={lang}
                          variant={playingAudio?.storyId === viewStoryId && playingAudio?.language === lang ? "default" : "outline"}
                          size="sm"
                          disabled={previewLoadingId === viewStoryId}
                          onClick={() => handlePlayPreview(viewStoryId!, lang)}
                          title={`Play ${LANG_LABELS[lang] ?? lang} narration`}
                        >
                          {previewLoadingId === viewStoryId ? (
                            <RefreshCw className="h-3.5 w-3.5 animate-spin mr-1" />
                          ) : (
                            <Volume2 className="h-3.5 w-3.5 mr-1" />
                          )}
                          {LANG_LABELS[lang] ?? lang}
                        </Button>
                      ))}
                      {playingAudio?.storyId === viewStoryId && (
                        <div className="flex items-center gap-1">
                          <Button variant="outline" size="sm" className="h-8" onClick={handlePausePreview} title={isPaused ? "Resume" : "Pause"}>
                            {isPaused ? <Play className="h-3.5 w-3.5" /> : <Pause className="h-3.5 w-3.5" />}
                          </Button>
                          <Button variant="outline" size="sm" className="h-8 text-destructive hover:text-destructive" onClick={handleStopPreview} title="Stop">
                            <StopCircle className="h-3.5 w-3.5" />
                          </Button>
                        </div>
                      )}
                    </div>
                  );
                })()}
                {viewStoryId != null && !pipelineStatusMap[viewStoryId] && (
                  <p className="text-sm text-muted-foreground">Loading pipeline status…</p>
                )}
              </div>
            </div>
          ) : viewStoryId !== null && !viewStoryLoading ? (
            <p className="py-8 text-center text-muted-foreground">Failed to load story.</p>
          ) : null}
          <DialogFooter>
            <Button variant="outline" onClick={() => setViewStoryId(null)}>Close</Button>
            {viewStoryData && (
              <Link href={`/dashboard/curated-stories/${viewStoryData.id}/edit`}>
                <Button onClick={() => setViewStoryId(null)}>Edit story</Button>
              </Link>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>

    </div>
  );
}

/** ~2 min per language (translate + rewrite + TTS + S3); used for ETA. */
const MINS_PER_LANGUAGE = 2;

function PipelineStatusBadges({
  status,
}: {
  status?: Record<string, string>;
}) {
  if (!status || Object.keys(status).length === 0) return <span>—</span>;
  const processingLang = status["processing"];
  const entries = Object.entries(status).filter(([k]) => k !== "processing");
  const total = entries.length;
  const completed = entries.filter(([, s]) => s === "COMPLETED").length;
  const failed = entries.filter(([, s]) => s?.includes("FAILED")).length;
  const inProgress = entries.some(
    ([, s]) =>
      s === "PENDING" ||
      s === "TRANSLATING" ||
      s === "REWRITING" ||
      s === "TTS_PROCESSING"
  );
  const pendingCount = total - completed - failed;
  const percent = total > 0 ? Math.round((completed / total) * 100) : 0;
  const estMinLeft = inProgress && pendingCount > 0 ? pendingCount * MINS_PER_LANGUAGE : 0;

  const currentStep = entries.find(
    ([, s]) =>
      s === "TRANSLATING" || s === "REWRITING" || s === "TTS_PROCESSING"
  );
  const progressLabel =
    completed === total
      ? "Complete"
      : failed > 0 && completed === 0
        ? "Failed"
        : inProgress
          ? processingLang
            ? `Generating ${LANG_LABELS[processingLang] || processingLang} audio…`
            : currentStep
              ? `${LANG_LABELS[currentStep[0]] || currentStep[0]}: ${currentStep[1].replace("_", " ")}…`
              : "Starting…"
          : null;

  return (
    <div className="flex flex-col gap-1.5 min-w-[180px] max-w-[240px]">
      <div className="flex items-center justify-between gap-2">
        <span className="text-[11px] font-medium text-foreground">
          {completed}/{total} done
          {failed > 0 && ` · ${failed} failed`}
        </span>
        {estMinLeft > 0 && (
          <span className="text-[10px] text-muted-foreground">
            ~{estMinLeft} min left
          </span>
        )}
      </div>
      {(inProgress || completed > 0 || failed > 0) && (
        <div className="h-1.5 w-full overflow-hidden rounded-full bg-orange-200 dark:bg-orange-900/40">
          <div
            className={cn(
              "h-full rounded-full transition-all duration-500 ease-out",
              failed > 0 && completed === 0 ? "bg-red-500" : "bg-emerald-500"
            )}
            style={{
              width:
                failed > 0 && completed === 0 ? "100%" : `${Math.max(percent, 2)}%`,
            }}
          />
        </div>
      )}
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
              title={error ? `${status} — ${error}` : stage ?? undefined}
            >
              {LANG_LABELS[lang] || lang}{" "}
              {isFailed && status ? status : stage}
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
                  <span className="font-medium">{LANG_LABELS[lang] || lang}</span>
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
