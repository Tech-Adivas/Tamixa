import { ALLOWED_MARKERS_REGEX } from "../constants/markers.js";

/**
 * Rough TTS duration from word count. WPM varies by language; use conservative mid estimate.
 */
const DEFAULT_WPM = 145;
const SECONDS_PER_WORD = 60 / DEFAULT_WPM;

export function countWordsForTts(storyText: string): number {
  const withoutMarkers = storyText.replace(ALLOWED_MARKERS_REGEX, " ");
  const normalized = withoutMarkers.replace(/\s+/g, " ").trim();
  if (!normalized) return 0;
  // Split on spaces; CJK/Indic often tokenized by spaces between words in provided text
  return normalized.split(/\s+/).filter(Boolean).length;
}

export function estimateDurationSecondsFromStoryText(storyText: string): number {
  const words = countWordsForTts(storyText);
  const sec = Math.round(words * SECONDS_PER_WORD);
  return Math.max(60, Math.min(1200, sec || 120));
}

/**
 * If model returned unrealistic duration, nudge toward estimate (used in repair).
 */
export function reconcileEstimatedDuration(storyText: string, reported: number): number {
  const est = estimateDurationSecondsFromStoryText(storyText);
  if (!Number.isFinite(reported) || reported <= 0) return est;
  // If off by > 50% from heuristic, prefer blend
  const ratio = reported / est;
  if (ratio < 0.5 || ratio > 2) {
    return Math.round((reported + est) / 2);
  }
  return Math.round(reported);
}
