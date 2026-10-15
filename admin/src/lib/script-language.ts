/**
 * Which writing system a text uses, for the app's languages (en, ta, hi, te, kn, ml).
 * Mirrors backend `ScriptLanguage` so review screens flag titles stored in the wrong language.
 */
const INDIC_SCRIPTS: Record<string, RegExp> = {
  ta: /[஀-௿]/,
  hi: /[ऀ-ॿ]/,
  te: /[ఀ-౿]/,
  kn: /[ಀ-೿]/,
  ml: /[ഀ-ൿ]/,
};

const LANGUAGE_NAMES: Record<string, string> = {
  en: "English",
  ta: "Tamil",
  hi: "Hindi",
  te: "Telugu",
  kn: "Kannada",
  ml: "Malayalam",
};

function indicScriptsIn(text: string): string[] {
  return Object.entries(INDIC_SCRIPTS)
    .filter(([, re]) => re.test(text))
    .map(([lang]) => lang);
}

/** True when `text` is written for `language` (Latin words like "UPI" inside an Indic title are fine). */
export function textMatchesLanguage(text: string | null | undefined, language: string | null | undefined): boolean {
  if (!text?.trim() || !language) return true;
  const lang = language.trim().toLowerCase();
  const found = indicScriptsIn(text);
  if (lang === "en") return found.length === 0;
  if (lang in INDIC_SCRIPTS) return found.includes(lang) && found.length === 1;
  return true;
}

/** e.g. "Title is in Telugu, story is Tamil" — or null when the title matches. */
export function titleLanguageMismatch(title: string | null | undefined, language: string | null | undefined): string | null {
  if (textMatchesLanguage(title, language) || !title || !language) return null;
  const found = indicScriptsIn(title);
  const titleLang = found.length === 0 ? "English" : found.length === 1 ? LANGUAGE_NAMES[found[0]] : "mixed scripts";
  return `Title is in ${titleLang}, story is ${LANGUAGE_NAMES[language.toLowerCase()] ?? language}`;
}
