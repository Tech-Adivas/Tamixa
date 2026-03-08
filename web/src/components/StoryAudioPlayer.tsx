/**
 * Global story audio player bar. Uses a single <audio> element in the DOM
 * so playback works reliably (parent sets src and calls play() on the ref).
 */
import { useEffect, useRef } from "react";

export interface StoryAudioPlayerProps {
  audioRef: React.RefObject<HTMLAudioElement | null>;
  isPlaying: boolean;
  isLoading: boolean;
  title: string | null;
  currentTime: number;
  duration: number;
  onPlayPause: () => void;
  onTimeUpdate: (currentTime: number) => void;
  onDurationChange: (duration: number) => void;
  onSeek: (seconds: number) => void;
}

function formatTime(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds < 0) return "0:00";
  const m = Math.floor(seconds / 60);
  const s = Math.floor(seconds % 60);
  return `${m}:${s.toString().padStart(2, "0")}`;
}

export default function StoryAudioPlayer({
  audioRef,
  isPlaying,
  isLoading,
  title,
  currentTime,
  duration,
  onPlayPause,
  onTimeUpdate,
  onDurationChange,
  onSeek,
}: StoryAudioPlayerProps) {
  const progressRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    const el = audioRef.current;
    if (!el) return;
    const onTimeUpdateEv = () => onTimeUpdate(el.currentTime);
    const onDurationChangeEv = () => onDurationChange(el.duration);
    el.addEventListener("timeupdate", onTimeUpdateEv);
    el.addEventListener("durationchange", onDurationChangeEv);
    return () => {
      el.removeEventListener("timeupdate", onTimeUpdateEv);
      el.removeEventListener("durationchange", onDurationChangeEv);
    };
  }, [audioRef, onTimeUpdate, onDurationChange]);

  const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    const v = parseFloat(e.target.value);
    if (Number.isFinite(v)) onSeek(v);
  };

  return (
    <>
      <audio ref={audioRef} preload="metadata" style={{ display: "none" }} />
      {title == null && !isLoading ? null : (
    <div className="story-audio-player" role="region" aria-label="Story playback">
      <p className="story-audio-player-title">{title ?? "Loading…"}</p>
      <div className="story-audio-player-controls">
        <button
          type="button"
          className="story-audio-player-btn"
          onClick={onPlayPause}
          disabled={isLoading}
          aria-label={isPlaying ? "Pause" : "Play"}
        >
          {isLoading ? "…" : isPlaying ? "⏸" : "▶"}
        </button>
        <div className="story-audio-player-progress-wrap">
          <span className="story-audio-player-time" aria-live="polite">
            {formatTime(currentTime)}
          </span>
          <input
            ref={progressRef}
            type="range"
            className="story-audio-player-progress"
            min={0}
            max={duration || 100}
            step={1}
            value={currentTime}
            onChange={handleSeek}
            aria-label="Playback position"
          />
          <span className="story-audio-player-time">{formatTime(duration)}</span>
        </div>
      </div>
    </div>
      )}
    </>
  );
}
