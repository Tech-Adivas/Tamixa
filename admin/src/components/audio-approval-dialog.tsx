"use client";

import { useState } from "react";
import { CheckCircle, XCircle, AlertCircle } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";

interface AudioApprovalDialogProps {
  /** Whether the dialog is open */
  open: boolean;
  /** Callback when dialog open state changes */
  onOpenChange: (open: boolean) => void;
  /** Story ID */
  storyId: number;
  /** Story title */
  storyTitle: string;
  /** Language code */
  language: string;
  /** Language display name */
  languageLabel: string;
  /** Audio duration in seconds */
  duration?: number;
  /** File size in bytes */
  fileSize?: number;
  /** Generation timestamp */
  generatedAt?: string;
  /** Callback when audio is approved */
  onApprove: (storyId: number, language: string, notes?: string) => Promise<void>;
  /** Callback when audio is rejected */
  onReject?: (storyId: number, language: string, reason: string) => Promise<void>;
  /** Whether approval is in progress */
  isApproving?: boolean;
  /** Whether rejection is in progress */
  isRejecting?: boolean;
}

/**
 * Audio Approval Dialog Component
 * 
 * Provides a confirmation dialog for approving or rejecting audio narrations.
 * Includes:
 * - Audio metadata display
 * - Quality checklist (optional)
 * - Approval notes (optional)
 * - Rejection reason (required for rejection)
 * - Approve/reject actions with confirmation
 * 
 * Follows Tamixa design tokens and professional standards.
 */
export function AudioApprovalDialog({
  open,
  onOpenChange,
  storyId,
  storyTitle,
  language,
  languageLabel,
  duration,
  fileSize,
  generatedAt,
  onApprove,
  onReject,
  isApproving = false,
  isRejecting = false,
}: AudioApprovalDialogProps) {
  const [action, setAction] = useState<"approve" | "reject" | null>(null);
  const [notes, setNotes] = useState("");
  const [rejectionReason, setRejectionReason] = useState("");
  const [error, setError] = useState<string | null>(null);

  // Format time as MM:SS
  const formatTime = (seconds: number): string => {
    if (!isFinite(seconds)) return "0:00";
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, "0")}`;
  };

  // Format file size
  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  // Handle approve action
  const handleApprove = async () => {
    setError(null);
    try {
      await onApprove(storyId, language, notes.trim() || undefined);
      // Reset state and close dialog
      setAction(null);
      setNotes("");
      setRejectionReason("");
      onOpenChange(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to approve audio");
    }
  };

  // Handle reject action
  const handleReject = async () => {
    if (!rejectionReason.trim()) {
      setError("Please provide a reason for rejection");
      return;
    }

    setError(null);
    try {
      if (onReject) {
        await onReject(storyId, language, rejectionReason.trim());
      }
      // Reset state and close dialog
      setAction(null);
      setNotes("");
      setRejectionReason("");
      onOpenChange(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to reject audio");
    }
  };

  // Handle cancel
  const handleCancel = () => {
    setAction(null);
    setNotes("");
    setRejectionReason("");
    setError(null);
    onOpenChange(false);
  };

  // Show initial confirmation view
  const showInitialView = action === null;
  const showApprovalView = action === "approve";
  const showRejectionView = action === "reject";

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>
            {showInitialView && "Audio Narration Review"}
            {showApprovalView && "Approve Audio Narration"}
            {showRejectionView && "Reject Audio Narration"}
          </DialogTitle>
          <DialogDescription>
            {showInitialView && "Review the audio narration and choose an action."}
            {showApprovalView && "Confirm approval to make this audio available in the mobile app."}
            {showRejectionView && "Provide a reason for rejection to help improve future generations."}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          {/* Story and Audio Metadata */}
          <div className="space-y-2">
            <div className="flex items-start justify-between gap-4">
              <div className="flex-1 min-w-0">
                <Label className="text-sm font-semibold text-foreground">Story</Label>
                <p className="text-sm text-muted-foreground truncate">{storyTitle}</p>
              </div>
              <div className="text-right">
                <Label className="text-sm font-semibold text-foreground">Language</Label>
                <p className="text-sm text-muted-foreground">{languageLabel}</p>
              </div>
            </div>

            {(duration || fileSize || generatedAt) && (
              <div className="grid grid-cols-3 gap-2 pt-2 border-t text-xs">
                {duration && (
                  <div>
                    <Label className="text-xs text-muted-foreground">Duration</Label>
                    <p className="font-medium">{formatTime(duration)}</p>
                  </div>
                )}
                {fileSize && (
                  <div>
                    <Label className="text-xs text-muted-foreground">File Size</Label>
                    <p className="font-medium">{formatFileSize(fileSize)}</p>
                  </div>
                )}
                {generatedAt && (
                  <div>
                    <Label className="text-xs text-muted-foreground">Generated</Label>
                    <p className="font-medium">{generatedAt}</p>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Approval Notes (optional) */}
          {showApprovalView && (
            <div className="space-y-2">
              <Label htmlFor="approval-notes" className="text-sm font-medium">
                Notes (Optional)
              </Label>
              <textarea
                id="approval-notes"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Add any notes about this audio narration..."
                className="w-full min-h-[80px] px-3 py-2 text-sm rounded-lg border border-input bg-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 resize-none"
                disabled={isApproving}
              />
            </div>
          )}

          {/* Rejection Reason (required) */}
          {showRejectionView && (
            <div className="space-y-2">
              <Label htmlFor="rejection-reason" className="text-sm font-medium">
                Reason for Rejection <span className="text-destructive">*</span>
              </Label>
              <textarea
                id="rejection-reason"
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                placeholder="Explain why this audio is being rejected (e.g., poor quality, incorrect pronunciation, technical issues)..."
                className="w-full min-h-[100px] px-3 py-2 text-sm rounded-lg border border-input bg-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 resize-none"
                disabled={isRejecting}
              />
              {!rejectionReason.trim() && (
                <p className="text-xs text-muted-foreground">
                  A rejection reason is required to help improve future audio generations.
                </p>
              )}
            </div>
          )}

          {/* Error Message */}
          {error && (
            <div className="flex items-start gap-2 bg-destructive/10 border border-destructive/30 rounded-lg p-3">
              <AlertCircle className="h-4 w-4 text-destructive shrink-0 mt-0.5" />
              <p className="text-sm text-destructive">{error}</p>
            </div>
          )}

          {/* Quality Checklist (informational) */}
          {showInitialView && (
            <div className="bg-muted/50 rounded-lg p-3 space-y-2">
              <Label className="text-sm font-semibold">Quality Checklist</Label>
              <ul className="text-xs text-muted-foreground space-y-1 list-disc list-inside">
                <li>Audio plays without errors or interruptions</li>
                <li>Narration matches the story content</li>
                <li>Pronunciation is clear and accurate</li>
                <li>Audio quality is acceptable (no distortion, clipping)</li>
                <li>Duration is appropriate for the story length</li>
              </ul>
            </div>
          )}
        </div>

        <DialogFooter>
          {showInitialView && (
            <>
              <Button
                variant="outline"
                onClick={handleCancel}
                disabled={isApproving || isRejecting}
              >
                Cancel
              </Button>
              {onReject && (
                <Button
                  variant="destructive"
                  onClick={() => setAction("reject")}
                  disabled={isApproving || isRejecting}
                >
                  <XCircle className="h-4 w-4 mr-2" />
                  Reject
                </Button>
              )}
              <Button
                variant="default"
                onClick={() => setAction("approve")}
                disabled={isApproving || isRejecting}
              >
                <CheckCircle className="h-4 w-4 mr-2" />
                Approve
              </Button>
            </>
          )}

          {showApprovalView && (
            <>
              <Button
                variant="outline"
                onClick={() => setAction(null)}
                disabled={isApproving}
              >
                Back
              </Button>
              <Button
                variant="default"
                onClick={handleApprove}
                disabled={isApproving}
              >
                {isApproving ? (
                  <>
                    <span className="animate-spin mr-2">⏳</span>
                    Approving...
                  </>
                ) : (
                  <>
                    <CheckCircle className="h-4 w-4 mr-2" />
                    Confirm Approval
                  </>
                )}
              </Button>
            </>
          )}

          {showRejectionView && (
            <>
              <Button
                variant="outline"
                onClick={() => setAction(null)}
                disabled={isRejecting}
              >
                Back
              </Button>
              <Button
                variant="destructive"
                onClick={handleReject}
                disabled={isRejecting || !rejectionReason.trim()}
              >
                {isRejecting ? (
                  <>
                    <span className="animate-spin mr-2">⏳</span>
                    Rejecting...
                  </>
                ) : (
                  <>
                    <XCircle className="h-4 w-4 mr-2" />
                    Confirm Rejection
                  </>
                )}
              </Button>
            </>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
