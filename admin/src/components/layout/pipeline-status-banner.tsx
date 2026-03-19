"use client";

import Link from "next/link";
import { Loader2 } from "lucide-react";
import { usePipelineActive, getLanguageLabel } from "@/contexts/pipeline-active-context";
import { useActionResult } from "@/contexts/action-result-context";
import { api } from "@/lib/api";
import { useState } from "react";

/** Minutes on one language = likely stuck. Must match backend claim-max-age-minutes. */
const STUCK_THRESHOLD_MIN = 5;

/**
 * Global banner shown when the story pipeline (translate + TTS) is running in the background.
 * Visible across all dashboard pages so users know to wait before Approve / resubmit.
 */
export function PipelineStatusBanner() {
  const { isPipelineActive, activeStories, refresh } = usePipelineActive();
  const { showSuccess, showError } = useActionResult();
  const [clearing, setClearing] = useState<number | null>(null);

  if (!isPipelineActive) return null;

  const stuckStories = activeStories.filter((s) => (s.ageMinutes ?? 0) >= STUCK_THRESHOLD_MIN);
  const statusLine =
    activeStories.length > 0
      ? activeStories
          .map((s) =>
            s.language ? `story #${s.storyId} (${getLanguageLabel(s.language)})` : `story #${s.storyId}`
          )
          .join(", ")
      : "stories";

  const handleClearStuck = async (storyId: number) => {
    setClearing(storyId);
    try {
      const res = await api.admin.clearStuckPipeline(storyId);
      if (res?.cleared) {
        await refresh();
        showSuccess("Pipeline cleared", "Stuck pipeline cleared. You can run the pipeline again from Story library.");
      } else {
        showError("Clear failed", res?.message ?? "Could not clear. Pipeline may still be running.");
      }
    } catch (e) {
      showError("Clear failed", (e as Error)?.message ?? "Clear failed");
    } finally {
      setClearing(null);
    }
  };

  return (
    <div
      className="flex flex-wrap items-center gap-2 border-b border-amber-500/30 bg-amber-500/10 px-4 py-2.5 text-sm text-amber-800 dark:text-amber-200 sm:gap-3 sm:px-6"
      role="status"
      aria-live="polite"
    >
      <Loader2
        className="h-4 w-4 shrink-0 animate-spin text-amber-600 dark:text-amber-400"
        aria-hidden
      />
      <span className="flex-1">
        <strong>Background pipeline running</strong> — Generating translations and audio for {statusLine}. Typically 10–30 min for 6 languages. Approve and Submit for review are disabled until complete.{" "}
        <Link
          href="/dashboard/stories/approve"
          className="underline underline-offset-2 hover:no-underline font-medium"
        >
          View Story for review
        </Link>
        {" "}
        {stuckStories.length > 0 ? (
          <>
            No progress?{" "}
            {stuckStories.map((s, i) => (
              <span key={s.storyId}>
                {i > 0 && ", "}
                <button
                  type="button"
                  onClick={() => handleClearStuck(s.storyId)}
                  disabled={clearing === s.storyId}
                  className="underline underline-offset-2 hover:no-underline font-medium"
                >
                  {clearing === s.storyId ? "Clearing…" : `Clear stuck #${s.storyId}`}
                </button>
              </span>
            ))}
            {" "}
            then Run pipeline in Story library.
          </>
        ) : (
          <>
            If stuck, use Run pipeline in Story library.
          </>
        )}
      </span>
    </div>
  );
}
