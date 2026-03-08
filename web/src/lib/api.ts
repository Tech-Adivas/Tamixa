/**
 * Araro Web API client.
 * In the browser we use the full backend URL so the Authorization header is sent
 * (Vite proxy can strip it, causing "Unauthorized. Please log in.").
 */
import { API_PATH, DEFAULT_API_BASE_URL } from "../../../config/api.config";
import { logger } from "./logger";

const API_BASE =
  typeof window !== "undefined"
    ? `${DEFAULT_API_BASE_URL}${API_PATH}`
    : API_PATH;

/** Origin of the backend API (e.g. http://localhost:8080). Used to detect same-origin stream URLs for blob fetch. */
export function getApiOrigin(): string {
  try {
    return new URL(API_BASE).origin;
  } catch {
    return "";
  }
}

/** Resolve cover or cover-video URL for img/video src (relative path → full API URL). */
export function resolveCoverUrl(url: string | null | undefined): string | null {
  if (!url?.trim()) return null;
  const u = url.trim();
  if (u.startsWith("http://") || u.startsWith("https://")) return u;
  const origin = getApiOrigin();
  return u.startsWith("/") ? `${origin}${u}` : `${origin}/${u}`;
}

/**
 * Fetch a stream URL with auth and return a blob URL so the browser can play it.
 * Use when the stream URL is same-origin (backend returns e.g. http://localhost:8080/audio/...)
 * so the audio element can play without CORS issues. Caller must revoke the returned URL when done.
 */
export async function fetchStreamAsBlobUrl(streamUrl: string): Promise<string> {
  const token = getStoredToken();
  if (!token) throw new Error("Not authenticated");
  const res = await fetch(streamUrl, {
    headers: { Authorization: `Bearer ${token}` },
    credentials: "omit",
  });
  if (!res.ok) throw new Error(`Stream failed: ${res.status}`);
  const blob = await res.blob();
  return URL.createObjectURL(blob);
}

function getStoredToken(): string | null {
  return localStorage.getItem("araro_access_token");
}

function getStoredRefreshToken(): string | null {
  return localStorage.getItem("araro_refresh_token");
}

function setStoredTokens(access: string, refresh: string) {
  localStorage.setItem("araro_access_token", access);
  localStorage.setItem("araro_refresh_token", refresh);
}

export function clearStoredTokens() {
  localStorage.removeItem("araro_access_token");
  localStorage.removeItem("araro_refresh_token");
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresInSeconds: number;
}

export interface CurrentUser {
  email: string;
  role: string;
}

export interface Child {
  id: number;
  name: string;
  dateOfBirth: string;
  languagePreference: string | null;
  interests?: string | null;
  favoriteColor?: string | null;
  favoriteAnimal?: string | null;
  characterTraits?: string | null;
  avatarChoice?: string | null;
}

export interface CuratedStory {
  id: number;
  title: string | null;
  content: string;
  theme: string;
  language: string;
  age: number;
  childName: string;
  wordCount: number;
  readingTimeMinutes: number;
  moral: string | null;
  audioFileUrl: string | null;
  status: string;
  coverImageUrl?: string | null;
  /** Animated cover (GIF or video) from Sora; played muted. */
  coverVideoUrl?: string | null;
  createdAt: string;
}

export interface Story {
  id: number;
  parentId: number;
  childId: number | null;
  content: string;
  theme: string;
  language: string;
  age: number;
  childName: string;
  wordCount: number;
  readingTimeMinutes: number;
  title: string | null;
  moral: string | null;
  status: string;
  audioFileUrl: string | null;
  coverImageUrl?: string | null;
  coverVideoUrl?: string | null;
  createdAt: string;
}

export interface StoriesPage {
  content: Story[];
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface GenerateStoryRequest {
  age: number;
  language?: string;
  theme: string;
  childName: string;
  childId?: number | null;
  emotionMode?: string | null;
  parentCustomPrompt?: string | null;
  conversationMessages?: string[] | null;
}

export interface StreamUrlResponse {
  streamUrl: string;
}

export interface VoiceOption {
  voiceProfile: string;
  isPremium: boolean;
}

export interface VoicesResponse {
  voices: VoiceOption[];
}

async function fetchWithAuth(path: string, options: RequestInit = {}, retry = true): Promise<Response> {
  const token = getStoredToken();
  const url = path.startsWith("http") ? path : `${API_BASE}${path}`;
  const headers: HeadersInit = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  let res = await fetch(url, { ...options, headers });

  if (res.status === 401 && retry) {
    const refresh = getStoredRefreshToken();
    if (refresh) {
      try {
        const refreshRes = await fetch(`${API_BASE}/auth/refresh`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken: refresh }),
        });
        if (refreshRes.ok) {
          const data = (await refreshRes.json()) as AuthResponse;
          setStoredTokens(data.accessToken, data.refreshToken);
          headers["Authorization"] = `Bearer ${data.accessToken}`;
          res = await fetch(url, { ...options, headers });
        } else {
          logger.warn("api", "Token refresh failed, session expired", { status: refreshRes.status });
          clearStoredTokens();
          throw new Error("Session expired. Please log in again.");
        }
      } catch (err) {
        logger.error("api", "Token refresh request failed", err, { path });
        clearStoredTokens();
        throw err instanceof Error ? err : new Error("Session expired. Please log in again.");
      }
    }
  }
  return res;
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) {
    const err = (await res.json().catch(() => ({}))) as { message?: string };
    const msg = err?.message ?? "Login failed";
    logger.error("api", "login failed", new Error(msg), { status: res.status });
    throw new Error(msg);
  }
  return res.json();
}

export async function register(
  email: string,
  password: string,
  acceptedTerms = false,
  acceptedPrivacy = false,
  acceptedParentalAttestation = false
): Promise<AuthResponse> {
  const res = await fetch(`${API_BASE}/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      email,
      password,
      acceptedTerms,
      acceptedPrivacy,
      acceptedParentalAttestation,
    }),
  });
  if (!res.ok) {
    const err = (await res.json().catch(() => ({}))) as { message?: string };
    const msg = err?.message ?? "Registration failed";
    logger.error("api", "register failed", new Error(msg), { status: res.status });
    throw new Error(msg);
  }
  return res.json();
}

export async function getMe(): Promise<CurrentUser> {
  const res = await fetchWithAuth("/auth/me");
  if (!res.ok) throw new Error("Unauthorized");
  return res.json();
}

// Children
export async function getChildren(): Promise<Child[]> {
  const res = await fetchWithAuth("/children");
  if (!res.ok) {
    const err = await res.json().catch(() => ({})) as { message?: string };
    throw new Error(err?.message ?? "Failed to load children");
  }
  return res.json();
}

export async function createChild(data: {
  name: string;
  dateOfBirth: string;
  languagePreference?: string | null;
  interests?: string | null;
  favoriteColor?: string | null;
  favoriteAnimal?: string | null;
  characterTraits?: string | null;
  avatarChoice?: string | null;
  childProfileConsent?: boolean;
}): Promise<Child> {
  const res = await fetchWithAuth("/children", {
    method: "POST",
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({})) as { message?: string };
    throw new Error(err?.message ?? "Failed to create child");
  }
  return res.json();
}

export async function updateChild(id: number, data: {
  languagePreference?: string | null;
  interests?: string | null;
  favoriteColor?: string | null;
  favoriteAnimal?: string | null;
  characterTraits?: string | null;
  avatarChoice?: string | null;
}): Promise<Child> {
  const res = await fetchWithAuth(`/children/${id}`, {
    method: "PATCH",
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({})) as { message?: string };
    throw new Error(err?.message ?? "Failed to update child");
  }
  return res.json();
}

// Search stories (curated + generated) — aligns with mobile Search
export interface SearchStoryItem {
  storyId: number;
  storySource: string;
  title: string | null;
  theme: string;
  language: string;
  age: number;
  childName: string;
  wordCount: number;
  readingTimeMinutes: number;
  coverImageUrl: string | null;
  coverVideoUrl?: string | null;
  status: string;
}

export async function searchStories(
  query: string,
  language = "ta",
  page = 0,
  size = 20
): Promise<{ content: SearchStoryItem[]; totalElements: number }> {
  const res = await fetchWithAuth(
    `/stories/search?q=${encodeURIComponent(query)}&language=${encodeURIComponent(language)}&page=${page}&size=${size}`
  );
  if (!res.ok) {
    const err = await res.json().catch(() => ({})) as { message?: string };
    throw new Error(err?.message ?? "Search failed");
  }
  return res.json();
}

// Curated stories (browse library)
export async function getCuratedStories(language = "ta"): Promise<CuratedStory[]> {
  const res = await fetchWithAuth(`/stories/curated?language=${encodeURIComponent(language)}&size=50`);
  if (!res.ok) {
    const err = await res.json().catch(() => ({})) as { message?: string };
    throw new Error(err?.message ?? "Failed to load stories");
  }
  return res.json();
}

// Generated stories (parent's AI stories)
export async function getMyStories(page = 0, size = 20): Promise<StoriesPage> {
  const res = await fetchWithAuth(`/stories?page=${page}&size=${size}`);
  if (!res.ok) {
    const err = await res.json().catch(() => ({})) as { message?: string };
    throw new Error(err?.message ?? "Failed to load stories");
  }
  return res.json();
}

export async function generateStory(request: GenerateStoryRequest): Promise<Story> {
  const res = await fetchWithAuth("/stories/generate", {
    method: "POST",
    body: JSON.stringify({ ...request, language: request.language ?? "ta" }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({})) as { message?: string };
    throw new Error(err?.message ?? "Story generation failed");
  }
  return res.json();
}

/**
 * Fetch available voices for a story. Used for voice picker.
 */
export async function getAvailableVoices(storyId: number, language = "ta"): Promise<VoiceOption[]> {
  const res = await fetchWithAuth(`/stories/${storyId}/voices?language=${encodeURIComponent(language)}`);
  if (!res.ok) return [];
  const data = (await res.json()) as VoicesResponse;
  return data.voices ?? [];
}

/**
 * Fetch signed stream URL for a story. Links audio to the correct backend stream by source.
 * Pass voiceProfile for multi-voice (e.g. "calm"). storySource ensures curated vs generated use the right endpoint.
 * Returns null on 404. Throws on 402 (UpgradeRequired) with upgrade message.
 */
export async function getStreamUrl(
  storyId: number,
  language = "ta",
  voiceProfile?: string | null,
  storySource?: string | null
): Promise<string | null> {
  const params = new URLSearchParams({ language });
  if (voiceProfile && voiceProfile !== "default") params.set("voiceProfile", voiceProfile);
  const qs = params.toString();
  const suffix = qs ? `?${qs}` : "";
  const base = "/stories";
  const path =
    storySource === "curated"
      ? `${base}/curated/${storyId}/stream-url${suffix}`
      : storySource === "generated" || storySource === "mine"
        ? `${base}/generated/${storyId}/stream-url${suffix}`
        : `${base}/${storyId}/stream-url${suffix}`;
  const res = await fetchWithAuth(path);
  if (res.status === 402) {
    const err = (await res.json().catch(() => ({}))) as { message?: string };
    throw new Error(err?.message ?? "Premium voice requires subscription upgrade");
  }
  if (!res.ok) {
    logger.warn("api", "getStreamUrl failed", { storyId, storySource, status: res.status });
    return null;
  }
  const data = (await res.json()) as StreamUrlResponse;
  return data.streamUrl ?? null;
}

export async function generateStoryCover(storyId: number): Promise<Story> {
  const res = await fetchWithAuth(`/stories/${storyId}/generate-cover`, { method: "POST" });
  if (!res.ok) {
    const err = await res.json().catch(() => ({})) as { message?: string };
    throw new Error(err?.message ?? "Cover generation failed");
  }
  return res.json();
}

export async function remixStory(storyId: number, remixInstruction: string): Promise<Story> {
  const res = await fetchWithAuth(`/stories/${storyId}/remix`, {
    method: "POST",
    body: JSON.stringify({ remixInstruction }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({})) as { message?: string };
    throw new Error(err?.message ?? "Remix failed");
  }
  return res.json();
}

export interface Soundscape {
  id: string;
  name: string;
  description: string;
  url: string;
}

export async function getSoundscapes(): Promise<Soundscape[]> {
  const res = await fetch(`${API_BASE}/soundscapes`);
  if (!res.ok) {
    logger.warn("api", "getSoundscapes failed", { status: res.status });
    return [];
  }
  return res.json();
}

// Achievements
export interface Achievement {
  type: string;
  name: string;
  description: string;
  requiredCompletions: number;
  earned: boolean;
  earnedAt?: string | null;
}

export async function getAchievements(childId: number): Promise<Achievement[]> {
  const res = await fetchWithAuth(`/achievements/children/${childId}`);
  if (!res.ok) {
    logger.warn("api", "getAchievements failed", { childId, status: res.status });
    return [];
  }
  return res.json();
}

export async function getAchievementDefinitions(): Promise<{ type: string; name: string; description: string; requiredCompletions: number }[]> {
  const res = await fetchWithAuth("/achievements/definitions");
  if (!res.ok) {
    logger.warn("api", "getAchievementDefinitions failed", { status: res.status });
    return [];
  }
  return res.json();
}

// Resume playback
export interface PlaybackPosition {
  storyId: number;
  storySource: string;
  positionSeconds: number;
  updatedAt: string;
}

export async function savePlaybackPosition(data: { storyId: number; storySource: string; positionSeconds: number; childId?: number }): Promise<void> {
  const res = await fetchWithAuth("/playback/position", {
    method: "POST",
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Failed to save position");
}

export async function getPlaybackPosition(storyId: number, storySource = "generated"): Promise<number> {
  const res = await fetchWithAuth(`/playback/position?storyId=${storyId}&storySource=${encodeURIComponent(storySource)}`);
  if (!res.ok) {
    logger.warn("api", "getPlaybackPosition failed", { storyId, status: res.status });
    return 0;
  }
  const data = (await res.json()) as { positionSeconds: number };
  return data.positionSeconds ?? 0;
}

export async function getRecentPlayback(limit = 10): Promise<PlaybackPosition[]> {
  const res = await fetchWithAuth(`/playback/recent?limit=${limit}`);
  if (!res.ok) {
    logger.warn("api", "getRecentPlayback failed", { limit, status: res.status });
    return [];
  }
  return res.json();
}

// Recommendations
export interface RecommendedStory {
  storyId: number;
  storySource: string;
  title: string;
  theme: string;
  age: number;
  reason: string;
}

export async function getRecommendedStories(childId?: number, language = "ta", limit = 15): Promise<RecommendedStory[]> {
  const params = new URLSearchParams({ language, limit: String(limit) });
  if (childId != null) params.set("childId", String(childId));
  const res = await fetchWithAuth(`/stories/recommended?${params}`);
  if (!res.ok) {
    logger.warn("api", "getRecommendedStories failed", { childId, status: res.status });
    return [];
  }
  return res.json();
}

// Favorites
export interface FavoriteStory {
  storyId: number;
  storySource: string;
}

export async function getFavorites(): Promise<FavoriteStory[]> {
  const res = await fetchWithAuth("/favorites");
  if (!res.ok) throw new Error("Failed to load favorites");
  return res.json();
}

export async function addFavorite(storyId: number, storySource = "generated"): Promise<FavoriteStory> {
  const res = await fetchWithAuth(`/favorites/${storyId}?storySource=${encodeURIComponent(storySource)}`, { method: "POST" });
  if (!res.ok) throw new Error("Failed to add favorite");
  return res.json();
}

export async function removeFavorite(storyId: number): Promise<void> {
  const res = await fetchWithAuth(`/favorites/${storyId}`, { method: "DELETE" });
  if (!res.ok) throw new Error("Failed to remove favorite");
}

export async function checkFavorite(storyId: number): Promise<{ storyId: number; isFavorite: boolean }> {
  const res = await fetchWithAuth(`/favorites/${storyId}/check`);
  if (!res.ok) {
    logger.warn("api", "checkFavorite failed", { storyId, status: res.status });
    return { storyId, isFavorite: false };
  }
  return res.json();
}

// Subscription & Usage
export interface Subscription {
  plan: string;
  status: string;
  provider: string | null;
  currentPeriodEnd: string | null;
  trialEnd: string | null;
  cancelAtPeriodEnd: boolean;
  maxChildren: number;
  voicePremium: boolean;
  isEntitledToUnlimitedStories: boolean;
}

export interface Usage {
  month: string;
  storiesUsed: number;
  storiesLimit: number | null;
  voiceUsed: number;
  voiceLimit: number;
}

export async function getSubscription(): Promise<Subscription> {
  const res = await fetchWithAuth("/subscription");
  if (!res.ok) throw new Error("Failed to load subscription");
  return res.json();
}

export async function getUsage(): Promise<Usage> {
  const res = await fetchWithAuth("/subscription/usage");
  if (!res.ok) throw new Error("Failed to load usage");
  return res.json();
}

export async function cancelSubscription(): Promise<void> {
  const res = await fetchWithAuth("/subscription/cancel", { method: "POST" });
  if (!res.ok) throw new Error("Failed to cancel");
}

// Voice profiles
export interface VoiceProfile {
  id: number;
  parentId: number;
  createdAt: string;
}

export async function getVoiceProfiles(): Promise<VoiceProfile[]> {
  const res = await fetchWithAuth("/voice");
  if (!res.ok) throw new Error("Failed to load voice profiles");
  return res.json();
}

// Consent
export interface ConsentRecord {
  consentType: string;
  version: number;
  grantedAt: string;
}

export async function getConsentRecords(): Promise<ConsentRecord[]> {
  const res = await fetchWithAuth("/consent");
  if (!res.ok) throw new Error("Failed to load consent");
  return res.json();
}

export async function recordConsent(consentType: string, version = 1): Promise<void> {
  const res = await fetchWithAuth("/consent", {
    method: "POST",
    body: JSON.stringify({ consentType, version }),
  });
  if (!res.ok) throw new Error("Failed to record consent");
}

// Data export
export interface ExportJob {
  id: number;
  status: string;
  requestedAt: string;
  downloadUrl: string | null;
}

export async function requestDataExport(): Promise<ExportJob> {
  const res = await fetchWithAuth("/data-export/request", { method: "POST" });
  if (!res.ok) throw new Error("Failed to request export");
  return res.json();
}

export async function getDataExportJobs(): Promise<ExportJob[]> {
  const res = await fetchWithAuth("/data-export");
  if (!res.ok) throw new Error("Failed to load export jobs");
  return res.json();
}

// Feedback
export async function submitFeedback(data: { storyId?: number; storySource?: string; rating?: number; comment?: string }): Promise<void> {
  const res = await fetchWithAuth("/feedback", {
    method: "POST",
    body: JSON.stringify(data),
  });
  if (!res.ok) throw new Error("Failed to submit feedback");
}

// Listening progress
export interface ListeningProgress {
  periodDays: number;
  storiesStarted: number;
  storiesCompleted: number;
  completionRate: number;
}

export async function getListeningProgress(days = 30): Promise<ListeningProgress> {
  const res = await fetchWithAuth(`/listening-progress?days=${days}`);
  if (!res.ok) throw new Error("Failed to load progress");
  return res.json();
}

// Magic link (sends link + 6-digit code to email)
export async function requestMagicLink(email: string): Promise<void> {
  const res = await fetch(`${API_BASE}/auth/magic-link/request`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email }),
  });
  if (!res.ok) throw new Error("Failed to send magic link");
}

// Passwordless email code (Option A: works for new and existing users)
export async function requestPasswordlessCode(email: string): Promise<boolean> {
  const res = await fetch(`${API_BASE}/auth/passwordless`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email }),
  });
  if (!res.ok) {
    const err = (await res.json().catch(() => ({}))) as { message?: string };
    throw new Error(err?.message ?? `Failed to send code (${res.status})`);
  }
  const data = (await res.json()) as { sent: boolean };
  return data.sent;
}

export async function verifyPasswordlessCode(
  email: string,
  code: string,
  acceptedTerms = false,
  acceptedPrivacy = false,
  acceptedParentalAttestation = false
): Promise<AuthResponse> {
  const res = await fetch(`${API_BASE}/auth/passwordless/verify`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email, code, acceptedTerms, acceptedPrivacy, acceptedParentalAttestation }),
  });
  if (!res.ok) {
    const err = (await res.json().catch(() => ({}))) as { message?: string };
    throw new Error(err?.message ?? "Invalid code or consent required");
  }
  return res.json();
}

export async function uploadVoiceProfile(file: File): Promise<VoiceProfile> {
  const formData = new FormData();
  formData.append("file", file);
  const token = getStoredToken();
  const headers: HeadersInit = {};
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const res = await fetch(`${API_BASE}/voice/upload`, {
    method: "POST",
    headers,
    body: formData,
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({})) as { message?: string };
    throw new Error(err?.message ?? "Failed to upload");
  }
  return res.json();
}

// Voice cloning
export async function getVoiceCloningJobs(): Promise<VoiceCloningJob[]> {
  const res = await fetchWithAuth("/voice-cloning");
  if (!res.ok) throw new Error("Failed to load voice cloning jobs");
  return res.json();
}

export async function createVoiceCloningJob(data: {
  audioStoragePath: string;
  audioFileSizeBytes: number;
  voiceName: string;
}): Promise<VoiceCloningJob> {
  const res = await fetchWithAuth("/voice-cloning", {
    method: "POST",
    body: JSON.stringify(data),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({})) as { message?: string };
    throw new Error(err?.message ?? "Failed to create voice cloning job");
  }
  return res.json();
}

export async function getReadyVoices(): Promise<VoiceCloningJob[]> {
  const res = await fetchWithAuth("/voice-cloning/ready");
  if (!res.ok) throw new Error("Failed to load ready voices");
  return res.json();
}

export async function getVoiceTiers(): Promise<VoiceTier[]> {
  const res = await fetchWithAuth("/voice-cloning/tiers");
  if (!res.ok) throw new Error("Failed to load voice tiers");
  return res.json();
}

// Soundscapes (getSoundscapes is defined earlier in this file)
export async function getSoundscapesByCategory(category: string): Promise<Soundscape[]> {
  const res = await fetch(`${API_BASE}/soundscapes/category/${encodeURIComponent(category)}`);
  if (!res.ok) {
    logger.warn("api", "getSoundscapesByCategory failed", { category, status: res.status });
    return [];
  }
  return res.json();
}

export async function searchSoundscapes(name: string): Promise<Soundscape[]> {
  const res = await fetch(`${API_BASE}/soundscapes/search?name=${encodeURIComponent(name)}`);
  if (!res.ok) {
    logger.warn("api", "searchSoundscapes failed", { name, status: res.status });
    return [];
  }
  return res.json();
}

export async function useSoundscape(soundscapeId: number, storyId?: number): Promise<void> {
  const params = storyId ? `?storyId=${storyId}` : "";
  const res = await fetchWithAuth(`/soundscapes/${soundscapeId}/use${params}`, {
    method: "POST",
  });
  if (!res.ok) throw new Error("Failed to use soundscape");
}

export async function getSoundscapeUsage(): Promise<SoundscapeUsage[]> {
  const res = await fetchWithAuth("/soundscapes/usage");
  if (!res.ok) throw new Error("Failed to load soundscape usage");
  return res.json();
}

export const authStorage = {
  getToken: getStoredToken,
  getRefreshToken: getStoredRefreshToken,
  setTokens: setStoredTokens,
  clearTokens: clearStoredTokens,
};
