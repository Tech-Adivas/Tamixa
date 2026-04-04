"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { ArrowLeft, RefreshCw, Sparkles } from "lucide-react";
import { api } from "@/lib/api";
import { BULK_LEARNING_FOCUS_OPTIONS, STORY_CATEGORIES } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useActionResult } from "@/contexts/action-result-context";
import { getApiErrorMessage } from "@/lib/utils";

const NO_LEARNING_FOCUS = "__none__";

export default function BulkGenerateStoriesPage() {
  const { showSuccess, showError } = useActionResult();
  const [selectedCategories, setSelectedCategories] = useState<string[]>(["Friendship"]);
  const [totalStories, setTotalStories] = useState<number>(25);
  const [publish, setPublish] = useState<boolean>(false);
  const [learningFocus, setLearningFocus] = useState<string>(NO_LEARNING_FOCUS);

  const isAllCategories = selectedCategories.length === STORY_CATEGORIES.length && STORY_CATEGORIES.every((c) => selectedCategories.includes(c));
  const [submitting, setSubmitting] = useState(false);
  const [progress, setProgress] = useState<{ current: number; total: number } | null>(null);
  const [result, setResult] = useState<{
    requested: number;
    createdCount: number;
    failedCount: number;
    publish?: boolean;
    created: Array<{ id: number; language: string; category: string; title: string }>;
    failed: Array<{ language: string; category: string; error: string }>;
  } | null>(null);

  const totalRequested = useMemo(
    () => Math.min(25, Math.max(1, Number(totalStories) || 25)),
    [totalStories]
  );

  useEffect(() => {
    if (!submitting) return;
    const onBeforeUnload = (e: BeforeUnloadEvent) => {
      e.preventDefault();
    };
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => window.removeEventListener("beforeunload", onBeforeUnload);
  }, [submitting]);

  const toggleValue = (items: string[], value: string): string[] =>
    items.includes(value) ? items.filter((v) => v !== value) : [...items, value];

  const handleGenerate = async () => {
    if (selectedCategories.length === 0) {
      showError("Validation failed", "Select at least one category or check All.");
      return;
    }
    if (totalRequested > 25) {
      showError("Validation failed", "Maximum 25 stories per bulk run.");
      return;
    }
    setSubmitting(true);
    setProgress(null);
    setResult(null);
    try {
      const { jobId } = await api.admin.bulkGenerateLibraryStoriesAsync({
        languages: ["ta"],
        categories: selectedCategories,
        totalStories: totalRequested,
        publish,
        ...(learningFocus !== NO_LEARNING_FOCUS && learningFocus.trim()
          ? { learningFocus: learningFocus.trim() }
          : {}),
      });
      const pollIntervalMs = 2000;
      const poll = async (): Promise<void> => {
        const status = await api.admin.getBulkGenerateJobStatus(jobId);
        setProgress({ current: status.progress.current, total: status.progress.total });
        if (status.status === "COMPLETED" && status.result) {
          setResult(status.result);
          if (status.result.createdCount > 0) {
            showSuccess(
              "Bulk generation completed",
              `Created ${status.result.createdCount} stories${status.result.publish ? " and submitted for review" : ""}.`
            );
          } else if (status.result.failedCount > 0) {
            showError(
              "No stories created",
              "All generations failed. Check AI_LLM_PROVIDER and the matching API key (OPENAI_API_KEY or GEMINI_API_KEY), then try fewer stories or different categories."
            );
          } else {
            showSuccess("Bulk generation completed", "Request completed (no stories requested).");
          }
          return;
        }
        if (status.status === "FAILED") {
          showError("Bulk generation failed", status.errorMessage ?? "Job failed");
          setResult({
            requested: status.requestedTotal,
            createdCount: status.createdCount,
            failedCount: status.failedCount,
            publish: status.publish,
            created: Array.isArray(status.result?.created) ? status.result.created as Array<{ id: number; language: string; category: string; title: string }> : [],
            failed: Array.isArray(status.result?.failed) ? status.result.failed as Array<{ language: string; category: string; error: string }> : [],
          });
          return;
        }
        await new Promise((r) => setTimeout(r, pollIntervalMs));
        return poll();
      };
      await poll();
    } catch (e) {
      showError("Bulk generation failed", getApiErrorMessage(e, "Failed to generate stories"));
    } finally {
      setSubmitting(false);
      setProgress(null);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/stories">
          <Button variant="outline" size="sm">
            <ArrowLeft className="h-4 w-4 mr-1" />
            Back
          </Button>
        </Link>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Bulk story generator</h1>
          <p className="text-sm text-muted-foreground">
            Generate multiple Tamixa-ready stories using the storyteller prompt template.
          </p>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Generation settings</CardTitle>
        </CardHeader>
        <CardContent className="space-y-5">
          <div className="rounded-md border border-border bg-muted/20 px-3 py-2 text-sm text-muted-foreground">
            <span className="font-medium text-foreground">How languages work:</span> Each story is generated and saved as
            a <strong>Tamil master</strong> (same as manual create and the edit screen). Right after each story is
            created, the server pre-fills text for <strong>English, Hindi, Telugu, Kannada, and Malayalam</strong>{" "}
            (from server pipeline settings) so all languages appear in the editor. Narration/audio still follow your
            usual review and Story-to-Speech flow.
          </div>

          <div>
            <Label>Categories</Label>
            <div className="mt-2 flex flex-wrap gap-2">
              <label className="inline-flex items-center gap-2 rounded-md border border-primary bg-muted/30 px-3 py-2 text-sm cursor-pointer font-medium">
                <input
                  type="checkbox"
                  checked={isAllCategories}
                  onChange={() => {
                    setSelectedCategories(isAllCategories ? [] : [...STORY_CATEGORIES]);
                  }}
                />
                All
              </label>
              {STORY_CATEGORIES.map((c) => (
                <label
                  key={c}
                  className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm cursor-pointer"
                >
                  <input
                    type="checkbox"
                    checked={selectedCategories.includes(c)}
                    onChange={() => setSelectedCategories((prev) => toggleValue(prev, c))}
                  />
                  {c}
                </label>
              ))}
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <Label htmlFor="stories-total">Number of stories to generate (1–25, default 25)</Label>
              <Input
                id="stories-total"
                type="number"
                min={1}
                max={25}
                value={totalStories}
                onChange={(e) => {
                  const v = e.target.value;
                  const n = v === "" ? 25 : Number(v);
                  setTotalStories(n >= 1 && n <= 25 ? n : 25);
                }}
                className="mt-1"
              />
            </div>
            <div className="flex items-end">
              <label className="inline-flex items-center gap-2 rounded-md border px-3 py-2 text-sm cursor-pointer">
                <input
                  type="checkbox"
                  checked={publish}
                  onChange={(e) => setPublish(e.target.checked)}
                />
                Submit for review after generation
              </label>
            </div>
          </div>

          <div>
            <Label htmlFor="bulk-learning-focus">Learning focus (optional)</Label>
            <p className="text-xs text-muted-foreground mt-1 mb-2">
              Same options as parent story generation. Applies to every story in this bulk run; unknown values are ignored by the server.
            </p>
            <Select value={learningFocus} onValueChange={setLearningFocus}>
              <SelectTrigger id="bulk-learning-focus" className="mt-1 w-full sm:max-w-md">
                <SelectValue placeholder="None" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NO_LEARNING_FOCUS}>None</SelectItem>
                {BULK_LEARNING_FOCUS_OPTIONS.map((o) => (
                  <SelectItem key={o.value} value={o.value}>
                    {o.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="rounded-md border bg-muted/20 p-3 text-sm">
            <div>
              <span className="font-medium">Total requested:</span> {totalRequested} stories (max 25 per run)
            </div>
            <div className="text-muted-foreground mt-1">
              Tamil story text is generated per slot (categories rotate). Translated text for other languages is seeded
              automatically; Submit for review still drives your approval and narration pipeline as before.
            </div>
            {totalRequested > 5 && (
              <p className="text-amber-600 dark:text-amber-500 mt-2 text-sm font-medium">
                Bulk generation may take 2–5 minutes. Please do not close or refresh this page until it completes.
              </p>
            )}
          </div>

          <div className="flex items-center gap-2">
            <Button onClick={handleGenerate} disabled={submitting}>
              {submitting ? (
                <>
                  <RefreshCw className="h-4 w-4 mr-2 animate-spin" />
                  {progress ? `Story ${progress.current}/${progress.total}` : "Generating…"}
                </>
              ) : (
                <>
                  <Sparkles className="h-4 w-4 mr-2" />
                  Generate stories
                </>
              )}
            </Button>
          </div>
          <p className="text-xs text-muted-foreground">
            Only one bulk run at a time. Runs in the background with progress; you can keep this page open.
          </p>
        </CardContent>
      </Card>

      {result && (
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Result</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <p className="text-sm">
              Requested: {result.requested} · Created: {result.createdCount} · Failed: {result.failedCount}
            </p>
            {result.publish != null && result.publish && (
              <p className="text-sm text-muted-foreground">Submitted for review; pipeline will add translations and audio.</p>
            )}
            {result.createdCount === 0 && result.failedCount > 0 && (
              <p className="text-sm text-muted-foreground">
                All generations failed. Check AI_LLM_PROVIDER and OPENAI_API_KEY or GEMINI_API_KEY, then try fewer stories or different categories.
              </p>
            )}
            {result.created.length > 0 && (
              <div>
                <p className="text-sm font-medium mb-1">Created stories</p>
                <div className="space-y-1">
                  {result.created.map((s) => (
                    <p key={s.id} className="text-sm text-muted-foreground">
                      #{s.id} · {s.language} · {s.category} · {s.title}
                    </p>
                  ))}
                </div>
                <div className="flex flex-wrap gap-2 mt-3">
                  <Link href="/dashboard/stories">
                    <Button variant="outline" size="sm">View in Stories</Button>
                  </Link>
                  {result.publish && (
                    <Link href="/dashboard/story-for-review">
                      <Button variant="outline" size="sm">Go to Story for review</Button>
                    </Link>
                  )}
                </div>
              </div>
            )}
            {result.failed.length > 0 && (
              <div>
                <p className="text-sm font-medium mb-1 text-destructive">Failures</p>
                <div className="space-y-1">
                  {result.failed.map((f, i) => (
                    <p key={`${f.language}-${f.category}-${i}`} className="text-sm text-destructive/90">
                      {f.language} · {f.category} · {f.error}
                    </p>
                  ))}
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
