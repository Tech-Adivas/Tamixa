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

interface SessionWarningDialogProps {
  open: boolean;
  minutesLeft: number;
  onStaySignedIn: () => void;
  onLogout: () => void;
}

export function SessionWarningDialog({
  open,
  minutesLeft,
  onStaySignedIn,
  onLogout,
}: SessionWarningDialogProps) {
  const message =
    minutesLeft <= 1
      ? "Your session will expire in less than a minute."
      : `Your session will expire in about ${minutesLeft} minutes due to inactivity.`;

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
            Session expiring soon
          </DialogTitle>
          <DialogDescription>{message} Stay signed in to continue.</DialogDescription>
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
