import type { StoryOutput } from "../schemas/story-output.schema.js";
import type { RecentStoryPatterns } from "../types/pipeline-input.js";

function normalize(s: string): string {
  return s
    .toLowerCase()
    .replace(/\[[^\]]+\]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function jaccardBigrams(a: string, b: string): number {
  const bigrams = (t: string) => {
    const s = t.slice(0, 400);
    const g = new Set<string>();
    for (let i = 0; i < s.length - 1; i++) g.add(s.slice(i, i + 2));
    return g;
  };
  const A = bigrams(a);
  const B = bigrams(b);
  if (!A.size || !B.size) return 0;
  let inter = 0;
  for (const x of A) if (B.has(x)) inter++;
  return inter / (A.size + B.size - inter);
}

/**
 * Flags structural repetition vs recent bulk outputs (heuristic).
 */
export function validateRepetition(
  story: StoryOutput,
  recent?: RecentStoryPatterns,
  avoidRepeating?: string[],
): { ok: true; warnings: string[] } | { ok: false; errors: string[]; warnings: string[] } {
  const warnings: string[] = [];
  const errors: string[] = [];
  const opening = normalize(story.story_text).slice(0, 220);

  if (recent?.openings?.length) {
    for (const o of recent.openings) {
      const sim = jaccardBigrams(opening, normalize(o));
      if (sim > 0.45) {
        errors.push(`repetition: Opening is too similar to a recent story (similarity ~${sim.toFixed(2)}).`);
        break;
      }
      if (sim > 0.28) warnings.push("repetition: Opening somewhat similar to recent — consider more variety.");
    }
  }

  if (recent?.morals?.length) {
    const m = normalize(story.moral);
    for (const rm of recent.morals) {
      if (m === normalize(rm)) {
        errors.push("repetition: Moral text matches a recent story moral.");
        break;
      }
      if (jaccardBigrams(m, normalize(rm)) > 0.55) {
        errors.push("repetition: Moral is too close to a recent moral.");
        break;
      }
    }
  }

  if (avoidRepeating?.length) {
    const blob = normalize(`${story.title} ${story.story_text} ${story.moral}`);
    for (const phrase of avoidRepeating) {
      const p = normalize(phrase);
      if (p.length >= 4 && blob.includes(p)) {
        errors.push(`repetition: Output contains avoided phrase: "${phrase.slice(0, 80)}".`);
      }
    }
  }

  return errors.length ? { ok: false, errors, warnings } : { ok: true, warnings };
}
