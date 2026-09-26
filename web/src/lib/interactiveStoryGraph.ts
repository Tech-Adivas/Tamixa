import { resolveLibraryAudioUrl } from "./api";
import type { ImpactStats } from "./branchingEduStory";
import { LIFE_STAT_KEYS } from "./branchingEduStory";
import type { FinancialChoiceImpact } from "./financialRealityEngine";

/** Public speaking / clarity axis for confidence meter pacing. */
export type DialogueStyle = "simple_clear" | "complex_or_performative";

export interface InteractiveChoice {
  id: string;
  label: string;
  nextSegmentId: string;
  skillDeltas?: Record<string, number> | null;
  /** Optional Life Bar deltas (see branchingEduStory); merged with life-relevant keys from skillDeltas. */
  impactStats?: ImpactStats | null;
  /** Digital safety: explain risk on hover/focus (Scam-Theater). */
  riskWhy?: string;
  /** Business mini-game: projected capital after this choice. */
  capitalRemaining?: number;
  estimatedProfit?: number;
  /** Leadership: Relationship meter deltas (Authority vs Harmony). */
  authorityDelta?: number;
  harmonyDelta?: number;
  /** Communication: simple/clear options raise confidence faster than performative wording. */
  dialogueStyle?: DialogueStyle;
  /** Ethics: shortcut / cheat path — flags integrity + optional consequence routing (see segment + choice fields). */
  isShortcut?: boolean;
  /** If set with isShortcut, navigation jumps here instead of nextSegmentId (immediate consequence chapter). */
  consequenceSegmentId?: string;
  /** Tamixa Financial Reality Engine — loans, EMIs, status vs savings. */
  financialImpact?: FinancialChoiceImpact;
}

/** Digital Survival live red-flag hints (shown on segment). */
export type ScamSignal = "urgency" | "hidden_fees" | "permissions";

export interface InteractiveSegment {
  audioUrl: string;
  /** Optional narrative shown in the player (co-listening); see contentBodyParent / contentBodyChild. */
  contentBody?: string;
  contentBodyParent?: string;
  contentBodyChild?: string;
  /** When true, simulator shows a family discussion countdown before choice bubbles. */
  reflectionPoint?: boolean;
  /**
   * When the listener previously took an ethics shortcut (`isShortcut` choice), entering this segment
   * jumps to the given segment id (consequence chapter). Clears the pending shortcut flag for the story.
   */
  ethicsConsequenceRedirectIfShortcut?: string;
  /** Scam-detection HUD for Digital Survival (real-time red flags). */
  scamSignals?: ScamSignal[];
  choices?: InteractiveChoice[];
}

export interface InteractiveStoryGraph {
  startSegmentId: string;
  segments: Record<string, InteractiveSegment>;
  overlayStyle?: string | null;
  /** Optional: Tech | Business | Leadership | Communication | Ethics — drives CategoryLogicFactory UI. */
  eduCategory?: string | null;
  /** Turn on FamilyEconomy HUD / EMI simulation for this episode. */
  financialRealityEnabled?: boolean;
  /** Fallback crisis chapter when liquid cash goes negative. */
  defaultCrisisSegmentId?: string | null;
}

function isRecord(v: unknown): v is Record<string, unknown> {
  return v !== null && typeof v === "object" && !Array.isArray(v);
}

function parseFinancialImpact(raw: unknown): FinancialChoiceImpact | undefined {
  if (!isRecord(raw)) return undefined;
  const out: FinancialChoiceImpact = {};
  const al = raw.addLoan;
  if (isRecord(al)) {
    const principal = al.principal;
    const aprAnnual = al.aprAnnual;
    const tenureMonths = al.tenureMonths;
    if (
      typeof principal === "number" &&
      Number.isFinite(principal) &&
      principal > 0 &&
      typeof aprAnnual === "number" &&
      Number.isFinite(aprAnnual) &&
      typeof tenureMonths === "number" &&
      Number.isFinite(tenureMonths) &&
      tenureMonths > 0
    ) {
      out.addLoan = { principal, aprAnnual, tenureMonths };
      const lab = al.label;
      if (typeof lab === "string" && lab.trim()) out.addLoan.label = lab.trim();
    }
  }
  const ame = raw.addMonthlyEmi;
  if (typeof ame === "number" && Number.isFinite(ame) && ame > 0) out.addMonthlyEmi = ame;
  const sv = raw.socialValidationDelta;
  if (typeof sv === "number" && Number.isFinite(sv)) out.socialValidationDelta = sv;
  const sd = raw.savingsDelta;
  if (typeof sd === "number" && Number.isFinite(sd)) out.savingsDelta = sd;
  const ots = raw.oneTimeSpend;
  if (typeof ots === "number" && Number.isFinite(ots)) out.oneTimeSpend = ots;
  const ccs = raw.crisisChapterSegmentId;
  if (typeof ccs === "string" && ccs.trim()) out.crisisChapterSegmentId = ccs.trim();
  if (Object.keys(out).length === 0) return undefined;
  return out;
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
        let impactStats: ImpactStats | undefined;
        const isd = ch.impactStats;
        if (isd !== undefined && isd !== null && isRecord(isd)) {
          const im: ImpactStats = {};
          for (const key of LIFE_STAT_KEYS) {
            const n = isd[key];
            if (typeof n === "number" && Number.isFinite(n)) im[key] = n;
          }
          if (Object.keys(im).length > 0) impactStats = im;
        }
        const choice: InteractiveChoice = { id: id.trim(), label: label.trim(), nextSegmentId: nextSegmentId.trim() };
        if (skillDeltas) choice.skillDeltas = skillDeltas;
        if (impactStats) choice.impactStats = impactStats;
        const rw = ch.riskWhy;
        if (typeof rw === "string" && rw.trim()) choice.riskWhy = rw.trim();
        const cr = ch.capitalRemaining;
        if (typeof cr === "number" && Number.isFinite(cr)) choice.capitalRemaining = cr;
        const ep = ch.estimatedProfit;
        if (typeof ep === "number" && Number.isFinite(ep)) choice.estimatedProfit = ep;
        const ad = ch.authorityDelta;
        if (typeof ad === "number" && Number.isFinite(ad)) choice.authorityDelta = ad;
        const hd = ch.harmonyDelta;
        if (typeof hd === "number" && Number.isFinite(hd)) choice.harmonyDelta = hd;
        const dst = ch.dialogueStyle;
        if (dst === "simple_clear" || dst === "complex_or_performative") choice.dialogueStyle = dst;
        if (ch.isShortcut === true) choice.isShortcut = true;
        const cseg = ch.consequenceSegmentId;
        if (typeof cseg === "string" && cseg.trim()) choice.consequenceSegmentId = cseg.trim();
        const fi = parseFinancialImpact(ch.financialImpact);
        if (fi) choice.financialImpact = fi;
        choices.push(choice);
      }
    }
    const reflectionRaw = segVal.reflectionPoint;
    const reflectionPoint = reflectionRaw === true;
    const seg: InteractiveSegment = { audioUrl, choices };
    const body = segVal.contentBody;
    if (typeof body === "string" && body.trim()) seg.contentBody = body.trim();
    const bp = segVal.contentBodyParent;
    if (typeof bp === "string" && bp.trim()) seg.contentBodyParent = bp.trim();
    const bc = segVal.contentBodyChild;
    if (typeof bc === "string" && bc.trim()) seg.contentBodyChild = bc.trim();
    if (reflectionPoint) seg.reflectionPoint = true;
    const ethicsRedir = segVal.ethicsConsequenceRedirectIfShortcut;
    if (typeof ethicsRedir === "string" && ethicsRedir.trim()) {
      seg.ethicsConsequenceRedirectIfShortcut = ethicsRedir.trim();
    }
    const scamRaw = segVal.scamSignals;
    if (Array.isArray(scamRaw)) {
      const flags: ScamSignal[] = [];
      for (const x of scamRaw) {
        if (x === "urgency" || x === "hidden_fees" || x === "permissions") flags.push(x);
      }
      if (flags.length) seg.scamSignals = flags;
    }
    segments[sid.trim()] = seg;
  }
  if (!segments[start.trim()]) return null;
  const overlayStyle =
    typeof raw.overlayStyle === "string" && raw.overlayStyle.trim() ? raw.overlayStyle.trim() : null;
  const eduRaw = raw.eduCategory;
  const eduCategory =
    typeof eduRaw === "string" && eduRaw.trim() ? eduRaw.trim() : null;
  const financialRealityEnabled = raw.financialRealityEnabled === true;
  const dcs = raw.defaultCrisisSegmentId;
  const defaultCrisisSegmentId =
    typeof dcs === "string" && dcs.trim() ? dcs.trim() : null;
  return {
    startSegmentId: start.trim(),
    segments,
    overlayStyle,
    eduCategory,
    financialRealityEnabled,
    defaultCrisisSegmentId,
  };
}

export function resolveInteractiveSegmentAudioUrl(audioUrl: string): string | null {
  return resolveLibraryAudioUrl(audioUrl);
}

/** Warm cache for branch audio while choice overlay is visible (best-effort). */
export async function prefetchInteractiveAudio(urls: string[]): Promise<void> {
  if (typeof window === "undefined") return;
  const token = localStorage.getItem("tamixa_access_token");
  // Resolve any bare storage keys (stories/...) that weren't pre-resolved by the caller.
  const resolved = urls.map((u) => resolveLibraryAudioUrl(u) ?? u.trim()).filter(Boolean);
  const distinct = [...new Set(resolved)];
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

/**
 * Life Bar deltas for a choice: per-key `impactStats` wins when set; otherwise uses the same key from `skillDeltas`.
 */
export function impactFromInteractiveChoice(ch: InteractiveChoice): ImpactStats {
  const out: ImpactStats = {};
  const sd = ch.skillDeltas;
  for (const key of LIFE_STAT_KEYS) {
    const a = ch.impactStats?.[key];
    const b = sd?.[key];
    const v =
      typeof a === "number" && Number.isFinite(a)
        ? a
        : typeof b === "number" && Number.isFinite(b)
          ? b
          : undefined;
    if (v !== undefined) out[key] = v;
  }
  return out;
}

/** Next segment after a choice; shortcut + consequenceSegmentId overrides linear next. */
export function resolveInteractiveChoiceNavigation(ch: InteractiveChoice): string {
  if (ch.isShortcut === true && ch.consequenceSegmentId?.trim()) {
    return ch.consequenceSegmentId.trim();
  }
  return ch.nextSegmentId;
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
