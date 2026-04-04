import { resolveCoverUrl } from "./api";

export interface InteractiveChoice {
  id: string;
  label: string;
  nextSegmentId: string;
  skillDeltas?: Record<string, number> | null;
}

export interface InteractiveSegment {
  audioUrl: string;
  choices?: InteractiveChoice[];
}

export interface InteractiveStoryGraph {
  startSegmentId: string;
  segments: Record<string, InteractiveSegment>;
  overlayStyle?: string | null;
}

function isRecord(v: unknown): v is Record<string, unknown> {
  return v !== null && typeof v === "object" && !Array.isArray(v);
}

/** Parse admin/CMS interactive_graph JSON (lenient). */
export function parseInteractiveStoryGraph(raw: unknown): InteractiveStoryGraph | null {
  if (!isRecord(raw)) return null;
  const start = raw.startSegmentId;
  if (typeof start !== "string" || !start.trim()) return null;
  const segmentsRaw = raw.segments;
  if (!isRecord(segmentsRaw)) return null;
  const segments: Record<string, InteractiveSegment> = {};
  for (const [sid, segVal] of Object.entries(segmentsRaw)) {
    if (!sid.trim() || !isRecord(segVal)) continue;
    const audioUrl = segVal.audioUrl;
    if (typeof audioUrl !== "string" || !audioUrl.trim()) continue;
    const choicesRaw = segVal.choices;
    const choices: InteractiveChoice[] = [];
    if (Array.isArray(choicesRaw)) {
      for (const ch of choicesRaw) {
        if (!isRecord(ch)) continue;
        const id = ch.id;
        const label = ch.label;
        const nextSegmentId = ch.nextSegmentId;
        if (typeof id !== "string" || !id.trim()) continue;
        if (typeof label !== "string" || !label.trim()) continue;
        if (typeof nextSegmentId !== "string" || !nextSegmentId.trim()) continue;
        let skillDeltas: Record<string, number> | undefined;
        const sd = ch.skillDeltas;
        if (sd !== undefined && sd !== null && isRecord(sd)) {
          skillDeltas = {};
          for (const [k, v] of Object.entries(sd)) {
            if (typeof v === "number" && Number.isFinite(v)) skillDeltas[k] = v;
          }
        }
        choices.push({ id, label, nextSegmentId, skillDeltas });
      }
    }
    segments[sid] = { audioUrl, choices };
  }
  if (!segments[start.trim()]) return null;
  const overlayStyle =
    typeof raw.overlayStyle === "string" && raw.overlayStyle.trim() ? raw.overlayStyle.trim() : null;
  return {
    startSegmentId: start.trim(),
    segments,
    overlayStyle,
  };
}

export function resolveInteractiveSegmentAudioUrl(audioUrl: string): string | null {
  return resolveCoverUrl(audioUrl);
}

/** Warm cache for branch audio while choice overlay is visible (best-effort). */
export async function prefetchInteractiveAudio(urls: string[]): Promise<void> {
  if (typeof window === "undefined") return;
  const token = localStorage.getItem("tamixa_access_token");
  const distinct = [...new Set(urls.map((u) => u.trim()).filter(Boolean))];
  await Promise.all(
    distinct.map(async (url) => {
      try {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return;
        const headers: Record<string, string> = {};
        if (token) headers.Authorization = `Bearer ${token}`;
        const ac = new AbortController();
        const tid = window.setTimeout(() => ac.abort(), 25_000);
        try {
          await fetch(url, { method: "GET", credentials: "omit", headers, signal: ac.signal });
        } finally {
          window.clearTimeout(tid);
        }
      } catch {
        /* ignore */
      }
    })
  );
}

export function libraryRowForInteractivePlayback(
  storyId: number,
  storySourceUi: string,
  library: { id: number }[],
  favorites: { storyId: number; storySource: string }[]
): { id: number } | null {
  if (storySourceUi === "library") {
    return library.find((c) => c.id === storyId) ?? null;
  }
  if (storySourceUi === "favorites") {
    const f = favorites.find((x) => x.storyId === storyId);
    if (f?.storySource === "library") return library.find((c) => c.id === storyId) ?? null;
  }
  return null;
}
