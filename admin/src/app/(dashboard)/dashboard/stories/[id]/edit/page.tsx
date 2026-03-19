"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter, useParams } from "next/navigation";
import Link from "next/link";
import { api, getApiBaseUrl } from "@/lib/api";
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
import { usePipelineActive } from "@/contexts/pipeline-active-context";
import { parseJsonStoryContent } from "@/lib/utils";
import { ArrowLeft, Save, ImagePlus, ChevronDown, ChevronRight, Info, Sparkles, RefreshCw } from "lucide-react";
import { cn } from "@/lib/utils";

/** Source languages for main story content. Tamil is pipeline source (recommended). */
const SOURCE_LANGUAGES = [
  { code: "ta", label: "Tamil (recommended)" },
  { code: "en", label: "English" },
  { code: "hi", label: "Hindi" },
  { code: "te", label: "Telugu" },
  { code: "kn", label: "Kannada" },
  { code: "ml", label: "Malayalam" },
] as const;

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
    <img
      src={src}
      alt="Story cover"
      className="w-full h-full object-cover rounded-lg"
      onError={() => setErrored(true)}
    />
  );
}

export default function EditLibraryStoryPage() {
  const router = useRouter();
  const params = useParams();
  const id = params?.id ? Number(params.id) : null;
  const { showSuccess, showError } = useActionResult();
  const { isPipelineActive, refresh: refreshPipelineActive } = usePipelineActive();

  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState<CreateLibraryStoryRequest | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [coverGenerating, setCoverGenerating] = useState(false);
  const [coverRefreshKey, setCoverRefreshKey] = useState(0);
  const [coverVideoUrl, setCoverVideoUrl] = useState<string | null>(null);
  const [rephrasing, setRephrasing] = useState(false);
  const [rephraseSuggestion, setRephraseSuggestion] = useState<{
    suggestedTitle: string;
    suggestedContent: string;
  } | null>(null);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const [regenerating, setRegenerating] = useState(false);
  /** Per-language content for non-Tamil languages (optional). When set, sent as translationContentEntries so other languages are updated. */
  const [translationContentEntries, setTranslationContentEntries] = useState<
    Record<string, { content: string; title: string; moral: string }>
  >({});

  const [activeLangTab, setActiveLangTab] = useState<string>("en");
  const [refreshingContent, setRefreshingContent] = useState(false);

  const wordCount = form?.content
    ? form.content.split(/\s+/).filter((w) => w.trim()).length
    : 0;
  const isValidWordCount = wordCount >= MIN_WORD_COUNT;

  /** Tab languages = all except source (source is in main form). */
  const TAB_LANGUAGES = [
    { code: "ta", label: "Tamil" },
    { code: "en", label: "English" },
    { code: "hi", label: "Hindi" },
    { code: "te", label: "Telugu" },
    { code: "kn", label: "Kannada" },
    { code: "ml", label: "Malayalam" },
  ];

  /** Load story and all language variants (pipeline-generated content). Use on mount and via "Refresh content" after pipeline completes. */
  const loadStoryContent = useCallback(
    async (storyId: number) => {
      const story = await api.admin.getLibraryStory(storyId);
      const sourceLang = (story.language ?? "ta").toLowerCase();
      const tabLangs = TAB_LANGUAGES.filter((l) => l.code !== sourceLang);
      const langResults = await Promise.all(
        tabLangs.map(({ code }) =>
          api.admin
            .getLibraryStory(storyId, code)
            .then((s) => ({
              code,
              content: (s?.narratedContent?.trim() || s?.content) ?? "",
              title: s?.title ?? "",
              moral: s?.moral ?? "",
            }))
            .catch(() => ({ code, content: "", title: "", moral: "" }))
        )
      );
      const contentToEdit = (story.narratedContent?.trim() || story.content) ?? "";
      const parsed = parseJsonStoryContent(contentToEdit);
      const resolved = parsed ?? { content: contentToEdit };
      setForm({
        title: (story.title ?? resolved.title ?? "")?.trim() || "",
        content: resolved.content,
        theme: resolved.theme ?? story.theme,
        language: story.language ?? "ta",
        age: story.age,
        childName: story.childName ?? "Child",
        moral: (story.moral ?? resolved.moral ?? "")?.trim() || "",
        status: story.status ?? "DRAFT",
        coverImageUrl: story.coverImageUrl ?? "",
        emotionMode: story.emotionMode ?? "CALM",
      });
      setCoverVideoUrl(story.coverVideoUrl ?? null);
      const next: Record<string, { content: string; title: string; moral: string }> = {};
      langResults.forEach(({ code, content, title, moral }) => {
        next[code] = { content, title: title ?? "", moral: moral ?? "" };
      });
      setTranslationContentEntries(next);
      setActiveLangTab(tabLangs[0]?.code ?? "en");
    },
    []
  );

  useEffect(() => {
    if (!id || isNaN(id)) {
      showError("Invalid story ID", "The story ID is invalid or not found.");
      router.push("/dashboard/stories");
      return;
    }
    loadStoryContent(id)
      .catch((e) => {
        showError("Failed to load story", e instanceof Error ? e.message : "Unable to load the story. Please try again.");
        router.push("/dashboard/stories");
      })
      .finally(() => setLoading(false));
  }, [id, router, loadStoryContent]);

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

  const handleSubmit = async (publish: boolean) => {
    if (!form || !id) return;
    if (!validate()) {
      showError("Validation failed", "Please fix the validation errors before saving.");
      return;
    }
    setSubmitting(true);
    try {
      const translationPayload: Record<string, { content: string; title?: string | null; moral?: string | null }> = {};
      Object.entries(translationContentEntries).forEach(([lang, entry]) => {
        const content = entry?.content?.trim();
        if (content) {
          translationPayload[lang] = {
            content,
            title: entry.title?.trim() || null,
            moral: entry.moral?.trim() || null,
          };
        }
      });
      await api.admin.updateLibraryStory(id, {
        ...form,
        title: form.title?.trim() || null,
        moral: form.moral?.trim() || null,
        status: publish ? "PUBLISHED" : "DRAFT",
        coverImageUrl: form.coverImageUrl ?? null,
        coverVideoUrl: coverVideoUrl ?? null,
        regenerateNarration: false,
        translationContentEntries: Object.keys(translationPayload).length > 0 ? translationPayload : undefined,
      });
      if (publish) {
        showSuccess(
          "Submitted for review",
          "Story sent to Story for review. In Story for review, Approve or Reject. After approval, use Narration (Story to Speech) → Generate audio to create audio."
        );
        refreshPipelineActive();
      } else {
        showSuccess(
          "Story saved as draft",
          "Draft saved. Generate AI cover if needed, then Submit for review to send to Story for review."
        );
      }
      router.push(publish ? `/dashboard/stories?edited=${id}` : "/dashboard/stories");
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Failed to update story";
      showError("Update failed", msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleRegenerateWithPrompt = async () => {
    if (!id || !form) return;
    if (!form.content?.trim()) {
      showError("No content", "Add story content first, then use Regenerate with prompt.");
      return;
    }
    setRegenerating(true);
    try {
      const result = await api.admin.regenerateStoryWithPrompt(id, form.content, true);
      const parsed =
        result.content?.trim().startsWith("{") ? parseJsonStoryContent(result.content) : null;
      const effective = parsed ?? {
        content: result.content,
        title: result.title,
        moral: result.moral,
        category: result.category,
        theme: result.theme,
      };
      setForm((prev) =>
        prev
          ? {
              ...prev,
              content: effective.content ?? prev.content,
              title: effective.title || prev.title,
              moral: effective.moral ?? prev.moral,
              theme: effective.category ?? effective.theme ?? prev.theme,
            }
          : prev
      );
      if (result.translations && Object.keys(result.translations).length > 0) {
        setTranslationContentEntries((prev) => {
          const next = { ...prev };
          for (const [lang, entry] of Object.entries(result.translations!)) {
            if (entry && typeof entry === "object") {
              next[lang] = {
                content: (entry.content ?? "").trim(),
                title: (entry.title ?? "").trim(),
                moral: (entry.moral ?? "").trim(),
              };
            }
          }
          return next;
        });
      }
      const langCount = result.translations ? Object.keys(result.translations).filter((l) => (result.translations![l]?.content ?? "").trim().length > 0).length : 0;
      showSuccess(
        "Story recreated",
        langCount > 0
          ? `Content, title, and moral rewritten. ${langCount} other language(s) generated. Review, edit if needed, then Save or Submit for review.`
          : "Content, title, and moral were rewritten with the Tamixa prompt. Review the result, edit if needed, then Save or Submit for review."
      );
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Regeneration failed";
      showError("Regenerate with prompt failed", msg);
    } finally {
      setRegenerating(false);
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
            Min {MIN_WORD_COUNT} words · Tamil script
          </p>
        </div>
        <Button
          variant="outline"
          size="sm"
          disabled={!id || refreshingContent}
          onClick={async () => {
            if (!id) return;
            setRefreshingContent(true);
            try {
              await loadStoryContent(id);
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

      {/* Collapsible workflow hint */}
      <details className="group rounded-xl border border-border/60 bg-muted/30 overflow-hidden">
        <summary className="flex cursor-pointer items-center gap-2 px-4 py-3 text-sm font-medium text-foreground hover:bg-muted/40 list-none [&::-webkit-details-marker]:hidden">
          <Info className="h-4 w-4 text-muted-foreground shrink-0" />
          <span>Recommended workflow</span>
          <ChevronDown className="h-4 w-4 text-muted-foreground ml-auto transition-transform group-open:rotate-180" />
        </summary>
        <div className="px-4 pb-4 pt-0 text-sm text-muted-foreground space-y-2">
          <ol className="list-decimal list-inside space-y-1">
            <li>Save story</li>
            <li>Optional: Regenerate with prompt to see Tamixa-style recreation</li>
            <li>Generate AI cover</li>
            <li>Submit for review → story shows as <strong>Ready for review</strong> (no audio until approved)</li>
            <li>In Story for review, Approve or Reject</li>
            <li>After approval, go to Narration (Story to Speech) and use Generate audio or Regenerate audio</li>
          </ol>
          <p className="text-xs">Stories with changes need approval before audio can be generated or regenerated.</p>
        </div>
      </details>

      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        {/* Left: Story content */}
        <div className="space-y-6">
          {/* Core story — title, category, content */}
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-base font-semibold">Story content</CardTitle>
              <p className="text-sm text-muted-foreground">
                {SOURCE_LANGUAGES.find((l) => l.code === form?.language)?.label ?? "Tamil"} title, category, and text
              </p>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <Label htmlFor="sourceLang">Source language *</Label>
                <Select
                  value={form?.language ?? "ta"}
                  onValueChange={(v) => setForm((f) => (f ? { ...f, language: v } : f))}
                >
                  <SelectTrigger id="sourceLang" className="mt-1 rounded-lg">
                    <SelectValue placeholder="Select language" />
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
                  Pipeline uses Tamil as source. If you edit to Tamil, select Tamil and save so the form loads correctly next time.
                </p>
              </div>
              <div>
                <div className="flex items-center justify-between gap-2">
                  <Label htmlFor="title">
                    Title ({SOURCE_LANGUAGES.find((l) => l.code === form?.language)?.label?.replace(/ \(recommended\)/, "") ?? "Tamil"}) *
                  </Label>
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
                <Label htmlFor="content">
                  Story text ({SOURCE_LANGUAGES.find((l) => l.code === form?.language)?.label?.replace(/ \(recommended\)/, "") ?? "Tamil"}) *
                </Label>
                <textarea
                  id="content"
                  value={form.content}
                  onChange={(e) => setForm((f) => (f ? { ...f, content: e.target.value } : f))}
                  placeholder={`Enter full story text in ${SOURCE_LANGUAGES.find((l) => l.code === form?.language)?.label?.replace(/ \(recommended\)/, "") ?? "Tamil"}...`}
                  className="mt-1 flex min-h-[220px] w-full rounded-lg border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  dir="ltr"
                />
                <div className="flex justify-between mt-1">
                  <span className={cn("text-xs", isValidWordCount ? "text-muted-foreground" : "text-destructive")}>
                    {wordCount} / {MIN_WORD_COUNT} words
                  </span>
                </div>
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
            </CardContent>
          </Card>

          {/* Other languages — tabbed */}
          <details className="group rounded-xl border border-border/60 overflow-hidden">
            <summary className="flex cursor-pointer items-center gap-2 px-4 py-3 text-sm font-medium text-foreground hover:bg-muted/30 list-none [&::-webkit-details-marker]:hidden">
              <ChevronRight className="h-4 w-4 text-muted-foreground transition-transform group-open:rotate-90" />
              Other languages (optional)
            </summary>
            <div className="border-t border-border/60 p-4 space-y-4">
              <p className="text-xs text-muted-foreground">Edit translations. After Generate audio completes, click Refresh content above to see pipeline output for all languages.</p>
              <div className="flex gap-1 flex-wrap">
                {TAB_LANGUAGES.filter((l) => l.code !== form?.language).map(({ code, label }) => (
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
              {TAB_LANGUAGES.filter((l) => l.code !== form?.language).map(({ code, label }) => {
                if (activeLangTab !== code) return null;
                const entry = translationContentEntries[code] ?? { content: "", title: "", moral: "" };
                return (
                  <div key={code} className="space-y-3">
                    <div>
                      <Label htmlFor={`tl-title-${code}`}>Title</Label>
                      <Input id={`tl-title-${code}`} value={entry.title} onChange={(e) => setTranslationContentEntries((p) => ({ ...p, [code]: { ...(p[code] ?? { content: "", title: "", moral: "" }), title: e.target.value } }))} placeholder={`Title in ${label}`} className="mt-1 rounded-lg" />
                    </div>
                    <div>
                      <Label htmlFor={`tl-content-${code}`}>Story text</Label>
                      <textarea id={`tl-content-${code}`} value={entry.content} onChange={(e) => setTranslationContentEntries((p) => ({ ...p, [code]: { ...(p[code] ?? { content: "", title: "", moral: "" }), content: e.target.value } }))} placeholder={`Story in ${label}`} className="mt-1 flex min-h-[120px] w-full rounded-lg border border-input bg-background px-3 py-2 text-sm" />
                    </div>
                    <div>
                      <Label htmlFor={`tl-moral-${code}`}>Moral</Label>
                      <Input id={`tl-moral-${code}`} value={entry.moral} onChange={(e) => setTranslationContentEntries((p) => ({ ...p, [code]: { ...(p[code] ?? { content: "", title: "", moral: "" }), moral: e.target.value } }))} placeholder={`Moral in ${label}`} className="mt-1 rounded-lg" />
                    </div>
                  </div>
                );
              })}
            </div>
          </details>
        </div>

        {/* Right sidebar: Cover + Actions */}
        <div className="space-y-6 lg:sticky lg:top-20 lg:self-start">
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
                      const updated = await api.admin.regenerateLibraryStoryCover(id, hasCover);
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
                    <div className="aspect-video rounded-lg border bg-muted/30 overflow-hidden">
                      {coverVideoUrl.includes(".gif") ? (
                        <img key={coverRefreshKey} src={`${resolveCoverSrc(coverVideoUrl) ?? coverVideoUrl}?t=${coverRefreshKey}`} alt="Animated" className="w-full h-full object-cover" />
                      ) : (
                        <video key={coverRefreshKey} src={resolveCoverSrc(coverVideoUrl) ?? coverVideoUrl} className="w-full h-full object-cover" controls loop muted playsInline />
                      )}
                    </div>
                  )}
                  {form.coverImageUrl?.trim() && (
                    <div className="aspect-video rounded-lg border bg-muted/30 overflow-hidden">
                      <CoverImageWithFallback key={coverRefreshKey} src={`${resolveCoverSrc(form.coverImageUrl) ?? form.coverImageUrl}?t=${coverRefreshKey}`} />
                    </div>
                  )}
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold">Actions</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <Button
                onClick={handleRegenerateWithPrompt}
                variant="outline"
                size="default"
                className="w-full"
                disabled={regenerating || submitting || !form?.content?.trim()}
                title={!form?.content?.trim() ? "Add story content first" : "Rewrite story with Tamixa style (preview only)"}
              >
                <Sparkles className={cn("h-4 w-4 mr-2", regenerating && "animate-pulse")} />
                {regenerating ? "Regenerating…" : "Regenerate with prompt"}
              </Button>
              <p className="text-xs text-muted-foreground">
                Rewrites content in place so you can review before Save or Submit for review.
              </p>
              <div className="flex flex-col gap-2">
                <Button onClick={() => handleSubmit(false)} variant="outline" size="default" className="w-full" disabled={submitting}>
                  <Save className="h-4 w-4 mr-2" /> Save draft
                </Button>
                <Button onClick={() => handleSubmit(true)} size="default" className="w-full" disabled={submitting || isPipelineActive} title={isPipelineActive ? "Pipeline running" : undefined}>
                  {submitting ? "Submitting…" : "Submit for review"}
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
