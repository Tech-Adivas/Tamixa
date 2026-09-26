/**
 * Pipeline Status Badge Component
 * 
 * Displays per-language pipeline status with visual indicators for:
 * - Translation progress
 * - Rewriting progress
 * - TTS processing
 * - Completion status
 * - Error states
 * 
 * Color coding:
 * - PENDING: Gray (not started)
 * - TRANSLATING/REWRITING/TTS_PROCESSING: Orange (in progress, animated)
 * - COMPLETED: Green (success)
 * - FAILED: Red (error with message)
 */

import * as React from "react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import type { PipelineStatusResponse } from "@/types/api";
import {
  isLibraryStoryPipelineMetaKey,
  adminStoryLanguageLabel,
  ADMIN_STORY_LANGUAGE_SHORT,
} from "@/lib/library-story-workflow";

export interface PipelineStatusBadgesProps {
  status?: PipelineStatusResponse | Record<string, string | undefined> | null;
  compact?: boolean;
  showProgress?: boolean;
  className?: string;
}

/** Parse "STATUS" or "STATUS — error message" from pipeline value */
function parsePipelineStatus(value: string): { status: string; error?: string } {
  if (!value) return { status: value ?? "" };
  const idx = value.indexOf(" — ");
  if (idx === -1) return { status: value };
  return { status: value.slice(0, idx).trim(), error: value.slice(idx + 3).trim() };
}

/**
 * Display pipeline status badges for all languages
 */
export function PipelineStatusBadges({
  status,
  compact = false,
  showProgress = true,
  className,
}: PipelineStatusBadgesProps) {
  if (!status || Object.keys(status).length === 0) {
    return <span className="text-muted-foreground text-xs">—</span>;
  }

  const processingLang = status["processing"];
  const entries = Object.entries(status).filter(
    ([k]) => !isLibraryStoryPipelineMetaKey(k)
  );
  const total = entries.length;
  const completed = entries.filter(([, s]) => s === "COMPLETED").length;
  const failed = entries.filter(([, s]) => s?.includes("FAILED")).length;

  const isInProgressStatus = (s: string) =>
    s === "PENDING" ||
    s === "TRANSLATING" ||
    s === "REWRITING" ||
    s === "TTS_PROCESSING" ||
    s?.startsWith("TRANSLATING") ||
    s?.startsWith("REWRITING") ||
    s?.startsWith("TTS_PROCESSING");

  const inProgress = entries.some(([, s]) => isInProgressStatus(s ?? ""));
  const currentStep = entries.find(([, s]) => isInProgressStatus(s ?? ""));
  const baseStatus = (s: string) => (s?.split(" — ")[0] ?? s ?? "").replace("_", " ");
  const processingStage = processingLang ? status[processingLang]?.split(" — ")[0] : null;

  const progressLabel =
    completed === total
      ? "Complete"
      : failed > 0 && completed === 0
        ? "Failed"
        : inProgress
          ? processingLang
            ? processingStage === "TRANSLATING"
              ? `Translating ${ADMIN_STORY_LANGUAGE_SHORT[processingLang] || processingLang}…`
              : processingStage === "REWRITING"
                ? `Rewriting ${ADMIN_STORY_LANGUAGE_SHORT[processingLang] || processingLang}…`
                : processingStage === "TTS_PROCESSING"
                  ? `Generating ${ADMIN_STORY_LANGUAGE_SHORT[processingLang] || processingLang} audio…`
                  : `${ADMIN_STORY_LANGUAGE_SHORT[processingLang] || processingLang}: ${baseStatus(processingStage ?? "")}…`
            : currentStep
              ? `${ADMIN_STORY_LANGUAGE_SHORT[currentStep[0]] || currentStep[0]}: ${baseStatus(currentStep[1] ?? "")}…`
              : "Starting…"
          : null;

  return (
    <div className={cn("flex min-w-0 max-w-full flex-col gap-1.5", className)}>
      {showProgress && progressLabel && (
        <span className="text-[10px] text-muted-foreground font-medium">
          {progressLabel}
        </span>
      )}
      <div className="flex flex-wrap gap-1">
        {entries.map(([lang, stage]) => {
          const isFailed = stage?.includes("FAILED");
          const isDone = stage === "COMPLETED";
          const isPending = stage === "PENDING";
          const isActive =
            stage === "TRANSLATING" ||
            stage === "REWRITING" ||
            stage === "TTS_PROCESSING";
          const isOrange = isPending || isActive;
          const { status: statusText, error } = parsePipelineStatus(stage ?? "");
          const label = compact
            ? ADMIN_STORY_LANGUAGE_SHORT[lang] || lang.toUpperCase()
            : adminStoryLanguageLabel(lang);
          const badgeText = isDone
            ? label
            : isFailed && statusText
              ? `${label} — ${statusText}`
              : `${label} ${stage ?? ""}`.trim();

          return (
            <Badge
              key={lang}
              variant="outline"
              className={cn(
                "text-[10px] px-1.5 py-0 transition-all duration-300 font-medium",
                isFailed &&
                  "border-red-500/60 bg-red-500/15 text-red-700 dark:text-red-400",
                isOrange &&
                  "border-orange-500/60 bg-orange-500/15 text-orange-700 dark:text-orange-400",
                isActive && "animate-pulse",
                isDone &&
                  "border-emerald-500/60 bg-emerald-500/15 text-emerald-700 dark:text-emerald-400"
              )}
              title={
                error
                  ? `${statusText} — ${error}`
                  : isDone
                    ? `${adminStoryLanguageLabel(lang)} completed`
                    : stage ?? undefined
              }
            >
              {badgeText}
            </Badge>
          );
        })}
      </div>
      {failed > 0 && (
        <PipelineErrorSummary entries={entries} />
      )}
    </div>
  );
}

/**
 * Display error summary for failed pipeline stages
 */
function PipelineErrorSummary({
  entries,
}: {
  entries: [string, string | undefined][];
}) {
  const failedEntries = entries.filter(([, stageStatus]) => stageStatus?.includes("FAILED"));

  return (
    <div className="mt-1.5 rounded border border-red-500/30 bg-red-500/5 px-2 py-1.5 space-y-1">
      <span className="text-[10px] font-medium text-red-600 dark:text-red-400">
        Errors:
      </span>
      {failedEntries.map(([lang, stageValue]) => {
        const { status: statusText, error } = parsePipelineStatus(stageValue ?? "");
        return (
          <div key={lang} className="text-[10px]">
            <span className="font-medium">
              {ADMIN_STORY_LANGUAGE_SHORT[lang] || lang}
            </span>
            {statusText && (
              <span className="text-muted-foreground"> — {statusText}</span>
            )}
            {error && (
              <p className="text-destructive pl-1 text-[9px] break-words mt-0.5">
                {error}
              </p>
            )}
          </div>
        );
      })}
    </div>
  );
}

/**
 * Compact pipeline progress indicator (just completion count)
 */
export function PipelineProgressIndicator({
  status,
  className,
}: {
  status?: PipelineStatusResponse | Record<string, string | undefined> | null;
  className?: string;
}) {
  if (!status || Object.keys(status).length === 0) {
    return <span className="text-muted-foreground text-xs">—</span>;
  }

  const entries = Object.entries(status).filter(
    ([k]) => !isLibraryStoryPipelineMetaKey(k)
  );
  const total = entries.length;
  const completed = entries.filter(([, s]) => s === "COMPLETED").length;
  const failed = entries.filter(([, s]) => s?.includes("FAILED")).length;

  const isComplete = completed === total;
  const hasFailed = failed > 0;

  return (
    <Badge
      variant="outline"
      className={cn(
        "text-xs px-2 py-0.5 font-medium",
        isComplete &&
          "border-emerald-500/60 bg-emerald-500/15 text-emerald-700 dark:text-emerald-400",
        hasFailed &&
          "border-red-500/60 bg-red-500/15 text-red-700 dark:text-red-400",
        !isComplete &&
          !hasFailed &&
          "border-orange-500/60 bg-orange-500/15 text-orange-700 dark:text-orange-400",
        className
      )}
    >
      {isComplete ? "Complete" : `${completed} / ${total}`}
    </Badge>
  );
}
