/**
 * Proxy for /api/* → backend. Forwards Authorization header so authenticated
 * requests work. Next.js rewrites do NOT forward client headers.
 *
 * Security: only paths under /api/v1/ are forwarded. All other paths are blocked.
 */
import { existsSync } from "node:fs";
import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";
import {
  getApiUrlPairForMismatchWarn,
  getBackendApiOriginFromEnv,
} from "@/lib/server-runtime-env";

export const runtime = "nodejs";

/** Base URL for Spring Boot (no trailing slash). Read per request so `next dev` picks up .env.local changes. */
let apiEnvMismatchWarned = false;
const LONG_RUNNING_ADMIN_OP_TIMEOUT_MS = 20 * 60 * 1000;
/** Fail fast when Spring is down or API_URL is wrong; avoids minute-long hangs (undici default). */
const PROXY_DEFAULT_TIMEOUT_MS = 25_000;
/** Large library rows (translations + interactive graph) can exceed the default. */
const PROXY_ADMIN_STORY_TIMEOUT_MS = 120_000;

function warnIfApiEnvMismatch(): void {
  if (apiEnvMismatchWarned) return;
  const { apiUrl, nextPublicApiUrl } = getApiUrlPairForMismatchWarn();
  if (!apiUrl || !nextPublicApiUrl || apiUrl === nextPublicApiUrl) return;
  apiEnvMismatchWarned = true;
  console.warn(
    `[admin api proxy] API_URL (${apiUrl}) differs from NEXT_PUBLIC_API_URL (${nextPublicApiUrl}). ` +
      "This can route requests to different backends. Set both to the same value."
  );
}

/**
 * Spring API origin from runtime env (API_URL, NEXT_PUBLIC_API_URL, or TAMIXA_API_BASE_URL).
 * Uses dynamic env reads so Docker/Railway variables are not wiped by Next build-time inlining.
 */
function backendBaseUrl(): string | null {
  warnIfApiEnvMismatch();
  const raw = getBackendApiOriginFromEnv();
  if (raw) return raw;
  if (process.env.NODE_ENV !== "production") return "http://127.0.0.1:8080";
  return null;
}

function buildCandidateBackendUrls(base: string): string[] {
  const urls = [base];
  // Local fallback: some environments intermittently fail resolving localhost/::1.
  if (base === "http://localhost:8080") {
    urls.push("http://127.0.0.1:8080");
  } else if (base === "http://127.0.0.1:8080") {
    urls.push("http://localhost:8080");
  }
  // Admin in Docker: localhost/127.0.0.1 is the container, not the host running Spring.
  try {
    const u = new URL(base);
    const loopback =
      u.hostname === "localhost" ||
      u.hostname === "127.0.0.1" ||
      u.hostname === "::1";
    if (loopback && existsSync("/.dockerenv")) {
      const port = u.port || (u.protocol === "https:" ? "443" : "80");
      urls.push(`${u.protocol}//host.docker.internal:${port}`);
    }
  } catch {
    /* ignore invalid base */
  }
  return urls;
}

function proxyTimeoutMs(pathStr: string, method: string): number {
  if (
    pathStr.includes("regenerate-with-prompt") ||
    pathStr.includes("rebuild-narration-pipeline") ||
    pathStr.includes("bulk-generate") ||
    pathStr.includes("regenerate-cover")
  ) {
    return LONG_RUNNING_ADMIN_OP_TIMEOUT_MS;
  }
  if (
    /^v1\/admin\/stories(\/|$)/.test(pathStr) &&
    ["GET", "PUT", "PATCH", "POST"].includes(method)
  ) {
    return PROXY_ADMIN_STORY_TIMEOUT_MS;
  }
  return PROXY_DEFAULT_TIMEOUT_MS;
}

function looksLikeConnectionFailure(err: Error): boolean {
  const msg = `${err.message} ${(err as Error & { cause?: unknown }).cause ?? ""}`.toLowerCase();
  return (
    msg.includes("econnrefused") ||
    msg.includes("econnreset") ||
    msg.includes("enotfound") ||
    msg.includes("eai_again") ||
    msg.includes("fetch failed") ||
    msg.includes("aborted") ||
    msg.includes("timeout") ||
    msg.includes("etimedout")
  );
}

function safeApiOrigin(base: string): string {
  try {
    return new URL(base).origin;
  } catch {
    return "(invalid API_URL)";
  }
}

/**
 * Segments after /api/ from the incoming URL (e.g. /api/v1/health → "v1/health").
 * Backend expects /api/v1/..., so we prepend /api/ when forwarding.
 */
const ALLOWED_PATH_PREFIX = "v1/";

export async function GET(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return proxy(request, context);
}
export async function POST(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return proxy(request, context);
}
export async function PUT(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return proxy(request, context);
}
export async function PATCH(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return proxy(request, context);
}
export async function DELETE(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return proxy(request, context);
}

async function proxy(
  request: NextRequest,
  context: { params: Promise<{ path: string[] }> }
) {
  const { path } = await context.params;
  const pathStr = path?.length ? path.join("/") : "";

  // Block requests that don't target the allowed API prefix
  if (!pathStr.startsWith(ALLOWED_PATH_PREFIX)) {
    return NextResponse.json({ message: "Not found" }, { status: 404 });
  }

  const search = request.nextUrl.search;
  const base = backendBaseUrl();
  if (!base) {
    return NextResponse.json(
      {
        message: "Admin API proxy is not configured.",
        hint:
          "Railway → admin service → Variables: set API_URL, API_BASE_URL, NEXT_PUBLIC_API_URL, or TAMIXA_API_BASE_URL to your Spring API HTTPS origin (e.g. https://tamixa-prod.up.railway.app)—not the admin app URL. No trailing slash; redeploy after changes.",
      },
      { status: 503 }
    );
  }
  const urls = buildCandidateBackendUrls(base).map((candidateBase) => `${candidateBase}/api/${pathStr}${search}`);

  const headers = new Headers();
  const forwardKeys = ["content-type", "x-request-id", "x-correlation-id"];
  request.headers.forEach((value, key) => {
    if (forwardKeys.includes(key.toLowerCase()) && value) {
      headers.set(key, value);
    }
  });

  // Use Authorization from the client when it looks like a real Bearer token; otherwise fall back to the session
  // cookie. The client reconciles localStorage vs cookie (JWT exp) so the header usually matches the fresher token.
  let authorization = request.headers.get("authorization")?.trim();
  const bearerLooksValid =
    !!authorization && /^Bearer\s+\S+$/i.test(authorization) && !/^Bearer\s+null$/i.test(authorization);
  if (!bearerLooksValid) {
    const rawCookie = request.cookies.get("admin_access_token")?.value;
    if (rawCookie?.trim()) {
      try {
        const token = decodeURIComponent(rawCookie.trim());
        if (token) authorization = `Bearer ${token}`;
      } catch {
        authorization = `Bearer ${rawCookie.trim()}`;
      }
    }
  }
  if (authorization && /^Bearer\s+\S+$/i.test(authorization)) {
    headers.set("Authorization", authorization);
  }

  if (!headers.has("content-type") && request.method !== "GET") {
    headers.set("Content-Type", "application/json");
  }

  const init: RequestInit = {
    method: request.method,
    headers,
    cache: "no-store",
  };
  if (request.method !== "GET" && request.body) {
    const MAX_BODY_BYTES = 50 * 1024 * 1024; // 50 MB — covers story content + base64 audio uploads
    const contentLength = request.headers.get("content-length");
    if (contentLength && parseInt(contentLength, 10) > MAX_BODY_BYTES) {
      return NextResponse.json({ message: "Request body too large." }, { status: 413 });
    }
    const bodyBuffer = await request.arrayBuffer();
    if (bodyBuffer.byteLength > MAX_BODY_BYTES) {
      return NextResponse.json({ message: "Request body too large." }, { status: 413 });
    }
    init.body = bodyBuffer;
  }
  const initWithTimeout = init as RequestInit & { signal?: AbortSignal };
  initWithTimeout.signal = AbortSignal.timeout(proxyTimeoutMs(pathStr, request.method));

  let lastErr: Error | null = null;
  let lastTriedUrl = urls[0];
  for (let i = 0; i < urls.length; i += 1) {
    const url = urls[i];
    lastTriedUrl = url;
    try {
      const res = await fetch(url, initWithTimeout);
      const resHeaders = new Headers(res.headers);
      resHeaders.delete("content-encoding");
      return new Response(res.body, {
        status: res.status,
        statusText: res.statusText,
        headers: resHeaders,
      });
    } catch (e) {
      const err = e instanceof Error ? e : new Error(String(e));
      lastErr = err;
      const shouldFallback = i < urls.length - 1 && looksLikeConnectionFailure(err);
      console.error("[admin api proxy] fetch failed", {
        url,
        message: err.message,
        cause: err.cause,
        fallbackAttempted: shouldFallback,
      });
      if (!shouldFallback) break;
    }
  }

  const isDev = process.env.NODE_ENV === "development";
  const inDocker = existsSync("/.dockerenv");
  const hints: string[] = [];
  if (inDocker && /localhost|127\.0\.0\.1|::1/.test(base)) {
    hints.push(
      "Admin appears to run inside Docker: set API_URL and NEXT_PUBLIC_API_URL to http://host.docker.internal:8080 (or your host IP) so the proxy reaches Spring on the host."
    );
  }
  if (
    lastErr &&
    (`${lastErr.message} ${(lastErr as Error & { cause?: unknown }).cause ?? ""}`.toLowerCase().includes("timeout") ||
      `${lastErr.message}`.toLowerCase().includes("aborted"))
  ) {
    hints.push(
      "Request timed out before the backend responded. For very large stories, timeouts were extended for /v1/admin/stories/*; if this persists, check DB/API slowness."
    );
  }
  const message = isDev
    ? `Backend unreachable (proxy → ${lastTriedUrl}). Start Spring Boot (e.g. ./gradlew :backend:bootRun) and ensure admin/.env.local sets API_URL and NEXT_PUBLIC_API_URL to the same origin (e.g. http://127.0.0.1:8080).`
    : "Backend unreachable from admin. Confirm the API service is running, healthy, and reachable at the URL in configuredApiOrigin (open …/api/v1/health in a browser). On Railway, use each service’s public HTTPS URL for API_URL / NEXT_PUBLIC_API_URL.";
  return NextResponse.json(
    {
      message,
      detail: lastErr?.message,
      configuredApiOrigin: safeApiOrigin(base),
      hints,
    },
    { status: 502 }
  );
}
