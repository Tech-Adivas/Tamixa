"use client";

import { useEffect, useState, useCallback } from "react";
import { api } from "@/lib/api";
import { mockApi } from "@/lib/mock-api";
import type { StorySummary, PagedResponse } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/empty-state";
import { PageHeader } from "@/components/layout/page-header";
import { BookOpen, CheckCircle, XCircle, Flag, FileText } from "lucide-react";
import { useActionResult } from "@/contexts/action-result-context";

const PAGE_SIZE = 20;
const STATUS_OPTIONS = [
  { value: "ALL", label: "All" },
  { value: "PENDING_REVIEW", label: "Pending review" },
  { value: "READY", label: "Ready" },
  { value: "FLAGGED", label: "Flagged" },
  { value: "FAILED", label: "Failed" },
];

export default function ModerationPage() {
  const { showSuccess, showError } = useActionResult();
  const [data, setData] = useState<PagedResponse<StorySummary> | null>(null);
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [themeFilter, setThemeFilter] = useState("");
  const [themeQuery, setThemeQuery] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [previewStory, setPreviewStory] = useState<StorySummary | null>(null);
  const [previewBody, setPreviewBody] = useState<string | null>(null);
  const [flagReason, setFlagReason] = useState("");
  const [actioningId, setActioningId] = useState<number | null>(null);
  const [rejectConfirmId, setRejectConfirmId] = useState<number | null>(null);
  const [rejectSubmitting, setRejectSubmitting] = useState(false);

  const load = useCallback(() => {
    setLoading(true);
    setError(null);
    if (mockApi.useMock()) {
      setData(
        mockApi.getMockStories(
          page,
          PAGE_SIZE,
          statusFilter !== "ALL" ? statusFilter : undefined,
          themeQuery.trim() || undefined
        )
      );
      setLoading(false);
      return;
    }
    api.admin
      .getStories(page, PAGE_SIZE, statusFilter !== "ALL" ? statusFilter : undefined, themeQuery.trim() || undefined)
      .then(setData)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [page, statusFilter, themeQuery]);

  useEffect(() => {
    load();
  }, [load]);

  const openPreview = (row: StorySummary) => {
    setPreviewStory(row);
    if (mockApi.useMock()) {
      setPreviewBody(mockApi.getMockStoryBody(row.id));
    } else {
      setPreviewBody(null);
      api.admin
        .getStory(row.id)
        .then((story) => setPreviewBody(story.content))
        .catch(() => setPreviewBody("(Failed to load content)"));
    }
  };

  const closePreview = () => {
    setPreviewStory(null);
    setPreviewBody(null);
    setFlagReason("");
  };

  const handleApprove = async (storyId: number, wasPendingReview: boolean) => {
    setActioningId(storyId);
    try {
      await api.admin.approveStory(storyId);
      showSuccess(
        wasPendingReview ? "Review approved" : "Story approved",
        wasPendingReview
          ? "Narration and cover will generate in the background. The action has been logged."
          : "The story has been approved and the action has been logged."
      );
      closePreview();
      load();
    } catch {
      showError("Approval failed", "Unable to approve the story. Please try again.");
    } finally {
      setActioningId(null);
    }
  };

  const handleReject = async (storyId: number) => {
    setActioningId(storyId);
    try {
      await api.admin.rejectStory(storyId);
      showSuccess("Story rejected", "The story has been rejected and the action has been logged.");
      setRejectConfirmId(null);
      closePreview();
      load();
    } catch {
      showError("Rejection failed", "Unable to reject the story. Please try again.");
    } finally {
      setActioningId(null);
    }
  };

  const handleRejectConfirm = async () => {
    if (rejectConfirmId == null) return;
    setRejectSubmitting(true);
    try {
      await handleReject(rejectConfirmId);
    } finally {
      setRejectSubmitting(false);
    }
  };

  const handleFlag = async (storyId: number) => {
    setActioningId(storyId);
    try {
      await api.admin.flagStory(storyId, flagReason || undefined);
      showSuccess("Story flagged", "The story has been flagged and the action has been logged in the audit trail.");
      closePreview();
      load();
    } catch {
      showError("Flag failed", "Unable to flag the story. Please try again.");
    } finally {
      setActioningId(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <PageHeader
          title="Story moderation"
          description="Review generated stories. When human review is enabled (STORY_HUMAN_REVIEW_BEFORE_NARRATION), new stories appear as Pending review until you approve—then narration runs. Approve, reject, or flag. All actions are audited."
          breadcrumbs
        />
        <div className="flex shrink-0 items-center gap-2 rounded-md border border-border bg-muted/30 px-3 py-2 text-sm text-muted-foreground">
          <FileText className="h-4 w-4" aria-hidden />
          <span>Actions are audited</span>
        </div>
      </div>

      <Card>
        <CardHeader className="card-header-responsive pb-2">
          <CardTitle>Stories</CardTitle>
          <div className="filters-row">
            <Input
              placeholder="Filter by theme"
              value={themeFilter}
              onChange={(e) => setThemeFilter(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && (setThemeQuery(themeFilter), setPage(0))}
              className="min-w-0 flex-1 sm:w-[160px]"
            />
            <Button
              variant="outline"
              size="sm"
              onClick={() => (setThemeQuery(themeFilter), setPage(0))}
            >
              Search
            </Button>
            <Select
              value={statusFilter}
              onValueChange={(v) => {
                setStatusFilter(v);
                setPage(0);
              }}
            >
              <SelectTrigger className="w-full min-w-[100px] sm:w-[130px]">
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                {STATUS_OPTIONS.map((opt) => (
                  <SelectItem key={opt.value} value={opt.value}>
                    {opt.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </CardHeader>
        <CardContent>
          {error && (
            <p className="mb-4 text-sm text-destructive">{error}</p>
          )}
          {loading ? (
            <div className="space-y-2">
              <Skeleton className="h-10 w-full" />
              {[1, 2, 3, 4, 5].map((i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : data ? (
            <>
              {data.content.length === 0 ? (
                <EmptyState
                  icon={BookOpen}
                  title="No stories found"
                  description="Try changing the status filter."
                />
              ) : (
                <>
                    <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>ID</TableHead>
                        <TableHead>Theme</TableHead>
                        <TableHead>Child</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead>Safety</TableHead>
                        <TableHead>Created</TableHead>
                        <TableHead className="w-[100px]">Actions</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {data.content.map((row) => (
                        <TableRow key={row.id}>
                          <TableCell>{row.id}</TableCell>
                          <TableCell>{row.theme}</TableCell>
                          <TableCell>{row.childName}</TableCell>
                          <TableCell>
                            <Badge
                              variant={
                                row.status === "READY"
                                  ? "default"
                                  : row.status === "FAILED"
                                    ? "destructive"
                                    : row.status === "PENDING_REVIEW"
                                      ? "outline"
                                      : "secondary"
                              }
                            >
                              {row.status === "PENDING_REVIEW" ? "PENDING REVIEW" : row.status}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            {row.safetyScore != null ? (
                              <span
                                className={
                                  row.safetyScore >= 80
                                    ? "text-green-600 font-medium"
                                    : row.safetyScore >= 60
                                      ? "text-amber-600"
                                      : "text-red-600 font-medium"
                                }
                              >
                                {row.safetyScore}
                              </span>
                            ) : (
                              <span className="text-muted-foreground">—</span>
                            )}
                          </TableCell>
                          <TableCell>
                            {new Date(row.createdAt).toLocaleString()}
                          </TableCell>
                          <TableCell>
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => openPreview(row)}
                            >
                              Preview
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                  <div className="pagination-row mt-4">
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
              )}
            </>
          ) : null}
        </CardContent>
      </Card>

      {/* Story preview modal */}
      <Dialog open={!!previewStory} onOpenChange={(open) => !open && closePreview()}>
        <DialogContent className="max-h-[90vh] max-w-2xl overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              Story #{previewStory?.id} — {previewStory?.theme}
            </DialogTitle>
            <DialogDescription>
              Child: {previewStory?.childName} · {previewStory?.wordCount} words
              {previewStory?.safetyScore != null && (
                <> · Safety: <span className={previewStory.safetyScore >= 80 ? "text-green-600" : previewStory.safetyScore >= 60 ? "text-amber-600" : "text-red-600"}>{previewStory.safetyScore}</span></>
              )}
              {previewStory?.status && (
                <Badge className="ml-2" variant={previewStory.status === "FLAGGED" ? "secondary" : "default"}>
                  {previewStory.status}
                </Badge>
              )}
            </DialogDescription>
          </DialogHeader>
          <div className="rounded-md border border-border bg-muted/30 p-4 text-sm leading-relaxed">
            {previewBody ?? (
              <p className="text-muted-foreground">
                Full story content loads from the API. Theme: {previewStory?.theme}.
              </p>
            )}
          </div>
          <div className="py-2">
            <label className="text-xs text-muted-foreground">Flag reason (optional)</label>
            <Input
              placeholder="e.g. inappropriate content, quality concern"
              value={flagReason}
              onChange={(e) => setFlagReason(e.target.value)}
              className="mt-1"
            />
          </div>
          <DialogFooter className="flex-wrap gap-2">
            <Button
              variant="outline"
              disabled={actioningId !== null}
              onClick={() =>
                previewStory &&
                handleApprove(previewStory.id, previewStory.status === "PENDING_REVIEW")
              }
            >
              <CheckCircle className="mr-2 h-4 w-4" />
              {actioningId === previewStory?.id
                ? "…"
                : previewStory?.status === "PENDING_REVIEW"
                  ? "Approve & start narration"
                  : "Approve"}
            </Button>
            <Button
              variant="outline"
              className="text-destructive hover:bg-destructive/10"
              disabled={actioningId !== null}
              onClick={() => previewStory && setRejectConfirmId(previewStory.id)}
            >
              <XCircle className="mr-2 h-4 w-4" />
              Reject
            </Button>
            <Button
              variant="destructive"
              disabled={actioningId !== null}
              onClick={() => previewStory && handleFlag(previewStory.id)}
            >
              <Flag className="mr-2 h-4 w-4" />
              {actioningId === previewStory?.id ? "Flagging…" : "Flag"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={rejectConfirmId != null}
        onOpenChange={(open) => !open && setRejectConfirmId(null)}
        title="Reject story"
        description="Reject this story? The action will be logged in the audit trail. The story will not be published."
        confirmLabel="Reject"
        variant="destructive"
        loading={rejectSubmitting}
        onConfirm={handleRejectConfirm}
      />
    </div>
  );
}
