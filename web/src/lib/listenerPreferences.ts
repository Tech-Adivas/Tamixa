/**
 * Local-only listener preferences (parity with mobile DataStore / UserDefaults).
 * Not synced to the server; per-story API preferences still win when set.
 */

const PREFERRED_VOICE_KEY = "tamixa_preferred_voice_profile";
const ONBOARDING_VOICE_AVATAR_KEY = "tamixa_onboarding_voice_avatar_dismissed";

export type PreferredVoiceProfile = "default" | "calm" | "family";

export function getPreferredVoiceProfile(): PreferredVoiceProfile {
  try {
    const v = localStorage.getItem(PREFERRED_VOICE_KEY)?.trim().toLowerCase();
    if (v === "calm" || v === "family") return v;
  } catch {
    /* ignore */
  }
  return "default";
}

export function setPreferredVoiceProfile(profile: string): void {
  const p = profile === "calm" || profile === "family" ? profile : "default";
  try {
    localStorage.setItem(PREFERRED_VOICE_KEY, p);
  } catch {
    /* ignore */
  }
}

export function getOnboardingVoiceAvatarDismissed(): boolean {
  try {
    return localStorage.getItem(ONBOARDING_VOICE_AVATAR_KEY) === "1";
  } catch {
    return true;
  }
}

export function setOnboardingVoiceAvatarDismissed(): void {
  try {
    localStorage.setItem(ONBOARDING_VOICE_AVATAR_KEY, "1");
  } catch {
    /* ignore */
  }
}
