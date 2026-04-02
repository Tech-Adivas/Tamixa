/**
 * Tamixa Web API client.
 * In the browser we use the full backend URL so the Authorization header is sent
 * (Vite proxy can strip it, causing "Unauthorized. Please log in.").
 */
import { API_PATH, DEFAULT_API_BASE_URL } from "../config/api.config";
import { logger } from "./logger";

const API_BASE =
  typeof window !== "undefined"
    ? `${DEFAULT_API_BASE_URL}${API_PATH}`
    : API_PATH;

const CORRELATION_SESSION_KEY = "tamixa_x_correlation_id";

/** Stable per-tab id for log correlation; aligns with backend X-Correlation-Id / X-Request-Id. */
function getOrCreateCorrelationId(): string {
  try {
    if (typeof sessionStorage === "undefined") return crypto.randomUUID();
    let id = sessionStorage.getItem(CORRELATION_SESSION_KEY);
    if (!id) {
      id = crypto.randomUUID();
      sessionStorage.setItem(CORRELATION_SESSION_KEY, id);
    }
    return id;
  } catch {
    return crypto.randomUUID();
  }
}

/** Per-request id plus optional session correlation (browser only). */
function buildTracingHeaders(): Record<string, string> {
  const requestId = crypto.randomUUID();
  if (typeof window === "undefined") {
    return { "X-Request-Id": requestId };
  }
  return {
    "X-Correlation-Id": getOrCreateCorrelationId(),
    "X-Request-Id": requestId,
  };
}

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
    headers: { Authorization: `Bearer ${token}`, ...buildTracingHeaders() },
    credentials: "omit",
  });
  if (!res.ok) throw new Error(`Stream failed: ${res.status}`);
  const blob = await res.blob();
  return URL.createObjectURL(blob);
}

function getStoredToken(): string | null {
  return localStorage.getItem("tamixa_access_token");
}

function getStoredRefreshToken(): string | null {
  return localStorage.getItem("tamixa_refresh_token");
}

function setStoredTokens(access: string, refresh: string, expiresInSeconds?: number) {
  localStorage.setItem("tamixa_access_token", access);
  localStorage.setItem("tamixa_refresh_token", refresh);
  if (expiresInSeconds != null) {
    const expiresAt = Date.now() + expiresInSeconds * 1000;
    localStorage.setItem("tamixa_token_expires_at", String(expiresAt));
  }
}

export function clearStoredTokens() {
  localStorage.removeItem("tamixa_access_token");
  localStorage.removeItem("tamixa_refresh_token");
  localStorage.removeItem("tamixa_token_expires_at");
}

export function getStoredTokenExpiresAt(): number | null {
  const v = localStorage.getItem("tamixa_token_expires_at");
  return v ? Number(v) : null;
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
  nickname?: string | null;
  displayName?: string | null;
  storyArtPersonalizationOptIn?: boolean;
}

export interface LibraryStory {
  id: number;
  title: string | null;
  content: string;
  theme: string;
  category?: string | null;
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
  /** Optional; when blank or omitted, backend uses "Listener". */
  childName?: string;
  childId?: number | null;
  emotionMode?: string | null;
  parentCustomPrompt?: string | null;
  conversationMessages?: string[] | null;
}

export interface StreamUrlResponse {
  streamUrl: string;
  avatarUrl?: string | null;
  avatarVideoUrl?: string | null;
  avatarStatus?: string | null;
  voiceFallback?: boolean;
  wordTimings?: { word: string; startSec: number; endSec: number }[] | null;
  durationSeconds?: number | null;
  narrativeScenes?: unknown[] | null;
  /** Muted supplementary clip; pair with primary story audio (muted looping video on web). */
  hostStoryClipUrl?: string | null;
}

/** Playback manifest (timeline) — scenes with segments for subtitle sync. From GET /stories/{id}/timeline. */
export interface PlaybackManifest {
  storyId: string;
  title: string;
  storySource: string;
  audioUrl: string;
  coverImageUrl?: string | null;
  coverVideoUrl?: string | null;
  scenes: PlaybackScene[];
  totalDurationMs: number;
}

export interface PlaybackScene {
  sceneId: string;
  backgroundHint?: string | null;
  segments: PlaybackSegment[];
}

export interface PlaybackSegment {
  segmentId: string;
  speaker: string;
  text: string;
  audioUrl: string;
  durationMs: number;
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
    ...buildTracingHeaders(),
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
          headers: { "Content-Type": "application/json", ...buildTracingHeaders() },
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
    headers: { "Content-Type": "application/json", ...buildTracingHeaders() },
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
    headers: { "Content-Type": "application/json", ...buildTracingHeaders() },
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

export async function updateStoryArtPersonalizationOptIn(optIn: boolean): Promise<boolean> {
  const res = await fetchWithAuth("/auth/me/story-art-personalization", {
    method: "PATCH",
    body: JSON.stringify({ optIn }),
  });
  if (!res.ok) {
    const err = (await res.json().catch(() => ({}))) as { message?: string };
    throw new Error(err?.message ?? "Failed to update personalization setting");
  }
  const data = (await res.json().catch(() => ({}))) as { optIn?: boolean };
  return data.optIn === true;
}

/** Permanently delete the authenticated account and all data (GDPR). */
export async function deleteAccount(): Promise<void> {
  const res = await fetchWithAuth("/auth/account", { method: "DELETE" });
  if (!res.ok) {
    const err = (await res.json().catch(() => ({}))) as { message?: string };
    throw new Error(err?.message ?? "Account deletion failed");
  }
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
export async function getLibraryStories(language = "ta"): Promise<LibraryStory[]> {
  const res = await fetchWithAuth(`/stories/library?language=${encodeURIComponent(language)}&size=50`);
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
 * Fetch signed stream URL and optional avatar URLs for a story.
 * Pass voiceProfile for multi-voice (e.g. "calm"). storySource ensures curated vs generated use the right endpoint.
 * playbackMode: default | my_voice | avatar. Only "avatar" triggers avatar video. "my_voice" = audio only.
 * Returns full response (streamUrl, avatarUrl, avatarVideoUrl) or null on 404. Throws on 402 (UpgradeRequired).
 */
export async function getStreamUrl(
  storyId: number,
  language = "ta",
  voiceProfile?: string | null,
  storySource?: string | null,
  playbackMode?: string | null
): Promise<StreamUrlResponse | null> {
  const params = new URLSearchParams({ language });
  if (voiceProfile && voiceProfile !== "default") params.set("voiceProfile", voiceProfile);
  if (playbackMode && ["default", "my_voice", "avatar"].includes(playbackMode)) params.set("playbackMode", playbackMode);
  const qs = params.toString();
  const suffix = qs ? `?${qs}` : "";
  const base = "/stories";
  const path =
    storySource === "library"
      ? `${base}/library/${storyId}/stream-url${suffix}`
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
  if (!data?.streamUrl) return null;
  return data;
}

/**
 * Fetch playback manifest (timeline) with scenes and segments for subtitle sync.
 * Use for karaoke-style playback; each segment can have its own audioUrl when per-segment TTS is enabled.
 */
export async function getTimeline(
  storyId: number,
  language = "ta",
  storySource?: string | null,
  voiceProfile?: string | null
): Promise<PlaybackManifest | null> {
  const params = new URLSearchParams({ language });
  if (storySource) params.set("storySource", storySource);
  if (voiceProfile && voiceProfile !== "default") params.set("voiceProfile", voiceProfile);
  const qs = params.toString();
  const res = await fetchWithAuth(`/stories/${storyId}/timeline${qs ? `?${qs}` : ""}`);
  if (!res.ok) return null;
  return res.json();
}

export async function regenerateStoryCover(storyId: number): Promise<Story> {
  const res = await fetchWithAuth(`/stories/${storyId}/regenerate-cover`, { method: "POST" });
  if (!res.ok) {
    const err = await res.json().catch(() => ({})) as { message?: string };
    throw new Error(err?.message ?? "Regenerate cover failed");
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

/** Referral code validation response. */
export interface ReferralCodeValidateResponse {
  valid: boolean;
  shortcode?: string | null;
  shopName?: string | null;
  offerPercent?: number | null;
}

/** Validate a referral code. Returns discount info if valid and not expired. */
export async function validateReferralCode(code: string): Promise<ReferralCodeValidateResponse> {
  const res = await fetchWithAuth(
    `/subscription/referral-code/validate?code=${encodeURIComponent(code.trim().toUpperCase())}`
  );
  if (!res.ok) return { valid: false };
  return res.json();
}

/** Upgrade response with Stripe checkout URL. */
export interface UpgradeResponse {
  checkoutUrl: string;
}

/** Create checkout session for subscription upgrade. Pass referralCode to apply discount. Returns URL to redirect to. */
export async function createCheckoutSession(options?: {
  successUrl?: string | null;
  cancelUrl?: string | null;
  referralCode?: string | null;
}): Promise<string | null> {
  const res = await fetchWithAuth("/subscription/upgrade", {
    method: "POST",
    body: JSON.stringify({
      successUrl: options?.successUrl ?? null,
      cancelUrl: options?.cancelUrl ?? null,
      referralCode: options?.referralCode?.trim() || null,
    }),
  });
  if (!res.ok) {
    const err = (await res.json().catch(() => ({}))) as { message?: string };
    throw new Error(err?.message ?? "Failed to start checkout");
  }
  const data = (await res.json()) as UpgradeResponse;
  return data?.checkoutUrl ?? null;
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

// Legacy compatibility: old callers ask for "magic link" but backend now supports passwordless code flow only.
export async function requestMagicLink(email: string): Promise<void> {
  const res = await fetch(`${API_BASE}/auth/passwordless`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...buildTracingHeaders() },
    body: JSON.stringify({ email }),
  });
  if (!res.ok) throw new Error("Failed to send code");
  const data = (await res.json()) as { sent?: boolean };
  if (!data.sent) throw new Error("Failed to send code");
}

// Passwordless email code (Option A: works for new and existing users)
export async function requestPasswordlessCode(email: string): Promise<boolean> {
  const res = await fetch(`${API_BASE}/auth/passwordless`, {
    method: "POST",
    headers: { "Content-Type": "application/json", ...buildTracingHeaders() },
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
    headers: { "Content-Type": "application/json", ...buildTracingHeaders() },
    body: JSON.stringify({ email, code, acceptedTerms, acceptedPrivacy, acceptedParentalAttestation }),
  });
  if (!res.ok) {
    const err = (await res.json().catch(() => ({}))) as { message?: string };
    throw new Error(err?.message ?? "Invalid code or consent required");
  }
  return res.json();
}

export async function uploadVoiceProfile(
  file: File,
  consentFile?: File | null,
  userConsent?: boolean
): Promise<VoiceProfile> {
  const formData = new FormData();
  formData.append("file", file);
  if (consentFile && consentFile.size > 0) {
    formData.append("consentFile", consentFile);
  }
  if (userConsent) {
    formData.append("userConsent", "true");
  }
  const token = getStoredToken();
  const headers: HeadersInit = { ...buildTracingHeaders() };
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

/** Matches backend [com.tamixa.api.dto.VoiceCloningJobDto] JSON. */
export interface VoiceCloningJob {
  id: number;
  parentId: number;
  audioStoragePath: string;
  audioFileSizeBytes: number;
  voiceName: string;
  elevenLabsVoiceId?: string | null;
  status: string;
  errorMessage?: string | null;
  createdAt: string;
  completedAt?: string | null;
}

/** Matches backend [com.tamixa.api.dto.VoiceTierDto] JSON. */
export interface VoiceTier {
  id: number;
  name: string;
  priceMonthly: number;
  priceYearly: number;
  maxChildren: number;
  maxVoices: number;
  maxAvatarVideos: number;
  maxSoundscapes: number;
  allowsVoiceCloning: boolean;
  allowsAvatarVideo: boolean;
  allowsSoundscapes: boolean;
  allowsFamilySharing: boolean;
  analyticsEnabled: boolean;
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

export const authStorage = {
  getToken: getStoredToken,
  getRefreshToken: getStoredRefreshToken,
  setTokens: setStoredTokens,
  clearTokens: clearStoredTokens,
};
