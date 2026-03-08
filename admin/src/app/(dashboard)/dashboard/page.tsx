"use client";

import { useDashboard } from "@/hooks/use-dashboard";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Users,
  DollarSign,
  BookOpen,
  Cpu,
  Flag,
  TrendingUp,
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
    iconBg: "bg-primary/8 text-primary",
  },
  {
    key: "monthlyRevenue" as const,
    label: "Monthly revenue",
    value: (k: ReturnType<typeof useDashboard>["kpis"]) =>
      k?.monthlyRevenue != null ? `$${k.monthlyRevenue.toLocaleString()}` : "—",
    icon: DollarSign,
    description: "This month",
    iconBg: "bg-primary/8 text-primary",
  },
  {
    key: "storyGenerationsToday" as const,
    label: "Story generations today",
    value: (k: ReturnType<typeof useDashboard>["kpis"]) =>
      k?.storyGenerationsToday?.toLocaleString() ?? "—",
    icon: BookOpen,
    description: "Last 24h",
    iconBg: "bg-muted text-muted-foreground",
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
    iconBg: "bg-muted text-muted-foreground",
  },
  {
    key: "moderationFlags" as const,
    label: "Moderation flags",
    value: (k: ReturnType<typeof useDashboard>["kpis"]) =>
      k?.moderationFlags?.toLocaleString() ?? "—",
    icon: Flag,
    description: "Pending review",
    iconBg: "bg-destructive/8 text-destructive",
  },
];

export default function DashboardPage() {
  const { kpis, revenue, storyUsage, loading, error } = useDashboard();

  if (error) {
    return (
      <div className="space-y-8">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
          <p className="text-muted-foreground">
            Overview and key metrics for Araro admin.
          </p>
        </div>
        <Card className="border-destructive/50">
          <CardContent className="pt-6">
            <p className="text-sm text-destructive">{error}</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-foreground">Dashboard</h1>
        <p className="mt-1 text-muted-foreground">
          Overview and key metrics for Araro admin.
        </p>
      </div>

      {/* KPI Cards */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
        {KPI_CONFIG.map(({ key, label, value, icon: Icon, description, iconBg }) => (
          <Card
            key={key}
            className="overflow-hidden border border-border"
          >
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground">
                {label}
              </CardTitle>
              <div className={`flex h-9 w-9 items-center justify-center rounded-md ${iconBg}`}>
                <Icon className="h-4 w-4" />
              </div>
            </CardHeader>
            <CardContent>
              {loading ? (
                <Skeleton className="h-8 w-24" />
              ) : (
                <>
                  <div className="text-2xl font-semibold tracking-tight text-foreground">
                    {value(kpis)}
                  </div>
                  <p className="mt-0.5 text-xs text-muted-foreground">{description}</p>
                </>
              )}
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Charts */}
      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="overflow-hidden border border-border">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary/8">
                <TrendingUp className="h-4 w-4 text-primary" />
              </div>
              Revenue
            </CardTitle>
            <p className="text-sm text-muted-foreground">
              Monthly revenue (last 6 months)
            </p>
          </CardHeader>
          <CardContent>
            {loading ? (
              <Skeleton className="h-64 w-full" />
            ) : (
              <ResponsiveContainer width="100%" height={256}>
                <BarChart data={revenue} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                  <XAxis dataKey="month" className="text-xs" tick={{ fill: "hsl(var(--muted-foreground))" }} />
                  <YAxis className="text-xs" tick={{ fill: "hsl(var(--muted-foreground))" }} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "hsl(var(--card))",
                      border: "1px solid hsl(var(--border))",
                      borderRadius: "var(--radius)",
                    }}
                    formatter={(v: number) => [`$${v.toLocaleString()}`, "Revenue"]}
                  />
                  <Bar dataKey="revenue" fill="hsl(var(--primary))" radius={[2, 2, 0, 0]} name="Revenue" />
                </BarChart>
              </ResponsiveContainer>
            )}
          </CardContent>
        </Card>

        <Card className="overflow-hidden border border-border">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-base">
              <div className="flex h-8 w-8 items-center justify-center rounded-md bg-primary/8">
                <BookOpen className="h-4 w-4 text-primary" />
              </div>
              Story usage trend
            </CardTitle>
            <p className="text-sm text-muted-foreground">
              Generations per day (last 7 days)
            </p>
          </CardHeader>
          <CardContent>
            {loading ? (
              <Skeleton className="h-64 w-full" />
            ) : (
              <ResponsiveContainer width="100%" height={256}>
                <LineChart data={storyUsage} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                  <XAxis dataKey="date" className="text-xs" tick={{ fill: "hsl(var(--muted-foreground))" }} />
                  <YAxis className="text-xs" tick={{ fill: "hsl(var(--muted-foreground))" }} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "hsl(var(--card))",
                      border: "1px solid hsl(var(--border))",
                      borderRadius: "var(--radius)",
                    }}
                  />
                  <Line
                    type="monotone"
                    dataKey="count"
                    stroke="hsl(var(--primary))"
                    strokeWidth={2}
                    dot={{ fill: "hsl(var(--primary))" }}
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
