"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import type {
  ShortContentDto,
  ShortContentPage,
  GeneratedShortContentItem,
} from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/empty-state";
import { Lightbulb, Plus, Pencil, Trash2, Sparkles } from "lucide-react";
import { useActionResult } from "@/contexts/action-result-context";
import { Label } from "@/components/ui/label";

const SHORT_CONTENT_TYPES = [
  "RIDDLE",
  "THOUGHT_FOR_THE_DAY",
  "PROVERB",
  "TONGUE_TWISTER",
  "JOKE",
  "FUN_FACT",
  "WORD_OF_THE_DAY",
  "BRAIN_TEASER",
  "AFFIRMATION",
  "QUOTE",
  "DID_YOU_KNOW",
  "RHYME",
  "TRIVIA",
] as const;

const LANGUAGES = [
  { value: "ta", label: "Tamil" },
  { value: "en", label: "English" },
  { value: "hi", label: "Hindi" },
  { value: "te", label: "Telugu" },
  { value: "kn", label: "Kannada" },
  { value: "ml", label: "Malayalam" },
];

function truncate(s: string, max: number) {
  if (!s) return "";
  return s.length <= max ? s : s.slice(0, max) + "…";
}

export default function ShortContentPage() {
  const { showSuccess, showError } = useActionResult();
  const [listPage, setListPage] = useState<ShortContentPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterType, setFilterType] = useState<string>("");
  const [filterLanguage, setFilterLanguage] = useState<string>("");
  const [filterStatus, setFilterStatus] = useState<string>("");
  const [createOpen, setCreateOpen] = useState(false);
  const [editRow, setEditRow] = useState<ShortContentDto | null>(null);
  const [deleteId, setDeleteId] = useState<number | null>(null);
  const [generateOpen, setGenerateOpen] = useState(false);
  const [saving, setSaving] = useState(false);

  const [formType, setFormType] = useState<string>("RIDDLE");
  const [formContent, setFormContent] = useState("");
  const [formAnswer, setFormAnswer] = useState("");
  const [formLanguage, setFormLanguage] = useState("ta");
  const [formStatus, setFormStatus] = useState("DRAFT");

  const [genType, setGenType] = useState("RIDDLE");
  const [genLanguage, setGenLanguage] = useState("ta");
  const [genCount, setGenCount] = useState(5);
  const [generating, setGenerating] = useState(false);
  const [generatedItems, setGeneratedItems] = useState<GeneratedShortContentItem[]>([]);
  const [addingGeneratedId, setAddingGeneratedId] = useState<number | null>(null);

  const loadPage = useCallback(
    (pageNum = 0) => {
      setLoading(true);
      setError(null);
      api.admin
        .getShortContent({
          type: filterType || undefined,
          language: filterLanguage || undefined,
          status: filterStatus || undefined,
          page: pageNum,
          size: 20,
        })
        .then(setListPage)
        .catch((e) => setError(e.message))
        .finally(() => setLoading(false));
    },
    [filterType, filterLanguage, filterStatus]
  );

  useEffect(() => {
    loadPage(0);
  }, [loadPage]);

  const resetForm = () => {
    setFormType("RIDDLE");
    setFormContent("");
    setFormAnswer("");
    setFormLanguage("ta");
    setFormStatus("DRAFT");
  };

  const openCreate = () => {
    resetForm();
    setCreateOpen(true);
  };

  const openEdit = (row: ShortContentDto) => {
    setFormType(row.type);
    setFormContent(row.content);
    setFormAnswer(row.answer ?? "");
    setFormLanguage(row.language);
    setFormStatus(row.status);
    setEditRow(row);
  };

  const handleCreate = async () => {
    if (!formContent.trim()) {
      showError("Validation", "Content is required.");
      return;
    }
    setSaving(true);
    try {
      await api.admin.createShortContent({
        type: formType,
        content: formContent.trim(),
        answer: formAnswer.trim() || null,
        language: formLanguage,
        status: formStatus,
      });
      showSuccess("Short content created", "Item has been added.");
      setCreateOpen(false);
      loadPage(0);
    } catch (e) {
      showError("Create failed", e instanceof Error ? e.message : "Failed to create.");
    } finally {
      setSaving(false);
    }
  };

  const handleUpdate = async () => {
    if (!editRow) return;
    if (!formContent.trim()) {
      showError("Validation", "Content is required.");
      return;
    }
    setSaving(true);
    try {
      await api.admin.updateShortContent(editRow.id, {
        type: formType,
        content: formContent.trim(),
        answer: formAnswer.trim() || null,
        language: formLanguage,
        status: formStatus,
      });
      showSuccess("Short content updated", "Item has been saved.");
      setEditRow(null);
      loadPage(listPage?.number ?? 0);
    } catch (e) {
      showError("Update failed", e instanceof Error ? e.message : "Failed to update.");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (deleteId == null) return;
    setSaving(true);
    try {
      await api.admin.deleteShortContent(deleteId);
      showSuccess("Short content deleted", "Item has been removed.");
      setDeleteId(null);
      loadPage(listPage?.number ?? 0);
    } catch (e) {
      showError("Delete failed", e instanceof Error ? e.message : "Failed to delete.");
    } finally {
      setSaving(false);
    }
  };

  const handleGenerate = async () => {
    setGenerating(true);
    setGeneratedItems([]);
    try {
      const res = await api.admin.generateShortContent({
        type: genType,
        language: genLanguage,
        count: genCount,
      });
      setGeneratedItems(res.items ?? []);
      if ((res.items?.length ?? 0) === 0) {
        showError("Generation", "No items generated. Check OpenAI API key and try again.");
      }
    } catch (e) {
      showError("Generate failed", e instanceof Error ? e.message : "Failed to generate.");
    } finally {
      setGenerating(false);
    }
  };

  const addGeneratedItem = async (item: GeneratedShortContentItem, index: number) => {
    setAddingGeneratedId(index);
    try {
      await api.admin.createShortContent({
        type: genType,
        content: item.content,
        answer: item.answer ?? null,
        language: genLanguage,
        status: "DRAFT",
      });
      showSuccess("Added", "Item added as draft.");
      setGeneratedItems((prev) => prev.filter((_, i) => i !== index));
      loadPage(0);
    } catch (e) {
      showError("Add failed", e instanceof Error ? e.message : "Failed to add.");
    } finally {
      setAddingGeneratedId(null);
    }
  };

  const addAllGenerated = async () => {
    for (let i = 0; i < generatedItems.length; i++) {
      const item = generatedItems[i];
      await api.admin.createShortContent({
        type: genType,
        content: item.content,
        answer: item.answer ?? null,
        language: genLanguage,
        status: "DRAFT",
      });
    }
    showSuccess("Added all", `${generatedItems.length} items added as draft.`);
    setGeneratedItems([]);
    loadPage(0);
  };

  const items = listPage?.content ?? [];
  const isEmpty = !loading && items.length === 0 && !error;

  return (
    <div className="space-y-8">
      <div className="rounded-2xl border-2 border-border bg-gradient-to-r from-primary/5 via-primary/[0.03] to-transparent p-4 sm:p-6">
        <h1 className="page-header">Short content</h1>
        <p className="page-subheader">
          Manage riddles, thought for the day, proverbs, tongue twisters, jokes, and more. Use &quot;Generate with AI&quot; to create new items.
        </p>
      </div>

      <Card className="border-2 border-border overflow-hidden">
        <CardHeader className="card-header-responsive border-b border-border/50 bg-muted/20 space-y-0">
          <CardTitle className="text-base font-bold flex items-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-tamixa-purple/15 text-tamixa-purple">
              <Lightbulb className="h-5 w-5" />
            </div>
            All short content
          </CardTitle>
          <div className="flex flex-wrap gap-2">
            <Button onClick={() => setGenerateOpen(true)} variant="outline" className="rounded-xl" size="sm">
              <Sparkles className="mr-2 h-4 w-4" />
              Generate with AI
            </Button>
            <Button onClick={openCreate} className="rounded-xl" size="sm">
              <Plus className="mr-2 h-4 w-4" />
              Add manually
            </Button>
          </div>
        </CardHeader>
        <CardContent className="p-4">
          <div className="flex flex-wrap gap-3 mb-4">
            <Select value={filterType || "all"} onValueChange={(v) => setFilterType(v === "all" ? "" : v)}>
              <SelectTrigger className="w-[180px] rounded-xl">
                <SelectValue placeholder="Type" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All types</SelectItem>
                {SHORT_CONTENT_TYPES.map((t) => (
                  <SelectItem key={t} value={t}>
                    {t.replace(/_/g, " ")}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select value={filterLanguage || "all"} onValueChange={(v) => setFilterLanguage(v === "all" ? "" : v)}>
              <SelectTrigger className="w-[140px] rounded-xl">
                <SelectValue placeholder="Language" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All languages</SelectItem>
                {LANGUAGES.map((l) => (
                  <SelectItem key={l.value} value={l.value}>
                    {l.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select value={filterStatus || "all"} onValueChange={(v) => setFilterStatus(v === "all" ? "" : v)}>
              <SelectTrigger className="w-[120px] rounded-xl">
                <SelectValue placeholder="Status" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All status</SelectItem>
                <SelectItem value="DRAFT">Draft</SelectItem>
                <SelectItem value="PUBLISHED">Published</SelectItem>
              </SelectContent>
            </Select>
            <Button variant="secondary" size="sm" onClick={() => loadPage(0)} className="rounded-xl">
              Apply
            </Button>
          </div>
          {error && (
            <p className="text-sm font-medium text-destructive mb-4">{error}</p>
          )}
          {loading && !listPage ? (
            <Skeleton className="h-48 w-full rounded-xl" />
          ) : isEmpty ? (
            <EmptyState
              icon={Lightbulb}
              title="No short content"
              description="Add items manually or generate with AI (riddles, proverbs, thought for the day, etc.)."
              action={{ label: "Add manually", onClick: openCreate }}
            />
          ) : (
            <Table>
              <TableHeader>
                <TableRow className="hover:bg-transparent">
                  <TableHead className="font-semibold">Type</TableHead>
                  <TableHead className="font-semibold">Content</TableHead>
                  <TableHead className="font-semibold">Answer</TableHead>
                  <TableHead className="font-semibold">Language</TableHead>
                  <TableHead className="font-semibold">Status</TableHead>
                  <TableHead className="font-semibold w-[120px]">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {items.map((row) => (
                  <TableRow key={row.id} className="transition-colors hover:bg-muted/30">
                    <TableCell className="font-medium text-muted-foreground">{row.type.replace(/_/g, " ")}</TableCell>
                    <TableCell className="max-w-[280px]">{truncate(row.content, 80)}</TableCell>
                    <TableCell className="max-w-[140px] text-muted-foreground">{truncate(row.answer ?? "", 40)}</TableCell>
                    <TableCell>{row.language}</TableCell>
                    <TableCell>
                      <Badge variant={row.status === "PUBLISHED" ? "default" : "secondary"} className="rounded-lg">
                        {row.status}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-2">
                        <Button variant="outline" size="sm" className="rounded-lg" onClick={() => openEdit(row)}>
                          <Pencil className="h-3 w-3" />
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          className="rounded-lg text-destructive hover:text-destructive"
                          onClick={() => setDeleteId(row.id)}
                        >
                          <Trash2 className="h-3 w-3" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
          {listPage && listPage.totalPages > 1 && (
            <div className="flex items-center justify-between mt-4">
              <p className="text-sm text-muted-foreground">
                Page {listPage.number + 1} of {listPage.totalPages} ({listPage.totalElements} total)
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={listPage.first}
                  onClick={() => loadPage(listPage.number - 1)}
                  className="rounded-xl"
                >
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={listPage.last}
                  onClick={() => loadPage(listPage.number + 1)}
                  className="rounded-xl"
                >
                  Next
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Create dialog */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="sm:max-w-lg rounded-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>New short content</DialogTitle>
            <DialogDescription>
              Add a riddle, proverb, thought for the day, tongue twister, or other type. Set status to Published to show in the app.
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label>Type</Label>
              <Select value={formType} onValueChange={setFormType}>
                <SelectTrigger className="rounded-xl">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {SHORT_CONTENT_TYPES.map((t) => (
                    <SelectItem key={t} value={t}>
                      {t.replace(/_/g, " ")}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-2">
              <Label htmlFor="create-content">Content</Label>
              <textarea
                id="create-content"
                value={formContent}
                onChange={(e) => setFormContent(e.target.value)}
                placeholder="Main text..."
                rows={3}
                className="flex min-h-[80px] w-full rounded-xl border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="create-answer">Answer (optional, for riddles/jokes)</Label>
              <Input
                id="create-answer"
                value={formAnswer}
                onChange={(e) => setFormAnswer(e.target.value)}
                placeholder="Answer or punchline"
                className="rounded-xl"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label>Language</Label>
                <Select value={formLanguage} onValueChange={setFormLanguage}>
                  <SelectTrigger className="rounded-xl">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {LANGUAGES.map((l) => (
                      <SelectItem key={l.value} value={l.value}>
                        {l.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-2">
                <Label>Status</Label>
                <Select value={formStatus} onValueChange={setFormStatus}>
                  <SelectTrigger className="rounded-xl">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="DRAFT">Draft</SelectItem>
                    <SelectItem value="PUBLISHED">Published</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateOpen(false)} className="rounded-xl">
              Cancel
            </Button>
            <Button onClick={handleCreate} disabled={saving} className="rounded-xl">
              {saving ? "Creating…" : "Create"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit dialog */}
      <Dialog open={editRow != null} onOpenChange={(open) => !open && setEditRow(null)}>
        <DialogContent className="sm:max-w-lg rounded-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Edit short content</DialogTitle>
            <DialogDescription>Update content, answer, or status.</DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label>Type</Label>
              <Select value={formType} onValueChange={setFormType}>
                <SelectTrigger className="rounded-xl">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {SHORT_CONTENT_TYPES.map((t) => (
                    <SelectItem key={t} value={t}>
                      {t.replace(/_/g, " ")}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-2">
              <Label htmlFor="edit-content">Content</Label>
              <textarea
                id="edit-content"
                value={formContent}
                onChange={(e) => setFormContent(e.target.value)}
                rows={3}
                className="flex min-h-[80px] w-full rounded-xl border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="edit-answer">Answer (optional)</Label>
              <Input
                id="edit-answer"
                value={formAnswer}
                onChange={(e) => setFormAnswer(e.target.value)}
                className="rounded-xl"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label>Language</Label>
                <Select value={formLanguage} onValueChange={setFormLanguage}>
                  <SelectTrigger className="rounded-xl">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {LANGUAGES.map((l) => (
                      <SelectItem key={l.value} value={l.value}>
                        {l.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-2">
                <Label>Status</Label>
                <Select value={formStatus} onValueChange={setFormStatus}>
                  <SelectTrigger className="rounded-xl">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="DRAFT">Draft</SelectItem>
                    <SelectItem value="PUBLISHED">Published</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditRow(null)} className="rounded-xl">
              Cancel
            </Button>
            <Button onClick={handleUpdate} disabled={saving} className="rounded-xl">
              {saving ? "Saving…" : "Save"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete confirm */}
      <Dialog open={deleteId != null} onOpenChange={(open) => !open && setDeleteId(null)}>
        <DialogContent className="sm:max-w-md rounded-2xl">
          <DialogHeader>
            <DialogTitle>Delete short content</DialogTitle>
            <DialogDescription>This cannot be undone. The item will be removed from the library.</DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteId(null)} className="rounded-xl">
              Cancel
            </Button>
            <Button variant="destructive" onClick={handleDelete} disabled={saving} className="rounded-xl">
              {saving ? "Deleting…" : "Delete"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Generate with AI dialog */}
      <Dialog open={generateOpen} onOpenChange={setGenerateOpen}>
        <DialogContent className="sm:max-w-2xl rounded-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Generate with AI</DialogTitle>
            <DialogDescription>
              Choose type and language. Generated items are added as Draft; you can edit and publish from the table.
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid grid-cols-3 gap-4">
              <div className="grid gap-2">
                <Label>Type</Label>
                <Select value={genType} onValueChange={setGenType}>
                  <SelectTrigger className="rounded-xl">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {SHORT_CONTENT_TYPES.map((t) => (
                      <SelectItem key={t} value={t}>
                        {t.replace(/_/g, " ")}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-2">
                <Label>Language</Label>
                <Select value={genLanguage} onValueChange={setGenLanguage}>
                  <SelectTrigger className="rounded-xl">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {LANGUAGES.map((l) => (
                      <SelectItem key={l.value} value={l.value}>
                        {l.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="grid gap-2">
                <Label>Count (1–20)</Label>
                <Input
                  type="number"
                  min={1}
                  max={20}
                  value={genCount}
                  onChange={(e) => setGenCount(Math.min(20, Math.max(1, Number(e.target.value) || 1)))}
                  className="rounded-xl"
                />
              </div>
            </div>
            <Button onClick={handleGenerate} disabled={generating} className="rounded-xl w-full">
              {generating ? "Generating…" : "Generate"}
            </Button>
            {generatedItems.length > 0 && (
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <Label>Generated ({generatedItems.length})</Label>
                  <Button variant="secondary" size="sm" onClick={addAllGenerated} className="rounded-xl">
                    Add all as draft
                  </Button>
                </div>
                <ul className="space-y-2 max-h-64 overflow-y-auto border rounded-xl p-3 bg-muted/20">
                  {generatedItems.map((item, i) => (
                    <li key={i} className="flex flex-col gap-1 rounded-lg border border-border/50 p-2 bg-background">
                      <p className="text-sm">{item.content}</p>
                      {item.answer && (
                        <p className="text-xs text-muted-foreground">Answer: {item.answer}</p>
                      )}
                      <Button
                        variant="outline"
                        size="sm"
                        className="w-fit rounded-lg"
                        onClick={() => addGeneratedItem(item, i)}
                        disabled={addingGeneratedId === i}
                      >
                        {addingGeneratedId === i ? "Adding…" : "Add as draft"}
                      </Button>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
