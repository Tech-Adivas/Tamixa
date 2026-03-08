import type {
  AdminInvoice,
  AdminUser,
  AuthResponse,
  CompletionMetricsDto,
  CurrentUserResponse,
  PagedResponse,
  ParentSummary,
  RetentionMetricsDto,
  ChildSummary,
  StorySummary,
  CuratedStorySummary,
  CreateCuratedStoryRequest,
  PipelineStatusResponse,
  VoiceUploadLog,
  SubscriptionStatus,
  HealthDto,
  AiMetricsDto,
  RevenueMetricsDto,
  SubscriptionMetricsDto,
  RevenueRow,
  Soundscape,
  SoundscapeUsage,
  VoiceCloningJob,
  VoiceTier,
} from "@/types/api";

const getBaseUrl = (): string => {
  // When NEXT_PUBLIC_API_URL is set, use it (e.g. production or explicit backend URL).
  const explicit = typeof process !== "undefined" && process.env?.NEXT_PUBLIC_API_URL;
  if (explicit) return process.env.NEXT_PUBLIC_API_URL!;
  // In the browser with no explicit URL: talk to backend directly so Authorization header is sent.
  // Next.js rewrites do not reliably forward client headers when proxying, which caused "Unauthorized. Please log in."
  if (typeof window !== "undefined") return "http://localhost:8080";
  // Server-side (SSR): same-origin so Next.js can rewrite
  return "";
};

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
}

function clearStoredTokens() {
  if (typeof window === "undefined") return;
  localStorage.removeItem("admin_access_token");
  localStorage.removeItem("admin_refresh_token");
}

export const authStorage = {
  getToken: getStoredToken,
  getRefreshToken: getStoredRefreshToken,
  setTokens: setStoredTokens,
  clearTokens: clearStoredTokens,
};

async function fetchWithAuth(
  path: string,
  options: RequestInit = {},
  retry = true
): Promise<Response> {
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

  if (res.status === 401 && retry) {
    const refresh = getStoredRefreshToken();
    if (refresh) {
      const refreshUrl = `${base}/api/v1/auth/refresh`;
      try {
        const refreshRes = await fetch(refreshUrl, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken: refresh }),
        });
        if (refreshRes.ok) {
          const data = (await refreshRes.json()) as AuthResponse;
          setStoredTokens(data.accessToken, data.refreshToken);
          headers["Authorization"] = `Bearer ${data.accessToken}`;
          res = await fetch(url, { ...options, headers });
        }
      } catch (e) {
        throw e;
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

  getStory: (id: number) =>
    fetchJson<{ id: number; content: string; theme: string; status: string; [k: string]: unknown }>(
      `/api/v1/admin/stories/${id}`
    ),

  /** Returns blob URL for fetching - use fetch with Authorization for actual streaming. */
  getStoryPreviewAudioUrl: (id: number) =>
    `/api/v1/admin/stories/${id}/preview-audio`,

  getChildren: (page = 0, size = 20, parentId?: number) =>
    fetchJson<PagedResponse<ChildSummary>>(
      `/api/v1/admin/children?page=${page}&size=${size}${parentId != null ? `&parentId=${parentId}` : ""}`
    ),

  getStories: (page = 0, size = 20, status?: string, theme?: string) =>
    fetchJson<PagedResponse<StorySummary>>(
      `/api/v1/admin/stories?page=${page}&size=${size}${status ? `&status=${encodeURIComponent(status)}` : ""}${theme ? `&theme=${encodeURIComponent(theme)}` : ""}`
    ),

  getVoiceLogs: (page = 0, size = 20) =>
    fetchJson<PagedResponse<VoiceUploadLog>>(
      `/api/v1/admin/voice-logs?page=${page}&size=${size}`
    ),

  getSubscriptions: (page = 0, size = 20) =>
    fetchJson<PagedResponse<SubscriptionStatus>>(
      `/api/v1/admin/subscriptions?page=${page}&size=${size}`
    ),

  getHealth: () => fetchJson<HealthDto>("/api/v1/admin/health"),

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

  getCuratedStories: (page = 0, size = 20, status?: string) =>
    fetchJson<PagedResponse<CuratedStorySummary>>(
      `/api/v1/admin/curated-stories?page=${page}&size=${size}${status ? `&status=${encodeURIComponent(status)}` : ""}`
    ),

  getCuratedStory: (id: number) =>
    fetchJson<CuratedStorySummary & { content: string }>(
      `/api/v1/admin/curated-stories/${id}`
    ),

  generateCuratedStoryCover: async (id: number, force = false) => {
    const url = `/api/v1/admin/curated-stories/${id}/generate-cover${force ? "?force=true" : ""}`;
    const r = await fetchWithAuth(url, {
      method: "POST",
    });
    if (!r.ok) {
      const err = (await r.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Cover generation failed");
    }
    return r.json() as Promise<CuratedStorySummary & { content?: string }>;
  },

  suggestRephrase: async (
    id: number,
    title: string | null,
    content: string
  ): Promise<{ suggestedTitle: string; suggestedContent: string }> => {
    const r = await fetchWithAuth(`/api/v1/admin/curated-stories/${id}/suggest-rephrase`, {
      method: "POST",
      body: JSON.stringify({ title: title ?? "", content }),
    });
    if (!r.ok) {
      const err = (await r.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Rephrase suggestion failed");
    }
    return r.json();
  },

  retryCuratedStory: (id: number) =>
    fetchJson<{ message: string }>(
      `/api/v1/admin/curated-stories/${id}/retry`,
      { method: "POST" }
    ),

  triggerCuratedStoryPipeline: (id: number) =>
    fetchJson<{ message: string }>(
      `/api/v1/admin/curated-stories/${id}/trigger-pipeline`,
      { method: "POST" }
    ),

  regenerateCuratedStoryNarration: (id: number) =>
    fetchJson<{ message: string }>(
      `/api/v1/admin/curated-stories/${id}/regenerate-narration`,
      { method: "POST" }
    ),

  republishCuratedStory: (id: number, options?: { languages?: string[] }) =>
    fetchJson<{ message: string }>(
      `/api/v1/admin/curated-stories/${id}/republish`,
      {
        method: "POST",
        body: JSON.stringify({
          languages: options?.languages ?? [],
        }),
      }
    ),

  getCuratedStoryPipelineStatus: async (id: number) => {
    const res = await fetchWithAuth(
      `/api/v1/admin/curated-stories/${id}/pipeline-status`
    );
    return res.ok ? (res.json() as Promise<PipelineStatusResponse>) : null;
  },

  getCuratedStoryStreamUrl: (id: number, language: string = "ta") =>
    fetchJson<{ streamUrl: string }>(
      `/api/v1/admin/curated-stories/${id}/stream-url?language=${encodeURIComponent(language)}`
    ),

  updateCuratedStory: async (id: number, data: CreateCuratedStoryRequest) => {
    const r = await fetchWithAuth(`/api/v1/admin/curated-stories/${id}`, {
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
      throw new Error(
        msg && msg !== "Invalid request"
          ? msg
          : r.status === 404
            ? "Story not found"
            : "Failed to update curated story"
      );
    }
    return r.json() as Promise<CuratedStorySummary>;
  },

  bulkPublish: (ids: number[]) =>
    fetchJson<{ updated: number; ids: number[] }>(
      "/api/v1/admin/curated-stories/bulk-publish",
      { method: "PUT", body: JSON.stringify({ ids }) }
    ),

  bulkUpdateCategory: (ids: number[], theme: string) =>
    fetchJson<{ updated: number; theme: string }>(
      "/api/v1/admin/curated-stories/bulk-category",
      { method: "PUT", body: JSON.stringify({ ids, theme }) }
    ),

  deleteCuratedStory: async (id: number): Promise<void> => {
    const r = await fetchWithAuth(`/api/v1/admin/curated-stories/${id}`, {
      method: "DELETE",
    });
    if (!r.ok && r.status !== 404) {
      const err = (await r.json().catch(() => ({}))) as { message?: string };
      throw new Error(err?.message ?? "Failed to delete story");
    }
  },

  bulkDeleteCuratedStories: async (ids: number[]) =>
    fetchJson<{ deleted: number; ids: number[] }>(
      "/api/v1/admin/curated-stories/bulk",
      { method: "DELETE", body: JSON.stringify({ ids }) }
    ),

  createCuratedStory: async (data: CreateCuratedStoryRequest) => {
    const r = await fetchWithAuth("/api/v1/admin/curated-stories", {
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
        (err?.message ?? err?.error ?? err?.detail ?? r.statusText) || "Failed to create curated story";
      throw new Error(msg);
    }
    return r.json() as Promise<CuratedStorySummary>;
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

export const api = { login, getMe, admin };
