import { lintInteractiveGraphJson } from "./interactive-graph-lint";

/**
 * When a valid interactive graph JSON is present, the story category (`theme`) must be a
 * Learn · Simulator lane so mobile/web discovery and trust labels stay correct.
 * @see docs/admin/EDU_METADATA_CONVENTIONS.md §2–3
 */
export function validateInteractiveStoryCategory(
  theme: string | null | undefined,
  interactiveGraphRaw: string | null | undefined
): string | null {
  const ig = interactiveGraphRaw?.trim();
  if (!ig) return null;
  const lint = lintInteractiveGraphJson(ig);
  if (!lint.ok) return null;
  const t = theme?.trim() ?? "";
  if (!t.startsWith("Learn · Simulator")) {
    return 'Interactive episodes need a "Learn · Simulator …" category (e.g. Learn · Simulator · Digital Safety). Linear safety tales use Learn · Digital Safety without a graph.';
  }
  return null;
}
