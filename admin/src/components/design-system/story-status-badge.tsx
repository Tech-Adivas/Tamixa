/**
 * Story Status Badge Component
 * 
 * Displays unified LibraryStoryStatus with consistent color coding and icons
 * across the admin dashboard.
 * 
 * Status Colors (aligned with design spec):
 * - DRAFT: Gray (muted)
 * - PUBLISHED: Amber (in review queue)
 * - PROCESSING: Blue (pipeline active)
 * - READY: Green (ready for approval)
 * - CHANGES_REQUESTED: Orange (needs revision)
 * - REJECTED: Red (rejected)
 */

import * as React from "react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";
import {
  FileText,
  Clock,
  CheckCircle,
  AlertCircle,
  XCircle,
  ClipboardCheck,
} from "lucide-react";

export type StoryStatus =
  | "DRAFT"
  | "PUBLISHED"
  | "PROCESSING"
  | "READY"
  | "CHANGES_REQUESTED"
  | "REJECTED";

export interface StoryStatusBadgeProps {
  status: StoryStatus;
  narrationApproved?: boolean;
  className?: string;
  showIcon?: boolean;
  size?: "sm" | "md" | "lg";
}

const STATUS_CONFIG: Record<
  StoryStatus,
  {
    label: string;
    icon: React.ComponentType<{ className?: string }>;
    colorClass: string;
    description: string;
  }
> = {
  DRAFT: {
    label: "Draft",
    icon: FileText,
    colorClass: "border-muted-foreground/40 bg-muted/30 text-muted-foreground",
    description: "Draft — not submitted for review",
  },
  PUBLISHED: {
    label: "In Review",
    icon: ClipboardCheck,
    colorClass: "border-amber-500/60 bg-amber-500/15 text-amber-700 dark:text-amber-400",
    description: "In review queue — approve to allow Narration / audio",
  },
  PROCESSING: {
    label: "Processing",
    icon: Clock,
    colorClass: "border-blue-500/60 bg-blue-500/15 text-blue-700 dark:text-blue-400",
    description: "Pipeline running (translate / script / prep)",
  },
  READY: {
    label: "Ready",
    icon: CheckCircle,
    colorClass: "border-emerald-500/60 bg-emerald-500/15 text-emerald-700 dark:text-emerald-400",
    description: "Ready for human review in Story for review",
  },
  CHANGES_REQUESTED: {
    label: "Changes Requested",
    icon: AlertCircle,
    colorClass: "border-orange-500/60 bg-orange-500/15 text-orange-700 dark:text-orange-400",
    description: "Changes requested by reviewer",
  },
  REJECTED: {
    label: "Rejected",
    icon: XCircle,
    colorClass: "border-red-500/60 bg-red-500/15 text-red-700 dark:text-red-400",
    description: "Rejected by reviewer",
  },
};

export function StoryStatusBadge({
  status,
  narrationApproved = false,
  className,
  showIcon = true,
  size = "md",
}: StoryStatusBadgeProps) {
  const config = STATUS_CONFIG[status];
  const Icon = config.icon;

  // Special case: PUBLISHED + narrationApproved = Live on app
  const isLive = status === "PUBLISHED" && narrationApproved;
  const displayLabel = isLive ? "Live" : config.label;
  const displayColorClass = isLive
    ? "border-green-500/60 bg-green-500/15 text-green-700 dark:text-green-400"
    : config.colorClass;
  const displayDescription = isLive
    ? "Live on app — narration approved for delivery"
    : config.description;

  const sizeClasses = {
    sm: "text-[10px] px-1.5 py-0",
    md: "text-xs px-2 py-0.5",
    lg: "text-sm px-3 py-1",
  };

  const iconSizes = {
    sm: "h-3 w-3",
    md: "h-3.5 w-3.5",
    lg: "h-4 w-4",
  };

  return (
    <Badge
      variant="outline"
      className={cn(
        "inline-flex items-center gap-1 font-medium transition-all duration-200",
        displayColorClass,
        sizeClasses[size],
        status === "PROCESSING" && "animate-pulse",
        className
      )}
      title={displayDescription}
    >
      {showIcon && <Icon className={cn("shrink-0", iconSizes[size])} />}
      <span>{displayLabel}</span>
    </Badge>
  );
}

/**
 * Compact status icon (no label) for dense table views
 */
export function StoryStatusIcon({
  status,
  narrationApproved = false,
  className,
}: Omit<StoryStatusBadgeProps, "showIcon" | "size">) {
  const config = STATUS_CONFIG[status];
  const Icon = config.icon;

  const isLive = status === "PUBLISHED" && narrationApproved;
  const displayColorClass = isLive
    ? "text-green-600"
    : status === "DRAFT"
      ? "text-muted-foreground"
      : status === "PUBLISHED"
        ? "text-amber-600"
        : status === "PROCESSING"
          ? "text-blue-500"
          : status === "READY"
            ? "text-emerald-600"
            : status === "CHANGES_REQUESTED"
              ? "text-orange-600"
              : "text-red-600";

  const displayDescription = isLive
    ? "Live on app — narration approved for delivery"
    : config.description;

  return (
    <Icon
      className={cn(
        "h-5 w-5 shrink-0",
        displayColorClass,
        status === "PROCESSING" && "animate-pulse",
        className
      )}
      title={displayDescription}
    />
  );
}

/**
 * Get status label for display
 */
export function getStoryStatusLabel(
  status: StoryStatus,
  narrationApproved = false
): string {
  if (status === "PUBLISHED" && narrationApproved) {
    return "Live";
  }
  return STATUS_CONFIG[status]?.label ?? status;
}

/**
 * Get status description for tooltips
 */
export function getStoryStatusDescription(
  status: StoryStatus,
  narrationApproved = false
): string {
  if (status === "PUBLISHED" && narrationApproved) {
    return "Live on app — narration approved for delivery";
  }
  return STATUS_CONFIG[status]?.description ?? status;
}
