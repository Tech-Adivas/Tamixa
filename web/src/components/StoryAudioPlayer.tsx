/**
 * Global story audio player bar. Uses a single <audio> element in the DOM
 * so playback works reliably (parent sets src and calls play() on the ref).
 * When avatarVideoUrl is provided, shows a small video preview and uses the video element for playback (video includes audio).
 */
import { useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";

export interface StoryAudioPlayerProps {
  audioRef: React.RefObject<HTMLAudioElement | null>;
  /** When set, playback is from this video (talking-head avatar); video ref is used for time/seek. */
  videoRef?: React.RefObject<HTMLVideoElement | null>;
  /** Resolved URL for avatar video; when set, player shows video and uses it for playback. */
  avatarVideoUrl?: string | null;
  /** Optional muted loop alongside audio-only playback (Phase 4 host clip). */
  hostClipUrl?: string | null;
  /** Browser SpeechSynthesis fallback (no scrub timeline). */
  isBrowserTts?: boolean;
  /** Scene boundaries as fractions of duration (0–1), e.g. from stream narrativeScenes. */
  chapterFractions?: number[] | null;
  /** Brief on-screen cue when the scene advances (Learn stories). */
  enableSceneReflectionCue?: boolean;
  isPlaying: boolean;
  isLoading: boolean;
  title: string | null;
  /** Optional line under the title (e.g. Learn focus or theme while listening). */
  subtitle?: string | null;
  /** Library interactive episode — parent-facing cue (pauses / choices). */
  practiceStoryChip?: boolean;
  /** Extra controls (e.g. per-story voice and play mode) rendered under the subtitle. */
  extras?: React.ReactNode;
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
  isBrowserTts = false,
  chapterFractions = null,
  enableSceneReflectionCue = false,
  isPlaying,
  isLoading,
  title,
  subtitle,
  practiceStoryChip = false,
  extras,
  currentTime,
  duration,
  onPlayPause,
  onTimeUpdate,
  onDurationChange,
  onSeek,
}: StoryAudioPlayerProps) {
  const progressRef = useRef<HTMLInputElement | null>(null);
  const hostClipRef = useRef<HTMLVideoElement | null>(null);
  const [reflectionCue, setReflectionCue] = useState(false);
  const prevSceneIdxRef = useRef<number | null>(null);
  const cueTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const sortedChapters = useMemo(
    () =>
      chapterFractions?.length
        ? [...new Set(chapterFractions.filter((t) => t > 0.02 && t < 0.98))].sort((a, b) => a - b)
        : [],
    [chapterFractions]
  );

  useEffect(() => {
    if (!enableSceneReflectionCue || sortedChapters.length < 1 || duration <= 0 || isBrowserTts) {
      prevSceneIdxRef.current = null;
      return;
    }
    const frac = currentTime / duration;
    let idx = 0;
    for (const b of sortedChapters) {
      if (frac + 1e-6 >= b) idx++;
    }
    const prev = prevSceneIdxRef.current;
    if (prev !== null && idx !== prev) {
      if (cueTimeoutRef.current) clearTimeout(cueTimeoutRef.current);
      setReflectionCue(true);
      cueTimeoutRef.current = setTimeout(() => setReflectionCue(false), 4200);
    }
    prevSceneIdxRef.current = idx;
    return () => {
      if (cueTimeoutRef.current) clearTimeout(cueTimeoutRef.current);
    };
  }, [currentTime, duration, enableSceneReflectionCue, isBrowserTts, sortedChapters]);

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
      {subtitle != null && subtitle !== "" ? (
        <p className="story-audio-player-subtitle">{subtitle}</p>
      ) : null}
      {practiceStoryChip ? (
        <p className="story-audio-player-practice-chip" role="status">
          Practice story · choices
        </p>
      ) : null}
      {extras != null ? <div className="story-audio-player-extras">{extras}</div> : null}
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
        {isBrowserTts ? (
          <p className="story-audio-player-tts-hint muted" style={{ margin: 0, fontSize: "0.75rem", flex: 1 }}>
            Read-aloud mode — pause stops playback (restart from beginning to play again).
          </p>
        ) : (
        <div className="story-audio-player-progress-wrap">
          <span className="story-audio-player-time" aria-live="polite">
            {formatTime(currentTime)}
          </span>
          <div style={{ position: "relative", flex: 1, minWidth: 0 }}>
            {sortedChapters.length > 0 && (
              <div
                className="story-audio-player-chapter-ticks"
                aria-hidden
                style={{
                  position: "absolute",
                  left: 0,
                  right: 0,
                  top: "50%",
                  transform: "translateY(-50%)",
                  height: 8,
                  pointerEvents: "none",
                }}
              >
                {sortedChapters.map((t, i) => (
                  <span
                    key={i}
                    style={{
                      position: "absolute",
                      left: `${t * 100}%`,
                      transform: "translateX(-50%)",
                      width: 2,
                      height: 8,
                      background: "rgba(255,255,255,0.55)",
                      borderRadius: 1,
                    }}
                  />
                ))}
              </div>
            )}
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
              style={{ position: "relative", zIndex: 1, width: "100%" }}
            />
          </div>
          <span className="story-audio-player-time">{formatTime(duration)}</span>
        </div>
        )}
      </div>
      {reflectionCue ? (
        <p className="story-audio-player-reflection-cue muted" style={{ margin: "0.35rem 0 0", fontSize: "0.8rem", textAlign: "center" }}>
          New scene — pause if you’d like to chat together for a moment.
        </p>
      ) : null}
    </div>
      )}
    </>
  );
}
