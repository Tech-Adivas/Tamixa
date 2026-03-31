/**
 * Global story audio player bar. Uses a single <audio> element in the DOM
 * so playback works reliably (parent sets src and calls play() on the ref).
 * When avatarVideoUrl is provided, shows a small video preview and uses the video element for playback (video includes audio).
 */
import { useEffect, useLayoutEffect, useRef } from "react";

export interface StoryAudioPlayerProps {
  audioRef: React.RefObject<HTMLAudioElement | null>;
  /** When set, playback is from this video (talking-head avatar); video ref is used for time/seek. */
  videoRef?: React.RefObject<HTMLVideoElement | null>;
  /** Resolved URL for avatar video; when set, player shows video and uses it for playback. */
  avatarVideoUrl?: string | null;
  /** Optional muted loop alongside audio-only playback (Phase 4 host clip). */
  hostClipUrl?: string | null;
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
  videoRef,
  avatarVideoUrl,
  hostClipUrl,
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
  const hostClipRef = useRef<HTMLVideoElement | null>(null);

  useEffect(() => {
    const clip = hostClipRef.current;
    if (!clip || !hostClipUrl || avatarVideoUrl) return;
    clip.muted = true;
    clip.loop = true;
    clip.playsInline = true;
    if (isPlaying) {
      clip.play().catch(() => {});
    } else {
      clip.pause();
    }
  }, [hostClipUrl, avatarVideoUrl, isPlaying]);

  useEffect(() => {
    if (!hostClipUrl || avatarVideoUrl || typeof document === "undefined") return;
    const onVisibility = () => {
      const clip = hostClipRef.current;
      if (!clip) return;
      if (document.hidden) {
        clip.pause();
      } else if (isPlaying) {
        clip.play().catch(() => {});
      }
    };
    document.addEventListener("visibilitychange", onVisibility);
    return () => document.removeEventListener("visibilitychange", onVisibility);
  }, [hostClipUrl, avatarVideoUrl, isPlaying]);

  useEffect(() => {
    if (avatarVideoUrl && videoRef?.current) return;
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
  }, [audioRef, avatarVideoUrl, videoRef, onTimeUpdate, onDurationChange]);

  useLayoutEffect(() => {
    if (!avatarVideoUrl || !videoRef?.current) return;
    const el = videoRef.current;
    const onTimeUpdateEv = () => onTimeUpdate(el.currentTime);
    const onDurationChangeEv = () => onDurationChange(el.duration);
    el.addEventListener("timeupdate", onTimeUpdateEv);
    el.addEventListener("durationchange", onDurationChangeEv);
    return () => {
      el.removeEventListener("timeupdate", onTimeUpdateEv);
      el.removeEventListener("durationchange", onDurationChangeEv);
    };
  }, [avatarVideoUrl, videoRef, onTimeUpdate, onDurationChange]);

  const handleSeek = (e: React.ChangeEvent<HTMLInputElement>) => {
    const v = parseFloat(e.target.value);
    if (!Number.isFinite(v)) return;
    onSeek(v);
    if (avatarVideoUrl && videoRef?.current) videoRef.current.currentTime = v;
    else if (audioRef.current) audioRef.current.currentTime = v;
  };

  return (
    <>
      <audio ref={audioRef as React.RefObject<HTMLAudioElement>} preload="metadata" style={{ display: "none" }} />
      {title == null && !isLoading ? null : (
    <div className="story-audio-player" role="region" aria-label="Story playback">
      {avatarVideoUrl && videoRef && (
        <div className="story-audio-player-avatar-wrap" style={{ marginBottom: "0.5rem", borderRadius: 8, overflow: "hidden", maxWidth: 200, aspectRatio: "1" }}>
          <video ref={videoRef as React.RefObject<HTMLVideoElement>} src={avatarVideoUrl} playsInline style={{ width: "100%", height: "100%", objectFit: "cover" }} aria-hidden />
        </div>
      )}
      {hostClipUrl && !avatarVideoUrl && (
        <div
          className="story-audio-player-host-clip-wrap"
          style={{ marginBottom: "0.5rem", borderRadius: 8, overflow: "hidden", maxWidth: 360, aspectRatio: "16 / 9" }}
        >
          <video
            ref={hostClipRef}
            src={hostClipUrl}
            muted
            loop
            playsInline
            preload="metadata"
            style={{ width: "100%", height: "100%", objectFit: "cover" }}
            aria-hidden
          />
        </div>
      )}
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
