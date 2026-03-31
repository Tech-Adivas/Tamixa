/**
 * Example: wire the orchestrator into an HTTP API (Express, Fastify, Hono, Next.js Route Handler).
 * Map the returned object to your framework's response. Do not expose raw LLM errors in production.
 */

import { z } from "zod";
import {
  StoryPipelineOrchestrator,
  StoryPipelineError,
  MockTamixaLlmProvider,
  OpenAiCompatibleProvider,
  type StoryPipelineInput,
} from "../src/index.js";

const BodySchema = z.object({
  language: z.enum(["ta", "en", "hi", "te", "ka", "ml"]),
  category: z.string().min(1).max(120),
  combined_situation: z.string().min(5).max(2000),
  recent_story_patterns: z
    .object({
      openings: z.array(z.string()).optional(),
      morals: z.array(z.string()).optional(),
      characterNames: z.array(z.string()).optional(),
      plotSummaries: z.array(z.string()).optional(),
    })
    .optional(),
  avoid_repeating: z.array(z.string()).max(100).optional(),
});

function createProviderFromEnv() {
  const key = process.env.OPENAI_API_KEY;
  if (key) {
    return new OpenAiCompatibleProvider({
      apiKey: key,
      baseUrl: process.env.OPENAI_BASE_URL,
      model: process.env.STORY_MODEL ?? "gpt-4o",
    });
  }
  return new MockTamixaLlmProvider();
}

/** Create one orchestrator per process (or per request if you inject per-tenant LLM config). */
export function createStoryOrchestrator() {
  return new StoryPipelineOrchestrator(createProviderFromEnv());
}

export type GenerateStoryHttpResult =
  | { status: 200; body: unknown }
  | { status: 400; body: unknown }
  | { status: 422; body: unknown }
  | { status: 500; body: unknown };

/**
 * Framework-agnostic handler: pass `req.body` (already parsed JSON).
 */
export async function handlePostGenerateStory(
  orchestrator: StoryPipelineOrchestrator,
  jsonBody: unknown,
): Promise<GenerateStoryHttpResult> {
  const parsed = BodySchema.safeParse(jsonBody);
  if (!parsed.success) {
    return { status: 400, body: { error: "invalid_body", details: parsed.error.flatten() } };
  }
  const body = parsed.data;
  const input: StoryPipelineInput = {
    language: body.language,
    category: body.category,
    combined_situation: body.combined_situation,
    recent_story_patterns: body.recent_story_patterns,
    avoid_repeating: body.avoid_repeating,
  };

  try {
    const result = await orchestrator.run(input);
    return {
      status: 200,
      body: {
        story: result.story,
        meta: {
          repairAttempts: result.repairAttempts,
          validationWarnings: result.validationWarnings,
          planSettingType: result.plan.setting_type,
        },
      },
    };
  } catch (e) {
    if (e instanceof StoryPipelineError) {
      if (e.code === "validation_failed") {
        return { status: 422, body: { error: e.code, message: e.message, details: e.details } };
      }
      return { status: 400, body: { error: e.code, message: e.message, details: e.details } };
    }
    return { status: 500, body: { error: "internal_error" } };
  }
}
