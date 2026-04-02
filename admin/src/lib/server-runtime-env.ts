/**
 * Backend API origin for server-side code (proxy, SSR).
 *
 * Next.js/Webpack can replace `process.env.FOO` at build time with the value from `next build`
 * (often empty in Docker). Even `process.env[dynamicKey]` is sometimes folded when the bundler
 * proves the key is constant. Reading via `new Function` keeps a real runtime lookup so
 * Railway/container env vars (API_URL, etc.) are visible after deploy.
 */
function pick(name: string): string | undefined {
  if (!/^[A-Z][A-Z0-9_]*$/i.test(name)) return undefined;
  try {
    const keyLit = JSON.stringify(name);
    const src = `try { var p = typeof process !== "undefined" ? process : undefined; var v = p && p.env && p.env[${keyLit}]; return typeof v === "string" ? v : undefined; } catch (e) { return undefined; }`;
    // eslint-disable-next-line @typescript-eslint/no-implied-eval -- intentional escape from DefinePlugin inlining
    const fn = new Function(src) as () => string | undefined;
    const v = fn();
    if (typeof v !== "string") return undefined;
    const t = v.trim();
    return t.length > 0 ? t : undefined;
  } catch {
    return undefined;
  }
}

const E = {
  apiUrl: "API" + "_" + "URL",
  /** Common Railway naming; must still point at Spring, not the admin app host. */
  apiBaseUrl: "API" + "_" + "BASE" + "_" + "URL",
  nextPublicApiUrl: "NEXT_PUBLIC" + "_" + "API" + "_" + "URL",
  tamixaApiBase: "TAMIXA" + "_" + "API" + "_" + "BASE" + "_" + "URL",
} as const;

/** First non-empty: API_URL, API_BASE_URL, NEXT_PUBLIC_API_URL, TAMIXA_API_BASE_URL. No trailing slash. */
export function getBackendApiOriginFromEnv(): string | undefined {
  const raw =
    pick(E.apiUrl) ||
    pick(E.apiBaseUrl) ||
    pick(E.nextPublicApiUrl) ||
    pick(E.tamixaApiBase);
  return raw ? raw.replace(/\/$/, "") : undefined;
}

export function getApiUrlPairForMismatchWarn(): { apiUrl?: string; nextPublicApiUrl?: string } {
  return { apiUrl: pick(E.apiUrl), nextPublicApiUrl: pick(E.nextPublicApiUrl) };
}
