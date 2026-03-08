"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
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
import { ArrowLeft, Save, Eye, Volume2 } from "lucide-react";

const AUTOSAVE_KEY = "araro_curated_story_draft";
const AUTOSAVE_DEBOUNCE_MS = 2000;

export default function NewCuratedStoryPage() {
  const router = useRouter();
  const { showSuccess, showError, showWarning, showInfo } = useActionResult();
  const [form, setForm] = useState<CreateCuratedStoryRequest>({
    title: "",
    content: "",
    theme: STORY_CATEGORIES[0], // Default: first category
    language: "ta",
    age: 5,
    childName: "Child",
    moral: "",
    status: "DRAFT",
    emotionMode: "CALM", // Default: calming for bedtime narration
  });
  const [submitting, setSubmitting] = useState(false);
  const [coverGenerating, setCoverGenerating] = useState(false);
  const [previewLang, setPreviewLang] = useState<"ta" | "en">("ta");
  const [showMobilePreview, setShowMobilePreview] = useState(false);
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const autosaveRef = useRef<ReturnType<typeof setTimeout>>();

  const wordCount = form.content
    ? form.content.split(/\s+/).filter((w) => w.trim()).length
    : 0;
  const isValidWordCount = wordCount >= MIN_WORD_COUNT;

  // Load draft from localStorage on mount
  useEffect(() => {
    try {
      const saved = localStorage.getItem(AUTOSAVE_KEY);
      if (saved) {
        const parsed = JSON.parse(saved) as Partial<CreateCuratedStoryRequest>;
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
  }, []);

  // Autosave draft
  useEffect(() => {
    if (form.content || form.title) {
      autosaveRef.current = setTimeout(() => {
        localStorage.setItem(AUTOSAVE_KEY, JSON.stringify(form));
      }, AUTOSAVE_DEBOUNCE_MS);
    }
    return () => {
      if (autosaveRef.current) clearTimeout(autosaveRef.current);
    };
  }, [form]);

  /** Tamil Unicode block U+0B80–U+0BFF */
  const hasTamilScript = (text: string) => /[\u0B80-\u0BFF]/.test(text);

  const validate = useCallback((): boolean => {
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

  const handleSubmit = async (publish: boolean, generateCover: boolean = true) => {
    if (!validate()) {
      showError("Validation failed", "Please fix the validation errors before saving.");
      return;
    }
    setSubmitting(true);
    try {
      const created = await api.admin.createCuratedStory({
        ...form,
        title: form.title?.trim() || null,
        moral: form.moral?.trim() || null,
        status: publish ? "PUBLISHED" : "DRAFT",
      });
      localStorage.removeItem(AUTOSAVE_KEY);

      if (generateCover && created?.id) {
        setCoverGenerating(true);
        try {
          await api.admin.generateCuratedStoryCover(created.id);
          showSuccess(
            publish ? "Story published" : "Story saved",
            publish
              ? "Story published with AI cover. Audio will be generated automatically."
              : "Story saved with AI cover. You can publish it when ready."
          );
        } catch (coverErr) {
          showWarning(
            "Cover generation failed",
            coverErr instanceof Error ? coverErr.message : "Cover generation failed, but the story was saved."
          );
        } finally {
          setCoverGenerating(false);
        }
      } else {
        showSuccess(
          publish ? "Story published" : "Story saved",
          publish
            ? "Story published. Audio will be generated automatically."
            : "Story saved as draft. You can publish it when ready."
        );
      }
      router.push("/dashboard/curated-stories");
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Unable to create the story. Please try again.";
      showError("Create failed", msg);
    } finally {
      setSubmitting(false);
    }
  };

  const handleTtsPreview = () => {
    const text = form.content?.trim().slice(0, 200) || "";
    if (!text) {
      showError("TTS preview", "Add story content first to preview narration.");
      return;
    }
    if ("speechSynthesis" in window) {
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.lang = form.language === "ta" ? "ta-IN" : "en-IN";
      window.speechSynthesis.speak(utterance);
      showInfo("TTS preview", "Playing sample using browser text-to-speech.");
    } else {
      showInfo("TTS preview", "TTS preview is not available in this browser.");
    }
  };

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center gap-4">
        <Link href="/dashboard/curated-stories">
          <Button variant="ghost" size="sm">
            <ArrowLeft className="h-4 w-4 mr-1" /> Back
          </Button>
        </Link>
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            Create Tamil Story
          </h1>
          <p className="text-sm text-muted-foreground">
            Autosave · Min {MIN_WORD_COUNT} words · Tamil script · Version history coming soon
          </p>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Story details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <Label htmlFor="title">Title (Tamil) *</Label>
                <Input
                  id="title"
                  value={form.title ?? ""}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, title: e.target.value }))
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
                  onValueChange={(v) => setForm((f) => ({ ...f, theme: v }))}
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
                    setForm((f) => ({ ...f, content: e.target.value }))
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
              </div>

              <div>
                <Label htmlFor="moral">Moral (optional)</Label>
                <Input
                  id="moral"
                  value={form.moral ?? ""}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, moral: e.target.value }))
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
                      setForm((f) => ({ ...f, age: parseInt(v, 10) }))
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
                      setForm((f) => ({ ...f, childName: e.target.value }))
                    }
                    className="mt-1"
                  />
                </div>
              </div>

              <div>
                <Label>Cover image</Label>
                <p className="text-xs text-muted-foreground mt-0.5 mb-1">
                  AI cover will be generated when saving (or add URL manually)
                </p>
                <Input
                  value={form.coverImageUrl ?? ""}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, coverImageUrl: e.target.value || null }))
                  }
                  placeholder="Leave empty for AI cover, or paste https://..."
                  className="mt-1"
                />
              </div>

              <div className="flex flex-wrap gap-2 pt-4">
                <Button
                  onClick={() => handleSubmit(false, !form.coverImageUrl)}
                  variant="outline"
                  disabled={submitting}
                >
                  <Save className="h-4 w-4 mr-2" /> Save draft
                </Button>
                <Button
                  onClick={() => handleSubmit(true, !form.coverImageUrl)}
                  disabled={submitting}
                >
                  {submitting ? (coverGenerating ? "Generating cover…" : "Publishing…") : "Publish"}
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Preview panel */}
        <div className="space-y-4">
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-base">Preview</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex gap-2">
                <Button
                  variant={showMobilePreview ? "default" : "outline"}
                  size="sm"
                  onClick={() => setShowMobilePreview(true)}
                >
                  <Eye className="h-4 w-4 mr-1" /> Card
                </Button>
                <Button variant="outline" size="sm" onClick={handleTtsPreview}>
                  <Volume2 className="h-4 w-4 mr-1" /> TTS sample
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
                  <div className="aspect-[4/3] rounded-lg bg-muted mb-3 flex items-center justify-center">
                    {form.coverImageUrl ? (
                      // eslint-disable-next-line @next/next/no-img-element -- User-provided URL, domain unknown
                      <img
                        src={form.coverImageUrl}
                        alt="Story cover"
                        className="w-full h-full object-cover rounded-lg"
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
