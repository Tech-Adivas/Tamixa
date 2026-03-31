"use client";

import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import {
  api,
  authStorage,
  ApiError,
  registerSessionExpiredHandler,
  unregisterSessionExpiredHandler,
  refreshTokensIfNeeded,
} from "@/lib/api";
import { shouldProactivelyRefresh } from "@/lib/session-utils";
import {
  REFRESH_CHECK_INTERVAL_MS,
  INACTIVITY_SESSION_WARNING_MS,
} from "@/lib/session-config";
import { startActivityTracking, isUserActive, getMsSinceLastUserInteraction } from "@/lib/activity-tracker";
import { subscribeSessionEvents, broadcastLogout } from "@/lib/session-sync";
import { SessionExpiredDialog } from "@/components/session-expired-dialog";
import { SessionWarningDialog } from "@/components/session-warning-dialog";
import type { CurrentUserResponse } from "@/types/api";

interface AuthState {
  user: CurrentUserResponse | null;
  loading: boolean;
  error: string | null;
}

const AuthContext = createContext<{
  user: CurrentUserResponse | null;
  loading: boolean;
  error: string | null;
  logout: () => void;
  setTokens: (access: string, refresh: string) => void;
  /** Call after login/session dialog already validated GET /me — avoids a second /me that can fail and wipe tokens. */
  hydrateUser: (user: CurrentUserResponse) => void;
  refreshUser: () => Promise<void>;
} | null>(null);

const RATE_LIMIT_RETRY_MS = 8000;

function isRateLimitError(e: unknown): e is ApiError {
  return e instanceof ApiError && e.status === 429;
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>({
    user: null,
    loading: true,
    error: null,
  });
  const [sessionExpiredOpen, setSessionExpiredOpen] = useState(false);
  const [sessionWarningOpen, setSessionWarningOpen] = useState(false);
  const retryTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const refreshIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  /** Many API calls can 401 at once (e.g. pipeline poll + save). Each must be resumed after re-login — a single ref would drop earlier waiters. */
  const sessionExpiredWaitersRef = useRef<Array<(ok: boolean) => void>>([]);

  const flushSessionExpiredWaiters = useCallback((ok: boolean) => {
    const waiters = sessionExpiredWaitersRef.current;
    sessionExpiredWaitersRef.current = [];
    for (const resolve of waiters) {
      resolve(ok);
    }
  }, []);

  const setTokens = useCallback((access: string, refresh: string) => {
    authStorage.setTokens(access, refresh);
  }, []);

  const hydrateUser = useCallback((user: CurrentUserResponse) => {
    setState((s) => ({ ...s, user, loading: false, error: null }));
  }, []);

  const router = useRouter();

  const logout = useCallback(() => {
    authStorage.setLogoutFlag();
    authStorage.clearTokens();
    broadcastLogout();
    setState({ user: null, loading: false, error: null });
    setSessionExpiredOpen(false);
    setSessionWarningOpen(false);
    flushSessionExpiredWaiters(false);
  }, [flushSessionExpiredWaiters]);

  const fetchUser = useCallback(() => {
    if (authStorage.consumeLogoutFlag()) {
      setState((s) => ({ ...s, user: null, loading: false }));
      return;
    }
    const token = authStorage.getToken();
    if (!token) {
      setState((s) => ({ ...s, user: null, loading: false }));
      return;
    }
    api
      .getMe()
      .then((user) =>
        setState((s) => ({ ...s, user, loading: false, error: null }))
      )
      .catch((e) => {
        if (isRateLimitError(e)) {
          setState((s) => ({ ...s, loading: true, error: "Rate limit exceeded. Retrying…" }));
          retryTimeoutRef.current = setTimeout(() => {
            retryTimeoutRef.current = null;
            setState((s) => ({ ...s, error: null }));
            fetchUser();
          }, RATE_LIMIT_RETRY_MS);
        } else if (e instanceof ApiError && e.status === 401) {
          authStorage.clearTokens();
          setState({ user: null, loading: false, error: null });
        } else if (e instanceof ApiError && e.status === 403) {
          setState((s) => ({
            ...s,
            loading: false,
            error: "Access denied for this account.",
          }));
        } else {
          setState((s) => ({
            ...s,
            loading: false,
            error: e instanceof Error ? e.message : "Unable to verify session",
          }));
        }
      });
  }, []);

  const refreshUser = useCallback(async () => {
    const token = authStorage.getToken();
    if (!token) {
      setState((s) => ({ ...s, user: null, loading: false }));
      return;
    }
    try {
      const user = await api.getMe();
      setState((s) => ({ ...s, user, loading: false, error: null }));
    } catch (e) {
      if (isRateLimitError(e)) {
        setState((s) => ({ ...s, loading: false, error: "Rate limit exceeded. Please wait and try again." }));
      } else if (e instanceof ApiError && e.status === 401) {
        authStorage.clearTokens();
        setState({ user: null, loading: false, error: null });
      } else if (e instanceof ApiError && e.status === 403) {
        setState((s) => ({
          ...s,
          loading: false,
          error: "Access denied for this account.",
        }));
      } else {
        setState((s) => ({
          ...s,
          loading: false,
          error: e instanceof Error ? e.message : "Unable to refresh session",
        }));
      }
    }
  }, []);

  useEffect(() => {
    fetchUser();
    return () => {
      if (retryTimeoutRef.current) clearTimeout(retryTimeoutRef.current);
    };
  }, [fetchUser]);

  // Session expired handler: show re-login dialog, resolve when user logs in or cancels
  useEffect(() => {
    const handler = (): Promise<boolean> => {
      return new Promise<boolean>((resolve) => {
        sessionExpiredWaitersRef.current.push(resolve);
        setSessionExpiredOpen(true);
      });
    };
    registerSessionExpiredHandler(handler);
    return () => {
      unregisterSessionExpiredHandler();
      const waiters = sessionExpiredWaitersRef.current;
      sessionExpiredWaitersRef.current = [];
      for (const resolve of waiters) {
        resolve(false);
      }
    };
  }, []);

  const handleSessionExpiredSuccess = useCallback(
    (user: CurrentUserResponse) => {
      flushSessionExpiredWaiters(true);
      hydrateUser(user);
    },
    [flushSessionExpiredWaiters, hydrateUser]
  );

  const handleSessionExpiredCancel = useCallback(() => {
    flushSessionExpiredWaiters(false);
  }, [flushSessionExpiredWaiters]);

  // Activity tracking for sliding-window session
  useEffect(() => {
    return startActivityTracking();
  }, []);

  // Multi-tab sync: logout and token updates
  useEffect(() => {
    return subscribeSessionEvents({
      onLogout: () => {
        authStorage.clearTokens();
        setState({ user: null, loading: false, error: null });
        setSessionExpiredOpen(false);
        setSessionWarningOpen(false);
        if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
          router.replace("/login?expired=1");
        }
      },
      onTokenUpdate: () => {
        if (authStorage.getToken()) refreshUser();
      },
    });
  }, [refreshUser, router]);

  // Proactive refresh (only when user active) + "stay signed in" after prolonged inactivity
  useEffect(() => {
    const run = () => {
      if (typeof document === "undefined") return;
      const token = authStorage.getToken();
      if (!token) {
        setSessionWarningOpen(false);
        return;
      }

      const idleMs = getMsSinceLastUserInteraction();

      if (
        sessionWarningOpen &&
        idleMs < INACTIVITY_SESSION_WARNING_MS
      ) {
        setSessionWarningOpen(false);
      } else if (
        !sessionWarningOpen &&
        document.visibilityState === "visible" &&
        idleMs >= INACTIVITY_SESSION_WARNING_MS
      ) {
        setSessionWarningOpen(true);
      }

      // Proactive refresh only when user active and token expires soon
      if (
        document.visibilityState === "visible" &&
        isUserActive() &&
        shouldProactivelyRefresh(token)
      ) {
        setSessionWarningOpen(false); // Dismiss idle warning if user returned and we refresh
        refreshTokensIfNeeded().then((ok) => {
          if (ok) refreshUser();
        });
      }
    };
    refreshIntervalRef.current = setInterval(run, REFRESH_CHECK_INTERVAL_MS);
    run();
    return () => {
      if (refreshIntervalRef.current) {
        clearInterval(refreshIntervalRef.current);
        refreshIntervalRef.current = null;
      }
    };
  }, [refreshUser, sessionWarningOpen]);

  const handleStaySignedIn = useCallback(async () => {
    setSessionWarningOpen(false);
    const ok = await refreshTokensIfNeeded();
    if (ok) refreshUser();
  }, [refreshUser]);

  return (
    <AuthContext.Provider
      value={{
        user: state.user,
        loading: state.loading,
        error: state.error,
        logout,
        setTokens,
        hydrateUser,
        refreshUser,
      }}
    >
      {children}
      <SessionExpiredDialog
        open={sessionExpiredOpen}
        onOpenChange={setSessionExpiredOpen}
        onSuccess={handleSessionExpiredSuccess}
        onCancel={handleSessionExpiredCancel}
      />
      <SessionWarningDialog
        open={sessionWarningOpen}
        onStaySignedIn={handleStaySignedIn}
        onLogout={logout}
      />
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
