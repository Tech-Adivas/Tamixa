/**
 * Supported output languages for Tamixa story generation.
 * Keys are API-facing codes. Scripts map to Unicode property names for validation.
 *
 * Note: Kannada is often ISO 639-1 `kn`; this project uses `ka` per product contract.
 */

export type SupportedLanguageCode = "ta" | "en" | "hi" | "te" | "ka" | "ml";

export type ScriptId = "Tamil" | "Latin" | "Devanagari" | "Telugu" | "Kannada" | "Malayalam";

export interface LanguageConfig {
  code: SupportedLanguageCode;
  /** English label for prompts */
  displayName: string;
  /** Unicode script(s) allowed for story body values (excluding English TTS markers) */
  primaryScript: ScriptId;
  /** Extra scripts allowed (e.g. Latin digits always allowed separately) */
  allowLatinDigitsAndPunctuation: boolean;
  /** Prompt hint for native grammar/style */
  styleHint: string;
}

export const LANGUAGE_CONFIG: Record<SupportedLanguageCode, LanguageConfig> = {
  ta: {
    code: "ta",
    displayName: "Tamil",
    primaryScript: "Tamil",
    allowLatinDigitsAndPunctuation: true,
    styleHint:
      "Tamil script only for Tamil words. SOV order, natural spoken Tamil for children; avoid English loanwords where a Tamil word fits.",
  },
  en: {
    code: "en",
    displayName: "English",
    primaryScript: "Latin",
    allowLatinDigitsAndPunctuation: true,
    styleHint:
      "Standard English, SVO, short clear sentences, warm read-aloud rhythm; age-appropriate vocabulary.",
  },
  hi: {
    code: "hi",
    displayName: "Hindi",
    primaryScript: "Devanagari",
    allowLatinDigitsAndPunctuation: true,
    styleHint:
      "Devanagari only for Hindi. Natural Hindi grammar, postpositions, respectful register suitable for families.",
  },
  te: {
    code: "te",
    displayName: "Telugu",
    primaryScript: "Telugu",
    allowLatinDigitsAndPunctuation: true,
    styleHint:
      "Telugu script only for Telugu. Natural SOV, everyday vocabulary, audiobook-friendly pacing.",
  },
  ka: {
    code: "ka",
    displayName: "Kannada",
    primaryScript: "Kannada",
    allowLatinDigitsAndPunctuation: true,
    styleHint:
      "Kannada script only for Kannada. Natural agglutination and case markers; clear oral storytelling.",
  },
  ml: {
    code: "ml",
    displayName: "Malayalam",
    primaryScript: "Malayalam",
    allowLatinDigitsAndPunctuation: true,
    styleHint:
      "Malayalam script only for Malayalam. Natural rhythm; avoid literal translation from English.",
  },
};

export const SUPPORTED_LANGUAGE_CODES = Object.keys(LANGUAGE_CONFIG) as SupportedLanguageCode[];

export function getLanguageConfig(code: string): LanguageConfig | undefined {
  const normalized = code.trim().toLowerCase() as SupportedLanguageCode;
  return LANGUAGE_CONFIG[normalized];
}

export function assertSupportedLanguage(code: string): SupportedLanguageCode {
  const c = code.trim().toLowerCase() as SupportedLanguageCode;
  if (!LANGUAGE_CONFIG[c]) {
    throw new Error(`Unsupported language code: ${code}. Expected one of: ${SUPPORTED_LANGUAGE_CODES.join(", ")}`);
  }
  return c;
}
