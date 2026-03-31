/**
 * Allowed inline TTS markers in story_text. Must appear exactly (English) even inside non-English stories.
 */

export const ALLOWED_TTS_MARKERS = [
  "[Pause 500ms]",
  "[Pause 1s]",
  "[Happy tone]",
  "[Soft voice]",
  "[Warm tone]",
  "[Calm]",
  "[Whisper]",
  "[Excited]",
] as const;

export type AllowedTtsMarker = (typeof ALLOWED_TTS_MARKERS)[number];

/** Regex that matches any allowed marker (for stripping when counting words / script checks) */
export const ALLOWED_MARKERS_REGEX = new RegExp(
  ALLOWED_TTS_MARKERS.map((m) => m.replace(/[[\]]/g, "\\$&")).join("|"),
  "g",
);

/** Find bracket segments that look like markers but are not in the allowlist */
const BRACKET_TOKEN = /\[([^\]]+)\]/g;

export function findUnknownMarkers(storyText: string): string[] {
  const unknown: string[] = [];
  let m: RegExpExecArray | null;
  const re = new RegExp(BRACKET_TOKEN.source, "g");
  while ((m = re.exec(storyText)) !== null) {
    const full = m[0];
    if (!(ALLOWED_TTS_MARKERS as readonly string[]).includes(full)) {
      unknown.push(full);
    }
  }
  return unknown;
}
