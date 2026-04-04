/**
 * Client-side validation for library story interactive_graph JSON (EduStory branching).
 * Mirrors shape in docs/admin/EDU_METADATA_CONVENTIONS.md §7.
 */

export type InteractiveGraphLintResult =
  | { ok: true; errors: [] }
  | { ok: false; errors: string[] };

export function lintInteractiveGraphJson(raw: string): InteractiveGraphLintResult {
  const errors: string[] = [];
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch (e) {
    return {
      ok: false,
      errors: [`Invalid JSON: ${e instanceof Error ? e.message : String(e)}`],
    };
  }
  if (parsed === null || typeof parsed !== "object" || Array.isArray(parsed)) {
    return { ok: false, errors: ["Root must be a JSON object"] };
  }
  const o = parsed as Record<string, unknown>;
  const start = o.startSegmentId;
  if (typeof start !== "string" || !start.trim()) {
    errors.push("startSegmentId is required (non-empty string)");
  }
  const segments = o.segments;
  if (segments === undefined) {
    errors.push("segments object is required");
  } else if (segments === null || typeof segments !== "object" || Array.isArray(segments)) {
    errors.push("segments must be an object mapping segment id → segment");
  } else {
    const segMap = segments as Record<string, unknown>;
    const keys = Object.keys(segMap);
    if (keys.length === 0) errors.push("segments must contain at least one segment");
    for (const [sid, seg] of Object.entries(segMap)) {
      if (!sid.trim()) errors.push("Invalid empty segment key");
      if (seg === null || typeof seg !== "object" || Array.isArray(seg)) {
        errors.push(`Segment "${sid}" must be an object`);
        continue;
      }
      const s = seg as Record<string, unknown>;
      const audioUrl = s.audioUrl;
      if (typeof audioUrl !== "string" || !audioUrl.trim()) {
        errors.push(`Segment "${sid}": audioUrl is required`);
      }
      const choices = s.choices;
      if (choices !== undefined) {
        if (!Array.isArray(choices)) {
          errors.push(`Segment "${sid}": choices must be an array`);
        } else {
          choices.forEach((ch, i) => {
            if (ch === null || typeof ch !== "object" || Array.isArray(ch)) {
              errors.push(`Segment "${sid}" choice[${i}] must be an object`);
              return;
            }
            const c = ch as Record<string, unknown>;
            if (typeof c.id !== "string" || !c.id.trim()) {
              errors.push(`Segment "${sid}" choice[${i}]: id is required`);
            }
            if (typeof c.label !== "string" || !c.label.trim()) {
              errors.push(`Segment "${sid}" choice[${i}]: label is required`);
            }
            if (typeof c.nextSegmentId !== "string" || !c.nextSegmentId.trim()) {
              errors.push(`Segment "${sid}" choice[${i}]: nextSegmentId is required`);
            }
            const deltas = c.skillDeltas;
            if (deltas !== undefined && (deltas === null || typeof deltas !== "object" || Array.isArray(deltas))) {
              errors.push(`Segment "${sid}" choice[${i}]: skillDeltas must be an object of string → number`);
            } else if (deltas && typeof deltas === "object" && !Array.isArray(deltas)) {
              for (const [k, v] of Object.entries(deltas)) {
                if (typeof v !== "number" || !Number.isFinite(v)) {
                  errors.push(`Segment "${sid}" choice[${i}]: skillDeltas.${k} must be a finite number`);
                }
              }
            }
          });
        }
      }
    }
    if (typeof start === "string" && start.trim()) {
      const id = start.trim();
      if (!segMap[id]) {
        errors.push(`startSegmentId "${id}" is not a key in segments`);
      }
      for (const [sid, seg] of Object.entries(segMap)) {
        if (seg === null || typeof seg !== "object" || Array.isArray(seg)) continue;
        const chList = (seg as Record<string, unknown>).choices;
        if (!Array.isArray(chList)) continue;
        for (const ch of chList) {
          if (ch === null || typeof ch !== "object" || Array.isArray(ch)) continue;
          const next = (ch as Record<string, unknown>).nextSegmentId;
          if (typeof next === "string" && next.trim() && !segMap[next.trim()]) {
            errors.push(`Segment "${sid}": choice points to missing segment "${next.trim()}"`);
          }
        }
      }
    }
  }
  const overlay = o.overlayStyle;
  if (overlay !== undefined && overlay !== null && typeof overlay !== "string") {
    errors.push("overlayStyle must be a string when set");
  }
  if (errors.length === 0) return { ok: true, errors: [] };
  return { ok: false, errors };
}
