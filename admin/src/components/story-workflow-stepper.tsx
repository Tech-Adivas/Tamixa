"use client";

import { Check } from "lucide-react";
import { cn } from "@/lib/utils";

export type StoryWorkflowStepDef = {
  key: string;
  label: string;
  description?: string;
};

type StoryWorkflowStepperProps = {
  steps: readonly StoryWorkflowStepDef[];
  currentStep: number;
  onStepChange: (index: number) => void;
  className?: string;
};

/**
 * Horizontal workflow stepper for curated library story creation/editing.
 * Steps are clickable to jump; use with Back/Next in the page for linear guidance.
 */
export function StoryWorkflowStepper({
  steps,
  currentStep,
  onStepChange,
  className,
}: StoryWorkflowStepperProps) {
  return (
    <nav
      aria-label="Story workflow steps"
      className={cn(
        "rounded-xl border border-border/60 bg-muted/20 px-2 py-3 sm:px-4 sm:py-4",
        className
      )}
    >
      <ol className="flex items-stretch gap-0 overflow-x-auto pb-1 [-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
        {steps.map((step, i) => {
          const complete = i < currentStep;
          const current = i === currentStep;
          return (
            <li key={step.key} className="flex min-w-0 flex-1 items-center">
              <button
                type="button"
                onClick={() => onStepChange(i)}
                className={cn(
                  "group flex min-w-[7.5rem] flex-1 flex-col items-center gap-1.5 rounded-lg px-2 py-2 text-center transition-colors sm:min-w-0 sm:px-3",
                  current && "bg-background shadow-sm ring-1 ring-primary/25",
                  !current && "hover:bg-muted/50"
                )}
              >
                <span
                  className={cn(
                    "flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-xs font-semibold transition-colors",
                    complete && "bg-primary text-primary-foreground",
                    current && !complete && "bg-primary text-primary-foreground",
                    !current && !complete && "bg-muted text-muted-foreground group-hover:bg-muted/80"
                  )}
                  aria-current={current ? "step" : undefined}
                >
                  {complete ? <Check className="h-4 w-4" strokeWidth={2.5} aria-hidden /> : i + 1}
                </span>
                <span className="w-full min-w-0">
                  <span
                    className={cn(
                      "block truncate text-xs font-medium sm:text-sm",
                      current ? "text-foreground" : "text-muted-foreground"
                    )}
                  >
                    {step.label}
                  </span>
                  {step.description ? (
                    <span className="mt-0.5 line-clamp-2 hidden text-[10px] leading-tight text-muted-foreground sm:block sm:text-xs">
                      {step.description}
                    </span>
                  ) : null}
                </span>
              </button>
              {i < steps.length - 1 ? (
                <div
                  className={cn(
                    "mx-0.5 hidden h-px w-4 shrink-0 sm:block sm:w-6 lg:w-10",
                    i < currentStep ? "bg-primary/50" : "bg-border"
                  )}
                  aria-hidden
                />
              ) : null}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}

type StoryWorkflowStepFooterProps = {
  currentStep: number;
  totalSteps: number;
  onBack: () => void;
  onNext: () => void;
  backLabel?: string;
  nextLabel?: string;
  disableBack?: boolean;
  disableNext?: boolean;
  className?: string;
};

export function StoryWorkflowStepFooter({
  currentStep,
  totalSteps,
  onBack,
  onNext,
  backLabel = "Previous step",
  nextLabel = "Next step",
  disableBack,
  disableNext,
  className,
}: StoryWorkflowStepFooterProps) {
  if (totalSteps <= 1) return null;
  return (
    <div
      className={cn(
        "flex flex-wrap items-center justify-between gap-3 border-t border-border/60 pt-4",
        className
      )}
    >
      <button
        type="button"
        className={cn(
          "text-sm font-medium text-muted-foreground underline-offset-4 hover:text-foreground hover:underline disabled:pointer-events-none disabled:opacity-40"
        )}
        disabled={disableBack ?? currentStep <= 0}
        onClick={onBack}
      >
        {backLabel}
      </button>
      <span className="text-xs text-muted-foreground sm:hidden">
        Step {currentStep + 1} of {totalSteps}
      </span>
      <button
        type="button"
        className={cn(
          "text-sm font-medium text-primary underline-offset-4 hover:underline disabled:pointer-events-none disabled:opacity-40"
        )}
        disabled={disableNext ?? currentStep >= totalSteps - 1}
        onClick={onNext}
      >
        {nextLabel}
      </button>
    </div>
  );
}

/** Full 5-step lifecycle shared by Create and Edit. On Create, steps 3–4 explain what happens after the story exists. */
export const LIBRARY_STORY_WORKFLOW_STEPS: readonly StoryWorkflowStepDef[] = [
  {
    key: "content",
    label: "Content",
    description: "Master language, title, story text, metadata",
  },
  {
    key: "cover-languages",
    label: "Cover & languages",
    description: "Art, other languages, generate scripts",
  },
  {
    key: "submit",
    label: "Save & submit",
    description: "Save draft or send for review",
  },
  {
    key: "review",
    label: "Review",
    description: "Approve in Story for review",
  },
  {
    key: "narration",
    label: "Narration",
    description: "Audio after approval",
  },
] as const;

/** @deprecated Use LIBRARY_STORY_WORKFLOW_STEPS — same 5 steps. */
export const EDIT_LIBRARY_STORY_WORKFLOW_STEPS = LIBRARY_STORY_WORKFLOW_STEPS;
