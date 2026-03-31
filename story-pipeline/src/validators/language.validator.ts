import type { LanguageConfig, ScriptId } from "../config/languages.js";
import { ALLOWED_MARKERS_REGEX } from "../constants/markers.js";

const RE_LATIN_LETTER = /\p{Script=Latin}/u;
const RE_TAMIL = /\p{Script=Tamil}/u;
const RE_DEVANAGARI = /\p{Script=Devanagari}/u;
const RE_TELUGU = /\p{Script=Telugu}/u;
const RE_KANNADA = /\p{Script=Kannada}/u;
const RE_MALAYALAM = /\p{Script=Malayalam}/u;

const OTHER_SCRIPT_CHECKERS: Partial<Record<LanguageConfig["code"], RegExp[]>> = {
  ta: [RE_DEVANAGARI, RE_TELUGU, RE_KANNADA, RE_MALAYALAM],
  hi: [RE_TAMIL, RE_TELUGU, RE_KANNADA, RE_MALAYALAM],
  te: [RE_TAMIL, RE_DEVANAGARI, RE_KANNADA, RE_MALAYALAM],
  ka: [RE_TAMIL, RE_DEVANAGARI, RE_TELUGU, RE_MALAYALAM],
  ml: [RE_TAMIL, RE_DEVANAGARI, RE_TELUGU, RE_KANNADA],
  en: [RE_TAMIL, RE_DEVANAGARI, RE_TELUGU, RE_KANNADA, RE_MALAYALAM],
};

const PRIMARY_SCRIPT_REGEX: Record<ScriptId, RegExp> = {
  Tamil: RE_TAMIL,
  Latin: RE_LATIN_LETTER,
  Devanagari: RE_DEVANAGARI,
  Telugu: RE_TELUGU,
  Kannada: RE_KANNADA,
  Malayalam: RE_MALAYALAM,
};

function stripMarkersAndNormalize(s: string): string {
  return s.replace(ALLOWED_MARKERS_REGEX, "");
}

function countLetterScripts(s: string): { primary: number; latin: number; other: number } {
  let primary = 0;
  let latin = 0;
  let other = 0;
  for (const ch of s) {
    if (/[\s\d\p{P}\p{S}]/u.test(ch)) continue;
    const isLatin = RE_LATIN_LETTER.test(ch);
    const isTamil = RE_TAMIL.test(ch);
    const isDeva = RE_DEVANAGARI.test(ch);
    const isTel = RE_TELUGU.test(ch);
    const isKan = RE_KANNADA.test(ch);
    const isMal = RE_MALAYALAM.test(ch);
    const isLetter = isLatin || isTamil || isDeva || isTel || isKan || isMal;
    if (!isLetter) {
      other++;
      continue;
    }
    if (isLatin) latin++;
    else primary++;
  }
  return { primary, latin, other };
}

/**
 * Validates that string fields use the expected script and avoid cross-script leakage.
 * Heuristic — not a substitute for human editorial review.
 */
export function validateLanguageCompliance(lang: LanguageConfig, story: {
  title: string;
  category: string;
  theme: string;
  story_text: string;
  moral: string;
}): { ok: true } | { ok: false; errors: string[] } {
  const errors: string[] = [];
  const fields: [string, string][] = [
    ["title", story.title],
    ["category", story.category],
    ["theme", story.theme],
    ["story_text", story.story_text],
    ["moral", story.moral],
  ];

  const forbidden = OTHER_SCRIPT_CHECKERS[lang.code] ?? [];

  for (const [key, val] of fields) {
    const stripped = stripMarkersAndNormalize(val);
    for (const re of forbidden) {
      if (re.test(stripped)) {
        errors.push(
          `language/script: field "${key}" contains characters from a script not allowed for ${lang.code} (transliteration or mixed-language suspected).`,
        );
        break;
      }
    }
    // English uses Latin; forbidden Indic scripts already checked above.
    if (lang.code === "en") continue;

    const primaryRe = PRIMARY_SCRIPT_REGEX[lang.primaryScript];
    const { primary, latin, other } = countLetterScripts(stripped);
    const letterTotal = primary + latin + other;
    if (letterTotal === 0) {
      errors.push(`language/script: field "${key}" has no letters after stripping markers.`);
      continue;
    }
    if (!primaryRe.test(stripped)) {
      errors.push(`language/script: field "${key}" missing expected ${lang.primaryScript} script for ${lang.code}.`);
    }
    // Allow small Latin fragments for acronyms — keep strict for story_text
    const latinRatio = latin / letterTotal;
    if (key === "story_text" && latinRatio > 0.03) {
      errors.push(
        `language/script: story_text has too much Latin text (${(latinRatio * 100).toFixed(1)}%) — likely transliteration or English mixing.`,
      );
    }
  }

  return errors.length ? { ok: false, errors } : { ok: true };
}
