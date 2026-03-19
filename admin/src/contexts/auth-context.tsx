"use client";

import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { api, authStorage, ApiError, registerSessionExpiredHandler, unregisterSessionExpiredHandler, refreshTokensIfNeeded } from "@/lib/api";
import { shouldProactivelyRefresh, getJwtExpiry } from "@/lib/session-utils";
import {
  REFRESH_CHECK_INTERVAL_MS,
  SESSION_WARNING_THRESHOLD_MS,
} from "@/lib/session-config";
import { startActivityTracking, isUserActive } from "@/lib/activity-tracker";
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
  const [sessionWarningMinutesLeft, setSessionWarningMinutesLeft] = useState(0);
  const retryTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const refreshIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const pendingResolveRef = useRef<((ok: boolean) => void) | null>(null);

  const setTokens = useCallback((access: string, refresh: string) => {
    authStorage.setTokens(access, refresh);
  }, []);

  const router = useRouter();

  const logout = useCallback(() => {
    authStorage.setLogoutFlag();
    authStorage.clearTokens();
    broadcastLogout();
    setState({ user: null, loading: false, error: null });
    setSessionExpiredOpen(false);
    setSessionWarningOpen(false);
    if (pendingResolveRef.current) {
      pendingResolveRef.current(false);
      pendingResolveRef.current = null;
    }
  }, []);

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
        } else {
          authStorage.clearTokens();
          setState({ user: null, loading: false, error: null });
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
      } else {
        authStorage.clearTokens();
        setState({ user: null, loading: false, error: null });
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
        pendingResolveRef.current = resolve;
        setSessionExpiredOpen(true);
      });
    };
    registerSessionExpiredHandler(handler);
    return () => {
      unregisterSessionExpiredHandler();
      if (pendingResolveRef.current) {
        pendingResolveRef.current(false);
        pendingResolveRef.current = null;
      }
    };
  }, []);

  const handleSessionExpiredSuccess = useCallback(() => {
    if (pendingResolveRef.current) {
      pendingResolveRef.current(true);
      pendingResolveRef.current = null;
    }
    refreshUser();
  }, [refreshUser]);

  const handleSessionExpiredCancel = useCallback(() => {
    if (pendingResolveRef.current) {
      pendingResolveRef.current(false);
      pendingResolveRef.current = null;
    }
  }, []);

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

  // Proactive refresh (only when user active) + session warning when expiring soon
  useEffect(() => {
    const run = () => {
      if (typeof document === "undefined") return;
      const token = authStorage.getToken();
      if (!token) return;

      const exp = getJwtExpiry(token);
      const expiresAtMs = exp != null ? exp * 1000 : 0;
      const msLeft = expiresAtMs - Date.now();

      // Show warning when token expires in < 5 min and tab visible; hide if already expired
      if (msLeft <= 0) {
        setSessionWarningOpen(false);
      } else if (document.visibilityState === "visible" && msLeft < SESSION_WARNING_THRESHOLD_MS) {
        const minutesLeft = Math.max(1, Math.ceil(msLeft / 60000));
        setSessionWarningMinutesLeft(minutesLeft);
        setSessionWarningOpen(true);
      }

      // Proactive refresh only when user active and token expires soon
      if (
        document.visibilityState === "visible" &&
        isUserActive() &&
        shouldProactivelyRefresh(token)
      ) {
        setSessionWarningOpen(false); // Dismiss warning if we're refreshing
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
  }, [refreshUser]);

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
        minutesLeft={sessionWarningMinutesLeft}
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
