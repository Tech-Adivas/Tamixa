/**
 * Single source of truth for admin library story UX constants (aligned with backend limits where noted).
 * Pipeline language codes should match `app.translation-pipeline` target languages on the server.
 */
import type { CreateLibraryStoryRequest } from "@/types/api";
import { STORY_CATEGORIES } from "@/types/api";

/** Slot key for master `library_stories.interactive_graph` vs per-translation overlays on the edit page. */
export const INTERACTIVE_GRAPH_MASTER_LOCALE_KEY = "__master__";

const PIPELINE_LANG = [
  { code: "ta", tabLabel: "Tamil", sourceLabel: "Tamil (recommended)" },
  { code: "en", tabLabel: "English", sourceLabel: "English" },
  { code: "hi", tabLabel: "Hindi", sourceLabel: "Hindi" },
  { code: "te", tabLabel: "Telugu", sourceLabel: "Telugu" },
  { code: "kn", tabLabel: "Kannada", sourceLabel: "Kannada" },
  { code: "ml", tabLabel: "Malayalam", sourceLabel: "Malayalam" },
] as const;

export const LIBRARY_TAB_LANGUAGES: ReadonlyArray<{ readonly code: string; readonly label: string }> =
  PIPELINE_LANG.map(({ code, tabLabel }) => ({ code, label: tabLabel }));

export const LIBRARY_SOURCE_LANGUAGE_OPTIONS: ReadonlyArray<{ readonly code: string; readonly label: string }> =
  PIPELINE_LANG.map(({ code, sourceLabel }) => ({ code, label: sourceLabel }));

export const DEFAULT_LIBRARY_SOURCE_LANGUAGE = "ta";
/** English master: Latin script is accepted without Indic character checks. */
export const LIBRARY_ENGLISH_LANGUAGE_CODE = "en";
export const DEFAULT_LIBRARY_CHILD_NAME = "Child";
export const DEFAULT_LIBRARY_EMOTION_MODE = "CALM" as const;
export const DEFAULT_LIBRARY_AGE = 5;

/** First tab language that is not the default master (for “Other languages” default selection). */
export function defaultTranslationTabLanguage(masterCode: string): string {
  const m = masterCode.toLowerCase();
  const hit = LIBRARY_TAB_LANGUAGES.find((l) => l.code !== m);
  return hit?.code ?? "en";
}

/** Backend: title length; discussion prompts 10×400; parent note 4000; speak-along 500; mission 8k; resource URL 512. */
export const ADMIN_LIBRARY_TITLE_MAX_LENGTH = 255;
export const ADMIN_LIBRARY_DISCUSSION_PROMPT_MAX_CHARS = 400;
export const ADMIN_LIBRARY_DISCUSSION_PROMPTS_MAX = 10;
export const ADMIN_LIBRARY_PARENT_NOTE_MAX_CHARS = 4000;
export const ADMIN_LIBRARY_SPEAK_ALONG_MAX_CHARS = 500;
export const ADMIN_LIBRARY_POST_MISSION_MAX_CHARS = 8000;
export const ADMIN_LIBRARY_POST_RESOURCE_URL_MAX_CHARS = 512;

/** Polling after async translation / pipeline operations (single policy for new + edit). */
export const ADMIN_LIBRARY_PIPELINE_POLL_MAX_ROUNDS = 120;
export const ADMIN_LIBRARY_PIPELINE_POLL_FIRST_MS = 6000;
export const ADMIN_LIBRARY_PIPELINE_POLL_INTERVAL_MS = 4000;

export const ADMIN_LIBRARY_SEGMENT_ERROR_DETAIL_MAX_CHARS = 700;

export const AUTOSAVE_DEBOUNCE_MS = 2000;

export const ADMIN_STORY_DRAFT_AUTOSAVE_STORAGE_KEY = "tamixa_story_draft";
export const ADMIN_NEW_LIBRARY_STORY_SESSION_STORAGE_KEY = "tamixa_admin_new_library_story_id";
export const SEGMENT_STUDIO_PARENT_STORAGE_KEY = "tamixa_segment_audio_parent_id";

export function newEmptyLibraryStoryForm(): CreateLibraryStoryRequest {
  return {
    title: "",
    content: "",
    theme: STORY_CATEGORIES[0],
    language: DEFAULT_LIBRARY_SOURCE_LANGUAGE,
    age: DEFAULT_LIBRARY_AGE,
    childName: DEFAULT_LIBRARY_CHILD_NAME,
    moral: "",
    status: "DRAFT",
    emotionMode: DEFAULT_LIBRARY_EMOTION_MODE,
    parentDiscussionPrompts: undefined,
    parentContentNote: null,
    speakAlongPrompt: null,
    interactiveGraph: "",
    postStoryMission: "",
    postStoryResourceUrl: "",
  };
}

/** Per-language tab on New/Edit library story (non-master languages). */
export type LibraryTranslationTabFields = {
  content: string;
  title: string;
  moral: string;
  postStoryMission: string;
  postStoryResourceUrl: string;
};

export function emptyLibraryTranslationTab(): LibraryTranslationTabFields {
  return { content: "", title: "", moral: "", postStoryMission: "", postStoryResourceUrl: "" };
}

/**
 * Build `translationContentEntries` for create/update. Empty strings for post fields clear per-locale overrides on the server.
 */
export function buildLibraryTranslationContentPayload(
  entries: Record<string, LibraryTranslationTabFields>
): NonNullable<CreateLibraryStoryRequest["translationContentEntries"]> | undefined {
  const translationPayload: NonNullable<CreateLibraryStoryRequest["translationContentEntries"]> = {};
  Object.entries(entries).forEach(([lang, entry]) => {
    const content = entry?.content?.trim() ?? "";
    const title = entry?.title?.trim() ?? "";
    const moral = entry?.moral?.trim() ?? "";
    const mission = (entry?.postStoryMission ?? "").trim().slice(0, ADMIN_LIBRARY_POST_MISSION_MAX_CHARS);
    const resourceUrl = (entry?.postStoryResourceUrl ?? "").trim().slice(0, ADMIN_LIBRARY_POST_RESOURCE_URL_MAX_CHARS);
    if (!content && !title && !moral && !mission && !resourceUrl) return;
    translationPayload[lang] = {
      content: content || "",
      title: title || null,
      moral: moral || null,
      postStoryMission: mission.slice(0, ADMIN_LIBRARY_POST_MISSION_MAX_CHARS),
      postStoryResourceUrl: resourceUrl.slice(0, ADMIN_LIBRARY_POST_RESOURCE_URL_MAX_CHARS),
    };
  });
  return Object.keys(translationPayload).length > 0 ? translationPayload : undefined;
}
