"use client";

import { cn } from "@/lib/utils";
import { Breadcrumbs, type BreadcrumbItem } from "./breadcrumbs";

interface PageHeaderProps {
  title: string;
  description?: string;
  /** "default" = compact; "hero" = full strip */
  variant?: "default" | "hero";
  /** true = auto from pathname; BreadcrumbItem[] = custom; false/undefined = none */
  breadcrumbs?: boolean | BreadcrumbItem[];
  className?: string;
}

export function PageHeader({
  title,
  description,
  variant = "default",
  breadcrumbs,
  className,
}: PageHeaderProps) {
  const content = (
    <>
      {breadcrumbs === true && <Breadcrumbs className="mb-2" />}
      {Array.isArray(breadcrumbs) && breadcrumbs.length > 0 && (
        <Breadcrumbs items={breadcrumbs} className="mb-2" />
      )}
      <h1 className="page-header text-foreground" id="page-title">
        {title}
      </h1>
      {description && <p className="page-subheader">{description}</p>}
    </>
  );

  if (variant === "hero") {
    return (
      <div className={cn("page-hero", className)}>
        {content}
      </div>
    );
  }

  return (
    <div className={cn("space-y-1", className)}>
      {content}
    </div>
  );
}
