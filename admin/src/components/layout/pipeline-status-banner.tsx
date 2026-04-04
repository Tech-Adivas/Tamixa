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
 * Global banner when a story pipeline job is running (translate/rewrite from Generate translations, or TTS from Generate audio).
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
        showSuccess("Pipeline cleared", "Stuck entry cleared. Retry from story Edit → Generate translations, Narration → Generate audio, or Stories with issues → Run pipeline as appropriate.");
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
        <strong>Background pipeline active</strong> — Translate/rewrite and/or TTS is queued or running for {statusLine}. Often about 10–30 minutes for six languages. Typical triggers: <strong>Generate translations</strong> on story edit, or <strong>Generate audio</strong> on Narration after approval.{" "}
        <strong>Submit for review</strong> stays disabled on the story <strong>Save &amp; submit</strong> step while any pipeline is active.{" "}
        <Link
          href="/dashboard/stories/approve"
          className="underline underline-offset-2 hover:no-underline font-medium"
        >
          Open Review
        </Link>
        {" · "}
        <Link
          href="/dashboard/stories"
          className="underline underline-offset-2 hover:no-underline font-medium"
        >
          Story library
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
            then retry from story <strong>Edit</strong> (Generate translations), <strong>Narration</strong> (Generate audio), or Stories with issues.
          </>
        ) : (
          <>
            If stuck, retry from story <strong>Edit</strong> → Generate translations, or <strong>Narration</strong> → Generate audio.
          </>
        )}
      </span>
    </div>
  );
}
