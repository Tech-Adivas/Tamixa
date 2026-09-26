/**
 * Shared segment studio types/helpers for admin new + edit library story flows (interactive graphs).
 */

export type SegmentStudioActivity =
  | { kind: "idle" }
  | { kind: "graph" }
  | { kind: "scripts" }
  | { kind: "voices" }
  | { kind: "audio"; scope: "all" | "one" | "some"; segmentIds: string[] };

export function segmentStudioActivityMessage(a: SegmentStudioActivity): string | null {
  switch (a.kind) {
    case "idle":
      return null;
    case "graph":
      return "Generating interactive graph from story…";
    case "scripts":
      return "Generating missing segment scripts (LLM)…";
    case "voices":
      return "Loading voice profiles…";
    case "audio":
      if (a.scope === "all") return "Generating audio for all segments…";
      if (a.scope === "one" && a.segmentIds[0]) return `Generating audio for “${a.segmentIds[0]}”…`;
      return `Generating audio for ${a.segmentIds.length} segment(s)…`;
  }
}

export type InteractiveSegmentDraft = { id: string; text: string; audioUrl: string };

export function parseInteractiveSegments(raw: string | null | undefined): InteractiveSegmentDraft[] {
  const t = raw?.trim();
  if (!t) return [];
  try {
    const parsed = JSON.parse(t) as { segments?: Record<string, { text?: string; audioUrl?: string }> };
    const segs = parsed?.segments ?? {};
    return Object.entries(segs).map(([id, seg]) => ({
      id,
      text: (seg?.text ?? "").trim(),
      audioUrl: (seg?.audioUrl ?? "").trim(),
    }));
  } catch {
    return [];
  }
}
