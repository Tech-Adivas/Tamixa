/**
 * Story pipeline orchestrator: Planner → Draft → Refiner → validate → Repair loop.
 * Architecture: each stage is a pure prompt build + LLM call + parse; validation is deterministic.
 */

import { getLanguageConfig, assertSupportedLanguage } from "../config/languages.js";
import { buildPlannerPrompt, buildDraftWriterPrompt, buildRefinerPrompt, buildRepairPrompt } from "../prompts/index.js";
import { parseJsonObject } from "../utils/json-extract.js";
import { coerceRawStoryOutput, tryParseStoryWithLocalFixes } from "../utils/repair-helpers.js";
import { validatePlannerShape, validateStoryShape } from "../validators/json-shape.validator.js";
import { runFullStoryValidation } from "../validators/index.js";
import type { StoryPipelineInput, StoryPipelineResult } from "../types/pipeline-input.js";
import type { LlmProvider, LlmCallContext } from "./llm-provider.js";
import type { PlannerPlan } from "../schemas/planner-output.schema.js";
import type { StoryOutput } from "../schemas/story-output.schema.js";

export interface StoryPipelineOptions {
  maxPlannerAttempts?: number;
  maxRepairAttempts?: number;
  draftTemperature?: number;
  refinerTemperature?: number;
}

const DEFAULT_OPTS: Required<StoryPipelineOptions> = {
  maxPlannerAttempts: 2,
  maxRepairAttempts: 3,
  draftTemperature: 0.75,
  refinerTemperature: 0.5,
};

export class StoryPipelineOrchestrator {
  constructor(
    private readonly llm: LlmProvider,
    private readonly options: StoryPipelineOptions = {},
  ) {}

  private opts(): Required<StoryPipelineOptions> {
    return { ...DEFAULT_OPTS, ...this.options };
  }

  async run(input: StoryPipelineInput): Promise<StoryPipelineResult> {
    const langCode = assertSupportedLanguage(input.language);
    const lang = getLanguageConfig(langCode)!;
    const o = this.opts();
    let repairAttempts = 0;
    const allWarnings: string[] = [];

    let plan: PlannerPlan | undefined;
    let lastPlannerErrors: string[] = [];

    for (let attempt = 0; attempt < o.maxPlannerAttempts; attempt++) {
      const prompt = buildPlannerPrompt({ ...input, language: langCode }, lang);
      const extra =
        lastPlannerErrors.length > 0
          ? `\n\nPrevious validation errors (fix in new plan):\n${lastPlannerErrors.map((e) => `- ${e}`).join("\n")}`
          : "";
      const raw = await this.llm.complete(prompt + extra, { stage: "planner", input }, { temperature: 0.55 });
      let parsed: unknown;
      try {
        parsed = parseJsonObject(raw);
      } catch (e) {
        lastPlannerErrors = [`planner JSON parse failed: ${(e as Error).message}`];
        continue;
      }
      const shaped = validatePlannerShape(parsed);
      if (!shaped.ok) {
        lastPlannerErrors = shaped.errors;
        continue;
      }
      if (shaped.data.target_language_code !== langCode) {
        lastPlannerErrors = [`planner target_language_code must be ${langCode}`];
        continue;
      }
      plan = shaped.data;
      break;
    }

    if (!plan) {
      throw new StoryPipelineError("planner_failed", `Planner failed after ${o.maxPlannerAttempts} attempts`, {
        errors: lastPlannerErrors,
      });
    }

    const draftPrompt = buildDraftWriterPrompt(plan, input.category, lang);
    const draftRaw = await this.llm.complete(draftPrompt, { stage: "draft_writer", input, plan }, { temperature: o.draftTemperature });
    const draftParsed = this.parseStoryJson(draftRaw, "draft_writer");

    const refinerPrompt = buildRefinerPrompt(draftParsed, lang);
    const refinerRaw = await this.llm.complete(refinerPrompt, { stage: "refiner", input, plan, draft: draftParsed }, { temperature: o.refinerTemperature });
    let story = this.parseStoryJson(refinerRaw, "refiner");

    let validation = runFullStoryValidation(story, lang, input.recent_story_patterns, input.avoid_repeating);
    allWarnings.push(...validation.warnings);

    while (!validation.ok && repairAttempts < o.maxRepairAttempts) {
      repairAttempts++;
      const repairPrompt = buildRepairPrompt({
        lang,
        malformedOrStoryJson: JSON.stringify(story),
        validationErrors: validation.errors,
      });
      const callCtx: LlmCallContext = {
        stage: "repair",
        input,
        plan,
        draft: story,
        validationErrors: validation.errors,
      };
      const repairedRaw = await this.llm.complete(repairPrompt, callCtx, { temperature: 0.35 });
      story = this.parseStoryJson(repairedRaw, "repair");
      validation = runFullStoryValidation(story, lang, input.recent_story_patterns, input.avoid_repeating);
      allWarnings.push(...validation.warnings);
    }

    if (!validation.ok) {
      throw new StoryPipelineError("validation_failed", "Story failed validation after repair attempts", {
        errors: validation.errors,
        warnings: validation.warnings,
        repairAttempts,
        lastStory: story,
      });
    }

    return {
      story,
      plan,
      validationWarnings: [...new Set(allWarnings)],
      repairAttempts,
    };
  }

  private parseStoryJson(raw: string, source: string): StoryOutput {
    let parsed: unknown;
    try {
      parsed = parseJsonObject(raw);
    } catch (e) {
      throw new StoryPipelineError("parse_failed", `Failed to parse story JSON from ${source}`, {
        cause: (e as Error).message,
      });
    }
    const fixed = tryParseStoryWithLocalFixes(parsed);
    if (fixed.ok) return fixed.data;
    const coerced = coerceRawStoryOutput(parsed as Record<string, unknown>);
    const shaped = validateStoryShape(coerced);
    if (shaped.ok) return shaped.data;
    throw new StoryPipelineError("shape_invalid", `Story shape invalid from ${source}`, { errors: shaped.errors });
  }
}

export class StoryPipelineError extends Error {
  constructor(
    public readonly code: "planner_failed" | "parse_failed" | "shape_invalid" | "validation_failed",
    message: string,
    public readonly details?: Record<string, unknown>,
  ) {
    super(message);
    this.name = "StoryPipelineError";
  }
}
