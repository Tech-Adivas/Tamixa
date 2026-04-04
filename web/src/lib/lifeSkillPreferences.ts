/** Keep in sync with mobile PreferencesPort key intent (`life_skill_preferred_child_id`). */
export const LIFE_SKILL_CHILD_STORAGE_KEY = "tamixa_life_skill_child_id";

const REFRESH_EVENT = "tamixa-life-skill-refresh";

/** Notify Settings (and other listeners) to refetch practice-signal counters — mirrors mobile `bumpLifeSkillCountersRefresh`. */
export function bumpLifeSkillCountersRefresh(): void {
  if (typeof window === "undefined") return;
  window.dispatchEvent(new CustomEvent(REFRESH_EVENT));
}

export function subscribeLifeSkillCountersRefresh(handler: () => void): () => void {
  if (typeof window === "undefined") return () => {};
  window.addEventListener(REFRESH_EVENT, handler);
  return () => window.removeEventListener(REFRESH_EVENT, handler);
}
