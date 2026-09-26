"use client";

import { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { AudioPreviewPlayer } from "@/components/audio-preview-player";
import { api } from "@/lib/api";
import { adminStoryLanguageLabel } from "@/lib/library-story-workflow";

interface AudioPreviewModalProps {
  /** Whether the modal is open */
  open: boolean;
  /** Callback when modal open state changes */
  onOpenChange: (open: boolean) => void;
  /** Story ID */
  storyId: number;
  /** Story title */
  storyTitle: string;
  /** Language code */
  language: string;
  /** Audio duration in seconds (optional) */
  duration?: number;
  /** File size in bytes (optional) */
  fileSize?: number;
  /** Generation timestamp (optional) */
  generatedAt?: string;
  /** Callback when preview error occurs */
  onError?: (error: string) => void;
}

/**
 * Audio Preview Modal Component
 * 
 * Displays a full-featured audio player in a modal dialog.
 * Fetches audio from the API and provides rich playback controls.
 * 
 * Follows Tamixa design tokens and professional standards.
 */
export function AudioPreviewModal({
  open,
  onOpenChange,
  storyId,
  storyTitle,
  language,
  duration,
  fileSize,
  generatedAt,
  onError,
}: AudioPreviewModalProps) {
  const [audioBlob, setAudioBlob] = useState<Blob | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Fetch audio when modal opens
  useEffect(() => {
    if (!open) {
      // Reset state when modal closes
      setAudioBlob(null);
      setError(null);
      return;
    }

    // Fetch audio blob
    const fetchAudio = async () => {
      setIsLoading(true);
      setError(null);
      try {
        const blob = await api.admin.getLibraryStoryPreviewAudioBlob(storyId, language);
        setAudioBlob(blob);
      } catch (err) {
        const errorMsg = err instanceof Error ? err.message : "Failed to load audio";
        setError(errorMsg);
        onError?.(errorMsg);
      } finally {
        setIsLoading(false);
      }
    };

    fetchAudio();
  }, [open, storyId, language, onError]);

  const languageLabel = adminStoryLanguageLabel(language);

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px]">
        <DialogHeader>
          <DialogTitle className="pr-8">Audio Preview: {storyTitle}</DialogTitle>
        </DialogHeader>

        <div className="space-y-4 py-2">
          {/* Loading State */}
          {isLoading && (
            <div className="flex items-center justify-center py-12 text-sm text-muted-foreground">
              <span className="animate-spin mr-2">⏳</span>
              Loading audio...
            </div>
          )}

          {/* Error State */}
          {error && !isLoading && (
            <div className="bg-destructive/10 border border-destructive/30 rounded-lg p-4 text-sm text-destructive">
              {error}
            </div>
          )}

          {/* Audio Player */}
          {audioBlob && !isLoading && !error && (
            <AudioPreviewPlayer
              audioSource={audioBlob}
              language={language}
              languageLabel={languageLabel}
              duration={duration}
              fileSize={fileSize}
              generatedAt={generatedAt}
              onError={(err) => {
                setError(err);
                onError?.(err);
              }}
            />
          )}

          {/* Story Information */}
          <div className="bg-muted/50 rounded-lg p-4 space-y-2">
            <div className="text-sm">
              <span className="font-semibold text-foreground">Story:</span>{" "}
              <span className="text-muted-foreground">{storyTitle}</span>
            </div>
            <div className="text-sm">
              <span className="font-semibold text-foreground">Language:</span>{" "}
              <span className="text-muted-foreground">{languageLabel} ({language})</span>
            </div>
            <div className="text-sm">
              <span className="font-semibold text-foreground">Story ID:</span>{" "}
              <span className="text-muted-foreground font-mono">#{storyId}</span>
            </div>
          </div>
        </div>

        <div className="flex justify-end pt-2">
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Close
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
