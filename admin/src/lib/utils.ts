import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/** Parse comma-separated language codes from pipeline API (e.g. reviewedLanguages, reviewStaleLanguages). */
export function parsePipelineLanguageSet(raw?: string): Set<string> {
  if (typeof raw !== "string" || !raw.trim()) return new Set()
  return new Set(raw.split(",").map((s) => s.trim().toLowerCase()).filter(Boolean))
}

/** API error body shape (backend ErrorResponse + validation errors). */
type ApiErrorBody = {
  message?: string
  error?: string
  detail?: string
  errors?: Record<string, string>
}

/**
 * Returns a user-facing error message from an API response body or unknown error.
 * Includes validation errors (e.g. "message. Field X: Y") when present.
 */
export function getApiErrorMessage(error: unknown, fallback = "Something went wrong"): string {
  if (error instanceof Error && error.message) return error.message
  if (typeof error === "string") return error
  if (error && typeof error === "object" && "message" in error) {
    const body = error as ApiErrorBody
    const msg = body.message ?? body.error ?? body.detail ?? fallback
    if (body.errors && Object.keys(body.errors).length > 0) {
      const details = Object.entries(body.errors)
        .map(([field, text]) => `${field}: ${text}`)
        .join("; ")
      return `${msg}. ${details}`
    }
    return msg
  }
  return fallback
}

/**
 * Text to load in admin story/review editors for the **story text** field (not the narration script).
 * Prefers `sourceContent` when the API sends it; otherwise merged `content`, then narrated as last resort.
 */
export function resolveLibraryStoryEditorBody(story: {
  content?: string | null
  sourceContent?: string | null
  narratedContent?: string | null
  preferNarratedContentForEditor?: boolean
}): string {
  if (typeof story.sourceContent === "string") return story.sourceContent
  const body = story.content?.trim() ?? ""
  const narrated = story.narratedContent?.trim() ?? ""
  if (body) return body
  if (story.preferNarratedContentForEditor && narrated) return narrated
  return narrated || ""
}

/** When content looks like JSON (e.g. from prompt that returned structured output), parse and extract fields for form display. */
export function parseJsonStoryContent(
  raw: string
): { title?: string; content: string; moral?: string; theme?: string; category?: string } | null {
  const trimmed = raw.trim().replace(/^```json\s*/i, "").replace(/\s*```$/g, "").trim()
  if (!trimmed.startsWith("{")) return null
  try {
    const obj = JSON.parse(trimmed) as Record<string, unknown>
    const storyText = (obj.story_text ?? obj.storyText ?? "") as string
    if (!storyText?.trim()) return null
    return {
      title: (obj.title as string)?.trim() || undefined,
      content: storyText.trim(),
      moral: (obj.moral as string)?.trim() || undefined,
      theme: (obj.theme as string)?.trim() || undefined,
      category: (obj.category as string)?.trim() || undefined,
    }
  } catch {
    return null
  }
}
