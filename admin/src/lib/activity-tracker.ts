/**
 * Tracks user activity for session lifecycle. Used for sliding-window semantics:
 * only proactively refresh when user has been active recently.
 */

import { IDLE_TIMEOUT_MS } from "./session-config";

let lastActivityMs = 0;

const ACTIVITY_EVENTS = ["mousedown", "keydown", "scroll", "touchstart", "mousemove"] as const;

function onActivity() {
  lastActivityMs = Date.now();
}

/** Call once to start tracking. Returns cleanup function. */
export function startActivityTracking(): () => void {
  if (typeof window === "undefined") return () => {};
  lastActivityMs = Date.now();
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

/** Mark activity (e.g. when making an API call). */
export function touchActivity(): void {
  lastActivityMs = Date.now();
}
