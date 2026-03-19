"use client";

import Link from "next/link";
import { useDashboard } from "@/hooks/use-dashboard";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import { cn } from "@/lib/utils";
import {
  Users,
  DollarSign,
  BookOpen,
  Cpu,
  Flag,
  TrendingUp,
  ChevronRight,
} from "lucide-react";
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  LineChart,
  Line,
} from "recharts";

const KPI_CONFIG = [
  {
    key: "activeSubscriptions" as const,
    label: "Active subscriptions",
    value: (k: ReturnType<typeof useDashboard>["kpis"]) =>
      k?.activeSubscriptions?.toLocaleString() ?? "—",
    icon: Users,
    description: "Current active plans",
    iconBg: "bg-tamixa-purple/15 text-tamixa-purple",
    borderAccent: "border-l-tamixa-purple",
  },
  {
    key: "monthlyRevenue" as const,
    label: "Monthly revenue",
    value: (k: ReturnType<typeof useDashboard>["kpis"]) =>
      k?.monthlyRevenue != null ? `$${k.monthlyRevenue.toLocaleString()}` : "—",
    icon: DollarSign,
    description: "This month",
    iconBg: "bg-tamixa-blue/15 text-tamixa-blue",
    borderAccent: "border-l-tamixa-blue",
  },
  {
    key: "storyGenerationsToday" as const,
    label: "Story generations today",
    value: (k: ReturnType<typeof useDashboard>["kpis"]) =>
      k?.storyGenerationsToday?.toLocaleString() ?? "—",
    icon: BookOpen,
    description: "Last 24h",
    iconBg: "bg-tamixa-yellow/20 text-tamixa-orange",
    borderAccent: "border-l-tamixa-orange",
  },
  {
    key: "aiTokenUsage" as const,
    label: "AI token usage",
    value: (k: ReturnType<typeof useDashboard>["kpis"]) =>
      k?.aiTokenUsage != null
        ? `${(k.aiTokenUsage / 1_000_000).toFixed(2)}M`
        : "—",
    icon: Cpu,
    description: "Current period",
    iconBg: "bg-tamixa-teal/15 text-tamixa-teal",
    borderAccent: "border-l-tamixa-teal",
    href: "/dashboard/ai-metrics",
    linkLabel: "View AI metrics",
  },
  {
    key: "moderationFlags" as const,
    label: "Moderation flags",
    value: (k: ReturnType<typeof useDashboard>["kpis"]) =>
      k?.moderationFlags?.toLocaleString() ?? "—",
    icon: Flag,
    description: "Pending review",
    iconBg: "bg-destructive/15 text-destructive",
    borderAccent: "border-l-destructive",
  },
];

export default function DashboardPage() {
  const { kpis, revenue, storyUsage, loading, error } = useDashboard();

  if (error) {
    return (
      <div className="space-y-8">
        <div className="page-hero">
          <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">Dashboard</h1>
          <p className="mt-1 text-sm text-muted-foreground sm:text-base">Overview and key metrics for Tamixa admin.</p>
        </div>
        <Card className="border border-destructive/40 bg-destructive/5">
          <CardContent className="pt-6">
            <p className="text-sm font-medium text-destructive">{error}</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Page hero — professional header strip */}
      <div className="page-hero">
        <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">Dashboard</h1>
        <p className="mt-1 text-sm text-muted-foreground sm:text-base">
          Overview and key metrics for Tamixa admin.
        </p>
      </div>

      {/* KPI Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        {KPI_CONFIG.map(({ key, label, value, icon: Icon, description, iconBg, borderAccent, href, linkLabel }) => (
          <Card
            key={key}
            className={cn(
              "group relative overflow-hidden border-l-4 transition-all duration-200 hover:shadow-card-hover",
              borderAccent
            )}
          >
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-semibold text-muted-foreground">
                {label}
              </CardTitle>
              <div className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-xl ${iconBg}`}>
                <Icon className="h-5 w-5" />
              </div>
            </CardHeader>
            <CardContent>
              {loading ? (
                <Skeleton className="h-8 w-24 rounded-lg" />
              ) : (
                <>
                  <div className="text-2xl font-bold tracking-tight text-foreground">
                    {value(kpis)}
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">{description}</p>
                  {href && linkLabel && (
                    <Link
                      href={href}
                      className="mt-2 inline-flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                    >
                      {linkLabel}
                      <ChevronRight className="h-3.5 w-3.5" />
                    </Link>
                  )}
                </>
              )}
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Charts */}
      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="overflow-hidden transition-shadow duration-200 hover:shadow-card-hover">
          <CardHeader className="border-b border-border bg-muted/50">
            <CardTitle className="flex items-center gap-3 text-base font-bold">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/15 text-primary">
                <TrendingUp className="h-5 w-5" />
              </div>
              <div>
                <span className="block">Revenue</span>
                <span className="text-sm font-normal text-muted-foreground">Monthly (last 6 months)</span>
              </div>
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-5">
            {loading ? (
              <Skeleton className="h-64 w-full rounded-xl" />
            ) : (
              <ResponsiveContainer width="100%" height={256}>
                <BarChart data={revenue} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                  <XAxis dataKey="month" className="text-xs" tick={{ fill: "hsl(var(--muted-foreground))" }} />
                  <YAxis className="text-xs" tick={{ fill: "hsl(var(--muted-foreground))" }} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "hsl(var(--card))",
                      border: "2px solid hsl(var(--border))",
                      borderRadius: "var(--radius)",
                      boxShadow: "var(--shadow-card)",
                    }}
                    formatter={(v: number) => [`$${v.toLocaleString()}`, "Revenue"]}
                  />
                  <Bar dataKey="revenue" fill="hsl(var(--primary))" radius={[6, 6, 0, 0]} name="Revenue" />
                </BarChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        <Card className="overflow-hidden transition-shadow duration-200 hover:shadow-card-hover">
          <CardHeader className="border-b border-border bg-muted/50">
            <CardTitle className="flex items-center gap-3 text-base font-bold">
              <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/15 text-primary">
                <BookOpen className="h-5 w-5" />
              </div>
              <div>
                <span className="block">Story usage trend</span>
                <span className="text-sm font-normal text-muted-foreground">Generations per day (last 7 days)</span>
              </div>
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-5">
            {loading ? (
              <Skeleton className="h-64 w-full rounded-xl" />
            ) : (
              <ResponsiveContainer width="100%" height={256}>
                <LineChart data={storyUsage} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                  <XAxis dataKey="date" className="text-xs" tick={{ fill: "hsl(var(--muted-foreground))" }} />
                  <YAxis className="text-xs" tick={{ fill: "hsl(var(--muted-foreground))" }} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "hsl(var(--card))",
                      border: "2px solid hsl(var(--border))",
                      borderRadius: "var(--radius)",
                      boxShadow: "var(--shadow-card)",
                    }}
                  />
                  <Line
                    type="monotone"
                    dataKey="count"
                    stroke="hsl(var(--primary))"
                    strokeWidth={2}
                    dot={{ fill: "hsl(var(--primary))", strokeWidth: 0 }}
                    name="Stories"
                  />
                </LineChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
