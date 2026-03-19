import { useState, useEffect, useRef, useCallback } from "react";
import { Link, useSearchParams, useLocation, useNavigate } from "react-router-dom";
import StoryAudioPlayer from "../components/StoryAudioPlayer";
import {
  getLibraryStories,
  getMyStories,
  generateStory,
  regenerateStoryCover,
  remixStory,
  getFavorites,
  addFavorite,
  removeFavorite,
  submitFeedback,
  getStreamUrl,
  getApiOrigin,
  resolveCoverUrl,
  fetchStreamAsBlobUrl,
  getPlaybackPosition,
  savePlaybackPosition,
  searchStories,
  type LibraryStory,
  type Story,
  type SearchStoryItem,
} from "../lib/api";
import type { StreamUrlResponse } from "../types/api";
import { useAuth } from "../contexts/AuthContext";
import { EmptyState } from "../components/EmptyState";

/** Renders story cover: animated GIF/video when coverVideoUrl is set, else static image or placeholder.
 * Dynamically shows generated cover when coverImageUrl/coverVideoUrl are present; falls back to default illustration.
 * Pass coverRefreshKey to bust cache when cover was just regenerated (same URL, new content). */
function StoryCover({
  coverImageUrl,
  coverVideoUrl,
  coverRefreshKey,
}: {
  coverImageUrl?: string | null;
  coverVideoUrl?: string | null;
  /** Cache-bust key (e.g. Date.now()) when cover was regenerated. */
  coverRefreshKey?: number;
}) {
  const baseVideoUrl = resolveCoverUrl(coverVideoUrl);
  const baseImageUrl = resolveCoverUrl(coverImageUrl);
  const appendCacheBust = (url: string) =>
    coverRefreshKey != null ? `${url}${url.includes("?") ? "&" : "?"}t=${coverRefreshKey}` : url;
  const videoUrl = baseVideoUrl ? appendCacheBust(baseVideoUrl) : null;
  const imageUrl = baseImageUrl ? appendCacheBust(baseImageUrl) : null;

  if (videoUrl && videoUrl.toLowerCase().includes(".gif")) {
    return <img src={videoUrl} alt="" />;
  }
  if (videoUrl) {
    return (
      <video
        src={videoUrl}
        poster={imageUrl ?? undefined}
        muted
        loop
        playsInline
        autoPlay
        aria-hidden
      />
    );
  }
  if (imageUrl) {
    return <img src={imageUrl} alt="" />;
  }
  return (
    <div
      className="poster-card-placeholder poster-card-placeholder--default-cover"
      aria-hidden
      style={{ backgroundImage: "url(/story-card-default.png)" }}
    />
  );
}

type Tab = "library" | "mine" | "favorites";

export default function Stories() {
  useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const resumeId = searchParams.get("resume");
  const resumeSource = searchParams.get("source");
  const playFromState = (location.state as { playStoryId?: number; playStorySource?: string } | null) ?? {};
  const [tab, setTab] = useState<Tab>("library");
  const [library, setLibrary] = useState<LibraryStory[]>([]);
  const [mine, setMine] = useState<Story[]>([]);
  const [_minePage, setMinePage] = useState({ page: 0, totalPages: 0, last: true });
  const [favorites, setFavorites] = useState<{ storyId: number; storySource: string }[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [genTheme, setGenTheme] = useState("");
  const [genAge, setGenAge] = useState(5);
  const [generating, setGenerating] = useState(false);
  const [favToggling, setFavToggling] = useState<number | null>(null);
  const [feedbackOpen, setFeedbackOpen] = useState(false);
  const [feedbackRating, setFeedbackRating] = useState(5);
  const [feedbackComment, setFeedbackComment] = useState("");
  const [coverGenId, setCoverGenId] = useState<number | null>(null);
  /** Cache-bust keys for covers just regenerated; forces fresh fetch when same URL has new content. */
  const [coverRefreshKeys, setCoverRefreshKeys] = useState<Record<number, number>>({});
  const [remixId, setRemixId] = useState<number | null>(null);
  const [remixInstruction, setRemixInstruction] = useState("");
  const [playingStoryId, setPlayingStoryId] = useState<number | null>(null);
  const [loadingStreamId, setLoadingStreamId] = useState<number | null>(null);
  const [playingTitle, setPlayingTitle] = useState<string | null>(null);
  const [audioCurrentTime, setAudioCurrentTime] = useState(0);
  const [audioDuration, setAudioDuration] = useState(0);
  const [selectedVoice, setSelectedVoice] = useState<string>("default");
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<SearchStoryItem[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const [playingAvatarVideoUrl, setPlayingAvatarVideoUrl] = useState<string | null>(null);
  const playingStoryRef = useRef<{ id: number; source: string } | null>(null);
  const playIntentRef = useRef<number | null>(null);
  const blobUrlRef = useRef<string | null>(null);

  useEffect(() => {
    if (searchQuery.trim().length < 2) {
      setSearchResults([]);
      return;
    }
    const t = setTimeout(() => {
      setSearchLoading(true);
      searchStories(searchQuery.trim(), "ta", 0, 20)
        .then((r) => setSearchResults(r.content))
        .catch(() => setSearchResults([]))
        .finally(() => setSearchLoading(false));
    }, 300);
    return () => clearTimeout(t);
  }, [searchQuery]);

  useEffect(() => {
    setLoading(true);
    setError("");
    getLibraryStories("ta")
      .then(setLibrary)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (tab === "mine") {
      setLoading(true);
      setError("");
      getMyStories(0, 20)
        .then((p) => {
          setMine(p.content);
          setMinePage({ page: 0, totalPages: p.totalPages, last: p.last });
        })
        .catch((e) => setError(e.message))
        .finally(() => setLoading(false));
    }
  }, [tab]);

  useEffect(() => {
    getFavorites()
      .then(setFavorites)
      .catch(() => setFavorites([]));
  }, []);

  // Handle "Continue listening" from Dashboard: ?resume=ID&source=library|generated
  const resumeHandled = useRef(false);
  useEffect(() => {
    if (!resumeId || !resumeSource || resumeHandled.current) return;
    const id = Number(resumeId);
    if (Number.isNaN(id)) return;
    const sourceTab: Tab = resumeSource === "library" ? "library" : resumeSource === "favorites" ? "favorites" : "mine";
    setTab(sourceTab);
  }, [resumeId, resumeSource]);

  useEffect(() => {
    if (!resumeId || !resumeSource || resumeHandled.current) return;
    const id = Number(resumeId);
    if (Number.isNaN(id)) return;
    const sourceTab: Tab = resumeSource === "library" ? "library" : resumeSource === "favorites" ? "favorites" : "mine";
    const listReady =
      (sourceTab === "library" && library.length > 0) ||
      (sourceTab === "mine" && tab === "mine" && !loading) ||
      (sourceTab === "favorites" && favorites.length > 0);
    if (!listReady) return;
    resumeHandled.current = true;
    (async () => {
      let startPos: number | undefined;
      try {
        startPos = await getPlaybackPosition(id, resumeSource);
      } catch {
        startPos = undefined;
      }
      playStory(id, resumeSource, startPos);
      setSearchParams({});
    })();
  }, [resumeId, resumeSource, tab, loading, library.length, mine.length, favorites.length]);

  // Handle "Play from Dashboard" recommended: navigate with state { playStoryId, playStorySource } → auto-play
  const playFromStateHandled = useRef(false);
  useEffect(() => {
    const { playStoryId: sid, playStorySource: ssrc } = playFromState;
    if (sid == null || !ssrc || playFromStateHandled.current) return;
    const sourceTab: Tab = ssrc === "library" ? "library" : ssrc === "favorites" ? "favorites" : "mine";
    setTab(sourceTab);
    const listReady =
      (sourceTab === "library" && library.length > 0) ||
      (sourceTab === "mine" && mine.length >= 0 && !loading) ||
      (sourceTab === "favorites" && favorites.length >= 0);
    if (!listReady) return;
    playFromStateHandled.current = true;
    playStory(sid, ssrc);
    navigate(location.pathname, { replace: true, state: {} });
  }, [playFromState.playStoryId, playFromState.playStorySource, library.length, mine.length, favorites.length, loading]);

  const isFav = (storyId: number) => favorites.some((f) => f.storyId === storyId);

  const getTitleForStory = useCallback(
    (id: number, source: string): string => {
      if (source === "library") {
        const s = library.find((c) => c.id === id);
        return s ? (s.title || s.theme) : `Story #${id}`;
      }
      if (source === "generated" || source === "mine") {
        const s = mine.find((m) => m.id === id);
        return s ? (s.title || s.theme) : `Story #${id}`;
      }
      const f = favorites.find((x) => x.storyId === id);
      if (f) {
        const s = library.find((c) => c.id === id) || mine.find((m) => m.id === id);
        return s ? (s.title || s.theme) : `Story #${id}`;
      }
      return `Story #${id}`;
    },
    [library, mine, favorites]
  );

  const playStory = async (
    storyId: number,
    storySource: string,
    startPositionSeconds?: number,
    titleOverride?: string
  ) => {
    if (playingStoryId === storyId) {
      audioRef.current?.pause();
      videoRef.current?.pause();
      setPlayingAvatarVideoUrl(null);
      if (blobUrlRef.current) {
        URL.revokeObjectURL(blobUrlRef.current);
        blobUrlRef.current = null;
      }
      setPlayingStoryId(null);
      setPlayingTitle(null);
      return;
    }
    audioRef.current?.pause();
    videoRef.current?.pause();
    setPlayingAvatarVideoUrl(null);
    playIntentRef.current = storyId;
    setLoadingStreamId(storyId);
    setError("");
    try {
      const voiceToUse = selectedVoice === "default" ? null : selectedVoice;
      const sourceForApi = storySource === "mine" ? "generated" : storySource;
      let data: StreamUrlResponse | null = await getStreamUrl(storyId, "ta", voiceToUse, sourceForApi);
      if (!data && voiceToUse != null) {
        data = await getStreamUrl(storyId, "ta", null, sourceForApi);
      }
      if (!data?.streamUrl) {
        setError("Audio not available for this story");
        return;
      }
      if (playIntentRef.current !== storyId) return;
      playingStoryRef.current = { id: storyId, source: storySource };
      setPlayingTitle(titleOverride ?? getTitleForStory(storyId, storySource));

      if (data.avatarVideoUrl) {
        const resolvedVideoUrl = resolveCoverUrl(data.avatarVideoUrl) ?? data.avatarVideoUrl;
        setPlayingAvatarVideoUrl(resolvedVideoUrl);
        setPlayingStoryId(storyId);
        setAudioDuration(0);
        setAudioCurrentTime(0);
        setLoadingStreamId(null);
        playIntentRef.current = null;
        return;
      }

      const audio = audioRef.current;
      if (!audio) {
        setError("Audio player not ready");
        return;
      }
      if (blobUrlRef.current) {
        URL.revokeObjectURL(blobUrlRef.current);
        blobUrlRef.current = null;
      }
      let playUrl = data.streamUrl;
      try {
        const streamOrigin = typeof window !== "undefined" ? new URL(data.streamUrl).origin : "";
        if (streamOrigin === getApiOrigin()) {
          playUrl = await fetchStreamAsBlobUrl(data.streamUrl);
          blobUrlRef.current = playUrl;
        }
      } catch (e) {
        if (playIntentRef.current === storyId) {
          setError(e instanceof Error ? e.message : "Failed to load audio");
        }
        return;
      }
      if (playIntentRef.current !== storyId) return;
      audio.src = playUrl;
      if (startPositionSeconds != null && startPositionSeconds > 0) {
        audio.currentTime = startPositionSeconds;
      }
      await audio.play();
      if (playIntentRef.current !== storyId) return;
      setPlayingStoryId(storyId);
      setAudioDuration(audio.duration || 0);
      setAudioCurrentTime(audio.currentTime || 0);
    } catch (e) {
      const isInterrupted =
        (e instanceof DOMException && e.name === "AbortError") ||
        (e instanceof Error && /interrupted|pause/i.test(e.message));
      if (!isInterrupted && playIntentRef.current === storyId) {
        const msg = e instanceof Error ? e.message : "Failed to load audio";
        setError(msg);
      }
    } finally {
      if (playIntentRef.current === storyId) {
        playIntentRef.current = null;
        setLoadingStreamId(null);
      }
    }
  };

  useEffect(() => {
    if (!playingAvatarVideoUrl || !playingStoryId) return;
    const t = setTimeout(() => {
      videoRef.current?.play().catch((e) => {
        if (playIntentRef.current !== playingStoryId) return;
        setError(e instanceof Error ? e.message : "Video playback failed");
      });
    }, 100);
    return () => clearTimeout(t);
  }, [playingAvatarVideoUrl, playingStoryId]);

  const handleAudioPlayPause = useCallback(() => {
    if (playingStoryId != null) {
      const p = playingStoryRef.current;
      if (p && playingAvatarVideoUrl && videoRef.current) {
        savePlaybackPosition({
          storyId: p.id,
          storySource: p.source,
          positionSeconds: Math.floor(videoRef.current.currentTime),
        }).catch(() => {});
      }
      audioRef.current?.pause();
      videoRef.current?.pause();
      setPlayingAvatarVideoUrl(null);
      setPlayingStoryId(null);
      return;
    }
    const p = playingStoryRef.current;
    if (p && (audioRef.current?.src || playingAvatarVideoUrl)) {
      if (playingAvatarVideoUrl) {
        videoRef.current?.play().then(() => {
          setPlayingStoryId(p.id);
          setPlayingTitle(getTitleForStory(p.id, p.source));
        }).catch(() => {});
      } else {
        audioRef.current!.play().then(() => {
          setPlayingStoryId(p.id);
          setPlayingTitle(getTitleForStory(p.id, p.source));
        }).catch((e) => {
          const isInterrupted =
            (e instanceof DOMException && e.name === "AbortError") ||
            (e instanceof Error && /interrupted|pause/i.test(e.message));
          if (!isInterrupted) setError("Playback failed");
        });
      }
    }
  }, [playingStoryId, playingAvatarVideoUrl, getTitleForStory]);

  const handleSeek = useCallback((seconds: number) => {
    if (!Number.isFinite(seconds)) return;
    if (playingAvatarVideoUrl && videoRef.current) {
      videoRef.current.currentTime = seconds;
    } else if (audioRef.current) {
      audioRef.current.currentTime = seconds;
    }
    setAudioCurrentTime(seconds);
  }, [playingAvatarVideoUrl]);

  useEffect(() => {
    const audio = audioRef.current;
    const video = videoRef.current;
    const handleEnded = () => {
      if (blobUrlRef.current) {
        URL.revokeObjectURL(blobUrlRef.current);
        blobUrlRef.current = null;
      }
      setPlayingAvatarVideoUrl(null);
      setPlayingStoryId(null);
      setPlayingTitle(null);
      playingStoryRef.current = null;
    };
    const handlePause = () => {
      const p = playingStoryRef.current;
      if (p && audio) {
        savePlaybackPosition({
          storyId: p.id,
          storySource: p.source,
          positionSeconds: Math.floor(audio.currentTime),
        }).catch(() => {});
      }
    };
    if (video && playingAvatarVideoUrl) {
      video.addEventListener("ended", handleEnded);
      return () => video.removeEventListener("ended", handleEnded);
    }
    if (audio) {
      audio.addEventListener("ended", handleEnded);
      audio.addEventListener("pause", handlePause);
      return () => {
        audio.removeEventListener("ended", handleEnded);
        audio.removeEventListener("pause", handlePause);
      };
    }
  }, [playingStoryId, playingAvatarVideoUrl]);

  const toggleFavorite = async (storyId: number, storySource: string) => {
    setFavToggling(storyId);
    try {
      if (isFav(storyId)) {
        await removeFavorite(storyId);
        setFavorites((f) => f.filter((x) => x.storyId !== storyId));
      } else {
        await addFavorite(storyId, storySource);
        setFavorites((f) => [...f, { storyId, storySource }]);
      }
    } catch {
      // ignore
    } finally {
      setFavToggling(null);
    }
  };

  const handleGenerate = async (e: React.FormEvent) => {
    e.preventDefault();
    const theme = genTheme.trim();
    if (!theme) {
      setError("Theme is required");
      return;
    }
    setGenerating(true);
    setError("");
    try {
      await generateStory({
        theme,
        childName: "Listener",
        age: genAge,
        language: "ta",
      });
      setTab("mine");
      const p = await getMyStories(0, 20);
      setMine(p.content);
      setMinePage({ page: 0, totalPages: p.totalPages, last: p.last });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Generation failed");
    } finally {
      setGenerating(false);
    }
  };

  const showPlayer = playingTitle != null || loadingStreamId != null;

  return (
    <div className={`page prime-page${showPlayer ? " has-audio-player" : ""}`}>
      <StoryAudioPlayer
        audioRef={audioRef}
        videoRef={videoRef}
        avatarVideoUrl={playingAvatarVideoUrl}
        isPlaying={playingStoryId != null}
        isLoading={loadingStreamId != null}
        title={playingTitle ?? (loadingStreamId != null ? "Loading…" : null)}
        currentTime={audioCurrentTime}
        duration={audioDuration}
        onPlayPause={handleAudioPlayPause}
        onTimeUpdate={setAudioCurrentTime}
        onDurationChange={setAudioDuration}
        onSeek={handleSeek}
      />
      <section className="prime-hero prime-hero-tamixa stories-hero">
        <div className="prime-hero-content stories-hero-content">
          <h1>Stories</h1>
          <p className="prime-hero-subtitle">Browse the story library or generate new ones with AI. Click any story to play audio.</p>
          <div className="stories-search-wrap stories-search-center">
            <label htmlFor="stories-search" className="stories-search-label">Search</label>
            <input
              id="stories-search"
              type="search"
              className="stories-search-input"
              placeholder="Search by theme or title…"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              aria-label="Search stories"
            />
          </div>
        </div>
      </section>

      {searchQuery.trim().length >= 2 && (
        <section className="prime-row">
          <div className="prime-row-header">
            <h2 className="prime-row-title">Search results</h2>
          </div>
          {searchLoading ? (
            <div className="prime-row-grid">
              {[1, 2, 3, 4, 5, 6].map((i) => (
                <div key={i} className="poster-card" style={{ pointerEvents: "none" }}>
                  <div className="poster-card-cover"><div className="poster-card-placeholder skeleton" style={{ margin: 0 }} /></div>
                  <p className="poster-card-title"><span className="skeleton" style={{ display: "block", height: 14, width: "80%" }} /></p>
                  <p className="poster-card-meta"><span className="skeleton" style={{ display: "block", height: 12, width: "60%" }} /></p>
                </div>
              ))}
            </div>
          ) : searchResults.length === 0 ? (
            <p className="muted" style={{ padding: "0 0.125rem" }}>No stories match &quot;{searchQuery}&quot;</p>
          ) : (
            <div className="prime-row-grid">
              {searchResults.map((s) => (
                <div
                  key={`${s.storySource}-${s.storyId}`}
                  className="poster-card poster-card-clickable"
                  role="button"
                  tabIndex={0}
                  onClick={() => playStory(s.storyId, s.storySource, undefined, s.title || s.theme)}
                  onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); playStory(s.storyId, s.storySource, undefined, s.title || s.theme); } }}
                  aria-label={`Play ${s.title || s.theme}`}
                >
                  <div className="poster-card-cover">
                    <StoryCover
                      coverImageUrl={s.coverImageUrl}
                      coverVideoUrl={s.coverVideoUrl}
                      coverRefreshKey={coverRefreshKeys[s.storyId]}
                    />
                  </div>
                  <p className="poster-card-title">{s.title || s.theme}</p>
                  <p className="poster-card-meta">{s.theme} · {s.wordCount} words</p>
                  <div className="poster-card-actions" onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      onClick={() => playStory(s.storyId, s.storySource, undefined, s.title || s.theme)}
                      disabled={loadingStreamId === s.storyId}
                      aria-label={playingStoryId === s.storyId ? "Pause story" : "Play story"}
                    >
                      {loadingStreamId === s.storyId ? "…" : playingStoryId === s.storyId ? "⏸" : "▶"}
                    </button>
                    <button
                      type="button"
                      className={`btn btn-sm btn-outline story-fav-btn ${isFav(s.storyId) ? "fav-active" : ""}`}
                      onClick={() => toggleFavorite(s.storyId, s.storySource)}
                      disabled={favToggling === s.storyId}
                      aria-label={isFav(s.storyId) ? "Remove from favorites" : "Add to favorites"}
                    >
                      ♥
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      )}

      <section className="prime-row" style={{ paddingTop: "0.25rem" }}>
      <div className="tabs" role="tablist" aria-label="Story tabs">
        <button
          type="button"
          role="tab"
          aria-selected={tab === "library"}
          aria-controls="library-panel"
          id="tab-library"
          className={tab === "library" ? "tab active" : "tab"}
          onClick={() => setTab("library")}
        >
          Library
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={tab === "mine"}
          aria-controls="mine-panel"
          id="tab-mine"
          className={tab === "mine" ? "tab active" : "tab"}
          onClick={() => setTab("mine")}
        >
          My generated stories
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={tab === "favorites"}
          aria-controls="favorites-panel"
          id="tab-favorites"
          className={tab === "favorites" ? "tab active" : "tab"}
          onClick={() => setTab("favorites")}
        >
          Favorites ({favorites.length})
        </button>
      </div>

      {error && (
        <p className="error">
          {error}
          {error.toLowerCase().includes("upgrade") && (
            <>
              {" "}
              <Link to="/subscription" className="btn btn-sm btn-primary" style={{ marginLeft: 8 }}>
                Upgrade now
              </Link>
            </>
          )}
        </p>
      )}

      <div className="voice-picker">
        <label htmlFor="voice-select">Narration voice:</label>
        <select
          id="voice-select"
          value={selectedVoice}
          onChange={(e) => setSelectedVoice(e.target.value)}
          className="voice-select"
          aria-describedby="voice-hint"
        >
          <option value="default">Default</option>
          <option value="calm">Calm (Premium)</option>
        </select>
        <span id="voice-hint" className="muted voice-hint">Premium voices require subscription.</span>
      </div>

      <div className="story-tab-panel">
      {tab === "library" && (
        <>
          {loading ? (
            <div className="prime-row-grid">
              {[1, 2, 3, 4, 5, 6].map((i) => (
                <div key={i} className="poster-card" style={{ pointerEvents: "none" }}>
                  <div className="poster-card-cover"><div className="poster-card-placeholder skeleton" style={{ margin: 0 }} /></div>
                  <p className="poster-card-title"><span className="skeleton" style={{ display: "block", height: 14, width: "80%" }} /></p>
                  <p className="poster-card-meta"><span className="skeleton" style={{ display: "block", height: 12, width: "60%" }} /></p>
                </div>
              ))}
            </div>
          ) : library.length === 0 ? (
            <EmptyState
              emoji="📚"
              title="No stories in library"
              description="Story library is being updated. Check back soon or try generating your own."
              action={{ label: "Generate Story", onClick: () => setTab("mine") }}
            />
          ) : (
            <div className="prime-row-grid">
              {library.map((s) => (
                <div
                  key={s.id}
                  className="poster-card poster-card-clickable"
                  role="button"
                  tabIndex={0}
                  onClick={() => playStory(s.id, "library")}
                  onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); playStory(s.id, "library"); } }}
                  aria-label={`Play ${s.title || s.theme}`}
                >
                  <div className="poster-card-cover">
                    <StoryCover
                      coverImageUrl={s.coverImageUrl}
                      coverVideoUrl={s.coverVideoUrl}
                      coverRefreshKey={coverRefreshKeys[s.id]}
                    />
                  </div>
                  <p className="poster-card-title">{s.title || s.theme}</p>
                  <p className="poster-card-meta">{s.theme} · {s.wordCount} words · {Number(s.readingTimeMinutes ?? 0).toFixed(1)} min</p>
                  <div className="poster-card-actions" onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      className="btn btn-primary btn-sm story-play-btn"
                      onClick={() => playStory(s.id, "library")}
                      disabled={loadingStreamId === s.id}
                      aria-label={playingStoryId === s.id ? "Pause story" : "Play story"}
                    >
                      {loadingStreamId === s.id ? "…" : playingStoryId === s.id ? "⏸" : "▶"}
                    </button>
                    <button
                      type="button"
                      className={`btn btn-sm btn-outline story-fav-btn ${isFav(s.id) ? "fav-active" : ""}`}
                      onClick={() => toggleFavorite(s.id, "library")}
                      disabled={favToggling === s.id}
                      aria-label={isFav(s.id) ? "Remove from favorites" : "Add to favorites"}
                    >
                      ♥
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}

      {tab === "mine" && (
        <>
          {loading ? (
            <div className="prime-row-grid">
              {[1, 2, 3, 4, 5, 6].map((i) => (
                <div key={i} className="poster-card" style={{ pointerEvents: "none" }}>
                  <div className="poster-card-cover"><div className="poster-card-placeholder skeleton" style={{ margin: 0 }} /></div>
                  <p className="poster-card-title"><span className="skeleton" style={{ display: "block", height: 14, width: "80%" }} /></p>
                  <p className="poster-card-meta"><span className="skeleton" style={{ display: "block", height: 12, width: "60%" }} /></p>
                </div>
              ))}
            </div>
          ) : mine.length === 0 ? (
            <EmptyState
              emoji="✨"
              title="No stories yet"
              description="Create your first AI story in Tamil. Pick a theme and we'll generate it for you."
              action={{ label: "Generate Story", onClick: () => document.getElementById("generate-story-section")?.scrollIntoView({ behavior: "smooth" }) }}
            />
          ) : (
            <>
            <div className="prime-row-grid">
              {mine.map((s) => (
                <div
                  key={s.id}
                  className="poster-card poster-card-clickable"
                  role="button"
                  tabIndex={0}
                  onClick={() => playStory(s.id, "generated")}
                  onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); playStory(s.id, "generated"); } }}
                  aria-label={`Play ${s.title || s.theme}`}
                >
                  <div className="poster-card-cover">
                    <StoryCover
                      coverImageUrl={s.coverImageUrl}
                      coverVideoUrl={s.coverVideoUrl}
                      coverRefreshKey={coverRefreshKeys[s.id]}
                    />
                  </div>
                  <p className="poster-card-title">{s.title || s.theme}</p>
                  <p className="poster-card-meta">{s.theme} · {s.childName} · {s.status}</p>
                  <div className="poster-card-actions" onClick={(e) => e.stopPropagation()}>
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      onClick={() => playStory(s.id, "generated")}
                      disabled={loadingStreamId === s.id}
                      aria-label={playingStoryId === s.id ? "Pause story" : "Play story"}
                    >
                      {loadingStreamId === s.id ? "…" : playingStoryId === s.id ? "⏸" : "▶"}
                    </button>
                    <button
                      type="button"
                      className={`btn btn-sm btn-outline story-fav-btn ${isFav(s.id) ? "fav-active" : ""}`}
                      onClick={() => toggleFavorite(s.id, "generated")}
                      disabled={favToggling === s.id}
                      aria-label={isFav(s.id) ? "Remove from favorites" : "Add to favorites"}
                    >
                      ♥
                    </button>
                    <button
                      type="button"
                      className="btn btn-sm btn-outline"
                      onClick={async () => {
                        setCoverGenId(s.id);
                        try {
                          const updated = await regenerateStoryCover(s.id);
                          setMine((prev) => prev.map((x) => (x.id === s.id ? updated : x)));
                          setCoverRefreshKeys((k) => ({ ...k, [s.id]: Date.now() }));
                        } catch {
                          // ignore
                        } finally {
                          setCoverGenId(null);
                        }
                      }}
                      disabled={coverGenId !== null}
                      title="Generate AI cover"
                    >
                      {coverGenId === s.id ? "…" : "🖼"}
                    </button>
                    <button
                      type="button"
                      className="btn btn-sm btn-outline"
                      onClick={() => setRemixId(remixId === s.id ? null : s.id)}
                      title="Remix"
                    >
                      ✨
                    </button>
                  </div>
                </div>
              ))}
            </div>
            {remixId != null && (
              <div className="generate-section" style={{ marginTop: 10 }}>
                <h3>Remix story</h3>
                <p className="muted">Change one element, e.g. &quot;make the dragon friendly&quot;</p>
                <form
                  onSubmit={async (e) => {
                    e.preventDefault();
                    if (!remixInstruction.trim()) return;
                    setError("");
                    try {
                      const newStory = await remixStory(remixId, remixInstruction.trim());
                      setMine((prev) => [newStory, ...prev]);
                      setRemixInstruction("");
                      setRemixId(null);
                    } catch (err) {
                      setError(err instanceof Error ? err.message : "Remix failed");
                    }
                  }}
                  className="form"
                  style={{ display: "flex", gap: 8, alignItems: "flex-end" }}
                >
                  <div className="field" style={{ flex: 1 }}>
                    <input
                      value={remixInstruction}
                      onChange={(e) => setRemixInstruction(e.target.value)}
                      placeholder="e.g. make the dragon friendly"
                    />
                  </div>
                  <button type="submit" className="btn btn-primary">Remix</button>
                  <button type="button" className="btn btn-outline" onClick={() => setRemixId(null)}>Cancel</button>
                </form>
              </div>
            )}
            </>
          )}
        </>
      )}

      {tab === "favorites" && (
        <>
          {favorites.length === 0 ? (
            <EmptyState
              emoji="♥"
              title="No favorites yet"
              description="Add stories from Library or My stories to find them here."
              action={{ label: "Browse Library", onClick: () => setTab("library") }}
            />
          ) : (
            <div className="prime-row-grid">
              {favorites.map((f) => {
                const s = library.find((c) => c.id === f.storyId) || mine.find((m) => m.id === f.storyId);
                const coverImageUrl = s && "coverImageUrl" in s ? s.coverImageUrl : null;
                const coverVideoUrl = s && "coverVideoUrl" in s ? (s as { coverVideoUrl?: string | null }).coverVideoUrl : null;
                const title = s ? (`title` in s && s.title ? s.title : s.theme) : `Story #${f.storyId}`;
                return (
                  <div
                    key={`${f.storyId}-${f.storySource}`}
                    className="poster-card poster-card-clickable"
                    role="button"
                    tabIndex={0}
                    onClick={() => playStory(f.storyId, f.storySource)}
                    onKeyDown={(e) => { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); playStory(f.storyId, f.storySource); } }}
                    aria-label={`Play ${title}`}
                  >
                    <div className="poster-card-cover">
                      <StoryCover
                        coverImageUrl={coverImageUrl}
                        coverVideoUrl={coverVideoUrl}
                        coverRefreshKey={s ? coverRefreshKeys[s.id] : undefined}
                      />
                    </div>
                    <p className="poster-card-title">{title}</p>
                    <p className="poster-card-meta">{s ? s.theme : "\u00A0"}</p>
                    <div className="poster-card-actions" onClick={(e) => e.stopPropagation()}>
                      <button
                        type="button"
                        className="btn btn-primary btn-sm"
                        onClick={() => playStory(f.storyId, f.storySource)}
                        disabled={loadingStreamId === f.storyId}
                        aria-label={playingStoryId === f.storyId ? "Pause story" : "Play story"}
                      >
                        {loadingStreamId === f.storyId ? "…" : playingStoryId === f.storyId ? "⏸" : "▶"}
                      </button>
                      <button
                        type="button"
                        className="btn btn-sm btn-outline story-fav-btn fav-active"
                        onClick={() => toggleFavorite(f.storyId, f.storySource)}
                        disabled={favToggling === f.storyId}
                        aria-label="Remove from favorites"
                      >
                        ♥
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </>
      )}
      </div>
      </section>

      <section className="prime-row">
        <div className="prime-row-header">
          <h2 className="prime-row-title">Feedback</h2>
        </div>
        <div style={{ padding: "0 0.125rem" }}>
        <button type="button" className="btn btn-outline" onClick={() => setFeedbackOpen(!feedbackOpen)}>
          {feedbackOpen ? "Hide" : "Send feedback"}
        </button>
        {feedbackOpen && (
          <form
            onSubmit={async (e) => {
              e.preventDefault();
              try {
                await submitFeedback({ rating: feedbackRating, comment: feedbackComment || undefined });
                setFeedbackComment("");
                setFeedbackOpen(false);
              } catch {
                // ignore
              }
            }}
            className="form"
          >
            <div className="field">
              <label>Rating (1-5)</label>
              <input type="number" min={1} max={5} value={feedbackRating} onChange={(e) => setFeedbackRating(parseInt(e.target.value, 10) || 5)} />
            </div>
            <div className="field">
              <label>Comment (optional)</label>
              <textarea value={feedbackComment} onChange={(e) => setFeedbackComment(e.target.value)} rows={2} />
            </div>
            <button type="submit" className="btn btn-primary">Submit</button>
          </form>
        )}
        </div>
      </section>

      <section id="generate-story-section" className="prime-row">
        <div className="prime-row-header">
          <h2 className="prime-row-title">Generate new story</h2>
        </div>
        <div style={{ padding: "0 0.125rem" }}>
        <p className="muted" style={{ marginBottom: "0.75rem" }}>AI will create a Tamil story. Enter a theme and age.</p>
        <form onSubmit={handleGenerate} className="form">
          <div className="field">
            <label htmlFor="theme">Theme</label>
            <input
              id="theme"
              value={genTheme}
              onChange={(e) => setGenTheme(e.target.value)}
              placeholder="e.g. Animals, Bedtime"
              required
            />
          </div>
          <div className="field">
            <label htmlFor="age">Age (1–12)</label>
            <input
              id="age"
              type="number"
              min={1}
              max={12}
              value={genAge}
              onChange={(e) => setGenAge(parseInt(e.target.value, 10) || 5)}
            />
          </div>
            <button type="submit" className="btn btn-primary" disabled={generating}>
              {generating ? "Generating…" : "Generate story"}
            </button>
          </form>
        </div>
      </section>
    </div>
  );
}
