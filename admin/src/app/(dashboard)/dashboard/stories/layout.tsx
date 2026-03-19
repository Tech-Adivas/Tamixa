"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import { BookOpen, ClipboardCheck, Mic } from "lucide-react";

export default function StoriesLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname() ?? "";

  const isStoryTab =
    pathname === "/dashboard/stories" ||
    (pathname.startsWith("/dashboard/stories/") &&
      pathname !== "/dashboard/stories/approve" &&
      pathname !== "/dashboard/stories/to-speech");
  const isApproveTab = pathname === "/dashboard/stories/approve";
  const isToSpeechTab = pathname === "/dashboard/stories/to-speech";

  return (
    <div className="space-y-4">
      <div className="border-b border-border bg-muted/30">
        <nav className="flex gap-0 px-1" aria-label="Story library tabs">
          <Link
            href="/dashboard/stories"
            className={cn(
              "flex items-center gap-2 rounded-t-md border border-transparent border-b-0 px-4 py-3 text-sm font-medium transition-colors -mb-px",
              isStoryTab
                ? "bg-primary text-primary-foreground border border-border border-b-2 border-b-card shadow-sm"
                : "text-muted-foreground hover:bg-muted hover:text-foreground"
            )}
          >
            <BookOpen className="h-4 w-4 shrink-0" />
            Library
          </Link>
          <Link
            href="/dashboard/stories/approve"
            className={cn(
              "flex items-center gap-2 rounded-t-md border border-transparent border-b-0 px-4 py-3 text-sm font-medium transition-colors -mb-px",
              isApproveTab
                ? "bg-primary text-primary-foreground border border-border border-b-2 border-b-card shadow-sm"
                : "text-muted-foreground hover:bg-muted hover:text-foreground"
            )}
          >
            <ClipboardCheck className="h-4 w-4 shrink-0" />
            Review
          </Link>
          <Link
            href="/dashboard/stories/to-speech"
            className={cn(
              "flex items-center gap-2 rounded-t-md border border-transparent border-b-0 px-4 py-3 text-sm font-medium transition-colors -mb-px",
              isToSpeechTab
                ? "bg-primary text-primary-foreground border border-border border-b-2 border-b-card shadow-sm"
                : "text-muted-foreground hover:bg-muted hover:text-foreground"
            )}
          >
            <Mic className="h-4 w-4 shrink-0" />
            Narration
          </Link>
        </nav>
      </div>
      {children}
    </div>
  );
}
