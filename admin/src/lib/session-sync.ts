/**
 * Multi-tab session synchronization.
 * - Logout in one tab logs out all tabs (BroadcastChannel)
 * - Token refresh in one tab updates others (storage event)
 */

import { SESSION_CHANNEL } from "./session-config";

export type SessionEvent = "logout" | "token-updated";

const STORAGE_KEY_LOGOUT = "admin_session_logout";
const STORAGE_KEY_TOKENS = "admin_tokens_updated";

function getChannel(): BroadcastChannel | null {
  if (typeof BroadcastChannel === "undefined") return null;
  return new BroadcastChannel(SESSION_CHANNEL);
}

/** Broadcast logout to other tabs. Call on explicit logout. */
export function broadcastLogout(): void {
  const ch = getChannel();
  if (ch) {
    ch.postMessage({ type: "logout" as SessionEvent });
  }
  // Fallback: localStorage so storage event fires in OTHER tabs (sessionStorage is per-tab)
  try {
    localStorage.setItem(STORAGE_KEY_LOGOUT, Date.now().toString());
  } catch {
    // Ignore
  }
}

/** Notify other tabs that tokens were updated (they read from localStorage). */
export function broadcastTokenUpdate(): void {
  const ch = getChannel();
  if (ch) {
    ch.postMessage({ type: "token-updated" as SessionEvent });
  }
  try {
    localStorage.setItem(STORAGE_KEY_TOKENS, Date.now().toString());
  } catch {
    // Ignore
  }
}

/** Subscribe to cross-tab session events. Returns cleanup. */
export function subscribeSessionEvents(handlers: {
  onLogout?: () => void;
  onTokenUpdate?: () => void;
}): () => void {
  if (typeof window === "undefined") return () => {};

  const ch = getChannel();
  const onMessage = (e: MessageEvent<{ type: SessionEvent }>) => {
    if (e.data?.type === "logout") handlers.onLogout?.();
    if (e.data?.type === "token-updated") handlers.onTokenUpdate?.();
  };

  const onStorage = (e: StorageEvent) => {
    if (e.key === STORAGE_KEY_LOGOUT) handlers.onLogout?.();
    if (e.key === STORAGE_KEY_TOKENS) handlers.onTokenUpdate?.();
  };

  if (ch) ch.addEventListener("message", onMessage);
  window.addEventListener("storage", onStorage);

  return () => {
    if (ch) ch.removeEventListener("message", onMessage);
    window.removeEventListener("storage", onStorage);
  };
}
