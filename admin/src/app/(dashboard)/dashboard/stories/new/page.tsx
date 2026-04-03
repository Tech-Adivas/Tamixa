"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { useRouter, usePathname, useSearchParams } from "next/navigation";
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
import { ArrowLeft, Save, ExternalLink } from "lucide-react";
import {
  StoryWorkflowStepper,
  StoryWorkflowStepFooter,
  LIBRARY_STORY_WORKFLOW_STEPS,
} from "@/components/story-workflow-stepper";
import { REGENERATE_THEN_TRANSLATIONS_HELP } from "@/lib/library-story-workflow";

const AUTOSAVE_KEY = "tamixa_story_draft";
const AUTOSAVE_DEBOUNCE_MS = 2000;
const WORKFLOW_STEP_COUNT = LIBRARY_STORY_WORKFLOW_STEPS.length;

const SOURCE_LANGUAGES = [
  { code: "ta", label: "Tamil (recommended)" },
  { code: "en", label: "English" },
  { code: "hi", label: "Hindi" },
  { code: "te", label: "Telugu" },
  { code: "kn", label: "Kannada" },
  { code: "ml", label: "Malayalam" },
] as const;

/** Script validation per language (matches backend StoryLibraryValidation) */
function hasScriptForLanguage(text: string, lang: string): boolean {
  if (!text?.trim()) return false;
  const normalized = lang.trim().toLowerCase();
  if (normalized === "en") return true;
  if (normalized === "ta") return /[\u0B80-\u0BFF]/.test(text);
  if (normalized === "hi") return /[\u0900-\u097F]/.test(text);
  if (normalized === "te") return /[\u0C00-\u0C7F]/.test(text);
  if (normalized === "kn") return /[\u0C80-\u0CFF]/.test(text);
  if (normalized === "ml") return /[\u0D00-\u0D7F]/.test(text);
  return true;
}

export default function NewLibraryStoryPage() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const { showSuccess, showError, showWarning } = useActionResult();

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
  });
  const [submitting, setSubmitting] = useState(false);
  const [coverGenerating, setCoverGenerating] = useState(false);
  /** Optional notes appended after the standard Tamixa cover template when AI generates a cover on Save/Publish. */
  const [coverCustomPrompt, setCoverCustomPrompt] = useState("");
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const autosaveRef = useRef<ReturnType<typeof setTimeout>>();

  const wordCount = form.content
    ? form.content.split(/\s+/).filter((w) => w.trim()).length
    : 0;
  const isValidWordCount = wordCount >= MIN_WORD_COUNT;

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

  const handleStepChange = useCallback(
    (i: number) => {
      const next = Math.min(WORKFLOW_STEP_COUNT - 1, Math.max(0, i));
      if (next > workflowStep && workflowStep === 0 && next >= 1 && !validate()) {
        showError("Check story", "Fix the highlighted fields before leaving the Content step.");
        return;
      }
      setWorkflowStep(next);
    },
    [workflowStep, validate, setWorkflowStep, showError]
  );

  const goNextStep = useCallback(() => {
    if (workflowStep === 0 && !validate()) {
      showError("Check story", "Fix the highlighted fields before continuing.");
      return;
    }
    setWorkflowStep(workflowStep + 1);
  }, [workflowStep, validate, setWorkflowStep, showError]);

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
        parentContentNote: form.parentContentNote?.trim() || null,
        speakAlongPrompt: form.speakAlongPrompt?.trim() || null,
        parentDiscussionPrompts: form.parentDiscussionPrompts?.length
          ? form.parentDiscussionPrompts
          : null,
      });
      localStorage.removeItem(AUTOSAVE_KEY);

      if (regenerateCover && created?.id) {
        setCoverGenerating(true);
        try {
          await api.admin.regenerateLibraryStoryCover(
            created.id,
            false,
            coverCustomPrompt.trim() || null
          );
          showSuccess(
            publish ? "Story published" : "Story saved",
            publish
              ? "Story published with AI cover. Open it from the list to continue translations and review in the same 5-step workflow."
              : "Story saved with AI cover. Open it from the list to continue the workflow (cover, languages, submit, review, narration)."
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
            ? "Story published. Open it from the list to continue the workflow."
            : "Story saved as draft. Open it from the list to continue the workflow."
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

  const langLabel = (code: string) =>
    SOURCE_LANGUAGES.find((l) => l.code === code)?.label?.replace(/ \(recommended\)/, "") ?? code;

  return (
    <div className="space-y-6 max-w-5xl">
      <div className="rounded-2xl border-2 border-border bg-gradient-to-r from-primary/5 via-primary/[0.03] to-transparent p-6 space-y-5">
        <div className="flex flex-wrap items-center gap-4">
          <Link href="/dashboard/stories">
            <Button variant="outline" size="sm" className="rounded-xl">
              <ArrowLeft className="h-4 w-4 mr-1" /> Back
            </Button>
          </Link>
          <div>
            <h1 className="text-3xl font-bold tracking-tight text-foreground">Create Story</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Same 5-step workflow as Edit · Autosave · Min {MIN_WORD_COUNT} words · {langLabel(form.language ?? "ta")}
            </p>
          </div>
        </div>
        <StoryWorkflowStepper
          steps={LIBRARY_STORY_WORKFLOW_STEPS}
          currentStep={workflowStep}
          onStepChange={handleStepChange}
          className="border border-border/60 bg-background/80"
        />
        <p className="text-xs text-muted-foreground">
          Steps 1–3 are on this page. After the story exists, open it from <strong>Stories</strong> → <strong>Edit</strong>.{" "}
          {REGENERATE_THEN_TRANSLATIONS_HELP} Then <strong>Submit for review</strong>, <strong>Story for review</strong>, and{" "}
          <strong>Narration</strong> (steps 4–5).
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_300px]">
        <div className="space-y-6 min-h-[120px]">
          {workflowStep === 0 && (
            <Card className="border-2 border-border overflow-hidden">
              <CardHeader className="border-b border-border/50 bg-muted/20">
                <CardTitle className="text-base font-bold">Content</CardTitle>
                <p className="text-sm text-muted-foreground mt-0.5">
                  Master language, title, category, story text, and metadata (step 1 of 5).
                </p>
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
                        <SelectItem key={l.code} value={l.code}>
                          {l.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
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
                      className={`text-xs ${isValidWordCount ? "text-muted-foreground" : "text-destructive"}`}
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
                    Shown in apps for library playback: context for caregivers and discussion ideas (up to 10 prompts,
                    max 400 chars each).
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
                <div className="grid grid-cols-2 gap-4">
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
                  <div>
                    <Label htmlFor="childName">Child name placeholder</Label>
                    <Input
                      id="childName"
                      value={form.childName ?? "Child"}
                      onChange={(e) => setForm((f) => ({ ...f, childName: e.target.value }))}
                      className="mt-1 rounded-xl"
                    />
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          {workflowStep === 1 && (
            <Card className="border-2 border-border overflow-hidden">
              <CardHeader className="border-b border-border/50 bg-muted/20">
                <CardTitle className="text-base font-bold">Cover &amp; languages</CardTitle>
                <p className="text-sm text-muted-foreground mt-0.5">
                  Optional cover URL before save. Other languages and <strong>Generate translations</strong> run in{" "}
                  <strong>Edit</strong> after the story exists.
                </p>
              </CardHeader>
              <CardContent className="space-y-4 pt-6">
                <div>
                  <Label>Cover image URL (optional)</Label>
                  <p className="text-xs text-muted-foreground mt-0.5 mb-1">
                    Leave empty to let AI generate a cover when you save or publish.
                  </p>
                  <Input
                    value={form.coverImageUrl ?? ""}
                    onChange={(e) => setForm((f) => ({ ...f, coverImageUrl: e.target.value || null }))}
                    placeholder="https://… or leave empty for AI cover"
                    className="mt-1 rounded-xl"
                  />
                </div>
                <p className="text-xs text-muted-foreground rounded-lg border border-border/60 bg-muted/20 px-3 py-2">
                  Optional <strong>custom cover instructions</strong> live in the <strong>AI cover notes</strong> card (right column on wide screens, below on mobile) for Content, Cover &amp; languages, and Save — same field everywhere.
                </p>
                <ul className="text-sm text-muted-foreground list-disc pl-5 space-y-1">
                  <li>After create, open the story → Edit → step <strong>Cover &amp; languages</strong>.</li>
                  <li>
                    Optional: <strong>Regenerate with prompt</strong> on step 1, then <strong>Save draft</strong>, then{" "}
                    <strong>Generate translations</strong> for server-side scripts (no MP3s). After approval, use{" "}
                    <strong>Narration</strong> → <strong>Generate audio</strong>.
                  </li>
                </ul>
              </CardContent>
            </Card>
          )}

          {workflowStep === 2 && (
            <Card className="border-2 border-border overflow-hidden">
              <CardHeader className="border-b border-border/50 bg-muted/20">
                <CardTitle className="text-base font-bold">Save &amp; submit</CardTitle>
                <p className="text-sm text-muted-foreground mt-0.5">
                  Save a draft to the library or publish. Then continue the same workflow from the story list (Edit).
                </p>
              </CardHeader>
              <CardContent className="flex flex-wrap gap-3 pt-6">
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
              </CardContent>
            </Card>
          )}

          {workflowStep === 3 && (
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-base font-semibold">Review</CardTitle>
                <p className="text-sm text-muted-foreground">
                  After you submit the story for review from <strong>Edit</strong>, approvers use the{" "}
                  <strong>Story for review</strong> tab.
                </p>
              </CardHeader>
              <CardContent className="flex flex-wrap gap-3">
                <Button variant="default" asChild>
                  <Link href="/dashboard/stories/approve">
                    Open Story for review <ExternalLink className="h-4 w-4 ml-1 opacity-70" />
                  </Link>
                </Button>
                <p className="text-xs text-muted-foreground w-full">
                  Create the story first (step 3 on this page), then submit it from Edit when ready.
                </p>
              </CardContent>
            </Card>
          )}

          {workflowStep === 4 && (
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-base font-semibold">Narration</CardTitle>
                <p className="text-sm text-muted-foreground">
                  Approved stories show under <strong>Narration</strong>. Audio is usually generated automatically after
                  approval.
                </p>
              </CardHeader>
              <CardContent className="flex flex-wrap gap-3">
                <Button variant="outline" asChild>
                  <Link href="/dashboard/stories/to-speech">
                    Open Narration <ExternalLink className="h-4 w-4 ml-1 opacity-70" />
                  </Link>
                </Button>
              </CardContent>
            </Card>
          )}
        </div>

        <div className="space-y-4 lg:sticky lg:top-20 lg:self-start">
          {workflowStep <= 2 && (
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm font-semibold">AI cover notes</CardTitle>
                <p className="text-xs text-muted-foreground font-normal">
                  Shown on Content, Cover &amp; languages, and Save. Used only when you save or publish <strong>without</strong> a cover URL:
                  standard Tamixa cover template first, then your notes.
                </p>
              </CardHeader>
              <CardContent className="space-y-1.5">
                <Label htmlFor="new-story-cover-custom-prompt" className="text-xs font-medium">
                  Custom cover instructions (optional)
                </Label>
                <textarea
                  id="new-story-cover-custom-prompt"
                  className="w-full min-h-[88px] rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                  placeholder="e.g. Warmer evening light; river in the background; keep characters younger…"
                  value={coverCustomPrompt}
                  onChange={(e) => setCoverCustomPrompt(e.target.value.slice(0, 8000))}
                  disabled={submitting || coverGenerating}
                  maxLength={8000}
                />
                <p className="text-xs text-muted-foreground">
                  {coverCustomPrompt.length}/8000 characters
                </p>
              </CardContent>
            </Card>
          )}
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-sm font-semibold">On this page</CardTitle>
            </CardHeader>
            <CardContent className="text-xs text-muted-foreground space-y-2">
              {workflowStep <= 2 ? (
                <>
                  <p>
                    <strong>Content</strong> → <strong>Cover &amp; languages</strong> → <strong>Save &amp; submit</strong>
                  </p>
                  <p>Steps 4–5 are a preview of tabs you will use after the story is created.</p>
                </>
              ) : (
                <p>
                  Go back to step <strong>Save &amp; submit</strong> to create the story, or open <strong>Stories</strong>{" "}
                  if you already saved it.
                </p>
              )}
            </CardContent>
          </Card>
          <Button variant="outline" size="sm" className="w-full rounded-xl" asChild>
            <Link href="/dashboard/stories">All stories</Link>
          </Button>
        </div>
      </div>

      <StoryWorkflowStepFooter
        currentStep={workflowStep}
        totalSteps={WORKFLOW_STEP_COUNT}
        onBack={() => setWorkflowStep(workflowStep - 1)}
        onNext={goNextStep}
        nextLabel={workflowStep === 0 ? "Cover & languages" : workflowStep === 1 ? "Save & submit" : "Next step"}
      />
    </div>
  );
}
