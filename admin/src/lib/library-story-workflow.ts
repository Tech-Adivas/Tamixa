/**
 * Canonical library story lifecycle helpers for admin UI.
 * Aligns with backend StoryStatus.REVIEW_QUEUE (PUBLISHED, PROCESSING, READY).
 */

import type { PipelineStatusResponse } from "@/types/api";
import {
  DEFAULT_LIBRARY_SOURCE_LANGUAGE,
  LIBRARY_TAB_LANGUAGES,
} from "@/lib/library-story-admin-constants";

/** Keys on pipeline-status payloads that are not per-language stage strings (single source for admin UI). */
export const LIBRARY_STORY_PIPELINE_META_KEYS = [
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
] as const;

const PIPELINE_STATUS_META_KEYS = new Set<string>(LIBRARY_STORY_PIPELINE_META_KEYS);

export function isLibraryStoryPipelineMetaKey(key: string): boolean {
  return PIPELINE_STATUS_META_KEYS.has(key);
}

/** Full display names for pipeline language codes (aligned with `LIBRARY_TAB_LANGUAGES`). */
export const ADMIN_STORY_LANGUAGE_LABELS: Record<string, string> = Object.fromEntries(
  LIBRARY_TAB_LANGUAGES.map((l) => [l.code, l.label])
);

export function adminStoryLanguageLabel(code: string): string {
  const c = code.trim().toLowerCase();
  return ADMIN_STORY_LANGUAGE_LABELS[c] ?? code;
}

/** Display labels for library story narration tone (matches EMOTION_MODES). */
export function adminStoryEmotionModeLabel(mode: string): string {
  const m = (mode ?? "").trim().toUpperCase();
  return (
    ({ CALM: "Calm", SOOTHING: "Soothing", ADVENTUROUS: "Adventurous" } as Record<string, string>)[m] ?? mode
  );
}

/** Short labels for dense pipeline badges (two-letter codes → Title case). */
export const ADMIN_STORY_LANGUAGE_SHORT: Record<string, string> = Object.fromEntries(
  LIBRARY_TAB_LANGUAGES.map((l) => {
    const c = l.code;
    const short = c.length >= 2 ? c.charAt(0).toUpperCase() + c.charAt(1) : c.toUpperCase();
    return [c, short];
  })
);

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

/** Shared admin UI: Create + Edit story progress strip (form → server → sync → cover → submit). */
export type LibraryStoryAdminProgressStep = {
  id: string;
  label: string;
  done: boolean;
  busy?: boolean;
  optional?: boolean;
};

export function buildLibraryStoryAdminProgressSteps(input: {
  minWordCount: number;
  wordCount: number;
  titleTrimmed: boolean;
  themeSet: boolean;
  contentTrimmed: boolean;
  simulatorSelected: boolean;
  /** When simulator: graph present, lint OK, segment URL rules OK. Ignored when not simulator. */
  simulatorGraphFieldsOk: boolean;
  onServer: boolean;
  regenerateBusy: boolean;
  hasCover: boolean;
  submitReady: boolean;
}): LibraryStoryAdminProgressStep[] {
  const {
    minWordCount,
    wordCount,
    titleTrimmed,
    themeSet,
    contentTrimmed,
    simulatorSelected,
    simulatorGraphFieldsOk,
    onServer,
    regenerateBusy,
    hasCover,
    submitReady,
  } = input;

  /** Interactive (Learn · Simulator) episodes use segment scripts + graph; master story text has no minimum word count. */
  const wordCountOk = simulatorSelected ? contentTrimmed : wordCount >= minWordCount;
  const formReady =
    titleTrimmed && themeSet && contentTrimmed && wordCountOk && (!simulatorSelected || simulatorGraphFieldsOk);

  return [
    { id: "write", label: "Form ready", done: formReady },
    { id: "row", label: "On server", done: onServer },
    {
      id: "sync",
      label: "Sync idle",
      done: onServer && !regenerateBusy,
      busy: regenerateBusy,
    },
    { id: "cover", label: "Cover", done: hasCover, optional: true },
    { id: "submit", label: "Can submit", done: submitReady },
  ];
}

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
  "On Edit → All languages, use Regenerate & sync all languages: it runs the Tamixa TTS script prompt, saves your draft, then rebuilds every pipeline language on the server (translate → conversational script; no MP3s until Narration after approval). You can still Save draft without LLM when you only fix typos.";

export const REVIEW_QUEUE_EXPECTATION_HELP =
  "Scripts are usually built before submit via Edit → Generate translations. Submit for review does not run the translation pipeline by itself.";

/** After narration approval, text changes require draft → translations → review again before TTS here stays correct. */
export const POST_APPROVAL_CONTENT_CHANGE_HELP =
  "If story or script text changes after approval, use Edit → Move to draft → Generate translations → Submit for review → Approve again, then return here to regenerate MP3s.";

/** sessionStorage: bridges optional cover/regenerate prompts from Create → Edit (shape: { storyId, cover?, regenerate? }). */
export const ADMIN_POST_CREATE_PROMPTS_KEY = "tamixa_admin_post_create_prompts";

/**
 * Whether `text` contains characters in the expected script for the library master language.
 * English skips script checks. Aligns with backend StoryLibraryValidation-style expectations.
 */
export function hasScriptForLibraryStoryLanguage(text: string, lang: string): boolean {
  if (!text?.trim()) return false;
  const normalized = lang.trim().toLowerCase();
  if (normalized === "en") return true;
  if (normalized === "ta") return /[\u0B80-\u0BFF]/.test(text);
  if (normalized === "hi") return /[\u0900-\u097F]/.test(text);
  if (normalized === "te") return /[\u0C00-\u0C7F]/.test(text);
  if (normalized === "kn") return /[\u0C80-\u0CFF]/.test(text);
  if (normalized === "ml") return /[\u0D00-\u0D7F]/.test(text);
  return true;
}

export function libraryStoryMasterScriptLabel(lang: string | undefined | null): string {
  const code = (lang ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE).trim().toLowerCase();
  return ADMIN_STORY_LANGUAGE_LABELS[code] ?? code;
}

/**
 * Content-level script validation for the master row. Returns a user-facing error or null if OK.
 * Call only when content is non-empty and word count is already valid.
 */
export function getLibraryStoryMasterScriptContentError(
  content: string,
  lang: string | undefined | null
): string | null {
  const l = (lang ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE).trim().toLowerCase();
  if (l === "en") return null;
  if (hasScriptForLibraryStoryLanguage(content, l)) return null;
  const label = libraryStoryMasterScriptLabel(l);
  if (l === "ta") {
    return `Story content must contain Tamil script (தமிழ் characters). Use “Regenerate with prompt” on Edit → All languages if you need Tamil output from English prose.`;
  }
  return `Story content must contain ${label} script.`;
}
