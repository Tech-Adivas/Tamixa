"use client";

import { useEffect } from "react";
import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import { X } from "lucide-react";
import { cn } from "@/lib/utils";
import { usePipelineActive } from "@/contexts/pipeline-active-context";
import { useSidebar } from "@/contexts/sidebar-context";
import {
  LayoutDashboard,
  Users,
  CreditCard,
  Shield,
  Activity,
  BookOpen,
  DollarSign,
  Mic,
  Heart,
  Cpu,
  MessageSquare,
  ClipboardList,
  UserCog,
  FlaskConical,
  Ticket,
  AlertTriangle,
  WandSparkles,
  Lightbulb,
  FolderCog,
} from "lucide-react";
import { useAuth } from "@/contexts/auth-context";
import { canAccessNav } from "@/lib/admin-roles";

const navMain = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard, key: "dashboard" as const },
  { href: "/dashboard/revenue", label: "Revenue Analytics", icon: DollarSign, key: "revenue" as const },
  { href: "/dashboard/parents", label: "Parent management", icon: Users, key: "parents" as const },
  { href: "/dashboard/moderation", label: "Story moderation", icon: Shield, key: "moderation" as const },
  { href: "/dashboard/stories", label: "Story library", icon: BookOpen, key: "storyLibrary" as const },
  { href: "/dashboard/stories/bulk-generate", label: "Bulk story generator", icon: WandSparkles, key: "storyLibrary" as const },
  { href: "/dashboard/stories-with-issues", label: "Pipeline triage", icon: AlertTriangle, key: "pipelineTriage" as const },
  { href: "/dashboard/subscriptions", label: "Subscriptions", icon: CreditCard, key: "subscriptions" as const },
  { href: "/dashboard/referral-codes", label: "Referral codes", icon: Ticket, key: "referralCodes" as const },
  { href: "/dashboard/short-content", label: "Short content", icon: Lightbulb, key: "storyLibrary" as const },
  { href: "/dashboard/monitoring", label: "System monitoring", icon: Activity, key: "monitoring" as const },
];

const navSecondary = [
  { href: "/dashboard/voice-logs", label: "Voice logs", icon: Mic, key: "voiceLogs" as const },
  { href: "/dashboard/voice-test", label: "Voice & Avatar Studio", icon: FlaskConical, key: "voiceTest" as const },
  { href: "/dashboard/health", label: "Health", icon: Heart, key: "health" as const },
  { href: "/dashboard/ai-metrics", label: "AI metrics", icon: Cpu, key: "aiMetrics" as const },
  { href: "/dashboard/ai-control-plane", label: "AI control plane", icon: FolderCog, key: "aiControlPlane" as const },
  { href: "/dashboard/kafka", label: "Kafka", icon: MessageSquare, key: "kafka" as const },
  { href: "/dashboard/audit", label: "Audit", icon: ClipboardList, key: "audit" as const },
  { href: "/dashboard/users", label: "User management", icon: UserCog, key: "users" as const },
];

/** Keys to disable when pipeline is running. Kept minimal—Story for review stays accessible so you can work on other stories; only the story in progress is blocked. */
const PIPELINE_DISABLED_NAV_KEYS = new Set<string>([]);

export function Sidebar() {
  const pathname = usePathname();
  const { user } = useAuth();
  const { isPipelineActive } = usePipelineActive();
  const { open, setOpen } = useSidebar();
  const role = user?.role ?? "";
  const mainItems = navMain.filter((item) => canAccessNav(role, item.key));
  const secondaryItems = navSecondary.filter((item) => canAccessNav(role, item.key));
  const isNavDisabled = (key: string) =>
    isPipelineActive && PIPELINE_DISABLED_NAV_KEYS.has(key);

  // Close sidebar on route change (mobile)
  useEffect(() => {
    setOpen(false);
  }, [pathname, setOpen]);

  return (
    <>
      {/* Mobile backdrop */}
      <div
        aria-hidden="true"
        className={cn(
          "fixed inset-0 z-40 bg-black/50 transition-opacity duration-200 md:hidden",
          open ? "opacity-100" : "pointer-events-none opacity-0"
        )}
        onClick={() => setOpen(false)}
      />
      <aside
        className={cn(
          "fixed left-0 top-0 z-50 flex h-screen w-56 max-w-[85vw] flex-col border-r border-sidebar-border bg-tamixa-sidebar shadow-elevated md:translate-x-0 md:shadow-none transition-transform duration-200 ease-out",
          open ? "translate-x-0" : "-translate-x-full"
        )}
        aria-label="Main navigation"
      >
      <div className="flex min-h-touch items-center justify-between gap-3 border-b border-sidebar-border/80 px-4">
        <div className="flex min-h-touch items-center gap-3">
          <Link
            href="/dashboard"
            className="tamixa-app-icon flex flex-shrink-0 rounded-xl bg-white/10 min-h-[48px] min-w-[48px] w-12 h-12 focus:outline-none focus:ring-2 focus:ring-sidebar-accent focus:ring-offset-2 focus:ring-offset-transparent [&>span]:!block [&>span]:!size-full [&>span]:!flex [&>span]:!items-center [&>span]:!justify-center"
            aria-label="Tamixa Admin home"
            onClick={() => setOpen(false)}
          >
            <Image
              src="/tamixa-logo-inverse.svg"
              alt=""
              width={128}
              height={36}
              className="object-contain"
            />
          </Link>
          <Link
            href="/dashboard"
            className="font-semibold tracking-tight text-sidebar-foreground"
            onClick={() => setOpen(false)}
          >
            Admin
          </Link>
        </div>
        <button
          type="button"
          onClick={() => setOpen(false)}
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl text-sidebar-foreground hover:bg-white/10 focus:outline-none focus:ring-2 focus:ring-sidebar-accent md:hidden"
          aria-label="Close menu"
        >
          <X className="h-5 w-5" />
        </button>
      </div>
      <nav className="flex flex-1 flex-col gap-0.5 overflow-y-auto p-3">
        <p className="mb-2 px-3 text-[11px] font-semibold uppercase tracking-wider text-sidebar-muted">
          Menu
        </p>
        {mainItems.map((item) => {
          const Icon = item.icon;
          const exactMatch = pathname === item.href;
          const nestedMatch =
            item.href !== "/dashboard" && pathname.startsWith(item.href + "/");
          const active = exactMatch || nestedMatch;
          const disabled = isNavDisabled(item.key);
          return disabled ? (
            <span
              key={item.href}
              className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold border-l-2 border-transparent text-sidebar-muted/60 cursor-not-allowed"
              title="Unavailable while TTS pipeline is running"
            >
              <Icon className="h-4 w-4 shrink-0" />
              {item.label}
            </span>
          ) : (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition-all duration-standard border-l-2",
                active
                  ? "border-l-sidebar-accent bg-sidebar-accent/15 text-sidebar-foreground"
                  : "border-transparent text-sidebar-muted hover:bg-white/5 hover:text-sidebar-foreground"
              )}
              aria-current={active ? "page" : undefined}
              aria-label={item.label}
              onClick={() => setOpen(false)}
            >
              <Icon className="h-4 w-4 shrink-0" />
              {item.label}
            </Link>
          );
        })}
        <p className="mb-2 mt-4 px-3 text-[11px] font-semibold uppercase tracking-wider text-sidebar-muted">
          More
        </p>
        {secondaryItems.map((item) => {
          const Icon = item.icon;
          const exactMatch = pathname === item.href;
          const nestedMatch =
            item.href !== "/dashboard" && pathname.startsWith(item.href + "/");
          const active = exactMatch || nestedMatch;
          const disabled = isNavDisabled(item.key);
          return disabled ? (
            <span
              key={item.href}
              className="flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold border-l-2 border-transparent text-sidebar-muted/60 cursor-not-allowed"
              title="Unavailable while TTS pipeline is running"
            >
              <Icon className="h-4 w-4 shrink-0" />
              {item.label}
            </span>
          ) : (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold transition-all duration-standard border-l-2",
                active
                  ? "border-l-sidebar-accent bg-sidebar-accent/15 text-sidebar-foreground"
                  : "border-transparent text-sidebar-muted hover:bg-white/5 hover:text-sidebar-foreground"
              )}
              aria-current={active ? "page" : undefined}
              aria-label={item.label}
              onClick={() => setOpen(false)}
            >
              <Icon className="h-4 w-4 shrink-0" />
              {item.label}
            </Link>
          );
        })}
      </nav>
    </aside>
    </>
  );
}
