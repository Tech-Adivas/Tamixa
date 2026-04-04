/**
 * Parent-facing safeguards — align with docs/EDUSTORY_INDIA_PROBLEMS_AND_FEATURES.md §10.
 * Optional env: VITE_COMPLIANCE_RESOURCE_LINES — pipe-separated lines (e.g. official portals only until legal approves numbers).
 */

export const supportSafetySectionTitle = "Support & safety";

export const mentalHealthDisclaimer =
  "Tamixa stories can support emotional literacy and family conversation — they are not therapy or crisis counselling. If you or someone you know may be in danger, contact local emergency services. For ongoing distress, seek qualified professional help.";

export const eduInteractiveDisclaimer =
  "Interactive library tales may pause for choices. What your child taps is practice in the story — not a school grade or clinical assessment.";

export const helplinePlaceholderNote =
  "Before launch in your region: add verified national or state helpline links and numbers only after legal/compliance sign-off (see internal runbook).";

/** Split with `|` — e.g. `National Cyber Crime Reporting Portal|https://www.cybercrime.gov.in` */
export function getComplianceResourceLinesFromEnv(): string[] {
  const raw = import.meta.env.VITE_COMPLIANCE_RESOURCE_LINES?.trim();
  if (!raw) return [];
  return raw
    .split("|")
    .map((s: string) => s.trim())
    .filter(Boolean);
}
