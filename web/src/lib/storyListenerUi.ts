/**
 * Listener-facing copy: every Tamixa library story is built for growth (heart, vocabulary, curiosity).
 * The "Fun" lane is for lighter / classic tales—use category "Fun stories" or "Funny Stories" in admin.
 */

const FUN_STORY_LABELS = new Set(["fun stories", "funny stories"]);

/** True when theme or category is the curated fun / laughs lane (legacy-friendly). */
export function isFunStory(theme: string, category?: string | null): boolean {
  const c = category?.trim().toLowerCase() ?? "";
  const t = theme?.trim().toLowerCase() ?? "";
  return FUN_STORY_LABELS.has(c) || FUN_STORY_LABELS.has(t);
}

/** @deprecated Prefer {@link isFunStory}; kept for any code still checking old "Learn ·" rows. */
const LEARN_PREFIX = /^learn\b/i;

export function isLearnStory(theme: string, category?: string | null): boolean {
  const c = category?.trim() ?? "";
  const t = theme?.trim() ?? "";
  return LEARN_PREFIX.test(c) || LEARN_PREFIX.test(t);
}

export function funCornerBadgeLabel(): string {
  return "Just for fun";
}

/** Short line under card title. */
export function listenerCardMetaLine(theme: string, category?: string | null): string {
  const c = category?.trim();
  if (c) return c;
  return theme.trim();
}

const MAX_PLAYER_CONTEXT = 120;

/** Subtitle under the player title while listening. */
export function listenerPlaybackSubtitle(
  storySource: string,
  theme: string,
  category?: string | null,
  title?: string | null,
): string | null {
  const t = theme?.trim() ?? "";
  const c = category?.trim() ?? "";
  const ti = title?.trim() ?? "";
  if (storySource === "library" || storySource === "favorites") {
    if (isFunStory(t, c || null)) {
      const focus = c || t || "Just for fun";
      return focus.length > MAX_PLAYER_CONTEXT ? `${focus.slice(0, MAX_PLAYER_CONTEXT)}…` : focus;
    }
    if (t && ti && t !== ti) {
      return t.length > MAX_PLAYER_CONTEXT ? `${t.slice(0, MAX_PLAYER_CONTEXT)}…` : t;
    }
    if (c) {
      return c.length > MAX_PLAYER_CONTEXT ? `${c.slice(0, MAX_PLAYER_CONTEXT)}…` : c;
    }
    return null;
  }
  if (storySource === "generated" || storySource === "mine") {
    if (!t) return null;
    if (ti && t === ti) return null;
    return t.length > MAX_PLAYER_CONTEXT ? `${t.slice(0, MAX_PLAYER_CONTEXT)}…` : t;
  }
  return null;
}
