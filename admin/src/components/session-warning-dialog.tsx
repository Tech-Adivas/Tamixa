"use client";

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Clock } from "lucide-react";
import { INACTIVITY_SESSION_WARNING_MS } from "@/lib/session-config";

const INACTIVITY_MINUTES = Math.round(INACTIVITY_SESSION_WARNING_MS / 60_000);

interface SessionWarningDialogProps {
  open: boolean;
  onStaySignedIn: () => void;
  onLogout: () => void;
}

export function SessionWarningDialog({
  open,
  onStaySignedIn,
  onLogout,
}: SessionWarningDialogProps) {
  return (
    <Dialog open={open} onOpenChange={() => {}}>
      <DialogContent
        className="sm:max-w-md"
        onPointerDownOutside={(e) => e.preventDefault()}
        onEscapeKeyDown={(e) => e.preventDefault()}
      >
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Clock className="h-5 w-5 text-amber-500" />
            Still there?
          </DialogTitle>
          <DialogDescription>
            You have been inactive for {INACTIVITY_MINUTES} minutes. Stay signed in to continue your session, or log
            out.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button type="button" variant="outline" onClick={onLogout}>
            Log out
          </Button>
          <Button type="button" onClick={onStaySignedIn}>
            Stay signed in
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
