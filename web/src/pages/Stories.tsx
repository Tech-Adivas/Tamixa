import { useState, useEffect, useRef, useCallback, useMemo } from "react";
import { Link, useSearchParams, useLocation, useNavigate } from "react-router-dom";
import StoryAudioPlayer from "../components/StoryAudioPlayer";
import TamixaSimulatorPlayer from "../components/TamixaSimulatorPlayer";
import MissionCardOverlay from "../components/MissionCardOverlay";
import FinancialRealityHud from "../components/FinancialRealityHud";
import ScamDetectionHud from "../components/ScamDetectionHud";
import TimePassageSummaryModal from "../components/TimePassageSummaryModal";
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
  getAvailableVoices,
  getVoicePreference,
  setVoicePreference,
  voicePreferenceStorySource,
  uploadFamilyVoice,
  deleteFamilyVoice,
  getTimeline,
  getNarrationScript,
  recordLifeSkillChoice,
  getProfile,
  prepareDigitalSurvivalDevE2eSeed,
  trackAppEventLibraryHub,
  trackStoryInteractiveBranch,
  reportStreamAnalytics,
  ApiClientError,
  STORY_GENERATE_ERROR_CODES,
  type LibraryStory,
  type Story,
  type SearchStoryItem,
  type GenerationTopic,
  type VoiceOption,
  type PlaybackManifest,
  type ProfileChildDto,
} from "../lib/api";
import { ROUTES } from "../lib/appRoutes";
import type { StreamUrlResponse } from "../lib/api";
import { stripNarrationMarkers } from "../lib/narrationTextUtils";

function narrativeSceneFractions(
  scenes: { startProgress: number }[] | null | undefined
): number[] {
  if (!scenes?.length) return [];
  const out: number[] = [];
  for (const s of scenes) {
    const v = Number(s.startProgress);
    if (Number.isFinite(v) && v > 0.02 && v < 0.98) out.push(v);
  }
  return [...new Set(out)].sort((a, b) => a - b);
}
import {
  getPreferredVoiceProfile,
  getOnboardingVoiceAvatarDismissed,
  setOnboardingVoiceAvatarDismissed,
} from "../lib/listenerPreferences";
import { bumpLifeSkillCountersRefresh } from "../lib/lifeSkillPreferences";
import {
  parseInteractiveStoryGraph,
  resolveInteractiveSegmentAudioUrl,
  libraryRowForInteractivePlayback,
  impactFromInteractiveChoice,
  resolveInteractiveChoiceNavigation,
  type InteractiveStoryGraph,
  type InteractiveChoice,
} from "../lib/interactiveStoryGraph";
import {
  clampMeter,
  consumeEthicsShortcutPending,
  getSimulatorPlaybook,
  peekEthicsShortcutPending,
  setEthicsShortcutPending,
  type SimulatorPlaybookKind,
} from "../lib/categoryLogicFactory";
import {
  applyImpactStats,
  createWebLocalStorageStore,
  DEFAULT_USER_LIFE_PROFILE,
  loadUserLifeProfileFromStore,
  saveUserLifeProfileToStore,
} from "../lib/branchingEduStory";
import type { FamilyEconomy } from "../lib/financialRealityEngine";
import {
  applyFinancialChoice,
  applyMonthlyBurn,
  DEFAULT_FAMILY_ECONOMY,
  graphUsesFinancialReality,
  isLiquidityCrisis,
  simulateTimePassage,
} from "../lib/financialRealityEngine";
import { profileToLifeReadinessSnapshot } from "../lib/lifeReadinessModel";
import {
  getPerspective,
  segmentContentBodyForRole,
  setPerspective,
  type FamilyPerspectiveRole,
} from "../lib/tamixaFamilyBridge";
import { useAuth } from "../contexts/AuthContext";
import { EmptyState } from "../components/EmptyState";
import {
  isFunStory,
  isLearnStory,
  isLearnOrDigitalSafetyStory,
  isInteractivePracticeLibraryStory,
  isSimulatorStory,
  funCornerBadgeLabel,
  interactivePracticeBadgeLabel,
  listenerPlaybackSubtitle,
} from "../lib/storyListenerUi";
import { normalizeLibraryHub, type LibraryHub } from "../lib/libraryHub";

type Tab = "library" | "mine" | "favorites";

type PlaybackMode = "default" | "my_voice" | "avatar";

function interactiveBadgeForSearchHit(theme: string): boolean {
  return isSimulatorStory(theme, null);
}

function dedupeVoicesForPicker(voices: VoiceOption[]): VoiceOption[] {
  const hasCloned = voices.some((v) => v.voiceProfile.toLowerCase().startsWith("cloned:"));
  if (hasCloned) return voices.filter((v) => v.voiceProfile.toLowerCase() !== "family");
  return voices;
}

function sanitizePlaybackMode(mode: string, voiceProfile: string): PlaybackMode {
  const m = mode === "my_voice" || mode === "avatar" ? mode : "default";
  const voiceIsMy =
    voiceProfile.toLowerCase().startsWith("cloned:") || voiceProfile.toLowerCase() === "family";
  if (m === "avatar" && !voiceIsMy) return "default";
  return m;
}

function voiceLabelForOption(v: VoiceOption): string {
  if (v.displayLabel?.trim()) return v.displayLabel.trim();
  const p = v.voiceProfile;
  if (p === "default") return "Default";
  if (p.toLowerCase() === "calm") return v.isPremium ? "Calm (plan)" : "Calm";
  if (p.toLowerCase() === "family") return "My voice (this story)";
  if (p.toLowerCase().startsWith("cloned:")) return `My voice (${p})`;
  return p;
}

type KaraokeSegment = { startSec: number; endSec: number; text: string };

/** Match mobile AudioPlayerScreen — small lead so captions track perceived speech. */
const READ_ALONG_TIMING_OFFSET_SEC = 0.08;

function flattenPlaybackManifest(m: PlaybackManifest | null): KaraokeSegment[] {
  if (!m?.scenes?.length) return [];
  let ms = 0;
  const out: KaraokeSegment[] = [];
  for (const sc of m.scenes) {
    for (const seg of sc.segments ?? []) {
      const startSec = ms / 1000;
      ms += seg.durationMs > 0 ? seg.durationMs : 0;
      const text = seg.text?.trim() ?? "";
      if (text) out.push({ startSec, endSec: ms / 1000, text });
    }
  }
  return out;
}

function captionFromWordTimings(
  timings: { word: string; startSec: number; endSec: number }[],
  t: number
): string | null {
  if (!timings.length) return null;
  const idx = timings.findIndex((w) => t >= w.startSec && t < w.endSec);
  const i = idx >= 0 ? idx : timings.findIndex((w) => t < w.startSec);
  const center = i >= 0 ? i : Math.max(0, timings.length - 1);
  const from = Math.max(0, center - 5);
  const to = Math.min(timings.length, center + 8);
  return timings
    .slice(from, to)
    .map((w) => w.word)
    .join(" ")
    .trim();
}

function captionFromSegments(segments: KaraokeSegment[], t: number): string | null {
  const hit = segments.find((s) => t >= s.startSec && t < s.endSec);
  return hit?.text?.trim() ? hit.text.trim() : null;
}

function timelineApiStorySource(
  storySourceUi: string,
  storyId: number,
  favorites: { storyId: number; storySource: string }[]
): string {
  if (storySourceUi === "favorites") {
    const f = favorites.find((x) => x.storyId === storyId);
    return f?.storySource === "generated" ? "generated" : "library";
  }
  if (storySourceUi === "mine") return "generated";
  if (storySourceUi === "library") return "library";
  return "generated";
}

function resolveLifeSkillChildIdForWeb(
  mine: Story[],
  profileFallback: number | null
): number | null {
  for (const s of mine) {
    if (s.childId != null && s.childId > 0) return s.childId;
  }
  return profileFallback;
}

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
  const { user } = useAuth();
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
  const parentPanelStoryRef = useRef<LibraryStory | null>(null);
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
  const [playbackVoiceProfile, setPlaybackVoiceProfile] = useState("default");
  const [playbackMode, setPlaybackMode] = useState<PlaybackMode>("default");
  const [playbackVoices, setPlaybackVoices] = useState<VoiceOption[]>([]);
  const [playbackAvatarStatus, setPlaybackAvatarStatus] = useState<string | null>(null);
  const [familyVoiceBusy, setFamilyVoiceBusy] = useState(false);
  const playbackContextRef = useRef<{
    storyId: number;
    storySourceUi: string;
    titleOverride?: string;
  } | null>(null);
  const familyVoiceFileRef = useRef<HTMLInputElement>(null);
  const browserTtsTextRef = useRef<string>("");
  const [playbackWordTimings, setPlaybackWordTimings] = useState<
    { word: string; startSec: number; endSec: number }[] | null
  >(null);
  const [playbackKaraokeSegments, setPlaybackKaraokeSegments] = useState<KaraokeSegment[]>([]);
  const [playbackChapterFractions, setPlaybackChapterFractions] = useState<number[]>([]);
  const [playbackUsesBrowserTts, setPlaybackUsesBrowserTts] = useState(false);
  const [showVoiceOnboarding, setShowVoiceOnboarding] = useState(false);
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
  const streamRequestStartMsRef = useRef<number | null>(null);
  const streamAnalyticsLatencySentRef = useRef(false);
  const streamAnalyticsBufferSentRef = useRef(false);
  const profileChildIdFallbackRef = useRef<number | null>(null);
  const [interactivePlayback, setInteractivePlayback] = useState<{
    storyId: number;
    graph: InteractiveStoryGraph;
    currentSegmentId: string;
    overlayStyle: string | null;
  } | null>(null);
  const interactivePlaybackRef = useRef<typeof interactivePlayback>(null);
  const [interactiveChoiceOpen, setInteractiveChoiceOpen] = useState(false);
  const [interactiveMissionOpen, setInteractiveMissionOpen] = useState(false);
  const [simulatorMeters, setSimulatorMeters] = useState({
    authority: 50,
    harmony: 50,
    confidence: 38,
  });
  const lastInteractiveStoryIdRef = useRef<number | null>(null);
  const [profileChildren, setProfileChildren] = useState<ProfileChildDto[]>([]);
  const [dsgDevPrepareBusy, setDsgDevPrepareBusy] = useState(false);
  const [dsgDevPrepareHint, setDsgDevPrepareHint] = useState<string | null>(null);
  const [familyPerspectiveRole, setFamilyPerspectiveRole] = useState<FamilyPerspectiveRole>(() =>
    typeof window !== "undefined" ? getPerspective() : "Child"
  );
  const [scamProofUnlocked, setScamProofUnlocked] = useState(false);
  const [familyEconomy, setFamilyEconomy] = useState<FamilyEconomy | null>(null);
  const familyEconomyRef = useRef<FamilyEconomy | null>(null);
  const [timePassageModal, setTimePassageModal] = useState<{ title: string; lines: string[] } | null>(null);

  const libraryLanguageCode = useMemo(() => {
    const first = profileChildren.find((c) => c.languagePreference?.trim());
    const lp = first?.languagePreference?.trim().toLowerCase();
    if (lp) {
      if (lp.startsWith("en")) return "en";
      if (lp.startsWith("ta")) return "ta";
      const two = lp.slice(0, 2);
      if (/^[a-z]{2}$/.test(two)) return two;
    }
    if (typeof navigator !== "undefined" && navigator.language?.toLowerCase().startsWith("en")) {
      return "en";
    }
    return "ta";
  }, [profileChildren]);

  useEffect(() => {
    interactivePlaybackRef.current = interactivePlayback;
  }, [interactivePlayback]);

  useEffect(() => {
    parentPanelStoryRef.current = parentPanelStory;
  }, [parentPanelStory]);

  useEffect(() => {
    familyEconomyRef.current = familyEconomy;
  }, [familyEconomy]);

  useEffect(() => {
    const store = createWebLocalStorageStore();
    loadUserLifeProfileFromStore(store)
      .then((s) => {
        const p = s?.profile ?? DEFAULT_USER_LIFE_PROFILE;
        setScamProofUnlocked(profileToLifeReadinessSnapshot(p).tech >= 70);
      })
      .catch(() => setScamProofUnlocked(false));
  }, []);

  useEffect(() => {
    if (!interactiveMissionOpen) return;
    const store = createWebLocalStorageStore();
    loadUserLifeProfileFromStore(store)
      .then((s) => {
        const p = s?.profile ?? DEFAULT_USER_LIFE_PROFILE;
        setScamProofUnlocked(profileToLifeReadinessSnapshot(p).tech >= 70);
      })
      .catch(() => {});
  }, [interactiveMissionOpen]);

  const familyBridgeSegmentCaption = useMemo(() => {
    if (!interactivePlayback) return null;
    const seg = interactivePlayback.graph.segments[interactivePlayback.currentSegmentId];
    const cap = segmentContentBodyForRole(seg, familyPerspectiveRole);
    return cap.trim() ? cap : null;
  }, [interactivePlayback, familyPerspectiveRole]);

  useEffect(() => {
    const sid = interactivePlayback?.storyId;
    if (sid == null) {
      lastInteractiveStoryIdRef.current = null;
      return;
    }
    if (lastInteractiveStoryIdRef.current !== sid) {
      lastInteractiveStoryIdRef.current = sid;
      setSimulatorMeters({ authority: 50, harmony: 50, confidence: 38 });
    }
  }, [interactivePlayback]);

  const simulatorPlaybook: SimulatorPlaybookKind = useMemo(() => {
    if (!interactivePlayback || !parentPanelStory) return "default";
    return getSimulatorPlaybook(
      parentPanelStory.theme ?? "",
      parentPanelStory.category ?? null,
      interactivePlayback.graph.eduCategory
    );
  }, [interactivePlayback, parentPanelStory]);

  const interactiveChoicePrefetchUrls = useMemo(() => {
    if (!interactiveChoiceOpen || !interactivePlayback) return [];
    const seg = interactivePlayback.graph.segments[interactivePlayback.currentSegmentId];
    const urls: (string | null)[] = [];
    for (const choice of seg?.choices ?? []) {
      const next = interactivePlayback.graph.segments[choice.nextSegmentId];
      if (next) urls.push(resolveInteractiveSegmentAudioUrl(next.audioUrl));
      const alt = choice.consequenceSegmentId?.trim();
      if (alt) {
        const cons = interactivePlayback.graph.segments[alt];
        if (cons) urls.push(resolveInteractiveSegmentAudioUrl(cons.audioUrl));
      }
    }
    return urls.filter((u): u is string => Boolean(u));
  }, [interactiveChoiceOpen, interactivePlayback]);

  /** Keep `?tab=` in sync when the user picks a tab (shareable / back button). */
  const setTabAndUrl = useCallback(
    (next: Tab) => {
      setTab(next);
      setSearchParams(
        (prev) => {
          const p = new URLSearchParams(prev);
          p.set("tab", next);
          if (next !== "library") p.delete("hub");
          return p;
        },
        { replace: true }
      );
    },
    [setSearchParams]
  );

  const libraryHubParam = searchParams.get("hub");
  const libraryHub: LibraryHub =
    tab === "library" ? normalizeLibraryHub(libraryHubParam) : "browse";

  const setLibraryHubInUrl = useCallback(
    (hub: LibraryHub) => {
      if (hub !== "browse") setLibraryThemeFilter(null);
      setTab("library");
      setSearchParams(
        (prev) => {
          const p = new URLSearchParams(prev);
          p.set("tab", "library");
          if (hub === "browse") p.delete("hub");
          else p.set("hub", hub);
          return p;
        },
        { replace: true }
      );
    },
    [setSearchParams]
  );

  const lastTrackedLibraryHubRef = useRef("");
  useEffect(() => {
    if (tab !== "library") {
      lastTrackedLibraryHubRef.current = "";
      return;
    }
    const hub = normalizeLibraryHub(libraryHubParam);
    const hubKey =
      hub === "browse" ? "browse" : hub === "fun" ? "fun" : hub === "learn" ? "learn" : "simulator";
    const dedupe = `${tab}:${hubKey}`;
    if (lastTrackedLibraryHubRef.current === dedupe) return;
    lastTrackedLibraryHubRef.current = dedupe;
    void trackAppEventLibraryHub(hubKey);
  }, [tab, libraryHubParam]);

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
    setShowVoiceOnboarding(!getOnboardingVoiceAvatarDismissed());
  }, []);

  useEffect(() => {
    if (!user) {
      profileChildIdFallbackRef.current = null;
      setProfileChildren([]);
      return;
    }
    getProfile()
      .then((p) => {
        setProfileChildren(p.children ?? []);
        const first = (p.children ?? []).map((c) => c.id).find((id) => id > 0);
        profileChildIdFallbackRef.current = first ?? null;
      })
      .catch(() => {
        setProfileChildren([]);
        profileChildIdFallbackRef.current = null;
      });
  }, [user]);

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
    getLibraryCategories(libraryLanguageCode)
      .then(setLibraryCategories)
      .catch(() => setLibraryCategories([]));
    getGenerationTopics()
      .then(setGenerationTopics)
      .catch(() => setGenerationTopics([]));
  }, [libraryLanguageCode]);

  useEffect(() => {
    if (tab !== "library") return;
    let cancelled = false;
    setLoading(true);
    setLibraryListError("");
    const hub = normalizeLibraryHub(libraryHubParam);
    const themeForApi = hub === "browse" ? libraryThemeFilter : null;
    const pageSize = hub === "browse" ? 50 : 100;
    getLibraryStories(libraryLanguageCode, 0, pageSize, themeForApi, false)
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
  }, [tab, libraryThemeFilter, libraryRetryKey, libraryHubParam, libraryLanguageCode]);

  const displayLibrary = useMemo(() => {
    if (tab !== "library") return [];
    switch (libraryHub) {
      case "fun":
        return library.filter((s) => isFunStory(s.theme, s.category));
      case "learn":
        return library.filter((s) => isLearnOrDigitalSafetyStory(s.theme, s.category));
      case "simulator":
        return library.filter((s) => isInteractivePracticeLibraryStory(s));
      default:
        return library;
    }
  }, [tab, library, libraryHub]);

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

  const getPrefSourceForStory = useCallback(
    (storyId: number, storySourceUi: string): string => {
      if (storySourceUi === "favorites") {
        const hit = favorites.find((f) => f.storyId === storyId);
        return voicePreferenceStorySource(hit?.storySource ?? "library");
      }
      return voicePreferenceStorySource(storySourceUi);
    },
    [favorites]
  );

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

  const resolveStoryPlainText = useCallback(
    (storyId: number, storySourceUi: string): string | null => {
      const clean = (raw: string | null | undefined): string | null => {
        const t = raw?.trim();
        if (!t) return null;
        const s = stripNarrationMarkers(t);
        return s.length > 0 ? s : null;
      };
      if (storySourceUi === "library") {
        return clean(library.find((c) => c.id === storyId)?.content);
      }
      if (storySourceUi === "generated" || storySourceUi === "mine") {
        return clean(mine.find((m) => m.id === storyId)?.content);
      }
      if (storySourceUi === "favorites") {
        const fromLib = clean(library.find((c) => c.id === storyId)?.content);
        if (fromLib) return fromLib;
        return clean(mine.find((m) => m.id === storyId)?.content);
      }
      return null;
    },
    [library, mine]
  );

  const clearPlaybackAfterEnd = useCallback(() => {
    if (typeof window !== "undefined" && window.speechSynthesis) {
      window.speechSynthesis.cancel();
    }
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
    playbackContextRef.current = null;
    setPlaybackAvatarStatus(null);
    setPlaybackWordTimings(null);
    setPlaybackKaraokeSegments([]);
    setPlaybackChapterFractions([]);
    setPlaybackUsesBrowserTts(false);
    browserTtsTextRef.current = "";
    setInteractivePlayback(null);
    setInteractiveChoiceOpen(false);
    setInteractiveMissionOpen(false);
    familyEconomyRef.current = null;
    setFamilyEconomy(null);
    setTimePassageModal(null);
    streamRequestStartMsRef.current = null;
    streamAnalyticsLatencySentRef.current = false;
    streamAnalyticsBufferSentRef.current = false;
  }, []);

  const persistLifeBarFromChoice = useCallback(async (ch: InteractiveChoice) => {
    const impact = impactFromInteractiveChoice(ch);
    if (Object.keys(impact).length === 0) return;
    try {
      const store = createWebLocalStorageStore();
      const prev = await loadUserLifeProfileFromStore(store);
      const base = prev?.profile ?? DEFAULT_USER_LIFE_PROFILE;
      await saveUserLifeProfileToStore(store, applyImpactStats(base, impact));
    } catch {
      /* best-effort offline Life Bar */
    }
  }, []);

  const handleInteractiveChoice = useCallback(
    async (ch: InteractiveChoice) => {
      const prev = interactivePlayback;
      if (!prev) return;
      void persistLifeBarFromChoice(ch);
      void trackStoryInteractiveBranch(prev.storyId, "library", "ta");
      const cid = resolveLifeSkillChildIdForWeb(mine, profileChildIdFallbackRef.current);
      if (cid != null) {
        const ok = await recordLifeSkillChoice({
          libraryStoryId: prev.storyId,
          childId: cid,
          segmentId: prev.currentSegmentId,
          choiceId: ch.id,
          skillDeltas: ch.skillDeltas ?? undefined,
        });
        if (ok) bumpLifeSkillCountersRefresh();
      }
      let nextSegmentId = resolveInteractiveChoiceNavigation(ch);
      const econRef = familyEconomyRef.current;
      if (econRef && graphUsesFinancialReality(prev.graph)) {
        let e = applyMonthlyBurn(econRef);
        e = applyFinancialChoice(e, ch.financialImpact, ch.id);
        familyEconomyRef.current = e;
        setFamilyEconomy(e);
        if (isLiquidityCrisis(e)) {
          const crisis =
            ch.financialImpact?.crisisChapterSegmentId?.trim() ||
            prev.graph.defaultCrisisSegmentId?.trim();
          if (crisis && prev.graph.segments[crisis]) {
            nextSegmentId = crisis;
          }
        }
      }
      if (ch.isShortcut === true && !ch.consequenceSegmentId?.trim()) {
        setEthicsShortcutPending(prev.storyId);
      }
      setSimulatorMeters((m) => ({
        authority: clampMeter(m.authority + (ch.authorityDelta ?? 0)),
        harmony: clampMeter(m.harmony + (ch.harmonyDelta ?? 0)),
        confidence: clampMeter(
          m.confidence +
            (ch.dialogueStyle === "simple_clear"
              ? 10
              : ch.dialogueStyle === "complex_or_performative"
                ? 3
                : 0)
        ),
      }));
      setInteractiveChoiceOpen(false);
      setInteractivePlayback({ ...prev, currentSegmentId: nextSegmentId });
    },
    [interactivePlayback, mine, persistLifeBarFromChoice]
  );

  const handleSimulateThreeMonths = useCallback(() => {
    const e = familyEconomyRef.current;
    if (!e) return;
    const r = simulateTimePassage(e, 3);
    familyEconomyRef.current = r.economy;
    setFamilyEconomy(r.economy);
    setTimePassageModal({ title: "Three months later", lines: r.summaryLines });
  }, []);

  const dismissInteractiveMission = useCallback(() => {
    setInteractiveMissionOpen(false);
    clearPlaybackAfterEnd();
  }, [clearPlaybackAfterEnd]);

  useEffect(() => {
    if (!interactivePlayback) return;
    const { graph, currentSegmentId, storyId } = interactivePlayback;
    const gate = graph.segments[currentSegmentId]?.ethicsConsequenceRedirectIfShortcut?.trim();
    if (gate && peekEthicsShortcutPending(storyId)) {
      const targetSeg = graph.segments[gate];
      if (targetSeg) {
        consumeEthicsShortcutPending(storyId);
        setInteractivePlayback((p) => (p ? { ...p, currentSegmentId: gate } : null));
        return;
      }
      consumeEthicsShortcutPending(storyId);
    }
    const seg = graph.segments[currentSegmentId];
    if (!seg) {
      setError("This episode’s interactive segment is missing.");
      return;
    }
    let cancelled = false;
    const run = async () => {
      setLoadingStreamId(storyId);
      setInteractiveChoiceOpen(false);
      setError("");
      const raw = resolveInteractiveSegmentAudioUrl(seg.audioUrl);
      if (!raw) {
        if (!cancelled) {
          setError("No audio for this segment.");
          setLoadingStreamId(null);
        }
        return;
      }
      if (blobUrlRef.current) {
        URL.revokeObjectURL(blobUrlRef.current);
        blobUrlRef.current = null;
      }
      const audio = audioRef.current;
      if (!audio) {
        if (!cancelled) setLoadingStreamId(null);
        return;
      }
      try {
        let playUrl = raw;
        const streamOrigin = new URL(raw).origin;
        if (streamOrigin === getApiOrigin()) {
          playUrl = await fetchStreamAsBlobUrl(raw);
          blobUrlRef.current = playUrl;
        }
        if (cancelled) return;
        audio.pause();
        audio.src = playUrl;
        audio.currentTime = 0;
        setAudioCurrentTime(0);
        setAudioDuration(0);
        await new Promise<void>((resolve, reject) => {
          const onMeta = () => {
            audio.removeEventListener("loadedmetadata", onMeta);
            audio.removeEventListener("error", onErr);
            resolve();
          };
          const onErr = () => {
            audio.removeEventListener("loadedmetadata", onMeta);
            audio.removeEventListener("error", onErr);
            reject(new Error("Audio failed to load"));
          };
          audio.addEventListener("loadedmetadata", onMeta);
          audio.addEventListener("error", onErr);
        });
        if (cancelled) return;
        setAudioDuration(Number.isFinite(audio.duration) ? audio.duration : 0);
        await audio.play();
        if (cancelled) return;
        setPlayingStoryId(storyId);
      } catch (e) {
        if (!cancelled) setError(e instanceof Error ? e.message : "Playback failed");
      } finally {
        if (!cancelled) setLoadingStreamId(null);
      }
    };
    void run();
    return () => {
      cancelled = true;
    };
  }, [interactivePlayback]);

  useEffect(() => {
    if (!interactivePlayback || interactiveChoiceOpen || interactiveMissionOpen) return;
    if (playbackUsesBrowserTts || playingAvatarVideoUrl) return;
    if (playingStoryId !== interactivePlayback.storyId) return;
    const seg = interactivePlayback.graph.segments[interactivePlayback.currentSegmentId];
    if (!seg) return;
    const d = audioDuration;
    const t = audioCurrentTime;
    if (d <= 0 || !Number.isFinite(d)) return;
    const frac = t / d;
    const ps = parentPanelStoryRef.current;
    const hasMission =
      ps != null &&
      ((ps.postStoryMission?.trim() ?? "") !== "" || (ps.postStoryResourceUrl?.trim() ?? "") !== "");
    /* Primary: audio `ended` opens choices; this covers browsers that omit `ended` near EOF */
    if (seg.choices && seg.choices.length > 0 && frac >= 0.999) {
      setInteractiveChoiceOpen(true);
      audioRef.current?.pause();
      return;
    }
    if ((!seg.choices || seg.choices.length === 0) && hasMission && frac >= 0.995) {
      setInteractiveMissionOpen(true);
      audioRef.current?.pause();
    }
  }, [
    audioCurrentTime,
    audioDuration,
    interactivePlayback,
    interactiveChoiceOpen,
    interactiveMissionOpen,
    playingStoryId,
    playbackUsesBrowserTts,
    playingAvatarVideoUrl,
  ]);

  type StreamLoadResult = "ok" | "upgrade" | "no_audio" | "aborted";

  async function loadAndPlayStream(
    storyId: number,
    storySourceUi: string,
    voiceProfile: string,
    playbackModeParam: PlaybackMode,
    startPositionSeconds: number | undefined,
    titleOverride: string | undefined
  ): Promise<StreamLoadResult> {
    if (typeof window !== "undefined" && window.speechSynthesis) {
      window.speechSynthesis.cancel();
    }
    setPlaybackUsesBrowserTts(false);
    streamRequestStartMsRef.current = performance.now();
    streamAnalyticsLatencySentRef.current = false;
    streamAnalyticsBufferSentRef.current = false;
    const sourceForApi = storySourceUi === "mine" ? "generated" : storySourceUi;
    const voiceToUse = voiceProfile === "default" ? null : voiceProfile;
    let data: StreamUrlResponse | null;
    try {
      data = await getStreamUrl(storyId, "ta", voiceToUse, sourceForApi, playbackModeParam);
    } catch (e) {
      if (e instanceof ApiClientError && e.httpStatus === 402) return "upgrade";
      throw e;
    }
    if (!data && voiceToUse != null) {
      try {
        data = await getStreamUrl(storyId, "ta", null, sourceForApi, playbackModeParam);
      } catch (e2) {
        if (e2 instanceof ApiClientError && e2.httpStatus === 402) return "upgrade";
        throw e2;
      }
    }
    if (playIntentRef.current !== storyId) return "aborted";
    if (!data?.streamUrl) return "no_audio";

    playingStoryRef.current = { id: storyId, source: storySourceUi };
    setPlayingTitle(titleOverride ?? getTitleForStory(storyId, storySourceUi));
    const subSrc =
      storySourceUi === "mine" ? "generated" : storySourceUi === "favorites" ? "favorites" : storySourceUi;
    const pf = getPlaybackFields(storyId, storySourceUi, titleOverride);
    setPlayingSubtitle(listenerPlaybackSubtitle(subSrc, pf.theme, pf.category, pf.title));
    if (storySourceUi === "library") {
      setParentPanelStory(library.find((c) => c.id === storyId) ?? null);
    } else {
      setParentPanelStory(null);
    }

    setPlaybackAvatarStatus(data.avatarStatus?.trim() ? data.avatarStatus : null);
    setPlaybackWordTimings(
      Array.isArray(data.wordTimings) && data.wordTimings.length > 0 ? data.wordTimings : null
    );
    setPlaybackChapterFractions(narrativeSceneFractions(data.narrativeScenes));
    setPlaybackUsesBrowserTts(false);

    if (data.avatarVideoUrl) {
      const resolvedVideoUrl = resolveCoverUrl(data.avatarVideoUrl) ?? data.avatarVideoUrl;
      setPlayingHostClipUrl(null);
      setPlayingAvatarVideoUrl(resolvedVideoUrl);
      setPlayingStoryId(storyId);
      const d =
        typeof data.durationSeconds === "number" && Number.isFinite(data.durationSeconds) && data.durationSeconds > 0
          ? data.durationSeconds
          : 0;
      setAudioDuration(d);
      const start = startPositionSeconds != null && startPositionSeconds > 0 ? startPositionSeconds : 0;
      setAudioCurrentTime(start);
      queueMicrotask(() => {
        if (videoRef.current && start > 0) videoRef.current.currentTime = start;
      });
      queueMicrotask(() => {
        const v = videoRef.current;
        if (!v || playingStoryRef.current?.id !== storyId) return;
        const onPlaying = () => {
          v.removeEventListener("playing", onPlaying);
          if (streamAnalyticsLatencySentRef.current) return;
          const t0 = streamRequestStartMsRef.current;
          if (t0 == null) return;
          streamAnalyticsLatencySentRef.current = true;
          const ms = performance.now() - t0;
          void reportStreamAnalytics({
            storyId,
            streamStartLatencyMs: Math.min(60_000, Math.max(0, ms)),
          });
        };
        v.addEventListener("playing", onPlaying, { once: true });
      });
      return "ok";
    }

    const audio = audioRef.current;
    if (!audio) {
      setError("Audio player not ready");
      return "no_audio";
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
      return "no_audio";
    }
    if (playIntentRef.current !== storyId) return "aborted";
    audio.src = playUrl;
    if (startPositionSeconds != null && startPositionSeconds > 0) {
      audio.currentTime = startPositionSeconds;
    }
    const reduceMotion =
      typeof window !== "undefined" && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const clipRaw = !reduceMotion && data.hostStoryClipUrl?.trim()
      ? resolveCoverUrl(data.hostStoryClipUrl) ?? data.hostStoryClipUrl
      : null;
    setPlayingHostClipUrl(clipRaw);
    setPlayingAvatarVideoUrl(null);
    const onPlayingAudio = () => {
      audio.removeEventListener("playing", onPlayingAudio);
      if (streamAnalyticsLatencySentRef.current) return;
      const t0 = streamRequestStartMsRef.current;
      if (t0 == null) return;
      streamAnalyticsLatencySentRef.current = true;
      const ms = performance.now() - t0;
      void reportStreamAnalytics({
        storyId,
        streamStartLatencyMs: Math.min(60_000, Math.max(0, ms)),
      });
    };
    audio.addEventListener("playing", onPlayingAudio, { once: true });
    await audio.play();
    if (playIntentRef.current !== storyId) return "aborted";
    setPlayingStoryId(storyId);
    setAudioDuration(audio.duration || 0);
    setAudioCurrentTime(audio.currentTime || 0);
    return "ok";
  }

  const beginBrowserTtsPlayback = useCallback(
    (storyId: number, storySourceUi: string, script: string, titleOverride?: string): boolean => {
      const trimmed = stripNarrationMarkers(script.trim());
      if (!trimmed) return false;
      if (typeof window === "undefined" || !window.speechSynthesis) {
        setError("This browser does not support read-aloud fallback.");
        return false;
      }
      window.speechSynthesis.cancel();
      browserTtsTextRef.current = trimmed;
      setPlaybackWordTimings(null);
      setPlaybackChapterFractions([]);
      setPlaybackUsesBrowserTts(true);
      playingStoryRef.current = { id: storyId, source: storySourceUi };
      setPlayingTitle(titleOverride ?? getTitleForStory(storyId, storySourceUi));
      const subSrc =
        storySourceUi === "mine" ? "generated" : storySourceUi === "favorites" ? "favorites" : storySourceUi;
      const pf = getPlaybackFields(storyId, storySourceUi, titleOverride);
      setPlayingSubtitle(listenerPlaybackSubtitle(subSrc, pf.theme, pf.category, pf.title));
      if (storySourceUi === "library") {
        setParentPanelStory(library.find((c) => c.id === storyId) ?? null);
      } else {
        setParentPanelStory(null);
      }
      setPlaybackAvatarStatus(null);
      setPlayingAvatarVideoUrl(null);
      setPlayingHostClipUrl(null);
      setAudioDuration(0);
      setAudioCurrentTime(0);
      setPlayingStoryId(storyId);
      const u = new SpeechSynthesisUtterance(trimmed);
      u.lang = "ta-IN";
      u.onend = () => {
        clearPlaybackAfterEnd();
      };
      u.onerror = () => {
        setError("Read-aloud was interrupted.");
        clearPlaybackAfterEnd();
      };
      window.speechSynthesis.speak(u);
      return true;
    },
    [library, getTitleForStory, getPlaybackFields, clearPlaybackAfterEnd]
  );

  const reloadStreamForPreferenceChange = async (
    storyId: number,
    storySourceUi: string,
    voiceProfile: string,
    mode: PlaybackMode,
    titleOverride?: string
  ) => {
    const pos = playingAvatarVideoUrl
      ? Math.floor(videoRef.current?.currentTime ?? 0)
      : Math.floor(audioRef.current?.currentTime ?? 0);
    playIntentRef.current = storyId;
    setLoadingStreamId(storyId);
    setError("");
    try {
      const result = await loadAndPlayStream(storyId, storySourceUi, voiceProfile, mode, pos, titleOverride);
      if (result === "upgrade") {
        setError("Premium voice or avatar needs an active plan. See Subscription in the sidebar.");
      } else if (result === "no_audio") {
        const script =
          (await getNarrationScript(storyId, "ta")) ??
          resolveStoryPlainText(storyId, storySourceUi) ??
          "";
        if (!beginBrowserTtsPlayback(storyId, storySourceUi, script, titleOverride)) {
          setError("Audio not available for this story");
        }
      }
    } catch (e) {
      const isInterrupted =
        (e instanceof DOMException && e.name === "AbortError") ||
        (e instanceof Error && /interrupted|pause/i.test(e.message));
      if (!isInterrupted) setError(e instanceof Error ? e.message : "Failed to load audio");
    } finally {
      if (playIntentRef.current === storyId) {
        playIntentRef.current = null;
        setLoadingStreamId(null);
      }
    }
  };

  const playStory = async (
    storyId: number,
    storySource: string,
    startPositionSeconds?: number,
    titleOverride?: string
  ) => {
    if (playingStoryId === storyId) {
      audioRef.current?.pause();
      videoRef.current?.pause();
      if (typeof window !== "undefined" && window.speechSynthesis) {
        window.speechSynthesis.cancel();
      }
      clearPlaybackAfterEnd();
      return;
    }
    audioRef.current?.pause();
    videoRef.current?.pause();
    if (typeof window !== "undefined" && window.speechSynthesis) {
      window.speechSynthesis.cancel();
    }
    setPlaybackUsesBrowserTts(false);
    browserTtsTextRef.current = "";
    setPlaybackWordTimings(null);
    setPlaybackKaraokeSegments([]);
    setPlaybackChapterFractions([]);
    setPlayingAvatarVideoUrl(null);
    setPlayingHostClipUrl(null);
    setInteractivePlayback(null);
    setInteractiveChoiceOpen(false);
    setInteractiveMissionOpen(false);
    familyEconomyRef.current = null;
    setFamilyEconomy(null);
    setTimePassageModal(null);
    streamRequestStartMsRef.current = null;
    streamAnalyticsLatencySentRef.current = false;
    streamAnalyticsBufferSentRef.current = false;
    playIntentRef.current = storyId;
    setLoadingStreamId(storyId);
    setError("");
    let skipFinallyLoadingClear = false;
    try {
      const prefSource = getPrefSourceForStory(storyId, storySource);
      playbackContextRef.current = { storyId, storySourceUi: storySource, titleOverride };

      const rawVoices = await getAvailableVoices(storyId, "ta");
      let voices = dedupeVoicesForPicker(rawVoices);
      if (voices.length === 0) {
        voices = [
          { voiceProfile: "default", isPremium: false },
          { voiceProfile: "calm", isPremium: true },
        ];
      }
      const pref = await getVoicePreference(storyId, prefSource);
      let selectedV =
        pref.voiceProfile &&
        pref.voiceProfile.trim() !== "" &&
        pref.voiceProfile.toLowerCase() !== "default" &&
        voices.some((v) => v.voiceProfile.toLowerCase() === pref.voiceProfile.toLowerCase())
          ? pref.voiceProfile
          : "default";
      if (selectedV === "default") {
        const localPref = getPreferredVoiceProfile();
        if (
          localPref !== "default" &&
          voices.some((v) => v.voiceProfile.toLowerCase() === localPref)
        ) {
          selectedV = localPref;
        }
      }
      const mode = sanitizePlaybackMode(pref.playbackMode, selectedV);
      if (mode !== pref.playbackMode) {
        await setVoicePreference(storyId, prefSource, selectedV, mode);
      }
      setPlaybackVoices(voices);
      setPlaybackVoiceProfile(selectedV);
      setPlaybackMode(mode);

      const libRow = libraryRowForInteractivePlayback(
        storyId,
        storySource,
        library,
        favorites
      ) as LibraryStory | null;
      const graph =
        libRow?.interactiveGraph != null ? parseInteractiveStoryGraph(libRow.interactiveGraph) : null;

      if (graph && libRow) {
        skipFinallyLoadingClear = true;
        setInteractiveChoiceOpen(false);
        setInteractiveMissionOpen(false);
        setPlaybackUsesBrowserTts(false);
        setPlaybackKaraokeSegments([]);
        setPlaybackWordTimings(null);
        setPlaybackChapterFractions([]);
        setPlayingAvatarVideoUrl(null);
        setPlayingHostClipUrl(null);
        setInteractivePlayback({
          storyId,
          graph,
          currentSegmentId: graph.startSegmentId,
          overlayStyle: graph.overlayStyle ?? null,
        });
        if (graphUsesFinancialReality(graph)) {
          const init = { ...DEFAULT_FAMILY_ECONOMY };
          familyEconomyRef.current = init;
          setFamilyEconomy(init);
        } else {
          familyEconomyRef.current = null;
          setFamilyEconomy(null);
        }
        playingStoryRef.current = { id: storyId, source: storySource };
        setPlayingTitle(titleOverride ?? getTitleForStory(storyId, storySource));
        const subSrcI =
          storySource === "mine" ? "generated" : storySource === "favorites" ? "favorites" : storySource;
        const pfI = getPlaybackFields(storyId, storySource, titleOverride);
        setPlayingSubtitle(listenerPlaybackSubtitle(subSrcI, pfI.theme, pfI.category, pfI.title));
        setParentPanelStory(libRow);
        return;
      }

      try {
        const tlSrc = timelineApiStorySource(storySource, storyId, favorites);
        const tl = await getTimeline(storyId, "ta", tlSrc, selectedV === "default" ? null : selectedV);
        if (playIntentRef.current === storyId) {
          setPlaybackKaraokeSegments(flattenPlaybackManifest(tl));
        }
      } catch {
        if (playIntentRef.current === storyId) setPlaybackKaraokeSegments([]);
      }

      const result = await loadAndPlayStream(
        storyId,
        storySource,
        selectedV,
        mode,
        startPositionSeconds,
        titleOverride
      );
      if (result === "upgrade") {
        setError("Premium voice or avatar needs an active plan. See Subscription in the sidebar.");
      } else if (result === "no_audio") {
        const script =
          (await getNarrationScript(storyId, "ta")) ??
          resolveStoryPlainText(storyId, storySource) ??
          "";
        if (!beginBrowserTtsPlayback(storyId, storySource, script, titleOverride)) {
          setError("Audio not available for this story");
        }
      }
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
        if (!skipFinallyLoadingClear) setLoadingStreamId(null);
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
    if (playbackUsesBrowserTts) {
      if (playingStoryId != null) {
        if (typeof window !== "undefined" && window.speechSynthesis) {
          window.speechSynthesis.cancel();
        }
        clearPlaybackAfterEnd();
        return;
      }
      const p = playingStoryRef.current;
      const text = browserTtsTextRef.current;
      if (p && text.trim()) {
        beginBrowserTtsPlayback(
          p.id,
          p.source,
          text,
          playbackContextRef.current?.titleOverride
        );
      }
      return;
    }
    if (playingStoryId != null) {
      const p = playingStoryRef.current;
      if (p && playingAvatarVideoUrl && videoRef.current) {
        savePlaybackPosition({
          storyId: p.id,
          storySource: p.source,
          positionSeconds: Math.floor(videoRef.current.currentTime),
        }).catch(() => {});
      }
      if (p && !playingAvatarVideoUrl && audioRef.current) {
        savePlaybackPosition({
          storyId: p.id,
          storySource: p.source,
          positionSeconds: Math.floor(audioRef.current.currentTime),
        }).catch(() => {});
      }
      audioRef.current?.pause();
      videoRef.current?.pause();
      clearPlaybackAfterEnd();
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
  }, [
    playbackUsesBrowserTts,
    playingStoryId,
    playingAvatarVideoUrl,
    getTitleForStory,
    getPlaybackFields,
    clearPlaybackAfterEnd,
    beginBrowserTtsPlayback,
  ]);

  const handleSeek = useCallback((seconds: number) => {
    if (playbackUsesBrowserTts) return;
    if (!Number.isFinite(seconds)) return;
    if (playingAvatarVideoUrl && videoRef.current) {
      videoRef.current.currentTime = seconds;
    } else if (audioRef.current) {
      audioRef.current.currentTime = seconds;
    }
    setAudioCurrentTime(seconds);
  }, [playingAvatarVideoUrl, playbackUsesBrowserTts]);

  useEffect(() => {
    if (playingStoryId == null || playbackUsesBrowserTts) return;
    const audio = audioRef.current;
    const video = videoRef.current;
    const el = playingAvatarVideoUrl && video ? video : audio;
    if (!el) return;
    const sid = playingStoryId;
    const onWaiting = () => {
      if (streamAnalyticsBufferSentRef.current) return;
      streamAnalyticsBufferSentRef.current = true;
      void reportStreamAnalytics({ storyId: sid, bufferingEvent: true });
    };
    el.addEventListener("waiting", onWaiting);
    return () => el.removeEventListener("waiting", onWaiting);
  }, [playingStoryId, playingAvatarVideoUrl, playbackUsesBrowserTts]);

  useEffect(() => {
    const audio = audioRef.current;
    const video = videoRef.current;
    const handleEnded = () => {
      const ipc = interactivePlaybackRef.current;
      if (ipc) {
        const seg = ipc.graph.segments[ipc.currentSegmentId];
        if (seg?.choices && seg.choices.length > 0) {
          setInteractiveChoiceOpen(true);
          return;
        }
        const ps = parentPanelStoryRef.current;
        const hasMission =
          ps != null &&
          ((ps.postStoryMission?.trim() ?? "") !== "" || (ps.postStoryResourceUrl?.trim() ?? "") !== "");
        if (hasMission) {
          setInteractiveMissionOpen(true);
          return;
        }
      }
      const psLinear = parentPanelStoryRef.current;
      const hasMissionLinear =
        psLinear != null &&
        ((psLinear.postStoryMission?.trim() ?? "") !== "" ||
          (psLinear.postStoryResourceUrl?.trim() ?? "") !== "");
      if (hasMissionLinear && !ipc) {
        setInteractiveMissionOpen(true);
        return;
      }
      const p = playingStoryRef.current;
      if (p && interactivePlaybackRef.current == null) {
        void reportStreamAnalytics({ storyId: p.id, completed: true });
      }
      clearPlaybackAfterEnd();
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
  }, [playingStoryId, playingAvatarVideoUrl, clearPlaybackAfterEnd]);

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

  const karaokeCaption = useMemo(() => {
    if (playbackUsesBrowserTts || playingStoryId == null) return null;
    const rawT = audioCurrentTime + READ_ALONG_TIMING_OFFSET_SEC;
    const t =
      audioDuration > 0
        ? Math.min(Math.max(0, rawT), audioDuration)
        : Math.max(0, rawT);
    if (playbackWordTimings && playbackWordTimings.length > 0) {
      return captionFromWordTimings(playbackWordTimings, t);
    }
    if (playbackKaraokeSegments.length > 0) {
      return captionFromSegments(playbackKaraokeSegments, t);
    }
    return null;
  }, [
    playbackUsesBrowserTts,
    playingStoryId,
    audioCurrentTime,
    audioDuration,
    playbackWordTimings,
    playbackKaraokeSegments,
  ]);

  const playbackCtx = playbackContextRef.current;
  const activePlaybackStoryId = playingStoryId ?? loadingStreamId;
  const showPlaybackExtras =
    interactivePlayback == null &&
    activePlaybackStoryId != null &&
    playbackCtx != null &&
    playbackCtx.storyId === activePlaybackStoryId;
  const voiceIsMy =
    playbackVoiceProfile.toLowerCase().startsWith("cloned:") ||
    playbackVoiceProfile.toLowerCase() === "family";
  const hasFamilyVoiceOption = playbackVoices.some((v) => v.voiceProfile.toLowerCase() === "family");

  const playbackExtrasEl = showPlaybackExtras ? (
    <div className="stories-playback-extras">
      <div className="stories-playback-extras__row">
        <label className="stories-playback-extras__label" htmlFor="playback-voice-select">
          Voice for this story
        </label>
        <select
          id="playback-voice-select"
          className="stories-playback-extras__select"
          value={playbackVoiceProfile}
          disabled={loadingStreamId != null}
          onChange={async (e) => {
            const next = e.target.value;
            const c = playbackContextRef.current;
            if (!c) return;
            const nextMode = sanitizePlaybackMode(playbackMode, next);
            setPlaybackVoiceProfile(next);
            if (nextMode !== playbackMode) setPlaybackMode(nextMode);
            const pref = getPrefSourceForStory(c.storyId, c.storySourceUi);
            await setVoicePreference(c.storyId, pref, next, nextMode);
            await reloadStreamForPreferenceChange(c.storyId, c.storySourceUi, next, nextMode, c.titleOverride);
          }}
        >
          {playbackVoices.map((v) => (
            <option key={v.voiceProfile} value={v.voiceProfile}>
              {voiceLabelForOption(v)}
              {v.isPremium ? " · Plan" : ""}
            </option>
          ))}
        </select>
      </div>
      <div className="stories-playback-extras__row">
        <span className="stories-playback-extras__label">Play mode</span>
        <select
          className="stories-playback-extras__select"
          aria-label="Play mode"
          value={!voiceIsMy && playbackMode !== "default" ? "default" : playbackMode}
          disabled={loadingStreamId != null}
          onChange={async (e) => {
            const m = e.target.value as PlaybackMode;
            const c = playbackContextRef.current;
            if (!c) return;
            const eff = sanitizePlaybackMode(m, playbackVoiceProfile);
            setPlaybackMode(eff);
            const pref = getPrefSourceForStory(c.storyId, c.storySourceUi);
            await setVoicePreference(c.storyId, pref, playbackVoiceProfile, eff);
            await reloadStreamForPreferenceChange(c.storyId, c.storySourceUi, playbackVoiceProfile, eff, c.titleOverride);
          }}
        >
          <option value="default">Tamixa voice (default)</option>
          <option value="my_voice" disabled={!voiceIsMy}>
            My voice — audio only
          </option>
          <option value="avatar" disabled={!voiceIsMy}>
            My voice + avatar video
          </option>
        </select>
      </div>
      {!voiceIsMy ? (
        <p className="stories-playback-extras__hint muted" style={{ margin: 0, fontSize: "0.75rem" }}>
          Choose a cloned profile or upload a family recording below to enable &quot;My voice&quot; and avatar video.{" "}
          <Link to="/voice">Voice</Link> · <Link to="/avatar">Avatar</Link>
        </p>
      ) : null}
      {playbackAvatarStatus === "VIDEO_GENERATING" ? (
        <p className="muted" style={{ margin: 0, fontSize: "0.75rem" }}>
          Avatar video is generating — try again in a moment.
        </p>
      ) : null}
      {playbackAvatarStatus === "VIDEO_FAILED" ? (
        <p className="muted" style={{ margin: 0, fontSize: "0.75rem" }}>
          Avatar video failed. Try another play mode or check your avatar photo on the Avatar page.
        </p>
      ) : null}
      {!playbackUsesBrowserTts && karaokeCaption ? (
        <div className="stories-playback-karaoke" role="status" aria-live="polite">
          {karaokeCaption}
        </div>
      ) : null}
      {playbackUsesBrowserTts ? (
        <p className="muted" style={{ margin: 0, fontSize: "0.75rem" }}>
          Using your browser&apos;s Tamil read-aloud because stream audio is unavailable.
        </p>
      ) : null}
      <div className="stories-playback-extras__row stories-playback-extras__row--wrap">
        <input
          ref={familyVoiceFileRef}
          type="file"
          accept="audio/*,.mp3,.wav,.m4a"
          className="visually-hidden"
          aria-hidden
          onChange={async (e) => {
            const file = e.target.files?.[0];
            e.target.value = "";
            const c = playbackContextRef.current;
            if (!file || !c) return;
            setFamilyVoiceBusy(true);
            setError("");
            try {
              await uploadFamilyVoice(c.storyId, "ta", file);
              const raw = await getAvailableVoices(c.storyId, "ta");
              const voices = dedupeVoicesForPicker(raw);
              if (voices.length > 0) setPlaybackVoices(voices);
              const nextV = voices.some((v) => v.voiceProfile.toLowerCase() === "family") ? "family" : playbackVoiceProfile;
              const nextMode = sanitizePlaybackMode(playbackMode, nextV);
              setPlaybackVoiceProfile(nextV);
              setPlaybackMode(nextMode);
              const pref = getPrefSourceForStory(c.storyId, c.storySourceUi);
              await setVoicePreference(c.storyId, pref, nextV, nextMode);
              await reloadStreamForPreferenceChange(c.storyId, c.storySourceUi, nextV, nextMode, c.titleOverride);
            } catch (err) {
              setError(err instanceof Error ? err.message : "Family voice upload failed");
            } finally {
              setFamilyVoiceBusy(false);
            }
          }}
        />
        <button
          type="button"
          className="btn btn-secondary"
          style={{ fontSize: "0.8rem", padding: "0.35rem 0.65rem" }}
          disabled={familyVoiceBusy || loadingStreamId != null}
          onClick={() => familyVoiceFileRef.current?.click()}
        >
          {familyVoiceBusy ? "Uploading…" : "Upload family voice (this story)"}
        </button>
        {hasFamilyVoiceOption ? (
          <button
            type="button"
            className="btn"
            style={{ fontSize: "0.8rem", padding: "0.35rem 0.65rem" }}
            disabled={familyVoiceBusy || loadingStreamId != null}
            onClick={async () => {
              const c = playbackContextRef.current;
              if (!c) return;
              setFamilyVoiceBusy(true);
              setError("");
              try {
                await deleteFamilyVoice(c.storyId, "ta");
                const raw = await getAvailableVoices(c.storyId, "ta");
                const voices = dedupeVoicesForPicker(raw);
                if (voices.length > 0) setPlaybackVoices(voices);
                const nextV =
                  playbackVoiceProfile.toLowerCase() === "family" ? "default" : playbackVoiceProfile;
                const nextMode = sanitizePlaybackMode(playbackMode, nextV);
                setPlaybackVoiceProfile(nextV);
                setPlaybackMode(nextMode);
                const pref = getPrefSourceForStory(c.storyId, c.storySourceUi);
                await setVoicePreference(c.storyId, pref, nextV, nextMode);
                await reloadStreamForPreferenceChange(c.storyId, c.storySourceUi, nextV, nextMode, c.titleOverride);
              } catch (err) {
                setError(err instanceof Error ? err.message : "Could not remove family voice");
              } finally {
                setFamilyVoiceBusy(false);
              }
            }}
          >
            Remove family voice
          </button>
        ) : null}
      </div>
    </div>
  ) : null;

  return (
    <div className={`page prime-page stories-page${showPlayer ? " has-audio-player" : ""}`}>
      {showVoiceOnboarding ? (
        <div className="stories-onboarding-hint" role="region" aria-label="Voice and avatar setup">
          <p className="stories-onboarding-hint__text">
            Fun library tales and <strong>Learn · Safety</strong> stories — including interactive episodes with choices. Optional: add <strong>your voice</strong> or a <strong>talking avatar</strong> like the mobile app.
          </p>
          <div className="stories-onboarding-hint__actions">
            <Link to="/voice" className="btn btn-secondary" style={{ fontSize: "0.85rem" }}>
              Voice
            </Link>
            <Link to="/avatar" className="btn btn-secondary" style={{ fontSize: "0.85rem" }}>
              Avatar
            </Link>
            <button
              type="button"
              className="btn"
              style={{ fontSize: "0.85rem" }}
              onClick={() => {
                setOnboardingVoiceAvatarDismissed();
                setShowVoiceOnboarding(false);
              }}
            >
              Dismiss
            </button>
          </div>
        </div>
      ) : null}
      <StoryAudioPlayer
        audioRef={audioRef}
        videoRef={videoRef}
        avatarVideoUrl={playingAvatarVideoUrl}
        hostClipUrl={playingHostClipUrl}
        isBrowserTts={playbackUsesBrowserTts}
        chapterFractions={playbackChapterFractions}
        enableSceneReflectionCue={Boolean(
          parentPanelStory &&
            isLearnStory(parentPanelStory.theme ?? "", parentPanelStory.category ?? null)
        )}
        practiceStoryChip={interactivePlayback != null}
        isPlaying={playingStoryId != null}
        isLoading={loadingStreamId != null}
        title={playingTitle ?? (loadingStreamId != null ? "Loading…" : null)}
        subtitle={playingSubtitle}
        extras={playbackExtrasEl}
        currentTime={audioCurrentTime}
        duration={audioDuration}
        onPlayPause={handleAudioPlayPause}
        onTimeUpdate={setAudioCurrentTime}
        onDurationChange={setAudioDuration}
        onSeek={handleSeek}
      />
      {interactivePlayback ? (
        <div className="family-bridge-bar" role="region" aria-label="Family co-listening">
          <span className="family-bridge-bar__label">Listening lens</span>
          <div className="family-bridge-bar__toggle" role="group" aria-label="Story viewpoint">
            <button
              type="button"
              className={`family-bridge-bar__seg${familyPerspectiveRole === "Child" ? " family-bridge-bar__seg--on" : ""}`}
              onClick={() => {
                setPerspective("Child");
                setFamilyPerspectiveRole("Child");
              }}
            >
              Child
            </button>
            <button
              type="button"
              className={`family-bridge-bar__seg${familyPerspectiveRole === "Parent" ? " family-bridge-bar__seg--on" : ""}`}
              onClick={() => {
                setPerspective("Parent");
                setFamilyPerspectiveRole("Parent");
              }}
            >
              Parent
            </button>
          </div>
          <span className="family-bridge-bar__hint muted">
            Two-sided lines appear when the episode includes them in the graph.
          </span>
        </div>
      ) : null}
      {familyBridgeSegmentCaption ? (
        <div className="family-bridge-caption" role="note">
          <p className="family-bridge-caption__text">{familyBridgeSegmentCaption}</p>
        </div>
      ) : null}
      {interactivePlayback && familyEconomy ? (
        <FinancialRealityHud economy={familyEconomy} onSimulateThreeMonths={handleSimulateThreeMonths} />
      ) : null}
      {interactivePlayback && simulatorPlaybook === "digitalSafety" ? (
        <ScamDetectionHud
          signals={interactivePlayback.graph.segments[interactivePlayback.currentSegmentId]?.scamSignals}
        />
      ) : null}
      {interactiveChoiceOpen && interactivePlayback ? (
        <TamixaSimulatorPlayer
          open
          segmentKey={`${interactivePlayback.storyId}:${interactivePlayback.currentSegmentId}`}
          reflectionPoint={
            interactivePlayback.graph.segments[interactivePlayback.currentSegmentId]?.reflectionPoint ??
            false
          }
          choices={
            interactivePlayback.graph.segments[interactivePlayback.currentSegmentId]?.choices ?? []
          }
          overlayStyle={interactivePlayback.overlayStyle}
          branchPrefetchUrls={interactiveChoicePrefetchUrls}
          playbook={simulatorPlaybook}
          meters={simulatorMeters}
          onChoice={(ch) => void handleInteractiveChoice(ch)}
        />
      ) : null}
      {interactiveMissionOpen && parentPanelStory ? (
        <MissionCardOverlay
          missionText={parentPanelStory.postStoryMission}
          resourceUrl={parentPanelStory.postStoryResourceUrl}
          storyTitle={parentPanelStory.title}
          scamProofUnlocked={scamProofUnlocked}
          onDismiss={dismissInteractiveMission}
        />
      ) : null}
      <TimePassageSummaryModal
        open={timePassageModal != null}
        title={timePassageModal?.title ?? ""}
        lines={timePassageModal?.lines ?? []}
        onClose={() => setTimePassageModal(null)}
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
        <p className="muted stories-page-top__hint">
          <Link to={ROUTES.lifeReadiness}>Life readiness</Link> — gentle snapshot from interactive practice on this device.
        </p>
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
                  {interactiveBadgeForSearchHit(s.theme) ? (
                    <span className="stories-interactive-badge">{interactivePracticeBadgeLabel()}</span>
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
        </div>

        {tab === "library" ? (
          <div className="stories-library-toolbar">
            <div className="stories-library-toolbar__head">
              <h3 className="stories-library-toolbar__title">Story library</h3>
              {!libraryListError ? (
                <span className="stories-library-toolbar__count muted" aria-live="polite">
                  {loading
                    ? "Loading…"
                    : `${displayLibrary.length} ${displayLibrary.length === 1 ? "tale" : "tales"}`}
                </span>
              ) : null}
            </div>
            <div className="stories-library-hub-grid" role="group" aria-label="Library hub">
              <div className="stories-library-hub-row">
                <button
                  type="button"
                  className={
                    libraryHub === "browse" ? "stories-library-hub-segment stories-library-hub-segment--active" : "stories-library-hub-segment"
                  }
                  onClick={() => setLibraryHubInUrl("browse")}
                >
                  Browse
                </button>
                <button
                  type="button"
                  className={
                    libraryHub === "fun" ? "stories-library-hub-segment stories-library-hub-segment--active" : "stories-library-hub-segment"
                  }
                  onClick={() => setLibraryHubInUrl("fun")}
                >
                  Fun corner
                </button>
              </div>
              <div className="stories-library-hub-row">
                <button
                  type="button"
                  className={
                    libraryHub === "learn"
                      ? "stories-library-hub-segment stories-library-hub-segment--active"
                      : "stories-library-hub-segment"
                  }
                  onClick={() => setLibraryHubInUrl("learn")}
                >
                  Learn &amp; safety
                </button>
                <button
                  type="button"
                  className={
                    libraryHub === "simulator"
                      ? "stories-library-hub-segment stories-library-hub-segment--active"
                      : "stories-library-hub-segment"
                  }
                  onClick={() => setLibraryHubInUrl("simulator")}
                >
                  Practice
                </button>
              </div>
            </div>
            {import.meta.env.DEV ? (
              <div className="muted" style={{ marginTop: 8, fontSize: "0.85rem" }}>
                <button
                  type="button"
                  className="stories-chip stories-chip--compact"
                  disabled={dsgDevPrepareBusy}
                  onClick={async () => {
                    setDsgDevPrepareBusy(true);
                    setDsgDevPrepareHint(null);
                    try {
                      const r = await prepareDigitalSurvivalDevE2eSeed();
                      setDsgDevPrepareHint(r.ok ? r.message : `Failed: ${r.message}`);
                      if (r.ok) setLibraryRetryKey((k) => k + 1);
                    } finally {
                      setDsgDevPrepareBusy(false);
                    }
                  }}
                >
                  {dsgDevPrepareBusy ? "Preparing DSG seed…" : "Dev: prepare Digital Survival E2E seed"}
                </button>
                {dsgDevPrepareHint ? (
                  <span style={{ marginLeft: 8 }} role="status">
                    {dsgDevPrepareHint}
                  </span>
                ) : null}
              </div>
            ) : null}
            {libraryHub === "browse" && libraryCategories.length > 0 ? (
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
            ) : libraryHub !== "browse" ? (
              <p className="muted stories-library-hub-theme-hint" style={{ margin: "0 0 4px", fontSize: "0.85rem" }}>
                Theme filters are available on <strong>Browse</strong>. Share this lane:{" "}
                <code className="stories-hub-deeplink">
                  ?tab=library&amp;hub=
                  {libraryHub === "fun" ? "fun" : libraryHub === "learn" ? "learn" : "simulator"}
                </code>
              </p>
            ) : null}
            <div className="stories-library-ethos" role="note">
              <p className="stories-library-ethos__title">Every listen is a little lesson</p>
              <p className="stories-library-ethos__body muted">
                {libraryHub === "browse" ? (
                  <>
                    Tamixa tales are written for growing minds—language, heart, and curiosity in every plot. On{" "}
                    <strong>Browse</strong>, use theme chips for curated lists. For laughs only, open <strong>Fun corner</strong>;
                    for Edu and digital safety, <strong>Learn &amp; safety</strong>; for choice-based practice,{" "}
                    <strong>Practice</strong> (Learn · Simulator or interactive graph).
                  </>
                ) : libraryHub === "fun" ? (
                  <>Lighthearted listens tagged <strong>Fun stories</strong> or <strong>Funny Stories</strong> in admin.</>
                ) : libraryHub === "learn" ? (
                  <>
                    Stories whose theme or category starts with <strong>Learn</strong>, or mention <strong>Digital Safety</strong>.
                  </>
                ) : (
                  <>
                    <strong>Learn · Simulator</strong> series and other library tales with an interactive graph (choices during
                    playback).
                  </>
                )}
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
          ) : displayLibrary.length === 0 ? (
            library.length === 0 ? (
              <EmptyState
                emoji="📚"
                title="No stories in library"
                description="Story library is being updated. Check back soon or try generating your own."
                action={{ label: "Generate Story", onClick: () => setTabAndUrl("mine") }}
              />
            ) : (
              <EmptyState
                emoji="🔎"
                title="No tales in this lane"
                description={
                  libraryHub === "fun"
                    ? "No Fun stories or Funny Stories tags in the current catalog slice. Try Browse or ask editors to tag lighter tales."
                    : libraryHub === "learn"
                      ? "No Learn-prefixed or Digital Safety rows in this slice. Try Browse or widen the catalog."
                      : "No Learn · Simulator or interactive-graph stories in this slice yet. Try Learn & safety or Browse."
                }
                action={{ label: "Browse all", onClick: () => setLibraryHubInUrl("browse") }}
              />
            )
          ) : (
            <div className="stories-grid stories-library-grid">
              {displayLibrary.map((s) => (
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
                  {isInteractivePracticeLibraryStory(s) ? (
                    <span className="stories-interactive-badge">{interactivePracticeBadgeLabel()}</span>
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
                    {s &&
                    isInteractivePracticeLibraryStory({
                      theme: s.theme,
                      category: "category" in s ? (s as LibraryStory).category : null,
                      interactiveGraph: "interactiveGraph" in s ? (s as LibraryStory).interactiveGraph : undefined,
                    }) ? (
                      <span className="stories-interactive-badge">{interactivePracticeBadgeLabel()}</span>
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
