"use client";

import { Info } from "lucide-react";
import { cn } from "@/lib/utils";

/**
 * Short, scannable reference for the interactive graph JSON shape (Learn · Simulator).
 */
export function InteractiveGraphSchemaHint({ className }: { className?: string }) {
  return (
    <div
      className={cn(
        "w-full min-w-0 rounded-lg border border-emerald-500/25 bg-emerald-500/[0.04] dark:border-emerald-500/20 dark:bg-emerald-950/30 px-3 py-3 space-y-2.5 [writing-mode:horizontal-tb]",
        className
      )}
    >
      <div className="flex flex-row items-start gap-2 text-xs font-semibold text-foreground">
        <Info className="h-3.5 w-3.5 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" aria-hidden />
        <span className="min-w-0 leading-snug">Interactive graph (JSON)</span>
      </div>
      <ul className="text-xs text-muted-foreground space-y-2 list-none m-0 p-0 leading-relaxed text-left [writing-mode:horizontal-tb]">
        <li className="min-w-0 break-normal">
          <span className="font-medium text-foreground/90">Start — </span>
          Set{" "}
          <code className="rounded bg-muted/90 dark:bg-muted/50 px-1 py-px text-[11px] font-mono">
            startSegmentId
          </code>{" "}
          to the id of the first segment listeners play.
        </li>
        <li className="min-w-0 break-normal">
          <span className="font-medium text-foreground/90">Segments — </span>
          A{" "}
          <code className="rounded bg-muted/90 dark:bg-muted/50 px-1 py-px text-[11px] font-mono">segments</code>{" "}
          object maps id → segment. Each segment usually has spoken{" "}
          <code className="rounded bg-muted/90 dark:bg-muted/50 px-1 py-px text-[11px] font-mono">text</code>, an{" "}
          <code className="rounded bg-muted/90 dark:bg-muted/50 px-1 py-px text-[11px] font-mono">audioUrl</code>{" "}
          (MP3), and optional{" "}
          <code className="rounded bg-muted/90 dark:bg-muted/50 px-1 py-px text-[11px] font-mono">choices</code>{" "}
          with{" "}
          <code className="rounded bg-muted/90 dark:bg-muted/50 px-1 py-px text-[11px] font-mono">id</code>,{" "}
          <code className="rounded bg-muted/90 dark:bg-muted/50 px-1 py-px text-[11px] font-mono">label</code>,{" "}
          <code className="rounded bg-muted/90 dark:bg-muted/50 px-1 py-px text-[11px] font-mono">
            nextSegmentId
          </code>
          , and optional{" "}
          <code className="rounded bg-muted/90 dark:bg-muted/50 px-1 py-px text-[11px] font-mono">
            skillDeltas
          </code>
          .
        </li>
        <li className="min-w-0 break-normal">
          <span className="font-medium text-foreground/90">URLs — </span>
          Prefer HTTPS MP3 in production.{" "}
          <code className="rounded bg-muted/90 dark:bg-muted/50 px-1 py-px text-[11px] font-mono">http://</code> is
          acceptable on localhost or a private LAN for local dev.
        </li>
      </ul>
    </div>
  );
}
