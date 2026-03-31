import { findUnknownMarkers } from "../constants/markers.js";

export function validateTtsMarkers(storyText: string): { ok: true } | { ok: false; errors: string[] } {
  const unknown = findUnknownMarkers(storyText);
  if (!unknown.length) return { ok: true };
  return {
    ok: false,
    errors: [
      `tts_markers: Disallowed or unknown bracket markers: ${[...new Set(unknown)].join(", ")}. Only use the exact allowed English marker strings.`,
    ],
  };
}
