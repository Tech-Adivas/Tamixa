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
import { ArrowLeft, Save, ImagePlus, Sparkles, RefreshCw, Braces, LayoutTemplate } from "lucide-react";
import { cn, parseJsonStoryContent, resolveLibraryStoryEditorBody } from "@/lib/utils";
import {
  REGENERATE_THEN_TRANSLATIONS_HELP,
  getLibraryStoryMasterScriptContentError,
  canSubmitLibraryStoryForReview,
  adminStoryEmotionModeLabel,
  isLibraryStoryPipelineActivelyRunning,
} from "@/lib/library-story-workflow";
import { lintInteractiveGraphJson } from "@/lib/interactive-graph-lint";
import { outlineInteractiveGraphJson } from "@/lib/interactive-graph-outline";
import {
  DIGITAL_SAFETY_INTERACTIVE_GRAPH_TEMPLATE,
  DIGITAL_SAFETY_SIMULATOR_POST_MISSION,
  DIGITAL_SAFETY_SIMULATOR_THEME,
  getDefaultDecisionJournalUrl,
} from "@/lib/edu-simulator-template";
import { validateInteractiveStoryCategory } from "@/lib/story-interactive-conventions";

const AUTOSAVE_KEY = "tamixa_story_draft";
const AUTOSAVE_DEBOUNCE_MS = 2000;

const SOURCE_LANGUAGES = [
  { code: "ta", label: "Tamil (recommended)" },
  { code: "en", label: "English" },
  { code: "hi", label: "Hindi" },
  { code: "te", label: "Telugu" },
  { code: "kn", label: "Kannada" },
  { code: "ml", label: "Malayalam" },
] as const;

const TAB_LANGUAGES = [
  { code: "ta", label: "Tamil" },
  { code: "en", label: "English" },
  { code: "hi", label: "Hindi" },
  { code: "te", label: "Telugu" },
  { code: "kn", label: "Kannada" },
  { code: "ml", label: "Malayalam" },
] as const;

/** Survives refresh so Regenerate / Cover keep using UPDATE instead of CREATE (avoids duplicate title). */
const ADMIN_NEW_STORY_SESSION_ID_KEY = "tamixa_admin_new_library_story_id";

async function loadTranslationTabEntries(
  storyId: number,
  masterLang: string
): Promise<{
  tabLangs: { code: string; label: string }[];
  entries: Record<string, { content: string; title: string; moral: string }>;
}> {
  const master = masterLang.toLowerCase();
  const tabLangs = TAB_LANGUAGES.filter((l) => l.code !== master);
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
  const entries: Record<string, { content: string; title: string; moral: string }> = {};
  langResults.forEach(({ code, content, title, moral }) => {
    entries[code] = { content, title: title ?? "", moral: moral ?? "" };
  });
  return { tabLangs, entries };
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

  const [form, setForm] = useState<CreateLibraryStoryRequest>({
    title: "",
    content: "",
    theme: STORY_CATEGORIES[0],
    language: "ta",
    age: 5,
    childName: "Child",
    moral: "",
    status: "DRAFT",
    emotionMode: "CALM",
    parentDiscussionPrompts: undefined,
    parentContentNote: null,
    speakAlongPrompt: null,
    interactiveGraph: "",
    postStoryMission: "",
    postStoryResourceUrl: "",
  });
  /** Set after first server create (via Regenerate, optional Save, Cover, or Submit). */
  const [savedStoryId, setSavedStoryId] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [coverCustomPrompt, setCoverCustomPrompt] = useState("");
  const [regenerateCustomPrompt, setRegenerateCustomPrompt] = useState("");
  const [coverGenerating, setCoverGenerating] = useState(false);
  const [coverRefreshKey, setCoverRefreshKey] = useState(0);
  const [coverVideoUrl, setCoverVideoUrl] = useState<string | null>(null);
  const [translationContentEntries, setTranslationContentEntries] = useState<
    Record<string, { content: string; title: string; moral: string }>
  >({});
  /** Active tab for “Other languages” (must not equal master `form.language`). */
  const [activeLangTab, setActiveLangTab] = useState<string>("en");
  const [translationsRefreshing, setTranslationsRefreshing] = useState(false);
  const [scriptSyncInProgress, setScriptSyncInProgress] = useState(false);
  const [regenerateServerRunning, setRegenerateServerRunning] = useState(false);
  const [storyPipelineRunning, setStoryPipelineRunning] = useState(false);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const autosaveRef = useRef<ReturnType<typeof setTimeout>>();
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
          const sourceLang = (story.language ?? "ta").toLowerCase();
          setSavedStoryId(sid);
          setForm({
            title: (story.title ?? resolved.title ?? "")?.trim() || "",
            content: resolved.content ?? "",
            theme: (resolved.theme ?? story.theme) as string,
            language: sourceLang,
            age: story.age,
            childName: story.childName ?? "Child",
            moral: (story.moral ?? resolved.moral ?? "")?.trim() || "",
            status: (story.status as CreateLibraryStoryRequest["status"]) ?? "DRAFT",
            coverImageUrl: story.coverImageUrl ?? "",
            emotionMode: story.emotionMode ?? "CALM",
            parentDiscussionPrompts: story.parentDiscussionPrompts?.length
              ? [...story.parentDiscussionPrompts]
              : undefined,
            parentContentNote: story.parentContentNote ?? null,
            speakAlongPrompt: story.speakAlongPrompt ?? null,
            interactiveGraph: (() => {
              const g = story.interactiveGraph;
              if (g == null) return "";
              if (typeof g === "string") return g;
              try {
                return JSON.stringify(g, null, 2);
              } catch {
                return "";
              }
            })(),
            postStoryMission: story.postStoryMission?.trim() ?? "",
            postStoryResourceUrl: story.postStoryResourceUrl?.trim() ?? "",
          });
          setCoverVideoUrl(story.coverVideoUrl ?? null);
          const { tabLangs, entries } = await loadTranslationTabEntries(sid, sourceLang);
          if (cancelled) return;
          setTranslationContentEntries(entries);
          const withText = tabLangs.find((l) => entries[l.code]?.content?.trim());
          setActiveLangTab(withText?.code ?? tabLangs[0]?.code ?? "en");
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
            emotionMode: parsed.emotionMode || "CALM",
          }));
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
      localStorage.setItem(AUTOSAVE_KEY, JSON.stringify(form));
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
    const master = (form.language ?? "ta").toLowerCase();
    if (activeLangTab === master) {
      const next = TAB_LANGUAGES.find((l) => l.code !== master)?.code ?? "en";
      setActiveLangTab(next);
    }
  }, [form.language, activeLangTab]);

  const refreshTranslationTabsFromServer = useCallback(
    async (storyId: number) => {
      const master = (form.language ?? "ta").toLowerCase();
      setTranslationsRefreshing(true);
      try {
        const { tabLangs, entries } = await loadTranslationTabEntries(storyId, master);
        setTranslationContentEntries(entries);
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
    [form.language]
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
    if (form.title?.trim() && form.title.length > 255)
      errs.title = "Title must be 255 characters or less";
    const ig = form.interactiveGraph?.trim();
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
      }
    }
    setValidationErrors(errs);
    return Object.keys(errs).length === 0;
  }, [form, wordCount]);

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
      const maxRounds = 90;
      let sawBusy = false;
      for (let round = 0; round < maxRounds; round++) {
        await new Promise((r) => setTimeout(r, round === 0 ? 6000 : 4000));
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

  const buildPersistPayload = useCallback(
    (
      next: CreateLibraryStoryRequest,
      status: "DRAFT" | "PUBLISHED",
      translationEntries?: Record<string, { content: string; title: string; moral: string }>
    ) => {
      const entries = translationEntries ?? translationContentEntries;
      const translationPayload = buildTranslationContentPayload(entries);
      return {
        ...next,
        title: next.title?.trim() || null,
        moral: next.moral?.trim() || null,
        status,
        coverImageUrl: next.coverImageUrl?.trim() || null,
        coverVideoUrl: coverVideoUrl ?? null,
        regenerateNarration: false,
        translationContentEntries: translationPayload,
        parentContentNote: next.parentContentNote?.trim() || null,
        speakAlongPrompt: next.speakAlongPrompt?.trim() || null,
        parentDiscussionPrompts: next.parentDiscussionPrompts?.length ? next.parentDiscussionPrompts : null,
        interactiveGraph: next.interactiveGraph?.trim() ? next.interactiveGraph.trim() : null,
        postStoryMission: next.postStoryMission?.trim() || null,
        postStoryResourceUrl: next.postStoryResourceUrl?.trim() || null,
      };
    },
    [translationContentEntries, coverVideoUrl]
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
    setForm({
      ...form,
      theme: DIGITAL_SAFETY_SIMULATOR_THEME,
      interactiveGraph: DIGITAL_SAFETY_INTERACTIVE_GRAPH_TEMPLATE,
      postStoryMission: form.postStoryMission?.trim()
        ? form.postStoryMission
        : DIGITAL_SAFETY_SIMULATOR_POST_MISSION,
      postStoryResourceUrl: form.postStoryResourceUrl?.trim() ? form.postStoryResourceUrl : journalUrl || "",
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
      setForm({ ...form, interactiveGraph: JSON.stringify(obj, null, 2) });
    } catch (e) {
      showError("Invalid JSON", e instanceof Error ? e.message : "Could not parse JSON.");
    }
  };

  /**
   * Ensures a library row exists (CREATE once, then UPDATE). Survives refresh via sessionStorage.
   * If CREATE fails with duplicate title, adopts the existing story id (same as backend case-insensitive title check).
   */
  const ensureDraftOnServer = useCallback(async (): Promise<number> => {
    if (savedStoryId != null) return savedStoryId;
    if (ensureDraftPromiseRef.current) return ensureDraftPromiseRef.current;

    const run = (async (): Promise<number> => {
      try {
        const created = await api.admin.createLibraryStory({
          ...form,
          title: form.title?.trim() || null,
          moral: form.moral?.trim() || null,
          status: "DRAFT",
          coverImageUrl: form.coverImageUrl?.trim() || null,
          coverVideoUrl: coverVideoUrl ?? null,
          parentContentNote: form.parentContentNote?.trim() || null,
          speakAlongPrompt: form.speakAlongPrompt?.trim() || null,
          parentDiscussionPrompts: form.parentDiscussionPrompts?.length ? form.parentDiscussionPrompts : null,
        });
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
    coverVideoUrl,
    refreshStoryPipelineStatus,
    refreshRegenerateStatus,
    refreshTranslationTabsFromServer,
    showSuccess,
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
          const created = await api.admin.createLibraryStory({
            ...form,
            title: form.title?.trim() || null,
            moral: form.moral?.trim() || null,
            status: "PUBLISHED",
            coverImageUrl: form.coverImageUrl?.trim() || null,
            coverVideoUrl: coverVideoUrl ?? null,
            parentContentNote: form.parentContentNote?.trim() || null,
            speakAlongPrompt: form.speakAlongPrompt?.trim() || null,
            parentDiscussionPrompts: form.parentDiscussionPrompts?.length ? form.parentDiscussionPrompts : null,
          });
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
        form.language ?? "ta",
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
            nextEntries[lang] = {
              content: (entry.content ?? "").trim(),
              title: (entry.title ?? "").trim(),
              moral: (entry.moral ?? "").trim(),
            };
          }
        }
      }
      await persistDraftWithRetry(storyId, buildPersistPayload(nextForm, "DRAFT", nextEntries));
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
    SOURCE_LANGUAGES.find((l) => l.code === code)?.label?.replace(/ \(recommended\)/, "") ?? code;

  const canSubmitForReview = canSubmitLibraryStoryForReview(form.status, !!form.content?.trim());

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
    setForm({
      title: "",
      content: "",
      theme: STORY_CATEGORIES[0],
      language: "ta",
      age: 5,
      childName: "Child",
      moral: "",
      status: "DRAFT",
      emotionMode: "CALM",
      parentDiscussionPrompts: undefined,
      parentContentNote: null,
      speakAlongPrompt: null,
    });
    setActiveLangTab("en");
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
              {MIN_WORD_COUNT} words · Master: {langLabel(form.language ?? "ta")}
              {form.language !== "en"
                ? ` · ${langLabel(form.language ?? "ta")} script rules apply when master is not English`
                : ""}
            </p>
          </div>
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
                  </>
                )}
              </p>
            </CardHeader>
            <CardContent className="space-y-5 pt-6">
              <div>
                <Label htmlFor="sourceLang">Language *</Label>
                <Select
                  value={form.language ?? "ta"}
                  disabled={savedStoryId != null && regenerateBusy}
                  onValueChange={(v) => setForm((f) => ({ ...f, language: v }))}
                >
                  <SelectTrigger id="sourceLang" className="mt-1 rounded-xl">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {SOURCE_LANGUAGES.map((l) => (
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
                <Label htmlFor="title">Title ({langLabel(form.language ?? "ta")}) *</Label>
                <Input
                  id="title"
                  value={form.title ?? ""}
                  onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                  placeholder="e.g. அறிவுள்ள காகம்"
                  className="mt-1 rounded-xl"
                  dir="ltr"
                />
                {validationErrors.title && (
                  <p className="text-sm text-destructive mt-1">{validationErrors.title}</p>
                )}
              </div>
              <div>
                <Label htmlFor="theme">Category *</Label>
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
                <Label htmlFor="content">Story text ({langLabel(form.language ?? "ta")}) *</Label>
                <textarea
                  id="content"
                  value={form.content}
                  onChange={(e) => setForm((f) => ({ ...f, content: e.target.value }))}
                  placeholder={`Enter full story text in ${langLabel(form.language ?? "ta")}…`}
                  className="mt-1 flex min-h-[280px] w-full rounded-xl border-2 border-input bg-background px-4 py-3 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  dir="ltr"
                />
                <div className="flex justify-between mt-1">
                  <span
                    className={cn("text-xs", isValidWordCount ? "text-muted-foreground" : "text-destructive")}
                  >
                    {wordCount} / {MIN_WORD_COUNT} words
                  </span>
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
              <div className="rounded-xl border border-border/80 bg-muted/10 p-4 space-y-3">
                <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <p className="text-sm font-semibold">Interactive episode (EduStory pilot)</p>
                    <p className="text-xs text-muted-foreground mt-1 max-w-prose">
                      Same as <strong>Edit</strong>: valid JSON with{" "}
                      <code className="rounded bg-muted px-1">startSegmentId</code> and{" "}
                      <code className="rounded bg-muted px-1">segments</code> (each segment:{" "}
                      <code className="rounded bg-muted px-1">audioUrl</code>, optional{" "}
                      <code className="rounded bg-muted px-1">choices</code>). Leave empty for linear playback only.
                    </p>
                  </div>
                  <div className="flex flex-wrap gap-2 shrink-0">
                    <Button type="button" variant="secondary" size="sm" onClick={applyDigitalSafetyTemplate}>
                      <LayoutTemplate className="h-4 w-4 mr-1.5" />
                      Digital Safety template
                    </Button>
                    <Button type="button" variant="outline" size="sm" onClick={formatInteractiveGraphField}>
                      <Braces className="h-4 w-4 mr-1.5" />
                      Format JSON
                    </Button>
                    <Button type="button" variant="ghost" size="sm" asChild>
                      <Link href="/dashboard/edu-simulator-analytics">Choice analytics</Link>
                    </Button>
                  </div>
                </div>
                <div>
                  <Label htmlFor="interactiveGraph">Interactive graph (JSON)</Label>
                  <textarea
                    id="interactiveGraph"
                    value={form.interactiveGraph ?? ""}
                    onChange={(e) => setForm((f) => ({ ...f, interactiveGraph: e.target.value || "" }))}
                    className="mt-1 flex min-h-[240px] w-full rounded-xl border-2 border-input bg-background px-3 py-2 text-sm font-mono text-xs"
                    spellCheck={false}
                  />
                  {validationErrors.interactiveGraph ? (
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
                  value={form.childName ?? "Child"}
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
                      Text loaded from the server after <strong>Regenerate &amp; sync</strong> (or when the pipeline
                      finishes). Edit here, then use <strong>Update draft</strong> to save tab changes.
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
                  {TAB_LANGUAGES.filter((l) => l.code !== form.language).map(({ code, label }) => (
                    <button
                      key={code}
                      type="button"
                      onClick={() => setActiveLangTab(code)}
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
                {TAB_LANGUAGES.filter((l) => l.code !== form.language).map(({ code, label }) => {
                  if (activeLangTab !== code) return null;
                  const entry = translationContentEntries[code] ?? { content: "", title: "", moral: "" };
                  return (
                    <div key={code} className="space-y-3">
                      <div>
                        <Label htmlFor={`new-tl-title-${code}`}>Title</Label>
                        <Input
                          id={`new-tl-title-${code}`}
                          value={entry.title}
                          onChange={(e) =>
                            setTranslationContentEntries((p) => ({
                              ...p,
                              [code]: { ...(p[code] ?? { content: "", title: "", moral: "" }), title: e.target.value },
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
                                ...(p[code] ?? { content: "", title: "", moral: "" }),
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
                              [code]: { ...(p[code] ?? { content: "", title: "", moral: "" }), moral: e.target.value },
                            }))
                          }
                          placeholder={`Moral in ${label}`}
                          className="mt-1 rounded-lg"
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
                disabled={submitting || !canSubmitForReview || (savedStoryId != null && storyPipelineRunning)}
                title={
                  storyPipelineRunning
                    ? "Wait for pipeline to finish"
                    : !canSubmitForReview
                      ? "Add story content first"
                      : undefined
                }
              >
                {submitting ? "Submitting…" : "Submit for review"}
              </Button>
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
