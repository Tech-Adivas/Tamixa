"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { api } from "@/lib/api";
import type { StoryVisualImage, StoryVisuals } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { useActionResult } from "@/contexts/action-result-context";

/** While a slideshow is being generated, refresh this often. */
const POLL_MS = 5000;

/**
 * Story slideshow review: generate the AI pictures for a library story, check each one, redraw any that are wrong,
 * and approve. Children only see the slideshow after approval.
 */
export default function StorySlideshowPage() {
  const params = useParams<{ id: string }>();
  const id = Number(params.id);
  const { showSuccess, showError } = useActionResult();
  const [visuals, setVisuals] = useState<StoryVisuals | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [confirm, setConfirm] = useState<"generate" | "delete" | null>(null);
  const [showShots, setShowShots] = useState(false);

  const load = useCallback(async () => {
    try {
      setVisuals(await api.admin.getStoryVisuals(id));
    } catch (e) {
      showError("Could not load slideshow", e instanceof Error ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, [id, showError]);

  useEffect(() => {
    void load();
  }, [load]);

  const inProgress = !!visuals && (visuals.running || visuals.status === "PLANNING" || visuals.status === "GENERATING");
  useEffect(() => {
    if (!inProgress) return;
    const timer = setInterval(() => void load(), POLL_MS);
    return () => clearInterval(timer);
  }, [inProgress, load]);

  const run = async (label: string, action: () => Promise<StoryVisuals | void>, done?: string) => {
    setBusy(true);
    try {
      const result = await action();
      if (result) setVisuals(result);
      else await load();
      if (done) showSuccess(label, done);
    } catch (e) {
      showError(`${label} failed`, e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  const readyCount = visuals?.images.filter((i) => i.status === "READY").length ?? 0;

  return (
    <div className="space-y-6">
      <div>
        <Link href={`/dashboard/stories/${id}/edit`} className="text-sm text-muted-foreground hover:underline">
          ← Back to story #{id}
        </Link>
        <h1 className="page-header mt-2">Story slideshow</h1>
        <p className="page-subheader mt-1">
          AI pictures shown during narration, each filmed as a few camera shots (wide, close-up, pan) that start exactly
          when the narrator reaches that part. About one picture per minute of narration (≈ $0.05 each). Pictures are
          shared by every language and voice. <strong>Children see the slideshow only after you approve it.</strong>
        </p>
      </div>

      <Card className="border-border shadow-sm">
        <CardHeader className="border-b bg-muted/30 px-4 py-3 sm:px-6 sm:py-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex flex-wrap items-center gap-2">
              <CardTitle className="text-base font-semibold tracking-tight">Status</CardTitle>
              {visuals ? (
                <>
                  <Badge variant={visuals.status === "FAILED" ? "destructive" : visuals.status === "READY" ? "default" : "secondary"}>
                    {inProgress ? `${visuals.status === "PLANNING" ? "Planning" : "Drawing pictures"}…` : visuals.status}
                  </Badge>
                  <Badge variant={visuals.approved ? "default" : "outline"}>
                    {visuals.approved ? "Approved — visible in the app" : "Not approved — hidden from children"}
                  </Badge>
                </>
              ) : (
                !loading && <Badge variant="outline">No slideshow yet</Badge>
              )}
            </div>
            <div className="flex flex-wrap gap-2">
              <Button size="sm" disabled={busy || inProgress} onClick={() => setConfirm("generate")}>
                {visuals ? "Regenerate all" : "Generate slideshow"}
              </Button>
              {visuals && !visuals.approved && (
                <Button
                  size="sm"
                  variant="outline"
                  disabled={busy || inProgress || visuals.status !== "READY"}
                  onClick={() => run("Approve", () => api.admin.approveStoryVisuals(id), "The slideshow is now shown in the app.")}
                >
                  Approve
                </Button>
              )}
              {visuals?.approved && (
                <Button
                  size="sm"
                  variant="outline"
                  disabled={busy}
                  onClick={() => run("Unapprove", () => api.admin.unapproveStoryVisuals(id), "The slideshow is hidden from the app.")}
                >
                  Unapprove
                </Button>
              )}
              {visuals && (
                <Button size="sm" variant="outline" disabled={busy || inProgress} onClick={() => setConfirm("delete")}>
                  Delete
                </Button>
              )}
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-2 px-4 py-4 sm:px-6 text-sm">
          {loading && <p className="text-muted-foreground">Loading…</p>}
          {!loading && !visuals && (
            <p className="text-muted-foreground">
              This story has no slideshow. The app shows the cover with subtitles until one is generated and approved.
              Generate after the narration is ready (shot times follow the narration).
            </p>
          )}
          {visuals && (
            <>
              <p>
                Pictures: <strong>{readyCount}</strong> / {visuals.images.length} ready · Camera shots:{" "}
                <strong>{visuals.shots.length}</strong> · Source language: {visuals.sourceLanguage}
              </p>
              {inProgress && <p className="text-muted-foreground">Generating in the background; this page updates by itself (usually 3–5 minutes).</p>}
              {visuals.lastError && <p className="text-destructive">{visuals.lastError}</p>}
            </>
          )}
        </CardContent>
      </Card>

      {visuals?.characterSheetUrl && (
        <Card className="border-border shadow-sm">
          <CardHeader className="border-b bg-muted/30 px-4 py-3 sm:px-6">
            <CardTitle className="text-base font-semibold tracking-tight">Characters</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-4 px-4 py-4 sm:flex-row sm:px-6">
            {/* eslint-disable-next-line @next/next/no-img-element -- backend proxy URL, sizes vary */}
            <img src={visuals.characterSheetUrl} alt="Character reference sheet" className="w-full max-w-xs rounded-lg border" />
            <ul className="space-y-2 text-sm">
              {visuals.characters?.map((c) => (
                <li key={c.name}>
                  <strong>{c.name}</strong>
                  {c.appearance ? ` — ${c.appearance}` : ""}
                  {c.outfits && Object.keys(c.outfits).length > 0 && (
                    <span className="text-muted-foreground">
                      {" "}
                      ({Object.entries(c.outfits).map(([k, v]) => `${k}: ${v}`).join("; ")})
                    </span>
                  )}
                </li>
              ))}
              <li className="text-muted-foreground">Every picture is drawn from this sheet so characters look the same.</li>
            </ul>
          </CardContent>
        </Card>
      )}

      {visuals && visuals.images.length > 0 && (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {visuals.images.map((image) => (
            <PictureCard
              key={image.imageKey}
              image={image}
              disabled={busy || inProgress}
              onRegenerate={(prompt) =>
                run(
                  `Redraw ${image.imageKey}`,
                  () => api.admin.regenerateStoryVisualImage(id, image.imageKey, prompt),
                  "Redrawing in the background; it updates here when done."
                )
              }
            />
          ))}
        </div>
      )}

      {visuals && visuals.shots.length > 0 && (
        <Card className="border-border shadow-sm">
          <CardHeader className="border-b bg-muted/30 px-4 py-3 sm:px-6">
            <button type="button" className="text-left" onClick={() => setShowShots((v) => !v)}>
              <CardTitle className="text-base font-semibold tracking-tight">
                Camera shots ({visuals.shots.length}) {showShots ? "▾" : "▸"}
              </CardTitle>
            </button>
          </CardHeader>
          {showShots && (
            <CardContent className="px-4 py-4 sm:px-6">
              <ol className="space-y-1 text-sm">
                {visuals.shots.map((s) => (
                  <li key={s.shotIndex} className="flex flex-wrap gap-2">
                    <span className="w-8 text-muted-foreground">{s.shotIndex + 1}.</span>
                    <span className="font-medium">{s.imageKey}</span>
                    <span className="text-muted-foreground">
                      {s.camera}
                      {s.focus ? ` on ${s.focus}` : ""} · {s.movement.replaceAll("_", " ")}
                    </span>
                    <span className="truncate">“{s.narrationStart}…”</span>
                  </li>
                ))}
              </ol>
            </CardContent>
          )}
        </Card>
      )}

      <ConfirmDialog
        open={confirm === "generate"}
        onOpenChange={(open) => !open && setConfirm(null)}
        title={visuals ? "Regenerate the whole slideshow?" : "Generate slideshow?"}
        description={
          (visuals ? "This replaces every picture and shot, and the slideshow must be approved again. " : "") +
          "It plans the shots and draws about one picture per minute of narration (≈ $0.05 per picture), in 3–5 minutes."
        }
        confirmLabel={visuals ? "Regenerate all" : "Generate"}
        loading={busy}
        onConfirm={async () => {
          setConfirm(null);
          await run("Generate slideshow", () => api.admin.generateStoryVisuals(id), "Started. This page updates as pictures are drawn.");
        }}
      />
      <ConfirmDialog
        open={confirm === "delete"}
        onOpenChange={(open) => !open && setConfirm(null)}
        title="Delete the slideshow?"
        description="The app goes back to showing the cover. You can generate a new slideshow later."
        confirmLabel="Delete"
        variant="destructive"
        loading={busy}
        onConfirm={async () => {
          setConfirm(null);
          await run("Delete slideshow", () => api.admin.deleteStoryVisuals(id), "Slideshow deleted.");
        }}
      />
    </div>
  );
}

function PictureCard({
  image,
  disabled,
  onRegenerate,
}: {
  image: StoryVisualImage;
  disabled: boolean;
  onRegenerate: (prompt: string | null) => void;
}) {
  const [editing, setEditing] = useState(false);
  const [prompt, setPrompt] = useState(image.prompt);
  useEffect(() => setPrompt(image.prompt), [image.prompt]);

  return (
    <Card className="overflow-hidden border-border shadow-sm">
      <div className="relative aspect-[2/3] bg-muted">
        {image.imageUrl && image.status === "READY" ? (
          // eslint-disable-next-line @next/next/no-img-element -- backend proxy URL
          <img src={image.imageUrl} alt={image.storyMoment ?? image.imageKey} className="h-full w-full object-cover" />
        ) : (
          <div className="flex h-full items-center justify-center p-4 text-center text-sm text-muted-foreground">
            {image.status === "FAILED" ? image.lastError ?? "Failed" : "Drawing…"}
          </div>
        )}
        <Badge className="absolute left-2 top-2" variant={image.status === "FAILED" ? "destructive" : "secondary"}>
          {image.imageKey} · {image.shotCount} shots
        </Badge>
      </div>
      <CardContent className="space-y-2 px-4 py-3 text-sm">
        <p className="font-medium">{image.storyMoment ?? "—"}</p>
        {image.subjects && image.subjects.length > 0 && (
          <p className="text-muted-foreground">{image.subjects.map((s) => `${s.name} (${s.region.replaceAll("_", " ")})`).join(" · ")}</p>
        )}
        {editing && (
          <textarea
            className="w-full min-h-[140px] rounded-md border border-input bg-background px-3 py-2 text-xs"
            value={prompt}
            maxLength={4000}
            onChange={(e) => setPrompt(e.target.value)}
          />
        )}
        <div className="flex flex-wrap gap-2">
          <Button
            size="sm"
            variant="outline"
            disabled={disabled}
            onClick={() => {
              onRegenerate(editing && prompt.trim() !== image.prompt ? prompt : null);
              setEditing(false);
            }}
          >
            Redraw
          </Button>
          <Button size="sm" variant="ghost" disabled={disabled} onClick={() => setEditing((v) => !v)}>
            {editing ? "Cancel edit" : "Edit description"}
          </Button>
        </div>
      </CardContent>
    </Card>
  );
}
