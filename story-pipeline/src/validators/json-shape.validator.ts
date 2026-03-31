import type { ZodError } from "zod";
import { StoryOutputSchema, type StoryOutput } from "../schemas/story-output.schema.js";
import { PlannerPlanSchema, type PlannerPlan } from "../schemas/planner-output.schema.js";

export function formatZodError(err: ZodError): string[] {
  return err.issues.map((i) => `${i.path.join(".") || "(root)"}: ${i.message}`);
}

export function validateStoryShape(raw: unknown): { ok: true; data: StoryOutput } | { ok: false; errors: string[] } {
  const r = StoryOutputSchema.safeParse(raw);
  if (r.success) return { ok: true, data: r.data };
  return { ok: false, errors: formatZodError(r.error) };
}

export function validatePlannerShape(raw: unknown): { ok: true; data: PlannerPlan } | { ok: false; errors: string[] } {
  const r = PlannerPlanSchema.safeParse(raw);
  if (r.success) return { ok: true, data: r.data };
  return { ok: false, errors: formatZodError(r.error) };
}
