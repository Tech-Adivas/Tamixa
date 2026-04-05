import { lintInteractiveGraphJson } from "./interactive-graph-lint";

/** Parsed segment object inside `segments` (before normalization). */
type InteractiveGraphSegmentJson = {
  choices?: { id: string; label: string; nextSegmentId: string }[];
};

export type InteractiveGraphOutlineSegment = {
  id: string;
  choiceCount: number;
  isEnd: boolean;
  choices: { id: string; label: string; nextSegmentId: string }[];
};

/**
 * Ordered walk from startSegmentId (BFS), then any disconnected segment ids alphabetically.
 */
export function outlineInteractiveGraphJson(raw: string): InteractiveGraphOutlineSegment[] | null {
  const lint = lintInteractiveGraphJson(raw);
  if (!lint.ok) return null;
  let root: {
    startSegmentId?: string;
    segments?: Record<string, InteractiveGraphSegmentJson>;
  };
  try {
    root = JSON.parse(raw) as {
      startSegmentId?: string;
      segments?: Record<string, InteractiveGraphSegmentJson>;
    };
  } catch {
    return null;
  }
  const start = root.startSegmentId?.trim();
  const segments = root.segments;
  if (!start || !segments || typeof segments !== "object") return null;

  const ordered: InteractiveGraphOutlineSegment[] = [];
  const seen = new Set<string>();
  const queue: string[] = [];

  if (segments[start]) queue.push(start);

  while (queue.length > 0) {
    const id = queue.shift()!;
    if (seen.has(id)) continue;
    seen.add(id);
    const seg: InteractiveGraphSegmentJson | undefined = segments[id];
    if (!seg || typeof seg !== "object") continue;
    const choices: InteractiveGraphOutlineSegment["choices"] = Array.isArray(seg.choices)
      ? seg.choices.map((c) => ({
          id: String(c.id ?? ""),
          label: String(c.label ?? ""),
          nextSegmentId: String(c.nextSegmentId ?? ""),
        }))
      : [];
    ordered.push({
      id,
      choiceCount: choices.length,
      isEnd: choices.length === 0,
      choices,
    });
    for (const c of choices) {
      const next = c.nextSegmentId?.trim();
      if (next && segments[next] && !seen.has(next)) queue.push(next);
    }
  }

  const rest = Object.keys(segments)
    .filter((k) => !seen.has(k))
    .sort();
  for (const id of rest) {
    const seg: InteractiveGraphSegmentJson | undefined = segments[id];
    if (!seg || typeof seg !== "object") continue;
    const choices: InteractiveGraphOutlineSegment["choices"] = Array.isArray(seg.choices)
      ? seg.choices.map((c) => ({
          id: String(c.id ?? ""),
          label: String(c.label ?? ""),
          nextSegmentId: String(c.nextSegmentId ?? ""),
        }))
      : [];
    ordered.push({
      id,
      choiceCount: choices.length,
      isEnd: choices.length === 0,
      choices,
    });
  }

  return ordered;
}
