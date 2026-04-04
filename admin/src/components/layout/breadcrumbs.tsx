"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";

export interface BreadcrumbItem {
  label: string;
  href?: string;
}

interface BreadcrumbsProps {
  /** Override automatic breadcrumbs from pathname */
  items?: BreadcrumbItem[];
  className?: string;
}

const SEGMENT_LABELS: Record<string, string> = {
  dashboard: "Dashboard",
  stories: "Story library",
  "bulk-generate": "Bulk generate",
  approve: "Review",
  "to-speech": "Narration",
  new: "New story",
  edit: "Edit",
  parents: "Parents",
  revenue: "Revenue",
  moderation: "Moderation",
  subscriptions: "Subscriptions",
  "referral-codes": "Referral codes",
  monitoring: "Monitoring",
  "voice-logs": "Voice logs",
  "voice-test": "Voice & Avatar Studio",
  health: "Health",
  "ai-metrics": "AI metrics",
  "ai-control-plane": "AI control plane",
  "workflow-runs": "Workflow runs",
  kafka: "Kafka",
  audit: "Audit",
  users: "User management",
  "stories-with-issues": "Pipeline triage",
  "short-content": "Short content",
  "edu-simulator-analytics": "Edu simulator analytics",
};

function getLabel(segment: string): string {
  return SEGMENT_LABELS[segment] ?? segment;
}

export function Breadcrumbs({ items, className }: BreadcrumbsProps) {
  const pathname = usePathname() ?? "";
  const segments = pathname.split("/").filter(Boolean);

  const resolvedItems: BreadcrumbItem[] =
    items ??
    segments.map((segment, i) => {
      const href = "/" + segments.slice(0, i + 1).join("/");
      const isLast = i === segments.length - 1;
      const isNumeric = /^\d+$/.test(segment);
      const label = isNumeric ? `#${segment}` : getLabel(segment);
      return {
        label: isLast && isNumeric ? `Story ${segment}` : label,
        href: isLast ? undefined : href,
      };
    });

  if (resolvedItems.length === 0) return null;

  return (
    <nav aria-label="Breadcrumb" className={cn("flex items-center gap-1 text-sm", className)}>
      {resolvedItems.map((item, i) => (
        <span key={i} className="flex items-center gap-1">
          {i > 0 && (
            <ChevronRight className="h-4 w-4 shrink-0 text-muted-foreground" aria-hidden />
          )}
          {item.href ? (
            <Link
              href={item.href}
              className="text-muted-foreground hover:text-foreground transition-colors"
            >
              {item.label}
            </Link>
          ) : (
            <span className="font-medium text-foreground" aria-current="page">
              {item.label}
            </span>
          )}
        </span>
      ))}
    </nav>
  );
}
