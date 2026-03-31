import type { LanguageConfig } from "../config/languages.js";
import type { StoryPipelineInput } from "../types/pipeline-input.js";
import {
  criticalJsonLanguageRules,
  formatAvoidRepeating,
  formatRecentPatterns,
  settingDiversityInstruction,
  tamixaSafetyBlock,
  plannerSupportsTenMinuteStoryBlock,
} from "./shared.js";

/**
 * Stage 1 — Planner: structured plan JSON only.
 */
export function buildPlannerPrompt(input: StoryPipelineInput, lang: LanguageConfig): string {
  const { category, combined_situation, recent_story_patterns, avoid_repeating } = input;

  return `
You are the Tamixa Story Planner. Output a single JSON object — no markdown, no code fences, no commentary.

${criticalJsonLanguageRules(lang)}

${tamixaSafetyBlock()}

${settingDiversityInstruction()}

${plannerSupportsTenMinuteStoryBlock(lang)}

## Input
- category (English key for downstream; the **value** fields in your JSON must still be in ${lang.displayName} where they are narrative text): ${category}
- combined_situation: ${combined_situation}
- target_language_code: ${lang.code}

## Anti-repetition context
${formatRecentPatterns(recent_story_patterns)}

## avoid_repeating (strings to not reuse or closely mimic)
${formatAvoidRepeating(avoid_repeating)}

## Required output schema (keys exactly)
{
  "target_language_code": "${lang.code}",
  "story_premise": "",
  "setting_type": "urban | semi_urban | village | school | home | nature | fantasy_magical_realism",
  "regional_flavor": "tamil_nadu_south_india_primary | south_india_broader | north_india_secondary | pan_india_inclusive",
  "protagonist": { "name": "", "short_description": "", "age_band": "young_child | older_child | animal_or_fantasy_being | group" },
  "supporting_characters": [{ "name": "", "role": "" }],
  "conflict_type": "misunderstanding | small_obstacle | kindness_challenge | teamwork_needed | honesty_dilemma_mild | curiosity_learning",
  "emotional_arc": "",
  "scene_outline": [{ "scene_id": 1, "summary": "", "emotional_beat": "" }],
  "theme": "",
  "moral": "",
  "safety_notes": "",
  "variation_notes": ""
}

Rules:
- scene_outline: 3–12 scenes, ordered, covering full arc with **explicit continuity** (no skipped beats; listener can follow start→middle→end).
- variation_notes: explicitly state how this plan differs from recent patterns (openings, morals, character types, setting).
- Primary cultural inspiration: Tamil Nadu / South India; secondary: broader India — reflect in names, festivals, or setting details when natural, without stereotypes.
- All string values except target_language_code enum tokens must be in ${lang.displayName}.
`.trim();
}
