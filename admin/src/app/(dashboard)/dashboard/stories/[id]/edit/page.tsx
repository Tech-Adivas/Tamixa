"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter, useParams, usePathname, useSearchParams } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import { api, getApiBaseUrl, reconcileAdminAuthCookie, refreshTokensIfNeeded } from "@/lib/api";
import type { CreateLibraryStoryRequest } from "@/types/api";
import {
  STORY_CATEGORIES,
  AGE_GROUPS,
  MIN_WORD_COUNT,
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
} from "@/lib/library-story-workflow";
import { parseJsonStoryContent, resolveLibraryStoryEditorBody } from "@/lib/utils";
import { ArrowLeft, Save, ImagePlus, Sparkles, RefreshCw, ExternalLink } from "lucide-react";
import { cn } from "@/lib/utils";
import {
  StoryWorkflowStepper,
  StoryWorkflowStepFooter,
  EDIT_LIBRARY_STORY_WORKFLOW_STEPS,
} from "@/components/story-workflow-stepper";
import { StoryReviewStepPanel, StoryNarrationStepPanel } from "@/components/story-workflow-contextual-panels";

/** Source languages for main story content. Tamil is pipeline source (recommended). */
const SOURCE_LANGUAGES = [
  { code: "ta", label: "Tamil (recommended)" },
  { code: "en", label: "English" },
  { code: "hi", label: "Hindi" },
  { code: "te", label: "Telugu" },
  { code: "kn", label: "Kannada" },
  { code: "ml", label: "Malayalam" },
] as const;

/** Tab languages = all except current master (same list without “recommended” label). */
const TAB_LANGUAGES = [
  { code: "ta", label: "Tamil" },
  { code: "en", label: "English" },
  { code: "hi", label: "Hindi" },
  { code: "te", label: "Telugu" },
  { code: "kn", label: "Kannada" },
  { code: "ml", label: "Malayalam" },
] as const;

function sourceLangLabel(code: string): string {
  return SOURCE_LANGUAGES.find((l) => l.code === code)?.label?.replace(/ \(recommended\)/, "") ?? code;
}

/** Build API payload for other-language tabs; include a row if story text, title, or moral is non-empty. */
function buildTranslationContentPayload(
  entries: Record<string, { content: string; title: string; moral: string }>
): Record<string, { content: string; title?: string | null; moral?: string | null }> | undefined {
  const translationPayload: Record<string, { content: string; title?: string | null; moral?: string | null }> = {};
  Object.entries(entries).forEach(([lang, entry]) => {
    const content = entry?.content?.trim() ?? "";
    const title = entry?.title?.trim() ?? "";
    const moral = entry?.moral?.trim() ?? "";
    if (!content && !title && !moral) return;
    translationPayload[lang] = {
      content: content || "",
      title: title || null,
      moral: moral || null,
    };
  });
  return Object.keys(translationPayload).length > 0 ? translationPayload : undefined;
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
    Record<string, { content: string; title: string; moral: string }>
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

  const elevatedAdmin = isElevatedStoryAdmin(user?.role);
  const regenerateBlockedForRole =
    regenerateGate.locked && !regenerateGate.lockApproved && !elevatedAdmin;

  const wordCount = form?.content
    ? form.content.split(/\s+/).filter((w) => w.trim()).length
    : 0;
  const isValidWordCount = wordCount >= MIN_WORD_COUNT;
  const inReviewQueue = isLibraryStoryInReviewQueue(form?.status);
  const workflowBusy =
    submitting || sourceLanguageLoading || scriptSyncInProgress || refreshingContent;
  const blockNextStepForPipeline = workflowStep === 1 && storyPipelineRunning;
  const regenerateBusy = scriptSyncInProgress || storyPipelineRunning || regenerateServerRunning;
  const canSubmitForReview = canSubmitLibraryStoryForReview(form?.status, !!form?.content?.trim());

  /** Fetch “other language” tabs for a given master/source language. */
  const fetchTranslationTabs = useCallback(
    async (storyId: number, masterLang: string) => {
      const tabLangs = TAB_LANGUAGES.filter((l) => l.code !== masterLang);
      const langResults = await Promise.all(
        tabLangs.map(({ code }) =>
          api.admin
            .getLibraryStory(storyId, code)
            .then((s) => {
              const raw = resolveLibraryStoryEditorBody(s ?? {});
              const parsed = parseJsonStoryContent(raw);
              return {
                code,
                content: parsed?.content ?? raw,
                title: (parsed?.title ?? s?.title ?? "")?.trim() || "",
                moral: (parsed?.moral ?? s?.moral ?? "")?.trim() || "",
              };
            })
            .catch(() => ({ code, content: "", title: "", moral: "" }))
        )
      );
      const next: Record<string, { content: string; title: string; moral: string }> = {};
      langResults.forEach(({ code, content, title, moral }) => {
        next[code] = { content, title: title ?? "", moral: moral ?? "" };
      });
      return { tabLangs, entries: next };
    },
    []
  );

  /** Load story and all language variants (pipeline-generated content). Use on mount and via "Refresh content" after pipeline completes. */
  const loadStoryContent = useCallback(
    async (storyId: number) => {
      const story = await api.admin.getLibraryStory(storyId);
      const sourceLang = (story.language ?? "ta").toLowerCase();
      const { tabLangs, entries } = await fetchTranslationTabs(storyId, sourceLang);
      const contentToEdit = resolveLibraryStoryEditorBody(story);
      const parsed = parseJsonStoryContent(contentToEdit);
      const resolved = parsed ?? { content: contentToEdit };
      setForm({
        title: (story.title ?? resolved.title ?? "")?.trim() || "",
        content: resolved.content,
        theme: resolved.theme ?? story.theme,
        language: sourceLang,
        age: story.age,
        childName: story.childName ?? "Child",
        moral: (story.moral ?? resolved.moral ?? "")?.trim() || "",
        status: story.status ?? "DRAFT",
        coverImageUrl: story.coverImageUrl ?? "",
        emotionMode: story.emotionMode ?? "CALM",
        parentDiscussionPrompts: story.parentDiscussionPrompts?.length
          ? [...story.parentDiscussionPrompts]
          : undefined,
        parentContentNote: story.parentContentNote ?? null,
        speakAlongPrompt: story.speakAlongPrompt ?? null,
      });
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
    },
    [fetchTranslationTabs]
  );

  /** Reload language tabs until pipeline no longer shows TRANSLATING/REWRITING/TTS (Generate translations is async). */
  const pollTranslationsAfterRebuild = useCallback(
    async (storyId: number) => {
      const maxRounds = 120;
      let sawLanguageWork = false;
      for (let round = 0; round < maxRounds; round++) {
        await new Promise((r) => setTimeout(r, round === 0 ? 6000 : 4000));
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
        const main = await api.admin.getLibraryStory(storyId, newLang);
        const { tabLangs, entries } = await fetchTranslationTabs(storyId, newLang);
        const contentToEdit = resolveLibraryStoryEditorBody(main);
        const parsed = parseJsonStoryContent(contentToEdit);
        const resolved = parsed ?? { content: contentToEdit };
        setForm((prev) =>
          prev
            ? {
                ...prev,
                language: newLang,
                title: (main.title ?? resolved.title ?? "")?.trim() || "",
                content: resolved.content,
                theme: resolved.theme ?? main.theme ?? prev.theme,
                moral: (main.moral ?? resolved.moral ?? "")?.trim() || "",
                coverImageUrl: main.coverImageUrl ?? prev.coverImageUrl ?? "",
                emotionMode: main.emotionMode ?? prev.emotionMode,
              }
            : prev
        );
        setCoverVideoUrl((v) => main.coverVideoUrl ?? v);
        setTranslationContentEntries(entries);
        setActiveLangTab(tabLangs[0]?.code ?? "en");
        setRegenerateGate({
          locked: !!main.regeneratePromptLocked,
          lockApproved: !!main.regeneratePromptLockApproved,
          unlockRequestedAt: main.regeneratePromptUnlockRequestedAt ?? null,
        });
        setRephraseSuggestion(null);
        setValidationErrors({});
        setParaphraseBefore(null);
        setParaphraseBeforeByLang(null);
        setShowParaphraseDiff(false);
      } catch (e) {
        showError(
          "Could not switch language",
          e instanceof Error ? e.message : "Failed to load this language. Try Refresh content."
        );
      } finally {
        setSourceLanguageLoading(false);
      }
    },
    [fetchTranslationTabs, showError]
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

  /** Tamil Unicode block U+0B80–U+0BFF */
  const hasTamilScript = (text: string) => /[\u0B80-\u0BFF]/.test(text);

  const validate = useCallback((): boolean => {
    if (!form) return false;
    const errs: Record<string, string> = {};
    if (!form.theme?.trim()) errs.theme = "Category is required";
    if (!form.content?.trim()) errs.content = "Story text is required";
    else if (wordCount < MIN_WORD_COUNT)
      errs.content = `Minimum ${MIN_WORD_COUNT} words required (current: ${wordCount})`;
    else if (form.language === "ta" && !hasTamilScript(form.content))
      errs.content = "Story content must contain Tamil script (தமிழ் characters). Use “Regenerate with prompt” to get Tamil output.";
    if (form.title?.trim() && form.title.length > 255)
      errs.title = "Title must be 255 characters or less";
    setValidationErrors(errs);
    return Object.keys(errs).length === 0;
  }, [form, wordCount]);

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
          translationContentEntries?: Record<
            string,
            { content: string; title?: string | null; moral?: string | null }
          >;
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
        form.language ?? "ta",
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
            nextEntries[lang] = {
              content: (entry.content ?? "").trim(),
              title: (entry.title ?? "").trim(),
              moral: (entry.moral ?? "").trim(),
            };
          }
        }
      }
      const translationPayload = buildTranslationContentPayload(nextEntries);
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
      const sourceLang = (form.language ?? "ta").toLowerCase();
      const expectedTargetLangs = TAB_LANGUAGES.map((l) => l.code).filter((code) => code !== sourceLang);
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
    submitInFlightRef.current = true;
    setSubmitting(true);
    try {
      const translationPayload = buildTranslationContentPayload(translationContentEntries);
      await api.admin.updateLibraryStory(id, {
        ...form,
        title: form.title?.trim() || null,
        moral: form.moral?.trim() || null,
        status: publish ? "PUBLISHED" : "DRAFT",
        coverImageUrl: form.coverImageUrl ?? null,
        coverVideoUrl: coverVideoUrl ?? null,
        regenerateNarration: false,
        translationContentEntries: translationPayload,
        parentContentNote: form.parentContentNote?.trim() || null,
        speakAlongPrompt: form.speakAlongPrompt?.trim() || null,
        parentDiscussionPrompts: form.parentDiscussionPrompts?.length
          ? form.parentDiscussionPrompts
          : null,
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
            Min {MIN_WORD_COUNT} words · Master language (saved on update): {sourceLangLabel(form.language ?? "ta")} · Tamil
            script required when master is Tamil
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
                Title, category, <strong>story text</strong> (translation/master row), and optional{" "}
                pipeline-managed TTS script for the <strong>master</strong> language.
                Use the next step for other languages, cover, and the
                translation pipeline.
              </p>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <Label htmlFor="sourceLang">Source language *</Label>
                <Select
                  value={form?.language ?? "ta"}
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
                    {SOURCE_LANGUAGES.map((l) => (
                      <SelectItem key={l.code} value={l.code}>
                        {l.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <p className="text-xs text-muted-foreground mt-1">
                  Changing this reloads the main fields and tabs from the server for that language. After editing, Save
                  or Submit so the library stores this choice as the story&rsquo;s master language.
                </p>
              </div>
              <div>
                <div className="flex items-center justify-between gap-2">
                  <Label htmlFor="title">Title ({sourceLangLabel(form.language ?? "ta")}) *</Label>
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
                <Label htmlFor="theme">Category *</Label>
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
              </div>

              <div>
                <Label htmlFor="content">Story text ({sourceLangLabel(form.language ?? "ta")}) *</Label>
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
                  placeholder={`Enter full story text in ${sourceLangLabel(form.language ?? "ta")}…`}
                  className="mt-2 flex min-h-[220px] w-full rounded-lg border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  dir="ltr"
                />
                <div className="flex justify-between mt-1">
                  <span className={cn("text-xs", isValidWordCount ? "text-muted-foreground" : "text-destructive")}>
                    {wordCount} / {MIN_WORD_COUNT} words
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

          {/* Metadata — moral, age, child name */}
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold">Metadata</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <Label htmlFor="moral">Moral (optional)</Label>
                <Input id="moral" value={form.moral ?? ""} onChange={(e) => setForm((f) => (f ? { ...f, moral: e.target.value } : f))} placeholder="e.g. Sharing brings joy" className="mt-1 rounded-lg" />
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
            </CardContent>
          </Card>
            </>
          )}

          {workflowStep === 1 && (
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base font-semibold">Other languages</CardTitle>
              <p className="text-sm text-muted-foreground">
                Edit per-language text if needed. Use <strong>Regenerate &amp; sync all languages</strong> in the right panel to run
                Tamixa TTS script conversion and rebuild conversational scripts for every pipeline language (no MP3s). After
                approval, open <strong>Narration</strong> and use <strong>Generate audio</strong>.
              </p>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex gap-1 flex-wrap">
                {TAB_LANGUAGES.filter((l) => l.code !== form.language).map(({ code, label }) => (
                  <button
                    key={code}
                    type="button"
                    onClick={() => setActiveLangTab(code)}
                    className={cn(
                      "rounded-lg px-3 py-1.5 text-sm font-medium transition-colors",
                      activeLangTab === code ? "bg-primary text-primary-foreground" : "bg-muted/50 text-muted-foreground hover:bg-muted"
                    )}
                  >
                    {label}
                  </button>
                ))}
              </div>
              {TAB_LANGUAGES.filter((l) => l.code !== form.language).map(({ code, label }) => {
                if (activeLangTab !== code) return null;
                const entry = translationContentEntries[code] ?? { content: "", title: "", moral: "" };
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
                      <Input id={`tl-title-${code}`} value={entry.title} onChange={(e) => setTranslationContentEntries((p) => ({ ...p, [code]: { ...(p[code] ?? { content: "", title: "", moral: "" }), title: e.target.value } }))} placeholder={`Title in ${label}`} className="mt-1 rounded-lg" />
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
                              ...(p[code] ?? { content: "", title: "", moral: "" }),
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
                      <Input id={`tl-moral-${code}`} value={entry.moral} onChange={(e) => setTranslationContentEntries((p) => ({ ...p, [code]: { ...(p[code] ?? { content: "", title: "", moral: "" }), moral: e.target.value } }))} placeholder={`Moral in ${label}`} className="mt-1 rounded-lg" />
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
                <CardTitle className="text-base font-semibold">Save &amp; submit for review</CardTitle>
                <p className="text-sm text-muted-foreground">
                  When you are happy with the master story, translations, and cover, save a draft or send the story to the
                  review queue. Use the actions in the right column.
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

          {workflowStep === 3 && (
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
                    onGoToSubmitStep={() => setWorkflowStep(2)}
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

          {workflowStep === 4 && (
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
                    onGoToReviewStep={() => setWorkflowStep(3)}
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

        <div className="space-y-6 lg:sticky lg:top-20 lg:self-start">
          {(workflowStep === 0 || workflowStep === 1) && (
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold">Cover</CardTitle>
              <p className="text-xs text-muted-foreground">AI-generated or paste URL</p>
            </CardHeader>
            <CardContent className="space-y-3">
              <Input
                value={form.coverImageUrl ?? ""}
                onChange={(e) => setForm((f) => (f ? { ...f, coverImageUrl: e.target.value || null } : f))}
                placeholder="URL or regenerate"
                className="rounded-lg h-9 text-sm"
              />
              <div className="space-y-1.5">
                <Label htmlFor="cover-custom-prompt" className="text-xs font-medium">
                  Custom cover instructions (optional)
                </Label>
                <textarea
                  id="cover-custom-prompt"
                  className="w-full min-h-[88px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  placeholder="e.g. Warmer evening light; show the river in the background; keep characters younger; more magical sparkles in the sky…"
                  value={coverCustomPrompt}
                  onChange={(e) => setCoverCustomPrompt(e.target.value.slice(0, 8000))}
                  disabled={coverGenerating || !id}
                  maxLength={8000}
                />
                <p className="text-xs text-muted-foreground">
                  Standard Tamixa cover template (story title + excerpt + style rules) is always applied first; your text is appended for
                  illustration and motion. {coverCustomPrompt.length}/8000 characters.
                </p>
              </div>
              <div className="flex gap-2 flex-wrap">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  className="flex-1 min-w-0"
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
                      setForm((f) => (f ? { ...f, coverImageUrl: updated?.coverImageUrl?.trim() ?? f.coverImageUrl ?? "" } : f));
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
                        <video key={coverRefreshKey} src={resolveCoverSrc(coverVideoUrl) ?? coverVideoUrl} className="w-full h-full object-cover" controls loop muted playsInline />
                      )}
                    </div>
                  )}
                  {form.coverImageUrl?.trim() && (
                    <div className="relative aspect-video rounded-lg border bg-muted/30 overflow-hidden">
                      <CoverImageWithFallback key={coverRefreshKey} src={`${resolveCoverSrc(form.coverImageUrl) ?? form.coverImageUrl}?t=${coverRefreshKey}`} />
                    </div>
                  )}
                </div>
              )}
            </CardContent>
          </Card>
          )}

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
                  ? " • Live regenerate status is temporarily unavailable; using best-effort UI state."
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
                Use <strong>Generate cover</strong> above for the poster. Tamixa TTS script conversion and language sync are on the{" "}
                <strong>Cover &amp; languages</strong> step: <strong>Regenerate &amp; sync all languages</strong>.
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
                Unlock only affects who may run the combined regenerate + pipeline action on the next workflow step.
              </p>
            </CardContent>
          </Card>
          )}

          {workflowStep === 2 && (
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold">Publish</CardTitle>
            </CardHeader>
            <CardContent className="flex flex-col gap-2">
              <Button onClick={() => handleSubmit(false)} variant="outline" size="default" className="w-full" disabled={workflowBusy}>
                <Save className="h-4 w-4 mr-2" /> {inReviewQueue ? "Move to draft" : "Save draft"}
              </Button>
              <Button
                onClick={() => handleSubmit(true)}
                size="default"
                className="w-full"
                disabled={workflowBusy || storyPipelineRunning || !canSubmitForReview}
                title={
                  storyPipelineRunning
                    ? "Pipeline queued or running for this story"
                    : inReviewQueue
                      ? "Already in review queue. Move to draft after edits, then submit again."
                      : !form?.content?.trim()
                        ? "Add story content first"
                        : undefined
                }
              >
                {submitting ? "Submitting…" : "Submit for review"}
              </Button>
            </CardContent>
          </Card>
          )}

          {workflowStep === 3 && (
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

          {workflowStep === 4 && (
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
        onNext={() => setWorkflowStep(workflowStep + 1)}
        disableNext={blockNextStepForPipeline}
      />
    </div>
  );
}
