"use client";

import { useCallback, useEffect, useState } from "react";
import { useRouter, useParams } from "next/navigation";
import Link from "next/link";
import { api } from "@/lib/api";
import type { CreateCuratedStoryRequest } from "@/types/api";
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
import { ArrowLeft, Save, Eye, Volume2, ImagePlus, RefreshCw } from "lucide-react";

/** Resolve cover URL: relative paths get API base (e.g. /api/v1/covers/... → http://localhost:8080/api/v1/covers/...). */
function resolveCoverSrc(coverImageUrl: string | null | undefined): string | null {
  if (!coverImageUrl?.trim()) return null;
  const u = coverImageUrl.trim();
  if (u.startsWith("http://") || u.startsWith("https://")) return u;
  const base =
    (typeof process !== "undefined" && process.env?.NEXT_PUBLIC_API_URL) ||
    (typeof window !== "undefined" ? "http://localhost:8080" : "");
  return u.startsWith("/") ? `${base}${u}` : `${base}/${u}`;
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

export default function EditCuratedStoryPage() {
  const router = useRouter();
  const params = useParams();
  const id = params?.id ? Number(params.id) : null;
  const { showSuccess, showError, showInfo } = useActionResult();

  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState<CreateCuratedStoryRequest | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [coverGenerating, setCoverGenerating] = useState(false);
  const [coverRefreshKey, setCoverRefreshKey] = useState(0);
  const [coverVideoUrl, setCoverVideoUrl] = useState<string | null>(null);
  const [rephrasing, setRephrasing] = useState(false);
  const [rephraseSuggestion, setRephraseSuggestion] = useState<{
    suggestedTitle: string;
    suggestedContent: string;
  } | null>(null);
  const [previewLang, setPreviewLang] = useState<"ta" | "en">("ta");
  const [showMobilePreview, setShowMobilePreview] = useState(false);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});

  const wordCount = form?.content
    ? form.content.split(/\s+/).filter((w) => w.trim()).length
    : 0;
  const isValidWordCount = wordCount >= MIN_WORD_COUNT;

  useEffect(() => {
    if (!id || isNaN(id)) {
      showError("Invalid story ID", "The story ID is invalid or not found.");
      router.push("/dashboard/curated-stories");
      return;
    }
    api.admin
      .getCuratedStory(id)
      .then((story) => {
        setForm({
          title: story.title ?? "",
          content: story.content ?? "",
          theme: story.theme,
          language: story.language ?? "ta",
          age: story.age,
          childName: story.childName ?? "Child",
          moral: story.moral ?? "",
          status: story.status ?? "DRAFT",
          coverImageUrl: story.coverImageUrl ?? "",
          emotionMode: story.emotionMode ?? "CALM",
        });
        setCoverVideoUrl(story.coverVideoUrl ?? null);
      })
      .catch((e) => {
        showError("Failed to load story", e instanceof Error ? e.message : "Unable to load the story. Please try again.");
        router.push("/dashboard/curated-stories");
      })
      .finally(() => setLoading(false));
  }, [id, router]);

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
      errs.content = "Story content must contain Tamil script (தமிழ் characters)";
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
      await api.admin.updateCuratedStory(id, {
        ...form,
        title: form.title?.trim() || null,
        moral: form.moral?.trim() || null,
        status: publish ? "PUBLISHED" : "DRAFT",
        coverImageUrl: form.coverImageUrl ?? null,
        coverVideoUrl: coverVideoUrl ?? null,
      });
      showSuccess(
        publish ? "Story published" : "Story saved as draft",
        publish
          ? "Audio regeneration started for all languages. Your cover (image + GIF) is preserved. Check progress on the list."
          : "Draft saved. Generate AI cover if needed, then Update & publish to go live."
      );
      router.push(publish ? `/dashboard/curated-stories?edited=${id}` : "/dashboard/curated-stories");
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Failed to update story";
      showError("Update failed", msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleTtsPreview = () => {
    const text = form?.content?.trim().slice(0, 200) || "";
    if (!text) {
      showError("TTS preview", "Add story content first to preview narration.");
      return;
    }
    if ("speechSynthesis" in window) {
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = form?.language === "ta" ? "ta-IN" : "en-IN";
      window.speechSynthesis.speak(utterance);
      showInfo("TTS preview", "Playing sample using browser text-to-speech.");
    } else {
      showInfo("TTS preview", "TTS preview is not available in this browser.");
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
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center gap-4">
        <Link href="/dashboard/curated-stories">
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
      </div>

      <div className="rounded-lg border border-primary/20 bg-primary/5 px-4 py-3 text-sm text-foreground">
        <p className="font-medium mb-1">Recommended workflow</p>
        <ol className="list-decimal list-inside space-y-0.5 text-muted-foreground">
          <li>Save story (content &amp; details)</li>
          <li>Generate AI cover with GIF (image + animated cover)</li>
          <li>Update &amp; publish — regenerates audio for all languages</li>
        </ol>
        <p className="text-xs text-muted-foreground mt-2">Your generated cover is kept when you publish; it is not overwritten.</p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_360px]">
        <div className="space-y-6">
          <Card className="border-border/80 shadow-sm">
            <CardHeader className="pb-3">
              <CardTitle className="text-lg font-semibold tracking-tight">Story details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <div className="flex items-center justify-between">
                  <Label htmlFor="title">Title (Tamil) *</Label>
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
                        const s = await api.admin.suggestRephrase(
                          id,
                          form.title || null,
                          form.content
                        );
                        setRephraseSuggestion(s);
                      } catch (e) {
                        showError("Rephrase failed", e instanceof Error ? e.message : "Rephrase failed");
                      } finally {
                        setRephrasing(false);
                      }
                    }}
                    title="Suggest AI rephrasing for title and content (Tamil)"
                  >
                    <RefreshCw className={`h-4 w-4 mr-1 ${rephrasing ? "animate-spin" : ""}`} />
                    {rephrasing ? "Rephrasing…" : "Rephrase (AI)"}
                  </Button>
                </div>
                <Input
                  id="title"
                  value={form.title ?? ""}
                  onChange={(e) =>
                    setForm((f) => (f ? { ...f, title: e.target.value } : f))
                  }
                  placeholder="e.g. அறிவுள்ள காகம்"
                  className="mt-1"
                  dir="ltr"
                />
                {validationErrors.title && (
                  <p className="text-sm text-destructive mt-1">
                    {validationErrors.title}
                  </p>
                )}
              </div>

              <div>
                <Label htmlFor="theme">Category *</Label>
                <Select
                  value={form.theme}
                  onValueChange={(v) =>
                    setForm((f) => (f ? { ...f, theme: v } : f))
                  }
                >
                  <SelectTrigger className="mt-1">
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
                  <p className="text-sm text-destructive mt-1">
                    {validationErrors.theme}
                  </p>
                )}
              </div>

              <div>
                <Label htmlFor="content">Story text (Tamil) *</Label>
                <textarea
                  id="content"
                  value={form.content}
                  onChange={(e) =>
                    setForm((f) => (f ? { ...f, content: e.target.value } : f))
                  }
                  placeholder="Enter full story text in Tamil..."
                  className="mt-1 flex min-h-[280px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  dir="ltr"
                />
                <div className="flex justify-between mt-1">
                  <span
                    className={`text-xs ${
                      isValidWordCount ? "text-muted-foreground" : "text-destructive"
                    }`}
                  >
                    {wordCount} / {MIN_WORD_COUNT} words
                  </span>
                </div>
                {validationErrors.content && (
                  <p className="text-sm text-destructive">{validationErrors.content}</p>
                )}
                {rephraseSuggestion && (
                  <div className="mt-3 p-3 rounded-lg border bg-muted/50 space-y-2">
                    <p className="text-sm font-medium">Suggested rephrasing</p>
                    <div className="flex gap-2">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => {
                          setForm((f) =>
                            f
                              ? { ...f, title: rephraseSuggestion.suggestedTitle || f.title }
                              : f
                          );
                          setRephraseSuggestion(null);
                          showSuccess("Title applied", "The title has been applied.");
                        }}
                      >
                        Apply title
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => {
                          setForm((f) =>
                            f ? { ...f, content: rephraseSuggestion.suggestedContent } : f
                          );
                          setRephraseSuggestion(null);
                          showSuccess("Content applied", "The content has been applied.");
                        }}
                      >
                        Apply content
                      </Button>
                      <Button
                        size="sm"
                        variant="default"
                        onClick={() => {
                          setForm((f) =>
                            f
                              ? {
                                  ...f,
                                  title: rephraseSuggestion.suggestedTitle || f.title,
                                  content: rephraseSuggestion.suggestedContent,
                                }
                              : f
                          );
                          setRephraseSuggestion(null);
                          showSuccess("Applied", "Title and content have been applied.");
                        }}
                      >
                        Apply both
                      </Button>
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => setRephraseSuggestion(null)}
                      >
                        Dismiss
                      </Button>
                    </div>
                  </div>
                )}
              </div>

              <div>
                <Label htmlFor="moral">Moral (optional)</Label>
                <Input
                  id="moral"
                  value={form.moral ?? ""}
                  onChange={(e) =>
                    setForm((f) => (f ? { ...f, moral: e.target.value } : f))
                  }
                  placeholder="e.g. Sharing brings joy"
                  className="mt-1"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <Label>Age group</Label>
                  <Select
                    value={String(form.age)}
                    onValueChange={(v) =>
                      setForm((f) =>
                        f ? { ...f, age: parseInt(v, 10) } : f
                      )
                    }
                  >
                    <SelectTrigger className="mt-1">
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
                <div>
                  <Label htmlFor="childName">Child name placeholder</Label>
                  <Input
                    id="childName"
                    value={form.childName ?? "Child"}
                    onChange={(e) =>
                      setForm((f) => (f ? { ...f, childName: e.target.value } : f))
                    }
                    className="mt-1"
                  />
                </div>
              </div>

              <div>
                <Label className="text-sm font-medium">Cover image · HD</Label>
                <div className="flex gap-2 mt-2">
                  <Input
                    value={form.coverImageUrl ?? ""}
                    onChange={(e) =>
                      setForm((f) =>
                        f ? { ...f, coverImageUrl: e.target.value || null } : f
                      )
                    }
                    placeholder="URL or generate AI cover"
                    className="flex-1 h-10 text-sm"
                  />
                  <Button
                    type="button"
                    variant="outline"
                    size="default"
                    className="shrink-0 h-10 font-medium"
                    disabled={coverGenerating || !id}
                    onClick={async () => {
                      if (!id) return;
                      setCoverGenerating(true);
                      try {
                        const updated = await api.admin.generateCuratedStoryCover(id, false);
                        const resolvedUrl =
                          updated?.coverImageUrl?.trim()
                            ? updated.coverImageUrl
                            : null;
                        setForm((f) =>
                          f
                            ? { ...f, coverImageUrl: resolvedUrl ?? f.coverImageUrl ?? "" }
                            : f
                        );
                        setCoverVideoUrl(updated?.coverVideoUrl ?? null);
                        setCoverRefreshKey(Date.now());
                        showSuccess("Cover generated", "AI cover has been generated (Animated HD style).");
                      } catch (e) {
                        showError("Cover generation failed", e instanceof Error ? e.message : "Unable to generate the cover. Please try again.");
                      } finally {
                        setCoverGenerating(false);
                      }
                    }}
                    title="Generate AI cover: uses English content when available, no text in image, Animated HD style"
                  >
                    <ImagePlus className="h-4 w-4 mr-1.5" />
                    {coverGenerating ? "Generating…" : "Generate AI cover"}
                  </Button>
                  {form.coverImageUrl?.trim() && (
                    <Button
                      type="button"
                      variant="outline"
                      size="default"
                      className="shrink-0 h-10 font-medium"
                      disabled={coverGenerating || !id}
                      onClick={async () => {
                        if (!id) return;
                        setCoverGenerating(true);
                        try {
                          const updated = await api.admin.generateCuratedStoryCover(id, true);
                          const resolvedUrl =
                            updated?.coverImageUrl?.trim()
                              ? updated.coverImageUrl
                              : null;
                          setForm((f) =>
                            f
                              ? { ...f, coverImageUrl: resolvedUrl ?? f.coverImageUrl ?? "" }
                              : f
                          );
                          setCoverVideoUrl(updated?.coverVideoUrl ?? null);
                          setCoverRefreshKey(Date.now());
                          showSuccess("Alternate cover generated", "A new AI cover has been generated. Animated cover (video) uses Sora when enabled.");
                        } catch (e) {
                          showError("Alternate cover failed", e instanceof Error ? e.message : "Unable to generate alternate cover.");
                        } finally {
                          setCoverGenerating(false);
                        }
                      }}
                      title="Clear existing cover and generate a new one (retry until it matches the story)"
                    >
                      {coverGenerating ? "Generating…" : "Alternate cover"}
                    </Button>
                  )}
                </div>
                <p className="text-xs text-muted-foreground mt-2">
                  Animated cover (GIF) uses Sora when enabled. Generated cover is preserved when you Update &amp; publish.
                </p>
                {(form.coverImageUrl?.trim() || coverVideoUrl) && (
                  <div className="mt-3 space-y-3">
                    <p className="text-xs text-muted-foreground mb-1.5">Generated cover</p>
                    {coverVideoUrl?.trim() && (
                      <div>
                        <p className="text-xs text-muted-foreground mb-1">Animated cover (GIF)</p>
                        <div className="aspect-video max-w-sm rounded-lg border bg-muted/30 overflow-hidden">
                          {coverVideoUrl.includes(".gif") ? (
                            <img
                              key={coverRefreshKey}
                              src={`${resolveCoverSrc(coverVideoUrl) ?? coverVideoUrl}?t=${coverRefreshKey}`}
                              alt="Animated cover"
                              className="w-full h-full object-cover"
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
                      </div>
                    )}
                    {form.coverImageUrl?.trim() && (
                      <div>
                        <p className="text-xs text-muted-foreground mb-1">Static image</p>
                        <div className="aspect-video max-w-sm rounded-lg border bg-muted/30 overflow-hidden">
                          <CoverImageWithFallback
                            key={coverRefreshKey}
                            src={`${resolveCoverSrc(form.coverImageUrl) ?? form.coverImageUrl}?t=${coverRefreshKey}`}
                          />
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>

              <div className="flex flex-wrap gap-3 pt-6 border-t border-border/60 mt-6">
                <Button
                  onClick={() => handleSubmit(false)}
                  variant="outline"
                  size="default"
                  className="h-10 px-5 font-medium"
                  disabled={submitting}
                >
                  <Save className="h-4 w-4 mr-2" /> Save draft
                </Button>
                <Button
                  onClick={() => handleSubmit(true)}
                  size="default"
                  className="h-10 px-5 font-medium"
                  disabled={submitting}
                >
                  {submitting ? "Updating…" : "Update & publish"}
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Preview panel */}
        <div className="space-y-4">
          <Card className="border-border/80 shadow-sm">
            <CardHeader className="pb-2">
              <CardTitle className="text-lg font-semibold tracking-tight">Preview</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex gap-2">
                <Button
                  variant={showMobilePreview ? "default" : "outline"}
                  size="default"
                  className="h-9 font-medium"
                  onClick={() => setShowMobilePreview(true)}
                >
                  <Eye className="h-4 w-4 mr-1.5" /> Card
                </Button>
                <Button variant="outline" size="default" className="h-9 font-medium" onClick={handleTtsPreview}>
                  <Volume2 className="h-4 w-4 mr-1.5" /> TTS sample
                </Button>
              </div>

              <div className="flex gap-1">
                {(["ta", "en"] as const).map((lang) => (
                  <Button
                    key={lang}
                    variant={previewLang === lang ? "secondary" : "ghost"}
                    size="sm"
                    onClick={() => setPreviewLang(lang)}
                  >
                    {lang === "ta" ? "தமிழ்" : "English"}
                  </Button>
                ))}
              </div>

              {showMobilePreview && (
                <div className="rounded-xl border bg-muted/30 p-4 max-w-[280px] mx-auto">
                  <div className="aspect-[4/3] rounded-lg bg-muted mb-3 flex items-center justify-center overflow-hidden">
                    {resolveCoverSrc(form.coverImageUrl) ? (
                      <CoverImageWithFallback
                        key={coverRefreshKey}
                        src={`${resolveCoverSrc(form.coverImageUrl)!}?t=${coverRefreshKey}`}
                      />
                    ) : (
                      <span className="text-3xl opacity-50">📖</span>
                    )}
                  </div>
                  <p className="font-medium text-sm truncate">
                    {form.title || "Untitled"}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {form.theme} · {Math.ceil(wordCount / 150)} min
                  </p>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
