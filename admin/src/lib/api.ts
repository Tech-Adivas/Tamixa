import type {
  AdminInvoice,
  AdminUser,
  AuditEntry,
  AuthResponse,
  CompletionMetricsDto,
  CurrentUserResponse,
  PagedResponse,
  ParentDetail,
  ParentSummary,
  CreateParentRequest,
  UpdateParentRequest,
  ReferralCode,
  ShortContentDto,
  ShortContentPage,
  CreateShortContentRequest,
  UpdateShortContentRequest,
  GenerateShortContentRequest,
  GenerateShortContentResponse,
  RetentionMetricsDto,
  ChildSummary,
  StorySummary,
  StoryWithIssues,
  LibraryStorySummary,
  CreateLibraryStoryRequest,
  BulkGenerateStoriesRequest,
  BulkGenerateStoriesResponse,
  BulkGenerateJobResponse,
  BulkGenerateJobStatusResponse,
  LibraryStoryStreamUrlResponse,
  PipelineStatusResponse,
  VoiceUploadLog,
  VoiceProfile,
  SubscriptionStatus,
  HealthDto,
  RuntimeConfigDto,
  AiMetricsDto,
  DoraMetricsDto,
  RevenueMetricsDto,
  SubscriptionMetricsDto,
  StoryLengthProfileDto,
  RevenueRow,
  VoiceCloningJob,
  VoiceTier,
  Soundscape,
  SoundscapeUsage,
  AiProjectSummary,
  AiWorkflowSummary,
  AiWorkflowRunSummary,
  ExecuteWorkflowRequest,
  WorkflowRunStarted,
  WorkflowRunDetail,
  PromptVersion,
  PublishPromptRequest,
} from "@/types/api";
import { touchActivity } from "./activity-tracker";
import { broadcastTokenUpdate } from "./session-sync";
import {
  getApiUrlPairForMismatchWarn,
  getBackendApiOriginFromEnv,
} from "./server-runtime-env";

let apiEnvMismatchWarned = false;

function warnIfApiEnvMismatch(): void {
  if (apiEnvMismatchWarned || typeof process === "undefined") return;
  const { apiUrl, nextPublicApiUrl } = getApiUrlPairForMismatchWarn();
  if (!apiUrl || !nextPublicApiUrl || apiUrl === nextPublicApiUrl) return;
  apiEnvMismatchWarned = true;
  console.warn(
    `[admin api] API_URL (${apiUrl}) differs from NEXT_PUBLIC_API_URL (${nextPublicApiUrl}). ` +
      "This can cause split reads across different backends. Set both to the same value."
  );
}

/** API base URL for client requests. Use for cover images, streams, etc. */
export const getApiBaseUrl = (): string => {
  // Browser: use same-origin so request hits our proxy (app/api/[...path]/route.ts)
  // which forwards Authorization header. Direct backend URL via rewrites does NOT.
  if (typeof window !== "undefined") return "";
  warnIfApiEnvMismatch();
  // Server-side (SSR): same resolution as API route proxy (runtime env, not build-inlined).
  return getBackendApiOriginFromEnv() ?? "";
};

const getBaseUrl = getApiBaseUrl;

/** Cookie set at login for edge middleware; used when localStorage was cleared but session cookie remains. */
function readAdminAccessTokenFromCookie(): string | null {
  if (typeof document === "undefined") return null;
  const parts = document.cookie.split(";").map((c) => c.trim());
  for (const p of parts) {
    if (p.startsWith("admin_access_token=")) {
      const raw = p.slice("admin_access_token=".length);
      if (!raw) return null;
      try {
        return decodeURIComponent(raw);
      } catch {
        return raw;
      }
    }
  }
  return null;
}

/** Best-effort JWT exp (seconds since epoch); no signature verify — only to pick the fresher of two access tokens. */
function readJwtExp(accessToken: string): number | null {
  try {
    const parts = accessToken.split(".");
    if (parts.length < 2) return null;
    const payload = parts[1];
    const b64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = b64 + "===".slice((b64.length + 3) % 4);
    const json = JSON.parse(atob(padded)) as { exp?: number };
    return typeof json.exp === "number" ? json.exp : null;
  } catch {
    return null;
  }
}

function getStoredToken(): string | null {
  if (typeof window === "undefined") return null;
  const fromLs = localStorage.getItem("admin_access_token")?.trim();
  const fromCookie = readAdminAccessTokenFromCookie()?.trim();
  if (fromCookie && fromLs && fromCookie !== fromLs) {
    const expLs = readJwtExp(fromLs);
    const expCk = readJwtExp(fromCookie);
    let chosen: string;
    if (expLs != null && expCk != null) {
      chosen = expCk >= expLs ? fromCookie : fromLs;
    } else if (expCk != null) {
      chosen = fromCookie;
    } else if (expLs != null) {
      chosen = fromLs;
    } else {
      chosen = fromCookie;
    }
    try {
      localStorage.setItem("admin_access_token", chosen);
    } catch {
      /* private mode / quota */
    }
    return chosen;
  }
  if (fromLs) return fromLs;
  if (fromCookie) {
    try {
      localStorage.setItem("admin_access_token", fromCookie);
    } catch {
      /* private mode / quota — still return cookie token for this request */
    }
    return fromCookie;
  }
  return null;
}

function getStoredRefreshToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("admin_refresh_token");
}

/** Matches refresh cadence; middleware only checks presence, not JWT exp. */
const ADMIN_ACCESS_COOKIE_MAX_AGE_SEC = 60 * 60 * 24 * 7;

function setStoredTokens(access: string, refresh: string) {
  if (typeof window === "undefined") return;
  localStorage.setItem("admin_access_token", access);
  localStorage.setItem("admin_refresh_token", refresh);
  // Also write a cookie so Next.js edge middleware can check auth without JS
  document.cookie = `admin_access_token=${encodeURIComponent(access)}; path=/; SameSite=Strict; Max-Age=${ADMIN_ACCESS_COOKIE_MAX_AGE_SEC}`;
  broadcastTokenUpdate();
}

/**
 * Edge middleware only sees the cookie; API calls may succeed via Bearer from localStorage alone.
 * Re-sync before each request so client navigations (e.g. after Submit for review) are not redirected to /login.
 */
function syncAdminAccessCookieFromCanonicalToken(): void {
  if (typeof document === "undefined") return;
  const token = getStoredToken()?.trim();
  if (!token) return;
  const cookieTok = readAdminAccessTokenFromCookie()?.trim();
  if (cookieTok === token) return;
  document.cookie = `admin_access_token=${encodeURIComponent(token)}; path=/; SameSite=Strict; Max-Age=${ADMIN_ACCESS_COOKIE_MAX_AGE_SEC}`;
}

/** Before `router.push` to dashboard routes: middleware only checks cookie; keeps cookie aligned with stored access token. */
export function reconcileAdminAuthCookie(): void {
  syncAdminAccessCookieFromCanonicalToken();
}

const LOGOUT_FLAG = "admin_logout";

function clearStoredTokens() {
  if (typeof window === "undefined") return;
  localStorage.removeItem("admin_access_token");
  localStorage.removeItem("admin_refresh_token");
  sessionStorage.removeItem("admin_access_token");
  sessionStorage.removeItem("admin_refresh_token");
  // Clear the middleware cookie too
  document.cookie = "admin_access_token=; path=/; SameSite=Strict; max-age=0";
}

function setLogoutFlag() {
  if (typeof window === "undefined") return;
  sessionStorage.setItem(LOGOUT_FLAG, "1");
}

function consumeLogoutFlag(): boolean {
  if (typeof window === "undefined") return false;
  const had = sessionStorage.getItem(LOGOUT_FLAG);
  if (had) sessionStorage.removeItem(LOGOUT_FLAG);
  return had === "1";
}

export const authStorage = {
  getToken: getStoredToken,
  getRefreshToken: getStoredRefreshToken,
  setTokens: setStoredTokens,
  clearTokens: clearStoredTokens,
  setLogoutFlag,
  consumeLogoutFlag,
};

/** Called when 401 + refresh fails. Resolves true when user re-logs in, false when cancelled. */
export type SessionExpiredHandler = () => Promise<boolean>;

let sessionExpiredHandler: SessionExpiredHandler | null = null;

export function registerSessionExpiredHandler(handler: SessionExpiredHandler): void {
  sessionExpiredHandler = handler;
}

export function unregisterSessionExpiredHandler(): void {
  sessionExpiredHandler = null;
}

/** Mutex: only one refresh runs at a time; concurrent 401s wait for it. */
let refreshPromise: Promise<boolean> | null = null;

/** Proactively refresh tokens. Returns true if refreshed, false otherwise. For use by session manager. */
export async function refreshTokensIfNeeded(): Promise<boolean> {
  if (refreshPromise) return refreshPromise;
  refreshPromise = (async () => {
    try {
      const refresh = getStoredRefreshToken();
      if (!refresh) return false;
      const base = getBaseUrl();
      const url = `${base}/api/v1/auth/refresh`;
      const attempt = async (): Promise<Response> =>
        fetch(url, {
          method: "POST",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken: refresh }),
        });
      let res = await attempt();
      // One short backoff if refresh hit rate limit (backend general bucket could starve /auth/refresh in edge cases).
      if (res.status === 429) {
        await new Promise((r) => setTimeout(r, 1200));
        res = await attempt();
      }
      if (!res.ok) return false;
      const data = (await res.json()) as AuthResponse;
      setStoredTokens(data.accessToken, data.refreshToken);
      return true;
    } catch {
      return false;
    } finally {
      refreshPromise = null;
    }
  })();
  return refreshPromise;
}

/**
 * Merge caller headers with JSON Content-Type (when body present) and Bearer token.
 * Using a plain record avoids bugs where `...(options.headers as Record)` drops `Headers` instances
 * and ensures retries reuse the same body + header shape after refresh.
 */
function buildAuthRequestHeaders(options: RequestInit, bearerToken: string | null): Record<string, string> {
  const out: Record<string, string> = {};
  const ih = options.headers;
  if (ih instanceof Headers) {
    ih.forEach((value, key) => {
      out[key] = value;
    });
  } else if (Array.isArray(ih)) {
    for (const pair of ih) {
      if (pair.length >= 2 && typeof pair[0] === "string") {
        out[pair[0]] = String(pair[1]);
      }
    }
  } else if (ih != null && typeof ih === "object") {
    for (const [k, v] of Object.entries(ih as Record<string, unknown>)) {
      if (v !== undefined && v !== null) out[k] = String(v);
    }
  }
  const hasContentType = Object.keys(out).some((k) => k.toLowerCase() === "content-type");
  if (options.body != null && !hasContentType) {
    out["Content-Type"] = "application/json";
  }
  if (bearerToken) {
    out["Authorization"] = `Bearer ${bearerToken}`;
  }
  return out;
}

type FetchInitWithAuthFlags = RequestInit & { suppressSessionExpiredDialog?: boolean };

function stripAuthFetchFlags(options: RequestInit): { init: RequestInit; suppressSessionExpiredDialog: boolean } {
  const { suppressSessionExpiredDialog, ...rest } = options as FetchInitWithAuthFlags;
  return { init: rest, suppressSessionExpiredDialog: suppressSessionExpiredDialog === true };
}

async function fetchWithAuth(
  path: string,
  options: RequestInit = {},
  retry = true
): Promise<Response> {
  touchActivity();
  const { init, suppressSessionExpiredDialog } = stripAuthFetchFlags(options);
  syncAdminAccessCookieFromCanonicalToken();
  const base = getBaseUrl();
  const url = path.startsWith("http") ? path : `${base}${path}`;
  const doFetch = (token: string | null) =>
    fetch(url, {
      ...init,
      cache: init.cache ?? "no-store",
      credentials: "include",
      headers: buildAuthRequestHeaders(init, token),
    });

  let res: Response;
  let tokenUsed: string | null = getStoredToken();
  try {
    res = await doFetch(tokenUsed);
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    throw new Error(normalizeNetworkError(msg));
  }

  const isAuthSessionProbe = path.includes("/api/v1/auth/me");
  // localStorage can hold a stale/wrong access token while the session cookie (used by middleware) is still valid.
  if (res.status === 401 && retry) {
    const cookieTok = readAdminAccessTokenFromCookie()?.trim() ?? null;
    if (cookieTok && cookieTok !== tokenUsed) {
      try {
        localStorage.setItem("admin_access_token", cookieTok);
      } catch {
        /* private mode / quota */
      }
      tokenUsed = cookieTok;
      res = await doFetch(cookieTok);
    }
  }
  if (res.status === 401 && retry) {
    const ok = await refreshTokensIfNeeded();
    if (ok) {
      const newToken = getStoredToken();
      if (newToken) {
        res = await doFetch(newToken);
      }
    }
    // Still 401 after refresh (or no refresh token): show re-login dialog if handler registered.
    // Background polls (e.g. pipeline banner) must not block the UI — return 401 to the caller instead.
    if (res.status === 401 && typeof window !== "undefined" && !suppressSessionExpiredDialog) {
      if (sessionExpiredHandler) {
        const dialogOk = await sessionExpiredHandler();
        if (dialogOk) {
          const newToken = getStoredToken();
          if (newToken) {
            res = await doFetch(newToken);
          }
        }
      } else if (isAuthSessionProbe) {
        clearStoredTokens();
        const pathname = window.location.pathname;
        if (pathname !== "/login") {
          window.location.href = "/login?expired=1";
        }
      }
    }
  }
  return res;
}

/** Normalize "Failed to fetch" and similar network errors to a user-friendly message. */
function normalizeNetworkError(msg: string): string {
  if (
    msg === "Failed to fetch" ||
    msg === "Load failed" ||
    msg === "NetworkError when attempting to fetch resource"
  ) {
    return "Cannot reach server. Ensure the backend is running and the API URL is correct.";
  }
  return msg;
}

/**
 * Classify provider-side blocked preview responses (moderation/auth/permissions style failures)
 * so UIs can show a specific "Preview blocked" state instead of generic failure text.
 */
export function isPreviewBlockedErrorMessage(message?: string | null): boolean {
  return /preview blocked|blocked:|auth\/permissions|moderation rejected|nsfw|content detected/i.test(message ?? "");
}

/** Throws on non-OK responses; parses error body when available. */
async function fetchJson<T>(path: string, options?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetchWithAuth(path, options);
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    throw new Error(normalizeNetworkError(msg));
  }
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    const body = data as { message?: string; error?: string; detail?: string };
    const msg = body?.message ?? body?.error ?? body?.detail ?? res.statusText ?? "Request failed";
    throw new Error(msg);
  }
  return data as T;
}

export async function login(
  email: string,
  password: string
): Promise<AuthResponse> {
  const base = getBaseUrl();
  const url = `${base}/api/v1/auth/login`;
    try {
      const res = await fetch(url, {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });
    const data = (await res.json().catch(() => ({}))) as AuthResponse & { message?: string };
    if (!res.ok) {
      const msg = (data as { message?: string }).message ?? "Login failed";
      if (res.status === 401) throw new Error(msg || "Invalid email or password");
      if (res.status === 403) throw new Error(msg || "Access denied");
      throw new Error(msg);
    }
    return data as AuthResponse;
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    throw new Error(normalizeNetworkError(msg));
  }
}

/** Error with optional HTTP status (e.g. 429 for rate limit). */
export class ApiError extends Error {
  constructor(
    message: string,
    public readonly status?: number
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export async function getMe(): Promise<CurrentUserResponse> {
  let res: Response;
  try {
    res = await fetchWithAuth("/api/v1/auth/me");
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    throw new Error(normalizeNetworkError(msg));
  }
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    const msg = (body as { message?: string })?.message ?? (res.status === 429 ? "Rate limit exceeded" : "Unauthorized");
    const err = new ApiError(msg, res.status);
    throw err;
  }
  return res.json();
}

/** Prevent duplicate concurrent PUT /stories/{id} from double-clicks/retries in the same tab. */
const updateLibraryStoryInFlight = new Map<number, Promise<LibraryStorySummary>>();

// Admin API (uses fetchJson to throw on non-OK responses)
const admin = {
  getAdminUsers: (page = 0, size = 20) =>
    fetchJson<PagedResponse<AdminUser>>(
      `/api/v1/admin/users?page=${page}&size=${size}`
    ),

  addAdminUser: async (parentId: number, role: string) => {
    const res = await fetchWithAuth("/api/v1/admin/users", {
      method: "POST",
      body: JSON.stringify({ parentId, role }),
    });
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to add admin user");
    }
    return res.json() as Promise<AdminUser>;
  },

  updateAdminRole: async (userId: number, role: string) => {
    const res = await fetchWithAuth(`/api/v1/admin/users/${userId}/role`, {
      method: "PUT",
      body: JSON.stringify({ role }),
    });
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to update role");
    }
    return res.json() as Promise<AdminUser>;
  },

  getParents: (page = 0, size = 20, email?: string, status?: string) =>
    fetchJson<PagedResponse<ParentSummary>>(
      `/api/v1/admin/parents?page=${page}&size=${size}${email ? `&email=${encodeURIComponent(email)}` : ""}${status ? `&status=${encodeURIComponent(status)}` : ""}`
    ),

  getParent: (id: number) => fetchJson<ParentDetail>(`/api/v1/admin/parents/${id}`),

  createParent: async (data: CreateParentRequest): Promise<ParentDetail> => {
    const res = await fetchWithAuth("/api/v1/admin/parents", {
      method: "POST",
      body: JSON.stringify(data),
    });
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to create parent");
    }
    return res.json() as Promise<ParentDetail>;
  },

  updateParent: async (id: number, data: UpdateParentRequest): Promise<ParentDetail> => {
    const res = await fetchWithAuth(`/api/v1/admin/parents/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    });
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to update parent");
    }
    return res.json() as Promise<ParentDetail>;
  },

  deleteParent: async (id: number): Promise<void> => {
    const res = await fetchWithAuth(`/api/v1/admin/parents/${id}`, { method: "DELETE" });
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to delete parent");
    }
  },

  getStory: (id: number) =>
    fetchJson<{ id: number; content: string; theme: string; status: string; [k: string]: unknown }>(
      `/api/v1/admin/stories/${id}/detail`
    ),

  /** Returns blob URL for fetching - use fetch with Authorization for actual streaming. */
  getStoryPreviewAudioUrl: (id: number) =>
    `/api/v1/admin/stories/${id}/preview-audio/generated`,

  getChildren: (page = 0, size = 20, parentId?: number) =>
    fetchJson<PagedResponse<ChildSummary>>(
      `/api/v1/admin/children?page=${page}&size=${size}${parentId != null ? `&parentId=${parentId}` : ""}`
    ),

  getStories: (page = 0, size = 20, status?: string, theme?: string) =>
    fetchJson<PagedResponse<StorySummary>>(
      `/api/v1/admin/stories/summary?page=${page}&size=${size}${status ? `&status=${encodeURIComponent(status)}` : ""}${theme ? `&theme=${encodeURIComponent(theme)}` : ""}`
    ),

  getVoiceLogs: (page = 0, size = 20) =>
    fetchJson<PagedResponse<VoiceUploadLog>>(
      `/api/v1/admin/voice-logs?page=${page}&size=${size}`
    ),

  getVoiceProfilesForParent: (parentId: number) =>
    fetchJson<VoiceProfile[]>(`/api/v1/admin/parents/${parentId}/voice`),

  uploadVoiceForParent: async (parentId: number, file: File, profileName?: string): Promise<VoiceProfile> => {
    const formData = new FormData();
    formData.append("file", file);
    const cleanedName = profileName?.trim();
    if (cleanedName) formData.append("profileName", cleanedName);
    const base = getBaseUrl();
    const token = getStoredToken();
    const url = `${base}/api/v1/admin/parents/${parentId}/voice/upload`;
    const headers: HeadersInit = {};
    if (token) headers["Authorization"] = `Bearer ${token}`;
    const res = await fetch(url, { method: "POST", body: formData, headers });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      const msg = (data as { message?: string })?.message ?? res.statusText ?? "Upload failed";
      throw new Error(msg);
    }
    return data as VoiceProfile;
  },

  getVoiceReferenceAudioBlob: async (parentId: number, voiceProfileId: number): Promise<Blob> => {
    const res = await fetchWithAuth(
      `/api/v1/admin/parents/${parentId}/voice/${voiceProfileId}/reference-audio`
    );
    if (!res.ok) throw new Error(res.status === 404 ? "No reference audio" : "Preview failed");
    return res.blob();
  },

  /** Run ElevenLabs voice cloning job for a profile that has reference audio (no consent). */
  runVoiceCloningJobForProfile: async (
    parentId: number,
    voiceProfileId: number
  ): Promise<{ jobId: number; status: string; message: string; provider: string }> => {
    const res = await fetchWithAuth(
      `/api/v1/admin/parents/${parentId}/voice/${voiceProfileId}/run-job`,
      { method: "POST" }
    );
    const data = (await res.json().catch(() => ({}))) as { message?: string; jobId?: number; status?: string; provider?: string };
    if (!res.ok) throw new Error(data?.message ?? res.statusText ?? "Run job failed");
    return {
      jobId: data.jobId ?? 0,
      status: data.status ?? "PENDING",
      message: data.message ?? "Job started.",
      provider: data.provider ?? "ElevenLabs",
    };
  },

  checkVoiceProviderForProfile: async (
    parentId: number,
    voiceProfileId: number,
    language = "ta"
  ): Promise<{
    ok: boolean;
    provider?: string;
    allowElevenLabsFallback?: boolean;
    hasGoogleVoiceCloningKey?: boolean;
    hasElevenLabsVoiceId?: boolean;
    hasReferenceAudio?: boolean;
    providerAuthFailed?: boolean;
    providerQuotaFailed?: boolean;
    providerStatusMessage?: string;
    elevenLabsApiKeyConfigured?: boolean;
    userApiStatus?: number;
    voiceApiStatus?: number;
    sampleBytes?: number;
    message: string;
  }> => {
    const res = await fetchWithAuth(
      `/api/v1/admin/parents/${parentId}/voice/${voiceProfileId}/provider-check?language=${encodeURIComponent(language)}`
    );
    const data = (await res.json().catch(() => ({}))) as {
      ok?: boolean;
      provider?: string;
      allowElevenLabsFallback?: boolean;
      hasGoogleVoiceCloningKey?: boolean;
      hasElevenLabsVoiceId?: boolean;
      hasReferenceAudio?: boolean;
      providerAuthFailed?: boolean;
      providerQuotaFailed?: boolean;
      providerStatusMessage?: string;
      elevenLabsApiKeyConfigured?: boolean;
      userApiStatus?: number;
      voiceApiStatus?: number;
      sampleBytes?: number;
      message?: string;
    };
    if (!res.ok) throw new Error(data?.message ?? res.statusText ?? "Provider check failed");
    return {
      ok: Boolean(data.ok),
      provider: data.provider,
      allowElevenLabsFallback: data.allowElevenLabsFallback,
      hasGoogleVoiceCloningKey: data.hasGoogleVoiceCloningKey,
      hasElevenLabsVoiceId: data.hasElevenLabsVoiceId,
      hasReferenceAudio: data.hasReferenceAudio,
      providerAuthFailed: data.providerAuthFailed,
      providerQuotaFailed: data.providerQuotaFailed,
      providerStatusMessage: data.providerStatusMessage,
      elevenLabsApiKeyConfigured: data.elevenLabsApiKeyConfigured,
      userApiStatus: data.userApiStatus,
      voiceApiStatus: data.voiceApiStatus,
      sampleBytes: data.sampleBytes,
      message: data.message ?? "Provider check completed.",
    };
  },

  getLatestVoiceCloningJobForProfile: async (
    parentId: number,
    voiceProfileId: number
  ): Promise<VoiceCloningJob | null> => {
    const res = await fetchWithAuth(`/api/v1/admin/parents/${parentId}/voice/${voiceProfileId}/job/latest`);
    if (!res.ok) {
      if (res.status === 404) return null;
      const data = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(data?.message ?? res.statusText ?? "Failed to load voice cloning job");
    }
    const data = (await res.json().catch(() => ({}))) as Partial<VoiceCloningJob>;
    if (!data || typeof data.id !== "number") return null;
    return {
      id: data.id,
      parentId: data.parentId ?? parentId,
      audioStoragePath: data.audioStoragePath ?? "",
      audioFileSizeBytes: data.audioFileSizeBytes ?? 0,
      voiceName: data.voiceName ?? "",
      elevenLabsVoiceId: data.elevenLabsVoiceId ?? null,
      status: data.status ?? "PENDING",
      errorMessage: data.errorMessage ?? null,
      providerAuthFailed: Boolean(data.providerAuthFailed),
      providerQuotaFailed: Boolean(data.providerQuotaFailed),
      providerStatusMessage: data.providerStatusMessage ?? null,
      createdAt: data.createdAt ?? new Date().toISOString(),
      completedAt: data.completedAt ?? null,
    };
  },

  /** Upload consent audio and run Google voice cloning job for a profile that has reference audio. For Tamil cloned voice flow. */
  uploadConsentAndRunVoiceCloning: async (
    parentId: number,
    voiceProfileId: number,
    file: File
  ): Promise<{ jobId: number; status: string; message: string }> => {
    const formData = new FormData();
    formData.append("file", file);
    const base = getBaseUrl();
    const token = getStoredToken();
    const url = `${base}/api/v1/admin/parents/${parentId}/voice/${voiceProfileId}/consent`;
    const headers: HeadersInit = {};
    if (token) headers["Authorization"] = `Bearer ${token}`;
    const res = await fetch(url, { method: "POST", body: formData, headers });
    const data = (await res.json().catch(() => ({}))) as { message?: string; jobId?: number; status?: string };
    if (!res.ok) throw new Error(data?.message ?? res.statusText ?? "Upload consent failed");
    return { jobId: data.jobId ?? 0, status: data.status ?? "PENDING", message: data.message ?? "Job started." };
  },

  deleteVoiceProfileForParent: async (parentId: number, voiceProfileId: number): Promise<void> => {
    const res = await fetchWithAuth(`/api/v1/admin/parents/${parentId}/voice/${voiceProfileId}`, {
      method: "DELETE",
    });
    if (!res.ok) {
      const body = await res.json().catch(() => ({})) as { message?: string };
      throw new Error(body?.message ?? res.statusText ?? "Delete failed");
    }
  },

  getParentAvatarUrl: async (parentId: number): Promise<{ avatarUrl: string } | null> => {
    const res = await fetchWithAuth(`/api/v1/admin/parents/${parentId}/avatar`);
    if (res.status === 404) return null;
    if (!res.ok) {
      const data = await res.json().catch(() => ({})) as { message?: string };
      throw new Error(data?.message ?? res.statusText ?? "Failed to load avatar");
    }
    return res.json() as Promise<{ avatarUrl: string }>;
  },

  uploadAvatarForParent: async (parentId: number, file: File): Promise<{ avatarUrl: string }> => {
    const formData = new FormData();
    formData.append("file", file);
    const base = getBaseUrl();
    const token = getStoredToken();
    const url = `${base}/api/v1/admin/parents/${parentId}/avatar`;
    const headers: HeadersInit = {};
    if (token) headers["Authorization"] = `Bearer ${token}`;
    const res = await fetch(url, { method: "POST", body: formData, headers });
    const data = await res.json().catch(() => ({})) as { avatarUrl?: string; message?: string };
    if (!res.ok) throw new Error(data?.message ?? res.statusText ?? "Upload failed");
    return { avatarUrl: data.avatarUrl! };
  },

  deleteAvatarForParent: async (parentId: number): Promise<void> => {
    const res = await fetchWithAuth(`/api/v1/admin/parents/${parentId}/avatar`, { method: "DELETE" });
    if (!res.ok) {
      const data = await res.json().catch(() => ({})) as { message?: string };
      throw new Error(data?.message ?? res.statusText ?? "Delete failed");
    }
  },

  getSubscriptions: (page = 0, size = 20) =>
    fetchJson<PagedResponse<SubscriptionStatus>>(
      `/api/v1/admin/subscriptions?page=${page}&size=${size}`
    ),

  getReferralCodes: (page = 0, size = 50) =>
    fetchJson<PagedResponse<ReferralCode>>(`/api/v1/admin/referral-codes?page=${page}&size=${size}`),
  getReferralCode: (id: number) => fetchJson<ReferralCode>(`/api/v1/admin/referral-codes/${id}`),
  createReferralCode: async (data: {
    shortcode: string;
    shopName: string;
    offerPercent: number;
    expiresAt: string;
    active?: boolean;
  }) => {
    const res = await fetchWithAuth("/api/v1/admin/referral-codes", {
      method: "POST",
      body: JSON.stringify(data),
    });
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to create referral code");
    }
    return res.json() as Promise<ReferralCode>;
  },
  updateReferralCode: async (
    id: number,
    data: {
      shortcode?: string;
      shopName?: string;
      offerPercent?: number;
      expiresAt?: string;
      active?: boolean;
    }
  ) => {
    const res = await fetchWithAuth(`/api/v1/admin/referral-codes/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    });
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to update referral code");
    }
    return res.json() as Promise<ReferralCode>;
  },
  deleteReferralCode: async (id: number) => {
    const res = await fetchWithAuth(`/api/v1/admin/referral-codes/${id}`, { method: "DELETE" });
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to delete referral code");
    }
  },

  getShortContent: (params?: { type?: string; language?: string; status?: string; page?: number; size?: number }) => {
    const p = params ?? {};
    const search = new URLSearchParams();
    if (p.type) search.set("type", p.type);
    if (p.language) search.set("language", p.language);
    if (p.status) search.set("status", p.status);
    search.set("page", String(p.page ?? 0));
    search.set("size", String(p.size ?? 20));
    return fetchJson<ShortContentPage>(`/api/v1/admin/short-content?${search.toString()}`);
  },
  getShortContentById: (id: number) => fetchJson<ShortContentDto>(`/api/v1/admin/short-content/${id}`),
  createShortContent: async (data: CreateShortContentRequest) => {
    const res = await fetchWithAuth("/api/v1/admin/short-content", {
      method: "POST",
      body: JSON.stringify(data),
    });
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to create short content");
    }
    return res.json() as Promise<ShortContentDto>;
  },
  updateShortContent: async (id: number, data: UpdateShortContentRequest) => {
    const res = await fetchWithAuth(`/api/v1/admin/short-content/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    });
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to update short content");
    }
    return res.json() as Promise<ShortContentDto>;
  },
  deleteShortContent: async (id: number) => {
    const res = await fetchWithAuth(`/api/v1/admin/short-content/${id}`, { method: "DELETE" });
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to delete short content");
    }
  },
  generateShortContent: async (data: GenerateShortContentRequest) => {
    const res = await fetchWithAuth("/api/v1/admin/short-content/generate", {
      method: "POST",
      body: JSON.stringify(data),
    });
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to generate short content");
    }
    return res.json() as Promise<GenerateShortContentResponse>;
  },

  getHealth: () => fetchJson<HealthDto>("/api/v1/admin/health"),

  getRuntimeConfig: () => fetchJson<RuntimeConfigDto>("/api/v1/admin/system/runtime-config"),

  getAiMetrics: () => fetchJson<AiMetricsDto>("/api/v1/admin/metrics/ai"),

  getRevenueMetrics: (month?: string) =>
    fetchJson<RevenueMetricsDto>(
      `/api/v1/admin/metrics/revenue${month ? `?month=${encodeURIComponent(month)}` : ""}`
    ),

  getSubscriptionMetrics: () =>
    fetchJson<SubscriptionMetricsDto>("/api/v1/admin/metrics/subscription"),

  getDoraMetrics: (
    days = 30,
    serviceName?: string,
    environment?: string
  ) =>
    fetchJson<DoraMetricsDto>(
      `/api/v1/admin/metrics/dora?days=${days}${serviceName ? `&serviceName=${encodeURIComponent(serviceName)}` : ""}${environment ? `&environment=${encodeURIComponent(environment)}` : ""}`
    ),

  getRevenueTable: (page = 0, size = 20, search?: string, planFilter?: string) =>
    fetchJson<PagedResponse<RevenueRow>>(
      `/api/v1/admin/metrics/revenue/table?page=${page}&size=${size}${search ? `&search=${encodeURIComponent(search)}` : ""}${planFilter ? `&plan=${encodeURIComponent(planFilter)}` : ""}`
    ),

  getKafkaEvents: (page = 0, size = 20) =>
    fetchJson<PagedResponse<Record<string, unknown>>>(
      `/api/v1/admin/kafka-events?page=${page}&size=${size}`
    ),

  getAuditTrail: (page = 0, size = 20) =>
    fetchJson<PagedResponse<AuditEntry>>(
      `/api/v1/admin/audit-trail?page=${page}&size=${size}`
    ),

  getRetentionMetrics: (days = 30) =>
    fetchJson<RetentionMetricsDto>(
      `/api/v1/admin/analytics/retention?days=${days}`
    ),

  getCompletionMetrics: (days = 30) =>
    fetchJson<CompletionMetricsDto>(
      `/api/v1/admin/analytics/completion?days=${days}`
    ),

  flagStory: async (storyId: number, reason?: string) => {
    const res = await fetchWithAuth(`/api/v1/admin/stories/${storyId}/flag`, {
      method: "POST",
      body: JSON.stringify({ reason: reason ?? null }),
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({})) as { message?: string };
      throw new Error(err?.message ?? "Failed to flag story");
    }
  },

  approveStory: async (storyId: number) => {
    const res = await fetchWithAuth(`/api/v1/admin/stories/${storyId}/approve`, {
      method: "POST",
    });
    if (!res.ok) throw new Error("Failed to approve story");
  },

  rejectStory: async (storyId: number) => {
    const res = await fetchWithAuth(`/api/v1/admin/stories/${storyId}/reject`, {
      method: "POST",
    });
    if (!res.ok) throw new Error("Failed to reject story");
  },

  suspendParent: async (parentId: number) => {
    const res = await fetchWithAuth(`/api/v1/admin/parents/${parentId}/suspend`, {
      method: "POST",
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({})) as { message?: string };
      throw new Error(err?.message ?? "Failed to suspend parent");
    }
  },

  unsuspendParent: async (parentId: number) => {
    const res = await fetchWithAuth(`/api/v1/admin/parents/${parentId}/unsuspend`, {
      method: "POST",
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({})) as { message?: string };
      throw new Error(err?.message ?? "Failed to unsuspend parent");
    }
  },

  getStoryUsagePerDay: (days = 7) =>
    fetchJson<{ date: string; count: number }[]>(
      `/api/v1/admin/metrics/story-usage?days=${days}`
    ),

  getStoryLengthProfile: (days = 7) =>
    fetchJson<StoryLengthProfileDto>(
      `/api/v1/admin/metrics/story-length-profile?days=${days}`
    ),

  getInvoices: (page = 0, size = 20) =>
    fetchJson<PagedResponse<AdminInvoice>>(
      `/api/v1/admin/invoices?page=${page}&size=${size}`
    ),

  refundInvoice: async (invoiceId: number) => {
    const res = await fetchWithAuth(`/api/v1/admin/invoices/${invoiceId}/refund`, {
      method: "POST",
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({})) as { message?: string };
      throw new Error(err?.message ?? "Failed to refund invoice");
    }
  },

  getLibraryStories: (
    page = 0,
    size = 20,
    status?: string,
    narrationApproved?: boolean
  ) => {
    const q = new URLSearchParams({ page: String(page), size: String(size) });
    if (status) q.set("status", status);
    if (narrationApproved === true) q.set("narrationApproved", "true");
    if (narrationApproved === false) q.set("narrationApproved", "false");
    return fetchJson<PagedResponse<LibraryStorySummary>>(
      `/api/v1/admin/stories?${q.toString()}`
    );
  },

  /** PUBLISHED / PROCESSING / READY with narration not yet approved — dedicated queue (same DB filter as admin intent). */
  getStoriesPendingReview: (page = 0, size = 20) =>
    fetchJson<PagedResponse<LibraryStorySummary>>(
      `/api/v1/admin/stories/pending-review?page=${page}&size=${size}`
    ),

  /** Approved stories for Narration tab: trigger TTS after content approval. Uses /narration/approved-stories to avoid path conflicts. */
  getStoriesToSpeech: (page = 0, size = 20) =>
    fetchJson<PagedResponse<LibraryStorySummary>>(
      `/api/v1/admin/narration/approved-stories?page=${page}&size=${size}`
    ),

  getStoriesWithIssues: () =>
    fetchJson<StoryWithIssues[]>(
      "/api/v1/admin/stories/with-issues"
    ),

  getLibraryStory: (id: number, language?: string) =>
    fetchJson<LibraryStorySummary & { content: string }>(
      `/api/v1/admin/stories/${id}${language ? `?language=${encodeURIComponent(language)}` : ""}`
    ),

  updateLibraryStoryTranslation: async (
    id: number,
    language: string,
    data: { title?: string | null; content?: string | null; moral?: string | null }
  ) => {
    const r = await fetchWithAuth(
      `/api/v1/admin/stories/${id}/translations/${encodeURIComponent(language)}`,
      { method: "PUT", body: JSON.stringify(data) }
    );
    if (!r.ok) {
      const err = (await r.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to update translation");
    }
  },

  regenerateLibraryStoryCover: async (id: number, force = false) => {
    const url = `/api/v1/admin/stories/${id}/regenerate-cover${force ? "?force=true" : ""}`;
    const r = await fetchWithAuth(url, {
      method: "POST",
    });
    if (!r.ok) {
      const err = (await r.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Generate cover failed");
    }
    return r.json() as Promise<LibraryStorySummary & { content?: string }>;
  },

  /**
   * Rebuild from saved master: when backend `audio-after-approval` is true (default), translate → rewrite only (no TTS on this call). After Approve, use Narration → Generate audio unless `AUTO_TTS_ON_APPROVE=true`. When audio-after-approval is false, behavior follows legacy full pipeline. 202 Accepted. Save the story first.
   */
  rebuildNarrationPipeline: async (id: number): Promise<{ message: string }> => {
    const r = await fetchWithAuth(`/api/v1/admin/stories/${id}/rebuild-narration-pipeline`, {
      method: "POST",
    });
    if (r.status === 202 || r.ok) {
      return r.json() as Promise<{ message: string }>;
    }
    const err = (await r.json().catch(() => ({}))) as { message?: string };
    throw new Error(err?.message ?? `Rebuild pipeline failed: ${r.status}`);
  },

  getRegenerateWithPromptStatus: async (
    id: number
  ): Promise<{ storyId: number; running: boolean }> => {
    const r = await fetchWithAuth(`/api/v1/admin/stories/${id}/regenerate-with-prompt/status`);
    if (!r.ok) {
      const err = (await r.json().catch(() => ({}))) as { message?: string };
      // Backward compatibility: older backend versions don't have this endpoint yet.
      if (r.status === 404 || /No static resource/i.test(err?.message ?? "")) {
        return { storyId: id, running: false };
      }
      throw new Error(err?.message ?? `Failed to load regenerate status: ${r.status}`);
    }
    const data = (await r.json().catch(() => ({}))) as { storyId?: number; running?: boolean };
    return {
      storyId: data.storyId ?? id,
      running: !!data.running,
    };
  },

  requestRegeneratePromptUnlock: async (id: number): Promise<{ message: string }> => {
    const r = await fetchWithAuth(`/api/v1/admin/stories/${id}/request-regenerate-prompt-unlock`, {
      method: "POST",
    });
    if (!r.ok) {
      const err = (await r.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? `Request failed: ${r.status}`);
    }
    return r.json() as Promise<{ message: string }>;
  },

  approveRegeneratePromptUnlock: async (id: number): Promise<{ message: string }> => {
    const r = await fetchWithAuth(`/api/v1/admin/stories/${id}/approve-regenerate-prompt-unlock`, {
      method: "POST",
    });
    if (!r.ok) {
      const err = (await r.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? `Approve failed: ${r.status}`);
    }
    return r.json() as Promise<{ message: string }>;
  },

  suggestRephrase: async (
    id: number,
    title: string | null,
    content: string
  ): Promise<{ suggestedTitle: string; suggestedContent: string }> => {
    const r = await fetchWithAuth(`/api/v1/admin/stories/${id}/suggest-rephrase`, {
      method: "POST",
      body: JSON.stringify({ title: title ?? "", content }),
    });
    if (!r.ok) {
      const err = (await r.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Rephrase suggestion failed");
    }
    return r.json();
  },

  retryLibraryStory: (id: number) =>
    fetchJson<{ message: string }>(
      `/api/v1/admin/stories/${id}/retry`,
      { method: "POST" }
    ),

  triggerLibraryStoryPipeline: (id: number) =>
    fetchJson<{ message: string }>(
      `/api/v1/admin/stories/${id}/trigger-pipeline`,
      { method: "POST" }
    ),

  regenerateLibraryStoryNarration: (id: number, options?: { languages?: string[] }) =>
    fetchJson<{ message: string }>(
      `/api/v1/admin/stories/${id}/regenerate-narration`,
      {
        method: "POST",
        body: options?.languages?.length ? JSON.stringify({ languages: options.languages }) : undefined,
      }
    ),

  republishLibraryStory: (id: number, options?: { languages?: string[] }) =>
    fetchJson<{ message: string }>(
      `/api/v1/admin/stories/${id}/republish`,
      {
        method: "POST",
        body: JSON.stringify({
          languages: options?.languages ?? [],
        }),
      }
    ),

  getLibraryStoryPipelineStatus: async (id: number) => {
    const res = await fetchWithAuth(
      `/api/v1/admin/stories/${id}/pipeline-status`
    );
    return res.ok ? (res.json() as Promise<PipelineStatusResponse>) : null;
  },

  /** Batch pipeline status for multiple stories. Reduces N requests to 1 when polling table. */
  getLibraryStoryPipelineStatusBatch: async (ids: number[]) => {
    if (ids.length === 0) return {} as Record<number, PipelineStatusResponse>;
    const query = ids.map((i) => `ids=${i}`).join("&");
    const res = await fetchWithAuth(
      `/api/v1/admin/pipeline/status-batch?${query}`
    );
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? `Status batch failed: ${res.status}`);
    }
    return res.json() as Promise<Record<number, PipelineStatusResponse>>;
  },

  /** Pipeline active status + which stories/langs are processing (for banner). */
  getPipelineActiveNow: () =>
    fetchJson<{ active: boolean; activeStories?: { storyId: number; language: string; ageMinutes?: number }[] }>(
      "/api/v1/admin/pipeline/active-now",
      { suppressSessionExpiredDialog: true } as RequestInit
    ),

  /** Clear stuck pipeline entry (enables Run pipeline) when banner shows no progress for 15+ min. */
  clearStuckPipeline: (storyId: number) =>
    fetchJson<{ cleared: boolean; message: string }>(
      `/api/v1/admin/pipeline/clear-stuck/${storyId}`,
      { method: "POST" }
    ),

  /** Reset narration status for a story so it shows Pending. Use when audio is missing in S3 but status shows Done; then run Regenerate audio. */
  resetNarrationStatus: async (storyId: number) => {
    const res = await fetchWithAuth(
      `/api/v1/admin/stories/${storyId}/reset-narration-status`,
      { method: "POST" }
    );
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Reset failed");
    }
    return res.json() as Promise<{ cleared: number; message: string }>;
  },

  /** Nuclear cleanup: delete all narration audio, legacy audio, S3 objects, clear cache. Use when audio is corrupted/stale. */
  clearAllAudioAndCache: async () => {
    const res = await fetchWithAuth(
      `/api/v1/admin/stories/clear-all-audio-and-cache`,
      { method: "DELETE" }
    );
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Clear failed");
    }
    return res.json() as Promise<Record<string, unknown>>;
  },

  approveLibraryStoryNarration: async (id: number) => {
    const res = await fetchWithAuth(
      `/api/v1/admin/stories/${id}/approve-narration`,
      { method: "POST" }
    );
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to approve narration");
    }
  },

  setRejectMarked: async (id: number, marked: boolean) => {
    const res = await fetchWithAuth(
      `/api/v1/admin/stories/${id}/reject-marked`,
      {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ marked }),
      }
    );
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to set reject marked");
    }
    return res.json() as Promise<{ storyId: number; rejectMarked: boolean }>;
  },

  markReviewedLanguages: async (id: number, languages: string[]) => {
    const res = await fetchWithAuth(
      `/api/v1/admin/stories/${id}/reviewed-languages`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ languages }),
      }
    );
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to mark languages as reviewed");
    }
    return res.json() as Promise<{ storyId: number; languages: string[] }>;
  },

  rejectLibraryStory: async (id: number, notes?: string) => {
    const res = await fetchWithAuth(
      `/api/v1/admin/stories/${id}/review-reject`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(notes?.trim() ? { notes: notes.trim() } : {}),
      }
    );
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to reject story");
    }
  },

  approveLibraryStoryTranslationNarration: async (id: number, language: string) => {
    const res = await fetchWithAuth(
      `/api/v1/admin/stories/${id}/translations/${encodeURIComponent(language)}/approve-narration`,
      { method: "POST" }
    );
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to approve narration for language");
    }
  },

  getLibraryStoryStreamUrl: (
    id: number,
    language: string = "ta",
    voiceProfile?: string | null,
    parentId?: number | null
  ) => {
    const params = new URLSearchParams({ language });
    if (voiceProfile?.trim() && parentId != null) {
      params.set("voiceProfile", voiceProfile.trim());
      params.set("parentId", String(parentId));
    }
    return fetchJson<LibraryStoryStreamUrlResponse>(
      `/api/v1/admin/stories/${id}/stream-url?${params.toString()}`
    );
  },

  /** Delete avatar video for the given story+parent+voice so it can be regenerated. Resolves when already absent (404). */
  deleteLibraryStoryAvatarVideo: async (
    id: number,
    language: string = "ta",
    voiceProfile: string,
    parentId: number
  ): Promise<void> => {
    const params = new URLSearchParams({ language, voiceProfile: voiceProfile.trim(), parentId: String(parentId) });
    const res = await fetchWithAuth(
      `/api/v1/admin/stories/${id}/avatar-video?${params.toString()}`,
      { method: "DELETE" }
    );
    if (res.status === 404) return; // nothing to delete, allow caller to trigger generation
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to delete avatar video");
    }
  },

  /** Build preview-audio URL for admin (fetch with Authorization header for playback). */
  getLibraryStoryPreviewAudioUrl: (
    id: number,
    language: string = "ta",
    voiceProfile?: string | null,
    parentId?: number | null
  ): string => {
    const base = getBaseUrl();
    const params = new URLSearchParams({ language });
    if (voiceProfile?.trim() && parentId != null) {
      params.set("voiceProfile", voiceProfile.trim());
      params.set("parentId", String(parentId));
    }
    return `${base}/api/v1/admin/stories/${id}/preview-audio?${params.toString()}`;
  },

  /** Fetch preview audio (with optional cloned voice params). Uses same base URL and auth as other admin calls. */
  getLibraryStoryPreviewAudioBlob: async (
    id: number,
    language: string = "ta",
    voiceProfile?: string | null,
    parentId?: number | null
  ): Promise<Blob> => {
    const params = new URLSearchParams({ language });
    if (voiceProfile?.trim() && parentId != null) {
      params.set("voiceProfile", voiceProfile.trim());
      params.set("parentId", String(parentId));
    }
    const path = `/api/v1/admin/stories/${id}/preview-audio?${params.toString()}`;
    const res = await fetchWithAuth(path);
    if (!res.ok) {
      const data = await res.json().catch(() => ({}));
      const msg = (data as { message?: string }).message ?? res.statusText ?? "Preview failed";
      throw new Error(msg);
    }
    return res.blob();
  },

  /** Generate TTS preview: direct (content as-is) or with custom prompt. Returns audio blob. */
  getTtsPreviewBlob: async (
    id: number,
    language: string = "ta",
    prompt?: string | null
  ): Promise<Blob> => {
    const res = await fetchWithAuth(`/api/v1/admin/stories/${id}/tts-preview`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ language: language.trim() || "ta", prompt: prompt?.trim() || null }),
    });
    if (!res.ok) {
      const data = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(data?.message ?? "TTS preview failed");
    }
    return res.blob();
  },

  /** Delete stored narration for story+language+voice so next preview regenerates it. For cloned voice pass parentId. */
  deleteLibraryStoryNarrationAudio: async (
    id: number,
    language: string = "ta",
    voiceProfile: string,
    parentId: number | null
  ): Promise<{ deleted: boolean; message?: string }> => {
    const params = new URLSearchParams({ language, voiceProfile: voiceProfile.trim() });
    if (parentId != null) params.set("parentId", String(parentId));
    const res = await fetchWithAuth(
      `/api/v1/admin/stories/${id}/narration-audio?${params.toString()}`,
      { method: "DELETE" }
    );
    const data = (await res.json().catch(() => ({}))) as { deleted?: boolean; message?: string };
    if (!res.ok) throw new Error(data?.message ?? "Delete failed");
    return { deleted: data?.deleted ?? false, message: data?.message };
  },

  updateLibraryStory: async (id: number, data: CreateLibraryStoryRequest) => {
    const existing = updateLibraryStoryInFlight.get(id);
    if (existing) return existing;
    const run = (async (): Promise<LibraryStorySummary> => {
    /** Save/submit can touch many translation rows; S3 cleanup is async server-side. Cap wait so UI does not hang forever. */
    const timeoutMs = 600_000;
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    let r: Response;
    try {
      r = await fetchWithAuth(`/api/v1/admin/stories/${id}`, {
        method: "PUT",
        body: JSON.stringify(data),
        signal: controller.signal,
      });
    } catch (e) {
      if (controller.signal.aborted) {
        throw new Error(
          `Request timed out after ${timeoutMs / 1000}s. Check that Spring Boot is running and the database is responsive. If a narration pipeline is still running for this story, wait for it to finish (or clear a stuck pipeline banner), then try again. If this persists, check backend logs for the PUT /api/v1/admin/stories/{id} request.`
        );
      }
      throw e instanceof Error ? e : new Error(String(e));
    } finally {
      clearTimeout(timer);
    }
    if (!r.ok) {
      const err = (await r.json().catch(() => ({}))) as {
        message?: string;
        error?: string;
        detail?: string;
      };
      const msg = err?.message ?? err?.error ?? err?.detail;
      const fallback =
        r.status === 401
          ? "Session expired. Please log in again."
          : r.status === 403
            ? "Access denied: your role cannot update library stories. You need MANAGE_STORIES (e.g. Content Manager, Admin, or Super Admin)."
            : r.status === 404
              ? "Story not found"
              : r.status === 409
                ? "Another operation is in progress for this story (often the translation pipeline). Wait for it to finish or use Clear stuck in the pipeline banner, then try again."
                : "Failed to update story";
      throw new Error(msg && msg !== "Invalid request" ? msg : fallback);
    }
    return r.json() as Promise<LibraryStorySummary>;
    })();
    updateLibraryStoryInFlight.set(id, run);
    try {
      return await run;
    } finally {
      updateLibraryStoryInFlight.delete(id);
    }
  },

  /** Regenerate story content with Tamixa conversion prompt; optionally generate for all languages. Returns content/title/moral/category/theme and per-language translations (does not save). */
  regenerateStoryWithPrompt: async (
    id: number,
    content?: string | null,
    generateForAllLanguages = true,
    /** ISO language code for the main rewrite (ta, en, hi, …). Should match the admin edit form; defaults on server if omitted. */
    language?: string | null
  ): Promise<{
    content: string;
    title: string;
    moral: string;
    category?: string;
    theme?: string;
    /** Present when the model returned caregiver resources (may be an empty array to clear discussion prompts). */
    parentDiscussionPrompts?: string[];
    parentContentNote?: string | null;
    speakAlongPrompt?: string | null;
    translations?: Record<string, { content: string; title: string; moral: string }>;
    /** Optional snapshot of converted content before the source-language paraphrase step (for UI diff). */
    paraphraseBefore?: { content: string; title: string; moral: string };
    /** Optional snapshots of converted content before the mandatory paraphrase pass, per target language. */
    translationsParaphraseBefore?: Record<string, { content: string; title: string; moral: string }>;
  }> => {
    const body: { content?: string; generateForAllLanguages?: boolean; language?: string } = {};
    if (content != null && content.trim() !== "") body.content = content.trim();
    if (generateForAllLanguages) body.generateForAllLanguages = true;
    const raw = language?.trim().toLowerCase();
    if (raw) {
      const aliases: Record<string, string> = {
        tamil: "ta",
        english: "en",
        hindi: "hi",
        telugu: "te",
        kannada: "kn",
        malayalam: "ml",
        bengali: "bn",
      };
      body.language = aliases[raw] ?? raw;
    }
    const start = await fetchWithAuth(`/api/v1/admin/stories/${id}/regenerate-with-prompt/async`, {
      method: "POST",
      body: JSON.stringify(body),
    });
    if (start.status === 403) {
      const err = (await start.json().catch(() => ({}))) as { message?: string };
      throw new Error(
        err?.message ?? "Regenerate & sync is not allowed for your role until a Super Admin approves unlock."
      );
    }
    if (!start.ok) {
      const err = (await start.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Regeneration failed");
    }
    const startData = (await start.json().catch(() => ({}))) as { jobId?: string; message?: string };
    const jobId = startData.jobId?.trim();
    if (!jobId) throw new Error(startData.message ?? "Regeneration job did not start.");

    const maxWaitMs = 20 * 60 * 1000;
    const pollEveryMs = 2000;
    const deadline = Date.now() + maxWaitMs;
    let finalResult: unknown = null;

    while (Date.now() < deadline) {
      await new Promise((resolve) => setTimeout(resolve, pollEveryMs));
      const poll = await fetchWithAuth(`/api/v1/admin/stories/${id}/regenerate-with-prompt/jobs/${encodeURIComponent(jobId)}`);
      if (!poll.ok) {
        const err = (await poll.json().catch(() => ({}))) as { message?: string };
        throw new Error(err?.message ?? `Failed to poll regenerate job: ${poll.status}`);
      }
      const pollData = (await poll.json().catch(() => ({}))) as {
        status?: string;
        result?: unknown;
        error?: string;
      };
      const status = (pollData.status ?? "").toUpperCase();
      if (status === "COMPLETED") {
        finalResult = pollData.result ?? null;
        break;
      }
      if (status === "FAILED") {
        throw new Error(pollData.error ?? "Regeneration failed.");
      }
    }
    if (!finalResult || typeof finalResult !== "object") {
      throw new Error("Regeneration is taking longer than expected. Please keep the page open and retry in a moment.");
    }
    const data = finalResult as {
      content?: string;
      title?: string;
      moral?: string;
      category?: string;
      theme?: string;
      parentDiscussionPrompts?: string[] | null;
      parentContentNote?: string | null;
      speakAlongPrompt?: string | null;
      translations?: Record<string, { content?: string; title?: string; moral?: string }>;
      paraphraseBefore?: { content?: string; title?: string; moral?: string };
      translationsParaphraseBefore?: Record<string, { content?: string; title?: string; moral?: string }>;
    };
    const translations: Record<string, { content: string; title: string; moral: string }> = {};
    const translationsParaphraseBefore: Record<string, { content: string; title: string; moral: string }> = {};
    if (data.translations && typeof data.translations === "object") {
      for (const [lang, entry] of Object.entries(data.translations)) {
        if (entry && typeof entry === "object")
          translations[lang] = {
            content: (entry as { content?: string }).content ?? "",
            title: (entry as { title?: string }).title ?? "",
            moral: (entry as { moral?: string }).moral ?? "",
          };
      }
    }
    if (data.translationsParaphraseBefore && typeof data.translationsParaphraseBefore === "object") {
      for (const [lang, entry] of Object.entries(data.translationsParaphraseBefore)) {
        if (entry && typeof entry === "object")
          translationsParaphraseBefore[lang] = {
            content: (entry as { content?: string }).content ?? "",
            title: (entry as { title?: string }).title ?? "",
            moral: (entry as { moral?: string }).moral ?? "",
          };
      }
    }
    const parentDiscussionPrompts =
      Array.isArray(data.parentDiscussionPrompts) && data.parentDiscussionPrompts !== null
        ? data.parentDiscussionPrompts
            .map((p) => (typeof p === "string" ? p.trim().slice(0, 400) : ""))
            .filter((p) => p.length > 0)
            .slice(0, 10)
        : undefined;
    return {
      content: data.content ?? "",
      title: data.title ?? "",
      moral: data.moral ?? "",
      ...(data.category != null ? { category: data.category } : {}),
      ...(data.theme != null ? { theme: data.theme } : {}),
      ...(data.parentDiscussionPrompts !== undefined && data.parentDiscussionPrompts !== null
        ? { parentDiscussionPrompts }
        : {}),
      ...(data.parentContentNote !== undefined
        ? { parentContentNote: (data.parentContentNote ?? "").trim() || null }
        : {}),
      ...(data.speakAlongPrompt !== undefined
        ? { speakAlongPrompt: (data.speakAlongPrompt ?? "").trim().slice(0, 500) || null }
        : {}),
      ...(Object.keys(translations).length > 0 ? { translations } : {}),
      ...(data.paraphraseBefore
        ? {
            paraphraseBefore: {
              content: data.paraphraseBefore.content ?? "",
              title: data.paraphraseBefore.title ?? "",
              moral: data.paraphraseBefore.moral ?? "",
            },
          }
        : {}),
      ...(Object.keys(translationsParaphraseBefore).length > 0
        ? { translationsParaphraseBefore }
        : {}),
    };
  },

  bulkPublish: (ids: number[]) =>
    fetchJson<{ updated: number; ids: number[] }>(
      "/api/v1/admin/stories/bulk-publish",
      { method: "PUT", body: JSON.stringify({ ids }) }
    ),

  bulkUpdateCategory: (ids: number[], theme: string) =>
    fetchJson<{ updated: number; theme: string }>(
      "/api/v1/admin/stories/bulk-category",
      { method: "PUT", body: JSON.stringify({ ids, theme }) }
    ),

  deleteLibraryStory: async (id: number): Promise<void> => {
    const r = await fetchWithAuth(`/api/v1/admin/stories/${id}`, {
      method: "DELETE",
    });
    if (!r.ok && r.status !== 404) {
      const err = (await r.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to delete story");
    }
  },

  bulkDeleteLibraryStories: async (ids: number[]) =>
    fetchJson<{ deleted: number; ids: number[] }>(
      "/api/v1/admin/stories/bulk",
      { method: "DELETE", body: JSON.stringify({ ids }) }
    ),

  /** Super Admin: stories in soft-delete retention (restore via restoreLibraryStory). */
  getSoftDeletedLibraryStories: (page = 0, size = 20) =>
    fetchJson<PagedResponse<LibraryStorySummary>>(
      `/api/v1/admin/stories/soft-deleted?page=${page}&size=${size}`
    ),

  restoreLibraryStory: async (id: number) =>
    fetchJson<{ message: string; id: number }>(`/api/v1/admin/stories/${id}/restore`, {
      method: "POST",
    }),

  createLibraryStory: async (data: CreateLibraryStoryRequest) => {
    const r = await fetchWithAuth("/api/v1/admin/stories", {
      method: "POST",
      body: JSON.stringify(data),
    });
    if (!r.ok) {
      const err = (await r.json().catch(() => ({}))) as {
        message?: string;
        error?: string;
        detail?: string;
      };
      const msg =
        (err?.message ?? err?.error ?? err?.detail ?? r.statusText) || "Failed to create story";
      throw new Error(msg);
    }
    return r.json() as Promise<LibraryStorySummary>;
  },

  bulkGenerateLibraryStories: async (data: BulkGenerateStoriesRequest) => {
    const r = await fetchWithAuth("/api/v1/admin/stories/bulk-generate", {
      method: "POST",
      body: JSON.stringify(data),
    });
    if (!r.ok) {
      if (r.status === 409) {
        const body = (await r.json().catch(() => ({}))) as { message?: string };
        throw new Error(body?.message ?? "A bulk story generation is already in progress.");
      }
      const err = (await r.json().catch(() => ({}))) as { message?: string; error?: string; detail?: string };
      const msg = err?.message ?? err?.error ?? err?.detail ?? "Bulk generation failed";
      throw new Error(msg);
    }
    return r.json() as Promise<BulkGenerateStoriesResponse>;
  },

  /** Start bulk generation in background; returns jobId. Poll getBulkGenerateJobStatus(jobId) for progress. */
  bulkGenerateLibraryStoriesAsync: async (data: BulkGenerateStoriesRequest) => {
    const r = await fetchWithAuth("/api/v1/admin/stories/bulk-generate/async", {
      method: "POST",
      body: JSON.stringify(data),
    });
    if (!r.ok) {
      if (r.status === 409) {
        const body = (await r.json().catch(() => ({}))) as { message?: string; runningJobId?: string };
        throw new Error(body?.message ?? "A bulk story generation is already in progress.");
      }
      const err = (await r.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to start bulk generation");
    }
    return r.json() as Promise<BulkGenerateJobResponse>;
  },

  getBulkGenerateJobStatus: async (jobId: string) => {
    const r = await fetchWithAuth(`/api/v1/admin/stories/bulk-generate/jobs/${encodeURIComponent(jobId)}`, {
      method: "GET",
    });
    if (!r.ok) {
      if (r.status === 404) throw new Error("Job not found");
      const err = (await r.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to get job status");
    }
    return r.json() as Promise<BulkGenerateJobStatusResponse>;
  },

  // Soundscapes
  getSoundscapes: () =>
    fetchJson<Soundscape[]>("/api/v1/admin/soundscapes"),

  createSoundscape: async (data: {
    name: string;
    category: string;
    description?: string | null;
    durationSeconds?: number;
  }) => {
    const r = await fetchWithAuth("/api/v1/admin/soundscapes", {
      method: "POST",
      body: JSON.stringify(data),
    });
    if (!r.ok) {
      const err = (await r.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to create soundscape");
    }
    return r.json() as Promise<Soundscape>;
  },

  useSoundscape: async (soundscapeId: number, storyId?: number) => {
    const params = storyId ? `?storyId=${storyId}` : "";
    const r = await fetchWithAuth(`/api/v1/admin/soundscapes/${soundscapeId}/use${params}`, {
      method: "POST",
    });
    if (!r.ok) {
      const err = (await r.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to use soundscape");
    }
  },

  getSoundscapeUsage: () =>
    fetchJson<SoundscapeUsage[]>("/api/v1/admin/soundscapes/usage"),

  // AI control plane (governance registry + workflow runs)
  getAiControlPlaneProjects: () =>
    fetchJson<AiProjectSummary[]>("/api/v1/admin/ai-control-plane/projects"),

  getAiControlPlaneWorkflows: (projectCode: string) =>
    fetchJson<AiWorkflowSummary[]>(
      `/api/v1/admin/ai-control-plane/projects/${encodeURIComponent(projectCode)}/workflows`
    ),

  getAiControlPlaneWorkflowRuns: (projectCode: string, page = 0, size = 20) =>
    fetchJson<PagedResponse<AiWorkflowRunSummary>>(
      `/api/v1/admin/ai-control-plane/workflow-runs?projectCode=${encodeURIComponent(projectCode)}&page=${page}&size=${size}`
    ),

  executeAiControlPlaneWorkflow: (body: ExecuteWorkflowRequest) =>
    fetchJson<WorkflowRunStarted>("/api/v1/admin/ai-control-plane/workflow-runs/execute", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }),

  getAiControlPlaneWorkflowRun: (runId: string) =>
    fetchJson<WorkflowRunDetail>(`/api/v1/admin/ai-control-plane/workflow-runs/${encodeURIComponent(runId)}`),

  cancelAiControlPlaneWorkflowRun: async (runId: string) => {
    const res = await fetchWithAuth(
      `/api/v1/admin/ai-control-plane/workflow-runs/${encodeURIComponent(runId)}/cancel`,
      { method: "POST" }
    );
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to cancel run");
    }
  },

  retryAiControlPlaneWorkflowRun: (runId: string) =>
    fetchJson<WorkflowRunStarted>(
      `/api/v1/admin/ai-control-plane/workflow-runs/${encodeURIComponent(runId)}/retry`,
      { method: "POST" }
    ),

  resolveAiControlPlanePrompt: (projectCode: string, assetKey: string, version?: number) => {
    const v =
      version != null && !Number.isNaN(version)
        ? `&version=${encodeURIComponent(String(version))}`
        : "";
    return fetchJson<PromptVersion>(
      `/api/v1/admin/ai-control-plane/prompts/resolve?projectCode=${encodeURIComponent(projectCode)}&assetKey=${encodeURIComponent(assetKey)}${v}`
    );
  },

  publishAiControlPlanePrompt: (body: PublishPromptRequest) =>
    fetchJson<PromptVersion>("/api/v1/admin/ai-control-plane/prompts/publish", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    }),

  approveAiControlPlanePromptVersion: async (assetId: string, version: number) => {
    const res = await fetchWithAuth(
      `/api/v1/admin/ai-control-plane/prompts/assets/${encodeURIComponent(assetId)}/versions/${encodeURIComponent(String(version))}/approve`,
      { method: "POST" }
    );
    if (!res.ok) {
      const err = (await res.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to approve version");
    }
  },

  // Voice cloning
  getVoiceCloningJobs: () =>
    fetchJson<VoiceCloningJob[]>("/api/v1/admin/voice-cloning"),

  getVoiceTiers: () =>
    fetchJson<VoiceTier[]>("/api/v1/admin/voice-cloning/tiers"),
};

/** Check if backend is reachable. Use on login page to show connectivity status. */
export async function checkBackendHealth(): Promise<{ ok: boolean; baseUrl: string }> {
  const base = getBaseUrl();
  const url = base ? `${base}/api/v1/health` : "/api/v1/health";
  try {
    const res = await fetch(url, { method: "GET" });
    return { ok: res.ok, baseUrl: base || url };
  } catch {
    return { ok: false, baseUrl: base || url };
  }
}

export const api = { login, getMe, admin };
