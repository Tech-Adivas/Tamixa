/**
 * Canonical library story lifecycle helpers for admin UI.
 * Aligns with backend StoryStatus.REVIEW_QUEUE (PUBLISHED, PROCESSING, READY).
 */

import type { PipelineStatusResponse } from "@/types/api";

export function normalizeLibraryStoryStatus(status?: string | null): string {
  return (status ?? "DRAFT").trim().toUpperCase() || "DRAFT";
}

/** Story is in the review queue (submitted or pipeline running / ready for review). */
export function isLibraryStoryInReviewQueue(status?: string | null): boolean {
  const s = normalizeLibraryStoryStatus(status);
  return s === "PUBLISHED" || s === "PROCESSING" || s === "READY";
}

/** Submit for review is only for draft-like rows; not when already queued. */
export function canSubmitLibraryStoryForReview(status: string | undefined | null, hasContent: boolean): boolean {
  return !isLibraryStoryInReviewQueue(status) && !!hasContent;
}

const PIPELINE_STATUS_META_KEYS = new Set([
  "processing",
  "progress",
  "overallStatus",
  "reviewedLanguages",
  "reviewStaleLanguages",
  "allLanguagesReviewed",
  "durationSeconds",
  "generatedAtIst",
  "audioCoverageWarnings",
  "failedLanguagesCount",
]);

/**
 * Single source of truth: treat *queued* and *in-progress* pipeline statuses as busy.
 * Used to disable Submit/Edit/Delete while background translation/script/TTS work is active.
 */
export function isLibraryStoryPipelineBusy(status?: PipelineStatusResponse | Record<string, string | undefined> | null): boolean {
  if (!status) return false;

  const overall = (status.overallStatus ?? "").trim();
  if (overall === "COMPLETED" || overall === "READY_FOR_REVIEW") return false;

  // Any queued/active overall status means "busy", even if per-language rows are still all PENDING.
  if (
    overall === "PENDING" ||
    overall === "TRANSLATING_LANGUAGES" ||
    overall === "TTS_PROCESSING" ||
    overall === "FINALIZING_STORY"
  )
    return true;

  // Backend may populate `processing` ("hi", "starting", etc.) before per-language stages flip.
  const processing = (status.processing ?? "").trim();
  if (processing) return true;

  // Fallback: per-language stages.
  for (const [k, v] of Object.entries(status)) {
    if (PIPELINE_STATUS_META_KEYS.has(k)) continue;
    if (typeof v !== "string") continue;
    const s = v.trim();
    if (
      s === "PENDING" ||
      s === "TRANSLATING" ||
      s === "REWRITING" ||
      s === "TTS_PROCESSING" ||
      s.startsWith("TRANSLATING") ||
      s.startsWith("REWRITING") ||
      s.startsWith("TTS_PROCESSING")
    ) {
      return true;
    }
  }

  return false;
}

/**
 * True only when the pipeline is *actively doing work* (translation/rewrite/TTS), not merely idle "PENDING".
 * Use this when you want to keep UI actions available unless the backend is genuinely processing.
 */
export function isLibraryStoryPipelineActivelyRunning(
  status?: PipelineStatusResponse | Record<string, string | undefined> | null
): boolean {
  if (!status) return false;

  const overall = (status.overallStatus ?? "").trim();
  if (overall === "COMPLETED" || overall === "READY_FOR_REVIEW") return false;
  if (overall === "TRANSLATING_LANGUAGES" || overall === "TTS_PROCESSING" || overall === "FINALIZING_STORY") return true;

  const p = Number(status.progress);
  if (!Number.isNaN(p) && p > 0) return true;

  // `processing` can be stale if the in-memory tracker wasn't cleared (e.g. backend restart / interrupted run).
  // Only treat it as active if we also see progress or a non-PENDING overall status.
  const processing = (status.processing ?? "").trim();
  if (processing && overall !== "PENDING") return true;

  for (const [k, v] of Object.entries(status)) {
    if (PIPELINE_STATUS_META_KEYS.has(k)) continue;
    if (typeof v !== "string") continue;
    const s = v.trim();
    if (
      s === "TRANSLATING" ||
      s === "REWRITING" ||
      s === "TTS_PROCESSING" ||
      s.startsWith("TRANSLATING") ||
      s.startsWith("REWRITING") ||
      s.startsWith("TTS_PROCESSING")
    ) {
      return true;
    }
  }

  return false;
}

/**
 * Admin edit: one action runs Tamixa TTS-style conversion, saves draft + translation fields, then the server pipeline for all languages.
 */
export const REGENERATE_THEN_TRANSLATIONS_HELP =
  "On Edit → Cover & languages, use Regenerate & sync all languages: it runs the Tamixa TTS script prompt, saves your draft, then rebuilds every pipeline language on the server (translate → conversational script; no MP3s until Narration after approval). You can still Save draft without LLM when you only fix typos.";

export const REVIEW_QUEUE_EXPECTATION_HELP =
  "Scripts are usually built before submit via Edit → Generate translations. Submit for review does not run the translation pipeline by itself.";

/** After narration approval, text changes require draft → translations → review again before TTS here stays correct. */
export const POST_APPROVAL_CONTENT_CHANGE_HELP =
  "If story or script text changes after approval, use Edit → Move to draft → Generate translations → Submit for review → Approve again, then return here to regenerate MP3s.";
