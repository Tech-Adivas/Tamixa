/**
 * Strips TTS stage directions like [Pause 500ms], [Thoughtful tone], [Medium pacing]
 * for display and browser read-aloud. Kept in sync with backend NarrationTextUtils.
 */
const PAUSE_REGEX = /(?:\[|\uFF3B)\s*Pause\s*[\d.]+(?:ms|s)\s*(?:\]|\uFF3D)/gi;

const TONE_REGEX = new RegExp(
  "[\\[\\uFF3B]\\s*(?:Warm\\s*tone|Gentle\\s*tone|Calm(?:\\s*tone)?|Happy\\s*tone|Excited\\s*tone|Playful\\s*tone|Curious\\s*tone|Wonder\\s*tone|Reassuring\\s*tone|Thoughtful\\s*tone|Soft\\s*voice|Whisper(?:ed)?\\s*tone|Emotional\\s*tone|Soft\\s*emotional\\s*tone|Celebration\\s*tone|Storyteller\\s*tone|Slow\\s*pacing|(?:Meduim|Medium)\\s*pacing|Brisk\\s*pacing|Scene\\s*opens\\s*softly|Scene\\s*shifts|A\\s*gentle\\s*moment|A\\s*magical\\s*moment|A\\s*quiet\\s*pause|A\\s*joyful\\s*moment|A\\s*surprise\\s*moment|Closing\\s*tone|Audio\\s*imagination|Joyful\\s*moment|Clear\\s*tone)\\s*[\\]\\uFF3D]",
  "gi",
);

const FALLBACK_REGEX =
  /(?:\[|\uFF3B)[^\]\uFF3D]*(?:tone|voice|pacing|scene|calm|whisper|excited|pause|happy|warm|soft|storyteller|gentle|curious|wonder|reassuring|thoughtful|celebration|audio|closing)[^\]\uFF3D]*(?:\]|\uFF3D)/gi;

export function stripNarrationMarkers(text: string): string {
  const t = text.trim();
  if (!t) return "";
  return t
    .replace(PAUSE_REGEX, " ")
    .replace(TONE_REGEX, "")
    .replace(FALLBACK_REGEX, "")
    .replace(/\bnn\b/gi, "\n\n")
    .replace(/[^\S\n]{2,}/g, " ")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}
