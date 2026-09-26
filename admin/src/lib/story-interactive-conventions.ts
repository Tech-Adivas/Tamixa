import { lintInteractiveGraphJson } from "./interactive-graph-lint";

/**
 * Canonical prefix for interactive / branching library stories (admin theme + mobile Practice lane).
 * Keep in sync with backend [StoryLibraryValidation.themeOrCategoryLooksLikeSimulator].
 */
export const SIMULATOR_THEME_PREFIX = "Learn · Simulator";

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
  if (!t.startsWith(SIMULATOR_THEME_PREFIX)) {
    return `Interactive episodes need a "${SIMULATOR_THEME_PREFIX} …" category (e.g. ${SIMULATOR_THEME_PREFIX} · Digital Safety). Linear safety tales use Learn · Digital Safety without a graph.`;
  }
  return null;
}
