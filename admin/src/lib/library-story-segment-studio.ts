/**
 * Shared segment studio types/helpers for admin new + edit library story flows (interactive graphs).
 */

export type SegmentStudioActivity =
  | { kind: "idle" }
  | { kind: "graph" }
  | { kind: "scripts" }
  | { kind: "voices" }
  | { kind: "audio"; scope: "all" | "one" | "some"; segmentIds: string[] }
  | { kind: "images"; segmentIds: string[] };

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
    case "images":
      return `Generating question picture(s) for ${a.segmentIds.join(", ")} — about 20 s each…`;
  }
}

export type InteractiveSegmentDraft = {
  id: string;
  text: string;
  audioUrl: string;
  /** Picture shown with this segment's question (`/api/v1/covers/interactive_images/...`); "" when none. */
  imageUrl: string;
  /** True when the segment ends in a question (has choices). */
  hasChoices: boolean;
};

export function parseInteractiveSegments(raw: string | null | undefined): InteractiveSegmentDraft[] {
  const t = raw?.trim();
  if (!t) return [];
  try {
    const parsed = JSON.parse(t) as {
      segments?: Record<string, { text?: string; audioUrl?: string; imageUrl?: string; choices?: unknown[] }>;
    };
    const segs = parsed?.segments ?? {};
    return Object.entries(segs).map(([id, seg]) => ({
      id,
      text: (seg?.text ?? "").trim(),
      audioUrl: (seg?.audioUrl ?? "").trim(),
      imageUrl: (seg?.imageUrl ?? "").trim(),
      hasChoices: Array.isArray(seg?.choices) && seg.choices.length > 0,
    }));
  } catch {
    return [];
  }
}

/**
 * Writes `imageUrl` into the matching segments of [raw] (the graph open in the editor, master or translation).
 * Returns [raw] unchanged when it isn't valid graph JSON.
 */
export function mergeSegmentImageUrls(raw: string | null | undefined, imageUrls: Record<string, string>): string {
  const t = raw?.trim() ?? "";
  if (!t || Object.keys(imageUrls).length === 0) return t;
  try {
    const parsed = JSON.parse(t) as { segments?: Record<string, Record<string, unknown>> };
    const segs = parsed?.segments;
    if (!segs || typeof segs !== "object") return t;
    for (const [segmentId, url] of Object.entries(imageUrls)) {
      if (segs[segmentId] && typeof segs[segmentId] === "object") segs[segmentId].imageUrl = url;
    }
    return JSON.stringify(parsed, null, 2);
  } catch {
    return t;
  }
}
