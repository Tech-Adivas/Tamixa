import React, { createContext, useContext, useState, useEffect, useCallback, useRef } from "react";
import {
  getMe,
  authStorage,
  clearStoredTokens,
  getStoredTokenExpiresAt,
  performTokenRefresh,
  type CurrentUser,
} from "../lib/api";
import { logger } from "../lib/logger";

interface AuthState {
  user: CurrentUser | null;
  loading: boolean;
  isAuthenticated: boolean;
}

interface AuthContextValue extends AuthState {
  logout: () => void;
  setUser: (user: CurrentUser | null) => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

const REFRESH_BEFORE_EXPIRY_MS = 60_000; // refresh 60s before token expires

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);
  const refreshTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const scheduleProactiveRefresh = useCallback(() => {
    if (refreshTimerRef.current) clearTimeout(refreshTimerRef.current);
    const expiresAt = getStoredTokenExpiresAt();
    if (!expiresAt) return;
    const delay = expiresAt - Date.now() - REFRESH_BEFORE_EXPIRY_MS;
    if (delay <= 0) return; // already expired or too close — let 401 handler deal with it
    refreshTimerRef.current = setTimeout(async () => {
      try {
        const newToken = await performTokenRefresh();
        if (newToken) scheduleProactiveRefresh();
        // On null: token already expired or no refresh token — 401 handler in fetchWithAuth recovers
      } catch {
        // Network error — silently ignore; fetchWithAuth 401 retry will handle it
      }
    }, delay);
  }, []);

  const logout = useCallback(() => {
    if (refreshTimerRef.current) clearTimeout(refreshTimerRef.current);
    clearStoredTokens();
    setUser(null);
  }, []);

  useEffect(() => {
    const token = authStorage.getToken();
    if (!token) {
      setLoading(false);
      return;
    }
    getMe()
      .then((u) => {
        setUser(u);
        scheduleProactiveRefresh();
      })
      .catch((err) => {
        logger.warn("auth", "Failed to restore session", { message: err instanceof Error ? err.message : String(err) });
        clearStoredTokens();
        setUser(null);
      })
      .finally(() => setLoading(false));

    return () => {
      if (refreshTimerRef.current) clearTimeout(refreshTimerRef.current);
    };
  }, [scheduleProactiveRefresh]);

  const value: AuthContextValue = {
    user,
    loading,
    isAuthenticated: !!user,
    logout,
    setUser,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
