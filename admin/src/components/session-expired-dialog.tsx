"use client";

import { useState, useCallback } from "react";
import Image from "next/image";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { api, authStorage } from "@/lib/api";
import { isAdminRole } from "@/lib/admin-roles";
import { getApiErrorMessage } from "@/lib/utils";
import type { CurrentUserResponse } from "@/types/api";

interface SessionExpiredDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** Called with /me payload after tokens are stored — parent must not call /me again (avoids race that cleared tokens). */
  onSuccess: (user: CurrentUserResponse) => void;
  onCancel?: () => void;
}

export function SessionExpiredDialog({
  open,
  onOpenChange,
  onSuccess,
  onCancel,
}: SessionExpiredDialogProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      setError(null);
      setLoading(true);
      try {
        const tokens = await api.login(email, password);
        authStorage.setTokens(tokens.accessToken, tokens.refreshToken);
        const me = await api.getMe();
        if (!isAdminRole(me.role)) {
          authStorage.clearTokens();
          setError("An admin role is required to sign in.");
          return;
        }
        onSuccess(me);
        onOpenChange(false);
        setEmail("");
        setPassword("");
      } catch (err) {
        setError(getApiErrorMessage(err, "Unable to sign in. Please try again."));
      } finally {
        setLoading(false);
      }
    },
    [email, password, onSuccess, onOpenChange]
  );

  const handleCancel = useCallback(() => {
    onCancel?.();
    onOpenChange(false);
    setEmail("");
    setPassword("");
    setError(null);
  }, [onCancel, onOpenChange]);

  return (
    <Dialog open={open} onOpenChange={(o) => !o && handleCancel()}>
      <DialogContent className="sm:max-w-md" onPointerDownOutside={(e) => e.preventDefault()}>
        <DialogHeader>
          <div className="mx-auto mb-2 flex justify-center">
            <Image
              src="/tamixa-app-icon.png"
              alt="Tamixa"
              width={64}
              height={64}
              className="h-16 w-16 object-contain"
            />
          </div>
          <DialogTitle className="text-center">Session expired</DialogTitle>
          <DialogDescription className="text-center">
            Your session has expired. Please sign in again to continue.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="space-y-4 pt-2">
          {error && (
            <p className="rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive">{error}</p>
          )}
          <div className="space-y-2">
            <Label htmlFor="session-email">Email</Label>
            <Input
              id="session-email"
              type="email"
              placeholder="admin@techadivas.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
              disabled={loading}
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="session-password">Password</Label>
            <Input
              id="session-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
              disabled={loading}
            />
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={handleCancel} disabled={loading}>
              Cancel
            </Button>
            <Button type="submit" disabled={loading}>
              {loading ? "Signing in…" : "Sign in"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
