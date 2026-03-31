import type { LanguageConfig } from "../config/languages.js";
import type { StoryOutput } from "../schemas/story-output.schema.js";
import { criticalJsonLanguageRules, tamixaSafetyBlock, targetTenMinuteNarrationAndClarityBlock } from "./shared.js";

/**
 * Stage 3 — Refiner: polish draft while preserving meaning and safety.
 */
export function buildRefinerPrompt(draft: StoryOutput, lang: LanguageConfig): string {
  return `
You are the Tamixa Story Refiner. Improve clarity, warmth, flow, transitions, and read-aloud quality.
Preserve plot, moral, character names, and safety. Output JSON only — no markdown, no code fences.

${criticalJsonLanguageRules(lang)}

${tamixaSafetyBlock()}

${targetTenMinuteNarrationAndClarityBlock(lang)}

## Refiner priorities
- **Do not** shorten the story below ~10 minutes of speakable content unless fixing errors; preserve **continuity** and **every scene** — fix gaps by **adding** bridging sentences, not by cutting beats.
- Prefer **simpler words** where meaning stays the same (vocabulary accessibility is critical).

## Draft to refine
${JSON.stringify(draft)}

## Output schema (keys exactly; same as input)
{
  "title": "",
  "category": "",
  "theme": "",
  "story_text": "",
  "moral": "",
  "estimated_duration_seconds": 0
}

- Adjust estimated_duration_seconds if story_text length changed; keep near **~600** seconds if length still matches ~10 min TTS.
- Keep only allowed TTS markers; do not add new bracket tags.
- Do not change JSON keys.
`.trim();
}
