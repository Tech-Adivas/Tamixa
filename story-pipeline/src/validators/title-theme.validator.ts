const GENERIC_TITLE_EXACT = new Set(
  [
    "a friendship story",
    "the friendship story",
    "a kindness story",
    "the kindness story",
    "a story about friendship",
    "my story",
    "untitled",
  ].map((s) => s.toLowerCase()),
);

const GENERIC_THEME_EXACT = new Set(
  [
    "kindness",
    "friendship",
    "good values",
    "values",
    "moral",
    "helping",
    "honesty",
    "courage",
  ].map((s) => s.toLowerCase()),
);

function normalize(s: string): string {
  return s.trim().toLowerCase().replace(/\s+/g, " ");
}

function isTooShortMeaningful(s: string, minWords: number): boolean {
  const words = normalize(s).split(" ").filter(Boolean);
  return words.length < minWords;
}

/**
 * Reject vague titles/themes that read like placeholders.
 */
export function validateTitleAndTheme(title: string, theme: string): { ok: true } | { ok: false; errors: string[] } {
  const errors: string[] = [];
  const nt = normalize(title);
  const nth = normalize(theme);

  if (GENERIC_TITLE_EXACT.has(nt)) {
    errors.push(`title_theme: Title is too generic: "${title}"`);
  }
  if (GENERIC_THEME_EXACT.has(nth) && nth.split(" ").length <= 2) {
    errors.push(`title_theme: Theme is too generic or single-word: "${theme}"`);
  }
  if (nt.startsWith("a ") && nt.includes("story") && nt.length < 28) {
    errors.push(`title_theme: Title pattern looks generic (short "A ... Story"): "${title}"`);
  }
  if (isTooShortMeaningful(theme, 2)) {
    errors.push(`title_theme: theme must be at least 2 meaningful words (in target language).`);
  }
  if (title.length < 5) {
    errors.push(`title_theme: title too short.`);
  }

  return errors.length ? { ok: false, errors } : { ok: true };
}
