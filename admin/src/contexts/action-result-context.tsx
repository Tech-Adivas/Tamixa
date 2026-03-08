"use client";

import React, { createContext, useCallback, useContext, useState } from "react";
import { CheckCircle2, XCircle, AlertTriangle, Info } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

type ActionResultType = "success" | "error" | "warning" | "info";

type ActionResult = {
  type: ActionResultType;
  title: string;
  message: string;
} | null;

type ActionResultContextValue = {
  showSuccess: (title: string, message: string) => void;
  showError: (title: string, message: string) => void;
  showWarning: (title: string, message: string) => void;
  showInfo: (title: string, message: string) => void;
};

const ActionResultContext = createContext<ActionResultContextValue | null>(null);

const icons = {
  success: CheckCircle2,
  error: XCircle,
  warning: AlertTriangle,
  info: Info,
};

const iconColors = {
  success: "text-primary",
  error: "text-destructive",
  warning: "text-muted-foreground",
  info: "text-primary",
};

const borderColors = {
  success: "border-primary/30",
  error: "border-destructive/50",
  warning: "border-border",
  info: "border-primary/30",
};

export function ActionResultProvider({ children }: { children: React.ReactNode }) {
  const [actionResult, setActionResult] = useState<ActionResult>(null);

  const showSuccess = useCallback((title: string, message: string) => {
    setActionResult({ type: "success", title, message });
  }, []);

  const showError = useCallback((title: string, message: string) => {
    setActionResult({ type: "error", title, message });
  }, []);

  const showWarning = useCallback((title: string, message: string) => {
    setActionResult({ type: "warning", title, message });
  }, []);

  const showInfo = useCallback((title: string, message: string) => {
    setActionResult({ type: "info", title, message });
  }, []);

  const value: ActionResultContextValue = {
    showSuccess,
    showError,
    showWarning,
    showInfo,
  };

  return (
    <ActionResultContext.Provider value={value}>
      {children}
      <Dialog open={actionResult !== null} onOpenChange={(open) => !open && setActionResult(null)}>
        <DialogContent
          className={cn(actionResult && borderColors[actionResult.type])}
        >
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              {actionResult && (
                <>
                  {React.createElement(icons[actionResult.type], {
                    className: cn("h-5 w-5", iconColors[actionResult.type]),
                  })}
                  {actionResult.title}
                </>
              )}
            </DialogTitle>
            <DialogDescription className="pt-1 text-foreground">
              {actionResult?.message}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button onClick={() => setActionResult(null)}>OK</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </ActionResultContext.Provider>
  );
}

export function useActionResult() {
  const ctx = useContext(ActionResultContext);
  if (!ctx) throw new Error("useActionResult must be used within ActionResultProvider");
  return ctx;
}
