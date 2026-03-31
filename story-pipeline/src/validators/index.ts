export * from "./json-shape.validator.js";
export * from "./language.validator.js";
export * from "./markers.validator.js";
export * from "./safety.validator.js";
export * from "./title-theme.validator.js";
export * from "./repetition.validator.js";

import type { LanguageConfig } from "../config/languages.js";
import type { StoryOutput } from "../schemas/story-output.schema.js";
import type { RecentStoryPatterns } from "../types/pipeline-input.js";
import { validateStoryShape } from "./json-shape.validator.js";
import { validateLanguageCompliance } from "./language.validator.js";
import { validateTtsMarkers } from "./markers.validator.js";
import { validateSafetyText } from "./safety.validator.js";
import { validateTitleAndTheme } from "./title-theme.validator.js";
import { validateRepetition } from "./repetition.validator.js";

export interface FullValidationResult {
  ok: boolean;
  errors: string[];
  warnings: string[];
}

/**
 * Aggregates all deterministic checks for final story JSON.
 */
export function runFullStoryValidation(
  raw: unknown,
  lang: LanguageConfig,
  recent?: RecentStoryPatterns,
  avoidRepeating?: string[],
): FullValidationResult {
  const errors: string[] = [];
  const warnings: string[] = [];

  const shape = validateStoryShape(raw);
  if (!shape.ok) {
    return { ok: false, errors: shape.errors, warnings };
  }

  const story = shape.data;

  const titleTheme = validateTitleAndTheme(story.title, story.theme);
  if (!titleTheme.ok) errors.push(...titleTheme.errors);

  const markers = validateTtsMarkers(story.story_text);
  if (!markers.ok) errors.push(...markers.errors);

  const langVal = validateLanguageCompliance(lang, story);
  if (!langVal.ok) errors.push(...langVal.errors);

  const safety = validateSafetyText(`${story.title}\n${story.theme}\n${story.story_text}\n${story.moral}`);
  if (!safety.ok) errors.push(...safety.errors);

  const rep = validateRepetition(story, recent, avoidRepeating);
  warnings.push(...rep.warnings);
  if (!rep.ok) errors.push(...rep.errors);

  return { ok: errors.length === 0, errors, warnings };
}

export function validateStoryObject(
  story: StoryOutput,
  lang: LanguageConfig,
  recent?: RecentStoryPatterns,
  avoidRepeating?: string[],
): FullValidationResult {
  return runFullStoryValidation(story, lang, recent, avoidRepeating);
}
