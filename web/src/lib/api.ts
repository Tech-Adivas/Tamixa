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

/**
 * Base URL for `/api/v1` requests. Matches `fetchWithAuth` (full backend origin in the browser).
 * Use for calls that bypass `fetchWithAuth` (for example proactive refresh on the deployed site).
 */
export function getApiV1Base(): string {
  return API_BASE;
}

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
 * Resolve library / interactive-segment audio URLs the same way as mobile `ApiConfig.resolveAudioUrl`:
 * `stories/…` → `{origin}/audio/stories/…`, `/api/…` paths → `{origin}/api/…`, localhost/127/10.0.2.2 rewritten to current API origin.
 */
export function resolveLibraryAudioUrl(url: string | null | undefined): string | null {
  if (!url?.trim()) return null;
  const base = getApiOrigin().replace(/\/$/, "");
  if (!base) return null;
  const audioFileUrl = url.trim();
  if (audioFileUrl.startsWith("http://") || audioFileUrl.startsWith("https://")) {
    if (
      audioFileUrl.includes("localhost") ||
      audioFileUrl.includes("127.0.0.1") ||
      audioFileUrl.includes("10.0.2.2")
    ) {
      return audioFileUrl
        .replace(/^https?:\/\/localhost(?::\d+)?/i, base)
        .replace(/^https?:\/\/127\.0\.0\.1(?::\d+)?/i, base)
        .replace(/^https?:\/\/10\.0\.2\.2(?::\d+)?/i, base);
    }
    return audioFileUrl;
  }
  if (audioFileUrl.startsWith("/audio/")) return base + audioFileUrl;
  if (audioFileUrl.startsWith("/")) return base + audioFileUrl;
  if (audioFileUrl.startsWith("stories/")) return `${base}/audio/${audioFileUrl}`;
  return `${base}/${audioFileUrl}`;
}

/** Dev backend only: publish Digital Survival Flyway seeds for parent library E2E. No auth (permitAll on /api/v1/dev/ in dev profile). */
export async function prepareDigitalSurvivalDevE2eSeed(): Promise<{ ok: boolean; message: string }> {
  const origin = getApiOrigin();
  const url = `${origin}/api/v1/dev/digital-survival/prepare-e2e-seed`;
  try {
    const res = await fetch(url, { method: "POST", credentials: "omit" });
    let parsed: { message?: string } = {};
    const text = await res.text();
    try {
      parsed = JSON.parse(text) as { message?: string };
    } catch {
      parsed = { message: text.slice(0, 200) };
    }
    if (!res.ok) {
      return { ok: false, message: parsed.message?.trim() || `HTTP ${res.status}` };
    }
    return { ok: true, message: parsed.message?.trim() || "OK" };
  } catch (e) {
    return { ok: false, message: e instanceof Error ? e.message : "Request failed" };
  }
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

/** Backend JSON error body from GlobalExceptionHandler and security filters. */
export interface ApiErrorBody {
  message?: string;
  code?: string;
  status?: number;
}

/**
 * Failed API response with optional stable `code` (e.g. UNKNOWN_GENERATION_TOPIC).
 */
export class ApiClientError extends Error {
  readonly code?: string;
  readonly httpStatus: number;

  constructor(message: string, httpStatus: number, code?: string) {
    super(message);
    this.name = "ApiClientError";
    this.httpStatus = httpStatus;
    this.code = code;
  }
}

/**
 * Aligns with backend `com.tamixa.api.exception.ApiErrorCodes` for `POST /stories/generate`.
 * Error JSON may include `code` plus `message` (see `docs/api/PARENT_STORY_API_ERRORS.md`).
 */
export const STORY_GENERATE_ERROR_CODES = {
  UNKNOWN_GENERATION_TOPIC: "UNKNOWN_GENERATION_TOPIC",
  THEME_OR_TOPIC_REQUIRED: "THEME_OR_TOPIC_REQUIRED",
  GENERATION_LANGUAGE_NOT_SUPPORTED: "GENERATION_LANGUAGE_NOT_SUPPORTED",
} as const;

export async function parseApiClientError(res: Response, fallbackMessage: string): Promise<ApiClientError> {
  const raw = (await res.json().catch(() => ({}))) as ApiErrorBody;
  const msg =
    typeof raw.message === "string" && raw.message.trim() ? raw.message.trim() : fallbackMessage;
  const code =
    typeof raw.code === "string" && raw.code.trim() ? raw.code.trim() : undefined;
  return new ApiClientError(msg, res.status, code);
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

/** GET /profile — child row (parent boot payload). */
export interface ProfileChildDto {
  id: number;
  name: string;
  dateOfBirth: string;
  languagePreference?: string | null;
}

export interface ProfileResponse {
  parent: { id: number; email: string; nickname?: string | null; displayName?: string | null };
  children: ProfileChildDto[];
  subscriptionPlan?: string | null;
}

/** GET /edu/life-skill-choices/counters — soft practice pillars (not grades). */
export interface LifeSkillCountersResponse {
  childId: number;
  wisdom: number;
  social: number;
  money: number;
  balance: number;
  copyForParents?: string;
}

export async function getProfile(): Promise<ProfileResponse> {
  const res = await fetchWithAuth("/profile");
  if (!res.ok) {
    throw await parseApiClientError(res, "Failed to load profile");
  }
  return res.json();
}

export async function getLifeSkillCounters(childId: number): Promise<LifeSkillCountersResponse | null> {
  const res = await fetchWithAuth(`/edu/life-skill-choices/counters?childId=${encodeURIComponent(String(childId))}`);
  if (res.status === 400) return null;
  if (!res.ok) {
    throw await parseApiClientError(res, "Failed to load practice signals");
  }
  return res.json();
}

export interface LifeSkillChoiceRequest {
  libraryStoryId: number;
  childId: number;
  segmentId: string;
  choiceId: string;
  skillDeltas?: Record<string, number> | null;
}

/** POST /edu/life-skill-choices — record interactive branch choice (soft stats). */
export async function recordLifeSkillChoice(body: LifeSkillChoiceRequest): Promise<boolean> {
  const res = await fetchWithAuth("/edu/life-skill-choices", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      libraryStoryId: body.libraryStoryId,
      childId: body.childId,
      segmentId: body.segmentId,
      choiceId: body.choiceId,
      skillDeltas: body.skillDeltas ?? undefined,
    }),
  });
  return res.ok;
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
  /** Animated cover (GIF) when generated; played muted. */
  coverVideoUrl?: string | null;
  createdAt: string;
  parentDiscussionPrompts?: string[] | null;
  parentContentNote?: string | null;
  speakAlongPrompt?: string | null;
  interactiveGraph?: unknown;
  postStoryMission?: string | null;
  postStoryResourceUrl?: string | null;
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

/** Parent GET /stories/library returns Spring-style paged JSON (same shape as mobile). */
export interface LibraryStoriesPage {
  content: LibraryStory[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface GenerationTopic {
  id: string;
  theme: string;
  suggestedLearningFocus?: string | null;
  descriptionEn?: string | null;
}

export interface GenerateStoryRequest {
  age: number;
  language?: string;
  /** Required unless generationTopicId is set. */
  theme?: string | null;
  /** Server-defined topic id (Tamil theme resolved server-side). */
  generationTopicId?: string | null;
  /** Optional; when blank or omitted, backend uses "Listener". */
  childName?: string;
  childId?: number | null;
  emotionMode?: string | null;
  parentCustomPrompt?: string | null;
  conversationMessages?: string[] | null;
  /** Backend allowlist: e.g. public_speaking, money_literacy, research_skills, empathy, … */
  learningFocus?: string | null;
}

export interface NarrativeSceneVisualDto {
  sceneIndex: number;
  startProgress: number;
  illustrationUrl?: string | null;
  backgroundHint?: string | null;
}

export interface StreamUrlResponse {
  streamUrl: string;
  avatarUrl?: string | null;
  avatarVideoUrl?: string | null;
  avatarStatus?: string | null;
  voiceFallback?: boolean;
  wordTimings?: { word: string; startSec: number; endSec: number }[] | null;
  durationSeconds?: number | null;
  narrativeScenes?: NarrativeSceneVisualDto[] | null;
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
  displayLabel?: string | null;
}

/** Backend story_voice_preference.story_source: use `generated` (not `mine`) for user-generated tales. */
export function voicePreferenceStorySource(uiStorySource: string): string {
  return uiStorySource === "mine" ? "generated" : uiStorySource;
}

export interface VoicePreferenceResponse {
  voiceProfile: string;
  playbackMode: string;
}

export async function getVoicePreference(
  storyId: number,
  storySource: string
): Promise<VoicePreferenceResponse> {
  const src = encodeURIComponent((storySource || "library").slice(0, 20));
  const res = await fetchWithAuth(`/stories/${storyId}/voice-preference?storySource=${src}`);
  if (!res.ok) return { voiceProfile: "default", playbackMode: "default" };
  return res.json() as Promise<VoicePreferenceResponse>;
}

export async function setVoicePreference(
  storyId: number,
  storySource: string,
  voiceProfile: string,
  playbackMode: string
): Promise<boolean> {
  const src = encodeURIComponent((storySource || "library").slice(0, 20));
  const res = await fetchWithAuth(`/stories/${storyId}/voice-preference?storySource=${src}`, {
    method: "PUT",
    body: JSON.stringify({
      voiceProfile: voiceProfile.trim() || "default",
      playbackMode: playbackMode.trim() || "default",
    }),
  });
  return res.ok;
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
    throw await parseApiClientError(res, "Search failed");
  }
  return res.json();
}

/** Distinct library themes/categories for the current language (browse chips). */
export async function getLibraryCategories(language = "ta"): Promise<string[]> {
  const res = await fetchWithAuth(`/stories/library/categories?language=${encodeURIComponent(language)}`);
  if (!res.ok) return [];
  const data = (await res.json()) as { categories?: string[] };
  return Array.isArray(data.categories) ? data.categories : [];
}

// Curated stories (browse library) — API returns PagedResponse<LibraryStoryResponse>, not a bare array.
export async function getGenerationTopics(): Promise<GenerationTopic[]> {
  const res = await fetchWithAuth("/stories/generation-topics");
  if (!res.ok) return [];
  const data = (await res.json()) as GenerationTopic[];
  return Array.isArray(data) ? data : [];
}

export async function getLibraryStories(
  language = "ta",
  page = 0,
  size = 50,
  theme?: string | null,
  learnHub = false
): Promise<LibraryStory[]> {
  const params = new URLSearchParams({
    language,
    page: String(page),
    size: String(size),
  });
  if (learnHub) params.set("learnHub", "true");
  const t = theme?.trim();
  if (t) params.set("theme", t);
  const res = await fetchWithAuth(`/stories/library?${params}`);
  if (!res.ok) {
    throw await parseApiClientError(res, "Failed to load stories");
  }
  const data = (await res.json()) as LibraryStory[] | LibraryStoriesPage;
  if (Array.isArray(data)) {
    return data;
  }
  if (data && Array.isArray(data.content)) {
    return data.content;
  }
  return [];
}

// Generated stories (parent's AI stories)
export async function getMyStories(page = 0, size = 20): Promise<StoriesPage> {
  const res = await fetchWithAuth(`/stories?page=${page}&size=${size}`);
  if (!res.ok) {
    throw await parseApiClientError(res, "Failed to load your stories");
  }
  return res.json();
}

export async function generateStory(request: GenerateStoryRequest): Promise<Story> {
  const res = await fetchWithAuth("/stories/generate", {
    method: "POST",
    body: JSON.stringify({ ...request, language: request.language ?? "ta" }),
  });
  if (!res.ok) {
    throw await parseApiClientError(res, "Story generation failed");
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
    throw await parseApiClientError(res, "Premium voice requires subscription upgrade");
  }
  if (!res.ok) {
    logger.warn("api", "getStreamUrl failed", { storyId, storySource, status: res.status });
    return null;
  }
  const data = (await res.json()) as StreamUrlResponse;
  if (!data?.streamUrl) return null;
  return data;
}

/** POST /stories/stream/analytics — same contract as mobile; best-effort, no throw. */
export interface StreamAnalyticsPayload {
  storyId: number;
  streamStartLatencyMs?: number;
  bufferingEvent?: boolean;
  completed?: boolean;
}

export async function reportStreamAnalytics(payload: StreamAnalyticsPayload): Promise<void> {
  const body: Record<string, unknown> = { storyId: payload.storyId };
  if (payload.streamStartLatencyMs != null && Number.isFinite(payload.streamStartLatencyMs)) {
    body.streamStartLatencyMs = Math.min(60_000, Math.max(0, payload.streamStartLatencyMs));
  }
  if (payload.bufferingEvent === true) body.bufferingEvent = true;
  if (payload.completed === true) body.completed = true;
  try {
    const res = await fetchWithAuth("/stories/stream/analytics", {
      method: "POST",
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      logger.warn("api", "reportStreamAnalytics failed", { storyId: payload.storyId, status: res.status });
    }
  } catch (e) {
    logger.warn("api", "reportStreamAnalytics error", {
      storyId: payload.storyId,
      message: e instanceof Error ? e.message : String(e),
    });
  }
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

/** Warm narration script for device TTS when stream audio is unavailable (mobile parity). */
export async function getNarrationScript(storyId: number, language = "ta"): Promise<string | null> {
  const res = await fetchWithAuth(
    `/stories/${storyId}/narration-script?language=${encodeURIComponent(language)}`
  );
  if (res.status === 404) return null;
  if (!res.ok) return null;
  const data = (await res.json()) as { script?: string | null };
  const s = data.script?.trim();
  return s ? s : null;
}

export async function regenerateStoryCover(storyId: number): Promise<Story> {
  const res = await fetchWithAuth(`/stories/${storyId}/regenerate-cover`, { method: "POST" });
  if (!res.ok) {
    throw await parseApiClientError(res, "Regenerate cover failed");
  }
  return res.json();
}

export async function remixStory(storyId: number, remixInstruction: string): Promise<Story> {
  const res = await fetchWithAuth(`/stories/${storyId}/remix`, {
    method: "POST",
    body: JSON.stringify({ remixInstruction }),
  });
  if (!res.ok) {
    throw await parseApiClientError(res, "Remix failed");
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

/** From `GET /playback/recent?enriched=true` — title, cover, and progress fraction for dashboard resume rows. */
export interface PlaybackPositionEnriched extends PlaybackPosition {
  title: string;
  coverImageUrl?: string | null;
  progress?: number | null;
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

export async function getRecentPlayback(limit?: number, enriched?: false): Promise<PlaybackPosition[]>;
export async function getRecentPlayback(limit: number, enriched: true): Promise<PlaybackPositionEnriched[]>;
export async function getRecentPlayback(limit = 10, enriched = false): Promise<PlaybackPosition[] | PlaybackPositionEnriched[]> {
  const q = enriched ? `limit=${limit}&enriched=true` : `limit=${limit}`;
  const res = await fetchWithAuth(`/playback/recent?${q}`);
  if (!res.ok) {
    logger.warn("api", "getRecentPlayback failed", { limit, enriched, status: res.status });
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
  if (!res.ok) {
    throw await parseApiClientError(res, "Failed to load favorites");
  }
  return res.json();
}

export async function addFavorite(storyId: number, storySource = "generated"): Promise<FavoriteStory> {
  const res = await fetchWithAuth(`/favorites/${storyId}?storySource=${encodeURIComponent(storySource)}`, { method: "POST" });
  if (!res.ok) {
    throw await parseApiClientError(res, "Failed to add favorite");
  }
  return res.json();
}

export async function removeFavorite(storyId: number): Promise<void> {
  const res = await fetchWithAuth(`/favorites/${storyId}`, { method: "DELETE" });
  if (!res.ok) {
    throw await parseApiClientError(res, "Failed to remove favorite");
  }
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

// Parent storytelling avatar (premium) — aligns with mobile AvatarApi
export async function getParentAvatarUrl(): Promise<string | null> {
  const res = await fetchWithAuth("/parents/me/avatar");
  if (res.status === 404) return null;
  if (!res.ok) return null;
  const data = (await res.json()) as { avatarUrl?: string | null };
  return data.avatarUrl?.trim() ? data.avatarUrl : null;
}

export async function uploadParentAvatar(file: File): Promise<string> {
  const token = getStoredToken();
  const formData = new FormData();
  formData.append("file", file);
  const headers: HeadersInit = { ...buildTracingHeaders() };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const res = await fetch(`${API_BASE}/parents/me/avatar`, {
    method: "POST",
    headers,
    body: formData,
  });
  if (res.status === 402) throw new Error("A Tamixa plan is required to upload a storytelling avatar.");
  if (res.status === 413) throw new Error("Image is too large.");
  if (!res.ok) throw new Error("Failed to upload avatar");
  const data = (await res.json()) as { avatarUrl?: string };
  if (!data.avatarUrl?.trim()) throw new Error("Upload succeeded but no URL was returned");
  return data.avatarUrl;
}

export async function deleteParentAvatar(): Promise<void> {
  const res = await fetchWithAuth("/parents/me/avatar", { method: "DELETE" });
  if (res.status === 404) return;
  if (res.status === 402) throw new Error("A Tamixa plan is required to manage your avatar.");
  if (!res.ok) throw new Error("Failed to remove avatar");
}

/** Per-story family voice recording (multipart), same as mobile StoryApi.uploadFamilyVoice. */
export async function uploadFamilyVoice(storyId: number, language: string, file: File): Promise<void> {
  const token = getStoredToken();
  const formData = new FormData();
  formData.append("file", file);
  const headers: HeadersInit = { ...buildTracingHeaders() };
  if (token) headers["Authorization"] = `Bearer ${token}`;
  const q = encodeURIComponent(language || "ta");
  const res = await fetch(`${API_BASE}/stories/${storyId}/upload-family-voice?language=${q}`, {
    method: "POST",
    headers,
    body: formData,
  });
  if (!res.ok) {
    const err = (await res.json().catch(() => ({}))) as { message?: string };
    throw new Error(err?.message ?? "Failed to upload family voice");
  }
}

export async function deleteFamilyVoice(storyId: number, language = "ta"): Promise<void> {
  const q = encodeURIComponent(language);
  const res = await fetchWithAuth(`/stories/${storyId}/delete-family-voice?language=${q}`, {
    method: "DELETE",
  });
  if (!res.ok) {
    const err = (await res.json().catch(() => ({}))) as { message?: string };
    throw new Error(err?.message ?? "Failed to remove family voice");
  }
}

/** Matches backend [com.tamixa.api.dto.VoiceCloningJobDto] JSON. */
export interface VoiceCloningJob {
  id: number;
  parentId: number;
  audioStoragePath: string;
  audioFileSizeBytes: number;
  voiceName: string;
  elevenLabsVoiceId?: string | null;
  fishAudioModelId?: string | null;
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

/** Parent app analytics — non-blocking; no PII in payload. */
export async function trackAppEventLibraryHub(hubKey: string): Promise<void> {
  try {
    const res = await fetchWithAuth("/analytics/app-events", {
      method: "POST",
      body: JSON.stringify({ eventType: "library_hub", hubKey }),
    });
    if (!res.ok) {
      logger.debug("api", "trackAppEventLibraryHub failed", { status: res.status });
    }
  } catch {
    /* ignore */
  }
}

export async function trackStoryInteractiveBranch(
  storyId: number,
  storySource: "library" | "generated",
  language: string
): Promise<void> {
  try {
    const res = await fetchWithAuth("/analytics/story-events", {
      method: "POST",
      body: JSON.stringify({
        storyId,
        storySource,
        language,
        eventType: "interactive_branch",
        playbackPositionSeconds: 0,
      }),
    });
    if (!res.ok) {
      logger.debug("api", "trackStoryInteractiveBranch failed", { status: res.status });
    }
  } catch {
    /* ignore */
  }
}

export const authStorage = {
  getToken: getStoredToken,
  getRefreshToken: getStoredRefreshToken,
  setTokens: setStoredTokens,
  clearTokens: clearStoredTokens,
};
