"use client";

import { useState, Suspense } from "react";
import Image from "next/image";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { api, authStorage } from "@/lib/api";
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

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { refreshUser } = useAuth();
  const { showSuccess, showError } = useActionResult();
  const errorParam = searchParams.get("error");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);

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
      showError("Login failed", err instanceof Error ? err.message : "Unable to sign in. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-muted/50 p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="pb-4">
          <div className="mb-4 flex items-center gap-3">
            <Image
              src="/araro-logo.svg"
              alt="Araro"
              width={120}
              height={32}
              className="h-8 w-auto"
              priority
            />
            <div>
              <CardTitle className="text-xl font-semibold tracking-tight">Admin</CardTitle>
              <CardDescription>Enterprise administration portal</CardDescription>
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {errorParam === "forbidden" && (
            <p className="mb-4 text-sm text-destructive">
              Only administrators can access this dashboard.
            </p>
          )}
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                placeholder="admin@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                autoComplete="email"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                autoComplete="current-password"
              />
            </div>
            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? "Signing in…" : "Sign in"}
            </Button>
          </form>
          <p className="mt-4 text-center text-sm text-muted-foreground">
            Admin uses email + password (same backend as web). Ensure the backend is running and the user has an admin role.
          </p>
          <p className="mt-2 text-center text-xs text-muted-foreground">
            Local dev: set <code className="rounded bg-muted px-1">SEED_ADMIN_ENABLED=true</code>, then POST <code className="rounded bg-muted px-1">/api/v1/dev/seed-admin</code>. Sign in with <strong>admin@techadivas.com</strong> / <strong>Admin123!</strong>
          </p>
          <p className="mt-4 border-t border-border pt-4 text-center text-sm text-muted-foreground">
            Back to{" "}
            <Link href="/" className="font-medium text-primary underline-offset-2 hover:underline">
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
        <div className="flex min-h-screen items-center justify-center bg-muted/20">
          <div className="flex flex-col items-center gap-3">
            <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
            <span className="text-sm text-muted-foreground">Loading…</span>
          </div>
        </div>
      }
    >
      <LoginForm />
    </Suspense>
  );
}
