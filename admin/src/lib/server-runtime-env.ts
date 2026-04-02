/**
 * Backend API origin for server-side code (proxy, SSR).
 *
 * Next.js may inline bare `process.env.API_URL` at build time with the value from the build
 * host (often empty in Docker). Dynamic property reads keep Railway/runtime variables visible.
 */
const proc = typeof process !== "undefined" ? process : undefined;

function pick(name: string): string | undefined {
  const v = proc?.env[name];
  if (typeof v !== "string") return undefined;
  const t = v.trim();
  return t.length > 0 ? t : undefined;
}

const E = {
  apiUrl: "API" + "_" + "URL",
  nextPublicApiUrl: "NEXT_PUBLIC" + "_" + "API" + "_" + "URL",
  tamixaApiBase: "TAMIXA" + "_" + "API" + "_" + "BASE" + "_" + "URL",
} as const;

/** First non-empty: API_URL, NEXT_PUBLIC_API_URL, TAMIXA_API_BASE_URL. No trailing slash. */
export function getBackendApiOriginFromEnv(): string | undefined {
  const raw = pick(E.apiUrl) || pick(E.nextPublicApiUrl) || pick(E.tamixaApiBase);
  return raw ? raw.replace(/\/$/, "") : undefined;
}

export function getApiUrlPairForMismatchWarn(): { apiUrl?: string; nextPublicApiUrl?: string } {
  return { apiUrl: pick(E.apiUrl), nextPublicApiUrl: pick(E.nextPublicApiUrl) };
}
