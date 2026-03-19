"use client";

import { useState, useEffect, Suspense } from "react";
import Image from "next/image";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { api, authStorage, checkBackendHealth, getApiBaseUrl } from "@/lib/api";
import { useAuth } from "@/contexts/auth-context";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { useActionResult } from "@/contexts/action-result-context";
import { isAdminRole } from "@/lib/admin-roles";
import { getApiErrorMessage } from "@/lib/utils";

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { refreshUser } = useAuth();
  const { showSuccess, showError } = useActionResult();
  const errorParam = searchParams.get("error");
  const expiredParam = searchParams.get("expired");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [backendOk, setBackendOk] = useState<boolean | null>(null);
  const [apiBaseUrl, setApiBaseUrl] = useState("");

  useEffect(() => {
    checkBackendHealth().then(({ ok, baseUrl }) => {
      setBackendOk(ok);
      setApiBaseUrl(baseUrl || getApiBaseUrl());
    });
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      const tokens = await api.login(email, password);
      authStorage.setTokens(tokens.accessToken, tokens.refreshToken);
      const me = await api.getMe();
      if (!isAdminRole(me.role)) {
        authStorage.clearTokens();
        showError("Access denied", "An admin role is required to sign in.");
        return;
      }
      await refreshUser();
      showSuccess("Signed in", "You have been successfully signed in.");
      router.push("/dashboard");
    } catch (err) {
      showError("Login failed", getApiErrorMessage(err, "Unable to sign in. Please try again."));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-tamixa-page p-4">
      <Card className="w-full max-w-md border border-border bg-card shadow-elevated">
        <CardHeader className="space-y-1 pb-6 text-center">
          <div className="mx-auto mb-2 flex justify-center">
            <Image
              src="/tamixa-logo.svg"
              alt="Tamixa"
              width={180}
              height={50}
              className="h-12 w-auto"
              priority
            />
          </div>
          <p className="text-sm font-semibold text-primary">Listen • Learn • Shine</p>
          <CardTitle className="text-2xl font-bold tracking-tight text-foreground">
            Tamixa Admin
          </CardTitle>
          <CardDescription className="text-muted-foreground">
            Enterprise administration portal
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          {expiredParam === "1" && (
            <p className="rounded-xl bg-amber-500/15 px-4 py-3 text-sm font-medium text-amber-700 dark:text-amber-400">
              Your session expired. Please sign in again.
            </p>
          )}
          {errorParam === "forbidden" && (
            <p className="rounded-xl bg-destructive/10 px-4 py-3 text-sm font-medium text-destructive">
              Only administrators can access this dashboard.
            </p>
          )}
          {backendOk === false && (
            <p className="rounded-xl bg-amber-500/15 px-4 py-3 text-sm font-medium text-amber-700 dark:text-amber-400">
              Cannot reach backend at <code className="rounded bg-muted px-1">{apiBaseUrl || "API"}</code>. Start the backend (e.g. <code className="rounded bg-muted px-1">./gradlew :backend:bootRun -Ptamixa.backendOnly=true</code>) and ensure <code className="rounded bg-muted px-1">NEXT_PUBLIC_API_URL</code> points to it if needed.
            </p>
          )}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="email" className="font-semibold text-foreground">
                Email
              </Label>
              <Input
                id="email"
                type="email"
                placeholder="admin@techadivas.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
                className="h-12 rounded-xl border-2 focus-visible:ring-2"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password" className="font-semibold text-foreground">
                Password
              </Label>
              <Input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="current-password"
                className="h-12 rounded-xl border-2 focus-visible:ring-2"
              />
            </div>
            <Button
              type="submit"
              variant="primary"
              size="lg"
              className="w-full rounded-xl"
              disabled={loading}
            >
              {loading ? "Signing in…" : "Sign in"}
            </Button>
          </form>
          <p className="text-center text-sm text-muted-foreground">
            Admin uses email + password (same backend as web). Ensure the backend is running and the user has an admin role.
          </p>
          <p className="text-center text-xs text-muted-foreground">
            Local dev: set <code className="rounded-lg bg-muted px-1.5 py-0.5 font-medium">SEED_ADMIN_ENABLED=true</code>, then POST <code className="rounded-lg bg-muted px-1.5 py-0.5 font-medium">/api/v1/dev/seed-admin</code>. Sign in with <strong>admin@techadivas.com</strong> / <strong>Admin123!</strong>
          </p>
          <p className="border-t-2 border-border pt-4 text-center text-sm text-muted-foreground">
            Back to{" "}
            <Link href="/" className="font-semibold text-primary underline-offset-2 hover:underline">
              home
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense
      fallback={
        <div className="flex min-h-screen items-center justify-center bg-tamixa-page">
          <div className="flex flex-col items-center gap-4">
            <div className="h-10 w-10 animate-spin rounded-full border-2 border-primary border-t-transparent" />
            <span className="text-sm font-medium text-muted-foreground">Loading…</span>
          </div>
        </div>
      }
    >
      <LoginForm />
    </Suspense>
  );
}
