import type { SupportedLanguageCode } from "../config/languages.js";

/**
 * Optional memory of recent generations for bulk / session-level anti-repetition.
 */
export interface RecentStoryPatterns {
  /** First ~1–3 sentences or normalized opening snippets */
  openings?: string[];
  morals?: string[];
  characterNames?: string[];
  /** Short one-line plot summaries */
  plotSummaries?: string[];
}

export interface StoryPipelineInput {
  language: SupportedLanguageCode;
  category: string;
  combined_situation: string;
  recent_story_patterns?: RecentStoryPatterns;
  /** Plain strings to avoid echoing (titles, names, phrases) */
  avoid_repeating?: string[];
}

export interface StoryPipelineResult {
  story: import("../schemas/story-output.schema.js").StoryOutput;
  /** Structured plan from stage 1 (for debugging / analytics) */
  plan: import("../schemas/planner-output.schema.js").PlannerPlan;
  /** Validation notes from final pass */
  validationWarnings: string[];
  /** Number of repair / retry cycles used */
  repairAttempts: number;
}
