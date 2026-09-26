"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useRouter, useParams, usePathname, useSearchParams } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import { api, getApiBaseUrl, reconcileAdminAuthCookie, refreshTokensIfNeeded } from "@/lib/api";
import type { CreateLibraryStoryRequest, LibraryStorySummary } from "@/types/api";
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
import { useAuth } from "@/contexts/auth-context";
import { usePipelineActive } from "@/contexts/pipeline-active-context";
import { isElevatedStoryAdmin } from "@/lib/admin-roles";
import {
  canSubmitLibraryStoryForReview,
  isLibraryStoryPipelineActivelyRunning,
  isLibraryStoryInReviewQueue,
  REGENERATE_THEN_TRANSLATIONS_HELP,
  getLibraryStoryMasterScriptContentError,
  adminStoryEmotionModeLabel,
  ADMIN_POST_CREATE_PROMPTS_KEY,
  buildLibraryStoryAdminProgressSteps,
} from "@/lib/library-story-workflow";
import { parseJsonStoryContent, resolveLibraryStoryEditorBody } from "@/lib/utils";
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
  DEFAULT_LIBRARY_CHILD_NAME,
  DEFAULT_LIBRARY_EMOTION_MODE,
  DEFAULT_LIBRARY_SOURCE_LANGUAGE,
  INTERACTIVE_GRAPH_MASTER_LOCALE_KEY,
  LIBRARY_ENGLISH_LANGUAGE_CODE,
  LIBRARY_SOURCE_LANGUAGE_OPTIONS,
  LIBRARY_TAB_LANGUAGES,
  SEGMENT_STUDIO_PARENT_STORAGE_KEY,
  emptyLibraryTranslationTab,
  buildLibraryTranslationContentPayload,
  type LibraryTranslationTabFields,
} from "@/lib/library-story-admin-constants";
import {
  parseInteractiveSegments,
  segmentStudioActivityMessage,
  type SegmentStudioActivity,
} from "@/lib/library-story-segment-studio";
import {
  ArrowLeft,
  Save,
  ImagePlus,
  Sparkles,
  RefreshCw,
  ExternalLink,
  Braces,
  LayoutTemplate,
  FileText,
  Loader2,
  GitBranch,
} from "lucide-react";
import { InteractiveGraphSchemaHint } from "@/components/interactive-graph-schema-hint";
import { cn, interactiveGraphJsonRoughlyEqual } from "@/lib/utils";
import {
  StoryWorkflowStepper,
  StoryWorkflowStepFooter,
  EDIT_LIBRARY_STORY_WORKFLOW_STEPS,
} from "@/components/story-workflow-stepper";
import { StoryReviewStepPanel, StoryNarrationStepPanel } from "@/components/story-workflow-contextual-panels";

function stringifyInteractiveGraphOverlay(raw: unknown): string {
  if (raw == null) return "";
  if (typeof raw === "string") return raw;
  try {
    return JSON.stringify(raw, null, 2);
  } catch {
    return "";
  }
}

function isSimulatorTheme(theme: string | null | undefined): boolean {
  return (theme?.trim() ?? "").startsWith(SIMULATOR_THEME_PREFIX);
}

function sourceLangLabel(code: string): string {
  return LIBRARY_SOURCE_LANGUAGE_OPTIONS.find((l) => l.code === code)?.label?.replace(/ \(recommended\)/, "") ?? code;
}

function splitSentences(text: string): string[] {
  const t = (text ?? "").replace(/\r\n/g, "\n").trim();
  if (!t) return [];
  // Split on common punctuation and also on newlines (helps for LLM output formatting).
  // Includes Indic danda "।" which commonly terminates sentences in Hindi/related languages.
  const parts = t.match(/[^.!?।\n]+[.!?।]+|[^.!?।\n]+/g);
  return parts ? parts.map((p) => p.trim()).filter(Boolean) : [t];
}

function normalizeForSimilarity(s: string): string[] {
  const clean = (s ?? "")
    .toLowerCase()
    // Keep letters/numbers (including Indic scripts) + whitespace; drop punctuation.
    // Avoid Unicode property escapes (`\p{...}`) so this works with older TS/JS targets.
    .replace(/[^a-z0-9\u00C0-\u024F\u0B80-\u0BFF\u0900-\u097F\u0C00-\u0C7F\u0C80-\u0CFF\u0D00-\u0D7F\u0980-\u09FF\s]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
  if (!clean) return [];
  return clean.split(/\s+/);
}

function renderHighlightedParaphrase(beforeText: string, afterText: string) {
  const beforeSentences = splitSentences(beforeText);
  const beforeTokens = beforeSentences.map(normalizeForSimilarity);
  const afterSentences = splitSentences(afterText);

  // Naive matching: for each after sentence, mark it "changed" if it isn't close to any before sentence.
  const threshold = 0.6;

  function tokenizeWordsForDiff(s: string): { raw: string; key: string }[] {
    const parts = (s ?? "").split(/\s+/).filter(Boolean);
    return parts.map((raw) => {
      // Keep only letters/digits from our supported script ranges.
      const key = raw
        .toLowerCase()
        .replace(/[^a-z0-9\u00C0-\u024F\u0B80-\u0BFF\u0900-\u097F\u0C00-\u0C7F\u0C80-\u0CFF\u0D00-\u0D7F\u0980-\u09FF]+/g, "");
      return { raw, key: key || raw.toLowerCase() };
    });
  }

  function renderWordDiff(beforeSentence: string, afterSentence: string) {
    const a = tokenizeWordsForDiff(beforeSentence);
    const b = tokenizeWordsForDiff(afterSentence);
    if (a.length === 0 || b.length === 0) {
      return (
        <span className="bg-yellow-200/70 rounded px-0.5 py-[1px]">
          {afterSentence + " "}
        </span>
      );
    }
    if (a.length > 80 || b.length > 80) {
      // Guardrail: prevent expensive DP for very long sentences.
      return (
        <mark className="rounded px-0.5 py-[1px] bg-yellow-200/70">
          {afterSentence + " "}
        </mark>
      );
    }

    const aKeys = a.map((t) => t.key);
    const bKeys = b.map((t) => t.key);
    const cols = bKeys.length + 1;
    const dp = new Uint16Array((aKeys.length + 1) * cols);
    const idx = (i: number, j: number) => i * cols + j;

    // LCS DP on normalized token keys.
    for (let i = 1; i <= aKeys.length; i++) {
      for (let j = 1; j <= bKeys.length; j++) {
        if (aKeys[i - 1] === bKeys[j - 1]) {
          dp[idx(i, j)] = dp[idx(i - 1, j - 1)] + 1;
        } else {
          dp[idx(i, j)] = Math.max(dp[idx(i - 1, j)], dp[idx(i, j - 1)]);
        }
      }
    }

    const unchangedAfter = Array(b.length).fill(false);
    let i = aKeys.length;
    let j = bKeys.length;
    while (i > 0 && j > 0) {
      if (aKeys[i - 1] === bKeys[j - 1]) {
        unchangedAfter[j - 1] = true;
        i--;
        j--;
        continue;
      }
      const v1 = dp[idx(i - 1, j)];
      const v2 = dp[idx(i, j - 1)];
      if (v1 >= v2) i--;
      else j--;
    }

    return (
      <>
        {b.map((tok, tokIdx) =>
          unchangedAfter[tokIdx] ? (
            // eslint-disable-next-line react/no-array-index-key
            <span key={tokIdx}>{tok.raw + " "}</span>
          ) : (
            // eslint-disable-next-line react/no-array-index-key
            <mark key={tokIdx} className="rounded px-0.5 py-[1px] bg-yellow-200/70">
              {tok.raw + " "}
            </mark>
          )
        )}
      </>
    );
  }

  return afterSentences.map((afterSentence, idx) => {
    let best = 0;
    let bestBeforeIdx = 0;
    const afterTokens = normalizeForSimilarity(afterSentence);
    const afterSet = new Set(afterTokens);
    for (let i = 0; i < beforeSentences.length; i++) {
      const beforeSet = new Set(beforeTokens[i]);
      let intersection = 0;
      for (const tok of afterSet) {
        if (beforeSet.has(tok)) intersection++;
      }
      const sim =
        beforeSet.size === 0 && afterSet.size === 0 ? 1 : intersection / Math.max(beforeSet.size, afterSet.size);
      if (sim > best) {
        best = sim;
        bestBeforeIdx = i;
      }
      if (best >= threshold) break;
    }
    const changed = best < threshold;
    if (!changed) {
      return (
        // eslint-disable-next-line react/no-array-index-key
        <span key={`${idx}-u`}>{afterSentence + " "}</span>
      );
    }

    return (
      // eslint-disable-next-line react/no-array-index-key
      <span key={`${idx}-c`}>{renderWordDiff(beforeSentences[bestBeforeIdx] ?? "", afterSentence)}</span>
    );
  });
}

function renderInlineFieldDiff(beforeValue: string, afterValue: string) {
  const before = (beforeValue ?? "").trim();
  const after = (afterValue ?? "").trim();
  if (before === after) return <span>{afterValue}</span>;
  return (
    <mark className="rounded px-0.5 py-[1px] bg-yellow-200/70">
      {afterValue}
    </mark>
  );
}

/** Resolve cover URL: relative paths get API base (e.g. /api/v1/covers/... → full API URL). */
function resolveCoverSrc(coverImageUrl: string | null | undefined): string | null {
  if (!coverImageUrl?.trim()) return null;
  const u = coverImageUrl.trim();
  if (u.startsWith("http://") || u.startsWith("https://")) return u;
  const base = getApiBaseUrl();
  if (base) return u.startsWith("/") ? `${base}${u}` : `${base}/${u}`;
  return u.startsWith("/") ? u : null; // SSR: relative path, same-origin
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

const WORKFLOW_STEP_COUNT = EDIT_LIBRARY_STORY_WORKFLOW_STEPS.length;

/** Keys on pipeline-status payload that are not per-language stage strings. */
function isStoryPipelineRunning(status: Record<string, string | undefined> | null | undefined): boolean {
  // Back-compat wrapper: keep local name, but share behavior across the admin flow.
  return isLibraryStoryPipelineActivelyRunning(status ?? null);
}

export default function EditLibraryStoryPage() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const params = useParams();
  const id = params?.id ? Number(params.id) : null;

  const workflowStep = (() => {
    const raw = searchParams.get("step");
    const n = raw !== null ? Number.parseInt(raw, 10) : 0;
    if (Number.isNaN(n)) return 0;
    return Math.min(WORKFLOW_STEP_COUNT - 1, Math.max(0, n));
  })();

  const setWorkflowStep = useCallback(
    (i: number) => {
      const next = Math.min(WORKFLOW_STEP_COUNT - 1, Math.max(0, i));
      const q = new URLSearchParams(searchParams.toString());
      q.set("step", String(next));
      router.replace(`${pathname}?${q.toString()}`, { scroll: false });
    },
    [pathname, router, searchParams]
  );
  const { showSuccess, showError } = useActionResult();
  const { user } = useAuth();
  const { refresh: refreshPipelineActive } = usePipelineActive();

  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState<CreateLibraryStoryRequest | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [coverGenerating, setCoverGenerating] = useState(false);
  const [coverCustomPrompt, setCoverCustomPrompt] = useState("");
  const [coverRefreshKey, setCoverRefreshKey] = useState(0);
  const [coverVideoUrl, setCoverVideoUrl] = useState<string | null>(null);
  const [rephrasing, setRephrasing] = useState(false);
  const [rephraseSuggestion, setRephraseSuggestion] = useState<{
    suggestedTitle: string;
    suggestedContent: string;
  } | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  /** Regenerate-with-prompt + save + rebuild-narration-pipeline (single admin action). */
  const [scriptSyncInProgress, setScriptSyncInProgress] = useState(false);
  /** Optional notes appended after the standard Tamixa conversion template for “Regenerate & sync”. */
  const [regenerateCustomPrompt, setRegenerateCustomPrompt] = useState("");
  /** Per-language content for non-Tamil languages (optional). When set, sent as translationContentEntries so other languages are updated. */
  const [translationContentEntries, setTranslationContentEntries] = useState<
    Record<string, LibraryTranslationTabFields>
  >({});

  const [paraphraseBefore, setParaphraseBefore] = useState<{
    content: string;
    title: string;
    moral: string;
  } | null>(null);
  /** Per-target language before-paraphrase snapshot (for UI diff). */
  const [paraphraseBeforeByLang, setParaphraseBeforeByLang] = useState<
    Record<string, { content: string; title: string; moral: string }> | null
  >(null);
  const [showParaphraseDiff, setShowParaphraseDiff] = useState(false);

  const [activeLangTab, setActiveLangTab] = useState<string>("en");
  const [refreshingContent, setRefreshingContent] = useState(false);
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
  /** Per-locale interactive graph text: master key + optional language codes (e.g. hi). Empty string = clear DB overlay. */
  const [interactiveGraphSlots, setInteractiveGraphSlots] = useState<Record<string, string>>({});
  const [interactiveGraphEditLocale, setInteractiveGraphEditLocale] = useState<string>(INTERACTIVE_GRAPH_MASTER_LOCALE_KEY);
  /** True while reloading main + tabs after changing Source language (master). */
  const [sourceLanguageLoading, setSourceLanguageLoading] = useState(false);
  const [regenerateGate, setRegenerateGate] = useState({
    locked: false,
    lockApproved: false,
    unlockRequestedAt: null as string | null,
  });
  const [unlockRequestSubmitting, setUnlockRequestSubmitting] = useState(false);
  const [approveUnlockSubmitting, setApproveUnlockSubmitting] = useState(false);
  /** Reviewer notes from API (REJECTED / CHANGES_REQUESTED); story is never deleted for those statuses. */
  const [reviewerFeedback, setReviewerFeedback] = useState<string | null>(null);
  /** Story-specific pipeline status (for this story id). */
  const [storyPipelineRunning, setStoryPipelineRunning] = useState(false);
  /** Server-side lock for regenerate-with-prompt (prevents concurrent regenerate requests). */
  const [regenerateServerRunning, setRegenerateServerRunning] = useState(false);
  /** Last time regenerate running/idle status was checked from backend. */
  const [regenerateStatusLastCheckedAt, setRegenerateStatusLastCheckedAt] = useState<Date | null>(null);
  /** Last time backend reported regenerate transitioned to completed for this story. */
  const [regenerateLastCompletedAt, setRegenerateLastCompletedAt] = useState<Date | null>(null);
  /** True when regenerate status endpoint is unavailable; UI falls back to best-effort signals. */
  const [regenerateStatusUnavailable, setRegenerateStatusUnavailable] = useState(false);
  const regeneratePrevRunningRef = useRef(false);
  /** Guard against rapid double-clicks before React state disables buttons. */
  const submitInFlightRef = useRef(false);
  const regenerateInFlightRef = useRef(false);
  /** One-time: apply Create-page prompts + optional welcome when landing from new story flow. */
  const postCreateFlowHandledRef = useRef<number | null>(null);

  const elevatedAdmin = isElevatedStoryAdmin(user?.role);
  const regenerateBlockedForRole =
    regenerateGate.locked && !regenerateGate.lockApproved && !elevatedAdmin;

  const wordCount = form?.content
    ? form.content.split(/\s+/).filter((w) => w.trim()).length
    : 0;
  const interactiveGraphLint = useMemo(() => {
    const ig = form?.interactiveGraph?.trim();
    if (!ig) return { ok: true as const, errors: [] as string[] };
    return lintInteractiveGraphJson(ig);
  }, [form?.interactiveGraph]);
  const interactiveCategoryHint = useMemo(() => {
    const ig = form?.interactiveGraph?.trim();
    if (!ig || !interactiveGraphLint.ok) return null;
    return validateInteractiveStoryCategory(form?.theme, ig);
  }, [form?.theme, form?.interactiveGraph, interactiveGraphLint.ok]);
  const interactiveGraphOutline = useMemo(() => {
    const ig = form?.interactiveGraph?.trim();
    if (!ig || !interactiveGraphLint.ok) return null;
    return outlineInteractiveGraphJson(ig);
  }, [form?.interactiveGraph, interactiveGraphLint.ok]);
  const simulatorSelected = useMemo(() => isSimulatorTheme(form?.theme), [form?.theme]);
  const interactiveGraphRequiredError = useMemo(() => {
    if (!simulatorSelected) return null;
    if (!form?.interactiveGraph?.trim()) {
      return 'Simulator stories require "Interactive graph (JSON)".';
    }
    return null;
  }, [simulatorSelected, form?.interactiveGraph]);
  const interactiveSegmentUrlErrors = useMemo(() => {
    const ig = form?.interactiveGraph?.trim();
    if (!ig || !interactiveGraphLint.ok) return [];
    return validateInteractiveSegmentUrls(ig);
  }, [form?.interactiveGraph, interactiveGraphLint.ok]);
  const interactiveSegments = useMemo(
    () => parseInteractiveSegments(form?.interactiveGraph),
    [form?.interactiveGraph]
  );
  const simulatorSegmentsMissingAudio = useMemo(
    () => interactiveSegments.filter((s) => !s.audioUrl?.trim()),
    [interactiveSegments]
  );
  const simulatorSubmitBlockedByAudio =
    simulatorSelected && interactiveSegments.length > 0 && simulatorSegmentsMissingAudio.length > 0;
  /** TTS / script fill language for the graph being edited (locale-specific overlays use that locale code). */
  const segmentStudioLanguage = useMemo(
    () =>
      interactiveGraphEditLocale === INTERACTIVE_GRAPH_MASTER_LOCALE_KEY
        ? (form?.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE).toLowerCase()
        : interactiveGraphEditLocale.toLowerCase(),
    [form?.language, interactiveGraphEditLocale]
  );
  /** LLM context for graph/script tools: master body, or the translation tab matching Graph locale when non-empty. */
  const segmentStudioStoryText = useMemo(() => {
    if (!form) return "";
    if (interactiveGraphEditLocale === INTERACTIVE_GRAPH_MASTER_LOCALE_KEY) {
      return form.content ?? "";
    }
    const localized = translationContentEntries[interactiveGraphEditLocale]?.content?.trim();
    return localized || form.content || "";
  }, [form, interactiveGraphEditLocale, translationContentEntries]);
  const isValidWordCount = simulatorSelected ? !!form?.content?.trim() : wordCount >= MIN_WORD_COUNT;
  const inReviewQueue = isLibraryStoryInReviewQueue(form?.status);
  const workflowBusy =
    submitting || sourceLanguageLoading || scriptSyncInProgress || refreshingContent;
  const blockNextStepForPipeline = workflowStep === 1 && storyPipelineRunning;
  const regenerateBusy = scriptSyncInProgress || storyPipelineRunning || regenerateServerRunning;
  const canSubmitForReview = canSubmitLibraryStoryForReview(form?.status, !!form?.content?.trim());
  const submitReady =
    !!form &&
    canSubmitForReview &&
    !storyPipelineRunning &&
    !simulatorSubmitBlockedByAudio;

  const storyProgressSteps = useMemo(
    () =>
      buildLibraryStoryAdminProgressSteps({
        minWordCount: MIN_WORD_COUNT,
        wordCount,
        titleTrimmed: !!form?.title?.trim(),
        themeSet: !!form?.theme,
        contentTrimmed: !!form?.content?.trim(),
        simulatorSelected,
        simulatorGraphFieldsOk:
          !!form?.interactiveGraph?.trim() &&
          interactiveGraphLint.ok &&
          interactiveSegmentUrlErrors.length === 0,
        onServer: id != null,
        regenerateBusy,
        hasCover: !!(form?.coverImageUrl?.trim() || coverVideoUrl?.trim()),
        submitReady,
      }),
    [
      id,
      wordCount,
      form?.title,
      form?.theme,
      form?.content,
      form?.interactiveGraph,
      form?.coverImageUrl,
      simulatorSelected,
      interactiveGraphLint.ok,
      interactiveSegmentUrlErrors.length,
      regenerateBusy,
      coverVideoUrl,
      submitReady,
    ]
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

  /** Fetch “other language” tabs for a given master/source language. */
  const fetchTranslationTabs = useCallback(
    async (storyId: number, masterLang: string, masterGraphStr: string) => {
      const tabLangs = LIBRARY_TAB_LANGUAGES.filter((l) => l.code !== masterLang);
      const masterNorm = masterGraphStr.trim();
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
      const next: Record<string, LibraryTranslationTabFields> = {};
      const graphOverlays: Record<string, string> = {};
      langResults.forEach(
        ({ code, content, title, moral, postStoryMission, postStoryResourceUrl, overlayGraphStr, mergedGraphStr }) => {
          next[code] = {
            content,
            title: title ?? "",
            moral: moral ?? "",
            postStoryMission: postStoryMission ?? "",
            postStoryResourceUrl: postStoryResourceUrl ?? "",
          };
          const overlay = overlayGraphStr.trim();
          const merged = mergedGraphStr.trim();
          const slot =
            overlay ||
            (merged && !interactiveGraphJsonRoughlyEqual(merged, masterNorm) ? merged : undefined);
          if (slot) graphOverlays[code] = slot;
        }
      );
      return { tabLangs, entries: next, graphOverlays };
    },
    []
  );

  /** Apply GET /admin/stories/:id (optional ?language=) to the whole editor: form, graph slots, tabs, segment studio. */
  const applyEditorFromLoadedLibraryStory = useCallback(
    (
      story: LibraryStorySummary & { content: string },
      tabLangs: { code: string; label: string }[],
      entries: Record<string, LibraryTranslationTabFields>,
      graphOverlays: Record<string, string>,
      masterInteractiveGraphStr: string
    ) => {
      const contentToEdit = resolveLibraryStoryEditorBody(story);
      const parsed = parseJsonStoryContent(contentToEdit);
      const resolved = parsed ?? { content: contentToEdit };
      const lang = (story.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE).toLowerCase();
      setForm({
        title: (story.title ?? resolved.title ?? "")?.trim() || "",
        content: resolved.content,
        theme: resolved.theme ?? story.theme,
        language: lang,
        age: story.age,
        childName: story.childName ?? DEFAULT_LIBRARY_CHILD_NAME,
        moral: (story.moral ?? resolved.moral ?? "")?.trim() || "",
        status: story.status ?? "DRAFT",
        coverImageUrl: story.coverImageUrl ?? "",
        emotionMode: story.emotionMode ?? DEFAULT_LIBRARY_EMOTION_MODE,
        parentDiscussionPrompts: story.parentDiscussionPrompts?.length
          ? [...story.parentDiscussionPrompts]
          : undefined,
        parentContentNote: story.parentContentNote ?? null,
        speakAlongPrompt: story.speakAlongPrompt ?? null,
        interactiveGraph: masterInteractiveGraphStr,
        postStoryMission: story.postStoryMission?.trim() ?? "",
        postStoryResourceUrl: story.postStoryResourceUrl?.trim() ?? "",
      });
      setInteractiveGraphSlots({
        [INTERACTIVE_GRAPH_MASTER_LOCALE_KEY]: masterInteractiveGraphStr,
        ...graphOverlays,
      });
      setInteractiveGraphEditLocale(INTERACTIVE_GRAPH_MASTER_LOCALE_KEY);
      setReviewerFeedback(story.reviewNotes?.trim() ? story.reviewNotes.trim() : null);
      setCoverVideoUrl(story.coverVideoUrl ?? null);
      setTranslationContentEntries(entries);
      setActiveLangTab(tabLangs[0]?.code ?? "en");
      setRegenerateGate({
        locked: !!story.regeneratePromptLocked,
        lockApproved: !!story.regeneratePromptLockApproved,
        unlockRequestedAt: story.regeneratePromptUnlockRequestedAt ?? null,
      });
      setParaphraseBefore(null);
      setParaphraseBeforeByLang(null);
      setShowParaphraseDiff(false);
      setRephraseSuggestion(null);
      setValidationErrors({});
      setSegmentStudioActivity({ kind: "idle" });
      setSegmentVoiceStatus(null);
      segmentVoiceAutoLoadedRef.current = false;
    },
    []
  );

  /** Load story and all language variants (pipeline-generated content). Use on mount and via "Refresh content" after pipeline completes. */
  const loadStoryContent = useCallback(
    async (storyId: number) => {
      const story = await api.admin.getLibraryStory(storyId);
      const sourceLang = (story.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE).toLowerCase();
      const masterIg = stringifyInteractiveGraphOverlay(story.interactiveGraph);
      const { tabLangs, entries, graphOverlays } = await fetchTranslationTabs(storyId, sourceLang, masterIg);
      applyEditorFromLoadedLibraryStory(story, tabLangs, entries, graphOverlays, masterIg);
    },
    [fetchTranslationTabs, applyEditorFromLoadedLibraryStory]
  );

  /** Reload language tabs until pipeline no longer shows TRANSLATING/REWRITING/TTS (Generate translations is async). */
  const pollTranslationsAfterRebuild = useCallback(
    async (storyId: number) => {
      const maxRounds = ADMIN_LIBRARY_PIPELINE_POLL_MAX_ROUNDS;
      let sawLanguageWork = false;
      for (let round = 0; round < maxRounds; round++) {
        await new Promise((r) =>
          setTimeout(r, round === 0 ? ADMIN_LIBRARY_PIPELINE_POLL_FIRST_MS : ADMIN_LIBRARY_PIPELINE_POLL_INTERVAL_MS)
        );
        try {
          const status = await api.admin.getLibraryStoryPipelineStatus(storyId);
          const busy = isStoryPipelineRunning(status ?? undefined);
          setStoryPipelineRunning(busy);
          if (busy) sawLanguageWork = true;
          await loadStoryContent(storyId);
          // Avoid stopping on the initial all-PENDING state before work starts; require work seen or several polls.
          if (
            status &&
            Object.keys(status).length > 0 &&
            !busy &&
            (sawLanguageWork || round >= 4)
          ) {
            showSuccess(
              "Language tabs updated",
              "The pipeline is idle; per-language fields were reloaded from the server."
            );
            return;
          }
        } catch {
          await loadStoryContent(storyId);
        }
      }
    },
    [loadStoryContent, showSuccess]
  );

  const refreshStoryPipelineStatus = useCallback(async (storyId: number) => {
    const status = await api.admin.getLibraryStoryPipelineStatus(storyId);
    setStoryPipelineRunning(isStoryPipelineRunning(status ?? undefined));
  }, []);

  const refreshRegenerateStatus = useCallback(async (storyId: number) => {
    try {
      const status = await api.admin.getRegenerateWithPromptStatus(storyId);
      const running = !!status.running;
      const wasRunning = regeneratePrevRunningRef.current;
      setRegenerateServerRunning(running);
      setRegenerateStatusUnavailable(false);
      setRegenerateStatusLastCheckedAt(new Date());
      if (wasRunning && !running) {
        setRegenerateLastCompletedAt(new Date());
      }
      regeneratePrevRunningRef.current = running;
      return running;
    } catch {
      // Keep page usable even if status endpoint is unavailable or temporarily failing.
      setRegenerateServerRunning(false);
      setRegenerateStatusUnavailable(true);
      setRegenerateStatusLastCheckedAt(new Date());
      regeneratePrevRunningRef.current = false;
      return false;
    }
  }, []);

  const getStoryPipelineRunningNow = useCallback(async (storyId: number): Promise<boolean> => {
    const status = await api.admin.getLibraryStoryPipelineStatus(storyId);
    let running = isStoryPipelineRunning(status ?? undefined);
    if (running) {
      try {
        const active = await api.admin.getPipelineActiveNow();
        const hasActiveEntryForStory = (active.activeStories ?? []).some((s) => s.storyId === storyId);
        // If this story is not active in the tracker, attempt to clear stale marker and re-check status once.
        if (!hasActiveEntryForStory) {
          try {
            await api.admin.clearStuckPipeline(storyId);
          } catch {
            // Best-effort only; fallback to status recheck below.
          }
          const rechecked = await api.admin.getLibraryStoryPipelineStatus(storyId);
          running = isStoryPipelineRunning(rechecked ?? undefined);
        }
      } catch {
        // Ignore active-now probe failures; keep first status result.
      }
    }
    setStoryPipelineRunning(running);
    return running;
  }, []);

  useEffect(() => {
    if (!id || (!storyPipelineRunning && !regenerateServerRunning)) return;
    const timer = window.setInterval(() => {
      void Promise.all([refreshStoryPipelineStatus(id), refreshRegenerateStatus(id)]).catch(() => {
        // Best-effort background refresh to clear stale busy flags.
      });
    }, 8000);
    return () => window.clearInterval(timer);
  }, [id, storyPipelineRunning, regenerateServerRunning, refreshStoryPipelineStatus, refreshRegenerateStatus]);

  /** Source language = master: load that locale into the main fields and refresh other-language tabs. */
  const switchMasterSourceLanguage = useCallback(
    async (storyId: number, newLang: string) => {
      setSourceLanguageLoading(true);
      try {
        const normalized = newLang.trim().toLowerCase();
        const main = await api.admin.getLibraryStory(storyId, normalized);
        const masterIg = stringifyInteractiveGraphOverlay(main.interactiveGraph);
        const { tabLangs, entries, graphOverlays } = await fetchTranslationTabs(storyId, normalized, masterIg);
        applyEditorFromLoadedLibraryStory(main, tabLangs, entries, graphOverlays, masterIg);
      } catch (e) {
        showError(
          "Could not switch language",
          e instanceof Error ? e.message : "Failed to load this language. Try Refresh content."
        );
      } finally {
        setSourceLanguageLoading(false);
      }
    },
    [fetchTranslationTabs, applyEditorFromLoadedLibraryStory, showError]
  );

  useEffect(() => {
    if (!id || isNaN(id)) {
      showError("Invalid story ID", "The story ID is invalid or not found.");
      router.push("/dashboard/stories");
      return;
    }
    Promise.all([loadStoryContent(id), refreshStoryPipelineStatus(id), refreshRegenerateStatus(id)])
      .catch((e) => {
        showError("Failed to load story", e instanceof Error ? e.message : "Unable to load the story. Please try again.");
        router.push("/dashboard/stories");
      })
      .finally(() => setLoading(false));
  }, [id, router, loadStoryContent, refreshStoryPipelineStatus, refreshRegenerateStatus, showError]);

  useEffect(() => {
    if (!id || Number.isNaN(id) || loading) return;
    if (postCreateFlowHandledRef.current === id) return;

    try {
      const raw = sessionStorage.getItem(ADMIN_POST_CREATE_PROMPTS_KEY);
      if (raw) {
        const parsed = JSON.parse(raw) as { storyId?: number; cover?: string; regenerate?: string };
        if (parsed.storyId === id) {
          if (parsed.cover?.trim()) {
            setCoverCustomPrompt((p) => (p.trim() ? p : parsed.cover!));
          }
          if (parsed.regenerate?.trim()) {
            setRegenerateCustomPrompt((p) => (p.trim() ? p : parsed.regenerate!));
          }
          sessionStorage.removeItem(ADMIN_POST_CREATE_PROMPTS_KEY);
        }
      }
      if (searchParams.get("fromCreate") === "1") {
        showSuccess(
          "Story created",
          "You are on All languages: run Regenerate & sync when ready, then Cover, then Submit — same order as Edit."
        );
        const q = new URLSearchParams(searchParams.toString());
        q.delete("fromCreate");
        router.replace(`${pathname}?${q.toString()}`, { scroll: false });
      }
    } catch {
      /* ignore */
    } finally {
      postCreateFlowHandledRef.current = id;
    }
  }, [id, loading, searchParams, pathname, router, showSuccess]);

  const validate = useCallback((): boolean => {
    if (!form) return false;
    const errs: Record<string, string> = {};
    if (!form.theme?.trim()) errs.theme = "Category is required";
    if (!form.content?.trim()) errs.content = "Story text is required";
    else if (!simulatorSelected) {
      if (wordCount < MIN_WORD_COUNT) {
        errs.content = `Minimum ${MIN_WORD_COUNT} words required (current: ${wordCount})`;
      } else {
        const scriptErr = getLibraryStoryMasterScriptContentError(form.content, form.language);
        if (scriptErr) errs.content = scriptErr;
      }
    } else {
      // Learn · Simulator: master text is optional context (e.g. for graph-from-story); skip min length and script checks when short.
      if (wordCount >= MIN_WORD_COUNT) {
        const scriptErr = getLibraryStoryMasterScriptContentError(form.content, form.language);
        if (scriptErr) errs.content = scriptErr;
      }
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

  const goNextWorkflowStep = useCallback(() => {
    if (workflowStep === 0 && !validate()) {
      showError("Check story", "Fix the highlighted fields before continuing.");
      return;
    }
    setWorkflowStep(workflowStep + 1);
  }, [workflowStep, validate, setWorkflowStep, showError]);

  /** Tamixa TTS-script conversion → persist draft (all languages from API) → server pipeline rebuild. */
  const handleRegenerateAndSyncScripts = useCallback(async () => {
    if (!id || !form) return;
    if (regenerateInFlightRef.current) return;
    if (regenerateServerRunning) {
      showError("Regenerate in progress", "Regenerate is already running for this story. Please wait for it to finish.");
      return;
    }
    if (!validate()) {
      showError("Validation failed", "Please fix the validation errors before continuing.");
      return;
    }
    if (regenerateBlockedForRole) {
      showError("Not allowed", "Regenerate is locked until a Super Admin approves unlock.");
      return;
    }
    regenerateInFlightRef.current = true;
    setScriptSyncInProgress(true);
    setRegenerateServerRunning(true);
    setRegenerateStatusLastCheckedAt(new Date());
    regeneratePrevRunningRef.current = true;
    try {
        const persistDraftWithRetry = async (
        storyId: number,
        payload: CreateLibraryStoryRequest & {
          regenerateNarration?: boolean;
          translationContentEntries?: CreateLibraryStoryRequest["translationContentEntries"];
        }
      ): Promise<void> => {
        try {
          await api.admin.updateLibraryStory(storyId, payload);
          return;
        } catch (e) {
          const msg = e instanceof Error ? e.message : String(e);
          const sessionLikely = /session expired|unauthorized|401|token/i.test(msg.toLowerCase());
          if (!sessionLikely) throw e;
          // When regenerate/update is long-running, the token can expire between calls.
          // Retry once after an explicit refresh so the admin doesn't lose progress.
          const refreshed = await refreshTokensIfNeeded();
          if (refreshed) reconcileAdminAuthCookie();
          await api.admin.updateLibraryStory(storyId, payload);
        }
      };

      setParaphraseBefore(null);
      setParaphraseBeforeByLang(null);
      setShowParaphraseDiff(false);
      const result = await api.admin.regenerateStoryWithPrompt(
        id,
        form.content,
        true,
        form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE,
        regenerateCustomPrompt.trim() || null
      );
      if (result.paraphraseBefore) {
        const beforeParsed =
          result.paraphraseBefore.content?.trim().startsWith("{")
            ? parseJsonStoryContent(result.paraphraseBefore.content)
            : null;
        setParaphraseBefore({
          content: beforeParsed?.content ?? result.paraphraseBefore.content ?? "",
          title: beforeParsed?.title ?? result.paraphraseBefore.title ?? "",
          moral: beforeParsed?.moral ?? result.paraphraseBefore.moral ?? "",
        });
        setShowParaphraseDiff(true);
      }
      if (result.translationsParaphraseBefore && Object.keys(result.translationsParaphraseBefore).length > 0) {
        setParaphraseBeforeByLang(result.translationsParaphraseBefore);
        setShowParaphraseDiff(true);
      }
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
        status: inReviewQueue ? "DRAFT" : form.status,
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
      const translationPayload = buildLibraryTranslationContentPayload(nextEntries);
      await persistDraftWithRetry(id, {
        ...nextForm,
        title: nextForm.title?.trim() || null,
        moral: nextForm.moral?.trim() || null,
        status: "DRAFT",
        coverImageUrl: nextForm.coverImageUrl ?? null,
        coverVideoUrl: coverVideoUrl ?? null,
        regenerateNarration: false,
        translationContentEntries: translationPayload,
      });
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
        // Skip backend rebuild when regenerate response already includes all target language content.
        setStoryPipelineRunning(false);
        refreshPipelineActive();
        await refreshStoryPipelineStatus(id);
        showSuccess(
          "Language tabs updated",
          inReviewQueue
            ? "Draft saved from Tamixa conversion and language tabs were updated directly. Re-submit for review when content looks right."
            : "Draft saved and language tabs were updated directly from regenerate output. No background rebuild was started."
        );
        void loadStoryContent(id);
      } else {
        await api.admin.rebuildNarrationPipeline(id);
        setStoryPipelineRunning(true);
        refreshPipelineActive();
        showSuccess(
          "TTS script saved · pipeline running",
          inReviewQueue
            ? "Draft saved from Tamixa conversion and all languages are rebuilding. Re-submit for review when the pipeline is idle and content looks right."
            : "Draft saved and languages are rebuilding in the background (often several minutes). Use Refresh content or wait for the pipeline banner to clear."
        );
        void loadStoryContent(id);
        void pollTranslationsAfterRebuild(id);
      }
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Save or pipeline failed.";
      if (/already running for this story|regenerate is already running/i.test(msg)) {
        setRegenerateServerRunning(true);
        showError("Regenerate in progress", "Regenerate is already running for this story. Please wait for it to finish.");
      } else {
        showError("Regenerate & sync failed", msg);
      }
    } finally {
      setScriptSyncInProgress(false);
      regenerateInFlightRef.current = false;
      void refreshRegenerateStatus(id).catch(() => {
        // Keep optimistic state if polling fails; interval refresh will reconcile.
      });
    }
  }, [
    regenerateInFlightRef,
    form,
    id,
    translationContentEntries,
    coverVideoUrl,
    inReviewQueue,
    regenerateBlockedForRole,
    regenerateServerRunning,
    validate,
    showError,
    showSuccess,
    refreshPipelineActive,
    refreshStoryPipelineStatus,
    refreshRegenerateStatus,
    loadStoryContent,
    pollTranslationsAfterRebuild,
    regenerateCustomPrompt,
  ]);

  const applyDigitalSafetyTemplate = () => {
    if (!form) return;
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
    if (!form) return;
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

  const handleGenerateGraphFromStory = useCallback(async () => {
    if (!id || !form) return;
    setSegmentStudioActivity({ kind: "graph" });
    try {
      const result = await api.admin.generateInteractiveGraphFromStory(id, {
        language: segmentStudioLanguage,
        storyText: segmentStudioStoryText,
      });
      const g = result.interactiveGraph ?? "";
      setForm((f) => (f ? { ...f, interactiveGraph: g || (f.interactiveGraph ?? "") } : f));
      setInteractiveGraphSlots((p) => {
        const next = { ...p, [interactiveGraphEditLocale]: g };
        if (interactiveGraphEditLocale === INTERACTIVE_GRAPH_MASTER_LOCALE_KEY) {
          next[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] = g;
        }
        return next;
      });
      showSuccess(
        "Interactive graph generated",
        "Segment narration scripts were filled where needed. Review branches, then use Segment Audio Studio to create MP3s."
      );
    } catch (e) {
      showError("Graph generation failed", e instanceof Error ? e.message : "Could not generate interactive graph.");
    } finally {
      setSegmentStudioActivity({ kind: "idle" });
    }
  }, [id, form, interactiveGraphEditLocale, segmentStudioLanguage, segmentStudioStoryText, showError, showSuccess]);

  const handleGenerateSegmentAudio = useCallback(
    async (segmentIds?: string[]) => {
      if (!id || !form) return;
      const selectedIds = (segmentIds ?? interactiveSegments.map((s) => s.id)).filter(Boolean);
      if (!selectedIds.length) {
        showError("No segments", "Interactive graph has no segments to synthesize.");
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
        const segments = selectedIds.map((sid) => ({
          segmentId: sid,
          text: (segmentScripts[sid] ?? interactiveSegments.find((s) => s.id === sid)?.text ?? "").trim(),
          overwriteExisting: true,
        }));
        const result = await api.admin.generateInteractiveSegmentAudio(id, {
          language: segmentStudioLanguage,
          voiceProfile: segmentVoiceProfile.trim() || "default",
          interactiveGraph: form.interactiveGraph?.trim() ?? "",
          segments,
        });
        const g = result.interactiveGraph ?? "";
        setForm((f) => (f ? { ...f, interactiveGraph: g || (f.interactiveGraph ?? "") } : f));
        setInteractiveGraphSlots((p) => {
          const next = { ...p, [interactiveGraphEditLocale]: g };
          if (interactiveGraphEditLocale === INTERACTIVE_GRAPH_MASTER_LOCALE_KEY) {
            next[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] = g;
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
          showSuccess("Segment audio generated", `${ok} segment audio URL(s) updated in interactive graph.`);
        }
      } catch (e) {
        showError("Segment audio failed", e instanceof Error ? e.message : "Could not generate segment audio.");
      } finally {
        setSegmentStudioActivity({ kind: "idle" });
      }
    },
    [
      id,
      form,
      interactiveSegments,
      segmentScripts,
      segmentVoiceProfile,
      segmentStudioLanguage,
      interactiveGraphEditLocale,
      showError,
      showSuccess,
    ]
  );

  const handleFillMissingSegmentScripts = useCallback(async () => {
    if (!id || !form) return;
    const ig = form.interactiveGraph?.trim();
    if (!ig) {
      showError("No graph", "Add or paste interactive graph JSON first.");
      return;
    }
    if (!segmentStudioStoryText.trim()) {
      showError(
        "Story text required",
        interactiveGraphEditLocale === INTERACTIVE_GRAPH_MASTER_LOCALE_KEY
          ? "Add story content in the main story body so the model can write segment narration."
          : `Add ${interactiveGraphEditLocale.toUpperCase()} story text in the "${LIBRARY_TAB_LANGUAGES.find((l) => l.code === interactiveGraphEditLocale)?.label ?? interactiveGraphEditLocale}" language tab (or fall back fills from the master body).`,
      );
      return;
    }
    setSegmentStudioActivity({ kind: "scripts" });
    try {
      const result = await api.admin.fillInteractiveSegmentScripts(id, {
        language: segmentStudioLanguage,
        interactiveGraph: ig,
        storyText: segmentStudioStoryText,
      });
      const g = result.interactiveGraph ?? "";
      setForm((f) => (f ? { ...f, interactiveGraph: g || (f.interactiveGraph ?? "") } : f));
      setInteractiveGraphSlots((p) => {
        const next = { ...p, [interactiveGraphEditLocale]: g };
        if (interactiveGraphEditLocale === INTERACTIVE_GRAPH_MASTER_LOCALE_KEY) {
          next[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] = g;
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
  }, [
    id,
    form,
    segmentStudioLanguage,
    segmentStudioStoryText,
    interactiveGraphEditLocale,
    showError,
    showSuccess,
  ]);

  const handleInteractiveGraphLocaleChange = useCallback(
    (next: string) => {
      if (!form) return;
      const flushed: Record<string, string> = {
        ...interactiveGraphSlots,
        [interactiveGraphEditLocale]: form.interactiveGraph ?? "",
      };
      const nextBody =
        next === INTERACTIVE_GRAPH_MASTER_LOCALE_KEY
          ? flushed[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] ?? ""
          : flushed[next] ?? flushed[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] ?? "";
      setInteractiveGraphSlots(flushed);
      setForm({ ...form, interactiveGraph: nextBody });
      setInteractiveGraphEditLocale(next);
    },
    [form, interactiveGraphEditLocale, interactiveGraphSlots]
  );

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

  const handleSubmit = async (publish: boolean) => {
    if (!form || !id) return;
    if (submitInFlightRef.current) return;
    try {
      const runningNow = await getStoryPipelineRunningNow(id);
      if (runningNow) {
        showError(
          "Pipeline still active",
          "The pipeline is queued or running for this story. Wait for it to finish before saving/submitting."
        );
        return;
      }
    } catch {
      // If status check fails, continue with save path; backend conflict guard still protects integrity.
    }
    if (publish && inReviewQueue) {
      showError(
        "Already in review queue",
        "This story is already in review. Save it as draft first, then submit again after your edits."
      );
      return;
    }
    if (!validate()) {
      showError("Validation failed", "Please fix the validation errors before saving.");
      return;
    }
    if (publish && simulatorSubmitBlockedByAudio) {
      showError(
        "Segment audio missing",
        "Simulator stories require audioUrl for all segments before submitting. Use Segment Audio Studio to generate missing audio."
      );
      return;
    }
    submitInFlightRef.current = true;
    setSubmitting(true);
    try {
      const translationPayload = buildLibraryTranslationContentPayload(translationContentEntries);
      const flushedSlots: Record<string, string> = {
        ...interactiveGraphSlots,
        [interactiveGraphEditLocale]: form.interactiveGraph ?? "",
      };
      const masterGraphStr = flushedSlots[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] ?? "";
      const translationInteractiveGraphEntries: Record<string, string> = {};
      for (const { code } of LIBRARY_TAB_LANGUAGES) {
        if (code === form.language) continue;
        if (Object.prototype.hasOwnProperty.call(flushedSlots, code)) {
          translationInteractiveGraphEntries[code] = flushedSlots[code] ?? "";
        }
      }
      await api.admin.updateLibraryStory(id, {
        ...form,
        title: form.title?.trim() || null,
        moral: form.moral?.trim() || null,
        status: publish ? "PUBLISHED" : "DRAFT",
        coverImageUrl: form.coverImageUrl ?? null,
        coverVideoUrl: coverVideoUrl ?? null,
        regenerateNarration: false,
        translationContentEntries: translationPayload,
        translationInteractiveGraphEntries:
          Object.keys(translationInteractiveGraphEntries).length > 0
            ? translationInteractiveGraphEntries
            : undefined,
        parentContentNote: form.parentContentNote?.trim() || null,
        speakAlongPrompt: form.speakAlongPrompt?.trim() || null,
        parentDiscussionPrompts: form.parentDiscussionPrompts?.length
          ? form.parentDiscussionPrompts
          : null,
        interactiveGraph: masterGraphStr.trim() ? masterGraphStr.trim() : null,
        postStoryMission: form.postStoryMission?.trim() || null,
        postStoryResourceUrl: form.postStoryResourceUrl?.trim() || null,
      });
      if (publish) {
        showSuccess(
          "Submitted for review",
          "Story sent to Story for review. Approve or Reject there. After approval, open Narration (Story to Speech) and use Generate audio to produce MP3s."
        );
        refreshPipelineActive();
      } else {
        showSuccess(
          "Story saved as draft",
          "Draft saved. Generate AI cover if needed, then Submit for review to send to Story for review."
        );
      }
      await refreshTokensIfNeeded();
      reconcileAdminAuthCookie();
      router.push(publish ? `/dashboard/stories?edited=${id}` : "/dashboard/stories");
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Failed to update story";
      showError("Update failed", msg);
    } finally {
      setSubmitting(false);
      submitInFlightRef.current = false;
    }
  };

  if (loading || !form) {
    return (
      <div className="space-y-6 max-w-4xl">
        <p className="text-muted-foreground">Loading story…</p>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-5xl">
      {/* Compact header */}
      <div className="flex flex-wrap items-center gap-4">
        <Link href="/dashboard/stories">
          <Button variant="ghost" size="sm" className="text-muted-foreground hover:text-foreground">
            <ArrowLeft className="h-4 w-4 mr-1" /> Back
          </Button>
        </Link>
        <div className="flex-1 min-w-0">
          <h1 className="text-2xl font-semibold tracking-tight text-foreground">
            Edit Story #{id}
          </h1>
          <p className="text-sm text-muted-foreground mt-0.5">
            {simulatorSelected ? (
              <>
                Interactive pilot · Master (saved on update):{" "}
                {sourceLangLabel(form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE)} · no minimum word count (graph + segments
                carry the experience)
              </>
            ) : (
              <>
                Min {MIN_WORD_COUNT} words · Master (saved on update):{" "}
                {sourceLangLabel(form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE)}
                {(form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE).toLowerCase() !== LIBRARY_ENGLISH_LANGUAGE_CODE
                  ? ` · ${sourceLangLabel(form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE)} script required in story text when master is not English`
                  : ""}
              </>
            )}
          </p>
        </div>
        <Button
          variant="outline"
          size="sm"
          disabled={!id || refreshingContent || sourceLanguageLoading}
          onClick={async () => {
            if (!id) return;
            setRefreshingContent(true);
            try {
              await loadStoryContent(id);
              await refreshStoryPipelineStatus(id);
              showSuccess("Content refreshed", "Story and all language content reloaded from the server (including pipeline-generated translations).");
            } catch (e) {
              showError("Refresh failed", e instanceof Error ? e.message : "Failed to load content.");
            } finally {
              setRefreshingContent(false);
            }
          }}
          title="Reload story and all language content from the server (e.g. after pipeline has generated translations)"
        >
          <RefreshCw className={cn("h-4 w-4 mr-2", refreshingContent && "animate-spin")} />
          {refreshingContent ? "Loading…" : "Refresh content"}
        </Button>
      </div>

      <div
        className="flex flex-wrap gap-2 sm:gap-3 border border-border/60 rounded-xl bg-muted/20 p-3"
        aria-label="Story readiness progress"
      >
        {storyProgressSteps.map((step, i) => (
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
            <span className={cn("font-medium", step.done ? "text-foreground" : "text-muted-foreground")}>
              {step.label}
              {"optional" in step && step.optional ? (
                <span className="font-normal text-muted-foreground"> · optional</span>
              ) : null}
            </span>
          </div>
        ))}
      </div>

      <StoryWorkflowStepper
        steps={EDIT_LIBRARY_STORY_WORKFLOW_STEPS}
        currentStep={workflowStep}
        onStepChange={(next) => {
          if (storyPipelineRunning && workflowStep === 1 && next > workflowStep) {
            showError(
              "Pipeline still active",
              "The pipeline is queued or running for this story. Wait for Regenerate & sync to finish before moving to the next step."
            );
            return;
          }
          if (next > workflowStep && workflowStep === 0 && next >= 1 && !validate()) {
            showError("Check story", "Fix the highlighted fields before leaving the Content step.");
            return;
          }
          setWorkflowStep(next);
        }}
      />
      <p className="text-xs text-muted-foreground -mt-1">
        Work through each step in order, or jump to a step. {REGENERATE_THEN_TRANSLATIONS_HELP} After{" "}
        <strong>Regenerate &amp; sync all languages</strong>, use <strong>Refresh content</strong> when the job finishes. If
        the story is already in review, use <strong>Move to draft</strong> before re-submitting.
      </p>

      {form &&
        (form.status === "REJECTED" ||
          form.status === "CHANGES_REQUESTED" ||
          (form.status === "DRAFT" && !!reviewerFeedback)) && (
          <div
            role="status"
            className="rounded-lg border border-amber-500/40 bg-amber-500/10 px-4 py-3 text-sm text-foreground"
          >
            <p className="font-medium">
              {form.status === "REJECTED"
                ? "This story was rejected — it was not removed from the database."
                : form.status === "CHANGES_REQUESTED"
                  ? "Changes were requested for this story."
                  : "This draft still has reviewer feedback from a previous review."}
            </p>
            <p className="mt-1 text-muted-foreground">
              Update the content as needed, then use <strong>Submit for review</strong> to send it back to Story for review.
            </p>
            {reviewerFeedback ? (
              <p className="mt-2 rounded-md bg-background/80 border border-border/60 px-3 py-2 text-sm whitespace-pre-wrap">
                {reviewerFeedback}
              </p>
            ) : null}
          </div>
        )}

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="space-y-6 min-h-[120px]">
          {workflowStep === 0 && (
            <>
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base font-semibold">Story content</CardTitle>
              <p className="text-sm text-muted-foreground">
                {simulatorSelected ? (
                  <>
                    Title, category, and a short <strong>master text</strong> (optional context for “generate graph from story”
                    and catalog). The <strong>interactive graph</strong> and segment lines drive the app. Next:{" "}
                    <strong>All languages</strong>, <strong>Cover</strong>, then submit.
                  </>
                ) : (
                  <>
                    Title, category, <strong>story text</strong> (master language), and optional pipeline-managed TTS script for
                    the master row. Next: <strong>All languages</strong> (regenerate &amp; sync), then <strong>Cover</strong>, then
                    submit.
                  </>
                )}
              </p>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <Label htmlFor="sourceLang">Source language *</Label>
                <Select
                  value={form?.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE}
                  disabled={sourceLanguageLoading || scriptSyncInProgress || submitting}
                  onValueChange={(v) => {
                    if (!id || v === form.language) return;
                    void switchMasterSourceLanguage(id, v);
                  }}
                >
                  <SelectTrigger id="sourceLang" className="mt-1 rounded-lg">
                    <SelectValue placeholder={sourceLanguageLoading ? "Loading…" : "Select language"} />
                  </SelectTrigger>
                  <SelectContent>
                    {LIBRARY_SOURCE_LANGUAGE_OPTIONS.map((l) => (
                      <SelectItem key={l.code} value={l.code}>
                        {l.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <p className="text-xs text-muted-foreground mt-1">
                  Changing this reloads the full editor for that language: story fields, post-episode mission/URL, parent
                  prompts, interactive graph (and per-locale slots), translation tabs, and segment studio state. After
                  editing, Save or Submit so the library stores this choice as the story&rsquo;s master language.
                </p>
              </div>
              <div>
                <div className="flex items-center justify-between gap-2">
                  <Label htmlFor="title">Title ({sourceLangLabel(form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE)}) *</Label>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    disabled={rephrasing || !id || !form?.content?.trim()}
                    onClick={async () => {
                      if (!id || !form) return;
                      setRephrasing(true);
                      setRephraseSuggestion(null);
                      try {
                        const s = await api.admin.suggestRephrase(id, form.title || null, form.content);
                        setRephraseSuggestion(s);
                      } catch (e) {
                        showError("Rephrase failed", e instanceof Error ? e.message : "Rephrase failed");
                      } finally {
                        setRephrasing(false);
                      }
                    }}
                    title="Suggest AI rephrasing"
                    className="text-xs"
                  >
                    <Sparkles className={cn("h-3.5 w-3.5 mr-1", rephrasing && "animate-pulse")} />
                    {rephrasing ? "Rephrasing…" : "AI Rephrase"}
                  </Button>
                </div>
                <Input
                  id="title"
                  value={form.title ?? ""}
                  onChange={(e) => setForm((f) => (f ? { ...f, title: e.target.value } : f))}
                  placeholder="e.g. அறிவுள்ள காகம்"
                  className="mt-1 rounded-lg"
                  dir="ltr"
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
                <Select value={form.theme} onValueChange={(v) => setForm((f) => (f ? { ...f, theme: v } : f))}>
                  <SelectTrigger className="mt-1 rounded-lg">
                    <SelectValue placeholder="Select category" />
                  </SelectTrigger>
                  <SelectContent>
                    {STORY_CATEGORIES.map((c) => (
                      <SelectItem key={c} value={c}>{c}</SelectItem>
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
                <Label htmlFor="content">
                  {simulatorSelected ? "Master text / outline" : "Story text"} (
                  {sourceLangLabel(form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE)}) *
                </Label>
                {showParaphraseDiff && paraphraseBefore ? (
                  <div className="mt-1 rounded-lg border border-border/60 bg-primary/5 p-3 space-y-2">
                    <p className="text-xs font-medium text-muted-foreground">Diff mode (live preview)</p>
                    <div className="text-sm leading-relaxed whitespace-pre-wrap" dir="ltr">
                      {renderHighlightedParaphrase(paraphraseBefore.content, form.content ?? "")}
                    </div>
                  </div>
                ) : null}
                <textarea
                  id="content"
                  value={form.content}
                  onChange={(e) => setForm((f) => (f ? { ...f, content: e.target.value } : f))}
                  placeholder={
                    simulatorSelected
                      ? `Optional: seed outline or notes in ${sourceLangLabel(form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE)} (segments hold spoken lines)…`
                      : `Enter full story text in ${sourceLangLabel(form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE)}…`
                  }
                  className="mt-2 flex min-h-[220px] w-full rounded-lg border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  dir="ltr"
                />
                <div className="flex justify-between mt-1">
                  <span className={cn("text-xs", isValidWordCount ? "text-muted-foreground" : "text-destructive")}>
                    {simulatorSelected ? (
                      <>{wordCount} words · interactive mode (no minimum)</>
                    ) : (
                      <>
                        {wordCount} / {MIN_WORD_COUNT} words
                      </>
                    )}
                  </span>
                </div>
                {paraphraseBefore && (
                  <div className="mt-3 rounded-lg border border-border/60 bg-primary/5 p-3 space-y-2">
                    <div className="flex items-center justify-between gap-3">
                      <p className="text-xs font-medium text-muted-foreground">Paraphrase changes preview</p>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => setShowParaphraseDiff((v) => !v)}
                      >
                        {showParaphraseDiff ? "Hide" : "Show"} changes
                      </Button>
                    </div>
                    {showParaphraseDiff && (
                      <div className="space-y-2">
                        <div className="text-xs text-muted-foreground space-y-1">
                          <div className="flex items-start gap-2">
                            <span className="font-medium">Title</span>
                            <span>{renderInlineFieldDiff(paraphraseBefore.title, form.title ?? "")}</span>
                          </div>
                          <div className="flex items-start gap-2">
                            <span className="font-medium">Moral</span>
                            <span>{renderInlineFieldDiff(paraphraseBefore.moral, form.moral ?? "")}</span>
                          </div>
                        </div>
                        <p className="text-xs text-muted-foreground">
                          Keep editing in the textarea below. Save draft / Submit for review will persist your edits.
                        </p>
                      </div>
                    )}
                  </div>
                )}
                {validationErrors.content && (
                  <p className="text-sm text-destructive">{validationErrors.content}</p>
                )}
                {rephraseSuggestion && (
                  <div className="mt-3 rounded-lg border border-border/60 bg-primary/5 p-3 space-y-2">
                    <p className="text-xs font-medium text-muted-foreground">Suggested rephrasing</p>
                    <div className="flex flex-wrap gap-2">
                      <Button size="sm" variant="outline" onClick={() => { setForm((f) => f ? { ...f, title: rephraseSuggestion!.suggestedTitle || f.title } : f); setRephraseSuggestion(null); showSuccess("Applied", "Title applied."); }}>Title</Button>
                      <Button size="sm" variant="outline" onClick={() => { setForm((f) => f ? { ...f, content: rephraseSuggestion!.suggestedContent } : f); setRephraseSuggestion(null); showSuccess("Applied", "Content applied."); }}>Content</Button>
                      <Button size="sm" onClick={() => { setForm((f) => f ? { ...f, title: rephraseSuggestion!.suggestedTitle || f.title, content: rephraseSuggestion!.suggestedContent } : f); setRephraseSuggestion(null); showSuccess("Applied", "Both applied."); }}>Apply both</Button>
                      <Button size="sm" variant="ghost" onClick={() => setRephraseSuggestion(null)}>Dismiss</Button>
                    </div>
                  </div>
                )}
              </div>

            </CardContent>
          </Card>

          {simulatorSelected ? (
          <Card className="border-emerald-500/35 bg-gradient-to-b from-emerald-500/[0.07] to-transparent shadow-sm dark:from-emerald-950/40">
            <CardHeader className="pb-3 space-y-1 border-b border-emerald-500/20 bg-emerald-500/[0.05] dark:bg-emerald-950/25">
              <div className="flex gap-2.5">
                <GitBranch className="h-5 w-5 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" aria-hidden />
                <div className="min-w-0 space-y-1">
                  <CardTitle className="text-base font-semibold">Interactive episode (Learn · Simulator)</CardTitle>
                  <p className="text-xs text-muted-foreground">
                    Graph + segment audio power the mobile practice hub. Use templates and studio tools below; submit when
                    every segment has audio unless you are still drafting.
                  </p>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-4 pt-4">
                <div className="flex flex-col gap-4">
                  <div className="w-full min-w-0">
                    <InteractiveGraphSchemaHint />
                  </div>
                  <div className="flex flex-wrap gap-2">
                    <Button type="button" variant="secondary" size="sm" onClick={applyDigitalSafetyTemplate}>
                      <LayoutTemplate className="h-4 w-4 mr-1.5" />
                      Digital Safety template
                    </Button>
                    <Button type="button" variant="outline" size="sm" onClick={formatInteractiveGraphField}>
                      <Braces className="h-4 w-4 mr-1.5" />
                      Format JSON
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => void handleGenerateGraphFromStory()}
                      disabled={!segmentStudioStoryText.trim() || segmentStudioBusy}
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
                        !form.interactiveGraph?.trim() ||
                        !segmentStudioStoryText.trim() ||
                        !interactiveGraphLint.ok ||
                        segmentStudioBusy
                      }
                      title="Uses the LLM to write spoken lines for any segment with empty text (e.g. outcomes after a template)."
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
                      >
                        <SelectTrigger className="h-8 w-full sm:w-[220px] rounded-lg text-xs">
                          <SelectValue placeholder="Locale" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value={INTERACTIVE_GRAPH_MASTER_LOCALE_KEY}>
                            Master ({sourceLangLabel(form.language ?? DEFAULT_LIBRARY_SOURCE_LANGUAGE)})
                          </SelectItem>
                          {LIBRARY_TAB_LANGUAGES.filter((l) => l.code !== form.language).map(({ code, label }) => (
                            <SelectItem key={code} value={code}>
                              {label} ({code})
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <p className="text-[11px] text-muted-foreground max-w-[280px] sm:text-right">
                        Each locale can store a full graph + segment audio. TTS/script fill uses the selected locale.
                        Choosing a tab under Other languages also switches this graph locale and loads saved or
                        locale-specific playback JSON (e.g. English segment URLs).
                      </p>
                    </div>
                  </div>
                  <textarea
                    id="interactiveGraph"
                    value={form.interactiveGraph ?? ""}
                    onChange={(e) => {
                      const v = e.target.value || "";
                      setForm((f) => (f ? { ...f, interactiveGraph: v } : f));
                      setInteractiveGraphSlots((p) => {
                        const next = { ...p, [interactiveGraphEditLocale]: v };
                        if (interactiveGraphEditLocale === INTERACTIVE_GRAPH_MASTER_LOCALE_KEY) {
                          next[INTERACTIVE_GRAPH_MASTER_LOCALE_KEY] = v;
                        }
                        return next;
                      });
                    }}
                    className="mt-1 flex min-h-[240px] w-full rounded-lg border border-input bg-background px-3 py-2 text-sm font-mono text-xs"
                    spellCheck={false}
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
                      <Label htmlFor="segmentVoiceProfileEdit" className="text-xs">Voice profile</Label>
                      <Select
                        value={segmentVoiceProfile}
                        onValueChange={setSegmentVoiceProfile}
                      >
                        <SelectTrigger id="segmentVoiceProfileEdit" className="mt-1 h-8 rounded-lg text-xs">
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
                          onClick={() => void handleLoadVoiceProfiles()}
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
                        Add a valid graph to parse segments, then generate missing audio URLs.
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
                      setForm((f) => (f ? { ...f, postStoryMission: e.target.value || "" } : f))
                    }
                    className="mt-1 flex min-h-[80px] w-full rounded-lg border border-input bg-background px-3 py-2 text-sm"
                    maxLength={8000}
                  />
                </div>
                <div>
                  <Label htmlFor="postStoryResourceUrl">Optional resource link (e.g. decision journal)</Label>
                  <p className="text-xs text-muted-foreground mt-1">
                    Printable journal: parent app <code className="rounded bg-muted px-1">/decision-journal.html</code>. Pre-fill
                    via admin env <code className="rounded bg-muted px-1">NEXT_PUBLIC_DECISION_JOURNAL_URL</code> (HTTPS in
                    production).
                  </p>
                  <Input
                    id="postStoryResourceUrl"
                    value={form.postStoryResourceUrl ?? ""}
                    onChange={(e) =>
                      setForm((f) => (f ? { ...f, postStoryResourceUrl: e.target.value || "" } : f))
                    }
                    className="mt-1 rounded-lg"
                    maxLength={512}
                    placeholder="https://..."
                  />
                </div>
            </CardContent>
          </Card>
          ) : (
          <Card className="border-dashed border-muted-foreground/35 bg-muted/15">
            <CardContent className="py-4">
              <p className="text-sm text-muted-foreground">
                <span className="font-medium text-foreground">Branching interactive episode?</span> Set{" "}
                <strong>Category</strong> to one that starts with <strong>Learn · Simulator</strong> to edit the JSON graph,
                segment scripts, and audio studio.
              </p>
            </CardContent>
          </Card>
          )}

          {/* Metadata: linear stories only; interactive pilots get catalog defaults above */}
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold">{simulatorSelected ? "Catalog defaults" : "Metadata"}</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              {!simulatorSelected ? (
              <>
              <div>
                <Label htmlFor="moral">Moral (optional)</Label>
                <Input id="moral" value={form.moral ?? ""} onChange={(e) => setForm((f) => (f ? { ...f, moral: e.target.value } : f))} placeholder="e.g. Sharing brings joy" className="mt-1 rounded-lg" />
              </div>
              <div>
                <Label htmlFor="emotionMode">Narration tone</Label>
                <Select
                  value={form.emotionMode ?? "CALM"}
                  onValueChange={(v) => setForm((f) => (f ? { ...f, emotionMode: v } : f))}
                >
                  <SelectTrigger id="emotionMode" className="mt-1 rounded-lg">
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
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label>Age group</Label>
                  <Select value={String(form.age)} onValueChange={(v) => setForm((f) => (f ? { ...f, age: parseInt(v, 10) } : f))}>
                    <SelectTrigger className="mt-1 rounded-lg"><SelectValue /></SelectTrigger>
                    <SelectContent>
                      {AGE_GROUPS.map((a) => (
                        <SelectItem key={a.value} value={String(a.value)}>{a.label}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <Label htmlFor="childName">Child name</Label>
                  <Input id="childName" value={form.childName ?? "Child"} onChange={(e) => setForm((f) => (f ? { ...f, childName: e.target.value } : f))} className="mt-1 rounded-lg" />
                </div>
              </div>
              <div className="rounded-lg border border-border/80 bg-muted/10 p-4 space-y-3">
                <p className="text-sm font-semibold">Parent resources (optional)</p>
                <div>
                  <Label htmlFor="parentContentNote">Content note for parents</Label>
                  <textarea
                    id="parentContentNote"
                    value={form.parentContentNote ?? ""}
                    onChange={(e) =>
                      setForm((f) => (f ? { ...f, parentContentNote: e.target.value || null } : f))
                    }
                    className="mt-1 flex min-h-[72px] w-full rounded-lg border border-input bg-background px-3 py-2 text-sm"
                    maxLength={4000}
                  />
                </div>
                <div>
                  <Label htmlFor="speakAlongPrompt">Speak-along prompt</Label>
                  <Input
                    id="speakAlongPrompt"
                    value={form.speakAlongPrompt ?? ""}
                    onChange={(e) =>
                      setForm((f) => (f ? { ...f, speakAlongPrompt: e.target.value || null } : f))
                    }
                    className="mt-1 rounded-lg"
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
                      setForm((f) =>
                        f
                          ? {
                              ...f,
                              parentDiscussionPrompts: lines.length ? lines : undefined,
                            }
                          : f
                      );
                    }}
                    className="mt-1 flex min-h-[100px] w-full rounded-lg border border-input bg-background px-3 py-2 text-sm"
                  />
                </div>
              </div>
              </>
              ) : null}
              {simulatorSelected ? (
                <>
                  <p className="text-xs text-muted-foreground">
                    Moral, narration tone, and parent discussion fields are not used for interactive pilots. Age and
                    listener label still apply for catalog and TTS defaults.
                  </p>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <Label>Age group</Label>
                      <Select value={String(form.age)} onValueChange={(v) => setForm((f) => (f ? { ...f, age: parseInt(v, 10) } : f))}>
                        <SelectTrigger className="mt-1 rounded-lg"><SelectValue /></SelectTrigger>
                        <SelectContent>
                          {AGE_GROUPS.map((a) => (
                            <SelectItem key={a.value} value={String(a.value)}>{a.label}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                    <div>
                      <Label htmlFor="childName">Child name</Label>
                      <Input id="childName" value={form.childName ?? "Child"} onChange={(e) => setForm((f) => (f ? { ...f, childName: e.target.value } : f))} className="mt-1 rounded-lg" />
                    </div>
                  </div>
                </>
              ) : null}
            </CardContent>
          </Card>
            </>
          )}

          {workflowStep === 1 && (
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base font-semibold">Other languages</CardTitle>
              <p className="text-sm text-muted-foreground">
                Edit per-language text if needed. A language tab also switches the <strong>Graph locale</strong> under All
                languages so the interactive JSON and segment studio match that locale when content exists on the server.
                Use <strong>Regenerate &amp; sync all languages</strong> in the right panel to run Tamixa TTS script
                conversion and rebuild scripts for every pipeline language (no MP3s). When idle, go to <strong>Cover</strong>.
                After approval, open <strong>Narration</strong> and use <strong>Generate audio</strong>.
              </p>
            </CardHeader>
            <CardContent className="space-y-4">
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
                      activeLangTab === code ? "bg-primary text-primary-foreground" : "bg-muted/50 text-muted-foreground hover:bg-muted"
                    )}
                  >
                    {label}
                  </button>
                ))}
              </div>
              {LIBRARY_TAB_LANGUAGES.filter((l) => l.code !== form.language).map(({ code, label }) => {
                if (activeLangTab !== code) return null;
                const entry = translationContentEntries[code] ?? emptyLibraryTranslationTab();
                const langBefore = paraphraseBeforeByLang?.[code] ?? null;
                return (
                  <div key={code} className="space-y-3">
                    {showParaphraseDiff && langBefore && (
                      <div className="mt-1 rounded-lg border border-border/60 bg-primary/5 p-3 space-y-2">
                        <p className="text-xs font-medium text-muted-foreground">
                          Diff mode (live preview) ({label})
                        </p>
                        <div className="text-xs text-muted-foreground space-y-1">
                          <div className="flex items-start gap-2">
                            <span className="font-medium">Title</span>
                            <span>{renderInlineFieldDiff(langBefore.title, entry.title)}</span>
                          </div>
                          <div className="flex items-start gap-2">
                            <span className="font-medium">Moral</span>
                            <span>{renderInlineFieldDiff(langBefore.moral, entry.moral)}</span>
                          </div>
                        </div>
                        <div className="text-sm leading-relaxed whitespace-pre-wrap" dir="ltr">
                          {renderHighlightedParaphrase(langBefore.content, entry.content ?? "")}
                        </div>
                      </div>
                    )}
                    <div>
                      <Label htmlFor={`tl-title-${code}`}>Title</Label>
                      <Input id={`tl-title-${code}`} value={entry.title} onChange={(e) => setTranslationContentEntries((p) => ({ ...p, [code]: { ...(p[code] ?? emptyLibraryTranslationTab()), title: e.target.value } }))} placeholder={`Title in ${label}`} className="mt-1 rounded-lg" />
                    </div>
                    <div>
                      <Label htmlFor={`tl-content-${code}`}>Story text</Label>
                      <textarea
                        id={`tl-content-${code}`}
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
                        placeholder={`Story in ${label}`}
                        className="mt-1 flex min-h-[120px] w-full rounded-lg border border-input bg-background px-3 py-2 text-sm"
                      />
                    </div>
                    <div>
                      <Label htmlFor={`tl-moral-${code}`}>Moral</Label>
                      <Input id={`tl-moral-${code}`} value={entry.moral} onChange={(e) => setTranslationContentEntries((p) => ({ ...p, [code]: { ...(p[code] ?? emptyLibraryTranslationTab()), moral: e.target.value } }))} placeholder={`Moral in ${label}`} className="mt-1 rounded-lg" />
                    </div>
                    <div>
                      <Label htmlFor={`tl-mission-${code}`}>Post-episode family mission ({label})</Label>
                      <textarea
                        id={`tl-mission-${code}`}
                        value={entry.postStoryMission}
                        onChange={(e) =>
                          setTranslationContentEntries((p) => ({
                            ...p,
                            [code]: { ...(p[code] ?? emptyLibraryTranslationTab()), postStoryMission: e.target.value },
                          }))
                        }
                        placeholder={`Plain text shown to parents after the episode in ${label}. Leave empty to use the master story value.`}
                        className="mt-1 flex min-h-[72px] w-full rounded-lg border border-input bg-background px-3 py-2 text-sm"
                        maxLength={ADMIN_LIBRARY_POST_MISSION_MAX_CHARS}
                      />
                    </div>
                    <div>
                      <Label htmlFor={`tl-resource-${code}`}>Post-episode resource URL ({label})</Label>
                      <Input
                        id={`tl-resource-${code}`}
                        value={entry.postStoryResourceUrl}
                        onChange={(e) =>
                          setTranslationContentEntries((p) => ({
                            ...p,
                            [code]: { ...(p[code] ?? emptyLibraryTranslationTab()), postStoryResourceUrl: e.target.value },
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

          {workflowStep === 2 && (
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-base font-semibold">Cover</CardTitle>
                <p className="text-sm text-muted-foreground">
                  After scripts are synced on the previous step, add the poster. AI-generated or paste a URL — same controls as
                  before, moved here so cover comes <strong>after</strong> language sync.
                </p>
              </CardHeader>
              <CardContent className="space-y-3">
                <Input
                  value={form.coverImageUrl ?? ""}
                  onChange={(e) => setForm((f) => (f ? { ...f, coverImageUrl: e.target.value || null } : f))}
                  placeholder="URL or generate below"
                  className="rounded-lg h-9 text-sm"
                />
                <div className="space-y-1.5">
                  <Label htmlFor="cover-custom-prompt-main" className="text-xs font-medium">
                    Custom cover instructions (optional)
                  </Label>
                  <textarea
                    id="cover-custom-prompt-main"
                    className="w-full min-h-[88px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                    placeholder="e.g. Warmer evening light; show the river in the background; keep characters younger…"
                    value={coverCustomPrompt}
                    onChange={(e) => setCoverCustomPrompt(e.target.value.slice(0, 8000))}
                    disabled={coverGenerating || !id}
                    maxLength={8000}
                  />
                  <p className="text-xs text-muted-foreground">
                    Standard Tamixa cover template is always applied first; your text is appended. {coverCustomPrompt.length}
                    /8000 characters.
                  </p>
                </div>
                <div className="flex gap-2 flex-wrap">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    disabled={coverGenerating || !id}
                    onClick={async () => {
                      if (!id) return;
                      setCoverGenerating(true);
                      try {
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
                    <ImagePlus className="h-3.5 w-3.5 mr-1" />
                    {coverGenerating ? "Generating…" : "Generate cover"}
                  </Button>
                </div>
                {(form.coverImageUrl?.trim() || coverVideoUrl) && (
                  <div className="space-y-2 pt-1">
                    {coverVideoUrl?.trim() && (
                      <div className="relative aspect-video max-w-xl rounded-lg border bg-muted/30 overflow-hidden">
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
                      <div className="relative aspect-video max-w-xl rounded-lg border bg-muted/30 overflow-hidden">
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
          )}

          {workflowStep === 3 && (
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-base font-semibold">Save &amp; submit for review</CardTitle>
                <p className="text-sm text-muted-foreground">
                  When content, languages, and cover are ready, save a draft or send the story to the review queue. Use the
                  actions in the right column.
                </p>
              </CardHeader>
              <CardContent className="space-y-3 text-sm text-muted-foreground">
                <ul className="list-disc space-y-1 pl-5">
                  <li>
                    <strong>Save draft</strong> keeps the story in your library without sending it to review.
                  </li>
                  <li>
                    <strong>Submit for review</strong> moves it to <strong>Story for review</strong> for an approver.
                  </li>
                  <li>Wait for any running pipeline to finish before submitting (or use Refresh content first).</li>
                </ul>
              </CardContent>
            </Card>
          )}

          {workflowStep === 4 && (
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-base font-semibold">Story for review</CardTitle>
                <p className="text-sm text-muted-foreground">
                  Review this story here, or use the <strong>Story for review</strong> tab when you are working through many
                  titles at once.
                </p>
              </CardHeader>
              <CardContent className="space-y-4">
                {id != null && !Number.isNaN(id) ? (
                  <StoryReviewStepPanel
                    storyId={id}
                    onGoToSubmitStep={() => setWorkflowStep(3)}
                    onStoryRefresh={() => void loadStoryContent(id)}
                  />
                ) : null}
                <div className="flex flex-wrap gap-2 border-t border-border pt-4">
                  <Button variant="outline" size="sm" asChild>
                    <Link href="/dashboard/stories/approve">
                      Open full Review tab <ExternalLink className="h-3.5 w-3.5 ml-1 opacity-70" />
                    </Link>
                  </Button>
                </div>
                <p className="text-xs text-muted-foreground">
                  After approval, use the <strong>Narration</strong> step or the Narration tab → <strong>Generate audio</strong> unless your backend has <code className="text-xs">AUTO_TTS_ON_APPROVE=true</code>.
                </p>
              </CardContent>
            </Card>
          )}

          {workflowStep === 5 && (
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-base font-semibold">Narration (audio)</CardTitle>
                <p className="text-sm text-muted-foreground">
                  Generate or monitor TTS for <strong>this story</strong> below, or open the <strong>Narration</strong> tab for
                  the full approved queue.
                </p>
              </CardHeader>
              <CardContent className="space-y-4">
                {id != null && !Number.isNaN(id) ? (
                  <StoryNarrationStepPanel
                    storyId={id}
                    onGoToReviewStep={() => setWorkflowStep(4)}
                    onStoryRefresh={() => void loadStoryContent(id)}
                  />
                ) : null}
                <div className="flex flex-wrap gap-2 border-t border-border pt-4">
                  <Button variant="outline" size="sm" asChild>
                    <Link href="/dashboard/stories/to-speech">
                      Open full Narration tab <ExternalLink className="h-3.5 w-3.5 ml-1 opacity-70" />
                    </Link>
                  </Button>
                </div>
              </CardContent>
            </Card>
          )}
        </div>

        <div className="space-y-6">
          {workflowStep === 1 && (
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold">TTS script &amp; language sync</CardTitle>
              <p className="text-xs text-muted-foreground">
                One step: Tamixa conversion (master + other languages in the response), save draft, then server pipeline for all configured languages. No MP3s until Narration after approval.
              </p>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="space-y-1.5">
                <Label htmlFor="regenerate-custom-prompt" className="text-xs font-medium">
                  Custom instructions (optional)
                </Label>
                <textarea
                  id="regenerate-custom-prompt"
                  className="w-full min-h-[88px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  placeholder="e.g. Emphasize dialogue between the two siblings; keep the festival scene longer; soften the scare in act two…"
                  value={regenerateCustomPrompt}
                  onChange={(e) => setRegenerateCustomPrompt(e.target.value.slice(0, 8000))}
                  disabled={regenerateBusy || regenerateBlockedForRole || scriptSyncInProgress}
                  maxLength={8000}
                />
                <p className="text-xs text-muted-foreground">
                  Standard Tamixa TTS conversion template is always applied first; your text is appended so the model can steer tone,
                  pacing, or emphasis without replacing safety and JSON rules. {regenerateCustomPrompt.length}/8000 characters.
                </p>
              </div>
              <Button
                type="button"
                variant="secondary"
                size="default"
                className="w-full"
                disabled={
                  !id ||
                  regenerateBusy ||
                  submitting ||
                  sourceLanguageLoading ||
                  !form?.content?.trim() ||
                  regenerateBlockedForRole
                }
                title={
                  regenerateBlockedForRole
                    ? "Locked until Super Admin approves regenerate unlock"
                    : regenerateServerRunning
                      ? "Regenerate is already running for this story"
                      : storyPipelineRunning
                      ? "Pipeline already running for this story"
                      : !form?.content?.trim()
                        ? "Add story content first"
                        : inReviewQueue
                          ? "Saves as draft, then rebuilds scripts (story was in review)"
                          : "Runs Tamixa TTS-style prompt, saves draft, rebuilds all pipeline languages"
                }
                onClick={() => void handleRegenerateAndSyncScripts()}
              >
                <Sparkles className={cn("h-4 w-4 mr-2", scriptSyncInProgress && "animate-pulse")} />
                {scriptSyncInProgress ? "Working…" : "Regenerate & sync all languages"}
              </Button>
              {regenerateServerRunning && (
                <p className="text-xs text-amber-700 dark:text-amber-300">
                  Regenerate is running on the server. The button will re-enable automatically when it completes.
                </p>
              )}
              {!regenerateServerRunning && storyPipelineRunning && (
                <p className="text-xs text-amber-700 dark:text-amber-300">
                  Pipeline is still processing this story. Wait until it becomes idle to run regenerate again.
                </p>
              )}
              <p className="text-xs text-muted-foreground">
                Status:{" "}
                <strong>
                  {regenerateServerRunning ? "Regenerate running" : storyPipelineRunning ? "Pipeline running" : "Idle"}
                </strong>
                {regenerateStatusLastCheckedAt
                  ? ` • Last checked ${regenerateStatusLastCheckedAt.toLocaleTimeString()}`
                  : ""}
                {regenerateLastCompletedAt
                  ? ` • Last completed ${regenerateLastCompletedAt.toLocaleTimeString()}`
                  : ""}
                {regenerateStatusUnavailable
                  ? " • Regenerate status could not be fetched (network or server). Idle / running below reflects this editor session until the next successful poll—refresh or retry."
                  : ""}
              </p>
              <p className="text-xs text-muted-foreground">
                To fix copy without the LLM, edit fields and use <strong>Save draft</strong> or <strong>Move to draft</strong>, then{" "}
                <strong>Submit for review</strong> when ready; use <strong>Refresh content</strong> after any background pipeline run.
              </p>
            </CardContent>
          </Card>
          )}

          {workflowStep === 0 && (
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold">AI &amp; unlock</CardTitle>
              <p className="text-xs text-muted-foreground">
                Cover art is on the <strong>Cover</strong> step (after sync). Tamixa script conversion is on{" "}
                <strong>All languages</strong>: <strong>Regenerate &amp; sync all languages</strong>.
              </p>
            </CardHeader>
            <CardContent className="space-y-3">
              {regenerateBlockedForRole && (
                <p className="text-xs text-amber-700 dark:text-amber-200/90">
                  Regenerate with prompt is restricted until a Super Admin approves unlock.
                  {regenerateGate.unlockRequestedAt ? " Unlock request sent." : ""}
                </p>
              )}
              {!regenerateBlockedForRole && regenerateGate.locked && elevatedAdmin && (
                <p className="text-xs text-muted-foreground">
                  Super Admin: this story was machine-translated; content managers are gated unless you approve unlock below.
                </p>
              )}
              {regenerateBlockedForRole && (
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  className="w-full"
                  disabled={!id || unlockRequestSubmitting || !!regenerateGate.unlockRequestedAt}
                  onClick={async () => {
                    if (!id) return;
                    setUnlockRequestSubmitting(true);
                    try {
                      await api.admin.requestRegeneratePromptUnlock(id);
                      setRegenerateGate((g) => ({ ...g, unlockRequestedAt: new Date().toISOString() }));
                      showSuccess("Request sent", "A Super Admin can approve from this same page.");
                    } catch (e) {
                      showError("Request failed", e instanceof Error ? e.message : "Could not record request.");
                    } finally {
                      setUnlockRequestSubmitting(false);
                    }
                  }}
                >
                  {unlockRequestSubmitting ? "Sending…" : regenerateGate.unlockRequestedAt ? "Unlock requested" : "Request regenerate unlock"}
                </Button>
              )}
              {elevatedAdmin && regenerateGate.locked && !regenerateGate.lockApproved && (
                <Button
                  type="button"
                  variant="default"
                  size="sm"
                  className="w-full"
                  disabled={!id || approveUnlockSubmitting}
                  onClick={async () => {
                    if (!id) return;
                    setApproveUnlockSubmitting(true);
                    try {
                      await api.admin.approveRegeneratePromptUnlock(id);
                      setRegenerateGate({ locked: true, lockApproved: true, unlockRequestedAt: null });
                      showSuccess("Approved", "Content managers can use Regenerate & sync all languages again.");
                    } catch (e) {
                      showError("Approve failed", e instanceof Error ? e.message : "Could not approve.");
                    } finally {
                      setApproveUnlockSubmitting(false);
                    }
                  }}
                >
                  {approveUnlockSubmitting ? "Approving…" : "Approve regenerate for content managers"}
                </Button>
              )}
              <p className="text-xs text-muted-foreground">
                Unlock only affects who may run the combined regenerate + pipeline action on the <strong>All languages</strong> step.
              </p>
            </CardContent>
          </Card>
          )}

          {workflowStep === 3 && (
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold">Save &amp; submit</CardTitle>
            </CardHeader>
            <CardContent className="flex flex-col gap-2">
              <Button onClick={() => handleSubmit(false)} variant="outline" size="default" className="w-full" disabled={workflowBusy}>
                <Save className="h-4 w-4 mr-2" /> {inReviewQueue ? "Move to draft" : "Save draft"}
              </Button>
              <Button
                onClick={() => handleSubmit(true)}
                size="default"
                className="w-full"
                disabled={
                  workflowBusy ||
                  storyPipelineRunning ||
                  !canSubmitForReview ||
                  simulatorSubmitBlockedByAudio
                }
                title={
                  storyPipelineRunning
                    ? "Pipeline queued or running for this story"
                    : simulatorSubmitBlockedByAudio
                      ? "Generate audio for all simulator segments first"
                    : inReviewQueue
                      ? "Already in review queue. Move to draft after edits, then submit again."
                      : !form?.content?.trim()
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
          )}

          {workflowStep === 4 && (
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold">Bulk queue</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              <Button variant="outline" className="w-full" size="sm" asChild>
                <Link href="/dashboard/stories/approve">
                  All stories — Review <ExternalLink className="h-3.5 w-3.5 ml-1 opacity-70" />
                </Link>
              </Button>
              <p className="text-xs text-muted-foreground">Use when you are clearing the whole review list, not only this title.</p>
            </CardContent>
          </Card>
          )}

          {workflowStep === 5 && (
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold">Bulk queue</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              <Button variant="outline" className="w-full" size="sm" asChild>
                <Link href="/dashboard/stories/to-speech">
                  All stories — Narration <ExternalLink className="h-3.5 w-3.5 ml-1 opacity-70" />
                </Link>
              </Button>
              <p className="text-xs text-muted-foreground">Use when batching audio work across many approved stories.</p>
            </CardContent>
          </Card>
          )}
        </div>
      </div>

      <StoryWorkflowStepFooter
        currentStep={workflowStep}
        totalSteps={WORKFLOW_STEP_COUNT}
        onBack={() => setWorkflowStep(workflowStep - 1)}
        onNext={goNextWorkflowStep}
        disableNext={blockNextStepForPipeline}
        nextLabel={
          workflowStep === 0
            ? "All languages"
            : workflowStep === 1
              ? "Cover"
              : workflowStep === 2
                ? "Submit"
                : workflowStep === 3
                  ? "Review"
                  : "Next step"
        }
      />
    </div>
  );
}
