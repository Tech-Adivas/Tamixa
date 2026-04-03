import { useState, useEffect, useRef, useCallback } from "react";
import { Link, useSearchParams, useLocation, useNavigate } from "react-router-dom";
import StoryAudioPlayer from "../components/StoryAudioPlayer";
import {
  getLibraryStories,
  getLibraryCategories,
  getGenerationTopics,
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
  ApiClientError,
  STORY_GENERATE_ERROR_CODES,
  type LibraryStory,
  type Story,
  type SearchStoryItem,
  type GenerationTopic,
} from "../lib/api";
import type { StreamUrlResponse } from "../lib/api";
import { useAuth } from "../contexts/AuthContext";
import { EmptyState } from "../components/EmptyState";
import {
  isFunStory,
  funCornerBadgeLabel,
  listenerPlaybackSubtitle,
} from "../lib/storyListenerUi";

type Tab = "library" | "mine" | "favorites";

function readTabFromSearch(): Tab {
  if (typeof window === "undefined") return "library";
  const t = new URLSearchParams(window.location.search).get("tab");
  if (t === "learn") return "library";
  if (t === "library" || t === "mine" || t === "favorites") return t;
  return "library";
}

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

export default function Stories() {
  useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const resumeId = searchParams.get("resume");
  const resumeSource = searchParams.get("source");
  const playFromState = (location.state as { playStoryId?: number; playStorySource?: string } | null) ?? {};
  const [tab, setTab] = useState<Tab>(readTabFromSearch);
  const [library, setLibrary] = useState<LibraryStory[]>([]);
  const [generationTopics, setGenerationTopics] = useState<GenerationTopic[]>([]);
  const [genGenerationTopicId, setGenGenerationTopicId] = useState("");
  const [parentPanelStory, setParentPanelStory] = useState<LibraryStory | null>(null);
  const [mine, setMine] = useState<Story[]>([]);
  const [, setMinePage] = useState({ page: 0, totalPages: 0, last: true });
  const [favorites, setFavorites] = useState<{ storyId: number; storySource: string }[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  /** Library browse fetch (theme filter). */
  const [libraryListError, setLibraryListError] = useState("");
  const [libraryRetryKey, setLibraryRetryKey] = useState(0);
  /** My stories list fetch. */
  const [mineListError, setMineListError] = useState("");
  const [mineRetryKey, setMineRetryKey] = useState(0);
  /** Favorites list (also drives tab count). */
  const [favoritesListError, setFavoritesListError] = useState("");
  const [favoritesRetryKey, setFavoritesRetryKey] = useState(0);
  const [genTheme, setGenTheme] = useState("");
  const [genAge, setGenAge] = useState(5);
  /** Empty string = omit learningFocus in API request. */
  const [genLearningFocus, setGenLearningFocus] = useState("");
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
  const [playingSubtitle, setPlayingSubtitle] = useState<string | null>(null);
  const [audioCurrentTime, setAudioCurrentTime] = useState(0);
  const [audioDuration, setAudioDuration] = useState(0);
  const [selectedVoice, setSelectedVoice] = useState<string>("default");
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<SearchStoryItem[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [libraryCategories, setLibraryCategories] = useState<string[]>([]);
  const [libraryThemeFilter, setLibraryThemeFilter] = useState<string | null>(null);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const [playingAvatarVideoUrl, setPlayingAvatarVideoUrl] = useState<string | null>(null);
  const [playingHostClipUrl, setPlayingHostClipUrl] = useState<string | null>(null);
  const playingStoryRef = useRef<{ id: number; source: string } | null>(null);
  const playIntentRef = useRef<number | null>(null);
  const blobUrlRef = useRef<string | null>(null);

  /** Keep `?tab=` in sync when the user picks a tab (shareable / back button). */
  const setTabAndUrl = useCallback(
    (next: Tab) => {
      setTab(next);
      setSearchParams(
        (prev) => {
          const p = new URLSearchParams(prev);
          p.set("tab", next);
          return p;
        },
        { replace: true }
      );
    },
    [setSearchParams]
  );

  /** After resume playback, drop only resume handoff params — preserve `tab` and other queries. */
  const clearResumeQueryParams = useCallback(() => {
    setSearchParams(
      (prev) => {
        const p = new URLSearchParams(prev);
        p.delete("resume");
        p.delete("source");
        return p;
      },
      { replace: true }
    );
  }, [setSearchParams]);

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
    const t = searchParams.get("tab");
    if (t === "learn") {
      setTabAndUrl("library");
      return;
    }
    if (t === "library" || t === "mine" || t === "favorites") {
      setTab(t);
    }
  }, [searchParams, setTabAndUrl]);

  useEffect(() => {
    getLibraryCategories("ta")
      .then(setLibraryCategories)
      .catch(() => setLibraryCategories([]));
    getGenerationTopics()
      .then(setGenerationTopics)
      .catch(() => setGenerationTopics([]));
  }, []);

  useEffect(() => {
    if (tab !== "library") return;
    let cancelled = false;
    setLoading(true);
    setLibraryListError("");
    getLibraryStories("ta", 0, 50, libraryThemeFilter, false)
      .then((rows) => {
        if (!cancelled) {
          setLibrary(rows);
          setLibraryListError("");
        }
      })
      .catch((e) => {
        if (!cancelled) {
          setLibrary([]);
          setLibraryListError(e instanceof Error ? e.message : "Failed to load library");
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [tab, libraryThemeFilter, libraryRetryKey]);

  useEffect(() => {
    if (tab === "mine") {
      setLoading(true);
      setMineListError("");
      getMyStories(0, 20)
        .then((p) => {
          setMine(p.content);
          setMinePage({ page: 0, totalPages: p.totalPages, last: p.last });
          setMineListError("");
        })
        .catch((e) => {
          setMine([]);
          setMineListError(e instanceof Error ? e.message : "Failed to load your stories");
        })
        .finally(() => setLoading(false));
    }
  }, [tab, mineRetryKey]);

  useEffect(() => {
    let cancelled = false;
    setFavoritesListError("");
    getFavorites()
      .then((rows) => {
        if (!cancelled) {
          setFavorites(rows);
          setFavoritesListError("");
        }
      })
      .catch((e) => {
        if (!cancelled) {
          setFavorites([]);
          setFavoritesListError(e instanceof Error ? e.message : "Failed to load favorites");
        }
      });
    return () => {
      cancelled = true;
    };
  }, [favoritesRetryKey]);

  // Handle "Continue listening" from Dashboard: ?resume=ID&source=library|generated
  const resumeHandled = useRef(false);
  useEffect(() => {
    if (!resumeId || !resumeSource || resumeHandled.current) return;
    const id = Number(resumeId);
    if (Number.isNaN(id)) return;
    const sourceTab: Tab = resumeSource === "library" ? "library" : resumeSource === "favorites" ? "favorites" : "mine";
    setTabAndUrl(sourceTab);
  }, [resumeId, resumeSource, setTabAndUrl]);

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
      clearResumeQueryParams();
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps -- resume handshake: omit playStory/clearResumeQueryParams to avoid loops
  }, [
    resumeId,
    resumeSource,
    tab,
    loading,
    library.length,
    mine.length,
    favorites.length,
    clearResumeQueryParams,
  ]);

  // Handle "Play from Dashboard" recommended: navigate with state { playStoryId, playStorySource } → auto-play
  const playFromStateHandled = useRef(false);
  useEffect(() => {
    const { playStoryId: sid, playStorySource: ssrc } = playFromState;
    if (sid == null || !ssrc || playFromStateHandled.current) return;
    const urlTab = searchParams.get("tab");
    const normalizedTab: Tab =
      urlTab === "learn" || urlTab === "library"
        ? "library"
        : urlTab === "mine" || urlTab === "favorites"
          ? urlTab
          : ssrc === "library"
            ? "library"
            : ssrc === "favorites"
              ? "favorites"
              : "mine";
    setTabAndUrl(normalizedTab);
    const listReady =
      (normalizedTab === "library" && library.length > 0) ||
      (normalizedTab === "mine" && mine.length >= 0 && !loading) ||
      (normalizedTab === "favorites" && favorites.length >= 0);
    if (!listReady) return;
    playFromStateHandled.current = true;
    playStory(sid, ssrc);
    navigate({ pathname: location.pathname, search: location.search }, { replace: true, state: {} });
    // eslint-disable-next-line react-hooks/exhaustive-deps -- dashboard play intent: omit navigate/playStory to avoid loops
  }, [
    playFromState.playStoryId,
    playFromState.playStorySource,
    library.length,
    mine.length,
    favorites.length,
    loading,
    tab,
    searchParams,
    setTabAndUrl,
  ]);

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

  const getPlaybackFields = useCallback(
    (id: number, source: string, titleOverride?: string) => {
      const title = titleOverride ?? getTitleForStory(id, source);
      const fromLib = library.find((c) => c.id === id);
      if (fromLib && (source === "library" || source === "favorites")) {
        return { theme: fromLib.theme, category: fromLib.category ?? null, title };
      }
      const fromMine = mine.find((m) => m.id === id);
      if (fromMine && (source === "generated" || source === "mine" || source === "favorites")) {
        return { theme: fromMine.theme, category: null as string | null, title };
      }
      const fromSearch = searchResults.find((s) => s.storyId === id && s.storySource === source);
      if (fromSearch) {
        return { theme: fromSearch.theme, category: null as string | null, title };
      }
      return { theme: "", category: null as string | null, title };
    },
    [library, mine, searchResults, getTitleForStory]
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
      setPlayingHostClipUrl(null);
      if (blobUrlRef.current) {
        URL.revokeObjectURL(blobUrlRef.current);
        blobUrlRef.current = null;
      }
      setPlayingStoryId(null);
      setPlayingTitle(null);
      setPlayingSubtitle(null);
      setParentPanelStory(null);
      return;
    }
    audioRef.current?.pause();
    videoRef.current?.pause();
    setPlayingAvatarVideoUrl(null);
    setPlayingHostClipUrl(null);
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
      const subSrc =
        storySource === "mine" ? "generated" : storySource === "favorites" ? "favorites" : storySource;
      const pf = getPlaybackFields(storyId, storySource, titleOverride);
      setPlayingSubtitle(listenerPlaybackSubtitle(subSrc, pf.theme, pf.category, pf.title));
      if (storySource === "library") {
        setParentPanelStory(library.find((c) => c.id === storyId) ?? null);
      } else {
        setParentPanelStory(null);
      }

      if (data.avatarVideoUrl) {
        const resolvedVideoUrl = resolveCoverUrl(data.avatarVideoUrl) ?? data.avatarVideoUrl;
        setPlayingHostClipUrl(null);
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
      const reduceMotion =
        typeof window !== "undefined" &&
        window.matchMedia("(prefers-reduced-motion: reduce)").matches;
      const clipRaw = !reduceMotion && data.hostStoryClipUrl?.trim()
        ? resolveCoverUrl(data.hostStoryClipUrl) ?? data.hostStoryClipUrl
        : null;
      setPlayingHostClipUrl(clipRaw);
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
      setPlayingHostClipUrl(null);
      setPlayingStoryId(null);
      return;
    }
    const p = playingStoryRef.current;
    if (p && (audioRef.current?.src || playingAvatarVideoUrl)) {
      if (playingAvatarVideoUrl) {
        videoRef.current?.play().then(() => {
          setPlayingStoryId(p.id);
          setPlayingTitle(getTitleForStory(p.id, p.source));
          const subSrc = p.source === "mine" ? "generated" : p.source === "favorites" ? "favorites" : p.source;
          const pf = getPlaybackFields(p.id, p.source);
          setPlayingSubtitle(listenerPlaybackSubtitle(subSrc, pf.theme, pf.category, pf.title));
        }).catch(() => {});
      } else {
        audioRef.current!.play().then(() => {
          setPlayingStoryId(p.id);
          setPlayingTitle(getTitleForStory(p.id, p.source));
          const subSrc = p.source === "mine" ? "generated" : p.source === "favorites" ? "favorites" : p.source;
          const pf = getPlaybackFields(p.id, p.source);
          setPlayingSubtitle(listenerPlaybackSubtitle(subSrc, pf.theme, pf.category, pf.title));
        }).catch((e) => {
          const isInterrupted =
            (e instanceof DOMException && e.name === "AbortError") ||
            (e instanceof Error && /interrupted|pause/i.test(e.message));
          if (!isInterrupted) setError("Playback failed");
        });
      }
    }
  }, [playingStoryId, playingAvatarVideoUrl, getTitleForStory, getPlaybackFields]);

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
      setPlayingHostClipUrl(null);
      setPlayingStoryId(null);
      setPlayingTitle(null);
      setPlayingSubtitle(null);
      setParentPanelStory(null);
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
    const topicId = genGenerationTopicId.trim();
    const theme = genTheme.trim();
    if (!topicId && !theme) {
      setError("Choose a curated topic or enter a theme");
      return;
    }
    setGenerating(true);
    setError("");
    try {
      await generateStory({
        childName: "Listener",
        age: genAge,
        language: "ta",
        ...(topicId
          ? {
              generationTopicId: topicId,
              ...(theme ? { theme } : {}),
            }
          : { theme }),
        ...(genLearningFocus.trim() ? { learningFocus: genLearningFocus.trim() } : {}),
      });
      setTabAndUrl("mine");
      try {
        const p = await getMyStories(0, 20);
        setMine(p.content);
        setMinePage({ page: 0, totalPages: p.totalPages, last: p.last });
        setMineListError("");
      } catch {
        setMineListError("Your story was created, but we couldn’t refresh the list. Tap Try again below.");
      }
    } catch (err) {
      if (err instanceof ApiClientError) {
        if (err.code === STORY_GENERATE_ERROR_CODES.UNKNOWN_GENERATION_TOPIC) {
          setError("That curated topic isn’t available. Pick another topic or enter a theme.");
        } else if (err.code === STORY_GENERATE_ERROR_CODES.GENERATION_LANGUAGE_NOT_SUPPORTED) {
          setError("Story generation is available in Tamil only for now.");
        } else if (err.code === STORY_GENERATE_ERROR_CODES.THEME_OR_TOPIC_REQUIRED) {
          setError("Choose a curated topic or enter a theme.");
        } else {
          setError(err.message);
        }
      } else {
        setError(err instanceof Error ? err.message : "Generation failed");
      }
    } finally {
      setGenerating(false);
    }
  };

  const showPlayer = playingTitle != null || loadingStreamId != null;

  return (
    <div className={`page prime-page stories-page${showPlayer ? " has-audio-player" : ""}`}>
      <StoryAudioPlayer
        audioRef={audioRef}
        videoRef={videoRef}
        avatarVideoUrl={playingAvatarVideoUrl}
        hostClipUrl={playingHostClipUrl}
        isPlaying={playingStoryId != null}
        isLoading={loadingStreamId != null}
        title={playingTitle ?? (loadingStreamId != null ? "Loading…" : null)}
        subtitle={playingSubtitle}
        currentTime={audioCurrentTime}
        duration={audioDuration}
        onPlayPause={handleAudioPlayPause}
        onTimeUpdate={setAudioCurrentTime}
        onDurationChange={setAudioDuration}
        onSeek={handleSeek}
      />
      {parentPanelStory &&
      (parentPanelStory.parentContentNote?.trim() ||
        (parentPanelStory.parentDiscussionPrompts?.filter(Boolean).length ?? 0) > 0 ||
        parentPanelStory.speakAlongPrompt?.trim()) ? (
        <section className="stories-section stories-parent-panel" aria-label="Parent resources">
          <div className="stories-section-head">
            <h2 className="stories-section-title">For parents</h2>
            <p className="stories-section-desc muted">Discussion ideas and context for this library story.</p>
          </div>
          <div className="stories-generate-card" style={{ textAlign: "left" }}>
            {parentPanelStory.parentContentNote?.trim() ? (
              <div className="field" style={{ marginBottom: 12 }}>
                <strong>Content note</strong>
                <p className="muted" style={{ margin: "6px 0 0", whiteSpace: "pre-wrap" }}>
                  {parentPanelStory.parentContentNote.trim()}
                </p>
              </div>
            ) : null}
            {parentPanelStory.speakAlongPrompt?.trim() ? (
              <div className="field" style={{ marginBottom: 12 }}>
                <strong>Speak-along</strong>
                <p className="muted" style={{ margin: "6px 0 0", whiteSpace: "pre-wrap" }}>
                  {parentPanelStory.speakAlongPrompt.trim()}
                </p>
              </div>
            ) : null}
            {(parentPanelStory.parentDiscussionPrompts?.filter(Boolean).length ?? 0) > 0 ? (
              <div className="field">
                <strong>Discussion prompts</strong>
                <ul style={{ margin: "8px 0 0", paddingLeft: 20 }}>
                  {parentPanelStory.parentDiscussionPrompts!.filter(Boolean).map((p, i) => (
                    <li key={i} className="muted" style={{ marginBottom: 6 }}>
                      {p}
                    </li>
                  ))}
                </ul>
              </div>
            ) : null}
          </div>
        </section>
      ) : null}
      <header className="stories-page-top">
        <h1 className="stories-page-top__title">Stories</h1>
        <div className="stories-page-top__row">
          <div className="stories-page-top__search">
            <label htmlFor="stories-search" className="visually-hidden">
              Search stories
            </label>
            <span className="stories-page-top__search-icon" aria-hidden="true">
              ⌕
            </span>
            <input
              id="stories-search"
              type="search"
              className="stories-page-top__input"
              placeholder="Search…"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              autoComplete="off"
              aria-label="Search stories"
            />
          </div>
          <a href="#generate-story-section" className="stories-page-top__cta">
            New tale
          </a>
        </div>
      </header>

      {searchQuery.trim().length >= 2 && (
        <section className="stories-section stories-section--search">
          <div className="stories-section-head">
            <h2 className="stories-section-title">Search results</h2>
            <span className="stories-section-meta muted">{searchLoading ? "Searching…" : `${searchResults.length} found`}</span>
          </div>
          {searchLoading ? (
            <div className="stories-grid">
              {[1, 2, 3, 4, 5, 6].map((i) => (
                <div key={i} className="poster-card" style={{ pointerEvents: "none" }}>
                  <div className="poster-card-cover"><div className="poster-card-placeholder skeleton" style={{ margin: 0 }} /></div>
                  <p className="poster-card-title"><span className="skeleton" style={{ display: "block", height: 14, width: "80%" }} /></p>
                  <p className="poster-card-meta"><span className="skeleton" style={{ display: "block", height: 12, width: "60%" }} /></p>
                </div>
              ))}
            </div>
          ) : searchResults.length === 0 ? (
            <p className="stories-empty-hint muted">No stories match &quot;{searchQuery}&quot;. Try another word or browse the library.</p>
          ) : (
            <div className="stories-grid">
              {searchResults.map((s) => (
                <div
                  key={`${s.storySource}-${s.storyId}`}
                  className="poster-card poster-card-clickable story-card-tile"
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
                  {isFunStory(s.theme, null) ? (
                    <span className="stories-fun-badge">{funCornerBadgeLabel()}</span>
                  ) : null}
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

      <section
        className={`stories-main-panel stories-main-panel--compact${tab === "library" ? " stories-main-panel--library" : ""}`}
        aria-labelledby="stories-collections-heading"
      >
        <h2 id="stories-collections-heading" className="visually-hidden">
          Library, your stories, and favorites
        </h2>
        <div className="stories-controls-row">
          <div className="stories-tabs-wrap" role="tablist" aria-label="Story collections">
            <button
              type="button"
              role="tab"
              aria-selected={tab === "library"}
              aria-controls="stories-collections-panel"
              id="tab-library"
              className={tab === "library" ? "stories-tab stories-tab--active" : "stories-tab"}
              onClick={() => setTabAndUrl("library")}
            >
              Library
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={tab === "mine"}
              aria-controls="stories-collections-panel"
              id="tab-mine"
              className={tab === "mine" ? "stories-tab stories-tab--active" : "stories-tab"}
              onClick={() => setTabAndUrl("mine")}
            >
              My stories
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={tab === "favorites"}
              aria-controls="stories-collections-panel"
              id="tab-favorites"
              className={tab === "favorites" ? "stories-tab stories-tab--active" : "stories-tab"}
              onClick={() => setTabAndUrl("favorites")}
            >
              Favorites
              <span className="stories-tab-count">{favorites.length}</span>
            </button>
          </div>
          <div className="stories-voice-inline">
            <label htmlFor="voice-select" className="stories-voice-inline__label">
              Voice
            </label>
            <select
              id="voice-select"
              value={selectedVoice}
              onChange={(e) => setSelectedVoice(e.target.value)}
              className="stories-voice-inline__select voice-select"
              aria-describedby="voice-hint"
              title="Premium Calm voice needs an active plan"
            >
              <option value="default">Default</option>
              <option value="calm">Calm · Pass</option>
            </select>
            <span id="voice-hint" className="visually-hidden">
              Premium voices require an active Tamixa plan.
            </span>
          </div>
        </div>

        {tab === "library" ? (
          <div className="stories-library-toolbar">
            <div className="stories-library-toolbar__head">
              <h3 className="stories-library-toolbar__title">Story library</h3>
              {!libraryListError ? (
                <span className="stories-library-toolbar__count muted" aria-live="polite">
                  {loading ? "Loading…" : `${library.length} ${library.length === 1 ? "tale" : "tales"}`}
                </span>
              ) : null}
            </div>
            {libraryCategories.length > 0 ? (
              <div className="stories-library-chips-shell">
                <div className="stories-chips stories-chips--compact stories-chips--library" role="group" aria-label="Filter by theme">
                  <button
                    type="button"
                    className={libraryThemeFilter == null ? "stories-chip stories-chip--active" : "stories-chip"}
                    onClick={() => setLibraryThemeFilter(null)}
                  >
                    All
                  </button>
                  {libraryCategories.slice(0, 28).map((c) => (
                    <button
                      key={c}
                      type="button"
                      className={libraryThemeFilter === c ? "stories-chip stories-chip--active" : "stories-chip"}
                      onClick={() => setLibraryThemeFilter(c)}
                    >
                      {c}
                    </button>
                  ))}
                </div>
              </div>
            ) : null}
            <div className="stories-library-ethos" role="note">
              <p className="stories-library-ethos__title">Every listen is a little lesson</p>
              <p className="stories-library-ethos__body muted">
                Tamixa tales are written for growing minds—language, heart, and curiosity in every plot. For pure laughs and
                older classics, editors tag stories as <strong>Fun stories</strong> or <strong>Funny Stories</strong>; use those
                chips above to jump straight there.
              </p>
            </div>
          </div>
        ) : null}

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

      <div
        className="stories-tab-panel"
        id="stories-collections-panel"
        role="tabpanel"
        aria-labelledby={
          tab === "library" ? "tab-library" : tab === "mine" ? "tab-mine" : "tab-favorites"
        }
      >
      {tab === "library" && (
        <>
          {loading ? (
            <div className="stories-grid stories-library-grid">
              {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
                <div key={i} className="poster-card story-card-tile story-card-tile--skeleton" style={{ pointerEvents: "none" }}>
                  <div className="poster-card-cover"><div className="poster-card-placeholder skeleton" style={{ margin: 0 }} /></div>
                  <p className="poster-card-title"><span className="skeleton" style={{ display: "block", height: 14, width: "80%" }} /></p>
                  <p className="poster-card-meta"><span className="skeleton" style={{ display: "block", height: 12, width: "60%" }} /></p>
                </div>
              ))}
            </div>
          ) : libraryListError ? (
            <div className="stories-generate-card" style={{ textAlign: "center", padding: "24px 16px" }}>
              <p className="error" style={{ marginBottom: 16 }}>
                {libraryListError}
              </p>
              <button type="button" className="btn btn-primary" onClick={() => setLibraryRetryKey((k) => k + 1)}>
                Try again
              </button>
            </div>
          ) : library.length === 0 ? (
            <EmptyState
              emoji="📚"
              title="No stories in library"
              description="Story library is being updated. Check back soon or try generating your own."
              action={{ label: "Generate Story", onClick: () => setTabAndUrl("mine") }}
            />
          ) : (
            <div className="stories-grid stories-library-grid">
              {library.map((s) => (
                <div
                  key={s.id}
                  className="poster-card poster-card-clickable story-card-tile"
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
                  {isFunStory(s.theme, s.category) ? (
                    <span className="stories-fun-badge">{funCornerBadgeLabel()}</span>
                  ) : null}
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
            <div className="stories-grid">
              {[1, 2, 3, 4, 5, 6, 7, 8].map((i) => (
                <div key={i} className="poster-card story-card-tile story-card-tile--skeleton" style={{ pointerEvents: "none" }}>
                  <div className="poster-card-cover"><div className="poster-card-placeholder skeleton" style={{ margin: 0 }} /></div>
                  <p className="poster-card-title"><span className="skeleton" style={{ display: "block", height: 14, width: "80%" }} /></p>
                  <p className="poster-card-meta"><span className="skeleton" style={{ display: "block", height: 12, width: "60%" }} /></p>
                </div>
              ))}
            </div>
          ) : mineListError ? (
            <div className="stories-generate-card" style={{ textAlign: "center", padding: "24px 16px" }}>
              <p className="error" style={{ marginBottom: 16 }}>
                {mineListError}
              </p>
              <button type="button" className="btn btn-primary" onClick={() => setMineRetryKey((k) => k + 1)}>
                Try again
              </button>
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
            <div className="stories-grid">
              {mine.map((s) => (
                <div
                  key={s.id}
                  className="poster-card poster-card-clickable story-card-tile"
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
                  {isFunStory(s.theme, null) ? (
                    <span className="stories-fun-badge">{funCornerBadgeLabel()}</span>
                  ) : null}
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
          {favoritesListError ? (
            <div className="stories-generate-card" style={{ textAlign: "center", padding: "24px 16px" }}>
              <p className="error" style={{ marginBottom: 16 }}>
                {favoritesListError}
              </p>
              <button type="button" className="btn btn-primary" onClick={() => setFavoritesRetryKey((k) => k + 1)}>
                Try again
              </button>
            </div>
          ) : favorites.length === 0 ? (
            <EmptyState
              emoji="♥"
              title="No favorites yet"
              description="Add stories from Library or My stories to find them here."
              action={{ label: "Browse Library", onClick: () => setTabAndUrl("library") }}
            />
          ) : (
            <div className="stories-grid">
              {favorites.map((f) => {
                const s =
                  library.find((c) => c.id === f.storyId) ||
                  mine.find((m) => m.id === f.storyId);
                const coverImageUrl = s && "coverImageUrl" in s ? s.coverImageUrl : null;
                const coverVideoUrl = s && "coverVideoUrl" in s ? (s as { coverVideoUrl?: string | null }).coverVideoUrl : null;
                const title = s ? (`title` in s && s.title ? s.title : s.theme) : `Story #${f.storyId}`;
                const favCategory = s && "category" in s ? (s as LibraryStory).category : null;
                return (
                  <div
                    key={`${f.storyId}-${f.storySource}`}
                    className="poster-card poster-card-clickable story-card-tile"
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
                    {s && isFunStory(s.theme, favCategory) ? (
                      <span className="stories-fun-badge">{funCornerBadgeLabel()}</span>
                    ) : null}
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

      <section className="stories-section stories-section--footer">
        <div className="stories-section-head">
          <h2 className="stories-section-title">Feedback</h2>
        </div>
        <div className="stories-section-body">
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

      <section id="generate-story-section" className="stories-section stories-section--generate">
        <div className="stories-section-head">
          <h2 className="stories-section-title">Create a new tale</h2>
          <p className="stories-section-desc muted">
            Pick a curated topic or write your own theme. Age 1–99; optional learning focus (speaking, money, research).
          </p>
        </div>
        <div className="stories-generate-card">
        <form onSubmit={handleGenerate} className="form">
          <div className="field">
            <label htmlFor="gen-topic">Curated topic (optional)</label>
            <select
              id="gen-topic"
              value={genGenerationTopicId}
              onChange={(e) => {
                const id = e.target.value;
                setGenGenerationTopicId(id);
                const t = generationTopics.find((x) => x.id === id);
                if (t?.suggestedLearningFocus) {
                  setGenLearningFocus(t.suggestedLearningFocus);
                }
              }}
              aria-label="Curated generation topic"
            >
              <option value="">None — use custom theme only</option>
              {generationTopics.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.descriptionEn?.trim() ? `${t.descriptionEn} (${t.theme})` : t.theme}
                </option>
              ))}
            </select>
          </div>
          <div className="field">
            <label htmlFor="theme">Custom theme {genGenerationTopicId.trim() ? "(optional)" : ""}</label>
            <input
              id="theme"
              value={genTheme}
              onChange={(e) => setGenTheme(e.target.value)}
              placeholder={genGenerationTopicId.trim() ? "Override or add detail…" : "e.g. Animals, Bedtime, Pongal"}
            />
          </div>
          <div className="field">
            <label htmlFor="age">Age (1–99)</label>
            <input
              id="age"
              type="number"
              min={1}
              max={99}
              value={genAge}
              onChange={(e) => {
                const n = parseInt(e.target.value, 10);
                if (Number.isNaN(n)) setGenAge(5);
                else setGenAge(Math.min(99, Math.max(1, n)));
              }}
            />
          </div>
          <div className="field">
            <label htmlFor="learning-focus">Learning focus (optional)</label>
            <select
              id="learning-focus"
              value={genLearningFocus}
              onChange={(e) => setGenLearningFocus(e.target.value)}
              aria-label="Learning focus"
            >
              <option value="">None</option>
              <option value="public_speaking">Public speaking</option>
              <option value="money_literacy">Money smarts</option>
              <option value="research_skills">Research &amp; facts</option>
              <option value="empathy">Empathy</option>
              <option value="problem_solving">Problem solving</option>
              <option value="vocabulary">Vocabulary</option>
              <option value="curiosity">Curiosity</option>
            </select>
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
