"use client";

import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import {
  LayoutDashboard,
  Users,
  CreditCard,
  Shield,
  Activity,
  BookOpen,
  DollarSign,
  FileText,
  UserCircle,
  Mic,
  Heart,
  Cpu,
  MessageSquare,
  ClipboardList,
  UserCog,
} from "lucide-react";
import { useAuth } from "@/contexts/auth-context";
import { canAccessNav } from "@/lib/admin-roles";

const navMain = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard, key: "dashboard" as const },
  { href: "/dashboard/revenue", label: "Revenue Analytics", icon: DollarSign, key: "revenue" as const },
  { href: "/dashboard/parents", label: "Parent management", icon: Users, key: "parents" as const },
  { href: "/dashboard/moderation", label: "Story moderation", icon: Shield, key: "moderation" as const },
  { href: "/dashboard/curated-stories", label: "Curated stories", icon: BookOpen, key: "curatedStories" as const },
  { href: "/dashboard/subscriptions", label: "Subscriptions", icon: CreditCard, key: "subscriptions" as const },
  { href: "/dashboard/monitoring", label: "System monitoring", icon: Activity, key: "monitoring" as const },
];

const navSecondary = [
  { href: "/dashboard/stories", label: "Stories", icon: FileText, key: "stories" as const },
  { href: "/dashboard/children", label: "Children", icon: UserCircle, key: "children" as const },
  { href: "/dashboard/voice-logs", label: "Voice logs", icon: Mic, key: "voiceLogs" as const },
  { href: "/dashboard/health", label: "Health", icon: Heart, key: "health" as const },
  { href: "/dashboard/ai-metrics", label: "AI metrics", icon: Cpu, key: "aiMetrics" as const },
  { href: "/dashboard/kafka", label: "Kafka", icon: MessageSquare, key: "kafka" as const },
  { href: "/dashboard/audit", label: "Audit", icon: ClipboardList, key: "audit" as const },
  { href: "/dashboard/users", label: "User management", icon: UserCog, key: "users" as const },
];

export function Sidebar() {
  const pathname = usePathname();
  const { user } = useAuth();
  const role = user?.role ?? "";
  const mainItems = navMain.filter((item) => canAccessNav(role, item.key));
  const secondaryItems = navSecondary.filter((item) => canAccessNav(role, item.key));
  return (
    <aside className="fixed left-0 top-0 z-40 flex h-screen w-56 flex-col border-r border-sidebar-border bg-sidebar-bg">
      <div className="flex h-14 items-center gap-2.5 border-b border-sidebar-border px-4">
        <Image
          src="/araro-logo-inverse.svg"
          alt="Araro"
          width={80}
          height={22}
          className="h-6 w-auto"
        />
        <Link
          href="/dashboard"
          className="font-semibold tracking-tight text-sidebar-foreground"
        >
          Admin
        </Link>
      </div>
      <nav className="flex flex-1 flex-col gap-0.5 overflow-y-auto p-3">
        <p className="mb-2 px-3 text-[11px] font-medium uppercase tracking-wider text-sidebar-muted">
          Menu
        </p>
        {mainItems.map((item) => {
          const Icon = item.icon;
          const active = pathname === item.href;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium transition-colors border-l-2",
                active
                  ? "border-l-primary bg-primary/15 text-sidebar-foreground"
                  : "border-transparent text-sidebar-muted hover:bg-white/5 hover:text-sidebar-foreground"
              )}
            >
              <Icon className="h-4 w-4 shrink-0" />
              {item.label}
            </Link>
          );
        })}
        <p className="mb-2 mt-4 px-3 text-[11px] font-medium uppercase tracking-wider text-sidebar-muted">
          More
        </p>
        {secondaryItems.map((item) => {
          const Icon = item.icon;
          const active = pathname === item.href;
          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium transition-colors border-l-2",
                active
                  ? "border-l-primary bg-primary/15 text-sidebar-foreground"
                  : "border-transparent text-sidebar-muted hover:bg-white/5 hover:text-sidebar-foreground"
              )}
            >
              <Icon className="h-4 w-4 shrink-0" />
              {item.label}
            </Link>
          );
        })}
      </nav>
    </aside>
  );
}
