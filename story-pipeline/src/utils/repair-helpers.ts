import { StoryOutputSchema, type StoryOutput } from "../schemas/story-output.schema.js";
import { reconcileEstimatedDuration } from "./duration-estimate.js";

/**
 * Best-effort coercion before sending to LLM repair (e.g. duration typed as string).
 */
export function coerceRawStoryOutput(raw: Record<string, unknown>): Record<string, unknown> {
  const next = { ...raw };
  let eds = next.estimated_duration_seconds;
  if (typeof eds === "string") {
    const n = parseInt(eds.replace(/[^\d]/g, ""), 10);
    if (Number.isFinite(n)) next.estimated_duration_seconds = n;
  }
  eds = next.estimated_duration_seconds;
  if (typeof eds === "number" && !Number.isInteger(eds)) {
    next.estimated_duration_seconds = Math.round(eds);
  }
  return next;
}

export function tryParseStoryWithLocalFixes(raw: unknown): { ok: true; data: StoryOutput } | { ok: false } {
  if (!raw || typeof raw !== "object") return { ok: false };
  const coerced = coerceRawStoryOutput(raw as Record<string, unknown>);
  const parsed = StoryOutputSchema.safeParse(coerced);
  if (!parsed.success) return { ok: false };
  const data = parsed.data;
  const fixedSeconds = reconcileEstimatedDuration(data.story_text, data.estimated_duration_seconds);
  if (fixedSeconds !== data.estimated_duration_seconds) {
    return { ok: true, data: { ...data, estimated_duration_seconds: fixedSeconds } };
  }
  return { ok: true, data };
}
