/**
 * Session utilities: JWT expiry parsing.
 * Uses session-config for thresholds.
 */

import { REFRESH_THRESHOLD_MS, REFRESH_CHECK_INTERVAL_MS } from "./session-config";

/** Re-export for consumers. */
export { REFRESH_THRESHOLD_MS, REFRESH_CHECK_INTERVAL_MS };

/** Decode JWT payload without verification (client-side; server validates). Returns exp (seconds) or null. */
export function getJwtExpiry(token: string | null): number | null {
  if (!token?.trim()) return null;
  try {
    const parts = token.split(".");
    if (parts.length !== 3) return null;
    const decoded = atob(parts[1].replace(/-/g, "+").replace(/_/g, "/"));
    const payload: { exp?: number } = JSON.parse(decoded);
    return typeof payload.exp === "number" ? payload.exp : null;
  } catch {
    return null;
  }
}

/** True if token expires within REFRESH_THRESHOLD_MS (needs proactive refresh). */
export function shouldProactivelyRefresh(token: string | null): boolean {
  const exp = getJwtExpiry(token);
  if (exp == null) return false;
  const expiresAtMs = exp * 1000;
  const now = Date.now();
  return expiresAtMs - now < REFRESH_THRESHOLD_MS;
}
