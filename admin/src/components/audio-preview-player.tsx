"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { Play, Pause, StopCircle, Volume2, VolumeX, SkipBack, SkipForward } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";

interface AudioPreviewPlayerProps {
  /** Audio blob or URL to play */
  audioSource: Blob | string;
  /** Language code for display */
  language: string;
  /** Language display name */
  languageLabel: string;
  /** Audio duration in seconds (optional, will be detected from audio) */
  duration?: number;
  /** File size in bytes (optional) */
  fileSize?: number;
  /** Generation timestamp (optional) */
  generatedAt?: string;
  /** Callback when playback ends */
  onEnded?: () => void;
  /** Callback when playback error occurs */
  onError?: (error: string) => void;
  /** Additional CSS classes */
  className?: string;
}

/**
 * Audio Preview Player Component
 * 
 * Provides a rich audio playback interface with:
 * - Play/pause/stop controls
 * - Seek bar with current time and duration
 * - Speed control (0.75x, 1x, 1.25x, 1.5x)
 * - Volume control
 * - Skip forward/backward (10 seconds)
 * - Audio metadata display
 * 
 * Follows Tamixa design tokens and professional standards.
 */
export function AudioPreviewPlayer({
  audioSource,
  language,
  languageLabel,
  duration: providedDuration,
  fileSize,
  generatedAt,
  onEnded,
  onError,
  className,
}: AudioPreviewPlayerProps) {
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [currentTime, setCurrentTime] = useState(0);
  const [duration, setDuration] = useState(providedDuration || 0);
  const [playbackRate, setPlaybackRate] = useState(1);
  const [volume, setVolume] = useState(1);
  const [isMuted, setIsMuted] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Initialize audio element
  useEffect(() => {
    const audio = new Audio();
    audioRef.current = audio;
    let currentObjectUrl: string | null = null;

    // Set up audio source
    if (audioSource instanceof Blob) {
      const url = URL.createObjectURL(audioSource);
      currentObjectUrl = url;
      audio.src = url;
    } else {
      audio.src = audioSource;
    }

    // Audio event listeners
    const handleLoadedMetadata = () => {
      setDuration(audio.duration);
      setIsLoading(false);
    };

    const handleTimeUpdate = () => {
      setCurrentTime(audio.currentTime);
    };

    const handleEnded = () => {
      setIsPlaying(false);
      setCurrentTime(0);
      onEnded?.();
    };

    const handleError = () => {
      const errorMsg = "Failed to load audio. Please try again.";
      setError(errorMsg);
      setIsLoading(false);
      onError?.(errorMsg);
    };

    const handleCanPlay = () => {
      setIsLoading(false);
    };

    audio.addEventListener("loadedmetadata", handleLoadedMetadata);
    audio.addEventListener("timeupdate", handleTimeUpdate);
    audio.addEventListener("ended", handleEnded);
    audio.addEventListener("error", handleError);
    audio.addEventListener("canplay", handleCanPlay);

    return () => {
      audio.removeEventListener("loadedmetadata", handleLoadedMetadata);
      audio.removeEventListener("timeupdate", handleTimeUpdate);
      audio.removeEventListener("ended", handleEnded);
      audio.removeEventListener("error", handleError);
      audio.removeEventListener("canplay", handleCanPlay);
      audio.pause();
      if (currentObjectUrl) {
        URL.revokeObjectURL(currentObjectUrl);
      }
    };
  }, [audioSource, onEnded, onError]);

  // Play/pause toggle
  const handlePlayPause = useCallback(() => {
    const audio = audioRef.current;
    if (!audio) return;

    if (isPlaying) {
      audio.pause();
      setIsPlaying(false);
    } else {
      audio.play().catch(() => {
        const errorMsg = "Failed to play audio. Please try again.";
        setError(errorMsg);
        onError?.(errorMsg);
      });
      setIsPlaying(true);
    }
  }, [isPlaying, onError]);

  // Stop playback
  const handleStop = useCallback(() => {
    const audio = audioRef.current;
    if (!audio) return;

    audio.pause();
    audio.currentTime = 0;
    setIsPlaying(false);
    setCurrentTime(0);
  }, []);

  // Seek to position
  const handleSeek = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const audio = audioRef.current;
    if (!audio) return;

    const newTime = parseFloat(e.target.value);
    audio.currentTime = newTime;
    setCurrentTime(newTime);
  }, []);

  // Skip forward/backward
  const handleSkip = useCallback((seconds: number) => {
    const audio = audioRef.current;
    if (!audio) return;

    const newTime = Math.max(0, Math.min(duration, audio.currentTime + seconds));
    audio.currentTime = newTime;
    setCurrentTime(newTime);
  }, [duration]);

  // Change playback speed
  const handleSpeedChange = useCallback((rate: number) => {
    const audio = audioRef.current;
    if (!audio) return;

    audio.playbackRate = rate;
    setPlaybackRate(rate);
  }, []);

  // Toggle mute
  const handleMuteToggle = useCallback(() => {
    const audio = audioRef.current;
    if (!audio) return;

    audio.muted = !isMuted;
    setIsMuted(!isMuted);
  }, [isMuted]);

  // Change volume
  const handleVolumeChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const audio = audioRef.current;
    if (!audio) return;

    const newVolume = parseFloat(e.target.value);
    audio.volume = newVolume;
    setVolume(newVolume);
    if (newVolume > 0 && isMuted) {
      audio.muted = false;
      setIsMuted(false);
    }
  }, [isMuted]);

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

  const speedOptions = [0.75, 1, 1.25, 1.5];

  return (
    <Card className={cn("overflow-hidden", className)}>
      <CardContent className="p-4 space-y-4">
        {/* Header: Language and Metadata */}
        <div className="flex items-start justify-between">
          <div>
            <h3 className="font-semibold text-base text-foreground">{languageLabel}</h3>
            <p className="text-xs text-muted-foreground mt-0.5">Language: {language}</p>
          </div>
          <div className="text-right text-xs text-muted-foreground space-y-0.5">
            {duration > 0 && <div>Duration: {formatTime(duration)}</div>}
            {fileSize && <div>Size: {formatFileSize(fileSize)}</div>}
            {generatedAt && <div>Generated: {generatedAt}</div>}
          </div>
        </div>

        {/* Error State */}
        {error && (
          <div className="bg-destructive/10 border border-destructive/30 rounded-lg p-3 text-sm text-destructive">
            {error}
          </div>
        )}

        {/* Loading State */}
        {isLoading && !error && (
          <div className="flex items-center justify-center py-4 text-sm text-muted-foreground">
            Loading audio...
          </div>
        )}

        {/* Player Controls */}
        {!isLoading && !error && (
          <>
            {/* Progress Bar */}
            <div className="space-y-2">
              <input
                type="range"
                min="0"
                max={duration || 0}
                value={currentTime}
                onChange={handleSeek}
                className="w-full h-2 bg-muted rounded-full appearance-none cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-4 [&::-webkit-slider-thumb]:h-4 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-primary [&::-webkit-slider-thumb]:cursor-pointer [&::-moz-range-thumb]:w-4 [&::-moz-range-thumb]:h-4 [&::-moz-range-thumb]:rounded-full [&::-moz-range-thumb]:bg-primary [&::-moz-range-thumb]:border-0 [&::-moz-range-thumb]:cursor-pointer"
                style={{
                  background: `linear-gradient(to right, hsl(var(--primary)) 0%, hsl(var(--primary)) ${(currentTime / duration) * 100}%, hsl(var(--muted)) ${(currentTime / duration) * 100}%, hsl(var(--muted)) 100%)`,
                }}
                disabled={!duration}
              />
              <div className="flex items-center justify-between text-xs text-muted-foreground">
                <span>{formatTime(currentTime)}</span>
                <span>{formatTime(duration)}</span>
              </div>
            </div>

            {/* Playback Controls */}
            <div className="flex items-center justify-center gap-2">
              <Button
                variant="outline"
                size="icon-sm"
                onClick={() => handleSkip(-10)}
                disabled={!duration}
                title="Skip backward 10s"
              >
                <SkipBack className="h-4 w-4" />
              </Button>

              <Button
                variant="default"
                size="icon"
                onClick={handlePlayPause}
                disabled={!duration}
                title={isPlaying ? "Pause" : "Play"}
              >
                {isPlaying ? <Pause className="h-5 w-5" /> : <Play className="h-5 w-5" />}
              </Button>

              <Button
                variant="outline"
                size="icon-sm"
                onClick={handleStop}
                disabled={!duration || currentTime === 0}
                title="Stop"
              >
                <StopCircle className="h-4 w-4" />
              </Button>

              <Button
                variant="outline"
                size="icon-sm"
                onClick={() => handleSkip(10)}
                disabled={!duration}
                title="Skip forward 10s"
              >
                <SkipForward className="h-4 w-4" />
              </Button>
            </div>

            {/* Speed and Volume Controls */}
            <div className="flex items-center justify-between gap-4 pt-2 border-t">
              {/* Speed Control */}
              <div className="flex items-center gap-2">
                <span className="text-xs text-muted-foreground whitespace-nowrap">Speed:</span>
                <div className="flex gap-1">
                  {speedOptions.map((rate) => (
                    <button
                      key={rate}
                      onClick={() => handleSpeedChange(rate)}
                      className={cn(
                        "px-2 py-1 text-xs rounded-md transition-colors",
                        playbackRate === rate
                          ? "bg-primary text-primary-foreground font-semibold"
                          : "bg-muted text-muted-foreground hover:bg-muted/80"
                      )}
                      disabled={!duration}
                    >
                      {rate}x
                    </button>
                  ))}
                </div>
              </div>

              {/* Volume Control */}
              <div className="flex items-center gap-2">
                <button
                  onClick={handleMuteToggle}
                  className="text-muted-foreground hover:text-foreground transition-colors"
                  title={isMuted ? "Unmute" : "Mute"}
                >
                  {isMuted ? <VolumeX className="h-4 w-4" /> : <Volume2 className="h-4 w-4" />}
                </button>
                <input
                  type="range"
                  min="0"
                  max="1"
                  step="0.1"
                  value={isMuted ? 0 : volume}
                  onChange={handleVolumeChange}
                  className="w-20 h-1.5 bg-muted rounded-full appearance-none cursor-pointer [&::-webkit-slider-thumb]:appearance-none [&::-webkit-slider-thumb]:w-3 [&::-webkit-slider-thumb]:h-3 [&::-webkit-slider-thumb]:rounded-full [&::-webkit-slider-thumb]:bg-primary [&::-webkit-slider-thumb]:cursor-pointer [&::-moz-range-thumb]:w-3 [&::-moz-range-thumb]:h-3 [&::-moz-range-thumb]:rounded-full [&::-moz-range-thumb]:bg-primary [&::-moz-range-thumb]:border-0 [&::-moz-range-thumb]:cursor-pointer"
                  disabled={!duration}
                />
              </div>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  );
}
