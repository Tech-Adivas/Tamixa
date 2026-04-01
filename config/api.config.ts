/**
 * Shared API configuration for Tamixa apps.
 * Use localhost for local development. Override via env vars for staging/production.
 *
 * Usage:
 * - Web: imports DEFAULT_API_BASE_URL (vite). Set VITE_API_BASE_URL at build time for prod (see web/Dockerfile).
 * - Admin: override with NEXT_PUBLIC_API_URL in .env.local (does not use this file for runtime in Next).
 * - Mobile: ApiConfig.kt should match deployed API URL.
 */

/** Backend API base URL. Vite: VITE_API_BASE_URL at build time; else localhost for dev. */
export const DEFAULT_API_BASE_URL = (() => {
  try {
    const v = (import.meta as { env?: { VITE_API_BASE_URL?: string } }).env?.VITE_API_BASE_URL
    if (typeof v === "string" && v.trim().length > 0) return v.trim().replace(/\/$/, "")
  } catch {
    /* non-Vite consumers */
  }
  return "http://localhost:8080"
})()

/** API path prefix. */
export const API_PATH = "/api/v1";

/** Full API URL (base + path). */
export const DEFAULT_FULL_API_URL = `${DEFAULT_API_BASE_URL}${API_PATH}`;

/** Web app base URL (for magic links, redirects, subscription management). */
export const DEFAULT_WEB_APP_URL = "http://localhost:3000";

/** Subscription management page path. Mobile "Manage" opens BASE + SUBSCRIPTION_PATH. */
export const SUBSCRIPTION_PATH = "/subscription";

/** Full subscription URL for dev. Override with TAMIXA_WEB_APP_URL in production. */
export const DEFAULT_SUBSCRIPTION_URL = `${DEFAULT_WEB_APP_URL}${SUBSCRIPTION_PATH}`;
