/**
 * Heuristic safety filter — catches common policy violations in English/Latin and romanized hints.
 * Editorial review remains required for production.
 */

const BLOCK_PATTERNS: RegExp[] = [
  /\b(kill|killed|murder|weapon|gun|knife|blood|gore|horror|ghost|curse|demon|devil)\b/i,
  /\b(war|bomb|terror|politic|election|vote campaign)\b/i,
  /\b(alcohol|beer|wine|whiskey|drunk|drug|cocaine|heroin)\b/i,
  /\b(suicide|self[\s-]?harm|cutting myself)\b/i,
  /\b(sex|romance|kiss|boyfriend|girlfriend|dating)\b/i,
  /\b(bully|humiliat|nude|naked)\b/i,
  /\b(instagram|youtube|facebook|netflix|tiktok)\b/i,
];

// Death-related softer phrases
const SOFT_BLOCK = [
  /\bdied\b/i,
  /\bdeath\b/i,
  /\bkill\b/i,
  /\bscared to death\b/i,
];

export function validateSafetyText(text: string): { ok: true } | { ok: false; errors: string[] } {
  const errors: string[] = [];
  const combined = text.slice(0, 80_000);

  for (const re of BLOCK_PATTERNS) {
    const m = re.exec(combined);
    if (m) errors.push(`safety: Possible policy violation near "${m[0]}".`);
  }
  for (const re of SOFT_BLOCK) {
    const m = re.exec(combined);
    if (m) errors.push(`safety: Disallowed theme (death/violence) near "${m[0]}".`);
  }

  return errors.length ? { ok: false, errors: [...new Set(errors)].slice(0, 12) } : { ok: true };
}
