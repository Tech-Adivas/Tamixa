import type {
  AdminInvoice,
  AdminUser,
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
  RevenueMetricsDto,
  SubscriptionMetricsDto,
  RevenueRow,
  VoiceCloningJob,
  VoiceTier,
  Soundscape,
  SoundscapeUsage,
} from "@/types/api";
import { touchActivity } from "./activity-tracker";
import { broadcastTokenUpdate } from "./session-sync";

/** API base URL for client requests. Use for cover images, streams, etc. */
export const getApiBaseUrl = (): string => {
  // Browser: use same-origin so request hits our proxy (app/api/[...path]/route.ts)
  // which forwards Authorization header. Direct backend URL via rewrites does NOT.
  if (typeof window !== "undefined") return "";
  // Server-side (SSR): use env or same-origin for proxy
  const explicit = typeof process !== "undefined" && process.env?.NEXT_PUBLIC_API_URL;
  if (explicit && process.env.NEXT_PUBLIC_API_URL!.trim() !== "") return process.env.NEXT_PUBLIC_API_URL!.trim();
  return "";
};

const getBaseUrl = getApiBaseUrl;

function getStoredToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("admin_access_token");
}

function getStoredRefreshToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem("admin_refresh_token");
}

function setStoredTokens(access: string, refresh: string) {
  if (typeof window === "undefined") return;
  localStorage.setItem("admin_access_token", access);
  localStorage.setItem("admin_refresh_token", refresh);
  broadcastTokenUpdate();
}

const LOGOUT_FLAG = "admin_logout";

function clearStoredTokens() {
  if (typeof window === "undefined") return;
  localStorage.removeItem("admin_access_token");
  localStorage.removeItem("admin_refresh_token");
  sessionStorage.removeItem("admin_access_token");
  sessionStorage.removeItem("admin_refresh_token");
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
      const res = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken: refresh }),
      });
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

async function fetchWithAuth(
  path: string,
  options: RequestInit = {},
  retry = true
): Promise<Response> {
  touchActivity();
  const base = getBaseUrl();
  const token = getStoredToken();
  const url = path.startsWith("http") ? path : `${base}${path}`;
  const headers: HeadersInit = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  let res: Response;
  try {
    res = await fetch(url, { ...options, headers });
  } catch (e) {
    throw e;
  }

  const isAuthSessionProbe = path.includes("/api/v1/auth/me");
  if (res.status === 401 && retry) {
    const ok = await refreshTokensIfNeeded();
    if (ok) {
      const newToken = getStoredToken();
      if (newToken) {
        (headers as Record<string, string>)["Authorization"] = `Bearer ${newToken}`;
        res = await fetch(url, { ...options, headers });
      }
    }
    // Still 401 after refresh (or no refresh token): show re-login dialog if handler registered.
    if (res.status === 401 && typeof window !== "undefined") {
      if (sessionExpiredHandler) {
        const ok = await sessionExpiredHandler();
        if (ok) {
          const newToken = getStoredToken();
          if (newToken) {
            (headers as Record<string, string>)["Authorization"] = `Bearer ${newToken}`;
            res = await fetch(url, { ...options, headers });
          }
        }
      } else if (isAuthSessionProbe) {
        clearStoredTokens();
        const path = window.location.pathname;
        if (path !== "/login") {
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

  uploadVoiceForParent: async (parentId: number, file: File): Promise<VoiceProfile> => {
    const formData = new FormData();
    formData.append("file", file);
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
  ): Promise<{ jobId: number; status: string; message: string }> => {
    const res = await fetchWithAuth(
      `/api/v1/admin/parents/${parentId}/voice/${voiceProfileId}/run-job`,
      { method: "POST" }
    );
    const data = (await res.json().catch(() => ({}))) as { message?: string; jobId?: number; status?: string };
    if (!res.ok) throw new Error(data?.message ?? res.statusText ?? "Run job failed");
    return { jobId: data.jobId ?? 0, status: data.status ?? "PENDING", message: data.message ?? "Job started." };
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

  getReferralCodes: () => fetchJson<ReferralCode[]>("/api/v1/admin/referral-codes"),
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

  getRevenueTable: (page = 0, size = 20, search?: string, planFilter?: string) =>
    fetchJson<PagedResponse<RevenueRow>>(
      `/api/v1/admin/metrics/revenue/table?page=${page}&size=${size}${search ? `&search=${encodeURIComponent(search)}` : ""}${planFilter ? `&plan=${encodeURIComponent(planFilter)}` : ""}`
    ),

  getKafkaEvents: (page = 0, size = 20) =>
    fetchJson<PagedResponse<Record<string, unknown>>>(
      `/api/v1/admin/kafka-events?page=${page}&size=${size}`
    ),

  getAuditTrail: (page = 0, size = 20) =>
    fetchJson<PagedResponse<Record<string, unknown>>>(
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

  getLibraryStories: (page = 0, size = 20, status?: string) =>
    fetchJson<PagedResponse<LibraryStorySummary>>(
      `/api/v1/admin/stories?page=${page}&size=${size}${status ? `&status=${encodeURIComponent(status)}` : ""}`
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
      "/api/v1/admin/pipeline/active-now"
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
    const r = await fetchWithAuth(`/api/v1/admin/stories/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    });
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
          : r.status === 404
            ? "Story not found"
            : "Failed to update story";
      throw new Error(msg && msg !== "Invalid request" ? msg : fallback);
    }
    return r.json() as Promise<LibraryStorySummary>;
  },

  /** Regenerate story content with Tamixa conversion prompt; optionally generate for all languages. Returns content/title/moral/category/theme and per-language translations (does not save). */
  regenerateStoryWithPrompt: async (
    id: number,
    content?: string | null,
    generateForAllLanguages = true
  ): Promise<{
    content: string;
    title: string;
    moral: string;
    category?: string;
    theme?: string;
    translations?: Record<string, { content: string; title: string; moral: string }>;
  }> => {
    const body: { content?: string; generateForAllLanguages?: boolean } = {};
    if (content != null && content.trim() !== "") body.content = content.trim();
    if (generateForAllLanguages) body.generateForAllLanguages = true;
    const r = await fetchWithAuth(`/api/v1/admin/stories/${id}/regenerate-with-prompt`, {
      method: "POST",
      body: JSON.stringify(body),
    });
    if (!r.ok) {
      const err = (await r.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Regeneration failed");
    }
    const data = (await r.json()) as {
      content?: string;
      title?: string;
      moral?: string;
      category?: string;
      theme?: string;
      translations?: Record<string, { content?: string; title?: string; moral?: string }>;
    };
    const translations: Record<string, { content: string; title: string; moral: string }> = {};
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
    return {
      content: data.content ?? "",
      title: data.title ?? "",
      moral: data.moral ?? "",
      ...(data.category != null ? { category: data.category } : {}),
      ...(data.theme != null ? { theme: data.theme } : {}),
      ...(Object.keys(translations).length > 0 ? { translations } : {}),
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
