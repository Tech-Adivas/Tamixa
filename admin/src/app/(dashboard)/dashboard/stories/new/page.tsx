"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import {
  api,
  getApiBaseUrl,
  reconcileAdminAuthCookie,
  refreshTokensIfNeeded,
} from "@/lib/api";
import type { CreateLibraryStoryRequest } from "@/types/api";
import {
  STORY_CATEGORIES,
  AGE_GROUPS,
  MIN_WORD_COUNT,
  EMOTION_MODES,
} from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { useActionResult } from "@/contexts/action-result-context";
import { usePipelineActive } from "@/contexts/pipeline-active-context";
import {
  ArrowLeft,
  Save,
  ImagePlus,
  Sparkles,
  RefreshCw,
  Braces,
  LayoutTemplate,
  FileText,
  Loader2,
} from "lucide-react";
import { InteractiveGraphSchemaHint } from "@/components/interactive-graph-schema-hint";
import { cn, interactiveGraphJsonRoughlyEqual, parseJsonStoryContent, resolveLibraryStoryEditorBody } from "@/lib/utils";
import {
  REGENERATE_THEN_TRANSLATIONS_HELP,
  getLibraryStoryMasterScriptContentError,
  canSubmitLibraryStoryForReview,
  adminStoryEmotionModeLabel,
  isLibraryStoryPipelineActivelyRunning,
  buildLibraryStoryAdminProgressSteps,
} from "@/lib/library-story-workflow";
import { lintInteractiveGraphJson } from "@/lib/interactive-graph-lint";
import { outlineInteractiveGraphJson } from "@/lib/interactive-graph-outline";
import {
  DIGITAL_SAFETY_INTERACTIVE_GRAPH_TEMPLATE,
  DIGITAL_SAFETY_SIMULATOR_POST_MISSION,
  DIGITAL_SAFETY_SIMULATOR_THEME,
  getDefaultDecisionJournalUrl,
} from "@/lib/edu-simulator-template";
import { SIMULATOR_THEME_PREFIX, validateInteractiveStoryCategory } from "@/lib/story-interactive-conventions";
import { validateInteractiveSegmentUrls } from "@/lib/interactive-segment-audio-url";
import {
  ADMIN_LIBRARY_PIPELINE_POLL_FIRST_MS,
  ADMIN_LIBRARY_PIPELINE_POLL_INTERVAL_MS,
  ADMIN_LIBRARY_PIPELINE_POLL_MAX_ROUNDS,
  ADMIN_LIBRARY_SEGMENT_ERROR_DETAIL_MAX_CHARS,
  ADMIN_LIBRARY_TITLE_MAX_LENGTH,
  ADMIN_LIBRARY_POST_MISSION_MAX_CHARS,
  ADMIN_LIBRARY_POST_RESOURCE_URL_MAX_CHARS,
  ADMIN_NEW_LIBRARY_STORY_SESSION_STORAGE_KEY,
  ADMIN_STORY_DRAFT_AUTOSAVE_STORAGE_KEY,
  AUTOSAVE_DEBOUNCE_MS,
  DEFAULT_LIBRARY_CHILD_NAME,
  DEFAULT_LIBRARY_EMOTION_MODE,
  DEFAULT_LIBRARY_SOURCE_LANGUAGE,
  INTERACTIVE_GRAPH_MASTER_LOCALE_KEY,
  LIBRARY_ENGLISH_LANGUAGE_CODE,
  LIBRARY_SOURCE_LANGUAGE_OPTIONS,
  LIBRARY_TAB_LANGUAGES,
  SEGMENT_STUDIO_PARENT_STORAGE_KEY,
  defaultTranslationTabLanguage,
  newEmptyLibraryStoryForm,
  emptyLibraryTranslationTab,
  buildLibraryTranslationContentPayload,
  type LibraryTranslationTabFields,
} from "@/lib/library-story-admin-constants";
import {
  parseInteractiveSegments,
  segmentStudioActivityMessage,
  type SegmentStudioActivity,
} from "@/lib/library-story-segment-studio";

const AUTOSAVE_KEY = ADMIN_STORY_DRAFT_AUTOSAVE_STORAGE_KEY;

function formatDraftAutosavedLabel(savedAt: number): string {
  const sec = Math.floor((Date.now() - savedAt) / 1000);
  if (sec < 30) return "just now";
  if (sec < 90) return "about a minute ago";
  if (sec < 3600) return `${Math.floor(sec / 60)} min ago`;
  return `${Math.floor(sec / 3600)} hr ago`;
}

function isSimulatorTheme(theme: string | null | undefined): boolean {
  return (theme?.trim() ?? "").startsWith(SIMULATOR_THEME_PREFIX);
}

/** Survives refresh so Regenerate / Cover keep using UPDATE instead of CREATE (avoids duplicate title). */
const ADMIN_NEW_STORY_SESSION_ID_KEY = ADMIN_NEW_LIBRARY_STORY_SESSION_STORAGE_KEY;

function stringifyInteractiveGraphOverlay(raw: unknown): string {
  if (raw == null) return "";
  if (typeof raw === "string") return raw;
  try {
    return JSON.stringify(raw, null, 2);
  } catch {
    return "";
  }
}

async function loadTranslationTabEntries(
  storyId: number,
  masterLang: string,
  masterGraphStr: string
): Promise<{
  tabLangs: { code: string; label: string }[];
  entries: Record<string, LibraryTranslationTabFields>;
  graphOverlays: Record<string, string>;
}> {
  const master = masterLang.toLowerCase();
  const masterNorm = masterGraphStr.trim();
  const tabLangs = LIBRARY_TAB_LANGUAGES.filter((l) => l.code !== master);
  const langResults = await Promise.all(
    tabLangs.map(({ code }) =>
      api.admin
        .getLibraryStory(storyId, code)
        .then((s) => {
          const raw = resolveLibraryStoryEditorBody(s ?? {});
          const parsed = parseJsonStoryContent(raw);
          const overlayRaw = stringifyInteractiveGraphOverlay(s?.translationInteractiveGraphOverlay).trim();
          const mergedRaw = stringifyInteractiveGraphOverlay(s?.interactiveGraph).trim();
          return {
            code,
            content: parsed?.content ?? raw,
            title: (parsed?.title ?? s?.title ?? "")?.trim() || "",
            moral: (parsed?.moral ?? s?.moral ?? "")?.trim() || "",
            postStoryMission: s?.postStoryMission?.trim() ?? "",
            postStoryResourceUrl: s?.postStoryResourceUrl?.trim() ?? "",
            overlayGraphStr: overlayRaw,
            mergedGraphStr: mergedRaw,
          };
        })
        .catch(() => ({
          code,
          content: "",
          title: "",
          moral: "",
          postStoryMission: "",
          postStoryResourceUrl: "",
          overlayGraphStr: "",
          mergedGraphStr: "",
        }))
    )
  );
  const entries: Record<string, LibraryTranslationTabFields> = {};
  const graphOverlays: Record<string, string> = {};
  langResults.forEach(
    ({ code, content, title, moral, postStoryMission, postStoryResourceUrl, overlayGraphStr, mergedGraphStr }) => {
      entries[code] = {
        content,
        title: title ?? "",
        moral: moral ?? "",
        postStoryMission: postStoryMission ?? "",
        postStoryResourceUrl: postStoryResourceUrl ?? "",
      };
      const overlay = overlayGraphStr.trim();
      const merged = mergedGraphStr.trim();
      const slot =
        overlay || (merged && !interactiveGraphJsonRoughlyEqual(merged, masterNorm) ? merged : undefined);
      if (slot) graphOverlays[code] = slot;
    }
  );
  return { tabLangs, entries, graphOverlays };
}

/** Matches backend `existsByTitle` (case-insensitive). */
async function findLibraryStoryIdByTitleIgnoreCase(title: string): Promise<number | null> {
  const target = title.trim().toLowerCase();
  if (!target) return null;
  const maxPages = 24;
  const pageSize = 50;
  for (let page = 0; page < maxPages; page++) {
    const res = await api.admin.getLibraryStories(page, pageSize);
    const hit = res.content?.find((s) => (s.title ?? "").trim().toLowerCase() === target);
    if (hit) return hit.id;
    if (res.last || !res.content?.length) break;
  }
  return null;
}

function resolveCoverSrc(coverImageUrl: string | null | undefined): string | null {
  if (!coverImageUrl?.trim()) return null;
  const u = coverImageUrl.trim();
  if (u.startsWith("http://") || u.startsWith("https://")) return u;
  const base = getApiBaseUrl();
  if (base) return u.startsWith("/") ? `${base}${u}` : `${base}/${u}`;
  return u.startsWith("/") ? u : null;
}

function CoverImageWithFallback({ src }: { src: string }) {
  const [errored, setErrored] = useState(false);
  if (errored) return <span className="text-3xl opacity-50">📖</span>;
  return (
    <Image
      src={src}
      alt="Story cover"
      fill
      unoptimized
      className="object-cover rounded-lg"
      onError={() => setErrored(true)}
    />
  );
}

/** Edit workflow: Submit step index after cover. */
const EDIT_STEP_SUBMIT = 3;

export default function NewLibraryStoryPage() {
  const router = useRouter();
  const { showSuccess, showError } = useActionResult();
  const { refresh: refreshPipelineActive, registerTriggered } = usePipelineActive();

  const [form, setForm] = useState<CreateLibraryStoryRequest>(() => newEmptyLibraryStoryForm());
  /** Set after first server create (via Regenerate, optional Save, Cover, or Submit). */
  const [savedStoryId, setSavedStoryId] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [coverCustomPrompt, setCoverCustomPrompt] = useState("");
  const [regenerateCustomPrompt, setRegenerateCustomPrompt] = useState("");
  const [coverGenerating, setCoverGenerating] = useState(false);
  const [coverRefreshKey, setCoverRefreshKey] = useState(0);
  const [coverVideoUrl, setCoverVideoUrl] = useState<string | null>(null);
  const [translationContentEntries, setTranslationContentEntries] = useState<
    Record<string, LibraryTranslationTabFields>
  >({});
  /** Active tab for “Other languages” (must not equal master `form.language`). */
  const [activeLangTab, setActiveLangTab] = useState<string>(() =>
    defaultTranslationTabLanguage(DEFAULT_LIBRARY_SOURCE_LANGUAGE)
  );
  const [translationsRefreshing, setTranslationsRefreshing] = useState(false);
  const [scriptSyncInProgress, setScriptSyncInProgress] = useState(false);
  const [regenerateServerRunning, setRegenerateServerRunning] = useState(false);
  const [storyPipelineRunning, setStoryPipelineRunning] = useState(false);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const [segmentScripts, setSegmentScripts] = useState<Record<string, string>>({});
  const [segmentVoiceProfile, setSegmentVoiceProfile] = useState("default");
  const [segmentVoiceParentId, setSegmentVoiceParentId] = useState("");
  const [segmentVoiceOptions, setSegmentVoiceOptions] = useState<Array<{ value: string; label: string }>>([
    { value: "default", label: "Default" },
  ]);
  const [segmentVoiceStatus, setSegmentVoiceStatus] = useState<string | null>(null);
  const [segmentStudioActivity, setSegmentStudioActivity] = useState<SegmentStudioActivity>({ kind: "idle" });
  const segmentStudioBusy = segmentStudioActivity.kind !== "idle";
  const segmentVoiceAutoLoadedRef = useRef(false);
  const autosaveRef = useRef<ReturnType<typeof setTimeout>>();
  /** Client-only: last time the unpublished form was written to `AUTOSAVE_KEY`. */
  const [localDraftSavedAt, setLocalDraftSavedAt] = useState<number | null>(null);
  /** Master key + per-locale full graph JSON (same model as Edit). */
  const [interactiveGraphSlots, setInteractiveGraphSlots] = useState<Record<string, string>>(() => ({
    [INTERACTIVE_GRAPH_MASTER_LOCALE_KEY]: "",
  }));
  const [interactiveGraphEditLocale, setInteractiveGraphEditLocale] = useState<string>(
    INTERACTIVE_GRAPH_MASTER_LOCALE_KEY
  );
  const regenerateInFlightRef = useRef(false);
  const regeneratePrevRunningRef = useRef(false);
  /** Prevents parallel CREATE requests that both hit duplicate-title. */
  const ensureDraftPromiseRef = useRef<Promise<number> | null>(null);

  const wordCount = form.content
    ? form.content.split(/\s+/).filter((w) => w.trim()).length
    : 0;
  const isValidWordCount = wordCount >= MIN_WORD_COUNT;

  const interactiveGraphLint = useMemo(() => {
    const ig = form.interactiveGraph?.trim();
    if (!ig) return { ok: true as const, errors: [] as string[] };
    return lintInteractiveGraphJson(ig);
  }, [form.interactiveGraph]);

  const interactiveCategoryHint = useMemo(() => {
    const ig = form.interactiveGraph?.trim();
    if (!ig || !interactiveGraphLint.ok) return null;
    return validateInteractiveStoryCategory(form.theme, ig);
  }, [form.theme, form.interactiveGraph, interactiveGraphLint.ok]);

  const interactiveGraphOutline = useMemo(() => {
    const ig = form.interactiveGraph?.trim();
    if (!ig || !interactiveGraphLint.ok) return null;
    return outlineInteractiveGraphJson(ig);
  }, [form.interactiveGraph, interactiveGraphLint.ok]);

  const simulatorSelected = useMemo(() => isSimulatorTheme(form.theme), [form.theme]);
  const interactiveGraphRequiredError = useMemo(() => {
    if (!simulatorSelected) return null;
    if (!form.interactiveGraph?.trim()) {
      return 'Simulator stories require "Interactive graph (JSON)".';
    }
    return null;
  }, [simulatorSelected, form.interactiveGraph]);
  const interactiveSegmentUrlErrors = useMemo(() => {
    const ig = form.interactiveGraph?.trim();
    if (!ig || !interactiveGraphLint.ok) return [];
    return validateInteractiveSegmentUrls(ig);
  }, [form.interactiveGraph, interactiveGraphLint.ok]);
  const interactiveSegments = useMemo(
    () => parseInteractiveSegments(form.interactiveGraph),
    [form.interactiveGraph]
  );
  const simulatorSegmentsMissingAudio = useMemo(
    () => interactiveSegments.filter((s) => !s.audioUrl?.trim()),
    [interactiveSegments]
  );
  const simulatorSubmitBlockedByAudio =
    simulatorSelected && interactiveSegments.length > 0 && simulatorSegmentsMissingAudio.length > 0;

  const segmentStudioLanguage = useMemo(
    () =>
      interactiveGraphEditLocale === INTERACTIVE_GRAPH_MASTER_LOCALE_KEY
        ? (form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE).toLowerCase()
        : interactiveGraphEditLocale.toLowerCase(),
    [form.language, interactiveGraphEditLocale]
  );

  const handleInteractiveGraphLocaleChange = useCallback(
    (next: string) => {
      const flushed: Record<string, string> = {
        ...interactiveGraphSlots,
        [interactiveGraphEditLocale]: form.interactiveGraph ?? "",
      };
      const nextBody =
        next === INTERACTIVE_GRAPH_MASTER_LOCALE_KEY
          ? flushed[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] ?? ""
          : flushed[next] ?? flushed[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] ?? "";
      setInteractiveGraphSlots(flushed);
      setForm((f) => ({ ...f, interactiveGraph: nextBody }));
      setInteractiveGraphEditLocale(next);
    },
    [form.interactiveGraph, interactiveGraphEditLocale, interactiveGraphSlots]
  );

  useEffect(() => {
    setSegmentScripts((prev) => {
      const next: Record<string, string> = {};
      for (const seg of interactiveSegments) {
        next[seg.id] = prev[seg.id]?.trim() ? prev[seg.id] : seg.text;
      }
      return next;
    });
  }, [interactiveSegments]);

  useEffect(() => {
    try {
      const saved = localStorage.getItem(SEGMENT_STUDIO_PARENT_STORAGE_KEY)?.trim() ?? "";
      if (saved) setSegmentVoiceParentId(saved);
    } catch {
      /* ignore */
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    void (async () => {
      const raw = sessionStorage.getItem(ADMIN_NEW_STORY_SESSION_ID_KEY);
      const sid = raw ? Number.parseInt(raw, 10) : Number.NaN;
      if (Number.isFinite(sid) && sid > 0) {
        try {
          const story = await api.admin.getLibraryStory(sid);
          if (cancelled) return;
          const contentToEdit = resolveLibraryStoryEditorBody(story);
          const parsed = parseJsonStoryContent(contentToEdit);
          const resolved = parsed ?? { content: contentToEdit };
          const sourceLang = (story.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE).toLowerCase();
          const masterIg = stringifyInteractiveGraphOverlay(story.interactiveGraph);
          setSavedStoryId(sid);
          setForm({
            title: (story.title ?? resolved.title ?? "")?.trim() || "",
            content: resolved.content ?? "",
            theme: (resolved.theme ?? story.theme) as string,
            language: sourceLang,
            age: story.age,
            childName: story.childName ?? DEFAULT_LIBRARY_CHILD_NAME,
            moral: (story.moral ?? resolved.moral ?? "")?.trim() || "",
            status: (story.status as CreateLibraryStoryRequest["status"]) ?? "DRAFT",
            coverImageUrl: story.coverImageUrl ?? "",
            emotionMode: story.emotionMode ?? DEFAULT_LIBRARY_EMOTION_MODE,
            parentDiscussionPrompts: story.parentDiscussionPrompts?.length
              ? [...story.parentDiscussionPrompts]
              : undefined,
            parentContentNote: story.parentContentNote ?? null,
            speakAlongPrompt: story.speakAlongPrompt ?? null,
            interactiveGraph: masterIg,
            postStoryMission: story.postStoryMission?.trim() ?? "",
            postStoryResourceUrl: story.postStoryResourceUrl?.trim() ?? "",
          });
          setCoverVideoUrl(story.coverVideoUrl ?? null);
          const { tabLangs, entries, graphOverlays } = await loadTranslationTabEntries(sid, sourceLang, masterIg);
          if (cancelled) return;
          setInteractiveGraphSlots({ [INTERACTIVE_GRAPH_MASTER_LOCALE_KEY]: masterIg, ...graphOverlays });
          setInteractiveGraphEditLocale(INTERACTIVE_GRAPH_MASTER_LOCALE_KEY);
          setTranslationContentEntries(entries);
          const withText = tabLangs.find((l) => entries[l.code]?.content?.trim());
          setActiveLangTab(
            withText?.code ?? tabLangs[0]?.code ?? defaultTranslationTabLanguage(sourceLang)
          );
          localStorage.removeItem(AUTOSAVE_KEY);
          return;
        } catch {
          sessionStorage.removeItem(ADMIN_NEW_STORY_SESSION_ID_KEY);
        }
      }
      if (cancelled) return;
      try {
        const saved = localStorage.getItem(AUTOSAVE_KEY);
        if (saved) {
          const parsed = JSON.parse(saved) as Partial<CreateLibraryStoryRequest>;
          setForm((f) => ({
            ...f,
            ...parsed,
            theme: parsed.theme?.trim() || STORY_CATEGORIES[0],
            emotionMode: parsed.emotionMode || DEFAULT_LIBRARY_EMOTION_MODE,
          }));
          if (typeof parsed.interactiveGraph === "string") {
            setInteractiveGraphSlots((p) => ({
              ...p,
              [INTERACTIVE_GRAPH_MASTER_LOCALE_KEY]: parsed.interactiveGraph as string,
            }));
            setInteractiveGraphEditLocale(INTERACTIVE_GRAPH_MASTER_LOCALE_KEY);
          }
        }
      } catch {
        /* ignore */
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (savedStoryId != null) return;
    if (!(form.content || form.title)) return;
    autosaveRef.current = setTimeout(() => {
      try {
        localStorage.setItem(AUTOSAVE_KEY, JSON.stringify(form));
        setLocalDraftSavedAt(Date.now());
      } catch {
        /* storage full / private mode */
      }
    }, AUTOSAVE_DEBOUNCE_MS);
    return () => {
      if (autosaveRef.current) clearTimeout(autosaveRef.current);
    };
  }, [form, savedStoryId]);

  useEffect(() => {
    if (savedStoryId != null) {
      sessionStorage.setItem(ADMIN_NEW_STORY_SESSION_ID_KEY, String(savedStoryId));
    }
  }, [savedStoryId]);

  useEffect(() => {
    if (savedStoryId == null) return;
    let cancelled = false;
    const tick = async () => {
      try {
        const s = await api.admin.getLibraryStoryPipelineStatus(savedStoryId);
        if (!cancelled) setStoryPipelineRunning(isLibraryStoryPipelineActivelyRunning(s ?? null));
      } catch {
        if (!cancelled) setStoryPipelineRunning(false);
      }
      try {
        const r = await api.admin.getRegenerateWithPromptStatus(savedStoryId);
        if (cancelled) return;
        const running = !!r.running;
        const wasRunning = regeneratePrevRunningRef.current;
        setRegenerateServerRunning(running);
        if (wasRunning && !running) {
          regeneratePrevRunningRef.current = false;
        } else {
          regeneratePrevRunningRef.current = running;
        }
      } catch {
        if (!cancelled) {
          setRegenerateServerRunning(false);
          regeneratePrevRunningRef.current = false;
        }
      }
    };
    void tick();
    const id = window.setInterval(tick, 5000);
    return () => {
      cancelled = true;
      clearInterval(id);
    };
  }, [savedStoryId]);

  useEffect(() => {
    const master = (form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE).toLowerCase();
    if (activeLangTab === master) {
      const next = defaultTranslationTabLanguage(master);
      setActiveLangTab(next);
    }
  }, [form.language, activeLangTab]);

  const refreshTranslationTabsFromServer = useCallback(
    async (storyId: number) => {
      const master = (form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE).toLowerCase();
      setTranslationsRefreshing(true);
      try {
        const masterSlot = (interactiveGraphSlots[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] ?? "").trim();
        const masterGraphForCompare =
          masterSlot || stringifyInteractiveGraphOverlay(form.interactiveGraph).trim();
        const { tabLangs, entries, graphOverlays } = await loadTranslationTabEntries(
          storyId,
          master,
          masterGraphForCompare
        );
        setTranslationContentEntries(entries);
        setInteractiveGraphSlots((prev) => {
          const merged = { ...prev };
          for (const [code, raw] of Object.entries(graphOverlays)) {
            merged[code] = raw;
          }
          return merged;
        });
        const withText = tabLangs.find((l) => entries[l.code]?.content?.trim());
        setActiveLangTab((prev) => {
          if (prev !== master && tabLangs.some((l) => l.code === prev)) return prev;
          return withText?.code ?? tabLangs[0]?.code ?? prev;
        });
      } catch {
        /* keep existing entries */
      } finally {
        setTranslationsRefreshing(false);
      }
    },
    [form.language, form.interactiveGraph, interactiveGraphSlots]
  );

  const validate = useCallback((): boolean => {
    const errs: Record<string, string> = {};
    if (!form.theme?.trim()) errs.theme = "Category is required";
    if (!form.content?.trim()) errs.content = "Story text is required";
    else if (wordCount < MIN_WORD_COUNT)
      errs.content = `Minimum ${MIN_WORD_COUNT} words required (current: ${wordCount})`;
    else {
      const scriptErr = getLibraryStoryMasterScriptContentError(form.content, form.language);
      if (scriptErr) errs.content = scriptErr;
    }
    if (form.title?.trim() && form.title.length > ADMIN_LIBRARY_TITLE_MAX_LENGTH)
      errs.title = `Title must be ${ADMIN_LIBRARY_TITLE_MAX_LENGTH} characters or less`;
    const ig = form.interactiveGraph?.trim();
    if (simulatorSelected && !ig) {
      errs.interactiveGraph = 'Simulator category requires "Interactive graph (JSON)".';
    }
    if (ig) {
      const igLint = lintInteractiveGraphJson(ig);
      if (!igLint.ok) errs.interactiveGraph = igLint.errors.join(" · ");
      else {
        const categoryErr = validateInteractiveStoryCategory(form.theme, ig);
        if (categoryErr) {
          if (errs.theme) errs.theme = `${errs.theme} — ${categoryErr}`;
          else errs.theme = categoryErr;
          errs.interactiveGraph = categoryErr;
        }
        const segUrlErrs = validateInteractiveSegmentUrls(ig);
        if (segUrlErrs.length > 0) {
          errs.interactiveGraph = segUrlErrs.join(" · ");
        }
      }
    }
    setValidationErrors(errs);
    return Object.keys(errs).length === 0;
  }, [form, wordCount, simulatorSelected]);

  const refreshStoryPipelineStatus = useCallback(async (storyId: number) => {
    const status = await api.admin.getLibraryStoryPipelineStatus(storyId);
    setStoryPipelineRunning(isLibraryStoryPipelineActivelyRunning(status ?? null));
  }, []);

  const refreshRegenerateStatus = useCallback(async (storyId: number) => {
    try {
      const status = await api.admin.getRegenerateWithPromptStatus(storyId);
      const running = !!status.running;
      regeneratePrevRunningRef.current = running;
      setRegenerateServerRunning(running);
    } catch {
      setRegenerateServerRunning(false);
      regeneratePrevRunningRef.current = false;
    }
  }, []);

  const pollUntilPipelineIdle = useCallback(
    async (storyId: number) => {
      const maxRounds = ADMIN_LIBRARY_PIPELINE_POLL_MAX_ROUNDS;
      let sawBusy = false;
      for (let round = 0; round < maxRounds; round++) {
        await new Promise((r) =>
          setTimeout(r, round === 0 ? ADMIN_LIBRARY_PIPELINE_POLL_FIRST_MS : ADMIN_LIBRARY_PIPELINE_POLL_INTERVAL_MS)
        );
        try {
          const status = await api.admin.getLibraryStoryPipelineStatus(storyId);
          const busy = isLibraryStoryPipelineActivelyRunning(status ?? null);
          setStoryPipelineRunning(busy);
          if (busy) sawBusy = true;
          if (status && Object.keys(status).length > 0 && !busy && (sawBusy || round >= 4)) {
            await refreshTranslationTabsFromServer(storyId);
            showSuccess(
              "Pipeline idle",
              "Other languages below were reloaded from the server. You can edit, generate cover, or submit."
            );
            return;
          }
        } catch {
          /* keep polling */
        }
      }
    },
    [showSuccess, refreshTranslationTabsFromServer]
  );

  const flushInteractiveGraphForApi = useCallback(
    (body: CreateLibraryStoryRequest) => {
      const flushed: Record<string, string> = {
        ...interactiveGraphSlots,
        [interactiveGraphEditLocale]: body.interactiveGraph ?? "",
      };
      const masterGraphStr = flushed[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] ?? "";
      const masterLang = (body.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE).toLowerCase();
      const translationInteractiveGraphEntries: Record<string, string> = {};
      for (const { code } of LIBRARY_TAB_LANGUAGES) {
        if (code === masterLang) continue;
        if (Object.prototype.hasOwnProperty.call(flushed, code)) {
          translationInteractiveGraphEntries[code] = flushed[code] ?? "";
        }
      }
      return {
        masterGraphStr,
        translationInteractiveGraphEntries:
          Object.keys(translationInteractiveGraphEntries).length > 0
            ? translationInteractiveGraphEntries
            : undefined,
      };
    },
    [interactiveGraphSlots, interactiveGraphEditLocale]
  );

  /** POST /admin/stories — same field normalization as first persist; includes translation rows so DB seeds match the form before any PUT. */
  const buildCreateLibraryStoryPayload = useCallback(
    (status: "DRAFT" | "PUBLISHED"): CreateLibraryStoryRequest => {
      const translationPayload = buildLibraryTranslationContentPayload(translationContentEntries);
      const { masterGraphStr, translationInteractiveGraphEntries } = flushInteractiveGraphForApi(form);
      return {
        ...form,
        title: form.title?.trim() || null,
        moral: form.moral?.trim() || null,
        status,
        coverImageUrl: form.coverImageUrl?.trim() || null,
        coverVideoUrl: coverVideoUrl ?? null,
        parentContentNote: form.parentContentNote?.trim() || null,
        speakAlongPrompt: form.speakAlongPrompt?.trim() || null,
        parentDiscussionPrompts: form.parentDiscussionPrompts?.length ? form.parentDiscussionPrompts : null,
        interactiveGraph: masterGraphStr.trim() ? masterGraphStr.trim() : null,
        translationInteractiveGraphEntries,
        postStoryMission: form.postStoryMission?.trim() || null,
        postStoryResourceUrl: form.postStoryResourceUrl?.trim() || null,
        translationContentEntries: translationPayload,
      };
    },
    [form, coverVideoUrl, translationContentEntries, flushInteractiveGraphForApi]
  );

  const buildPersistPayload = useCallback(
    (
      next: CreateLibraryStoryRequest,
      status: "DRAFT" | "PUBLISHED",
      translationEntries?: Record<string, LibraryTranslationTabFields>
    ) => {
      const entries = translationEntries ?? translationContentEntries;
      const translationPayload = buildLibraryTranslationContentPayload(entries);
      const { masterGraphStr, translationInteractiveGraphEntries } = flushInteractiveGraphForApi(next);
      return {
        ...next,
        title: next.title?.trim() || null,
        moral: next.moral?.trim() || null,
        status,
        coverImageUrl: next.coverImageUrl?.trim() || null,
        coverVideoUrl: coverVideoUrl ?? null,
        regenerateNarration: false,
        translationContentEntries: translationPayload,
        translationInteractiveGraphEntries,
        parentContentNote: next.parentContentNote?.trim() || null,
        speakAlongPrompt: next.speakAlongPrompt?.trim() || null,
        parentDiscussionPrompts: next.parentDiscussionPrompts?.length ? next.parentDiscussionPrompts : null,
        interactiveGraph: masterGraphStr.trim() ? masterGraphStr.trim() : null,
        postStoryMission: next.postStoryMission?.trim() || null,
        postStoryResourceUrl: next.postStoryResourceUrl?.trim() || null,
      };
    },
    [translationContentEntries, coverVideoUrl, flushInteractiveGraphForApi]
  );

  const applyDigitalSafetyTemplate = () => {
    if (form.interactiveGraph?.trim()) {
      if (
        !window.confirm(
          "Replace the interactive graph? Theme will be set to Digital Safety simulator; post-mission is filled only if it is empty."
        )
      ) {
        return;
      }
    }
    const journalUrl = getDefaultDecisionJournalUrl();
    const tpl = DIGITAL_SAFETY_INTERACTIVE_GRAPH_TEMPLATE;
    setForm({
      ...form,
      theme: DIGITAL_SAFETY_SIMULATOR_THEME,
      interactiveGraph: tpl,
      postStoryMission: form.postStoryMission?.trim()
        ? form.postStoryMission
        : DIGITAL_SAFETY_SIMULATOR_POST_MISSION,
      postStoryResourceUrl: form.postStoryResourceUrl?.trim() ? form.postStoryResourceUrl : journalUrl || "",
    });
    setInteractiveGraphSlots((p) => {
      const next = { ...p, [interactiveGraphEditLocale]: tpl };
      if (interactiveGraphEditLocale === INTERACTIVE_GRAPH_MASTER_LOCALE_KEY) {
        next[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] = tpl;
      }
      return next;
    });
  };

  const formatInteractiveGraphField = () => {
    const ig = form.interactiveGraph?.trim();
    if (!ig) {
      showError("Nothing to format", "Paste or load interactive graph JSON first.");
      return;
    }
    try {
      const obj = JSON.parse(ig) as unknown;
      const formatted = JSON.stringify(obj, null, 2);
      setForm({ ...form, interactiveGraph: formatted });
      setInteractiveGraphSlots((p) => {
        const next = { ...p, [interactiveGraphEditLocale]: formatted };
        if (interactiveGraphEditLocale === INTERACTIVE_GRAPH_MASTER_LOCALE_KEY) {
          next[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] = formatted;
        }
        return next;
      });
    } catch (e) {
      showError("Invalid JSON", e instanceof Error ? e.message : "Could not parse JSON.");
    }
  };

  const handleGenerateGraphFromStory = async () => {
    setSegmentStudioActivity({ kind: "graph" });
    try {
      const storyId = await ensureDraftOnServer();
      const result = await api.admin.generateInteractiveGraphFromStory(storyId, {
        language: segmentStudioLanguage,
        storyText: form.content ?? "",
      });
      const g = result.interactiveGraph ?? "";
      setForm((f) => ({ ...f, interactiveGraph: g || (f.interactiveGraph ?? "") }));
      setInteractiveGraphSlots((p) => {
        const next = { ...p, [interactiveGraphEditLocale]: g || (p[interactiveGraphEditLocale] ?? "") };
        if (interactiveGraphEditLocale === INTERACTIVE_GRAPH_MASTER_LOCALE_KEY) {
          next[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] = g || (p[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] ?? "");
        }
        return next;
      });
      showSuccess(
        "Interactive graph generated",
        "Segment narration scripts were filled where needed. Review branches, then use Segment Audio Studio for MP3s."
      );
    } catch (e) {
      showError("Graph generation failed", e instanceof Error ? e.message : "Unable to generate interactive graph.");
    } finally {
      setSegmentStudioActivity({ kind: "idle" });
    }
  };

  const handleGenerateSegmentAudio = async (segmentIds?: string[]) => {
    const selectedIds = (segmentIds ?? interactiveSegments.map((s) => s.id)).filter(Boolean);
    if (!selectedIds.length) {
      showError("No segments", "Add a valid interactive graph first.");
      return;
    }
    const audioScope: "all" | "one" | "some" =
      selectedIds.length === 1
        ? "one"
        : selectedIds.length === interactiveSegments.length
          ? "all"
          : "some";
    setSegmentStudioActivity({ kind: "audio", scope: audioScope, segmentIds: selectedIds });
    try {
      const storyId = await ensureDraftOnServer();
      const segments = selectedIds.map((id) => ({
        segmentId: id,
        text: (segmentScripts[id] ?? interactiveSegments.find((s) => s.id === id)?.text ?? "").trim(),
        overwriteExisting: true,
      }));
      const result = await api.admin.generateInteractiveSegmentAudio(storyId, {
        language: segmentStudioLanguage,
        voiceProfile: segmentVoiceProfile.trim() || "default",
        interactiveGraph: form.interactiveGraph?.trim() ?? "",
        segments,
      });
      const g = result.interactiveGraph ?? "";
      setForm((f) => ({ ...f, interactiveGraph: g || (f.interactiveGraph ?? "") }));
      setInteractiveGraphSlots((p) => {
        const next = { ...p, [interactiveGraphEditLocale]: g || (p[interactiveGraphEditLocale] ?? "") };
        if (interactiveGraphEditLocale === INTERACTIVE_GRAPH_MASTER_LOCALE_KEY) {
          next[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] = g || (p[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] ?? "");
        }
        return next;
      });
      const ok = result.generated?.length ?? 0;
      const failed = result.failed?.length ?? 0;
      if (failed > 0) {
        const failLines = (result.failed ?? [])
          .map((f) => {
            const msg = (f.message ?? "failed").replace(/\s+/g, " ").trim();
            return `${f.segmentId}: ${msg}`;
          })
          .join(" · ");
        const detail =
          failLines.length > ADMIN_LIBRARY_SEGMENT_ERROR_DETAIL_MAX_CHARS
            ? `${failLines.slice(0, ADMIN_LIBRARY_SEGMENT_ERROR_DETAIL_MAX_CHARS)}…`
            : failLines;
        showError(
          ok === 0 ? "Segment audio failed" : "Segment audio partially generated",
          `${ok} generated, ${failed} failed.${detail ? ` ${detail}` : ""}`,
        );
      } else {
        showSuccess("Segment audio generated", `${ok} segment audio URL(s) written into interactive graph.`);
      }
    } catch (e) {
      showError("Segment audio failed", e instanceof Error ? e.message : "Could not generate segment audio.");
    } finally {
      setSegmentStudioActivity({ kind: "idle" });
    }
  };

  const handleFillMissingSegmentScripts = async () => {
    const ig = form.interactiveGraph?.trim();
    if (!ig) {
      showError("No graph", "Add or paste interactive graph JSON first.");
      return;
    }
    if (!form.content?.trim()) {
      showError("Story text required", "Add story content so the model can write segment narration.");
      return;
    }
    setSegmentStudioActivity({ kind: "scripts" });
    try {
      const storyId = await ensureDraftOnServer();
      const result = await api.admin.fillInteractiveSegmentScripts(storyId, {
        language: segmentStudioLanguage,
        interactiveGraph: ig,
        storyText: form.content ?? "",
      });
      const g = result.interactiveGraph ?? "";
      setForm((f) => ({ ...f, interactiveGraph: g || (f.interactiveGraph ?? "") }));
      setInteractiveGraphSlots((p) => {
        const next = { ...p, [interactiveGraphEditLocale]: g || (p[interactiveGraphEditLocale] ?? "") };
        if (interactiveGraphEditLocale === INTERACTIVE_GRAPH_MASTER_LOCALE_KEY) {
          next[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] = g || (p[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] ?? "");
        }
        return next;
      });
      const filled = result.filledSegmentIds?.length ?? 0;
      showSuccess(
        filled === 0 ? "Segment scripts" : "Segment scripts generated",
        result.message ?? (filled === 0 ? "All segments already have text." : `${filled} segment(s) updated.`)
      );
    } catch (e) {
      showError("Segment scripts failed", e instanceof Error ? e.message : "Could not fill segment narration.");
    } finally {
      setSegmentStudioActivity({ kind: "idle" });
    }
  };

  const handleLoadVoiceProfiles = useCallback(async () => {
    const pid = Number.parseInt(segmentVoiceParentId.trim(), 10);
    if (!Number.isFinite(pid) || pid <= 0) {
      showError("Parent ID required", "Enter a valid parent ID to load cloned voices.");
      return;
    }
    setSegmentStudioActivity({ kind: "voices" });
    try {
      const profiles = await api.admin.getVoiceProfilesForParent(pid);
      const clonedOptions = profiles.map((p) => ({
        value: `cloned:${p.id}`,
        label: `${p.profileName?.trim() || `Voice ${p.id}`} (cloned:${p.id})`,
      }));
      const next = [{ value: "default", label: "Default" }, ...clonedOptions];
      setSegmentVoiceOptions(next);
      if (!next.some((o) => o.value === segmentVoiceProfile)) {
        setSegmentVoiceProfile(next[0]?.value ?? "default");
      }
      setSegmentVoiceStatus(
        clonedOptions.length > 0
          ? `${clonedOptions.length} cloned voice(s) loaded for parent ${pid}.`
          : `No cloned voices found for parent ${pid}.`
      );
      localStorage.setItem(SEGMENT_STUDIO_PARENT_STORAGE_KEY, String(pid));
      showSuccess("Voices loaded", `${clonedOptions.length} cloned voice profile(s) available.`);
    } catch (e) {
      setSegmentVoiceStatus("Failed to load voices. Check Parent ID and try again.");
      showError("Load voices failed", e instanceof Error ? e.message : "Could not fetch voice profiles.");
    } finally {
      setSegmentStudioActivity({ kind: "idle" });
    }
  }, [segmentVoiceParentId, segmentVoiceProfile, showError, showSuccess]);

  useEffect(() => {
    const pid = Number.parseInt(segmentVoiceParentId.trim(), 10);
    if (!simulatorSelected || segmentVoiceAutoLoadedRef.current) return;
    if (!Number.isFinite(pid) || pid <= 0) return;
    if (segmentVoiceOptions.length > 1) return;
    segmentVoiceAutoLoadedRef.current = true;
    void handleLoadVoiceProfiles();
  }, [segmentVoiceParentId, simulatorSelected, segmentVoiceOptions.length, handleLoadVoiceProfiles]);

  /**
   * Ensures a library row exists (CREATE once, then UPDATE). Survives refresh via sessionStorage.
   * If CREATE fails with duplicate title, adopts the existing story id (same as backend case-insensitive title check).
   */
  const ensureDraftOnServer = useCallback(async (): Promise<number> => {
    if (savedStoryId != null) return savedStoryId;
    if (ensureDraftPromiseRef.current) return ensureDraftPromiseRef.current;

    const run = (async (): Promise<number> => {
      try {
        const created = await api.admin.createLibraryStory(buildCreateLibraryStoryPayload("DRAFT"));
        if (!created?.id) throw new Error("Could not create the library story.");
        localStorage.removeItem(AUTOSAVE_KEY);
        setSavedStoryId(created.id);
        setForm((f) => ({ ...f, status: "DRAFT" }));
        await refreshTokensIfNeeded();
        reconcileAdminAuthCookie();
        void refreshStoryPipelineStatus(created.id);
        void refreshRegenerateStatus(created.id);
        return created.id;
      } catch (e) {
        const msg = e instanceof Error ? e.message : String(e);
        if (/already exists/i.test(msg) && form.title?.trim()) {
          const existingId = await findLibraryStoryIdByTitleIgnoreCase(form.title.trim());
          if (existingId != null) {
            showSuccess(
              "Using existing story",
              `This title matches story #${existingId}. Continuing with that row so Regenerate and cover use update, not create.`
            );
            localStorage.removeItem(AUTOSAVE_KEY);
            setSavedStoryId(existingId);
            setForm((f) => ({ ...f, status: "DRAFT" }));
            await refreshTokensIfNeeded();
            reconcileAdminAuthCookie();
            void refreshStoryPipelineStatus(existingId);
            void refreshRegenerateStatus(existingId);
            await refreshTranslationTabsFromServer(existingId).catch(() => {});
            return existingId;
          }
        }
        throw e;
      }
    })();

    ensureDraftPromiseRef.current = run;
    try {
      return await run;
    } finally {
      ensureDraftPromiseRef.current = null;
    }
  }, [
    savedStoryId,
    form,
    refreshStoryPipelineStatus,
    refreshRegenerateStatus,
    refreshTranslationTabsFromServer,
    showSuccess,
    buildCreateLibraryStoryPayload,
  ]);

  const handleSaveDraft = async () => {
    if (!validate()) {
      showError("Validation failed", "Please fix the validation errors before saving.");
      return;
    }
    setSubmitting(true);
    try {
      if (savedStoryId == null) {
        const id = await ensureDraftOnServer();
        showSuccess("Draft saved", `Story #${id} — use Regenerate & sync when you want Tamixa conversion and all languages.`);
      } else {
        await api.admin.updateLibraryStory(savedStoryId, buildPersistPayload(form, "DRAFT"));
        showSuccess("Draft updated", "Changes saved to the library.");
        void refreshStoryPipelineStatus(savedStoryId);
      }
      await refreshTokensIfNeeded();
      reconcileAdminAuthCookie();
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Unable to save the story.";
      showError("Save failed", msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleSubmitForReview = async () => {
    if (!validate()) {
      showError("Validation failed", "Please fix the validation errors before submitting.");
      return;
    }
    if (simulatorSubmitBlockedByAudio) {
      showError(
        "Segment audio missing",
        "Simulator stories require audioUrl for all segments before submitting. Use Segment Audio Studio to generate missing audio."
      );
      return;
    }
    setSubmitting(true);
    try {
      let id = savedStoryId;
      if (id != null) {
        try {
          const status = await api.admin.getLibraryStoryPipelineStatus(id);
          if (isLibraryStoryPipelineActivelyRunning(status ?? null)) {
            showError(
              "Pipeline still active",
              "Wait for Regenerate & sync / language pipeline to finish before submitting."
            );
            return;
          }
        } catch {
          /* continue */
        }
        await api.admin.updateLibraryStory(id, buildPersistPayload(form, "PUBLISHED"));
      } else {
        try {
          const created = await api.admin.createLibraryStory(buildCreateLibraryStoryPayload("PUBLISHED"));
          if (!created?.id) throw new Error("Could not create the story.");
          id = created.id;
          localStorage.removeItem(AUTOSAVE_KEY);
          setSavedStoryId(id);
        } catch (createErr) {
          const cmsg = createErr instanceof Error ? createErr.message : String(createErr);
          if (/already exists/i.test(cmsg) && form.title?.trim()) {
            const existingId = await findLibraryStoryIdByTitleIgnoreCase(form.title.trim());
            if (existingId != null) {
              id = existingId;
              localStorage.removeItem(AUTOSAVE_KEY);
              setSavedStoryId(existingId);
              await api.admin.updateLibraryStory(existingId, buildPersistPayload(form, "PUBLISHED"));
            } else {
              throw createErr;
            }
          } else {
            throw createErr;
          }
        }
      }
      showSuccess("Submitted for review", "Story sent to Story for review.");
      refreshPipelineActive();
      await refreshTokensIfNeeded();
      reconcileAdminAuthCookie();
      router.push(`/dashboard/stories/${id}/edit?step=${EDIT_STEP_SUBMIT}`);
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Unable to submit.";
      showError("Submit failed", msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleRegenerateAndSync = useCallback(async () => {
    if (regenerateInFlightRef.current) return;
    if (savedStoryId != null && regenerateServerRunning) {
      showError(
        "Regenerate in progress",
        "Regenerate is already running for this story. Please wait for it to finish."
      );
      return;
    }
    if (!validate()) {
      showError("Validation failed", "Please fix the validation errors before continuing.");
      return;
    }
    regenerateInFlightRef.current = true;
    setScriptSyncInProgress(true);
    let storyId: number | null = savedStoryId;
    try {
      if (storyId == null) {
        storyId = await ensureDraftOnServer();
      }
      setRegenerateServerRunning(true);
      const persistDraftWithRetry = async (
        id: number,
        payload: Parameters<typeof api.admin.updateLibraryStory>[1]
      ): Promise<void> => {
        try {
          await api.admin.updateLibraryStory(id, payload);
          return;
        } catch (e) {
          const msg = e instanceof Error ? e.message : String(e);
          const sessionLikely = /session expired|unauthorized|401|token/i.test(msg.toLowerCase());
          if (!sessionLikely) throw e;
          const refreshed = await refreshTokensIfNeeded();
          if (refreshed) reconcileAdminAuthCookie();
          await api.admin.updateLibraryStory(id, payload);
        }
      };

      const result = await api.admin.regenerateStoryWithPrompt(
        storyId,
        form.content,
        true,
        form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE,
        regenerateCustomPrompt.trim() || null
      );
      const parsed =
        result.content?.trim().startsWith("{") ? parseJsonStoryContent(result.content) : null;
      const effective = parsed ?? {
        content: result.content,
        title: result.title,
        moral: result.moral,
        category: result.category,
        theme: result.theme,
      };
      const mergedTheme = [effective.category, effective.theme, form.theme].find(
        (t) => typeof t === "string" && t.trim().length > 0
      );
      const nextForm: CreateLibraryStoryRequest = {
        ...form,
        content: effective.content ?? form.content,
        title: (effective.title || form.title) ?? "",
        moral: effective.moral ?? form.moral,
        theme: (mergedTheme ?? form.theme) as string,
        status: "DRAFT",
        ...(result.parentDiscussionPrompts !== undefined
          ? {
              parentDiscussionPrompts:
                result.parentDiscussionPrompts.length > 0 ? [...result.parentDiscussionPrompts] : undefined,
            }
          : {}),
        ...(result.parentContentNote !== undefined
          ? { parentContentNote: result.parentContentNote?.trim() || null }
          : {}),
        ...(result.speakAlongPrompt !== undefined
          ? { speakAlongPrompt: result.speakAlongPrompt?.trim() || null }
          : {}),
      };
      const nextEntries = { ...translationContentEntries };
      if (result.translations && typeof result.translations === "object") {
        for (const [lang, entry] of Object.entries(result.translations)) {
          if (entry && typeof entry === "object") {
            const prev = nextEntries[lang] ?? emptyLibraryTranslationTab();
            nextEntries[lang] = {
              content: (entry.content ?? "").trim(),
              title: (entry.title ?? "").trim(),
              moral: (entry.moral ?? "").trim(),
              postStoryMission: prev.postStoryMission,
              postStoryResourceUrl: prev.postStoryResourceUrl,
            };
          }
        }
      }
      await persistDraftWithRetry(storyId, buildPersistPayload(nextForm, "DRAFT", nextEntries));
      setForm({ ...nextForm, status: "DRAFT" });
      setTranslationContentEntries(nextEntries);

      const regeneratedTranslations = result.translations ?? {};
      const sourceLang = (form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE).toLowerCase();
      const expectedTargetLangs = LIBRARY_TAB_LANGUAGES.map((l) => l.code).filter((code) => code !== sourceLang);
      const hasAllRegeneratedTargets = expectedTargetLangs.every((lang) => {
        const entry = regeneratedTranslations[lang];
        return !!entry?.content?.trim();
      });

      if (hasAllRegeneratedTargets) {
        setStoryPipelineRunning(false);
        refreshPipelineActive();
        await refreshStoryPipelineStatus(storyId);
        await refreshTranslationTabsFromServer(storyId);
        showSuccess(
          "Language content updated",
          "Other languages appear below. Draft saved — you can Generate cover when ready."
        );
      } else {
        await api.admin.rebuildNarrationPipeline(storyId);
        setStoryPipelineRunning(true);
        refreshPipelineActive();
        registerTriggered(storyId);
        showSuccess(
          "TTS script saved · pipeline running",
          "Languages are rebuilding in the background. Other languages below refresh when the pipeline finishes — use Reload anytime."
        );
        await refreshTranslationTabsFromServer(storyId);
        void pollUntilPipelineIdle(storyId);
      }
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Save or pipeline failed.";
      if (/already running for this story|regenerate is already running/i.test(msg)) {
        setRegenerateServerRunning(true);
        showError("Regenerate in progress", "Regenerate is already running for this story. Please wait.");
      } else {
        showError("Regenerate & sync failed", msg);
      }
    } finally {
      setScriptSyncInProgress(false);
      regenerateInFlightRef.current = false;
      if (storyId != null) void refreshRegenerateStatus(storyId).catch(() => {});
    }
  }, [
    savedStoryId,
    form,
    translationContentEntries,
    buildPersistPayload,
    validate,
    showError,
    showSuccess,
    refreshPipelineActive,
    refreshStoryPipelineStatus,
    refreshRegenerateStatus,
    regenerateCustomPrompt,
    regenerateServerRunning,
    registerTriggered,
    pollUntilPipelineIdle,
    ensureDraftOnServer,
    refreshTranslationTabsFromServer,
  ]);

  const regenerateBusy = scriptSyncInProgress || storyPipelineRunning || regenerateServerRunning;

  const langLabel = (code: string) =>
    LIBRARY_SOURCE_LANGUAGE_OPTIONS.find((l) => l.code === code)?.label?.replace(/ \(recommended\)/, "") ?? code;

  const canSubmitForReview = canSubmitLibraryStoryForReview(form.status, !!form.content?.trim());
  const submitReady =
    canSubmitForReview &&
    !(savedStoryId != null && storyPipelineRunning) &&
    !simulatorSubmitBlockedByAudio;

  const creationProgressSteps = useMemo(
    () =>
      buildLibraryStoryAdminProgressSteps({
        minWordCount: MIN_WORD_COUNT,
        wordCount,
        titleTrimmed: !!form.title?.trim(),
        themeSet: !!form.theme,
        contentTrimmed: !!form.content?.trim(),
        simulatorSelected,
        simulatorGraphFieldsOk:
          !!form.interactiveGraph?.trim() && interactiveGraphLint.ok && interactiveSegmentUrlErrors.length === 0,
        onServer: savedStoryId != null,
        regenerateBusy,
        hasCover: !!(form.coverImageUrl?.trim() || coverVideoUrl?.trim()),
        submitReady,
      }),
    [
      wordCount,
      form.title,
      form.theme,
      form.content,
      form.interactiveGraph,
      form.coverImageUrl,
      simulatorSelected,
      interactiveGraphLint.ok,
      interactiveSegmentUrlErrors.length,
      savedStoryId,
      regenerateBusy,
      coverVideoUrl,
      submitReady,
    ]
  );

  const [, setDraftAutosaveTick] = useState(0);
  useEffect(() => {
    if (savedStoryId != null || localDraftSavedAt == null) return;
    const id = window.setInterval(() => setDraftAutosaveTick((t) => t + 1), 60_000);
    return () => window.clearInterval(id);
  }, [savedStoryId, localDraftSavedAt]);

  const startNewStory = useCallback(() => {
    sessionStorage.removeItem(ADMIN_NEW_STORY_SESSION_ID_KEY);
    localStorage.removeItem(AUTOSAVE_KEY);
    setSavedStoryId(null);
    setTranslationContentEntries({});
    setCoverVideoUrl(null);
    setCoverRefreshKey((k) => k + 1);
    setValidationErrors({});
    setRegenerateServerRunning(false);
    setStoryPipelineRunning(false);
    setForm(newEmptyLibraryStoryForm());
    setLocalDraftSavedAt(null);
    setInteractiveGraphSlots({ [INTERACTIVE_GRAPH_MASTER_LOCALE_KEY]: "" });
    setInteractiveGraphEditLocale(INTERACTIVE_GRAPH_MASTER_LOCALE_KEY);
    setActiveLangTab(defaultTranslationTabLanguage(DEFAULT_LIBRARY_SOURCE_LANGUAGE));
    setSegmentScripts({});
    setSegmentStudioActivity({ kind: "idle" });
    setSegmentVoiceStatus(null);
  }, []);

  return (
    <div className="space-y-6 max-w-5xl">
      <div className="rounded-2xl border-2 border-border bg-gradient-to-r from-primary/5 via-primary/[0.03] to-transparent p-6 space-y-4">
        <div className="flex flex-wrap items-center gap-4">
          <Link href="/dashboard/stories">
            <Button variant="outline" size="sm" className="rounded-xl">
              <ArrowLeft className="h-4 w-4 mr-1" /> Back
            </Button>
          </Link>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            className="rounded-xl text-muted-foreground"
            onClick={startNewStory}
          >
            Start new story
          </Button>
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-foreground">Create Story</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              <strong>Regenerate &amp; sync</strong> uses the story text in this form and starts Tamixa conversion + all
              languages (a library row is created automatically the first time). Then cover, then submit. Autosave · Min{" "}
              {MIN_WORD_COUNT} words · Master: {langLabel(form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE)}
              {(form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE).toLowerCase() !== LIBRARY_ENGLISH_LANGUAGE_CODE
                ? ` · ${langLabel(form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE)} script rules apply when master is not English`
                : ""}
            </p>
          </div>
        </div>
        <div
          className="flex flex-wrap gap-2 sm:gap-3 border border-border/60 rounded-xl bg-background/80 p-3"
          aria-label="Story creation progress"
        >
          {creationProgressSteps.map((step, i) => (
            <div key={step.id} className="flex items-center gap-2 text-xs sm:text-sm">
              <span
                className={cn(
                  "flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-[11px] font-bold tabular-nums",
                  step.done
                    ? "bg-emerald-600 text-white dark:bg-emerald-600"
                    : "busy" in step && step.busy
                      ? "bg-primary text-primary-foreground animate-pulse"
                      : "bg-muted text-muted-foreground"
                )}
                aria-hidden
              >
                {step.done ? "✓" : i + 1}
              </span>
              <span
                className={cn(
                  "font-medium",
                  step.done ? "text-foreground" : "text-muted-foreground"
                )}
              >
                {step.label}
                {"optional" in step && step.optional ? (
                  <span className="font-normal text-muted-foreground"> · optional</span>
                ) : null}
              </span>
            </div>
          ))}
        </div>
        <ol className="list-decimal space-y-1.5 pl-5 text-sm text-muted-foreground border border-border/60 rounded-xl bg-background/80 p-4">
          <li>
            <span className="text-foreground font-medium">Write</span> title, category, story text, metadata — and
            optionally <strong>interactive graph</strong> / post-episode mission (same fields as Edit) for Practice hub
            episodes.
          </li>
          <li>
            <span className="text-foreground font-medium">Regenerate &amp; sync all languages</span> — uses your form text;
            first run also creates the draft. Then use the <strong>Other languages</strong> card (and <strong>Reload</strong> if
            the pipeline is still running). {REGENERATE_THEN_TRANSLATIONS_HELP}
          </li>
          <li>
            <span className="text-foreground font-medium">Generate cover</span> (or paste a URL) after sync when possible.
          </li>
          <li>
            <span className="text-foreground font-medium">Submit for review</span> when the pipeline is idle (or use
            optional Save draft to persist without running sync).
          </li>
        </ol>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_300px]">
        <div className="space-y-6 min-h-[120px]">
          <Card className="border-2 border-border overflow-hidden">
            <CardHeader className="border-b border-border/50 bg-muted/20">
              <CardTitle className="text-base font-bold">Story content</CardTitle>
              <p className="text-sm text-muted-foreground mt-0.5">
                {savedStoryId != null ? (
                  <>
                    Story <strong>#{savedStoryId}</strong> on the server — <strong>Update draft</strong> saves edits without
                    running sync.
                  </>
                ) : (
                  <>
                    No library row yet — <strong>Regenerate &amp; sync</strong> (or Save draft / Cover / Submit) will create
                    one from this form.
                    {localDraftSavedAt != null ? (
                      <>
                        {" "}
                        <span className="text-foreground/90">
                          Local draft saved {formatDraftAutosavedLabel(localDraftSavedAt)} (this device).
                        </span>
                      </>
                    ) : null}
                  </>
                )}
              </p>
            </CardHeader>
            <CardContent className="space-y-5 pt-6">
              <div>
                <Label htmlFor="sourceLang">Language *</Label>
                <Select
                  value={form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE}
                  disabled={savedStoryId != null && regenerateBusy}
                  onValueChange={(v) => setForm((f) => ({ ...f, language: v }))}
                >
                  <SelectTrigger id="sourceLang" className="mt-1 rounded-xl">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {LIBRARY_SOURCE_LANGUAGE_OPTIONS.map((l) => (
                      <SelectItem key={l.code} value={l.code}>
                        {l.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {savedStoryId != null ? (
                  <p className="text-xs text-muted-foreground mt-1">
                    Changing master language on Create is best before Regenerate; for full control use{" "}
                    <Link href={`/dashboard/stories/${savedStoryId}/edit`} className="underline">
                      Edit
                    </Link>
                    .
                  </p>
                ) : null}
              </div>
              <div>
                <Label htmlFor="title">Title ({langLabel(form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE)}) *</Label>
                <Input
                  id="title"
                  value={form.title ?? ""}
                  onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                  placeholder="e.g. அறிவுள்ள காகம்"
                  className="mt-1 rounded-xl"
                  dir="ltr"
                  maxLength={ADMIN_LIBRARY_TITLE_MAX_LENGTH}
                />
                {validationErrors.title && (
                  <p className="text-sm text-destructive mt-1">{validationErrors.title}</p>
                )}
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <Label htmlFor="theme">Category *</Label>
                  <span
                    className={cn(
                      "inline-flex items-center rounded-full px-2 py-0.5 text-[11px] font-medium",
                      simulatorSelected
                        ? "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300"
                        : "bg-muted text-muted-foreground"
                    )}
                  >
                    {simulatorSelected ? "Interactive mode enabled" : "Linear mode"}
                  </span>
                </div>
                <Select value={form.theme} onValueChange={(v) => setForm((f) => ({ ...f, theme: v }))}>
                  <SelectTrigger className="mt-1 rounded-xl">
                    <SelectValue placeholder="Select category" />
                  </SelectTrigger>
                  <SelectContent>
                    {STORY_CATEGORIES.map((c) => (
                      <SelectItem key={c} value={c}>
                        {c}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {validationErrors.theme && (
                  <p className="text-sm text-destructive mt-1">{validationErrors.theme}</p>
                )}
                {interactiveCategoryHint && !validationErrors.theme ? (
                  <p className="text-sm text-amber-600 dark:text-amber-500 mt-1">{interactiveCategoryHint}</p>
                ) : null}
              </div>
              <div>
                <Label htmlFor="content">Story text ({langLabel(form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE)}) *</Label>
                <textarea
                  id="content"
                  value={form.content}
                  onChange={(e) => setForm((f) => ({ ...f, content: e.target.value }))}
                  placeholder={`Enter full story text in ${langLabel(form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE)}…`}
                  className="mt-1 flex min-h-[280px] w-full rounded-xl border-2 border-input bg-background px-4 py-3 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  dir="ltr"
                />
                <div className="mt-2 space-y-1">
                  <div
                    className="h-1.5 w-full overflow-hidden rounded-full bg-muted"
                    role="progressbar"
                    aria-valuemin={0}
                    aria-valuemax={MIN_WORD_COUNT}
                    aria-valuenow={Math.min(wordCount, MIN_WORD_COUNT)}
                    aria-label="Minimum word count progress"
                  >
                    <div
                      className={cn(
                        "h-full rounded-full transition-[width] duration-300",
                        isValidWordCount ? "bg-emerald-500 dark:bg-emerald-600" : "bg-primary/70"
                      )}
                      style={{ width: `${Math.min(100, (wordCount / Math.max(MIN_WORD_COUNT, 1)) * 100)}%` }}
                    />
                  </div>
                  <div className="flex justify-between">
                    <span
                      className={cn("text-xs", isValidWordCount ? "text-muted-foreground" : "text-destructive")}
                    >
                      {wordCount} / {MIN_WORD_COUNT} words minimum
                    </span>
                  </div>
                </div>
                {validationErrors.content && (
                  <p className="text-sm text-destructive">{validationErrors.content}</p>
                )}
              </div>
              <div>
                <Label htmlFor="moral">Moral (optional)</Label>
                <Input
                  id="moral"
                  value={form.moral ?? ""}
                  onChange={(e) => setForm((f) => ({ ...f, moral: e.target.value }))}
                  placeholder="e.g. Sharing brings joy"
                  className="mt-1 rounded-xl"
                />
              </div>
              <div className="rounded-xl border border-border/80 bg-muted/10 p-4 space-y-3">
                <p className="text-sm font-semibold">Parent resources (optional)</p>
                <p className="text-xs text-muted-foreground">
                  Shown in apps for library playback: context for caregivers and discussion ideas (up to 10 prompts, max
                  400 chars each).
                </p>
                <div>
                  <Label htmlFor="parentContentNote">Content note for parents</Label>
                  <textarea
                    id="parentContentNote"
                    value={form.parentContentNote ?? ""}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, parentContentNote: e.target.value || null }))
                    }
                    placeholder="Cultural context, content advisory, etc."
                    className="mt-1 flex min-h-[72px] w-full rounded-xl border-2 border-input bg-background px-3 py-2 text-sm"
                    maxLength={4000}
                  />
                </div>
                <div>
                  <Label htmlFor="speakAlongPrompt">Speak-along prompt</Label>
                  <Input
                    id="speakAlongPrompt"
                    value={form.speakAlongPrompt ?? ""}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, speakAlongPrompt: e.target.value || null }))
                    }
                    placeholder="Short line for kids to repeat"
                    className="mt-1 rounded-xl"
                    maxLength={500}
                  />
                </div>
                <div>
                  <Label htmlFor="discussionPrompts">Discussion prompts (one per line, max 10)</Label>
                  <textarea
                    id="discussionPrompts"
                    value={(form.parentDiscussionPrompts ?? []).join("\n")}
                    onChange={(e) => {
                      const lines = e.target.value
                        .split("\n")
                        .map((s) => s.trim())
                        .filter(Boolean)
                        .slice(0, 10)
                        .map((s) => s.slice(0, 400));
                      setForm((f) => ({
                        ...f,
                        parentDiscussionPrompts: lines.length ? lines : undefined,
                      }));
                    }}
                    placeholder={"What surprised you?\nHow would you help the character?"}
                    className="mt-1 flex min-h-[100px] w-full rounded-xl border-2 border-input bg-background px-3 py-2 text-sm"
                  />
                </div>
              </div>
              <div
                className={cn(
                  "rounded-xl border p-4 space-y-3 transition-opacity",
                  simulatorSelected
                    ? "border-border/80 bg-muted/10"
                    : "border-border/50 bg-muted/5 opacity-70"
                )}
              >
                <div className="flex flex-col gap-3">
                  <div className="w-full min-w-0 space-y-2">
                    <p className="text-sm font-semibold">Interactive episode (EduStory pilot)</p>
                    {simulatorSelected ? (
                      <InteractiveGraphSchemaHint />
                    ) : (
                      <p className="text-xs text-muted-foreground max-w-prose leading-relaxed">
                        Select a category that starts with <strong>Learn · Simulator</strong> to enable interactive graph
                        entry. For linear Fun/Learn stories, leave this section empty.
                      </p>
                    )}
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Button
                      type="button"
                      variant="secondary"
                      size="sm"
                      onClick={applyDigitalSafetyTemplate}
                      disabled={!simulatorSelected}
                    >
                      <LayoutTemplate className="h-4 w-4 mr-1.5" />
                      Digital Safety template
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={formatInteractiveGraphField}
                      disabled={!simulatorSelected}
                    >
                      <Braces className="h-4 w-4 mr-1.5" />
                      Format JSON
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={handleGenerateGraphFromStory}
                      disabled={!simulatorSelected || !form.content?.trim() || segmentStudioBusy}
                    >
                      {segmentStudioActivity.kind === "graph" ? (
                        <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />
                      ) : (
                        <Sparkles className="h-4 w-4 mr-1.5" />
                      )}
                      {segmentStudioActivity.kind === "graph" ? "Generating graph…" : "Generate graph from story"}
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => void handleFillMissingSegmentScripts()}
                      disabled={
                        !simulatorSelected ||
                        !form.interactiveGraph?.trim() ||
                        !form.content?.trim() ||
                        !interactiveGraphLint.ok ||
                        segmentStudioBusy
                      }
                      title="Uses the LLM to write spoken lines for any segment with empty text."
                    >
                      {segmentStudioActivity.kind === "scripts" ? (
                        <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />
                      ) : (
                        <FileText className="h-4 w-4 mr-1.5" />
                      )}
                      {segmentStudioActivity.kind === "scripts" ? "Generating scripts…" : "Generate missing segment scripts"}
                    </Button>
                    <Button type="button" variant="ghost" size="sm" asChild>
                      <Link href="/dashboard/edu-simulator-analytics">Choice analytics</Link>
                    </Button>
                  </div>
                </div>
                {segmentStudioActivityMessage(segmentStudioActivity) ? (
                  <div
                    role="status"
                    className="flex items-center gap-2 rounded-md border border-primary/25 bg-primary/5 px-3 py-2 text-xs text-foreground"
                  >
                    <Loader2 className="h-4 w-4 shrink-0 animate-spin text-primary" aria-hidden />
                    <span>{segmentStudioActivityMessage(segmentStudioActivity)}</span>
                  </div>
                ) : null}
                <div>
                  <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
                    <Label htmlFor="interactiveGraph">Interactive graph (JSON)</Label>
                    <div className="flex flex-col gap-1 sm:items-end">
                      <Label className="text-xs text-muted-foreground font-normal">Graph locale</Label>
                      <Select
                        value={interactiveGraphEditLocale}
                        onValueChange={(v) => handleInteractiveGraphLocaleChange(v)}
                        disabled={!simulatorSelected}
                      >
                        <SelectTrigger className="h-8 w-full sm:w-[220px] rounded-lg text-xs">
                          <SelectValue placeholder="Locale" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value={INTERACTIVE_GRAPH_MASTER_LOCALE_KEY}>
                            Master ({langLabel(form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE)})
                          </SelectItem>
                          {LIBRARY_TAB_LANGUAGES.filter((l) => l.code !== form.language).map(({ code, label }) => (
                            <SelectItem key={code} value={code}>
                              {label} ({code})
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <p className="text-[11px] text-muted-foreground max-w-[280px] sm:text-right">
                        Per-locale graphs are saved with the draft (same as Edit). TTS and script fill use this locale.
                        Choosing a tab under Other languages also switches graph locale and loads locale-specific JSON when
                        the API differs from master (e.g. English segment audio URLs).
                      </p>
                    </div>
                  </div>
                  <textarea
                    id="interactiveGraph"
                    value={form.interactiveGraph ?? ""}
                    onChange={(e) => {
                      const v = e.target.value || "";
                      setForm((f) => ({ ...f, interactiveGraph: v }));
                      setInteractiveGraphSlots((p) => {
                        const next = { ...p, [interactiveGraphEditLocale]: v };
                        if (interactiveGraphEditLocale === INTERACTIVE_GRAPH_MASTER_LOCALE_KEY) {
                          next[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] = v;
                        }
                        return next;
                      });
                    }}
                    className="mt-1 flex min-h-[240px] w-full rounded-xl border-2 border-input bg-background px-3 py-2 text-sm font-mono text-xs"
                    spellCheck={false}
                    disabled={!simulatorSelected}
                  />
                  {interactiveGraphRequiredError && !validationErrors.interactiveGraph ? (
                    <p className="text-sm text-destructive mt-2">{interactiveGraphRequiredError}</p>
                  ) : validationErrors.interactiveGraph ? (
                    <p className="text-sm text-destructive mt-2">{validationErrors.interactiveGraph}</p>
                  ) : !interactiveGraphLint.ok ? (
                    <ul className="text-sm text-destructive mt-2 list-disc pl-5 space-y-0.5">
                      {interactiveGraphLint.errors.map((err, i) => (
                        <li key={i}>{err}</li>
                      ))}
                    </ul>
                  ) : form.interactiveGraph?.trim() ? (
                    <p className="text-sm text-muted-foreground mt-2">Interactive graph JSON looks valid.</p>
                  ) : null}
                  {simulatorSelected && interactiveSegmentUrlErrors.length > 0 ? (
                    <ul className="text-sm text-destructive mt-2 list-disc pl-5 space-y-0.5">
                      {interactiveSegmentUrlErrors.map((err, i) => (
                        <li key={i}>{err}</li>
                      ))}
                    </ul>
                  ) : null}
                  {simulatorSelected ? (
                    <div className="mt-3 rounded-md border border-border/60 bg-background/80 p-3 text-xs space-y-1.5">
                      <p className="font-semibold text-foreground">Interactive checklist</p>
                      <p className={form.interactiveGraph?.trim() ? "text-emerald-600 dark:text-emerald-500" : "text-amber-600 dark:text-amber-500"}>
                        {form.interactiveGraph?.trim() ? "✓ Graph added" : "• Add interactive graph JSON"}
                      </p>
                      <p className={interactiveGraphLint.ok ? "text-emerald-600 dark:text-emerald-500" : "text-amber-600 dark:text-amber-500"}>
                        {interactiveGraphLint.ok ? "✓ Graph structure valid" : "• Fix graph JSON lint issues"}
                      </p>
                      <p className={interactiveSegmentUrlErrors.length === 0 ? "text-emerald-600 dark:text-emerald-500" : "text-amber-600 dark:text-amber-500"}>
                        {interactiveSegmentUrlErrors.length === 0
                          ? "✓ Segment URLs (HTTPS MP3, or dev http on LAN)"
                          : "• Fix segment audioUrl HTTPS/MP3 issues"}
                      </p>
                    </div>
                  ) : null}
                </div>
                {interactiveGraphOutline && interactiveGraphOutline.length > 0 ? (
                  <div className="rounded-md border border-border/60 bg-background/80 p-3 space-y-2">
                    <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">Flow preview</p>
                    <ol className="text-sm space-y-2 list-decimal pl-4">
                      {interactiveGraphOutline.map((seg) => (
                        <li key={seg.id}>
                          <span className="font-mono text-xs">{seg.id}</span>
                          {seg.isEnd ? (
                            <span className="text-muted-foreground"> — end</span>
                          ) : (
                            <ul className="mt-1 space-y-0.5 pl-0 list-none">
                              {seg.choices.map((c) => (
                                <li key={c.id} className="text-xs text-muted-foreground">
                                  → <span className="text-foreground">{c.label}</span>{" "}
                                  <span className="font-mono">({c.nextSegmentId})</span>
                                </li>
                              ))}
                            </ul>
                          )}
                        </li>
                      ))}
                    </ol>
                  </div>
                ) : null}
                {simulatorSelected ? (
                  <div className="rounded-md border border-border/60 bg-background/80 p-3 space-y-2">
                    <div className="flex items-center justify-between gap-3">
                      <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                        Segment Audio Studio
                      </p>
                      <Button
                        type="button"
                        size="sm"
                        variant="secondary"
                        onClick={() => void handleGenerateSegmentAudio()}
                        disabled={
                          segmentStudioBusy ||
                          interactiveSegments.length === 0 ||
                          !interactiveGraphLint.ok
                        }
                      >
                        {segmentStudioActivity.kind === "audio" &&
                        (segmentStudioActivity.scope === "all" || segmentStudioActivity.scope === "some") ? (
                          <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />
                        ) : (
                          <RefreshCw className="h-4 w-4 mr-1.5" />
                        )}
                        {segmentStudioActivity.kind === "audio" &&
                        (segmentStudioActivity.scope === "all" || segmentStudioActivity.scope === "some")
                          ? "Generating audio…"
                          : "Generate all segment audio"}
                      </Button>
                    </div>
                    <div>
                      <Label htmlFor="segmentVoiceProfile" className="text-xs">Voice profile</Label>
                      <Select
                        value={segmentVoiceProfile}
                        onValueChange={setSegmentVoiceProfile}
                      >
                        <SelectTrigger id="segmentVoiceProfile" className="mt-1 h-8 rounded-lg text-xs">
                          <SelectValue placeholder="Select voice profile" />
                        </SelectTrigger>
                        <SelectContent>
                          {segmentVoiceOptions.map((opt) => (
                            <SelectItem key={opt.value} value={opt.value}>
                              {opt.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <div className="mt-2 flex gap-2">
                        <Input
                          value={segmentVoiceParentId}
                          onChange={(e) => setSegmentVoiceParentId(e.target.value)}
                          className="h-8 rounded-lg text-xs"
                          placeholder="Parent ID for cloned voices"
                        />
                        <Button
                          type="button"
                          size="sm"
                          variant="outline"
                          onClick={handleLoadVoiceProfiles}
                          disabled={segmentStudioBusy}
                        >
                          {segmentStudioActivity.kind === "voices" ? (
                            <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />
                          ) : null}
                          {segmentStudioActivity.kind === "voices" ? "Loading…" : "Load voices"}
                        </Button>
                      </div>
                      <p className="mt-1 text-[11px] text-muted-foreground">
                        {segmentVoiceStatus ?? "Tip: load voices with Parent ID, then pick cloned:{id}. Keep Default as fallback."}
                      </p>
                    </div>
                    {interactiveSegments.length === 0 ? (
                      <p className="text-xs text-muted-foreground">
                        Add a valid graph to parse segments. Then generate audio URLs in one click.
                      </p>
                    ) : (
                      <div className="space-y-2">
                        {interactiveSegments.map((seg) => (
                          <div key={seg.id} className="rounded border border-border/60 p-2 space-y-2">
                            <div className="flex items-center justify-between gap-2">
                              <code className="text-xs">{seg.id}</code>
                              <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => void handleGenerateSegmentAudio([seg.id])}
                                disabled={segmentStudioBusy || !interactiveGraphLint.ok}
                              >
                                {segmentStudioActivity.kind === "audio" &&
                                segmentStudioActivity.segmentIds.includes(seg.id) ? (
                                  <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />
                                ) : null}
                                {segmentStudioActivity.kind === "audio" &&
                                segmentStudioActivity.segmentIds.includes(seg.id)
                                  ? "Generating…"
                                  : "Generate audio"}
                              </Button>
                            </div>
                            <textarea
                              value={segmentScripts[seg.id] ?? seg.text}
                              onChange={(e) =>
                                setSegmentScripts((prev) => ({ ...prev, [seg.id]: e.target.value }))
                              }
                              className="flex min-h-[68px] w-full rounded border border-input bg-background px-2 py-1.5 text-xs"
                              placeholder="Segment narration script"
                              spellCheck={false}
                            />
                            <p className="text-[11px] text-muted-foreground">
                              {seg.audioUrl ? `Current audio: ${seg.audioUrl}` : "Audio missing"}
                            </p>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                ) : null}
                <div>
                  <Label htmlFor="postStoryMission">Post-episode family mission (plain text)</Label>
                  <textarea
                    id="postStoryMission"
                    value={form.postStoryMission ?? ""}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, postStoryMission: e.target.value || "" }))
                    }
                    className="mt-1 flex min-h-[80px] w-full rounded-xl border-2 border-input bg-background px-3 py-2 text-sm"
                    maxLength={8000}
                    disabled={!simulatorSelected}
                  />
                </div>
                <div>
                  <Label htmlFor="postStoryResourceUrl">Optional resource link (e.g. decision journal)</Label>
                  <p className="text-xs text-muted-foreground mt-1">
                    Printable journal: parent app <code className="rounded bg-muted px-1">/decision-journal.html</code>.
                    Pre-fill via admin env{" "}
                    <code className="rounded bg-muted px-1">NEXT_PUBLIC_DECISION_JOURNAL_URL</code> (HTTPS in production).
                  </p>
                  <Input
                    id="postStoryResourceUrl"
                    value={form.postStoryResourceUrl ?? ""}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, postStoryResourceUrl: e.target.value || "" }))
                    }
                    className="mt-1 rounded-xl"
                    maxLength={512}
                    placeholder="https://..."
                    disabled={!simulatorSelected}
                  />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label htmlFor="emotionMode">Narration tone</Label>
                  <Select
                    value={form.emotionMode ?? "CALM"}
                    onValueChange={(v) => setForm((f) => ({ ...f, emotionMode: v }))}
                  >
                    <SelectTrigger id="emotionMode" className="mt-1 rounded-xl">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {EMOTION_MODES.map((m) => (
                        <SelectItem key={m} value={m}>
                          {adminStoryEmotionModeLabel(m)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <Label>Age group</Label>
                  <Select
                    value={String(form.age)}
                    onValueChange={(v) => setForm((f) => ({ ...f, age: parseInt(v, 10) }))}
                  >
                    <SelectTrigger className="mt-1 rounded-xl">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {AGE_GROUPS.map((a) => (
                        <SelectItem key={a.value} value={String(a.value)}>
                          {a.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <div>
                <Label htmlFor="childName">Child name placeholder</Label>
                <Input
                  id="childName"
                  value={form.childName ?? DEFAULT_LIBRARY_CHILD_NAME}
                  onChange={(e) => setForm((f) => ({ ...f, childName: e.target.value }))}
                  className="mt-1 rounded-xl"
                />
              </div>
            </CardContent>
          </Card>

          {savedStoryId != null && (
            <Card className="border-2 border-border overflow-hidden">
              <CardHeader className="border-b border-border/50 bg-muted/20">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <CardTitle className="text-base font-bold">Other languages</CardTitle>
                    <p className="text-sm text-muted-foreground mt-0.5">
                      Text loads from the server after <strong>Regenerate &amp; sync</strong> (or when the pipeline
                      finishes). You can <strong>Copy from master</strong> to seed a tab, then edit. Use{" "}
                      <strong>Update draft</strong> to save tab changes.
                    </p>
                  </div>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    className="shrink-0 rounded-lg"
                    disabled={translationsRefreshing || regenerateBusy}
                    onClick={() => void refreshTranslationTabsFromServer(savedStoryId)}
                  >
                    <RefreshCw className={cn("h-3.5 w-3.5 mr-1.5", translationsRefreshing && "animate-spin")} />
                    {translationsRefreshing ? "Loading…" : "Reload"}
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="space-y-4 pt-6">
                <div className="flex gap-1 flex-wrap">
                  {LIBRARY_TAB_LANGUAGES.filter((l) => l.code !== form.language).map(({ code, label }) => (
                    <button
                      key={code}
                      type="button"
                      onClick={() => {
                        setActiveLangTab(code);
                        handleInteractiveGraphLocaleChange(code);
                      }}
                      className={cn(
                        "rounded-lg px-3 py-1.5 text-sm font-medium transition-colors",
                        activeLangTab === code
                          ? "bg-primary text-primary-foreground"
                          : "bg-muted/50 text-muted-foreground hover:bg-muted"
                      )}
                    >
                      {label}
                    </button>
                  ))}
                </div>
                {LIBRARY_TAB_LANGUAGES.filter((l) => l.code !== form.language).map(({ code, label }) => {
                  if (activeLangTab !== code) return null;
                  const entry = translationContentEntries[code] ?? emptyLibraryTranslationTab();
                  const masterCode = form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE;
                  const masterLbl = langLabel(masterCode);
                  const tabEmpty =
                    !entry.content?.trim() &&
                    !entry.title?.trim() &&
                    !entry.moral?.trim() &&
                    !entry.postStoryMission?.trim() &&
                    !entry.postStoryResourceUrl?.trim();
                  return (
                    <div key={code} className="space-y-3">
                      <div className="flex flex-wrap items-center gap-2">
                        <Button
                          type="button"
                          variant="secondary"
                          size="sm"
                          className="rounded-lg"
                          disabled={!form.content?.trim() && !form.title?.trim()}
                          onClick={() => {
                            setTranslationContentEntries((p) => ({
                              ...p,
                              [code]: {
                                content: form.content ?? "",
                                title: (form.title ?? "").trim(),
                                moral: (form.moral ?? "").trim(),
                                postStoryMission: (form.postStoryMission ?? "").trim(),
                                postStoryResourceUrl: (form.postStoryResourceUrl ?? "").trim(),
                              },
                            }));
                            showSuccess(
                              "Copied from master",
                              `${masterLbl} fields copied into ${label}. Edit the translation, then Update draft.`
                            );
                          }}
                        >
                          Copy from master ({masterLbl})
                        </Button>
                        {tabEmpty ? (
                          <span className="text-xs text-muted-foreground">Tab is empty — paste, sync, or copy to start.</span>
                        ) : null}
                      </div>
                      <div>
                        <Label htmlFor={`new-tl-title-${code}`}>Title</Label>
                        <Input
                          id={`new-tl-title-${code}`}
                          value={entry.title}
                          onChange={(e) =>
                            setTranslationContentEntries((p) => ({
                              ...p,
                              [code]: { ...(p[code] ?? emptyLibraryTranslationTab()), title: e.target.value },
                            }))
                          }
                          placeholder={`Title in ${label}`}
                          className="mt-1 rounded-lg"
                        />
                      </div>
                      <div>
                        <Label htmlFor={`new-tl-content-${code}`}>Story text</Label>
                        <textarea
                          id={`new-tl-content-${code}`}
                          value={entry.content}
                          onChange={(e) =>
                            setTranslationContentEntries((p) => ({
                              ...p,
                              [code]: {
                                ...(p[code] ?? emptyLibraryTranslationTab()),
                                content: e.target.value,
                              },
                            }))
                          }
                          placeholder={
                            translationsRefreshing
                              ? "Loading…"
                              : `Story in ${label} — run Regenerate & sync or Reload if empty`
                          }
                          className="mt-1 flex min-h-[160px] w-full rounded-lg border border-input bg-background px-3 py-2 text-sm"
                        />
                      </div>
                      <div>
                        <Label htmlFor={`new-tl-moral-${code}`}>Moral</Label>
                        <Input
                          id={`new-tl-moral-${code}`}
                          value={entry.moral}
                          onChange={(e) =>
                            setTranslationContentEntries((p) => ({
                              ...p,
                              [code]: { ...(p[code] ?? emptyLibraryTranslationTab()), moral: e.target.value },
                            }))
                          }
                          placeholder={`Moral in ${label}`}
                          className="mt-1 rounded-lg"
                        />
                      </div>
                      <div>
                        <Label htmlFor={`new-tl-mission-${code}`}>Post-episode family mission ({label})</Label>
                        <textarea
                          id={`new-tl-mission-${code}`}
                          value={entry.postStoryMission}
                          onChange={(e) =>
                            setTranslationContentEntries((p) => ({
                              ...p,
                              [code]: { ...(p[code] ?? emptyLibraryTranslationTab()), postStoryMission: e.target.value },
                            }))
                          }
                          placeholder={`Plain text for parents after the episode in ${label}. Empty uses master story values.`}
                          className="mt-1 flex min-h-[72px] w-full rounded-lg border border-input bg-background px-3 py-2 text-sm"
                          maxLength={ADMIN_LIBRARY_POST_MISSION_MAX_CHARS}
                        />
                      </div>
                      <div>
                        <Label htmlFor={`new-tl-resource-${code}`}>Post-episode resource URL ({label})</Label>
                        <Input
                          id={`new-tl-resource-${code}`}
                          value={entry.postStoryResourceUrl}
                          onChange={(e) =>
                            setTranslationContentEntries((p) => ({
                              ...p,
                              [code]: {
                                ...(p[code] ?? emptyLibraryTranslationTab()),
                                postStoryResourceUrl: e.target.value,
                              },
                            }))
                          }
                          placeholder="Optional link (inherits master if empty)"
                          className="mt-1 rounded-lg"
                          maxLength={ADMIN_LIBRARY_POST_RESOURCE_URL_MAX_CHARS}
                        />
                      </div>
                    </div>
                  );
                })}
              </CardContent>
            </Card>
          )}
        </div>

        <div className="space-y-4 lg:sticky lg:top-20 lg:self-start">
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold">1 · Regenerate &amp; sync all languages</CardTitle>
              <p className="text-xs text-muted-foreground font-normal">
                Uses the <strong>story text and fields in this form</strong>. First click creates a draft, then runs Tamixa
                conversion and syncs all languages (same as Edit → All languages). Run before Generate cover when possible.
              </p>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="space-y-1.5">
                <Label htmlFor="new-story-regenerate-prompt" className="text-xs font-medium">
                  Custom instructions (optional)
                </Label>
                <textarea
                  id="new-story-regenerate-prompt"
                  className="w-full min-h-[88px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  placeholder="e.g. Emphasize dialogue; soften act two…"
                  value={regenerateCustomPrompt}
                  onChange={(e) => setRegenerateCustomPrompt(e.target.value.slice(0, 8000))}
                  disabled={submitting || regenerateBusy}
                  maxLength={8000}
                />
                <p className="text-xs text-muted-foreground">{regenerateCustomPrompt.length}/8000</p>
              </div>
              <Button
                type="button"
                variant="secondary"
                size="default"
                className="w-full"
                disabled={submitting || !form?.content?.trim() || regenerateBusy}
                title={
                  regenerateServerRunning
                    ? "Regenerate already running"
                    : storyPipelineRunning
                      ? "Pipeline running for this story"
                      : !form?.content?.trim()
                        ? "Add story content first"
                        : undefined
                }
                onClick={() => void handleRegenerateAndSync()}
              >
                <Sparkles className={cn("h-4 w-4 mr-2", scriptSyncInProgress && "animate-pulse")} />
                {scriptSyncInProgress ? "Working…" : "Regenerate & sync all languages"}
              </Button>
              <p className="text-xs text-muted-foreground">
                Status:{" "}
                <strong>
                  {savedStoryId == null
                    ? "No row yet — first sync creates it"
                    : regenerateServerRunning
                      ? "Regenerate running"
                      : storyPipelineRunning
                        ? "Pipeline running"
                        : "Idle"}
                </strong>
              </p>
              {savedStoryId != null ? (
                <Button variant="ghost" size="sm" className="w-full rounded-xl text-xs h-auto py-2" asChild>
                  <Link href={`/dashboard/stories/${savedStoryId}/edit`}>Open full editor</Link>
                </Button>
              ) : null}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold">2 · Cover</CardTitle>
              <p className="text-xs text-muted-foreground font-normal">
                Paste a URL and/or run <strong>Generate cover</strong> after sync (recommended order).
              </p>
            </CardHeader>
            <CardContent className="space-y-3">
              <Input
                value={form.coverImageUrl ?? ""}
                onChange={(e) => setForm((f) => ({ ...f, coverImageUrl: e.target.value || null }))}
                placeholder="https://… or leave empty"
                className="rounded-lg h-9 text-sm"
                disabled={submitting || scriptSyncInProgress}
              />
              <div className="space-y-1.5">
                <Label htmlFor="new-story-cover-custom-prompt" className="text-xs font-medium">
                  Custom cover instructions (optional)
                </Label>
                <textarea
                  id="new-story-cover-custom-prompt"
                  className="w-full min-h-[72px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  placeholder="e.g. Warmer light, river in background…"
                  value={coverCustomPrompt}
                  onChange={(e) => setCoverCustomPrompt(e.target.value.slice(0, 8000))}
                  disabled={submitting || coverGenerating || scriptSyncInProgress}
                  maxLength={8000}
                />
                <p className="text-xs text-muted-foreground">{coverCustomPrompt.length}/8000</p>
              </div>
              <Button
                type="button"
                variant="outline"
                size="sm"
                className="w-full"
                disabled={coverGenerating || submitting || scriptSyncInProgress || !form?.content?.trim()}
                title={!form?.content?.trim() ? "Add story content first" : undefined}
                onClick={async () => {
                  if (!validate()) {
                    showError("Validation failed", "Fix the form before generating a cover.");
                    return;
                  }
                  setCoverGenerating(true);
                  try {
                    const id = await ensureDraftOnServer();
                    const hasCover = !!(form.coverImageUrl?.trim() || coverVideoUrl?.trim());
                    const updated = await api.admin.regenerateLibraryStoryCover(
                      id,
                      hasCover,
                      coverCustomPrompt.trim() || null
                    );
                    setForm((f) =>
                      f ? { ...f, coverImageUrl: updated?.coverImageUrl?.trim() ?? f.coverImageUrl ?? "" } : f
                    );
                    setCoverVideoUrl(updated?.coverVideoUrl ?? null);
                    setCoverRefreshKey(Date.now());
                    showSuccess("Generate cover", hasCover ? "New cover generated." : "Cover generated.");
                  } catch (e) {
                    showError("Failed", e instanceof Error ? e.message : "Generate cover failed");
                  } finally {
                    setCoverGenerating(false);
                  }
                }}
              >
                <ImagePlus className="h-3.5 w-3.5 mr-1 shrink-0" />
                {coverGenerating ? "Generating…" : "Generate cover"}
              </Button>
              {(form.coverImageUrl?.trim() || coverVideoUrl) && (
                <div className="space-y-2 pt-1">
                  {coverVideoUrl?.trim() && (
                    <div className="relative aspect-video rounded-lg border bg-muted/30 overflow-hidden">
                      {coverVideoUrl.includes(".gif") ? (
                        <Image
                          key={coverRefreshKey}
                          src={`${resolveCoverSrc(coverVideoUrl) ?? coverVideoUrl}?t=${coverRefreshKey}`}
                          alt="Animated"
                          fill
                          unoptimized
                          className="object-cover"
                        />
                      ) : (
                        <video
                          key={coverRefreshKey}
                          src={resolveCoverSrc(coverVideoUrl) ?? coverVideoUrl}
                          className="w-full h-full object-cover"
                          controls
                          loop
                          muted
                          playsInline
                        />
                      )}
                    </div>
                  )}
                  {form.coverImageUrl?.trim() && (
                    <div className="relative aspect-video rounded-lg border bg-muted/30 overflow-hidden">
                      <CoverImageWithFallback
                        key={coverRefreshKey}
                        src={`${resolveCoverSrc(form.coverImageUrl) ?? form.coverImageUrl}?t=${coverRefreshKey}`}
                      />
                    </div>
                  )}
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold">3 · Submit for review</CardTitle>
              <p className="text-xs text-muted-foreground font-normal">
                Sends to Story for review. If no row exists yet, the story is created and submitted in one step. When a row
                already exists, the pipeline must be idle. Opens Edit on the Submit step.
              </p>
            </CardHeader>
            <CardContent className="flex flex-col gap-2">
              <Button
                onClick={() => void handleSubmitForReview()}
                size="default"
                className="w-full rounded-xl"
                variant="primary"
                disabled={
                  submitting ||
                  !canSubmitForReview ||
                  (savedStoryId != null && storyPipelineRunning) ||
                  simulatorSubmitBlockedByAudio
                }
                title={
                  storyPipelineRunning
                    ? "Wait for pipeline to finish"
                    : simulatorSubmitBlockedByAudio
                      ? "Generate audio for all simulator segments first"
                    : !canSubmitForReview
                      ? "Add story content first"
                      : undefined
                }
              >
                {submitting ? "Submitting…" : "Submit for review"}
              </Button>
              {simulatorSubmitBlockedByAudio ? (
                <div className="flex items-center justify-between gap-2">
                  <p className="text-xs text-amber-600 dark:text-amber-500">
                    Blocked: {simulatorSegmentsMissingAudio.length} segment(s) missing audioUrl. Generate all segment
                    audio in Segment Audio Studio before submitting.
                  </p>
                  <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    onClick={() => void handleGenerateSegmentAudio(simulatorSegmentsMissingAudio.map((s) => s.id))}
                    disabled={segmentStudioBusy || submitting}
                  >
                    {segmentStudioActivity.kind === "audio" ? (
                      <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />
                    ) : null}
                    {segmentStudioActivity.kind === "audio" ? "Generating audio…" : "Generate all missing now"}
                  </Button>
                </div>
              ) : null}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold">Optional · Save draft only</CardTitle>
              <p className="text-xs text-muted-foreground font-normal">
                Persist the form to the library <strong>without</strong> running Regenerate &amp; sync (e.g. save work in
                progress).
              </p>
            </CardHeader>
            <CardContent className="flex flex-col gap-2">
              <Button
                onClick={() => void handleSaveDraft()}
                variant="outline"
                size="default"
                className="w-full rounded-xl"
                disabled={submitting || scriptSyncInProgress}
              >
                <Save className="h-4 w-4 mr-2" />
                {savedStoryId == null ? "Save draft" : "Update draft"}
              </Button>
            </CardContent>
          </Card>

          <Button variant="outline" size="sm" className="w-full rounded-xl" asChild>
            <Link href="/dashboard/stories">All stories</Link>
          </Button>
        </div>
      </div>
    </div>
  );
}
