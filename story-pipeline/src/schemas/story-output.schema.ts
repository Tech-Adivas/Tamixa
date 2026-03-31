import { z } from "zod";

/**
 * Final production schema — exact keys, English keys only.
 * Values must be in target language (validated separately).
 */

export const StoryOutputSchema = z.object({
  title: z.string().min(3).max(200),
  category: z.string().min(2).max(120),
  theme: z.string().min(3).max(200),
  story_text: z.string().min(400).max(50_000),
  moral: z.string().min(5).max(400),
  estimated_duration_seconds: z.number().finite().positive().max(3600),
});

export type StoryOutput = z.infer<typeof StoryOutputSchema>;

/** Lenient parse for repair stage input */
export const StoryOutputLooseSchema = z.object({
  title: z.unknown(),
  category: z.unknown(),
  theme: z.unknown(),
  story_text: z.unknown(),
  moral: z.unknown(),
  estimated_duration_seconds: z.unknown(),
});
