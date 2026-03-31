import type { LanguageConfig } from "../config/languages.js";
import { ALLOWED_TTS_MARKERS } from "../constants/markers.js";
import type { RecentStoryPatterns } from "../types/pipeline-input.js";

export function formatRecentPatterns(r?: RecentStoryPatterns): string {
  if (!r) return "(none provided)";
  const parts: string[] = [];
  if (r.openings?.length) parts.push(`Recent openings (avoid echoing):\n${r.openings.map((s) => `- ${s}`).join("\n")}`);
  if (r.morals?.length) parts.push(`Recent morals (vary phrasing):\n${r.morals.map((s) => `- ${s}`).join("\n")}`);
  if (r.characterNames?.length)
    parts.push(`Recent character names (prefer new names):\n${r.characterNames.map((s) => `- ${s}`).join("\n")}`);
  if (r.plotSummaries?.length)
    parts.push(`Recent plot summaries (differentiate):\n${r.plotSummaries.map((s) => `- ${s}`).join("\n")}`);
  return parts.length ? parts.join("\n\n") : "(none provided)";
}

export function formatAvoidRepeating(list?: string[]): string {
  if (!list?.length) return "(none)";
  return list.map((s) => `- ${s}`).join("\n");
}

export function criticalJsonLanguageRules(lang: LanguageConfig): string {
  return `
## JSON language rules (non-negotiable)
- JSON **keys** must remain exactly in English as specified in the schema.
- All JSON **string values** must be written entirely in **${lang.displayName}** (${lang.code}).
- No transliteration of ${lang.displayName} into Latin (except language code "en" uses Latin script naturally).
- Do not mix languages inside any string value.
- Inline TTS markers inside story_text must remain **exactly** in English, one of:
  ${ALLOWED_TTS_MARKERS.join(", ")}
- Do not invent any other [Bracket] markers.

## Style for ${lang.displayName}
${lang.styleHint}
`.trim();
}

export function tamixaSafetyBlock(): string {
  return `
## Child safety & content policy (zero tolerance)
- Audience: children under 12 and families. Warm, magical, comforting, joyful tone.
- Mild conflict only; resolved peacefully. No shaming winners/losers.
- FORBIDDEN anywhere in output: violence, gore, horror, death, abuse, bullying, humiliation, weapons, war, politics, religious conflict, drugs, alcohol, romance/dating, sexual content, self-harm, suicide, ghosts, curses, scary supernatural threats, dangerous stunts readers might imitate, brand names, celebrities, real sensitive locations.
- No stereotyping of communities, religions, regions, or languages.
- Stories are fictional and uplifting; inclusive across India.
`.trim();
}

export function settingDiversityInstruction(): string {
  return `
## Setting diversity
Choose a **setting_type** that fits the premise; rotate across bulk runs — include urban, semi-urban, village, school, home, nature, and fantasy/magical realism over time. Do not default every story to a village.
`.trim();
}

/**
 * Planner-only: scene graph must support a full ~10 min story with simple vocabulary (no missing beats).
 */
export function plannerSupportsTenMinuteStoryBlock(lang: LanguageConfig): string {
  return `
## Plan must support ~10 minutes of narration
- **scene_outline** must include **every step** the listener needs: **no missing scenes**, **no implied jumps** — problem, attempts, emotional turns, and resolution all appear as separate beats where needed.
- Scenes must chain **cause → effect** so the draft never fills unexplained gaps.
- Design the arc so it can be told in **simple, everyday ${lang.displayName}** (child-friendly vocabulary — not rare or literary words as the default).
`.trim();
}

/**
 * Target ~10 min TTS, strict continuity, and vocabulary-first clarity — draft / refiner / repair.
 */
export function targetTenMinuteNarrationAndClarityBlock(lang: LanguageConfig): string {
  return `
## Target narration length (~10 minutes)
The story must be written and paced for **about 10 minutes** of TTS (aim **9–11 minutes**, not a short sketch).
- Set **estimated_duration_seconds** close to **600**, derived from realistic speakable length at a **moderate, clear child-friendly pace** (~130–160 words/min varies by language — prioritize clarity over speed).
- **story_text** must carry enough speakable content to fill that duration (typically **~1,200–1,600 words** of narration+dialogue in **${lang.displayName}**, excluding TTS marker tokens).

## Continuity & completeness (mandatory)
- **Single clear timeline** from opening to ending; **no gaps, no missing scenes**, and **no jumps** the listener cannot follow.
- Every beat in the plan’s **scene_outline** must appear in **story_text** with **causes and effects** spelled out — do not leap from problem to resolution without the **middle steps**.
- When time or place changes, add an **explicit transition** in **${lang.displayName}** (e.g. next morning, after a little while, on the way home, or natural equivalents).

## Vocabulary & understandability (highest priority)
- **Everyday, simple vocabulary in ${lang.displayName}** matters more than sounding literary or advanced.
- Prefer words and phrases a young listener already knows; introduce any less common word only if **context makes the meaning obvious** in one hearing.
- **Short to medium sentences**, **one main idea per sentence**, natural **spoken** rhythm — easy to paraphrase aloud without stumbling.
`.trim();
}
