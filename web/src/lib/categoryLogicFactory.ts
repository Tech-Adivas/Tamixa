/**
 * Maps library metadata + optional graph hint to simulator "mini-game" UI playbooks.
 */

export type SimulatorPlaybookKind =
  | "digitalSafety"
  | "business"
  | "leadership"
  | "communication"
  | "ethics"
  | "default";

const EDU_HINTS: Record<string, SimulatorPlaybookKind> = {
  tech: "digitalSafety",
  technology: "digitalSafety",
  business: "business",
  entrepreneurship: "business",
  leadership: "leadership",
  conflict: "leadership",
  communication: "communication",
  ethics: "ethics",
  research: "ethics",
};

function norm(s: string): string {
  return s.trim().toLowerCase();
}

function playbookFromEduHint(raw: string | null | undefined): SimulatorPlaybookKind | null {
  if (!raw?.trim()) return null;
  const k = norm(raw).replace(/\s+/g, "_");
  const direct = EDU_HINTS[k];
  if (direct) return direct;
  const compact = norm(raw).replace(/[^a-z]/g, "");
  for (const [hint, kind] of Object.entries(EDU_HINTS)) {
    if (compact.includes(hint.replace(/_/g, ""))) return kind;
  }
  return null;
}

/**
 * Resolve which category UI kit to show during interactive playback.
 * @param graphEduCategory optional `eduCategory` on interactive_graph JSON (e.g. "Tech", "Ethics").
 */
export function getSimulatorPlaybook(
  theme: string,
  category: string | null | undefined,
  graphEduCategory?: string | null
): SimulatorPlaybookKind {
  const fromGraph = playbookFromEduHint(graphEduCategory);
  if (fromGraph && fromGraph !== "default") return fromGraph;

  const t = norm(theme);
  const c = category ? norm(category) : "";

  const hay = `${t} ${c}`;

  if (
    hay.includes("digital safety") ||
    hay.includes("digital_safety") ||
    hay.includes("scam") ||
    (hay.includes("simulator") && hay.includes("safety"))
  ) {
    return "digitalSafety";
  }
  if (hay.includes("entrepreneur") || hay.includes("business") || hay.includes("money") || hay.includes("budget")) {
    return "business";
  }
  if (hay.includes("leadership") || hay.includes("conflict") || hay.includes("team")) {
    return "leadership";
  }
  if (hay.includes("communication") || hay.includes("speaking") || hay.includes("presentation")) {
    return "communication";
  }
  if (hay.includes("ethics") || hay.includes("research") || hay.includes("integrity")) {
    return "ethics";
  }

  return "default";
}

export function ethicsShortcutStorageKey(storyId: number): string {
  return `tamixa_sim_ethics_shortcut_${storyId}`;
}

export function setEthicsShortcutPending(storyId: number): void {
  try {
    sessionStorage.setItem(ethicsShortcutStorageKey(storyId), "1");
  } catch {
    /* private mode */
  }
}

export function consumeEthicsShortcutPending(storyId: number): boolean {
  try {
    const k = ethicsShortcutStorageKey(storyId);
    if (sessionStorage.getItem(k) !== "1") return false;
    sessionStorage.removeItem(k);
    return true;
  } catch {
    return false;
  }
}

export function peekEthicsShortcutPending(storyId: number): boolean {
  try {
    return sessionStorage.getItem(ethicsShortcutStorageKey(storyId)) === "1";
  } catch {
    return false;
  }
}

export function clampMeter(n: number, min = 0, max = 100): number {
  if (!Number.isFinite(n)) return min;
  return Math.min(max, Math.max(min, n));
}
