"use client";

import { useEffect, useState, useRef } from "react";
import { Play, Pause, StopCircle } from "lucide-react";
import { api } from "@/lib/api";
import type { StorySummary, PagedResponse } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { useActionResult } from "@/contexts/action-result-context";

const PAGE_SIZE = 20;
const STATUS_OPTIONS = ["", "PENDING", "PROCESSING", "READY", "FAILED"];

export default function StoriesPage() {
  const { showError } = useActionResult();
  const [data, setData] = useState<PagedResponse<StorySummary> | null>(null);
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [playingAudio, setPlayingAudio] = useState<{ storyId: number } | null>(null);
  const [isPaused, setIsPaused] = useState(false);
  const [previewLoadingId, setPreviewLoadingId] = useState<number | null>(null);
  const audioRef = useRef<{ element: HTMLAudioElement; objectUrl: string } | null>(null);

  const stopCurrentPlayback = () => {
    const current = audioRef.current;
    if (current) {
      current.element.pause();
      current.element.currentTime = 0;
      URL.revokeObjectURL(current.objectUrl);
      audioRef.current = null;
    }
    setPlayingAudio(null);
    setIsPaused(false);
  };

  useEffect(() => {
    return () => {
      if (audioRef.current) {
        URL.revokeObjectURL(audioRef.current.objectUrl);
      }
    };
  }, []);

  const handlePlayPreview = async (id: number) => {
    stopCurrentPlayback();
    setPreviewLoadingId(id);
    try {
      const base = (typeof process !== "undefined" && process.env?.NEXT_PUBLIC_API_URL) || "";
      const url = `${base}${api.admin.getStoryPreviewAudioUrl(id)}`;
      const token = typeof window !== "undefined" ? localStorage.getItem("admin_access_token") : null;
      const res = await fetch(url, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        const msg = (err as { message?: string }).message;
        throw new Error(msg ?? (res.status === 404 ? "Audio not ready for this story" : "Preview failed"));
      }
      const blob = await res.blob();
      const objectUrl = URL.createObjectURL(blob);
      const audio = new Audio(objectUrl);
      audio.addEventListener("ended", stopCurrentPlayback);
      audioRef.current = { element: audio, objectUrl };
      setPlayingAudio({ storyId: id });
      setIsPaused(false);
      await audio.play();
    } catch (e) {
      showError("TTS preview failed", e instanceof Error ? e.message : "Preview failed");
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

  useEffect(() => {
    setLoading(true);
    setError(null);
    api.admin
      .getStories(page, PAGE_SIZE, status || undefined)
      .then(setData)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [page, status]);

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Story generation logs</h1>
        <p className="text-muted-foreground">
          View story generation history and status.
        </p>
      </div>
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
          <CardTitle>Stories</CardTitle>
          <select
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(0);
            }}
            className="rounded-md border border-input bg-background px-3 py-2 text-sm"
          >
            {STATUS_OPTIONS.map((s) => (
              <option key={s} value={s}>
                {s || "All statuses"}
              </option>
            ))}
          </select>
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
                    <TableHead>ID</TableHead>
                    <TableHead>Theme</TableHead>
                    <TableHead>Child</TableHead>
                    <TableHead>Language</TableHead>
                    <TableHead>Words</TableHead>
                    <TableHead>Status</TableHead>
                    <TableHead>TTS Preview</TableHead>
                    <TableHead>Created</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {data.content.map((row) => (
                    <TableRow key={row.id}>
                      <TableCell>{row.id}</TableCell>
                      <TableCell>{row.theme}</TableCell>
                      <TableCell>{row.childName}</TableCell>
                      <TableCell>{row.language}</TableCell>
                      <TableCell>{row.wordCount}</TableCell>
                      <TableCell>
                        <Badge
                          variant={
                            row.status === "READY"
                              ? "default"
                              : row.status === "FAILED"
                                ? "destructive"
                                : "secondary"
                          }
                        >
                          {row.status}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {row.status === "READY" ? (
                          playingAudio?.storyId === row.id ? (
                            <div className="flex items-center gap-2" title="Narrated audio controls">
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
                          ) : (
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-7 px-2"
                              disabled={previewLoadingId === row.id}
                              onClick={() => handlePlayPreview(row.id)}
                              title="Preview narrated TTS audio"
                            >
                              {previewLoadingId === row.id ? "…" : "Play"}
                            </Button>
                          )
                        ) : (
                          <span className="text-xs text-muted-foreground">—</span>
                        )}
                      </TableCell>
                      <TableCell>
                        {new Date(row.createdAt).toLocaleString()}
                      </TableCell>
                    </TableRow>
                  ))}
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
    </div>
  );
}
