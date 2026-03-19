"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { api } from "@/lib/api";
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
import { ArrowLeft, Save } from "lucide-react";

const AUTOSAVE_KEY = "tamixa_story_draft";
const AUTOSAVE_DEBOUNCE_MS = 2000;

export default function NewLibraryStoryPage() {
  const router = useRouter();
  const { showSuccess, showError, showWarning } = useActionResult();
  const [form, setForm] = useState<CreateLibraryStoryRequest>({
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

  /** Script validation per language (matches backend StoryLibraryValidation) */
  const hasScriptForLanguage = (text: string, lang: string): boolean => {
    if (!text?.trim()) return false;
    const normalized = lang.trim().toLowerCase();
    if (normalized === "en") return true; // English has no script requirement
    if (normalized === "ta") return /[\u0B80-\u0BFF]/.test(text);   // Tamil
    if (normalized === "hi") return /[\u0900-\u097F]/.test(text);   // Devanagari
    if (normalized === "te") return /[\u0C00-\u0C7F]/.test(text);   // Telugu
    if (normalized === "kn") return /[\u0C80-\u0CFF]/.test(text);   // Kannada
    if (normalized === "ml") return /[\u0D00-\u0D7F]/.test(text);   // Malayalam
    return true;
  };

  const SOURCE_LANGUAGES = [
    { code: "ta", label: "Tamil (recommended)" },
    { code: "en", label: "English" },
    { code: "hi", label: "Hindi" },
    { code: "te", label: "Telugu" },
    { code: "kn", label: "Kannada" },
    { code: "ml", label: "Malayalam" },
  ];

  const validate = useCallback((): boolean => {
    const errs: Record<string, string> = {};
    if (!form.theme?.trim()) errs.theme = "Category is required";
    if (!form.content?.trim()) errs.content = "Story text is required";
    else if (wordCount < MIN_WORD_COUNT)
      errs.content = `Minimum ${MIN_WORD_COUNT} words required (current: ${wordCount})`;
    else if (form.language && form.language !== "en" && !hasScriptForLanguage(form.content, form.language))
      errs.content = `Story content must contain ${SOURCE_LANGUAGES.find((l) => l.code === form.language)?.label ?? form.language} script`;
    if (form.title?.trim() && form.title.length > 255)
      errs.title = "Title must be 255 characters or less";
    setValidationErrors(errs);
    return Object.keys(errs).length === 0;
  }, [form, wordCount]);

  const handleSubmit = async (publish: boolean, regenerateCover: boolean = true) => {
    if (!validate()) {
      showError("Validation failed", "Please fix the validation errors before saving.");
      return;
    }
    setSubmitting(true);
    try {
      const created = await api.admin.createLibraryStory({
        ...form,
        title: form.title?.trim() || null,
        moral: form.moral?.trim() || null,
        status: publish ? "PUBLISHED" : "DRAFT",
      });
      localStorage.removeItem(AUTOSAVE_KEY);

      if (regenerateCover && created?.id) {
        setCoverGenerating(true);
        try {
          await api.admin.regenerateLibraryStoryCover(created.id, false);
          showSuccess(
            publish ? "Story published" : "Story saved",
            publish
              ? "Story published with AI cover. Audio will be generated automatically."
              : "Story saved with AI cover. You can publish it when ready."
          );
        } catch (coverErr) {
          showWarning(
            "Regenerate cover failed",
            coverErr instanceof Error ? coverErr.message : "Regenerate cover failed, but the story was saved."
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
      router.push("/dashboard/stories");
    } catch (e) {
      const msg = e instanceof Error ? e.message : "Unable to create the story. Please try again.";
      showError("Create failed", msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-8 max-w-4xl">
      {/* Page header */}
      <div className="rounded-2xl border-2 border-border bg-gradient-to-r from-primary/5 via-primary/[0.03] to-transparent p-6">
        <div className="flex flex-wrap items-center gap-4">
          <Link href="/dashboard/stories">
            <Button variant="outline" size="sm" className="rounded-xl">
              <ArrowLeft className="h-4 w-4 mr-1" /> Back
            </Button>
          </Link>
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-foreground">
              Create Story
            </h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Autosave · Min {MIN_WORD_COUNT} words · {SOURCE_LANGUAGES.find((l) => l.code === form.language)?.label ?? "Tamil"} · Version history coming soon
            </p>
          </div>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <div className="space-y-6">
          <Card className="border-2 border-border overflow-hidden">
            <CardHeader className="border-b border-border/50 bg-muted/20">
              <CardTitle className="text-base font-bold">Story details</CardTitle>
              <p className="text-sm text-muted-foreground mt-0.5">Title, category, content, and metadata.</p>
            </CardHeader>
            <CardContent className="space-y-5 pt-6">
              <div>
                <Label htmlFor="sourceLang">Language *</Label>
                <Select value={form.language ?? "ta"} onValueChange={(v) => setForm((f) => ({ ...f, language: v }))}>
                  <SelectTrigger id="sourceLang" className="mt-1 rounded-xl">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {SOURCE_LANGUAGES.map((l) => (
                      <SelectItem key={l.code} value={l.code}>{l.label}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <Label htmlFor="title">Title ({SOURCE_LANGUAGES.find((l) => l.code === form.language)?.label?.replace(/ \(recommended\)/, "") ?? "Tamil"}) *</Label>
                <Input
                  id="title"
                  value={form.title ?? ""}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, title: e.target.value }))
                  }
                  placeholder="e.g. அறிவுள்ள காகம்"
                  className="mt-1 rounded-xl"
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
                  <p className="text-sm text-destructive mt-1">
                    {validationErrors.theme}
                  </p>
                )}
              </div>

              <div>
                <Label htmlFor="content">Story text ({SOURCE_LANGUAGES.find((l) => l.code === form.language)?.label?.replace(/ \(recommended\)/, "") ?? "Tamil"}) *</Label>
                <textarea
                  id="content"
                  value={form.content}
                  onChange={(e) =>
                    setForm((f) => ({ ...f, content: e.target.value }))
                  }
                  placeholder={`Enter full story text in ${SOURCE_LANGUAGES.find((l) => l.code === form.language)?.label?.replace(/ \(recommended\)/, "") ?? "Tamil"}...`}
                  className="mt-1 flex min-h-[280px] w-full rounded-xl border-2 border-input bg-background px-4 py-3 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
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
                  className="mt-1 rounded-xl"
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
                <div>
                  <Label htmlFor="childName">Child name placeholder</Label>
                  <Input
                    id="childName"
                    value={form.childName ?? "Child"}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, childName: e.target.value }))
                    }
                    className="mt-1 rounded-xl"
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
                  className="mt-1 rounded-xl"
                />
              </div>

              <div className="flex flex-wrap gap-3 pt-6 border-t border-border">
                <Button
                  onClick={() => handleSubmit(false, !form.coverImageUrl)}
                  variant="outline"
                  disabled={submitting}
                  className="rounded-xl"
                >
                  <Save className="h-4 w-4 mr-2" /> Save draft
                </Button>
                <Button
                  onClick={() => handleSubmit(true, !form.coverImageUrl)}
                  disabled={submitting}
                  className="rounded-xl"
                  variant="primary"
                >
                  {submitting ? (coverGenerating ? "Generating cover…" : "Publishing…") : "Publish"}
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
