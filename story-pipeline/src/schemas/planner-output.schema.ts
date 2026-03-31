import { z } from "zod";

/**
 * Stage 1 — Planner output. Keys stay English; string values must match target language in production
 * (we validate language in later stages on the final story JSON).
 */

export const SettingTypeEnum = z.enum([
  "urban",
  "semi_urban",
  "village",
  "school",
  "home",
  "nature",
  "fantasy_magical_realism",
]);

export const RegionalFlavorEnum = z.enum([
  "tamil_nadu_south_india_primary",
  "south_india_broader",
  "north_india_secondary",
  "pan_india_inclusive",
]);

export const ConflictTypeEnum = z.enum([
  "misunderstanding",
  "small_obstacle",
  "kindness_challenge",
  "teamwork_needed",
  "honesty_dilemma_mild",
  "curiosity_learning",
]);

const SceneBeat = z.object({
  scene_id: z.number().int().min(1),
  summary: z.string().min(8).max(800),
  emotional_beat: z.string().min(2).max(200),
});

export const PlannerPlanSchema = z.object({
  target_language_code: z.enum(["ta", "en", "hi", "te", "ka", "ml"]),
  story_premise: z.string().min(20).max(2000),
  setting_type: SettingTypeEnum,
  regional_flavor: RegionalFlavorEnum,
  protagonist: z.object({
    name: z.string().min(1).max(120),
    short_description: z.string().min(5).max(400),
    age_band: z.enum(["young_child", "older_child", "animal_or_fantasy_being", "group"]),
  }),
  supporting_characters: z
    .array(
      z.object({
        name: z.string().min(1).max(120),
        role: z.string().min(3).max(200),
      }),
    )
    .min(0)
    .max(6),
  conflict_type: ConflictTypeEnum,
  emotional_arc: z.string().min(10).max(600),
  scene_outline: z.array(SceneBeat).min(3).max(12),
  theme: z.string().min(3).max(200),
  moral: z.string().min(5).max(300),
  safety_notes: z.string().min(5).max(600),
  variation_notes: z.string().min(5).max(800),
});

export type PlannerPlan = z.infer<typeof PlannerPlanSchema>;
