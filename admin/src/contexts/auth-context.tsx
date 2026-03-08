"use client";

import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import { api, authStorage, ApiError } from "@/lib/api";
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
  const retryTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const setTokens = useCallback((access: string, refresh: string) => {
    authStorage.setTokens(access, refresh);
  }, []);

  const logout = useCallback(() => {
    authStorage.clearTokens();
    setState({ user: null, loading: false, error: null });
  }, []);

  const fetchUser = useCallback(() => {
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
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
