/**
 * Proxy for /api/* → backend. Forwards Authorization header so authenticated
 * requests work. Next.js rewrites do NOT forward client headers.
 *
 * Security: only paths under /api/v1/ are forwarded. All other paths are blocked.
 */
import type { NextRequest } from "next/server";
import { NextResponse } from "next/server";

export const runtime = "nodejs";

/** Base URL for Spring Boot (no trailing slash). Read per request so `next dev` picks up .env.local changes. */
let apiEnvMismatchWarned = false;
const LONG_RUNNING_ADMIN_OP_TIMEOUT_MS = 20 * 60 * 1000;

function warnIfApiEnvMismatch(): void {
  if (apiEnvMismatchWarned) return;
  const apiUrl = process.env.API_URL?.trim();
  const nextPublicApiUrl = process.env.NEXT_PUBLIC_API_URL?.trim();
  if (!apiUrl || !nextPublicApiUrl || apiUrl === nextPublicApiUrl) return;
  apiEnvMismatchWarned = true;
  console.warn(
    `[admin api proxy] API_URL (${apiUrl}) differs from NEXT_PUBLIC_API_URL (${nextPublicApiUrl}). ` +
      "This can route requests to different backends. Set both to the same value."
  );
}

function backendBaseUrl(): string {
  warnIfApiEnvMismatch();
  const raw =
    process.env.API_URL?.trim() ||
    process.env.NEXT_PUBLIC_API_URL?.trim() ||
    "http://127.0.0.1:8080";
  return raw.replace(/\/$/, "");
}

function buildCandidateBackendUrls(base: string): string[] {
  const urls = [base];
  // Local fallback: some environments intermittently fail resolving localhost/::1.
  if (base === "http://localhost:8080") {
    urls.push("http://127.0.0.1:8080");
  } else if (base === "http://127.0.0.1:8080") {
    urls.push("http://localhost:8080");
  }
  return urls;
}

function looksLikeConnectionFailure(err: Error): boolean {
  const msg = `${err.message} ${(err as Error & { cause?: unknown }).cause ?? ""}`.toLowerCase();
  return (
    msg.includes("econnrefused") ||
    msg.includes("econnreset") ||
    msg.includes("enotfound") ||
    msg.includes("eai_again") ||
    msg.includes("fetch failed")
  );
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
    init.body = await request.arrayBuffer();
  }
  const isLongRunningAdminOp =
    pathStr.includes("regenerate-with-prompt") ||
    pathStr.includes("rebuild-narration-pipeline") ||
    pathStr.includes("bulk-generate");
  const initWithTimeout = init as RequestInit & { signal?: AbortSignal };
  if (isLongRunningAdminOp) {
    // Allow long-running admin jobs to finish before fetch timeout.
    initWithTimeout.signal = AbortSignal.timeout(LONG_RUNNING_ADMIN_OP_TIMEOUT_MS);
  }

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
  const message = isDev
    ? `Backend unreachable (proxy → ${lastTriedUrl}). Start Spring Boot (e.g. ./gradlew :backend:bootRun) and ensure admin/.env.local points API_URL or NEXT_PUBLIC_API_URL to the running backend.`
    : "Backend unreachable";
  return NextResponse.json({ message, detail: lastErr?.message }, { status: 502 });
}
