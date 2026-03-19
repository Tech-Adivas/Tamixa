"use client";

import * as React from "react";
import { cn } from "@/lib/utils";

export interface StoryProgressBarProps
  extends React.HTMLAttributes<HTMLDivElement> {
  value: number;
  max?: number;
  showLabel?: boolean;
  variant?: "default" | "thin" | "thick";
}

const StoryProgressBar = React.forwardRef<HTMLDivElement, StoryProgressBarProps>(
  (
    { value, max = 100, showLabel = false, variant = "default", className, ...props },
    ref
  ) => {
    const percentage = Math.min(100, Math.max(0, (value / max) * 100));

    return (
      <div
        ref={ref}
        className={cn("flex w-full flex-col gap-1", className)}
        role="progressbar"
        aria-valuenow={value}
        aria-valuemin={0}
        aria-valuemax={max}
        aria-label={`Progress: ${Math.round(percentage)}%`}
        {...props}
      >
        <div
          className={cn(
            "w-full overflow-hidden rounded-full bg-muted",
            variant === "thin" && "h-1.5",
            variant === "default" && "h-2",
            variant === "thick" && "h-3"
          )}
        >
          <div
            className={cn(
              "h-full rounded-full bg-primary transition-all duration-standard ease-out-expo",
              variant === "default" && "bg-gradient-to-r from-primary to-tamixa-purple"
            )}
            style={{ width: `${percentage}%` }}
          />
        </div>
        {showLabel && (
          <span className="text-xs font-medium text-muted-foreground">
            {Math.round(percentage)}%
          </span>
        )}
      </div>
    );
  }
);
StoryProgressBar.displayName = "StoryProgressBar";

export { StoryProgressBar };
