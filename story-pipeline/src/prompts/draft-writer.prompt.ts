import type { LanguageConfig } from "../config/languages.js";
import type { PlannerPlan } from "../schemas/planner-output.schema.js";
import { criticalJsonLanguageRules, tamixaSafetyBlock, targetTenMinuteNarrationAndClarityBlock } from "./shared.js";

/**
 * Stage 2 — Draft Writer: full story JSON from plan.
 */
export function buildDraftWriterPrompt(plan: PlannerPlan, category: string, lang: LanguageConfig): string {
  return `
You are the Tamixa Draft Writer. Convert the story plan into a full story as JSON only — no markdown, no code fences.

${criticalJsonLanguageRules(lang)}

${tamixaSafetyBlock()}

${targetTenMinuteNarrationAndClarityBlock(lang)}

## Storytelling format (story_text)
- Mix narrator prose and dialogue. Attribute dialogue clearly (e.g. "Name: …" in ${lang.displayName}).
- Use allowed TTS markers at natural pauses and emotional beats.
- Align tightly with the plan: **every scene_outline beat** must appear in order with **full continuity** — **no skipped or missing scenes**.
- **Vocabulary first:** keep wording **easy to understand** for a child listener in ${lang.displayName}; clarity beats cleverness.

## Plan (source of truth)
${JSON.stringify(plan)}

## category label for output field
The "category" value must be written entirely in **${lang.displayName}**, conveying the same idea as this admin label: ${category}

## Output schema (keys exactly)
{
  "title": "",
  "category": "",
  "theme": "",
  "story_text": "",
  "moral": "",
  "estimated_duration_seconds": 0
}

- estimated_duration_seconds: integer — target **~600** (10 minutes) from actual speakable length at a **clear, moderate** pace; adjust if word count clearly supports 9–11 minutes only.
- title: specific and catchy, not generic.
- theme: concrete, not a single vague word like "Kindness" alone.
`.trim();
}
