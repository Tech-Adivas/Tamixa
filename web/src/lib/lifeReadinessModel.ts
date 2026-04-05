import type { UserLifeProfile } from "./branchingEduStory";

/**
 * Five family-facing readiness dimensions (spider chart axes).
 * Mobile uses the same axis names and library hub deep links; API soft counters map via
 * `lifeSkillCountersToLifeReadinessSnapshot` in the app (ethics derived from wisdom/social/balance).
 */
export type LifeReadinessAxisId = "tech" | "business" | "leadership" | "ethics" | "communication";

export interface LifeReadinessSnapshot {
  tech: number;
  business: number;
  leadership: number;
  ethics: number;
  communication: number;
}

export const LIFE_READINESS_AXES: {
  id: LifeReadinessAxisId;
  label: string;
  /** Maps stored Life Bar field → radar axis (communication uses emotional balance / regulation as proxy). */
  sourceNote: string;
}[] = [
  { id: "tech", label: "Tech", sourceNote: "Safe, smart technology habits" },
  { id: "business", label: "Business", sourceNote: "Money sense and planning" },
  { id: "leadership", label: "Leadership", sourceNote: "Guiding and working with others" },
  { id: "ethics", label: "Ethics", sourceNote: "Honesty and careful thinking" },
  { id: "communication", label: "Communication", sourceNote: "Calm, clear expression" },
];

/**
 * Map persisted profile to the five radar axes.
 * Leadership ↔ socialCapital; Communication ↔ emotionalBalance (regulation & clarity).
 */
export function profileToLifeReadinessSnapshot(p: UserLifeProfile): LifeReadinessSnapshot {
  return {
    tech: p.digitalWisdom,
    business: p.fiscalMuscle,
    leadership: p.socialCapital,
    ethics: p.integrity,
    communication: p.emotionalBalance,
  };
}

export function valueForAxis(snapshot: LifeReadinessSnapshot, axis: LifeReadinessAxisId): number {
  return snapshot[axis];
}

function clampReadinessStat(value: number): number {
  if (!Number.isFinite(value)) return 0;
  return Math.max(0, Math.min(100, Math.round(value)));
}

/**
 * Map GET /edu/life-skill-choices/counters into the same five axes as the radar.
 * Matches Kotlin `lifeSkillCountersToLifeReadinessSnapshot` (ethics blends wisdom/social/balance).
 */
export function lifeSkillCountersToLifeReadinessSnapshot(c: {
  wisdom: number;
  social: number;
  money: number;
  balance: number;
}): LifeReadinessSnapshot {
  const w = clampReadinessStat(c.wisdom);
  const s = clampReadinessStat(c.social);
  const m = clampReadinessStat(c.money);
  const b = clampReadinessStat(c.balance);
  const ethics = clampReadinessStat(Math.round((w + s + b) / 3));
  return { tech: w, business: m, leadership: s, ethics, communication: b };
}

export function hasAnyLifeSkillCounterSignal(c: {
  wisdom: number;
  social: number;
  money: number;
  balance: number;
} | null | undefined): boolean {
  if (!c) return false;
  return c.wisdom > 0 || c.social > 0 || c.money > 0 || c.balance > 0;
}

/**
 * When both browser (offline life bar) and server counters have practice data, average per axis.
 * Otherwise prefer whichever side has signal (API wins alone when only server has rows).
 */
export function mergeLocalAndApiLifeReadiness(
  localSnapshot: LifeReadinessSnapshot,
  apiSnapshot: LifeReadinessSnapshot | null,
  hasLocalPractice: boolean,
  hasApiPractice: boolean,
): LifeReadinessSnapshot {
  if (hasLocalPractice && hasApiPractice && apiSnapshot) {
    const out: LifeReadinessSnapshot = { ...localSnapshot };
    for (const a of LIFE_READINESS_AXES) {
      out[a.id] = clampReadinessStat(
        Math.round((valueForAxis(localSnapshot, a.id) + valueForAxis(apiSnapshot, a.id)) / 2),
      );
    }
    return out;
  }
  if (hasApiPractice && apiSnapshot && !hasLocalPractice) return { ...apiSnapshot };
  return { ...localSnapshot };
}

export function hasAnyReadinessAxisSignal(snapshot: LifeReadinessSnapshot): boolean {
  return LIFE_READINESS_AXES.some((a) => valueForAxis(snapshot, a.id) > 0);
}

/** Descriptive status only — no scores shown as “marks” in the UI. */
export function readinessStatusLabel(value: number): string {
  const v = Math.max(0, Math.min(100, Math.round(value)));
  if (v <= 24) return "Starting out";
  if (v <= 49) return "Growing";
  if (v <= 69) return "Building steady";
  if (v <= 84) return "Resilient";
  if (v <= 94) return "Strong";
  return "Master";
}

const BADGE_THRESHOLD = 70;

export interface WisdomBadge {
  id: string;
  title: string;
  axis: LifeReadinessAxisId;
  /** Short line for parents */
  familyLine: string;
}

/** Unlocked when that axis crosses the threshold (not shown as a “grade”). */
export function wisdomBadgesUnlocked(snapshot: LifeReadinessSnapshot): WisdomBadge[] {
  const out: WisdomBadge[] = [];
  if (snapshot.tech >= BADGE_THRESHOLD) {
    out.push({
      id: "scam-proof-senior",
      title: "Scam-Proof Senior",
      axis: "tech",
      familyLine: "Your child is building sharp instincts for digital safety.",
    });
  }
  if (snapshot.business >= BADGE_THRESHOLD) {
    out.push({
      id: "kirana-king",
      title: "Kirana King",
      axis: "business",
      familyLine: "Great nose for value, saving, and small-business smarts.",
    });
  }
  if (snapshot.leadership >= BADGE_THRESHOLD) {
    out.push({
      id: "team-captain",
      title: "Team Captain",
      axis: "leadership",
      familyLine: "Stepping up with fairness and follow-through.",
    });
  }
  if (snapshot.ethics >= BADGE_THRESHOLD) {
    out.push({
      id: "truth-seeker",
      title: "Truth Seeker",
      axis: "ethics",
      familyLine: "Chooses honesty and checks facts before acting.",
    });
  }
  if (snapshot.communication >= BADGE_THRESHOLD) {
    out.push({
      id: "clear-voice",
      title: "Clear Voice",
      axis: "communication",
      familyLine: "Growing calm, clear ways to speak up and listen.",
    });
  }
  return out;
}

export function lowestReadinessAxis(snapshot: LifeReadinessSnapshot): LifeReadinessAxisId {
  let min: LifeReadinessAxisId = "tech";
  let minV = Infinity;
  for (const a of LIFE_READINESS_AXES) {
    const v = snapshot[a.id];
    if (v < minV) {
      minV = v;
      min = a.id;
    }
  }
  return min;
}

export interface NextStoryRecommendation {
  axis: LifeReadinessAxisId;
  headline: string;
  familyMessage: string;
  /** Deep link into Library (practice / simulator lane where possible). */
  storiesHref: string;
}

export function recommendationForAxis(axis: LifeReadinessAxisId): NextStoryRecommendation {
  const base = "/stories?tab=library&hub=simulator";
  switch (axis) {
    case "tech":
      return {
        axis,
        headline: "Lean into digital judgment together",
        familyMessage:
          "Pick a Learn · Simulator or digital safety tale next — short episodes with choices help spot scams and kind boundaries online.",
        storiesHref: base,
      };
    case "business":
      return {
        axis,
        headline: "Practice money stories side by side",
        familyMessage:
          "Look for library tales about saving, earning, or family budgets — chat about what you’d do in each scene.",
        storiesHref: "/stories?tab=library&hub=learn",
      };
    case "leadership":
      return {
        axis,
        headline: "Stories about teamwork and fairness",
        familyMessage:
          "Choose adventures where characters resolve conflict or lead with empathy — pause and ask what they’d try next.",
        storiesHref: "/stories?tab=library&hub=browse",
      };
    case "ethics":
      return {
        axis,
        headline: "Honesty and “slow thinking” tales",
        familyMessage:
          "Favor stories about telling the truth, research, and consequences — celebrate small honest wins at home.",
        storiesHref: base,
      };
    case "communication":
      return {
        axis,
        headline: "Calm conversation practice",
        familyMessage:
          "Use stories with dialogue prompts; take turns naming feelings in plain words — no pressure to perform.",
        storiesHref: "/stories?tab=library&hub=browse",
      };
    default:
      return {
        axis: "tech",
        headline: "Keep exploring together",
        familyMessage: "Browse the library and pick something you’d both enjoy tonight.",
        storiesHref: "/stories?tab=library",
      };
  }
}
