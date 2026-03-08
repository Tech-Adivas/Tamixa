/**
 * Shared API configuration for Araro apps.
 * Use localhost for local development. Override via env vars for staging/production.
 *
 * Usage:
 * - Web: imports DEFAULT_API_BASE_URL (vite proxy), API_PATH (api.ts)
 * - Admin: imports DEFAULT_API_BASE_URL; override with NEXT_PUBLIC_API_URL in .env.local
 * - Mobile: ApiConfig.kt should match DEFAULT_API_BASE_URL
 */

/** Backend API base URL. Default: localhost for development. */
export const DEFAULT_API_BASE_URL = "http://localhost:8080";

/** API path prefix. */
export const API_PATH = "/api/v1";

/** Full API URL (base + path). */
export const DEFAULT_FULL_API_URL = `${DEFAULT_API_BASE_URL}${API_PATH}`;

/** Web app base URL (for magic links, redirects, subscription management). */
export const DEFAULT_WEB_APP_URL = "http://localhost:3000";

/** Subscription management page path. Mobile "Manage" opens BASE + SUBSCRIPTION_PATH. */
export const SUBSCRIPTION_PATH = "/subscription";

/** Full subscription URL for dev. Override with ARARO_WEB_APP_URL in production. */
export const DEFAULT_SUBSCRIPTION_URL = `${DEFAULT_WEB_APP_URL}${SUBSCRIPTION_PATH}`;
