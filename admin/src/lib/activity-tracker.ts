/**
 * Tracks user activity for session lifecycle. Used for sliding-window semantics:
 * only proactively refresh when user has been active recently.
 */

import { IDLE_TIMEOUT_MS } from "./session-config";

/**
 * Last “presence” for the inactivity dialog: DOM gestures, and authenticated API calls while the tab is visible.
 * Tab-hidden API polling does not advance this (so returning after a long absence can still prompt).
 */
let lastUserInteractionMs = 0;
/** Last activity for proactive token refresh (includes API traffic via touchActivity()). */
let lastActivityMs = 0;

const ACTIVITY_EVENTS = ["mousedown", "keydown", "scroll", "touchstart", "mousemove"] as const;

function onActivity() {
  const now = Date.now();
  lastUserInteractionMs = now;
  lastActivityMs = now;
}

/** Call once to start tracking. Returns cleanup function. */
export function startActivityTracking(): () => void {
  if (typeof window === "undefined") return () => {};
  const now = Date.now();
  lastUserInteractionMs = now;
  lastActivityMs = now;
  ACTIVITY_EVENTS.forEach((ev) => window.addEventListener(ev, onActivity, { passive: true }));
  return () => {
    ACTIVITY_EVENTS.forEach((ev) => window.removeEventListener(ev, onActivity));
  };
}

/** True if user has interacted within IDLE_TIMEOUT_MS. */
export function isUserActive(): boolean {
  if (typeof window === "undefined") return false;
  return Date.now() - lastActivityMs < IDLE_TIMEOUT_MS;
}

/** Milliseconds since the last real user input (not API calls). */
export function getMsSinceLastUserInteraction(): number {
  if (typeof window === "undefined") return 0;
  return Date.now() - lastUserInteractionMs;
}

/** Mark activity (e.g. when making an API call). */
export function touchActivity(): void {
  const now = Date.now();
  lastActivityMs = now;
  if (typeof document !== "undefined" && document.visibilityState === "visible") {
    lastUserInteractionMs = now;
  }
}
