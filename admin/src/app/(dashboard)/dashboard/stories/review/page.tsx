"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { api, getApiBaseUrl } from "@/lib/api";
import type { LibraryStorySummary, PagedResponse, PipelineStatusResponse } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Textarea } from "@/components/ui/textarea";
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
import {
  RefreshCw,
  CheckCircle,
  XCircle,
  AlertCircle,
  ChevronLeft,
  ChevronRight,
  Eye,
  Filter,
} from "lucide-react";
import { useAuth } from "@/contexts/auth-context";
import { useActionResult } from "@/contexts/action-result-context";
import { canModerateStories } from "@/lib/admin-roles";
import { PageHeader } from "@/components/layout/page-header";
import { StoryStatusBadge } from "@/components/design-system/story-status-badge";
import { PipelineStatusBadges } from "@/components/design-system/pipeline-status-badge";
import {
  isLibraryStoryPipelineActivelyRunning,
  isLibraryStoryPipelineMetaKey,
  adminStoryLanguageLabel,
} from "@/lib/library-story-workflow";
import { cn } from "@/lib/utils";

const PAGE_SIZE = 20;

/** Resolve cover URL for img src (relative path → full API URL). */
function resolveCoverSrc(coverImageUrl: string | null | undefined): string | null {
  if (!coverImageUrl?.trim()) return null;
  const u = coverImageUrl.trim();
  if (u.startsWith("http://") || u.startsWith("https://")) return u;
  const base = getApiBaseUrl();
  if (base) return u.startsWith("/") ? `${base}${u}` : `${base}/${u}`;
  return u.startsWith("/") ? u : null;
}

function isPipelineFullyComplete(
  status?: PipelineStatusResponse | Record<string, string | undefined> | null
): boolean {
  if (!status) return false;
  const entries = Object.entries(status).filter(([k]) => !isLibraryStoryPipelineMetaKey(k));
  return entries.length > 0 && entries.every(([, s]) => s === "COMPLETED");
}

export default function ContentReviewPage() {
  const { user } = useAuth();
  const { showSuccess, showError } = useActionResult();
  const canModerate = canModerateStories(user ?? null);

  const [data, setData] = useState<PagedResponse<LibraryStorySummary> | null>(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [categoryFilter, setCategoryFilter] = useState<string>("ALL");
  const [languageFilter, setLanguageFilter] = useState<string>("ALL");

  const [pipelineStatusMap, setPipelineStatusMap] = useState<
    Record<number, PipelineStatusResponse>
  >({});

  const [viewStoryId, setViewStoryId] = useState<number | null>(null);
  const [viewStoryData, setViewStoryData] = useState<LibraryStorySummary | null>(null);
  const [viewStoryLoading, setViewStoryLoading] = useState(false);

  const [actionStoryId, setActionStoryId] = useState<number | null>(null);
  const [actionType, setActionType] = useState<"approve" | "reject" | "request-changes" | null>(
    null
  );
  const [actionNotes, setActionNotes] = useState("");
  const [actionLoading, setActionLoading] = useState(false);

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

  const load = useCallback(
    (silent = false) => {
      if (!silent) {
        setLoading(true);
        setError(null);
      }

      // Fetch stories with status=READY (ready for review)
      api.admin
        .getLibraryStories(page, PAGE_SIZE, "READY", undefined)
        .then((res) => {
          // Apply client-side filters for category and language
          let filtered = res.content;
          if (categoryFilter !== "ALL") {
            filtered = filtered.filter((s) => s.theme === categoryFilter);
          }
          if (languageFilter !== "ALL") {
            filtered = filtered.filter((s) => s.language === languageFilter);
          }

          setData({
            ...res,
            content: filtered,
            totalElements: filtered.length,
          });
        })
        .catch((e) => {
          if (!silent) {
            setError(e instanceof Error ? e.message : "Failed to load");
            setData(null);
          }
        })
        .finally(() => !silent && setLoading(false));
    },
    [page, categoryFilter, languageFilter]
  );

  useEffect(() => {
    setPage(0);
  }, [categoryFilter, languageFilter]);

  useEffect(load, [load]);

  useEffect(() => {
    if (!data?.content?.length) return;
    loadPipelineStatuses(data.content.map((r) => r.id));
  }, [data?.content, loadPipelineStatuses]);

  // Poll pipeline status for READY stories
  useEffect(() => {
    if (!data?.content?.length) return;
    const ids = data.content.map((r) => r.id);
    const interval = setInterval(() => {
      loadPipelineStatuses(ids);
      load(true); // Silent refresh
    }, 5000);
    return () => clearInterval(interval);
  }, [data?.content, loadPipelineStatuses, load]);

  // Load story details when viewing
  useEffect(() => {
    if (viewStoryId == null) {
      setViewStoryData(null);
      return;
    }
    setViewStoryLoading(true);
    api.admin
      .getLibraryStory(viewStoryId)
      .then((story) => {
        setViewStoryData(story as LibraryStorySummary);
        loadPipelineStatuses([viewStoryId]);
      })
      .catch(() => setViewStoryData(null))
      .finally(() => setViewStoryLoading(false));
  }, [viewStoryId, loadPipelineStatuses]);

  const handleApprove = useCallback(
    async (storyId: number) => {
      setActionLoading(true);
      try {
        await api.admin.approveLibraryStory(storyId);
        showSuccess(
          "Story approved",
          "Story moved to PUBLISHED status. You can now generate audio from the Narration tab."
        );
        setActionStoryId(null);
        setActionType(null);
        setActionNotes("");
        load();
      } catch (e) {
        showError("Approve failed", e instanceof Error ? e.message : "Failed to approve story");
      } finally {
        setActionLoading(false);
      }
    },
    [showSuccess, showError, load]
  );

  const handleReject = useCallback(
    async (storyId: number, notes: string) => {
      setActionLoading(true);
      try {
        await api.admin.rejectLibraryStory(storyId, notes);
        showSuccess("Story rejected", "Story status set to REJECTED.");
        setActionStoryId(null);
        setActionType(null);
        setActionNotes("");
        load();
      } catch (e) {
        showError("Reject failed", e instanceof Error ? e.message : "Failed to reject story");
      } finally {
        setActionLoading(false);
      }
    },
    [showSuccess, showError, load]
  );

  const handleRequestChanges = useCallback(
    async (storyId: number, notes: string) => {
      setActionLoading(true);
      try {
        await api.admin.requestChangesLibraryStory(storyId, notes);
        showSuccess(
          "Changes requested",
          "Story status set to CHANGES_REQUESTED. The content team will revise and resubmit."
        );
        setActionStoryId(null);
        setActionType(null);
        setActionNotes("");
        load();
      } catch (e) {
        showError(
          "Request changes failed",
          e instanceof Error ? e.message : "Failed to request changes"
        );
      } finally {
        setActionLoading(false);
      }
    },
    [showSuccess, showError, load]
  );

  const handleActionConfirm = useCallback(() => {
    if (actionStoryId == null || actionType == null) return;

    if (actionType === "approve") {
      handleApprove(actionStoryId);
    } else if (actionType === "reject") {
      handleReject(actionStoryId, actionNotes);
    } else if (actionType === "request-changes") {
      handleRequestChanges(actionStoryId, actionNotes);
    }
  }, [actionStoryId, actionType, actionNotes, handleApprove, handleReject, handleRequestChanges]);

  const openActionDialog = useCallback(
    (storyId: number, type: "approve" | "reject" | "request-changes") => {
      setActionStoryId(storyId);
      setActionType(type);
      setActionNotes("");
    },
    []
  );

  const closeActionDialog = useCallback(() => {
    setActionStoryId(null);
    setActionType(null);
    setActionNotes("");
  }, []);

  if (!canModerate) {
    return (
      <div className="space-y-6">
        <PageHeader
          title="Content Review"
          description="Review and approve stories ready for publication"
          breadcrumbs
        />
        <Card>
          <CardContent className="pt-6">
            <p className="text-muted-foreground">
              You don&apos;t have permission to review stories. Contact your administrator.
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <PageHeader
          title="Content Review"
          description="Review stories ready for approval"
          breadcrumbs
        />
        <Link href="/dashboard/stories">
          <Button variant="outline">Back to Story Library</Button>
        </Link>
      </div>

      <div className="rounded-lg border border-border/80 bg-muted/20 px-4 py-2.5 text-sm text-muted-foreground">
        <span className="font-medium text-foreground">Review workflow:</span> Stories in{" "}
        <strong>READY</strong> status have completed pipeline processing (translation, rewriting,
        TTS prep). Review content quality, approve for publication, request changes, or reject.
        After approval, generate audio from the <strong>Narration</strong> tab.
      </div>

      <Card className="border-border/80 shadow-sm">
        <CardHeader className="flex flex-col gap-4 pb-4 sm:flex-row sm:items-start sm:justify-between sm:space-y-0">
          <div className="space-y-1.5 min-w-0 pr-2">
            <CardTitle className="text-lg font-semibold tracking-tight">
              Stories Ready for Review
            </CardTitle>
            <p className="text-sm text-muted-foreground leading-snug">
              Review content, check pipeline status, and approve or reject stories.
            </p>
          </div>
          <div className="flex flex-wrap items-center gap-2">
            <Select value={categoryFilter} onValueChange={setCategoryFilter}>
              <SelectTrigger className="w-full min-w-[140px] max-w-[160px] h-9 sm:w-[160px]">
                <Filter className="h-4 w-4 mr-1 shrink-0" />
                <SelectValue placeholder="All categories" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All categories</SelectItem>
                <SelectItem value="Adventure">Adventure</SelectItem>
                <SelectItem value="Animals">Animals</SelectItem>
                <SelectItem value="Friendship">Friendship</SelectItem>
                <SelectItem value="Moral Stories">Moral Stories</SelectItem>
                <SelectItem value="Fun stories">Fun stories</SelectItem>
                <SelectItem value="Learn · Life Skills">Learn · Life Skills</SelectItem>
                <SelectItem value="Learn · Digital Safety">Learn · Digital Safety</SelectItem>
              </SelectContent>
            </Select>
            <Select value={languageFilter} onValueChange={setLanguageFilter}>
              <SelectTrigger className="w-full min-w-[120px] max-w-[140px] h-9 sm:w-[140px]">
                <SelectValue placeholder="All languages" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ALL">All languages</SelectItem>
                <SelectItem value="ta">Tamil</SelectItem>
                <SelectItem value="en">English</SelectItem>
                <SelectItem value="hi">Hindi</SelectItem>
                <SelectItem value="te">Telugu</SelectItem>
                <SelectItem value="kn">Kannada</SelectItem>
                <SelectItem value="ml">Malayalam</SelectItem>
              </SelectContent>
            </Select>
            <Button
              variant="outline"
              size="default"
              className="h-9 font-medium shrink-0"
              onClick={() => load()}
              disabled={loading}
            >
              <RefreshCw className={`h-4 w-4 mr-1 shrink-0 ${loading ? "animate-spin" : ""}`} />
              Refresh
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {error && <p className="mb-4 text-sm text-destructive">{error}</p>}
          {loading ? (
            <p className="text-muted-foreground">Loading…</p>
          ) : data ? (
            <>
              {data.content.length === 0 ? (
                <div className="py-12 text-center">
                  <CheckCircle className="h-12 w-12 mx-auto mb-4 text-muted-foreground/50" />
                  <p className="text-muted-foreground mb-2">No stories ready for review</p>
                  <p className="text-sm text-muted-foreground">
                    Stories will appear here when pipeline processing completes.
                  </p>
                </div>
              ) : (
                <>
                  <div className="space-y-4">
                    {data.content.map((story) => {
                      const pipelineStatus = pipelineStatusMap[story.id];
                      const isPipelineActive = isLibraryStoryPipelineActivelyRunning(
                        pipelineStatus
                      );
                      const isComplete = isPipelineFullyComplete(pipelineStatus);

                      return (
                        <Card
                          key={story.id}
                          className={cn(
                            "border-border/60 transition-all hover:border-border",
                            isPipelineActive && "opacity-70"
                          )}
                        >
                          <CardContent className="pt-6">
                            <div className="flex flex-col gap-4 lg:flex-row lg:gap-6">
                              {/* Cover Image */}
                              {story.coverImageUrl && (
                                <div className="relative w-full lg:w-48 aspect-video rounded-lg border bg-muted/30 overflow-hidden shrink-0">
                                  <Image
                                    src={resolveCoverSrc(story.coverImageUrl) ?? ""}
                                    alt={story.title ?? "Story cover"}
                                    fill
                                    unoptimized
                                    className="object-cover"
                                    onError={(e) => {
                                      (e.currentTarget as HTMLImageElement).style.display = "none";
                                    }}
                                  />
                                </div>
                              )}

                              {/* Story Details */}
                              <div className="flex-1 min-w-0 space-y-3">
                                <div className="flex items-start justify-between gap-4">
                                  <div className="min-w-0 flex-1">
                                    <h3 className="text-lg font-semibold mb-1 truncate">
                                      {story.title || `Story #${story.id}`}
                                    </h3>
                                    <div className="flex flex-wrap items-center gap-2 text-sm text-muted-foreground">
                                      <Badge variant="outline">{story.theme}</Badge>
                                      <span>•</span>
                                      <span>{adminStoryLanguageLabel(story.language)}</span>
                                      <span>•</span>
                                      <span>Age {story.age}</span>
                                      <span>•</span>
                                      <span>{story.wordCount} words</span>
                                    </div>
                                  </div>
                                  <StoryStatusBadge status={story.status as "DRAFT" | "READY" | "PUBLISHED" | "REJECTED" | "CHANGES_REQUESTED"} size="md" />
                                </div>

                                {/* Pipeline Status */}
                                <div>
                                  <p className="text-xs font-medium text-muted-foreground mb-1.5">
                                    Pipeline Status
                                  </p>
                                  <PipelineStatusBadges
                                    status={pipelineStatus}
                                    showProgress={true}
                                  />
                                </div>

                                {/* Content Preview */}
                                {story.content && (
                                  <div>
                                    <p className="text-xs font-medium text-muted-foreground mb-1">
                                      Content Preview
                                    </p>
                                    <p className="text-sm line-clamp-3">{story.content}</p>
                                  </div>
                                )}

                                {/* Moral */}
                                {story.moral && (
                                  <div>
                                    <p className="text-xs font-medium text-muted-foreground mb-1">
                                      Moral
                                    </p>
                                    <p className="text-sm italic">{story.moral}</p>
                                  </div>
                                )}

                                {/* Actions */}
                                <div className="flex flex-wrap items-center gap-2 pt-2">
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={() => setViewStoryId(story.id)}
                                  >
                                    <Eye className="h-4 w-4 mr-1.5" />
                                    View Full Story
                                  </Button>
                                  <Button
                                    variant="default"
                                    size="sm"
                                    onClick={() => openActionDialog(story.id, "approve")}
                                    disabled={isPipelineActive || !isComplete}
                                    title={
                                      isPipelineActive
                                        ? "Pipeline is processing"
                                        : !isComplete
                                          ? "Wait for pipeline to complete"
                                          : "Approve story for publication"
                                    }
                                  >
                                    <CheckCircle className="h-4 w-4 mr-1.5" />
                                    Approve
                                  </Button>
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={() => openActionDialog(story.id, "request-changes")}
                                    disabled={isPipelineActive}
                                  >
                                    <AlertCircle className="h-4 w-4 mr-1.5" />
                                    Request Changes
                                  </Button>
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    className="text-destructive hover:text-destructive"
                                    onClick={() => openActionDialog(story.id, "reject")}
                                    disabled={isPipelineActive}
                                  >
                                    <XCircle className="h-4 w-4 mr-1.5" />
                                    Reject
                                  </Button>
                                  <Link href={`/dashboard/stories/${story.id}/edit`}>
                                    <Button variant="ghost" size="sm">
                                      Edit Story
                                    </Button>
                                  </Link>
                                </div>
                              </div>
                            </div>
                          </CardContent>
                        </Card>
                      );
                    })}
                  </div>

                  {/* Pagination */}
                  <div className="mt-6 flex items-center justify-between">
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
                        <ChevronLeft className="h-4 w-4 mr-1" />
                        Previous
                      </Button>
                      <Button
                        variant="outline"
                        size="sm"
                        disabled={data.last}
                        onClick={() => setPage((p) => p + 1)}
                      >
                        Next
                        <ChevronRight className="h-4 w-4 ml-1" />
                      </Button>
                    </div>
                  </div>
                </>
              )}
            </>
          ) : null}
        </CardContent>
      </Card>

      {/* View Story Dialog */}
      <Dialog open={viewStoryId !== null} onOpenChange={(open) => !open && setViewStoryId(null)}>
        <DialogContent className="max-w-3xl max-h-[90vh] overflow-hidden flex flex-col">
          <DialogHeader>
            <DialogTitle>Story Details</DialogTitle>
            <DialogDescription>
              Review full story content, metadata, and pipeline status.
            </DialogDescription>
          </DialogHeader>
          {viewStoryLoading ? (
            <p className="py-8 text-center text-muted-foreground">Loading…</p>
          ) : viewStoryData ? (
            <div className="space-y-4 overflow-y-auto pr-2 -mr-2">
              {/* Cover Image */}
              {viewStoryData.coverImageUrl && (
                <div className="relative aspect-video max-w-full rounded-lg border bg-muted/30 overflow-hidden">
                  <Image
                    src={resolveCoverSrc(viewStoryData.coverImageUrl) ?? ""}
                    alt="Story cover"
                    fill
                    unoptimized
                    className="object-cover"
                    onError={(e) => {
                      (e.currentTarget as HTMLImageElement).style.display = "none";
                    }}
                  />
                </div>
              )}

              {/* Metadata */}
              <div className="grid grid-cols-2 gap-x-4 gap-y-2 text-sm">
                <div>
                  <span className="text-muted-foreground">Title</span>
                  <br />
                  {viewStoryData.title || "—"}
                </div>
                <div>
                  <span className="text-muted-foreground">Category</span>
                  <br />
                  {viewStoryData.theme}
                </div>
                <div>
                  <span className="text-muted-foreground">Language</span>
                  <br />
                  {adminStoryLanguageLabel(viewStoryData.language)}
                </div>
                <div>
                  <span className="text-muted-foreground">Age</span>
                  <br />
                  {viewStoryData.age}
                </div>
                <div>
                  <span className="text-muted-foreground">Word Count</span>
                  <br />
                  {viewStoryData.wordCount}
                </div>
                <div>
                  <span className="text-muted-foreground">Status</span>
                  <br />
                  <StoryStatusBadge status={viewStoryData.status as "DRAFT" | "READY" | "PUBLISHED" | "REJECTED" | "CHANGES_REQUESTED"} />
                </div>
              </div>

              {/* Pipeline Status */}
              <div>
                <p className="text-xs font-medium text-muted-foreground mb-2">Pipeline Status</p>
                <PipelineStatusBadges
                  status={pipelineStatusMap[viewStoryData.id]}
                  showProgress={true}
                />
              </div>

              {/* Moral */}
              {viewStoryData.moral && (
                <div>
                  <p className="text-xs font-medium text-muted-foreground mb-1">Moral</p>
                  <p className="text-sm italic">{viewStoryData.moral}</p>
                </div>
              )}

              {/* Full Content */}
              <div>
                <p className="text-xs font-medium text-muted-foreground mb-2">Full Content</p>
                <div className="rounded-lg border bg-muted/20 p-4 max-h-[300px] overflow-y-auto text-sm whitespace-pre-wrap">
                  {viewStoryData.content || "—"}
                </div>
              </div>
            </div>
          ) : (
            <p className="py-8 text-center text-muted-foreground">Failed to load story.</p>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setViewStoryId(null)}>
              Close
            </Button>
            {viewStoryData && (
              <>
                <Button
                  variant="default"
                  onClick={() => {
                    if (viewStoryData) {
                      openActionDialog(viewStoryData.id, "approve");
                      setViewStoryId(null);
                    }
                  }}
                  disabled={
                    isLibraryStoryPipelineActivelyRunning(pipelineStatusMap[viewStoryData.id]) ||
                    !isPipelineFullyComplete(pipelineStatusMap[viewStoryData.id])
                  }
                >
                  <CheckCircle className="h-4 w-4 mr-1.5" />
                  Approve
                </Button>
              </>
            )}
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Action Confirmation Dialog */}
      <Dialog open={actionStoryId !== null} onOpenChange={(open) => !open && closeActionDialog()}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {actionType === "approve"
                ? "Approve Story"
                : actionType === "reject"
                  ? "Reject Story"
                  : "Request Changes"}
            </DialogTitle>
            <DialogDescription>
              {actionType === "approve"
                ? "Approve this story for publication. It will move to PUBLISHED status and be ready for audio generation."
                : actionType === "reject"
                  ? "Reject this story. It will be marked as REJECTED and removed from the review queue."
                  : "Request changes to this story. It will move to CHANGES_REQUESTED status and the content team will revise it."}
            </DialogDescription>
          </DialogHeader>
          {(actionType === "reject" || actionType === "request-changes") && (
            <div className="space-y-2">
              <Label htmlFor="action-notes">
                {actionType === "reject" ? "Rejection Reason" : "Change Requests"}
                {actionType === "reject" && " (optional)"}
              </Label>
              <Textarea
                id="action-notes"
                placeholder={
                  actionType === "reject"
                    ? "Explain why this story is being rejected..."
                    : "Describe what changes are needed..."
                }
                value={actionNotes}
                onChange={(e) => setActionNotes(e.target.value)}
                rows={4}
              />
            </div>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={closeActionDialog} disabled={actionLoading}>
              Cancel
            </Button>
            <Button
              variant={actionType === "approve" ? "default" : "destructive"}
              onClick={handleActionConfirm}
              disabled={actionLoading}
            >
              {actionLoading
                ? "Processing…"
                : actionType === "approve"
                  ? "Approve"
                  : actionType === "reject"
                    ? "Reject"
                    : "Request Changes"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
