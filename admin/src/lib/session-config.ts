/**
 * Enterprise session management configuration.
 * Aligns with OWASP session guidelines and JWT best practices.
 */

/** Idle timeout (ms). No activity for this long = consider user idle; do NOT proactively refresh. */
export const IDLE_TIMEOUT_MS = 10 * 60 * 1000; // 10 min

/** Refresh token when it expires in less than this (ms). Backend access token ~15 min. */
export const REFRESH_THRESHOLD_MS = 2 * 60 * 1000; // 2 min

/** How often to check for proactive refresh (ms). */
export const REFRESH_CHECK_INTERVAL_MS = 60 * 1000; // 1 min

/** Show "stay signed in" dialog after this much continuous inactivity (ms). */
export const INACTIVITY_SESSION_WARNING_MS = 15 * 60 * 1000; // 15 min

/** BroadcastChannel name for cross-tab session events. */
export const SESSION_CHANNEL = "tamixa-admin-session";
