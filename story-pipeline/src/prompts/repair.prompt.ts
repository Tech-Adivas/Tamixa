import type { LanguageConfig } from "../config/languages.js";
import { criticalJsonLanguageRules, tamixaSafetyBlock, targetTenMinuteNarrationAndClarityBlock } from "./shared.js";

/**
 * Stage 4 — Repair: fix validation errors while keeping Tamixa rules.
 */
export function buildRepairPrompt(params: {
  lang: LanguageConfig;
  malformedOrStoryJson: string;
  validationErrors: string[];
}): string {
  const { lang, malformedOrStoryJson, validationErrors } = params;

  return `
You are the Tamixa JSON Repair Agent. Fix the story JSON to pass validation. Output **only** valid JSON — no markdown, no code fences, no explanation.

${criticalJsonLanguageRules(lang)}

${tamixaSafetyBlock()}

${targetTenMinuteNarrationAndClarityBlock(lang)}

## Validation errors to resolve
${validationErrors.map((e, i) => `${i + 1}. ${e}`).join("\n")}

## Current content (may be invalid JSON — reconstruct if needed)
${malformedOrStoryJson}

## Required output schema (keys exactly)
{
  "title": "",
  "category": "",
  "theme": "",
  "story_text": "",
  "moral": "",
  "estimated_duration_seconds": 0
}

If story_text was truncated, **complete** it safely within policy to restore **~10 minutes** of narration with **continuous, gap-free** scenes and **simple vocabulary**.
If forbidden content appeared, rewrite those passages to be compliant while keeping the story coherent.
estimated_duration_seconds must be a positive integer, typically **~600** for a full-length repaired story.
`.trim();
}
